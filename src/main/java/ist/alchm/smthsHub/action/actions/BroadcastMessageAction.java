package ist.alchm.smthsHub.action.actions;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastMessageAction implements Action {

    @Override
    public String getIdentifier() {
        return "BROADCAST";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ColorUtil.color(data));
        }
    }
}
