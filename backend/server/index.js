const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');
const crypto = require('crypto');
require('dotenv').config();

// Enforce secure configuration: require DB credentials and least-privileged role (never postgres)
const requiredEnv = ['DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASSWORD'];
for (const envKey of requiredEnv) {
  if (!process.env[envKey]) {
    console.error(`FATAL SECURITY ERROR: Missing required environment variable ${envKey}. Startup aborted.`);
    process.exit(1);
  }
}

if (process.env.DB_USER === 'postgres') {
  console.error('FATAL SECURITY ERROR: Default PostgreSQL superuser (postgres) is forbidden. Use a least-privileged role.');
  process.exit(1);
}

const app = express();

const allowedOrigin = process.env.WEB_ORIGIN || 'http://localhost:3001';
app.use(cors({
  origin: [allowedOrigin, 'http://localhost:3000', 'http://127.0.0.1:3001'],
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-farm-id']
}));

app.use(express.json());

const pool = new Pool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT, 10) || 5432,
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD
});

// Helper: Hash password with salt for verification (Strict PBKDF2 enforcement)
function verifyPassword(password, storedHash) {
  if (!storedHash || typeof storedHash !== 'string') return false;
  try {
    const parts = storedHash.split(':');
    if (parts.length !== 2) return false;
    const [salt, originalHash] = parts;
    const hashVerify = crypto.pbkdf2Sync(password, salt, 10000, 64, 'sha512').toString('hex');
    return hashVerify === originalHash;
  } catch (e) {
    return false;
  }
}

// Middleware: Transaction-scoped authentication and query execution on a single checked-out client
async function withAuthenticatedClient(req, res, next) {
  const authHeader = req.headers.authorization;
  const farmId = req.headers['x-farm-id'] || req.query.farm_id || req.body.farm_id;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized: Missing or invalid Authorization header' });
  }

  if (!farmId) {
    return res.status(400).json({ error: 'Bad Request: Missing x-farm-id header or farm_id parameter' });
  }

  const token = authHeader.split(' ')[1];
  const client = await pool.connect();

  try {
    await client.query('BEGIN');
    const authRes = await client.query('SELECT auth.authenticate_session($1, $2)', [token, farmId]);
    req.userId = authRes.rows[0].authenticate_session;
    req.farmId = farmId;
    req.dbClient = client;
    next();
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    client.release();
    console.error('Authentication failure for token [REDACTED]');
    return res.status(401).json({ error: 'Unauthorized: Invalid or expired session' });
  }
}

// Health check endpoint
app.get('/api/health', async (req, res) => {
  try {
    const dbRes = await pool.query('SELECT NOW()');
    res.json({ status: 'healthy', time: dbRes.rows[0].now });
  } catch (err) {
    res.status(500).json({ status: 'unhealthy', error: 'Database unavailable' });
  }
});

// Secure Login Endpoint: requires email and password verification, issues fresh cryptographically secure session token
app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required' });
  }

  try {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const userRes = await client.query('SELECT * FROM auth.get_user_auth_data($1)', [email]);
      if (userRes.rows.length === 0) {
        await client.query('ROLLBACK');
        client.release();
        return res.status(401).json({ error: 'Invalid email or password' });
      }

      const user = userRes.rows[0];
      const passwordValid = verifyPassword(password, user.password_hash);

      if (!passwordValid) {
        await client.query('ROLLBACK');
        client.release();
        return res.status(401).json({ error: 'Invalid email or password' });
      }

      if (!user.farm_id) {
        await client.query('ROLLBACK');
        client.release();
        return res.status(403).json({ error: 'User is not assigned to any farm' });
      }

      // Generate session using SECURITY DEFINER function
      const sessionRes = await client.query(
        'SELECT auth.create_session($1, $2) AS session_token',
        [user.user_id, user.farm_id]
      );
      const sessionToken = sessionRes.rows[0].session_token;

      await client.query('COMMIT');
      client.release();

      console.log(`[AUTH] Successful login for user ${email}.`);
      res.json({
        token: sessionToken,
        farm_id: user.farm_id,
        email: email
      });
    } catch (innerErr) {
      await client.query('ROLLBACK').catch(() => {});
      client.release();
      throw innerErr;
    }
  } catch (err) {
    console.error('Login error:', err.message);
    res.status(401).json({ error: 'Authentication failed' });
  }
});

