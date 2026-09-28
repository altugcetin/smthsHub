package ist.alchm.smthsHub.command.commands;

import cl.bgmp.minecraft.util.commands.CommandContext;
import cl.bgmp.minecraft.util.commands.annotations.Command;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.Permissions;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.module.modules.chat.ChatLock;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class LockchatCommand {

    private final SmthsHubPlugin plugin;

    public LockchatCommand(SmthsHubPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(
            aliases = {"lockchat"},
            desc = "Locks global chat"
    )
    public void lockchat(final CommandContext args, final CommandSender sender) throws CommandException {

        if (!sender.hasPermission(Permissions.COMMAND_LOCKCHAT.getPermission())) {
            Messages.NO_PERMISSION.send(sender);
            return;
        }

        ChatLock chatLockModule = (ChatLock) plugin.getModuleManager().getModule(ModuleType.CHAT_LOCK);

        if (chatLockModule.isChatLocked()) {
            Bukkit.getOnlinePlayers().forEach(player -> Messages.CHAT_UNLOCKED_BROADCAST.send(player, "%player%", sender.getName()));
            chatLockModule.setChatLocked(false);
        } else {
            Bukkit.getOnlinePlayers().forEach(player -> Messages.CHAT_LOCKED_BROADCAST.send(player, "%player%", sender.getName()));
            chatLockModule.setChatLocked(true);
        }
    }
}
