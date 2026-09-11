package best.yay.loginplugin.manager;

import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks every player currently pending authentication, and the scheduled
 * kick-on-timeout task associated with them. Thread-safe: PlayerLoginData
 * is written from the main thread but isLocked()/isPending() may be read
 * from other threads (e.g. inside async API callbacks before they hop back
 * to the main thread).
 */
public class LoginManager {

    private final Map<UUID, PlayerLoginData> pendingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    public void addPendingPlayer(UUID uuid, PlayerLoginData data) {
        pendingPlayers.put(uuid, data);
    }

    public PlayerLoginData getData(UUID uuid) {
        return pendingPlayers.get(uuid);
    }

    public boolean isLocked(UUID uuid) {
        PlayerLoginData data = pendingPlayers.get(uuid);
        return data != null && data.isLocked();
    }

    public boolean isPending(UUID uuid) {
        return pendingPlayers.containsKey(uuid);
    }

    public void removePlayer(UUID uuid) {
        pendingPlayers.remove(uuid);
        cancelTimeout(uuid);
    }

    public void setTimeoutTask(UUID uuid, BukkitTask task) {
        timeoutTasks.put(uuid, task);
    }

    public void cancelTimeout(UUID uuid) {
        BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}
