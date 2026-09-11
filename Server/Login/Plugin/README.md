# LoginPlugin

Minecraft (Paper/Spigot) plugin that gates joining behind an external
whitelist + password database reachable over HTTP.

## How it works

1. **Before the connection is even accepted** (`AsyncPlayerPreLoginEvent`),
   the plugin calls your API to check if the username exists. If not,
   the player is kicked immediately - they never actually join the world.
2. **On join**, the player's current location and game mode are saved,
   they are teleported to a fixed "lock" location (default `0, 100, 0`),
   switched to Spectator mode, and frozen in place (they can still look
   around, but cannot fly/move away).
3. While locked, **all commands except `/login`** and all chat are blocked.
4. The player runs `/login <password>`. The plugin sends the
   username/password to your API. If valid, the player is teleported
   back to where they were, their original game mode is restored, and
   they play normally. If invalid, they get an attempt counter and are
   kicked after too many failures. They're also kicked if they never
   log in within the configured timeout.

## Required API contract

Every request includes an `X-API-Key` header (`api.key` in
`config.yml`) - the backend should reject anything without a matching
key, so the endpoints below can't be hit directly by someone who just
finds the URL.

Your central API needs to expose these endpoints (paths are
configurable in `config.yml`):

**Check whitelist** - `GET {base-url}{check-user-path}?username=<name>`
```json
{ "exists": false }
{ "exists": true, "allowed": true }
{ "exists": true, "allowed": false, "reason": "banned" }
```
`allowed` defaults to `true` if the field is missing, and `reason` is
optional - so a minimal backend that only ever returns `{"exists":
...}` still works.

**Login** - `POST {base-url}{login-path}`
```json
// request body
{ "username": "Steve", "password": "hunter2", "uuid": "<player uuid>", "ip": "<player's real IP>" }
```
```json
// response
{ "valid": true }
{ "valid": false, "reason": "invalid_credentials" }
{ "valid": false, "reason": "banned" }        // also "restricted", "unauthorised"
{ "valid": false, "reason": "uuid_mismatch" } // account already linked to a different Minecraft account
{ "valid": false, "reason": "not_found" }
```
The `uuid` is sent so the backend can bind the Minecraft account to
the database account on first login, and reject a correct password
coming from a *different* Minecraft account afterwards
(`uuid_mismatch`). The `ip` is the player's real address, taken from
`Player#getAddress()` on this end — the backend can't determine it
itself, since every HTTP call it receives comes from this server's
own network, not the player's machine. `invalid_credentials` (or any other/missing reason)
is treated as a normal wrong-password guess and counts toward
`login.max-attempts` and the brute-force guard; every other known
reason kicks immediately without touching those counters, since it's
an account-status problem, not a guessed password.

**Logout** - `POST {base-url}{logout-path}` (fire-and-forget, only
called for a session that actually logged in successfully)
```json
// request body
{ "username": "Steve", "uuid": "<player uuid>" }
```
```json
// response
{ "ok": true }
```

Endpoints return HTTP 200 for success. `login` may also return
401/403/404/409 alongside a `{"valid": false, "reason": ...}` body for
the outcomes above - those are still parsed normally, not treated as
transport errors. Any other status code, or a missing/unparseable
field, is treated as an error - the player is not let in / not
authenticated (fail-closed).

## Configuration

Edit `src/main/resources/config.yml` before building, or edit the
generated `config.yml` on the server after the first run (in
`plugins/LoginPlugin/config.yml`):

- `api.base-url`, `api.key`, `api.check-user-path`, `api.login-path`, `api.logout-path`
- `lock-location.world/x/y/z` - where players are held
- `login.timeout-seconds` - how long they have to log in
- `login.max-attempts` - wrong-password attempts before kick
- `messages.*` - all user-facing text, supports `&`-color codes,
  including per-reason messages (`login-banned`, `login-restricted`,
  `login-unauthorised`, `login-uuid-mismatch`, `login-account-not-found`)

