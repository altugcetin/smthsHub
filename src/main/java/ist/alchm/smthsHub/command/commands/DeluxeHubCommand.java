package ist.alchm.smthsHub.command.commands;

import cl.bgmp.minecraft.util.commands.CommandContext;
import cl.bgmp.minecraft.util.commands.annotations.Command;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import com.tcoded.folialib.impl.PlatformScheduler;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.Permissions;
import ist.alchm.smthsHub.command.CommandManager;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.inventory.BedrockMenus;
import ist.alchm.smthsHub.inventory.InventoryManager;
import ist.alchm.smthsHub.module.ModuleManager;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.module.modules.hologram.Hologram;
import ist.alchm.smthsHub.module.modules.hotbar.HotbarItem;
import ist.alchm.smthsHub.module.modules.hotbar.HotbarManager;
import ist.alchm.smthsHub.module.modules.visual.scoreboard.ScoreboardManager;
import ist.alchm.smthsHub.module.modules.world.LobbySpawn;
import ist.alchm.smthsHub.utility.TextUtil;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DeluxeHubCommand {

    private final SmthsHubPlugin plugin;
    private final PlatformScheduler scheduler;

    public DeluxeHubCommand(SmthsHubPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = SmthsHubPlugin.scheduler();
    }

    @Command(
            aliases = {"deluxehub", "dhub", "smthshub"},
            desc = "View plugin information"
    )
    public void main(final CommandContext args, final CommandSender sender) throws CommandException {

        PluginDescriptionFile pdfFile = plugin.getDescription();

		/*
		Command: help
		Description: displays help message
		*/
        if (args.argsLength() == 0 || args.getString(0).equalsIgnoreCase("help")) {

            if (!sender.hasPermission(Permissions.COMMAND_DELUXEHUB_HELP.getPermission())) {
                sender.sendMessage(ColorUtil.color("&8&l> &7Server is running &dsmthsHub &ev" + pdfFile.getVersion() + " &7By &6ItzSave & ItsLewizzz"));
                return;
            }

            sender.sendMessage("");
            sender.sendMessage(ColorUtil.color("&d&lsmthsHub " + "&fv" + plugin.getDescription().getVersion()));
            sender.sendMessage(ColorUtil.color("&7Author: &fItzSave & ItsLewizzz"));
            sender.sendMessage("");
            sender.sendMessage(ColorUtil.color(" &d/deluxehub info &8- &7&oDisplays information about the current config"));
            sender.sendMessage(ColorUtil.color(" &d/deluxehub scoreboard &8- &7&oToggle the scoreboard"));
            sender.sendMessage(ColorUtil.color(" &d/deluxehub open <menu> &8- &7&oOpen a custom menu"));
            sender.sendMessage(ColorUtil.color(" &d/deluxehub hologram &8- &7&oView the hologram help"));
            sender.sendMessage("");
            sender.sendMessage(ColorUtil.color("  &d/vanish &8- &7&oToggle vanish mode"));
            sender.sendMessage(ColorUtil.color("  &d/fly &8- &7&oToggle flight mode"));
            sender.sendMessage(ColorUtil.color("  &d/setlobby &8- &7&oSet the spawn location"));
            sender.sendMessage(ColorUtil.color("  &d/lobby &8- &7&oTeleport to the spawn location"));
            sender.sendMessage(ColorUtil.color("  &d/gamemode <gamemode> &8- &7&oSet your gamemode"));
            sender.sendMessage(ColorUtil.color("  &d/gmc &8- &7&oGo into creative mode"));
            sender.sendMessage(ColorUtil.color("  &d/gms &8- &7&oGo into survival mode"));
            sender.sendMessage(ColorUtil.color("  &d/gma &8- &7&oGo into adventure mode"));
            sender.sendMessage(ColorUtil.color("  &d/gmsp &8- &7&oGo into spectator mode"));
            sender.sendMessage(ColorUtil.color("  &d/clearchat &8- &7&oClear global chat"));
            sender.sendMessage(ColorUtil.color("  &d/lockchat &8- &7&oLock/unlock global chat"));
            sender.sendMessage("");
            return;
        }

		/*
		Command: reload
		Description: reloads the entire plugin
		*/
        else if (args.getString(0).equalsIgnoreCase("reload")) {

            if (!sender.hasPermission(Permissions.COMMAND_DELUXEHUB_RELOAD.getPermission())) {
                Messages.NO_PERMISSION.send(sender);
                return;
            }

            long start = System.currentTimeMillis();
            plugin.reload();
            Messages.CONFIG_RELOAD.send(sender,"%time%", String.valueOf(System.currentTimeMillis() - start));
            int inventories = plugin.getInventoryManager().getInventories().size();
            if (inventories > 0) {
                sender.sendMessage(ColorUtil.color("&8- &7Loaded &a" + inventories + "&7 custom menus."));
            }
        }

		/*
		Command: scoreboard
		Description: toggles the scoreboard on/off
		*/
        else if (args.getString(0).equalsIgnoreCase("scoreboard")) {

            if (!(sender instanceof Player player)) {
                throw new CommandException("Console cannot toggle the scoreboard");
            }

            if (!sender.hasPermission(Permissions.COMMAND_SCOREBOARD_TOGGLE.getPermission())) {
                Messages.NO_PERMISSION.send(sender);
                return;
            }

            if (!plugin.getModuleManager().isEnabled(ModuleType.SCOREBOARD)) {
                sender.sendMessage(ColorUtil.color("&cThe scoreboard module is not enabled in the configuration."));
                return;
            }

            ScoreboardManager scoreboardManager = ((ScoreboardManager) plugin.getModuleManager().getModule(ModuleType.SCOREBOARD));

            if (scoreboardManager.hasScore(player.getUniqueId())) {
                scoreboardManager.removeScoreboard(player);
                Messages.SCOREBOARD_TOGGLE.send(player, "%value%", "disabled");
            } else {
                scoreboardManager.createScoreboard(player);
                Messages.SCOREBOARD_TOGGLE.send(player, "%value%", "enabled");
            }
        }

		/*
		Command: info
		Description: displays useful information about the configuration
		*/
        else if (args.getString(0).equalsIgnoreCase("info")) {

            if (!sender.hasPermission(Permissions.COMMAND_DELUXEHUB_HELP.getPermission())) {
                Messages.NO_PERMISSION.send(sender);
                return;
            }

            sender.sendMessage("");
            sender.sendMessage(ColorUtil.color("&d&lPlugin Information"));
            sender.sendMessage("");

            Location location = ((LobbySpawn) plugin.getModuleManager().getModule(ModuleType.LOBBY)).getLocation();
            sender.sendMessage(ColorUtil.color("&7Spawn set &8- " + (location != null ? "&ayes" : "&cno &7&o(/setlobby)")));

            sender.sendMessage("");

            ModuleManager moduleManager = plugin.getModuleManager();
            sender.sendMessage(ColorUtil.color("&7Disabled Worlds (" + moduleManager.getDisabledWorlds().size() + ") &8- &a" + (String.join(", ", moduleManager.getDisabledWorlds()))));

            InventoryManager inventoryManager = plugin.getInventoryManager();
            sender.sendMessage(ColorUtil.color("&7Custom menus (" + inventoryManager.getInventories().size() + ")" + " &8- &a" + (String.join(", ", inventoryManager.getInventories().keySet()))));

            HotbarManager hotbarManager = ((HotbarManager) plugin.getModuleManager().getModule(ModuleType.HOTBAR_ITEMS));
            sender.sendMessage(ColorUtil.color("&7Hotbar items (" + hotbarManager.getHotbarItems().size() + ")" + " &8- &a" + (hotbarManager.getHotbarItems().stream().map(HotbarItem::getKey).collect(Collectors.joining(", ")))));

            CommandManager commandManager = plugin.getCommandManager();
            sender.sendMessage(ColorUtil.color("&7Custom commands (" + commandManager.getCustomCommands().size() + ")" + " &8- &a" + (commandManager.getCustomCommands().stream().map(command -> command.getAliases().get(0)).collect(Collectors.joining(", ")))));

            sender.sendMessage("");

            sender.sendMessage(ColorUtil.color("&7PlaceholderAPI Hook: " + (plugin.getHookManager().isHookEnabled("PLACEHOLDER_API") ? "&ayes" : "&cno")));
            sender.sendMessage(ColorUtil.color("&7HeadDatabase Hook: " + (plugin.getHookManager().isHookEnabled("HEAD_DATABASE") ? "&ayes" : "&cno")));

            sender.sendMessage("");
        }

		/*
		Command: open
		Description: opens a custom menu
		*/
        else if (args.getString(0).equalsIgnoreCase("open")) {
            if (!(sender instanceof Player)) {
                throw new CommandException("Console cannot open menus");
            }

            if (!sender.hasPermission(Permissions.COMMAND_OPEN_MENUS.getPermission())) {
                Messages.NO_PERMISSION.send(sender);
                return;
            }

            if (args.argsLength() < 2) {
                sender.sendMessage(ColorUtil.color("&cUsage: /deluxehub open <menu>"));
                return;
            }

            String menuId = args.getString(1);

            plugin.getInventoryManager().getInventory(menuId).ifPresentOrElse(
                    inventory -> scheduler.runAtEntity((Player) sender, task -> inventory.openInventory((Player) sender)),
                    () -> sender.sendMessage(ColorUtil.color("&c'" + menuId + "' is not a valid menu ID."))
            );
        }

        else if (args.getString(0).equalsIgnoreCase("bedrock")) {
            if (!sender.hasPermission(Permissions.COMMAND_BEDROCK.getPermission())) {
                Messages.NO_PERMISSION.send(sender);
                return;
            }
            if (args.argsLength() < 2) {
                Messages.BEDROCK_USAGE.send(sender);
                return;
            }
            Player target = Bukkit.getPlayer(args.getString(1));
            if (target == null) {
                Messages.BEDROCK_OFFLINE.send(sender, "%player%", args.getString(1));
                return;
            }
            BedrockMenus.Status status = BedrockMenus.status(target);
            String yes = Messages.BEDROCK_YES.text();
            String no = Messages.BEDROCK_NO.text();
            String plugins = status.plugins().isEmpty()
                    ? Messages.BEDROCK_NONE.text()
                    : String.join(", ", status.plugins());
            String pending = status.pendingId() == null
                    ? Messages.BEDROCK_NONE.text()
                    : status.pendingId().toString();
            Messages.BEDROCK_REPORT.send(sender,
                    "%uuid%", target.getUniqueId().toString(),
                    "%floodgate_uuid%", status.floodgateUuid() ? yes : no,
                    "%listed%", status.listed() ? yes : no,
                    "%listening%", status.listening() ? yes : no,
                    "%plugins%", plugins,
                    "%holding%", status.holding() ? yes : no,
                    "%pending%", pending
            );
        }

        /*
         * Holograms
         */
        if (args.getString(0).equalsIgnoreCase("hologram") || args.getString(0).equalsIgnoreCase("holo")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("You cannot do this command.");
                return;
            }

            if (!sender.hasPermission(Permissions.COMMAND_HOLOGRAMS.getPermission())) {
                Messages.NO_PERMISSION.send(sender);
                return;
            }

            if (args.argsLength() == 1) {
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.color("&d&lsmthsHub Holograms"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.color(" &d/" + args.getCommand() + " hologram list"));
                sender.sendMessage(ColorUtil.color("   &7&oList all created holograms"));
                sender.sendMessage(ColorUtil.color(" &d/" + args.getCommand() + " hologram create <id>"));
                sender.sendMessage(ColorUtil.color("   &7&oCreate a new hologram"));
                sender.sendMessage(ColorUtil.color(" &d/" + args.getCommand() + " hologram remove <id>"));
                sender.sendMessage(ColorUtil.color("   &7&oDelete an existing hologram"));
                sender.sendMessage(ColorUtil.color(" &d/" + args.getCommand() + " hologram move <id>"));
                sender.sendMessage(ColorUtil.color("   &7&oMove the location of a hologram"));
                sender.sendMessage(ColorUtil.color(""));
                sender.sendMessage(ColorUtil.color(" &d/" + args.getCommand() + " hologram setline <id> <line> <text>"));
                sender.sendMessage(ColorUtil.color("   &7&oSet the line of a specific hologram"));
                sender.sendMessage(ColorUtil.color(" &d/" + args.getCommand() + " hologram addline <id> <text>"));
                sender.sendMessage(ColorUtil.color("   &7&oAdd a new line to a hologram"));
                sender.sendMessage(ColorUtil.color(" &d/" + args.getCommand() + " hologram removeline <id> <line>"));
                sender.sendMessage(ColorUtil.color("   &7&oRemove a line from a hologram"));
                sender.sendMessage("");
                return;
            }

            if (args.argsLength() >= 2) {
                if (args.getString(1).equalsIgnoreCase("list")) {

                    if (plugin.getHologramManager().getHolograms().isEmpty()) {
                        Messages.HOLOGRAMS_EMPTY.send(player);
                        return;
                    }

                    sender.sendMessage("");
                    sender.sendMessage(ColorUtil.color("&d&lHologram List"));
                    for (Hologram entry : plugin.getHologramManager().getHolograms()) {
                        sender.sendMessage(ColorUtil.color("&8- &7" + entry.getName()));
                    }
                    sender.sendMessage("");
                }

                if (args.getString(1).equalsIgnoreCase("create")) {
                    if (args.argsLength() == 2) {
                        sender.sendMessage(ColorUtil.color("&cUsage: /deluxehub hologram create <id>"));
                        return;
                    }

                    if (plugin.getHologramManager().hasHologram(args.getString(2))) {
                        Messages.HOLOGRAMS_ALREADY_EXISTS.send(player, "%name%", args.getString(2));
                        return;
                    }

                    Hologram holo = plugin.getHologramManager().createHologram(args.getString(2), player.getLocation());
                    List<String> defaultMsg = new ArrayList<String>();
                    defaultMsg.add("&7Created new Hologram called &b" + args.getString(2));
                    defaultMsg.add("&7Use &b/deluxehub holo &7to customise");
                    holo.setLines(defaultMsg);
                    Messages.HOLOGRAMS_SPAWNED.send(player, "%name%", args.getString(2));
                    return;
                }

                if (args.getString(1).equalsIgnoreCase("remove") || args.getString(1).equalsIgnoreCase("delete")) {
                    if (args.argsLength() == 2) {
                        sender.sendMessage(ColorUtil.color("&cUsage: /deluxehub hologram remove <id>"));
                        return;
                    }

                    if (!plugin.getHologramManager().hasHologram(args.getString(2))) {
                        Messages.HOLOGRAMS_INVALID_HOLOGRAM.send(player, "%name%", args.getString(2));
                        return;
                    }

                    plugin.getHologramManager().deleteHologram(args.getString(2));
                    Messages.HOLOGRAMS_DESPAWNED.send(player, "%name%", args.getString(2));
                    return;
                }

                if (args.getString(1).equalsIgnoreCase("setline")) {
                    if (args.argsLength() < 5) {
                        sender.sendMessage(ColorUtil.color("&cUsage: /deluxehub hologram setline <id> <line> <text>"));
                        return;
                    }

                    if (!plugin.getHologramManager().hasHologram(args.getString(2))) {
                        Messages.HOLOGRAMS_INVALID_HOLOGRAM.send(player, "%name%", args.getString(2));
                        return;
                    }

                    Hologram holo = plugin.getHologramManager().getHologram(args.getString(2));
                    int line = Integer.parseInt(args.getString(3));
                    String text = TextUtil.joinString(5, args.getOriginalArgs());

                    if (!holo.hasLine(line)) {
                        Messages.HOLOGRAMS_INVALID_LINE.send(player, "%line%", String.valueOf(line));
                        return;
                    }
                    holo.setLine(line, text);
                    Messages.HOLOGRAMS_LINE_SET.send(player, "%line%", String.valueOf(line));
                    return;
                }

                if (args.getString(1).equalsIgnoreCase("addline")) {
                    if (args.argsLength() <= 3) {
                        sender.sendMessage(ColorUtil.color("&cUsage: /deluxehub hologram addline <id> <text>"));
                        return;
                    }

                    if (!plugin.getHologramManager().hasHologram(args.getString(2))) {
                        Messages.HOLOGRAMS_INVALID_HOLOGRAM.send(player, "%name%", args.getString(2));
                        return;
                    }

                    Hologram holo = plugin.getHologramManager().getHologram(args.getString(2));
                    String text = TextUtil.joinString(4, args.getOriginalArgs());

                    holo.addLine(text);
                    Messages.HOLOGRAMS_ADDED_LINE.send(player, "%name%", args.getString(2));
                }

                if (args.getString(1).equalsIgnoreCase("removeline")) {
                    if (args.argsLength() != 4) {
                        sender.sendMessage(ColorUtil.color("&cUsage: /deluxehub hologram removeline <id> <line>"));
                        return;
                    }

                    if (!plugin.getHologramManager().hasHologram(args.getString(2))) {
                        Messages.HOLOGRAMS_INVALID_HOLOGRAM.send(player, "%name%", args.getString(2));
                        return;
                    }

                    Hologram holo = plugin.getHologramManager().getHologram(args.getString(2));
                    int line = Integer.parseInt(args.getString(3));

                    if (!holo.hasLine(line)) {
                        Messages.HOLOGRAMS_INVALID_LINE.send(player, "%line%", String.valueOf(line));
                        return;
                    }

                    if (holo.removeLine(line) == null) {
                        plugin.getHologramManager().deleteHologram(args.getString(2));
                        Messages.HOLOGRAMS_REMOVED_LINE.send(player, "%name%", args.getString(2));
                    }

                    return;
                }

                if (args.getString(1).equalsIgnoreCase("move")) {
                    if (args.argsLength() == 2) {
                        sender.sendMessage(ColorUtil.color("&cUsage: /deluxehub hologram move <id>"));
                        return;
                    }

                    if (!plugin.getHologramManager().hasHologram(args.getString(2))) {
                        Messages.HOLOGRAMS_INVALID_HOLOGRAM.send(player, "%name%", args.getString(2));
                        return;
                    }

                    Hologram holo = plugin.getHologramManager().getHologram(args.getString(2));

                    holo.setLocation(player.getLocation());
                    Messages.HOLOGRAMS_MOVED.send(player, "%name%", args.getString(2));
                }
            }
        }
    }
}