// Secure Registration Endpoint: allows new users to create an account and default farm
app.post('/api/auth/register', async (req, res) => {
  const { email, password, full_name, farm_name } = req.body;
  if (!email || !password || !full_name) {
    return res.status(400).json({ error: 'Email, password, and full name are required' });
  }

  if (password.length < 6) {
    return res.status(400).json({ error: 'Password must be at least 6 characters long' });
  }

  try {
    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      // Hash password securely with PBKDF2
      const salt = crypto.randomBytes(16).toString('hex');
      const hash = crypto.pbkdf2Sync(password, salt, 10000, 64, 'sha512').toString('hex');
      const passwordHash = `${salt}:${hash}`;

      // Call secure SECURITY DEFINER function auth.register_user
      const regRes = await client.query(
        'SELECT * FROM auth.register_user($1, $2, $3, $4)',
        [email, passwordHash, full_name, farm_name || null]
      );

      const regData = regRes.rows[0];

      await client.query('COMMIT');
      client.release();

      console.log(`[AUTH] Successful registration and login for new user ${email}.`);
      res.status(201).json({
        token: regData.out_session_token,
        farm_id: regData.out_farm_id,
        email: regData.out_email,
        full_name: regData.out_full_name
      });
    } catch (innerErr) {
      await client.query('ROLLBACK').catch(() => {});
      client.release();
      if (innerErr.code === '23505' || innerErr.message.includes('already exists')) {
        return res.status(409).json({ error: 'User with this email already exists' });
      }
      throw innerErr;
    }
  } catch (err) {
    console.error('Registration error:', err.message);
    res.status(500).json({ error: 'Registration failed: ' + err.message });
  }
});

