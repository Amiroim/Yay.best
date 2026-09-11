require('dotenv').config();

const { createApp } = require('./app');
const { verifyConnection, pool } = require('./db');

const PORT = process.env.PORT || 3000;

async function main() {
  try {
    await verifyConnection();
    console.log('[mysql] connected');
  } catch (err) {
    console.error('[mysql] could not connect at startup:', err.message);
    console.error('[mysql] the pool will keep retrying on incoming requests, but fix DB_* in .env if this persists.');
  }

  const app = createApp();
  const server = app.listen(PORT, () => {
    console.log(`[http] listening on port ${PORT}`);
  });

  const shutdown = async (signal) => {
    console.log(`[shutdown] received ${signal}, closing...`);
    server.close(async () => {
      await pool.end();
      process.exit(0);
    });
  };

  process.on('SIGINT', () => shutdown('SIGINT'));
  process.on('SIGTERM', () => shutdown('SIGTERM'));
}

main();
