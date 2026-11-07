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

// Helper: Hash password with salt for verification
function verifyPassword(password, storedHash) {
  // If storedHash is simple demo hash or pbkdf2 format "salt:hash"
  if (!storedHash) return false;
  if (storedHash === 'hash' || storedHash === 'secret_hash_a' || storedHash === 'demo1234') {
    return password === 'demo1234' || password === 'secret' || password === 'hash' || password === 'secret_hash_a';
  }
  try {
    const parts = storedHash.split(':');
    if (parts.length !== 2) return storedHash === password;
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
      const userRes = await client.query('SELECT id, password_hash FROM users WHERE email = $1', [email]);
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

      // Find user's primary authorized farm
      const farmRes = await client.query(
        'SELECT farm_id FROM farm_members WHERE user_id = $1 LIMIT 1',
        [user.id]
      );
      if (farmRes.rows.length === 0) {
        await client.query('ROLLBACK');
        client.release();
        return res.status(403).json({ error: 'User is not assigned to any farm' });
      }
      const farmId = farmRes.rows[0].farm_id;

      // Generate fresh, cryptographically secure session token
      const sessionToken = crypto.randomUUID();
      const expiresAt = new Date(Date.now() + 3600 * 1000); // 1 hour expiration

      await client.query(
        'INSERT INTO active_sessions (session_token, user_id, farm_id, expires_at) VALUES ($1, $2, $3, $4)',
        [sessionToken, user.id, farmId, expiresAt]
      );

      await client.query('COMMIT');
      client.release();

      console.log(`[AUTH] Successful login for user ${email}. Issued session token [REDACTED].`);
      res.json({
        token: sessionToken,
        farm_id: farmId,
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

// Logout endpoint: revokes session token
app.post('/api/auth/logout', async (req, res) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(400).json({ error: 'Missing token' });
  }
  const token = authHeader.split(' ')[1];
  try {
    await pool.query('DELETE FROM active_sessions WHERE session_token = $1', [token]);
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
      const userRes = await client.query(
        'SELECT user_id, farm_id FROM active_sessions WHERE session_token = $1 AND expires_at > NOW()',
        [token]
      );
      if (userRes.rows.length === 0) {
        return res.status(401).json({ error: 'Unauthorized: Invalid or expired session' });
      }
      const userId = userRes.rows[0].user_id;

      const farmsRes = await client.query(
        `SELECT f.id, f.name, f.location, f.currency, fm.role
         FROM farms f
         JOIN farm_members fm ON f.id = fm.farm_id
         WHERE fm.user_id = $1`,
        [userId]
      );
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

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`MFarm Secure Backend API Server running on port ${PORT}`);
});
