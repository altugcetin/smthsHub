package ist.alchm.smthsHub.action.actions;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.entity.Player;

public class MessageAction implements Action {

    @Override
    public String getIdentifier() {
        return "MESSAGE";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        player.sendMessage(ColorUtil.color(data));
    }
}