// Logout endpoint: revokes session token
app.post('/api/auth/logout', async (req, res) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(400).json({ error: 'Missing token' });
  }
  const token = authHeader.split(' ')[1];
  try {
    await pool.query('SELECT auth.revoke_session($1)', [token]);
    res.json({ status: 'success', message: 'Logged out successfully' });
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Protected GET /api/farms: returns ONLY farms the authenticated user is authorized to access
app.get('/api/farms', async (req, res) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  const token = authHeader.split(' ')[1];

  try {
    const client = await pool.connect();
    try {
      const farmsRes = await client.query(
        'SELECT * FROM auth.get_user_farms($1)',
        [token]
      );
      if (farmsRes.rows.length === 0) {
        return res.status(401).json({ error: 'Unauthorized: Invalid or expired session' });
      }
      res.json({ farms: farmsRes.rows });
    } finally {
      client.release();
    }
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Protected GET /api/livestock
app.get('/api/livestock', withAuthenticatedClient, async (req, res) => {
  try {
    const result = await req.dbClient.query(
      'SELECT global_id, tag, species, breed, sex, status, server_revision, created_at FROM livestock WHERE farm_id = $1 AND deleted_at IS NULL ORDER BY created_at DESC',
      [req.farmId]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.json({ livestock: result.rows });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(500).json({ error: 'Internal server error during livestock retrieval' });
  }
});

// Protected POST /api/livestock
app.post('/api/livestock', withAuthenticatedClient, async (req, res) => {
  const { tag, species, breed, sex } = req.body;
  if (!tag || !species || !breed) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    return res.status(400).json({ error: 'Bad Request: Missing required fields (tag, species, breed)' });
  }

  try {
    const insertRes = await req.dbClient.query(
      'SELECT create_livestock($1, $2, $3, $4, $5)',
      [req.farmId, tag, species, breed, sex || 'UNKNOWN']
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.status(201).json({ status: 'success', global_id: insertRes.rows[0].create_livestock });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(400).json({ error: 'Bad Request: Unable to create livestock record' });
  }
});

// Protected GET /api/financials
app.get('/api/financials', withAuthenticatedClient, async (req, res) => {
  try {
    const result = await req.dbClient.query(
      'SELECT global_id, entry_type, category, amount, entry_date, notes, server_revision, created_at FROM financial_ledgers WHERE farm_id = $1 ORDER BY entry_date DESC',
      [req.farmId]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.json({ financials: result.rows });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(500).json({ error: 'Internal server error during financials retrieval' });
  }
});

// Protected POST /api/financials
app.post('/api/financials', withAuthenticatedClient, async (req, res) => {
  const { entry_type, category, amount, entry_date, notes } = req.body;
  if (!entry_type || !category || amount === undefined) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    return res.status(400).json({ error: 'Missing entry_type, category, or amount' });
  }
  try {
    const resId = await req.dbClient.query(
      'SELECT insert_financial_ledger($1, $2, $3, $4, $5, $6)',
      [req.farmId, entry_type, category, amount, entry_date || null, notes || null]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.status(201).json({ status: 'success', global_id: resId.rows[0].insert_financial_ledger });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(400).json({ error: err.message });
  }
});

// Protected GET /api/milk
app.get('/api/milk', withAuthenticatedClient, async (req, res) => {
  try {
    const result = await req.dbClient.query(
      'SELECT id, livestock_tag, litres, shift, recorded_date, notes FROM milk_production WHERE farm_id = $1 ORDER BY recorded_date DESC',
      [req.farmId]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.json({ milk: result.rows });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Protected POST /api/milk
app.post('/api/milk', withAuthenticatedClient, async (req, res) => {
  const { tag, litres, shift, date, notes } = req.body;
  if (!tag || litres === undefined) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    return res.status(400).json({ error: 'Missing tag or litres' });
  }
  try {
    const resId = await req.dbClient.query(
      'SELECT auth.add_milk_entry($1, $2, $3, $4, $5, $6)',
      [req.farmId, tag, litres, shift || 'MORNING', date || null, notes || null]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.status(201).json({ status: 'success', id: resId.rows[0].add_milk_entry });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(400).json({ error: err.message });
  }
});

// Protected GET /api/breeding
app.get('/api/breeding', withAuthenticatedClient, async (req, res) => {
  try {
    const result = await req.dbClient.query(
      'SELECT id, livestock_tag, sire_tag, mating_date, expected_calving_date, status, notes FROM breeding_records WHERE farm_id = $1 ORDER BY mating_date DESC',
      [req.farmId]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.json({ breeding: result.rows });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Protected POST /api/breeding
app.post('/api/breeding', withAuthenticatedClient, async (req, res) => {
  const { tag, sire_tag, mating_date, notes } = req.body;
  if (!tag || !mating_date) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    return res.status(400).json({ error: 'Missing tag or mating_date' });
  }
  try {
    const resId = await req.dbClient.query(
      'SELECT auth.add_breeding_entry($1, $2, $3, $4, $5)',
      [req.farmId, tag, sire_tag || null, mating_date, notes || null]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.status(201).json({ status: 'success', id: resId.rows[0].add_breeding_entry });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(400).json({ error: err.message });
  }
});

// Protected GET /api/health-records
app.get('/api/health-records', withAuthenticatedClient, async (req, res) => {
  try {
    const result = await req.dbClient.query(
      'SELECT id, livestock_tag, record_type, disease_name, symptoms, diagnosis, treatment, cost, vet_name, record_date FROM health_records WHERE farm_id = $1 ORDER BY record_date DESC',
      [req.farmId]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.json({ health: result.rows });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Protected POST /api/health-records
app.post('/api/health-records', withAuthenticatedClient, async (req, res) => {
  const { tag, record_type, disease_name, symptoms, diagnosis, treatment, cost, vet_name, record_date } = req.body;
  if (!tag) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    return res.status(400).json({ error: 'Missing tag' });
  }
  try {
    const resId = await req.dbClient.query(
      'SELECT auth.add_health_record($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)',
      [req.farmId, tag, record_type || 'ILLNESS', disease_name || null, symptoms || null, diagnosis || null, treatment || null, cost || 0, vet_name || null, record_date || null]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.status(201).json({ status: 'success', id: resId.rows[0].add_health_record });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(400).json({ error: err.message });
  }
});

// Protected GET /api/inventory
app.get('/api/inventory', withAuthenticatedClient, async (req, res) => {
  try {
    const result = await req.dbClient.query(
      'SELECT id, item_name, category, quantity, unit, min_quantity FROM inventory WHERE farm_id = $1 ORDER BY item_name',
      [req.farmId]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.json({ inventory: result.rows });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Protected POST /api/inventory
app.post('/api/inventory', withAuthenticatedClient, async (req, res) => {
  const { item_name, category, quantity, unit, min_quantity } = req.body;
  if (!item_name || !category) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    return res.status(400).json({ error: 'Missing item_name or category' });
  }
  try {
    const resId = await req.dbClient.query(
      'SELECT auth.add_inventory_item($1, $2, $3, $4, $5, $6)',
      [req.farmId, item_name, category, quantity || 0, unit || 'KG', min_quantity || 10]
    );
    await req.dbClient.query('COMMIT');
    req.dbClient.release();
    res.status(201).json({ status: 'success', id: resId.rows[0].add_inventory_item });
  } catch (err) {
    await req.dbClient.query('ROLLBACK').catch(() => {});
    req.dbClient.release();
    res.status(400).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`MFarm Secure Backend API Server running on port ${PORT}`);
});
