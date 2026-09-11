const mysql = require('mysql2/promise');

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  port: Number(process.env.DB_PORT) || 3306,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  waitForConnections: true,
  connectionLimit: Number(process.env.DB_CONNECTION_LIMIT) || 10,
  queueLimit: 0,
  // Kill queries that hang instead of piling up forever if MySQL is stuck.
  connectTimeout: 10000,
  enableKeepAlive: true,
  keepAliveInitialDelay: 10000,
});

// A pool already opens a fresh connection whenever the old one dies, so
// "reconnect" mostly happens for free. Still, log pool-level errors so a
// dead DB doesn't fail silently, and add a small retry for the handful of
// transient error codes that mean "the connection under you just died".
const TRANSIENT_CODES = new Set([
  'PROTOCOL_CONNECTION_LOST',
  'ECONNRESET',
  'ETIMEDOUT',
  'ER_LOCK_DEADLOCK',
  'PROTOCOL_SEQUENCE_TIMEOUT',
]);

pool.on('error', (err) => {
  console.error('[mysql] pool error:', err.code || err.message);
});

/**
 * Run a parameterized query with one automatic retry on transient
 * connection errors. Always use `?` placeholders — never string-concat
 * user input into `sql`.
 */
async function query(sql, params = [], { retries = 1 } = {}) {
  try {
    const [rows] = await pool.execute(sql, params);
    return rows;
  } catch (err) {
    if (retries > 0 && TRANSIENT_CODES.has(err.code)) {
      console.warn(`[mysql] transient error (${err.code}), retrying query...`);
      return query(sql, params, { retries: retries - 1 });
    }
    throw err;
  }
}

/** Acquire a dedicated connection for multi-statement transactions. */
async function withTransaction(work) {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const result = await work(conn);
    await conn.commit();
    return result;
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

/** Call once at startup to fail fast if the DB is unreachable/misconfigured. */
async function verifyConnection() {
  const conn = await pool.getConnection();
  await conn.ping();
  conn.release();
}

module.exports = { pool, query, withTransaction, verifyConnection };
