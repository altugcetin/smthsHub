package ist.alchm.smthsHub.action.actions;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import org.bukkit.entity.Player;

public class CommandAction implements Action {

    @Override
    public String getIdentifier() {
        return "COMMAND";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        player.chat(data.contains("/") ? data : "/" + data);
    }
}
