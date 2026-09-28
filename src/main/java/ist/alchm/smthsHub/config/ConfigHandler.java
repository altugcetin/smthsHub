package ist.alchm.smthsHub.config;

import ist.alchm.smthsHub.SmthsHubPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

public class ConfigHandler {

    private final SmthsHubPlugin plugin;
    private final String name;
    private final File file;
    private FileConfiguration configuration;

    public ConfigHandler(SmthsHubPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name + ".yml";
        this.file = new File(plugin.storageFolder(), this.name);
        this.configuration = new YamlConfiguration();
    }

    public void saveDefaultConfig() {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (InputStream input = plugin.getResource(name)) {
                if (input == null) {
                    plugin.getLogger().severe("Missing default resource " + name);
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    return;
                }
                Files.copy(input, file.toPath());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save default configuration file: " + name, e);
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }
        }

        try {
            configuration.load(file);
        } catch (InvalidConfigurationException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration file: " + name, e);
            plugin.getLogger().severe("============= CONFIGURATION ERROR =============");
            plugin.getLogger().severe("There was an error loading " + name);
            plugin.getLogger().severe("Please check for any obvious configuration mistakes");
            plugin.getLogger().severe("such as using tabs for spaces or forgetting to end quotes");
            plugin.getLogger().severe("before reporting to the developer. The plugin will now disable..");
            plugin.getLogger().severe("============= CONFIGURATION ERROR =============");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public void save() {
        if (configuration == null || file == null) {
            return;
        }

        try {
            getConfig().save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration file: " + name, e);
        }
    }

    public void reload() {
        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return configuration;
    }
}
