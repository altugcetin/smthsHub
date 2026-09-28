package ist.alchm.smthsHub.module.modules.player;

import com.tcoded.folialib.impl.PlatformScheduler;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerVanish extends Module {

    private List<UUID> vanished;
    private final SmthsHubPlugin plugin;
    private final PlatformScheduler scheduler;

    public PlayerVanish(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.VANISH);
        this.plugin = plugin;
        this.scheduler = SmthsHubPlugin.scheduler();
    }

    @Override
    public void onEnable() {
        vanished = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onDisable() {
        vanished.clear();
    }

    public void toggleVanish(Player player) {
        if (isVanished(player)) {
            vanished.remove(player.getUniqueId());
            
            for (Player pl : Bukkit.getOnlinePlayers()) {
                showPlayer(pl, player);
            }

            Messages.VANISH_DISABLE.send(player);
            
            scheduler.runAtEntity(player, task ->
                  player.removePotionEffect(PotionEffectType.NIGHT_VISION));
        } else {
            vanished.add(player.getUniqueId());
            
            for (Player pl : Bukkit.getOnlinePlayers()) {
                hidePlayer(pl, player);
            }

            Messages.VANISH_ENABLE.send(player);
            
            scheduler.runAtEntity(player, task ->
                  player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0)));
        }
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        vanished.forEach(hiddenUUID -> {
            Player hiddenPlayer = Bukkit.getPlayer(hiddenUUID);
            if (hiddenPlayer != null) {
                hidePlayer(joiningPlayer, hiddenPlayer);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        scheduler.runAtEntity(player, task -> player.removePotionEffect(PotionEffectType.NIGHT_VISION));
        vanished.remove(player.getUniqueId());
    }

    private void hidePlayer(Player source, Player target) {
        if (!Bukkit.isOwnedByCurrentRegion(source) || !Bukkit.isOwnedByCurrentRegion(target)) {
            return;
        }

        scheduler.runAtEntity(source, task -> source.hidePlayer(plugin, target));
    }

    private void showPlayer(Player source, Player target) {
        if (!Bukkit.isOwnedByCurrentRegion(source) || !Bukkit.isOwnedByCurrentRegion(target)) {
            return;
        }

        scheduler.runAtEntity(source, task -> source.showPlayer(plugin, target));
    }
}
