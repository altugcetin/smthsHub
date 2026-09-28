package ist.alchm.smthsHub.action;

import ist.alchm.smthsHub.SmthsHubPlugin;
import org.bukkit.entity.Player;

public interface Action {

    String getIdentifier();

    void execute(SmthsHubPlugin plugin, Player player, String data);
}