## Building

Requires JDK 17+ and Maven, and internet access to pull the Paper API
from `repo.papermc.io` (the plugin has zero other dependencies).

```bash
mvn clean package
```

The compiled `login-plugin.jar` will be in `target/`. Drop it into
your server's `plugins/` folder and restart.

**Important:** the `paper-api` version in `pom.xml` is pinned to
`1.20.4-R0.1-SNAPSHOT`. Change it to match your server's actual
Minecraft version, and if you're running Spigot/Bukkit instead of
Paper, replace the `AsyncChatEvent` usage in `LockListener.java`
with the older `org.bukkit.event.player.AsyncPlayerChatEvent`
(Paper-only API is used there and in a couple of `Component`
conveniences elsewhere).

## Security notes

**Duplicate usernames / same name logging in twice.** The server core
already refuses a second connection under a name that's already online
(whether that first session is still locked or already playing). This
plugin adds an explicit check in `PreLoginListener` on top of that, as
a defense-in-depth measure for proxied setups (Velocity/BungeeCord)
where that guarantee can be weaker.

**Password brute-forcing.** Two layers:
- `login.max-attempts` kicks a player after N wrong passwords in a
  single session.
- `brute-force.*` (`BruteForceGuard`) tracks failures **per username,
  across reconnects** - so kicking and immediately rejoining does not
  reset the counter. Once the threshold is hit within the time window,
  further connection attempts for that username are rejected at
  `AsyncPlayerPreLoginEvent`, before even calling your API.
- `LoginCommand` also rejects a second `/login` while a previous one
  is still being verified, and enforces a short cooldown between
  attempts, so spamming the command can't fire a burst of concurrent
  requests at your API or race two verification results against each
  other.

**Passwords in server logs.** Bukkit/Paper logs every command a player
runs to the console and log files verbatim, including `/login
<password>`. This plugin never logs the password itself, but the
*server* will, unless you filter it out. The standard fix (used by
AuthMe-style plugins) is a Log4j2 filter in your server's
`log4j2.xml`:

```xml
<RegexFilter regex=".*issued server command: /login.*" onMatch="DENY" onMismatch="NEUTRAL"/>
```

Add it near the top of your Console/File appender filter chain. This
has to be done in the server's own logging config - a plugin cannot
reliably suppress this from inside itself.

**Unauthenticated API calls.** Every request now sends `api.key` as
an `X-API-Key` header. Set it in `config.yml` to the same secret your
backend expects, and make sure the backend actually rejects requests
without it - otherwise anyone who finds your API's URL could call
`login` directly and grind through passwords without ever going
through this plugin's brute-force guard.

**Transport security.** If `api.base-url` is `http://` (and not
localhost), the plugin logs a startup warning - usernames and
passwords travel in the clear over that connection. Use HTTPS in
production, and make sure your API hashes/salts passwords server-side;
this plugin only ever forwards the password once per attempt and
never stores it.

**Username enumeration.** The whitelist-check and password-check are
separate API calls by design, which means a rejected connection tells
an attacker whether a username is registered at all (different kick
messages for "not whitelisted" vs. a wrong password). This is a minor
information leak inherent to the "reject before connecting" behavior
you asked for; if it matters for your threat model, you could unify
the messaging, at the cost of a slightly worse UX for legitimate
players who mistype their name.

## Notes / things you may want to extend

- There's no bypass permission for staff - add a check like
  `if (player.hasPermission("loginplugin.bypass")) return;` at the
  top of `onJoin` if you want trusted accounts to skip the lock.
- Passwords are sent to your API as plain JSON over HTTP - **use HTTPS**
  for `api.base-url` in production, and make sure your API itself
  hashes/salts passwords server-side. This plugin never stores a
  password anywhere; it only forwards it once per attempt.
- The whitelist check and the password check are two separate calls
  by design - it lets your API return a generic "not found" for
  unregistered names without leaking whether a name almost matched
  during the password step.
