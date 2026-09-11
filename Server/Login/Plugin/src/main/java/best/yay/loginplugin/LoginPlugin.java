package best.yay.loginplugin;

import best.yay.loginplugin.api.DatabaseAPI;
import best.yay.loginplugin.command.LoginCommand;
import best.yay.loginplugin.listener.JoinQuitListener;
import best.yay.loginplugin.listener.LockListener;
import best.yay.loginplugin.listener.PreLoginListener;
import best.yay.loginplugin.manager.BruteForceGuard;
import best.yay.loginplugin.manager.LoginManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LoginPlugin extends JavaPlugin {

    private LoginManager loginManager;
    private DatabaseAPI databaseAPI;
    private BruteForceGuard bruteForceGuard;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.loginManager = new LoginManager();
        this.databaseAPI = new DatabaseAPI(getConfig(), getLogger());
        this.bruteForceGuard = new BruteForceGuard(
                getConfig().getLong("brute-force.window-seconds", 300),
                getConfig().getInt("brute-force.max-failures-in-window", 5),
                getConfig().getLong("brute-force.lockout-seconds", 300)
        );

        warnIfInsecureApiUrl();

        getServer().getPluginManager().registerEvents(new PreLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new LockListener(this), this);

        PluginCommand loginCommand = getCommand("login");
        if (loginCommand != null) {
            loginCommand.setExecutor(new LoginCommand(this));
        } else {
            getLogger().severe("Could not register /login - check that plugin.yml defines it correctly.");
        }

        getLogger().info("LoginPlugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LoginPlugin disabled.");
    }

    private void warnIfInsecureApiUrl() {
        String baseUrl = getConfig().getString("api.base-url", "");
        boolean isLocal = baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1");
        if (baseUrl.startsWith("http://") && !isLocal) {
            getLogger().warning("api.base-url is using plain HTTP - usernames and passwords will "
                    + "travel over the network unencrypted. Switch to HTTPS before going to production.");
        }
    }

    public LoginManager getLoginManager() {
        return loginManager;
    }

    public DatabaseAPI getDatabaseAPI() {
        return databaseAPI;
    }

    public BruteForceGuard getBruteForceGuard() {
        return bruteForceGuard;
    }

    public World getLockWorld() {
        String worldName = getConfig().getString("lock-location.world", "world");
        return getServer().getWorld(worldName);
    }

    public Location getLockLocation() {
        World world = getLockWorld();
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                getConfig().getDouble("lock-location.x", 0.5),
                getConfig().getDouble("lock-location.y", 100.0),
                getConfig().getDouble("lock-location.z", 0.5),
                (float) getConfig().getDouble("lock-location.yaw", 0.0),
                (float) getConfig().getDouble("lock-location.pitch", 0.0)
        );
    }

    /** Converts a "&"-coded message from config.yml into an Adventure Component. */
    public Component legacy(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message == null ? "" : message);
    }

    /**
     * Maps a "reason" string coming back from the backend (checkUser's or
     * login's JSON body) to the matching message key in config.yml. Falls
     * back to messages.api-error for anything unrecognized, so a new reason
     * added on the backend later never surfaces a blank/missing message.
     */
    public String messageForReason(String reason) {
        String key = switch (reason == null ? "" : reason.toLowerCase()) {
            case "banned" -> "messages.login-banned";
            case "restricted" -> "messages.login-restricted";
            case "unauthorised", "unauthorized" -> "messages.login-unauthorised";
            case "uuid_mismatch" -> "messages.login-uuid-mismatch";
            case "not_found" -> "messages.login-account-not-found";
            default -> "messages.api-error";
        };
        return getConfig().getString(key, getConfig().getString("messages.api-error"));
    }
}
