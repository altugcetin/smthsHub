package ist.alchm.smthsHub.module.modules.world;

import com.cryptomorin.xseries.XMaterial;
import de.tr7zw.changeme.nbtapi.NBTItem;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.Permissions;
import ist.alchm.smthsHub.config.ConfigType;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.cooldown.CooldownType;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.module.modules.hologram.Hologram;
import ist.alchm.smthsHub.utility.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class WorldProtect extends Module {

    private boolean hungerLoss;
    private boolean fallDamage;
    private boolean weatherChange;
    private boolean deathMessage;
    private boolean fireSpread;
    private boolean leafDecay;
    private boolean mobSpawning;
    private boolean blockBurn;
    private boolean voidDeath;
    private boolean itemDrop;
    private boolean itemPickup;
    private boolean blockBreak;
    private boolean blockPlace;
    private boolean blockInteract;
    private boolean playerPvP;
    private boolean playerDrowning;
    private boolean fireDamage;

    private final List<Material> interactable = Arrays.asList(
            XMaterial.ACACIA_DOOR.parseMaterial(),
            XMaterial.ACACIA_FENCE_GATE.parseMaterial(),
            XMaterial.ANVIL.parseMaterial(),
            XMaterial.FLOWER_POT.parseMaterial(),
            XMaterial.PAINTING.parseMaterial(),
            XMaterial.BEACON.parseMaterial(),
            XMaterial.RED_BED.parseMaterial(),
            XMaterial.BIRCH_DOOR.parseMaterial(),
            XMaterial.BIRCH_FENCE_GATE.parseMaterial(),
            XMaterial.OAK_BOAT.parseMaterial(),
            XMaterial.BREWING_STAND.parseMaterial(),
            XMaterial.COMMAND_BLOCK.parseMaterial(),
            XMaterial.CHEST.parseMaterial(),
            XMaterial.DARK_OAK_DOOR.parseMaterial(),
            XMaterial.SPRUCE_DOOR.parseMaterial(),
            XMaterial.DARK_OAK_FENCE_GATE.parseMaterial(),
            XMaterial.DAYLIGHT_DETECTOR.parseMaterial(),
            XMaterial.DAYLIGHT_DETECTOR.parseMaterial(),
            XMaterial.DISPENSER.parseMaterial(),
            XMaterial.DROPPER.parseMaterial(),
            XMaterial.ENCHANTING_TABLE.parseMaterial(),
            XMaterial.ENDER_CHEST.parseMaterial(),
            XMaterial.OAK_FENCE_GATE.parseMaterial(),
            XMaterial.FURNACE.parseMaterial(),
            XMaterial.HOPPER.parseMaterial(),
            XMaterial.HOPPER_MINECART.parseMaterial(),
            XMaterial.ITEM_FRAME.parseMaterial(),
            XMaterial.JUNGLE_DOOR.parseMaterial(),
            XMaterial.JUNGLE_FENCE_GATE.parseMaterial(),
            XMaterial.LEVER.parseMaterial(),
            XMaterial.MINECART.parseMaterial(),
            XMaterial.NOTE_BLOCK.parseMaterial(),
            XMaterial.MINECART.parseMaterial(),
            XMaterial.COMPARATOR.parseMaterial(),
            XMaterial.ACACIA_SIGN.parseMaterial(),
            XMaterial.BIRCH_SIGN.parseMaterial(),
            XMaterial.DARK_OAK_SIGN.parseMaterial(),
            XMaterial.JUNGLE_SIGN.parseMaterial(),
            XMaterial.OAK_SIGN.parseMaterial(),
            XMaterial.CHEST_MINECART.parseMaterial(),
            XMaterial.OAK_DOOR.parseMaterial(),
            XMaterial.OAK_TRAPDOOR.parseMaterial(),
            XMaterial.TRAPPED_CHEST.parseMaterial(),
            XMaterial.OAK_BUTTON.parseMaterial(),
            XMaterial.OAK_DOOR.parseMaterial());

    public WorldProtect(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.WORLD_PROTECT);
    }

    @Override
    public void onEnable() {
        FileConfiguration config = getConfig(ConfigType.SETTINGS);
        hungerLoss = config.getBoolean("world_settings.disable_hunger_loss");
        fallDamage = config.getBoolean("world_settings.disable_fall_damage");
        playerPvP = config.getBoolean("world_settings.disable_player_pvp");
        voidDeath = config.getBoolean("world_settings.disable_void_death");
        weatherChange = config.getBoolean("world_settings.disable_weather_change");
        deathMessage = config.getBoolean("world_settings.disable_death_message");
        mobSpawning = config.getBoolean("world_settings.disable_mob_spawning");
        itemDrop = config.getBoolean("world_settings.disable_item_drop");
        itemPickup = config.getBoolean("world_settings.disable_item_pickup");
        blockBreak = config.getBoolean("world_settings.disable_block_break");
        blockPlace = config.getBoolean("world_settings.disable_block_place");
        blockInteract = config.getBoolean("world_settings.disable_block_interact");
        blockBurn = config.getBoolean("world_settings.disable_block_burn");
        fireSpread = config.getBoolean("world_settings.disable_block_fire_spread");
        leafDecay = config.getBoolean("world_settings.disable_block_leaf_decay");
        playerDrowning = config.getBoolean("world_settings.disable_drowning");
        fireDamage = config.getBoolean("world_settings.disable_fire_damage");
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent event) {
        for (Hologram entry : getPlugin().getHologramManager().getHolograms()) {
            for (ArmorStand stand : entry.getStands()) {
                if (stand.equals(event.getRightClicked())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!blockBreak || event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) {
            return;
        }

        if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) {
            return;
        }

        event.setCancelled(true);

        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_BREAK, 3)) {
            Messages.EVENT_BLOCK_BREAK.send(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!blockPlace || event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        if (inDisabledWorld(player.getLocation())) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.AIR) {
            return;
        }

        if (new NBTItem(event.getItemInHand()).hasKey("hotbarItem")) {
            event.setCancelled(true);
            return;
        }

        if (player.hasPermission(Permissions.EVENT_BLOCK_PLACE.getPermission())) {
            return;
        }

        event.setCancelled(true);

        if (tryCooldown(event.getPlayer().getUniqueId(), CooldownType.BLOCK_PLACE, 3)) {
            Messages.EVENT_BLOCK_PLACE.send(player);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!blockBurn) {
            return;
        }

        if (inDisabledWorld(event.getBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);
    }

    // Prevent destroying of item frame/paintings
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDestroy(HangingBreakByEntityEvent event) {
        if (!blockBreak || inDisabledWorld(event.getEntity().getLocation())) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Painting) && !(entity instanceof ItemFrame)) {
            return;
        }
        if (!(event.getRemover() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (player.hasPermission(Permissions.EVENT_BLOCK_BREAK.getPermission())) {
            return;
        }
        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_BREAK, 3)) {
            Messages.EVENT_BLOCK_BREAK.send(player);
        }
    }

    // Prevent items being rotated in item frame
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getRightClicked().getLocation())) {
            return;
        }

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) {
            return;
        }

        if (entity instanceof ItemFrame) {
            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    // Prevent items being taken from item frames
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!blockInteract || inDisabledWorld(event.getEntity().getLocation())) {
            return;
        }

        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        if (entity instanceof ItemFrame && damager instanceof Player player) {
            if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) {
                return;
            }

            event.setCancelled(true);
            if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                Messages.EVENT_BLOCK_INTERACT.send(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (!blockInteract || inDisabledWorld(event.getPlayer().getLocation())) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(Permissions.EVENT_BLOCK_INTERACT.getPermission())) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            for (Material material : interactable) {
                if (block.getType() == material || block.getType().toString().contains("POTTED")) {

                    event.setCancelled(true);
                    if (tryCooldown(player.getUniqueId(), CooldownType.BLOCK_INTERACT, 3)) {
                        Messages.EVENT_BLOCK_INTERACT.send(player);
                    }

                    return;
                }
            }

        } else if (event.getAction() == Action.PHYSICAL && block.getType() == XMaterial.FARMLAND.parseMaterial())
            event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (inDisabledWorld(player.getLocation())) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (fallDamage && cause == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        } else if (playerDrowning && cause == EntityDamageEvent.DamageCause.DROWNING) {
            event.setCancelled(true);
        } else if (fireDamage && (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.LAVA)) {
            event.setCancelled(true);
        } else if (voidDeath && cause == EntityDamageEvent.DamageCause.VOID) {
            player.setFallDistance(0.0F);

            Location location = ((LobbySpawn) getPlugin().getModuleManager().getModule(ModuleType.LOBBY)).getLocation();
            if (location == null) {
                return;
            }

            TeleportUtil.teleportCompatLaterAtEntity(player, location, 3L, null);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireSpread(BlockIgniteEvent event) {
        if (!fireSpread) {
            return;
        }

        if (inDisabledWorld(event.getBlock().getLocation())) {
            return;
        }

        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!hungerLoss) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (inDisabledWorld(player.getLocation())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropEvent(PlayerDropItemEvent event) {
        if (!itemDrop) {
            return;
        }

        Player player = event.getPlayer();

        if (inDisabledWorld(player.getLocation())) {
            return;
        }

        if (player.hasPermission(Permissions.EVENT_ITEM_DROP.getPermission())) {
            return;
        }

        event.setCancelled(true);

        if (tryCooldown(player.getUniqueId(), CooldownType.ITEM_DROP, 3)) {
            Messages.EVENT_ITEM_DROP.send(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!itemPickup) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (inDisabledWorld(player.getLocation())) {
            return;
        }

        if (player.hasPermission(Permissions.EVENT_ITEM_PICKUP.getPermission())) {
            return;
        }

        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.ITEM_PICKUP, 3)) {
            Messages.EVENT_ITEM_PICKUP.send(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!leafDecay) {
            return;
        }

        if (inDisabledWorld(event.getBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!mobSpawning) {
            return;
        }

        if (inDisabledWorld(event.getEntity().getLocation())) {
            return;
        }

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!weatherChange || inDisabledWorld(event.getWorld())) {
            return;
        }

        event.setCancelled(event.toWeatherState());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!deathMessage || inDisabledWorld(event.getEntity().getLocation())) {
            return;
        }

        event.setDeathMessage(null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!playerPvP) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (inDisabledWorld(player.getLocation())) {
            return;
        }

        if (!(event.getDamager() instanceof Player damager)) {
            event.setCancelled(true);
            return;
        }
        if (damager.hasPermission(Permissions.EVENT_PLAYER_PVP.getPermission())) {
            return;
        }

        event.setCancelled(true);
        if (tryCooldown(player.getUniqueId(), CooldownType.PLAYER_PVP, 3)) {
            Messages.EVENT_PLAYER_PVP.send(player);
        }
    }
}
