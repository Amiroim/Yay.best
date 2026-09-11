package best.yay.loginplugin.manager;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed password attempts per-username, independent of any single
 * connection/session. Without this, a player who gets kicked for hitting the
 * per-session attempt cap could just reconnect instantly and get a fresh set
 * of attempts, indefinitely - this closes that hole by remembering failures
 * across a sliding time window and issuing a temporary lockout once a
 * threshold is crossed. State lives in memory only (cleared on restart),
 * which is fine for this purpose since it exists to slow down live brute-force
 * traffic, not to be a permanent ban list.
 */
public class BruteForceGuard {

    private static final class Entry {
        volatile int failures;
        volatile long windowStartMillis;
        volatile long lockedUntilMillis;
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final long windowMillis;
    private final int maxFailuresInWindow;
    private final long lockoutMillis;

    public BruteForceGuard(long windowSeconds, int maxFailuresInWindow, long lockoutSeconds) {
        this.windowMillis = windowSeconds * 1000L;
        this.maxFailuresInWindow = maxFailuresInWindow;
        this.lockoutMillis = lockoutSeconds * 1000L;
    }

    private String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    /** Seconds remaining in an active lockout for this username, or 0 if not locked out. */
    public long getRemainingLockoutSeconds(String username) {
        Entry entry = entries.get(key(username));
        if (entry == null) {
            return 0;
        }
        long remaining = entry.lockedUntilMillis - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 1000L) + 1 : 0;
    }

    /** Records one failed attempt; starts a lockout if the threshold is crossed within the window. */
    public void recordFailure(String username) {
        Entry entry = entries.computeIfAbsent(key(username), k -> new Entry());
        synchronized (entry) {
            long now = System.currentTimeMillis();
            if (now - entry.windowStartMillis > windowMillis) {
                entry.windowStartMillis = now;
                entry.failures = 0;
            }
            entry.failures++;
            if (entry.failures >= maxFailuresInWindow) {
                entry.lockedUntilMillis = now + lockoutMillis;
                entry.failures = 0;
                entry.windowStartMillis = now;
            }
        }
    }

    /** Clears any failure history for this username - call this on a successful login. */
    public void clear(String username) {
        entries.remove(key(username));
    }
}
