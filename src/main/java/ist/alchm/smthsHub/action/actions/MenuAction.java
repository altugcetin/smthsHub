package ist.alchm.smthsHub.action.actions;

import com.tcoded.folialib.impl.PlatformScheduler;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import org.bukkit.entity.Player;

public class MenuAction implements Action {

    private final PlatformScheduler scheduler = SmthsHubPlugin.scheduler();

    @Override
    public String getIdentifier() {
        return "MENU";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        plugin.getInventoryManager().getInventory(data).ifPresentOrElse(
                inventory -> scheduler.runAtEntity(player, task -> inventory.openInventory(player)),
                () -> plugin.getLogger().warning("[MENU] Action Failed: Menu '" + data + "' not found.")
        );
    }
}
