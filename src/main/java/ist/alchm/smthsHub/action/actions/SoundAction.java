package ist.alchm.smthsHub.action.actions;

import com.cryptomorin.xseries.XSound;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SoundAction implements Action {

    @Override
    public String getIdentifier() {
        return "SOUND";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        try {
            player.playSound(player.getLocation(), XSound.matchXSound(data).get().parseSound(), 1L, 1L);
        } catch (Exception ex) {
            Bukkit.getLogger().warning("[DeluxeHub Action] Invalid sound name: " + data.toUpperCase());
        }
    }
}
