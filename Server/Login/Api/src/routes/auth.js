const express = require('express');
const bcrypt = require('bcrypt');
const rateLimit = require('express-rate-limit');

const { query, withTransaction } = require('../db');
const { asyncHandler } = require('../middleware/errorHandler');
const {
  isValidUsername,
  isValidPassword,
  normalizeUuid,
  isBlockedStatus,
  isValidIp,
} = require('../utils/validators');

const router = express.Router();

// Extra layer on top of the plugin's own brute-force guard: caps requests
// per source IP regardless of which username is being tried.
const loginLimiter = rateLimit({
  windowMs: 60 * 1000,
  limit: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'too_many_requests' },
});

// NOTE: there is intentionally no "get the caller's IP" helper here anymore.
// req.ip is the IP of whoever called this API — which is always the
// Minecraft server itself (the plugin runs server-side and makes this HTTP
// call from the server's own network), never the player. The player's real
// IP has to come from the plugin, which reads it off the player's actual
// game connection (player.getAddress()) and sends it as `ip` in the body.

/**
 * GET /api/checkUser?username=Steve
 * Called from AsyncPlayerPreLoginEvent, before the player is let into the
 * lock. Tells the plugin whether the username exists AND whether the
 * account is currently blocked, so banned/restricted players can be kicked
 * immediately instead of occupying a lock slot.
 */
router.get(
  '/checkUser',
  asyncHandler(async (req, res) => {
    const { username } = req.query;

    if (!isValidUsername(username)) {
      return res.status(400).json({ error: 'invalid_username' });
    }

    const rows = await query(
      'SELECT staus AS status FROM users WHERE user_name = ? LIMIT 1',
      [username]
    );

    if (rows.length === 0) {
      return res.json({ exists: false });
    }

    const blocked = isBlockedStatus(rows[0].status);
    return res.json({
      exists: true,
      allowed: !blocked,
      ...(blocked ? { reason: rows[0].status.toLowerCase() } : {}),
    });
  })
);

/**
 * POST /api/login   { username, password, uuid, ip }
 * Called from /login <password>. `ip` must be the PLAYER's real IP as seen
 * by the plugin (player.getAddress()) — never trust req.ip here, since
 * every call to this API comes from the Minecraft server itself, not the
 * player's machine. On success this is also where the player's Minecraft
 * UUID gets linked to the account the FIRST time they ever log in from the
 * game — see README "how UUID linking works".
 */
router.post(
  '/login',
  loginLimiter,
  asyncHandler(async (req, res) => {
    const { username, password, uuid, ip } = req.body || {};
    const normalizedUuid = normalizeUuid(uuid);

    if (
      !isValidUsername(username) ||
      !isValidPassword(password) ||
      !normalizedUuid ||
      !isValidIp(ip)
    ) {
      return res.status(400).json({ valid: false, reason: 'invalid_request' });
    }

    const rows = await query(
      'SELECT id, staus AS status, password, player_uuid FROM users WHERE user_name = ? LIMIT 1',
      [username]
    );

    if (rows.length === 0) {
      return res.status(404).json({ valid: false, reason: 'not_found' });
    }

    const user = rows[0];

    if (isBlockedStatus(user.status)) {
      return res.status(403).json({ valid: false, reason: user.status.toLowerCase() });
    }

    const passwordOk = await bcrypt.compare(password, user.password);
    if (!passwordOk) {
      return res.status(401).json({ valid: false, reason: 'invalid_credentials' });
    }

    // Account already linked to a DIFFERENT Minecraft account than the one
    // logging in — reject. Without this check, anyone who knows a
    // website username+password could log into the game AS that account
    // from a totally different Minecraft account.
    if (user.player_uuid && user.player_uuid !== normalizedUuid) {
      return res.status(409).json({ valid: false, reason: 'uuid_mismatch' });
    }

    await withTransaction(async (conn) => {
      // First successful game login for this account: bind the UUID now.
      // Every login after that just refreshes IP/timestamp.
      await conn.execute(
        `UPDATE users
         SET player_uuid = ?, last_player_ip = ?, last_player_login = NOW()
         WHERE id = ?`,
        [normalizedUuid, ip, user.id]
      );

      await conn.execute(
        `INSERT INTO logins (user_name, player_uuid, action) VALUES (?, ?, 'login')`,
        [username, normalizedUuid]
      );
    });

    return res.json({ valid: true });
  })
);

/**
 * POST /api/logout   { username, uuid }
 * Not in the original plugin contract — call this from a quit/disconnect
 * listener once you add it, so login/logout pairs are in `logins` for the
 * later playtime calculation you mentioned.
 */
router.post(
  '/logout',
  asyncHandler(async (req, res) => {
    const { username, uuid } = req.body || {};
    const normalizedUuid = normalizeUuid(uuid);

    if (!isValidUsername(username) || !normalizedUuid) {
      return res.status(400).json({ ok: false, error: 'invalid_request' });
    }

    await query(
      `INSERT INTO logins (user_name, player_uuid, action) VALUES (?, ?, 'logout')`,
      [username, normalizedUuid]
    );

    return res.json({ ok: true });
  })
);

module.exports = router;
