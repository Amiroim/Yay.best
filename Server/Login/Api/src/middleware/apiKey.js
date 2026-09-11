const crypto = require('crypto');

/**
 * The Minecraft plugin must send: X-API-Key: <same value as API_KEY in .env>
 * This isn't in the original API contract the plugin currently uses — you'll
 * need to add this header to DatabaseAPI.java's requests too. Without it,
 * anyone who finds your server's URL could call /api/login and brute-force
 * or probe accounts directly, bypassing the plugin's own brute-force guard.
 */
function requireApiKey(req, res, next) {
  const expected = process.env.API_KEY;
  const provided = req.get('X-API-Key');

  if (!expected) {
    console.error('[auth] API_KEY is not set in the environment — refusing all requests.');
    return res.status(500).json({ error: 'server_misconfigured' });
  }

  if (!provided) {
    return res.status(401).json({ error: 'missing_api_key' });
  }

  const a = Buffer.from(provided);
  const b = Buffer.from(expected);
  const valid = a.length === b.length && crypto.timingSafeEqual(a, b);

  if (!valid) {
    return res.status(401).json({ error: 'invalid_api_key' });
  }

  next();
}

module.exports = { requireApiKey };
