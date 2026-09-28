package ist.alchm.smthsHub.inventory;

import com.tcoded.folialib.impl.PlatformScheduler;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.inventory.inventories.CustomGUI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class InventoryManager {

    private SmthsHubPlugin plugin;
    private PlatformScheduler scheduler;
    private final Map<String, AbstractInventory> inventories;

    public InventoryManager() {
        inventories = new ConcurrentHashMap<>();
    }

    public void onEnable(SmthsHubPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = SmthsHubPlugin.scheduler();
        loadCustomMenus();
        plugin.getServer().getPluginManager().registerEvents(new InventoryListener(), plugin);
    }

    private void loadCustomMenus() {
        File directory = new File(plugin.storageFolder(), "menus");
        createMenusDirectoryIfNeeded(directory);

        // Load all menu files
        File[] yamlFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (yamlFiles == null) {
            return;
        }

        for (File file : yamlFiles) {
            String name = file.getName().replace(".yml", "");
            if (inventories.containsKey(name)) {
                plugin.getLogger().warning("Inventory with name '" + file.getName() + "' already exists, skipping duplicate..");
                continue;
            }

            try {
                CustomGUI customGUI = new CustomGUI(plugin, YamlConfiguration.loadConfiguration(file));
                inventories.put(name, customGUI);
                customGUI.onEnable();
                plugin.getLogger().info("Loaded custom menu '" + name + "'.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not load file '" + name + "' (YAML error)", e);
            }
        }
    }

    private void createMenusDirectoryIfNeeded(File directory) {
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                plugin.getLogger().warning("Failed to create menus directory at: " + directory.getPath());
                return;
            }

            File file = new File(directory, "serverselector.yml");
            if (!file.exists()) {
                try (InputStream inputStream = this.plugin.getResource("serverselector.yml")) {
                    if (inputStream != null) {
                        Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().info("Created default serverselector.yml menu.");
                    } else {
                        plugin.getLogger().warning("Resource 'serverselector.yml' not found.");
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to copy serverselector.yml to menus directory", e);
                }
            }
        }
    }

    public void addInventory(String key, AbstractInventory inventory) {
        inventories.put(key, inventory);
    }

    public Map<String, AbstractInventory> getInventories() {
        return inventories;
    }

    public Optional<AbstractInventory> getInventory(String key) {
        return Optional.ofNullable(inventories.get(key));
    }

    public void onDisable() {
        inventories.values().forEach(abstractInventory -> {
            abstractInventory.getOpenInventories().forEach(uuid -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    scheduler.runAtEntity(player, task -> player.closeInventory());
                }
            });

            abstractInventory.getOpenInventories().clear();
        });

        inventories.clear();
    }
}
