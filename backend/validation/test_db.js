const { Pool } = require('pg');
const pool = new Pool({
  host: 'localhost',
  port: 5432,
  database: 'postgres',
  user: 'postgres',
  password: process.env.PGPASSWORD || 'postgres'
});

pool.query('SELECT current_user, current_database()', (err, res) => {
  if (err) {
    console.error('Connection failed:', err.message);
  } else {
    console.log('Connected successfully:', res.rows[0]);
  }
  pool.end();
});
