package ist.alchm.smthsHub.module.modules.chat;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.command.CustomCommand;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CustomCommands extends Module {

    private List<CustomCommand> commands;

    public CustomCommands(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.CUSTOM_COMMANDS);
    }

    @Override
    public void onEnable() {
        commands = getPlugin().getCommandManager().getCustomCommands();
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) {
            return;
        }

        String command = event.getMessage().toLowerCase().replace("/", "");

        for (CustomCommand customCommand : commands) {
            if (customCommand.getAliases().stream().anyMatch(alias -> alias.equals(command))) {
                if (customCommand.getPermission() != null) if (!player.hasPermission(customCommand.getPermission())) {
                    Messages.CUSTOM_COMMAND_NO_PERMISSION.send(player);
                    event.setCancelled(true);
                    return;
                }

                event.setCancelled(true);
                getPlugin().getActionManager().executeActions(player, customCommand.getActions());
            }
        }
    }
}
