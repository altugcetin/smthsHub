package ist.alchm.smthsHub.command.commands;

import cl.bgmp.minecraft.util.commands.CommandContext;
import cl.bgmp.minecraft.util.commands.annotations.Command;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.Permissions;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.module.modules.world.LobbySpawn;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLobbyCommand {

    private final SmthsHubPlugin plugin;

    public SetLobbyCommand(SmthsHubPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(
            aliases = {"setlobby"},
            desc = "Set the lobby location"
    )
    public void setlobby(final CommandContext args, final CommandSender sender) throws CommandException {

        if (!sender.hasPermission(Permissions.COMMAND_SET_LOBBY.getPermission())) {
            Messages.NO_PERMISSION.send(sender);
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot set the spawn location.");
            return;
        }

        if (plugin.getModuleManager().getDisabledWorlds().contains(player.getWorld().getName())) {
            sender.sendMessage(ColorUtil.color("&cYou cannot set the lobby location in a disabled world."));
            return;
        }

        LobbySpawn lobbyModule = ((LobbySpawn) plugin.getModuleManager().getModule(ModuleType.LOBBY));
        lobbyModule.setLocation(player.getLocation());
        plugin.getConfigManager().saveFiles(); // Save config files.
        Messages.SET_LOBBY.send(sender);
    }
}
