package best.yay.loginplugin.command;

import best.yay.loginplugin.LoginPlugin;
import best.yay.loginplugin.api.LoginResult;
import best.yay.loginplugin.manager.PlayerLoginData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class LoginCommand implements CommandExecutor {

    /** Minimum time between attempts for the same player - slows down manual/scripted brute forcing. */
    private static final long MIN_ATTEMPT_INTERVAL_MILLIS = 1500L;

    /**
     * Reasons that mean "this account is currently blocked", not "wrong
     * password". These end the session immediately and do NOT count against
     * the per-session wrong-password attempt counter or the brute-force
     * guard, since the player didn't necessarily do anything wrong - guessing
     * a password has nothing to do with being banned or UUID-locked.
     */
    private static final Set<String> IMMEDIATE_KICK_REASONS =
            Set.of("banned", "restricted", "unauthorised", "unauthorized", "uuid_mismatch", "not_found");

    private final LoginPlugin plugin;

    public LoginCommand(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        PlayerLoginData data = plugin.getLoginManager().getData(uuid);

        if (data == null || !plugin.getLoginManager().isLocked(uuid)) {
            player.sendMessage(plugin.legacy(plugin.getConfig().getString("messages.already-logged-in")));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.legacy("&eUsage: /login <password>"));
            return true;
        }

        // Reject a second attempt while one is already in flight - stops a
        // client (or a script) from firing a burst of concurrent verification
        // calls, which previously could race and process a stale result after
        // the player had already logged in via an earlier attempt.
        if (!data.tryStartVerifying()) {
            player.sendMessage(plugin.legacy("&eYour previous attempt is still being checked, please wait."));
            return true;
        }

        long now = System.currentTimeMillis();
        if (now - data.getLastAttemptMillis() < MIN_ATTEMPT_INTERVAL_MILLIS) {
            data.finishVerifying();
            player.sendMessage(plugin.legacy("&eYou're trying too fast - wait a moment and try again."));
            return true;
        }
        data.setLastAttemptMillis(now);

        String password = args[0];
        String username = player.getName();

        // The player's real address, taken straight off their live game
        // connection - this is the only place that IP is actually known.
        // Every HTTP call to the backend comes from this server's own
        // network, so if we didn't send it explicitly the backend would
        // have no way to tell the player's IP from the server's IP.
        String playerIp = player.getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : null;

        if (playerIp == null) {
            data.finishVerifying();
            player.sendMessage(plugin.legacy(plugin.getConfig().getString("messages.api-error")));
            plugin.getLogger().warning("Could not resolve address for '" + username + "', skipping login attempt.");
            return true;
        }

        plugin.getDatabaseAPI().login(username, password, uuid, playerIp)
                .thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin,
                        () -> handleResult(uuid, username, data, result)))
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Login verification errored for '" + username + "': " + ex.getMessage());
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        data.finishVerifying();
                        Player online = plugin.getServer().getPlayer(uuid);
                        if (online != null && online.isOnline()) {
                            online.sendMessage(plugin.legacy(plugin.getConfig().getString("messages.api-error")));
                        }
                    });
                    return null;
                });

        return true;
    }

    private void handleResult(UUID uuid, String username, PlayerLoginData data, LoginResult result) {
        data.finishVerifying();

        // The result may arrive after the player already logged in via a
        // different in-flight attempt, disconnected, or was otherwise removed
        // from the pending map - never act on a stale result. This is what
        // stops a slow "wrong password" response from re-locking or kicking
        // a player who already got in via a faster, correct attempt.
        if (!data.isLocked() || !plugin.getLoginManager().isPending(uuid)) {
            return;
        }

        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        if (result.valid()) {
            data.setLocked(false);
            plugin.getLoginManager().removePlayer(uuid);
            plugin.getBruteForceGuard().clear(username);

            player.teleport(data.getOriginalLocation());
            player.setGameMode(data.getOriginalGameMode());
            player.sendMessage(plugin.legacy(plugin.getConfig().getString("messages.login-success")));
            return;
        }

        String reason = result.reason() == null ? "" : result.reason().toLowerCase();

        if (IMMEDIATE_KICK_REASONS.contains(reason)) {
            // Account-status problem, not a guessed password - kick right
            // away and don't touch the wrong-password attempt counters.
            plugin.getLoginManager().removePlayer(uuid);
            player.kick(plugin.legacy(plugin.messageForReason(reason)));
            return;
        }

        if (reason.isEmpty()) {
            // Transport/server error (ERROR result) rather than a definite
            // "wrong password" - let them retry without burning an attempt.
            player.sendMessage(plugin.legacy(plugin.getConfig().getString("messages.api-error")));
            return;
        }

        // "invalid_credentials" (or any other unrecognized-but-present reason):
        // treat as a wrong-password guess and apply the normal attempt/lockout flow.
        plugin.getBruteForceGuard().recordFailure(username);

        int attempts = data.incrementAndGetAttempts();
        int maxAttempts = plugin.getConfig().getInt("login.max-attempts", 3);

        if (attempts >= maxAttempts) {
            plugin.getLoginManager().removePlayer(uuid);
            player.kick(plugin.legacy(plugin.getConfig().getString("messages.login-max-attempts")));
        } else {
            String message = plugin.getConfig()
                    .getString("messages.login-failed", "&cIncorrect password. Attempts remaining: %attempts%")
                    .replace("%attempts%", String.valueOf(maxAttempts - attempts));
            player.sendMessage(plugin.legacy(message));
        }
    }
}
