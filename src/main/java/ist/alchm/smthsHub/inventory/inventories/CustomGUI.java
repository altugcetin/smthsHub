package ist.alchm.smthsHub.inventory.inventories;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.inventory.AbstractInventory;
import ist.alchm.smthsHub.inventory.BedrockMenus;
import ist.alchm.smthsHub.inventory.InventoryBuilder;
import ist.alchm.smthsHub.inventory.InventoryItem;
import ist.alchm.smthsHub.utility.ItemStackBuilder;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.logging.Level;

public class CustomGUI extends AbstractInventory {

    private InventoryBuilder inventory;
    private final FileConfiguration config;

    public CustomGUI(SmthsHubPlugin plugin, FileConfiguration config) {
        super(plugin);
        this.config = config;
    }

    @Override
    public void onEnable() {

        InventoryBuilder inventoryBuilder = new InventoryBuilder(config.getInt("slots"), ColorUtil.color(config.getString("title")));

        if (config.contains("refresh") && config.getBoolean("refresh.enabled")) {
            setInventoryRefresh(config.getLong("refresh.rate"));
        }

        for (String entry : config.getConfigurationSection("items").getKeys(false)) {

            try {
                ItemStackBuilder builder = ItemStackBuilder.getItemStack(config.getConfigurationSection("items." + entry));

                InventoryItem inventoryItem;
                if (!config.contains("items." + entry + ".actions")) {
                    inventoryItem = new InventoryItem(builder.build());
                } else {
                    inventoryItem = new InventoryItem(builder.build()).addClickAction(p -> getPlugin().getActionManager().executeActions(p, config.getStringList("items." + entry + ".actions")));
                }

                if (config.contains("items." + entry + ".slots")) {
                    for (String slot : config.getStringList("items." + entry + ".slots")) {
                        inventoryBuilder.setItem(Integer.parseInt(slot), inventoryItem);
                    }
                } else if (config.contains("items." + entry + ".slot")) {
                    int slot = config.getInt("items." + entry + ".slot");
                    if (slot == -1) {
                        while (inventoryBuilder.getInventory().firstEmpty() != -1) {
                            inventoryBuilder.setItem(inventoryBuilder.getInventory().firstEmpty(), inventoryItem);
                        }
                    } else inventoryBuilder.setItem(slot, inventoryItem);
                }
            } catch (Exception e) {
                getPlugin().getLogger().log(Level.WARNING, "Failed to load GUI item ID '" + entry + "', skipping", e);
            }
        }

        inventory = inventoryBuilder;
    }

    @Override
    public void openInventory(Player player) {
        if (inventory != null && BedrockMenus.tryOpen(getPlugin(), player, config.getString("title"), inventory.getIcons())) {
            return;
        }
        super.openInventory(player);
    }

    @Override
    protected Inventory getInventory() {
        return inventory.getInventory();
    }
}
