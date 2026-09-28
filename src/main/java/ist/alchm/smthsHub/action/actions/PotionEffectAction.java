package ist.alchm.smthsHub.action.actions;

import com.cryptomorin.xseries.XPotion;
import com.tcoded.folialib.impl.PlatformScheduler;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.action.Action;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class PotionEffectAction implements Action {

    private final PlatformScheduler scheduler = SmthsHubPlugin.scheduler();

    @Override
    public String getIdentifier() {
        return "EFFECT";
    }

    @Override
    public void execute(SmthsHubPlugin plugin, Player player, String data) {
        String[] args = data.split(";");
        scheduler.runAtEntity(player, task -> {
            PotionEffect effect = XPotion.matchXPotion(args[0]).get()
                    .buildPotionEffect(
                        PotionEffect.INFINITE_DURATION, 
                        Integer.parseInt(args[1]) - 1
                    );
            player.addPotionEffect(effect);
        });
    }
}
