package ist.alchm.smthsHub.utility.reflection;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;

public class ActionBar {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public static void sendActionBar(Player player, String message) {
        Objects.requireNonNull(player, "Cannot send action bar to null player");
        player.sendActionBar(LEGACY.deserialize(message));
    }

    public static void sendAllActionBar(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendActionBar(player, message);
        }
    }
}
