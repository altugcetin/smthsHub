package ist.alchm.smthsHub.module.modules.player;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerOffHandSwap extends Module {

    public PlayerOffHandSwap(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.PLAYER_OFFHAND_LISTENER);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onPlayerSwapItem(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        World eventWorld = event.getWhoClicked().getWorld();

        if (inDisabledWorld(eventWorld)) {
            return;
        }

        if (event.getRawSlot() != event.getSlot() && event.getSlot() == 40) {
            event.setCancelled(true);
        }
    }
}
