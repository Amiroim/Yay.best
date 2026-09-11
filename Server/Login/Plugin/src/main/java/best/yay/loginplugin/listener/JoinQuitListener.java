package best.yay.loginplugin.listener;

import best.yay.loginplugin.LoginPlugin;
import best.yay.loginplugin.manager.PlayerLoginData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class JoinQuitListener implements Listener {

    private final LoginPlugin plugin;

    public JoinQuitListener(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // By the time we get here the player already passed the whitelist
        // check in AsyncPlayerPreLoginEvent - we just need to lock them down
        // until they authenticate with /login.

        // Defensive cleanup: in the extremely unlikely case a previous
        // session for this UUID never got cleaned up (e.g. an abnormal
        // disconnect the QuitEvent didn't catch), don't let it leak into
        // this new session's state.
        if (plugin.getLoginManager().isPending(player.getUniqueId())) {
            plugin.getLoginManager().removePlayer(player.getUniqueId());
        }

        Location originalLocation = player.getLocation().clone();
        GameMode originalGameMode = player.getGameMode();

        PlayerLoginData data = new PlayerLoginData(originalLocation, originalGameMode);
        plugin.getLoginManager().addPendingPlayer(player.getUniqueId(), data);

        Location lockLocation = plugin.getLockLocation();
        World lockWorld = plugin.getLockWorld();

        if (lockWorld == null || lockLocation == null) {
            plugin.getLogger().severe("Lock location world '" +
                    plugin.getConfig().getString("lock-location.world") +
                    "' is not loaded! Check config.yml. Player will be locked in place instead.");
        } else {
            player.teleport(lockLocation);
        }

        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(plugin.legacy(plugin.getConfig().getString("messages.please-login")));

        int timeoutSeconds = plugin.getConfig().getInt("login.timeout-seconds", 60);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getLoginManager().isLocked(player.getUniqueId())) {
                player.kick(plugin.legacy(plugin.getConfig().getString("messages.login-timeout")));
            }
        }, timeoutSeconds * 20L);

        plugin.getLoginManager().setTimeoutTask(player.getUniqueId(), task);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // A player is still "pending" if they never got past /login this
        // session (still locked, or timed out/kicked before authenticating).
        // Only log a logout for someone who actually had a matching login -
        // otherwise the logins table would end up with logout rows that have
        // no login to pair with, and playtime math built on top of it later
        // would be wrong.
        boolean neverAuthenticated = plugin.getLoginManager().isPending(player.getUniqueId());

        // Cleans up pending state AND cancels the timeout task,
        // so a player who disconnects while locked doesn't leak a scheduled task.
        plugin.getLoginManager().removePlayer(player.getUniqueId());

        if (!neverAuthenticated) {
            plugin.getDatabaseAPI().logout(player.getName(), player.getUniqueId())
                    .exceptionally(ex -> {
                        plugin.getLogger().warning("Logout log failed for '" + player.getName() + "': " + ex.getMessage());
                        return null;
                    });
        }
    }
}
