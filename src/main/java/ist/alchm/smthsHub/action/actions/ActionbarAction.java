package ist.alchm.smthsHub.action.actions;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import ist.alchm.smthsHub.utility.reflection.ActionBar;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.entity.Player;

public class ActionbarAction implements Action {

    @Override
    public String getIdentifier() {
        return "ACTIONBAR";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        ActionBar.sendActionBar(player, ColorUtil.color(data));
    }
}
