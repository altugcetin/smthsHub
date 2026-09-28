package ist.alchm.smthsHub.module.modules.hotbar.items;

import com.tcoded.folialib.impl.PlatformScheduler;
import de.tr7zw.changeme.nbtapi.NBTItem;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.config.ConfigType;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.cooldown.CooldownType;
import ist.alchm.smthsHub.module.modules.hotbar.HotbarItem;
import ist.alchm.smthsHub.module.modules.hotbar.HotbarManager;
import ist.alchm.smthsHub.utility.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerHider extends HotbarItem {

    private final PlatformScheduler scheduler = SmthsHubPlugin.scheduler();
    private final int cooldown;
    private final ItemStack hiddenItem;
    private final List<UUID> hidden;

    public PlayerHider(HotbarManager hotbarManager, ItemStack item, int slot, String key) {
        super(hotbarManager, item, slot, key);
        hidden = new CopyOnWriteArrayList<>();
        FileConfiguration config = getHotbarManager().getConfig(ConfigType.SETTINGS);
        NBTItem nbtItem = new NBTItem(ItemStackBuilder.getItemStack(config.getConfigurationSection("player_hider.hidden")).build());
        nbtItem.setString("hotbarItem", key);
        hiddenItem = nbtItem.getItem();
        cooldown = config.getInt("player_hider.cooldown");
    }

    @Override
    protected void onInteract(Player player) {

        if (!getHotbarManager().tryCooldown(player.getUniqueId(), CooldownType.PLAYER_HIDER, cooldown)) {
            Messages.COOLDOWN_ACTIVE.send(player, "%time%", getHotbarManager().getCooldown(player.getUniqueId(), CooldownType.PLAYER_HIDER));
            return;
        }

        if (!hidden.contains(player.getUniqueId())) {
            for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
                hidePlayer(player, pl);
            }

            hidden.add(player.getUniqueId());
            Messages.PLAYER_HIDER_HIDDEN.send(player);

            player.getInventory().setItem(getSlot(), hiddenItem);
        } else {
            for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
                showPlayer(player, pl);
            }

            hidden.remove(player.getUniqueId());
            Messages.PLAYER_HIDER_SHOWN.send(player);

            player.getInventory().setItem(getSlot(), getItem());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (hidden.contains(player.getUniqueId())) {
            for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
                showPlayer(player, pl);
            }
        }

        hidden.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        hidden.forEach(uuid -> {
            Player hiddenPlayer = Bukkit.getPlayer(uuid);
            if (hiddenPlayer != null) {
                hidePlayer(hiddenPlayer, player);
            }
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (getHotbarManager().inDisabledWorld(player.getLocation()) && hidden.contains(player.getUniqueId())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                showPlayer(player, p);
            }

            hidden.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawnEvent(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (hidden.contains(player.getUniqueId())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                showPlayer(player, p);
            }

            hidden.remove(player.getUniqueId());
        }
    }

    @SuppressWarnings("deprecation")
    private void hidePlayer(Player source, Player target) {
        if (!Bukkit.isOwnedByCurrentRegion(source) || !Bukkit.isOwnedByCurrentRegion(target)) {
            return;
        }

        scheduler.runAtEntity(source, task -> source.hidePlayer(target));
    }

    @SuppressWarnings("deprecation")
    private void showPlayer(Player source, Player target) {
        if (!Bukkit.isOwnedByCurrentRegion(source) || !Bukkit.isOwnedByCurrentRegion(target)) {
            return;
        }

        scheduler.runAtEntity(source, task -> source.showPlayer(target));
    }
}
