package ist.alchm.smthsHub.command.commands;

import cl.bgmp.minecraft.util.commands.CommandContext;
import cl.bgmp.minecraft.util.commands.annotations.Command;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.module.modules.world.LobbySpawn;
import ist.alchm.smthsHub.utility.TeleportUtil;
import net.zithium.library.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand {

    private final SmthsHubPlugin plugin;

    public LobbyCommand(SmthsHubPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(
            aliases = {"lobby"},
            desc = "Teleport to the lobby (if set)"
    )
    public void lobby(final CommandContext args, final CommandSender sender) throws CommandException {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot teleport to spawn");
            return;
        }

        Location location = ((LobbySpawn) plugin.getModuleManager().getModule(ModuleType.LOBBY)).getLocation();
        if (location == null) {
            sender.sendMessage(ColorUtil.color("&cThe spawn location has not been set &7(/setlobby)&c."));
            return;
        }

        TeleportUtil.teleportCompatLaterAtEntity(player, location, 3L, null);
    }
}
