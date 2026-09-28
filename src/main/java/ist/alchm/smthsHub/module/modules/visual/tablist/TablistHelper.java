package ist.alchm.smthsHub.module.modules.visual.tablist;

import com.google.common.base.Strings;
import ist.alchm.smthsHub.SmthsHubPlugin;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class TablistHelper {

    private static final JavaPlugin PLUGIN = JavaPlugin.getProvidingPlugin(SmthsHubPlugin.class);

    public static void sendTabList(Player player, String header, String footer) {

        Objects.requireNonNull(player, "Cannot update tab for null player");
        header = Strings.isNullOrEmpty(header) ?
                "" : ColorUtil.color(header).replace("%player%", player.getDisplayName());
        footer = Strings.isNullOrEmpty(footer) ?
                "" : ColorUtil.color(footer).replace("%player%", player.getDisplayName());

        player.setPlayerListHeaderFooter(header, footer);
    }
}
