package ist.alchm.smthsHub.command.commands;

import cl.bgmp.minecraft.util.commands.CommandContext;
import cl.bgmp.minecraft.util.commands.annotations.Command;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.Permissions;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.module.modules.player.PlayerVanish;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand {

    private final SmthsHubPlugin plugin;

    public VanishCommand(SmthsHubPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(
            aliases = {"vanish"},
            desc = "Disappear into thin air!"
    )
    public void vanish(final CommandContext args, final CommandSender sender) throws CommandException {

        if (!sender.hasPermission(Permissions.COMMAND_VANISH.getPermission())) {
            Messages.NO_PERMISSION.send(sender);
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot set the spawn location.");
            return;
        }

        PlayerVanish vanishModule = ((PlayerVanish) plugin.getModuleManager().getModule(ModuleType.VANISH));
        vanishModule.toggleVanish(player);
    }
}
