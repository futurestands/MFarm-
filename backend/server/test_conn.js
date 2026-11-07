const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT, 10) || 5432,
  database: 'postgres',
  user: 'postgres',
  password: process.env.PGPASSWORD || 'postgres'
});

pool.query('SELECT current_user, current_database()', (err, res) => {
  if (err) {
    console.error('Connection failed:', err.message);
  } else {
    console.log('Connected successfully as superuser:', res.rows[0]);
  }
  pool.end();
});
