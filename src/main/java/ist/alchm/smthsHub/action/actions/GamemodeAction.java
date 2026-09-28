package ist.alchm.smthsHub.action.actions;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GamemodeAction implements Action {

    @Override
    public String getIdentifier() {
        return "GAMEMODE";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        try {
            player.setGameMode(GameMode.valueOf(data.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            Bukkit.getLogger().warning("[DeluxeHub Action] Invalid gamemode name: " + data.toUpperCase());
        }
    }
}
