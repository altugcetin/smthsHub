package ist.alchm.smthsHub.action.actions;

import com.tcoded.folialib.impl.PlatformScheduler;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleCommandAction implements Action {

    private final PlatformScheduler scheduler = SmthsHubPlugin.scheduler();

    @Override
    public String getIdentifier() {
        return "CONSOLE";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        scheduler.runNextTick(task -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), data));
    }
}
