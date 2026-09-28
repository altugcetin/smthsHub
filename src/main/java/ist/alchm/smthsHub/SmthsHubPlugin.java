package ist.alchm.smthsHub;

import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import cl.bgmp.minecraft.util.commands.exceptions.CommandPermissionsException;
import cl.bgmp.minecraft.util.commands.exceptions.CommandUsageException;
import cl.bgmp.minecraft.util.commands.exceptions.MissingNestedCommandException;
import cl.bgmp.minecraft.util.commands.exceptions.WrappedCommandException;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import ist.alchm.smthsHub.action.ActionManager;
import ist.alchm.smthsHub.command.CommandManager;
import ist.alchm.smthsHub.config.ConfigManager;
import ist.alchm.smthsHub.config.ConfigType;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.cooldown.CooldownManager;
import ist.alchm.smthsHub.hook.HooksManager;
import ist.alchm.smthsHub.inventory.InventoryManager;
import ist.alchm.smthsHub.module.ModuleManager;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.module.modules.hologram.HologramManager;
import ist.alchm.smthsHub.utility.UpdateChecker;
import net.zithium.library.utils.ColorUtil;
import org.bstats.bukkit.MetricsLite;
import org.bukkit.command.CommandSender;

import java.io.File;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class SmthsHubPlugin extends JavaPlugin {

    private static PlatformScheduler scheduler;

    private static final int BSTATS_ID = 26336;

    public static PlatformScheduler scheduler() {
        return scheduler;
    }

    private ConfigManager configManager;
    private ActionManager actionManager;
    private HooksManager hooksManager;
    private CommandManager commandManager;
    private CooldownManager cooldownManager;
    private ModuleManager moduleManager;
    private InventoryManager inventoryManager;
    private File storageFolder;

    public File storageFolder() {
        if (storageFolder == null) {
            storageFolder = new File(getDataFolder().getParentFile(), "DeluxeHub");
        }
        return storageFolder;
    }

    @Override
    @SuppressWarnings("deprecation") // getDescription() is deprecated but there is no alternative yet.
    public void onEnable() {
        long start = System.currentTimeMillis();

        getLogger().info("smthsHub " + getDescription().getVersion());
        getLogger().info("Data folder: " + storageFolder().getAbsolutePath());
        getLogger().info("Author: ItzSave & ItsLewizzz");
        getLogger().info("");

        // Ensure we're running on Spigot
        if (!isSpigotEnvironment()) {
            getLogger().severe("============= SPIGOT NOT DETECTED =============");
            getLogger().severe("smthsHub requires Spigot or Paper to run.");
            getLogger().severe("Download it here: https://www.spigotmc.org/wiki/spigot-installation/");
            getLogger().severe("Plugin will now disable.");
            getLogger().severe("============= SPIGOT NOT DETECTED =============");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        MinecraftVersion.disableUpdateCheck();

        // Initialize Folia scheduling (if needed)
        FoliaLib foliaLib = new FoliaLib(this);
        scheduler = foliaLib.getScheduler();

        // Metrics
        new MetricsLite(this, BSTATS_ID);

        // Hooks and config
        hooksManager = new HooksManager(this);

        configManager = new ConfigManager();
        configManager.loadFiles(this);

        if (!getServer().getPluginManager().isPluginEnabled(this)) {
            return;
        }

        // Core managers
        commandManager = new CommandManager(this);
        commandManager.reload();

        cooldownManager = new CooldownManager();

        inventoryManager = new InventoryManager();
        inventoryManager.onEnable(this);

        moduleManager = new ModuleManager();
        moduleManager.loadModules(this);

        actionManager = new ActionManager(this);

        // Optional update check
        if (getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getBoolean("update-check")) {
            new UpdateChecker(this).checkForUpdate();
        }

        // BungeeCord channel registration
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getLogger().info("");
        getLogger().info("Successfully loaded in " + (System.currentTimeMillis() - start) + "ms");
    }

    private boolean isSpigotEnvironment() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isPurpurEnviroment() {
        try {
            Class.forName("org.purpurmc.purpur.PurpurConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void onDisable() {
        scheduler.cancelAllTasks();
        moduleManager.unloadModules();
        inventoryManager.onDisable(true);
        configManager.saveFiles();
    }

    public void reload() {
        scheduler.cancelAllTasks();
        HandlerList.unregisterAll(this);

        configManager.reloadFiles();

        inventoryManager.onDisable(false);
        inventoryManager.onEnable(this);

        getCommandManager().reload();

        moduleManager.loadModules(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String commandLabel, String[] args) {
        try {
            getCommandManager().execute(cmd.getName(), args, sender);
        } catch (CommandPermissionsException e) {
            Messages.NO_PERMISSION.send(sender);
        } catch (MissingNestedCommandException e) {
            sender.sendMessage(ColorUtil.color("<red>" + e.getUsage()));
        } catch (CommandUsageException e) {
            sender.sendMessage(ColorUtil.color("<red>" + "Usage: " + e.getUsage()));
        } catch (WrappedCommandException e) {
            if (e.getCause() instanceof NumberFormatException) {
                sender.sendMessage(ColorUtil.color("<red>Number expected, string received instead."));
            } else {
                sender.sendMessage(ColorUtil.color("<red>An internal error has occurred. See console."));
                getLogger().severe("An error occurred while executing command: " + e.getMessage());
                if (e.getCause() != null) {
                    getLogger().severe("Caused by: " + e.getCause().getMessage());
                }
            }
        } catch (CommandException e) {
            sender.sendMessage(ColorUtil.color("<red>" + e.getMessage()));
        }

        return true;
    }

    public HologramManager getHologramManager() {
        return (HologramManager) moduleManager.getModule(ModuleType.HOLOGRAMS);
    }

    public HooksManager getHookManager() {
        return hooksManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }
}
