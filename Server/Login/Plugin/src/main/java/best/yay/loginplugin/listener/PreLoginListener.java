package best.yay.loginplugin.listener;

import best.yay.loginplugin.LoginPlugin;
import best.yay.loginplugin.api.WhitelistCheckResult;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.concurrent.TimeUnit;

/**
 * Fires before the player's connection is even accepted. AsyncPlayerPreLoginEvent
 * already runs on its own thread pool (not the main server thread), so it is safe
 * to block here while we wait for the whitelist API to answer.
 */
public class PreLoginListener implements Listener {

    private final LoginPlugin plugin;

    public PreLoginListener(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();

        // Defense in depth: the server core already refuses a second connection
        // under a name that's already online, but we check explicitly too, in
        // case that protection is ever weakened by a proxy setup (Velocity/Bungee).
        if (plugin.getServer().getPlayerExact(username) != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    plugin.legacy("&cThis account is already connected to the server."));
            return;
        }

        // Persistent brute-force lockout - checked BEFORE the whitelist/API call
        // so a locked-out username can't even trigger another API round trip.
        long lockoutSeconds = plugin.getBruteForceGuard().getRemainingLockoutSeconds(username);
        if (lockoutSeconds > 0) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    plugin.legacy("&cToo many failed login attempts. Try again in " + lockoutSeconds + "s."));
            return;
        }

        int timeoutSeconds = plugin.getConfig().getInt("api.request-timeout-seconds", 5) + 2;
        WhitelistCheckResult result;
        try {
            result = plugin.getDatabaseAPI()
                    .checkUser(username)
                    .get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().warning("Whitelist check failed for '" + username + "': " + e.getMessage());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    plugin.legacy(plugin.getConfig().getString("messages.api-error")));
            return;
        }

        if (!result.exists()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    plugin.legacy(plugin.getConfig().getString("messages.not-whitelisted")));
            return;
        }

        // Account exists but is banned/restricted/unauthorised - kick now,
        // before the player even occupies a lock slot in-world.
        if (!result.allowed()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    plugin.legacy(plugin.messageForReason(result.reason())));
        }
    }
}
