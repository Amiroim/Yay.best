package best.yay.loginplugin.listener;

import best.yay.loginplugin.LoginPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Locale;
import java.util.UUID;

/**
 * While a player is locked (pending authentication) this listener:
 *  - cancels positional movement, but still lets them look around (rotate camera)
 *  - blocks every command except /login
 *  - blocks chat
 *
 * Spectator mode already prevents block breaking/placing, item interaction,
 * combat and hunger, so we don't need to duplicate that here - we only need
 * to handle the things Spectator mode does NOT restrict on its own: free flight.
 */
public class LockListener implements Listener {

    private final LoginPlugin plugin;

    public LockListener(LoginPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getLoginManager().isLocked(uuid)) {
            return;
        }

        if (event.getTo() == null || !positionChanged(event)) {
            return;
        }

        // Keep the player's exact position from "from", but preserve the
        // look direction from "to" so they can still rotate the camera freely.
        event.setTo(event.getFrom().clone().setDirection(event.getTo().getDirection()));
    }

    private boolean positionChanged(PlayerMoveEvent event) {
        return event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getLoginManager().isLocked(player.getUniqueId())) {
            return;
        }

        String command = event.getMessage().toLowerCase(Locale.ROOT).trim();
        if (!command.startsWith("/login")) {
            event.setCancelled(true);
            player.sendMessage(plugin.legacy(plugin.getConfig().getString("messages.locked-cannot-move")));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        if (plugin.getLoginManager().isLocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.legacy(plugin.getConfig().getString("messages.locked-cannot-move")));
        }
    }
}
