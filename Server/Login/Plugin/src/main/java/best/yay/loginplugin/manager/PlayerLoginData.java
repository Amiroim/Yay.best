package best.yay.loginplugin.manager;

import org.bukkit.GameMode;
import org.bukkit.Location;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Snapshot of a player's state at join-time, plus their live login progress.
 * One instance exists per pending player, from PlayerJoinEvent until they
 * either authenticate successfully or get kicked/disconnect.
 *
 * All mutation happens on the main server thread (command execution and the
 * scheduler callbacks that process API responses both run there), so plain
 * fields are safe. The one exception is `verifying`, kept atomic defensively
 * in case that assumption ever changes.
 */
public class PlayerLoginData {

    private final Location originalLocation;
    private final GameMode originalGameMode;
    private volatile boolean locked;
    private int attempts;
    private long lastAttemptMillis;
    private final AtomicBoolean verifying = new AtomicBoolean(false);

    public PlayerLoginData(Location originalLocation, GameMode originalGameMode) {
        this.originalLocation = originalLocation;
        this.originalGameMode = originalGameMode;
        this.locked = true;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public GameMode getOriginalGameMode() {
        return originalGameMode;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int incrementAndGetAttempts() {
        return ++attempts;
    }

    /** Attempts to claim the "verifying" state; returns false if a verification is already in flight. */
    public boolean tryStartVerifying() {
        return verifying.compareAndSet(false, true);
    }

    /** Releases the "verifying" state - must be called exactly once after tryStartVerifying() succeeded. */
    public void finishVerifying() {
        verifying.set(false);
    }

    public long getLastAttemptMillis() {
        return lastAttemptMillis;
    }

    public void setLastAttemptMillis(long millis) {
        this.lastAttemptMillis = millis;
    }
}
