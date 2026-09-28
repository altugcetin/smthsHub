package ist.alchm.smthsHub.config;

import ist.alchm.smthsHub.SmthsHubPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Map<ConfigType, ConfigHandler> configurations;
    private SmthsHubPlugin plugin;

    public ConfigManager() {
        configurations = new HashMap<>();
    }

    public void loadFiles(SmthsHubPlugin plugin) {
        this.plugin = plugin;

        registerFile(ConfigType.SETTINGS, new ConfigHandler(plugin, "config"));
        registerFile(ConfigType.MESSAGES, new ConfigHandler(plugin, "messages"));
        registerFile(ConfigType.DATA, new ConfigHandler(plugin, "data"));
        registerFile(ConfigType.COMMANDS, new ConfigHandler(plugin, "commands"));

        configurations.values().forEach(ConfigHandler::saveDefaultConfig);

        bindMessages();
    }

    public ConfigHandler getFile(ConfigType type) {
        return configurations.get(type);
    }

    public void reloadFiles() {
        configurations.values().forEach(ConfigHandler::reload);
        bindMessages();
    }

    private void bindMessages() {
        FileConfiguration config = getFile(ConfigType.MESSAGES).getConfig();
        InputStream in = plugin.getResource("messages.yml");
        if (in != null) {
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                config.setDefaults(YamlConfiguration.loadConfiguration(reader));
            } catch (IOException ignored) {
            }
        }
        Messages.setConfiguration(config);
    }

    /**
     * Saves the "ConfigType.DATA" file
     */
    public void saveFiles() {
        getFile(ConfigType.DATA).save();
    }

    public void registerFile(ConfigType type, ConfigHandler config) {
        configurations.put(type, config);
    }

    public FileConfiguration getFileConfiguration(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }
}
