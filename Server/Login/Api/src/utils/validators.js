// Minecraft usernames: 3-16 chars, letters/digits/underscore.
const USERNAME_RE = /^[a-zA-Z0-9_]{3,16}$/;

// Accept both dashed and undashed UUIDs, normalize to dashed lowercase.
const UUID_RE = /^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$/;

// Good enough to catch typos/garbage without pulling in a full IP library —
// not meant to be a strict RFC validator.
const IPV4_RE = /^(\d{1,3}\.){3}\d{1,3}$/;
const IPV6_RE = /^[0-9a-fA-F:]+:[0-9a-fA-F:]*$/;

const BLOCKED_STATUSES = new Set(['BANNED', 'RESTRICTED', 'UNAUTHORISED', 'UNAUTHORIZED']);

function isValidUsername(username) {
  return typeof username === 'string' && USERNAME_RE.test(username);
}

function isValidPassword(password) {
  return typeof password === 'string' && password.length > 0 && password.length <= 256;
}

function normalizeUuid(uuid) {
  if (typeof uuid !== 'string' || !UUID_RE.test(uuid)) return null;
  const hex = uuid.replace(/-/g, '').toLowerCase();
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function isBlockedStatus(status) {
  return typeof status === 'string' && BLOCKED_STATUSES.has(status.toUpperCase());
}

function isValidIp(ip) {
  if (typeof ip !== 'string' || ip.length === 0 || ip.length > 45) return false;
  return IPV4_RE.test(ip) || IPV6_RE.test(ip);
}

module.exports = {
  isValidUsername,
  isValidPassword,
  normalizeUuid,
  isBlockedStatus,
  isValidIp,
};
