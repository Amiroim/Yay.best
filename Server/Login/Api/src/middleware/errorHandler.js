/** Wrap an async route handler so rejected promises reach the error handler. */
function asyncHandler(fn) {
  return (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);
}

function notFound(req, res) {
  res.status(404).json({ error: 'not_found' });
}

// Keep this LAST in the middleware chain (4 args = Express error handler).
function errorHandler(err, req, res, _next) {
  // Never leak raw DB/driver errors (could contain schema/query details) to callers.
  console.error('[error]', err);

  if (err.code === 'ER_DUP_ENTRY') {
    return res.status(409).json({ error: 'duplicate_entry' });
  }

  const status = err.status || 500;
  res.status(status).json({ error: status === 500 ? 'internal_error' : err.message });
}

module.exports = { asyncHandler, notFound, errorHandler };
