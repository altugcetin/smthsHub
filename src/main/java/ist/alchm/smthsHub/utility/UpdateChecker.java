package ist.alchm.smthsHub.utility;

import com.tcoded.folialib.impl.PlatformScheduler;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.Permissions;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

/*
   Credits: Benz56
   https://www.spigotmc.org/threads/async-update-checker-for-premium-and-regular-plugins.327921/
 */
public class UpdateChecker {

    private final JavaPlugin plugin;
    private final PlatformScheduler scheduler;
    private final String localPluginVersion;
    private String spigotPluginVersion;

    private static final int ID = 49425;
    private static final Permission UPDATE_PERM = new Permission(Permissions.UPDATE_NOTIFICATION.getPermission(), PermissionDefault.TRUE);
    private static final long CHECK_INTERVAL = 12_000;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = SmthsHubPlugin.scheduler();
        this.localPluginVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdate() {
        scheduler.runTimerAsync(task -> {
            try {
                final URI uri = new URI("https://api.spigotmc.org/legacy/update.php?resource=" + ID);
                final HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                spigotPluginVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
            } catch (IOException | URISyntaxException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates from Spigot API", e);
                task.cancel();
                return;
            }

            if (localPluginVersion.equals(spigotPluginVersion)) {
                return;
            }

            plugin.getLogger().info("An update for DeluxeHub (v%VERSION%) is available at:".replace("%VERSION%", spigotPluginVersion));
            plugin.getLogger().info("https://www.spigotmc.org/resources/" + ID);

            scheduler.runNextTick(nextTask -> Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.MONITOR)
                public void onPlayerJoin(final PlayerJoinEvent event) {
                    final Player player = event.getPlayer();
                    if (!player.hasPermission(UPDATE_PERM)) {
                        return;
                    }

                    player.sendMessage(ColorUtil.color("&7An update (v%VERSION%) for DeluxeHub is available at:".replace("%VERSION%", spigotPluginVersion)));
                    player.sendMessage(ColorUtil.color("&6https://www.spigotmc.org/resources/" + ID));
                }
            }, plugin));

            task.cancel();
        }, 1, CHECK_INTERVAL);
    }
}
