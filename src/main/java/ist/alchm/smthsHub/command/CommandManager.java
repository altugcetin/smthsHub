package ist.alchm.smthsHub.command;

import cl.bgmp.bukkit.util.BukkitCommandsManager;
import cl.bgmp.bukkit.util.CommandsManagerRegistration;
import cl.bgmp.minecraft.util.commands.CommandsManager;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import cl.bgmp.minecraft.util.commands.injection.SimpleInjector;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.command.commands.ClearchatCommand;
import ist.alchm.smthsHub.command.commands.DeluxeHubCommand;
import ist.alchm.smthsHub.command.commands.FlyCommand;
import ist.alchm.smthsHub.command.commands.LobbyCommand;
import ist.alchm.smthsHub.command.commands.LockchatCommand;
import ist.alchm.smthsHub.command.commands.SetLobbyCommand;
import ist.alchm.smthsHub.command.commands.VanishCommand;
import ist.alchm.smthsHub.command.commands.gamemode.AdventureCommand;
import ist.alchm.smthsHub.command.commands.gamemode.CreativeCommand;
import ist.alchm.smthsHub.command.commands.gamemode.GamemodeCommand;
import ist.alchm.smthsHub.command.commands.gamemode.SpectatorCommand;
import ist.alchm.smthsHub.command.commands.gamemode.SurvivalCommand;
import ist.alchm.smthsHub.config.ConfigType;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    private final SmthsHubPlugin plugin;
    private final FileConfiguration config;

    private CommandsManager commands;
    private CommandsManagerRegistration commandRegistry;

    private final List<CustomCommand> customCommands;

    public CommandManager(SmthsHubPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getFile(ConfigType.COMMANDS).getConfig();
        this.customCommands = new ArrayList<>();
    }

    public void reload() {
        if (commandRegistry != null) {
            commandRegistry.unregisterCommands();
        }

        commands = new BukkitCommandsManager();
        commandRegistry = new CommandsManagerRegistration(plugin, commands);
        commands.setInjector(new SimpleInjector(plugin));

        commandRegistry.register(DeluxeHubCommand.class);

        for (String command : config.getConfigurationSection("commands").getKeys(false)) {
            if (!config.getBoolean("commands." + command + ".enabled")) continue;

            registerCommand(command, config.getStringList("commands." + command + ".aliases").toArray(new String[0]));
        }

        reloadCustomCommands();
    }

    public void execute(String cmd, String[] args, CommandSender sender) throws CommandException {
        commands.execute(cmd, args, sender, sender);
    }

    public void reloadCustomCommands() {
        if (!customCommands.isEmpty()) {
            customCommands.clear();
        }

        if (!config.isSet("custom_commands")) {
            return;
        }

        for (String entry : config.getConfigurationSection("custom_commands").getKeys(false)) {

            CustomCommand customCommand = new CustomCommand(entry, config.getStringList("custom_commands." + entry + ".actions"));

            if (config.contains("custom_commands." + entry + ".aliases")) {
                customCommand.addAliases(config.getStringList("custom_commands." + entry + ".aliases"));
            }

            if (config.contains("custom_commands." + entry + ".permission")) {
                customCommand.setPermission(config.getString("custom_commands." + entry + ".permission"));
            }

            customCommands.add(customCommand);
        }
    }

    private void registerCommand(String cmd, String[] aliases) {
        switch (cmd.toUpperCase()) {
            case "GAMEMODE" -> commandRegistry.register(GamemodeCommand.class, aliases);
            case "GMS" -> commandRegistry.register(SurvivalCommand.class, aliases);
            case "GMC" -> commandRegistry.register(CreativeCommand.class, aliases);
            case "GMA" -> commandRegistry.register(AdventureCommand.class, aliases);
            case "GMSP" -> commandRegistry.register(SpectatorCommand.class, aliases);
            case "CLEARCHAT" -> commandRegistry.register(ClearchatCommand.class, aliases);
            case "FLY" -> commandRegistry.register(FlyCommand.class, aliases);
            case "LOCKCHAT" -> commandRegistry.register(LockchatCommand.class, aliases);
            case "SETLOBBY" -> commandRegistry.register(SetLobbyCommand.class, aliases);
            case "LOBBY" -> commandRegistry.register(LobbyCommand.class, aliases);
            case "VANISH" -> commandRegistry.register(VanishCommand.class, aliases);
        }
    }

    public List<CustomCommand> getCustomCommands() {
        return customCommands;
    }
}
