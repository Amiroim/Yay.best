const express = require('express');
const helmet = require('helmet');

const { requireApiKey } = require('./middleware/apiKey');
const { notFound, errorHandler } = require('./middleware/errorHandler');
const authRoutes = require('./routes/auth');

function createApp() {
  const app = express();

  // If you put this behind nginx/Caddy/a load balancer, this makes req.ip
  // reflect the real client IP (X-Forwarded-For) instead of the proxy's IP.
  // Set to the number of proxy hops in front of the app, e.g. 1.
  app.set('trust proxy', 1);

  app.use(helmet());
  app.use(express.json({ limit: '10kb' }));

  app.get('/health', (req, res) => res.json({ ok: true }));

  app.use('/api', requireApiKey, authRoutes);

  app.use(notFound);
  app.use(errorHandler);

  return app;
}

module.exports = { createApp };
