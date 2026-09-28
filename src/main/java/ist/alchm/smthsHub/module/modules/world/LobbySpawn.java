package ist.alchm.smthsHub.module.modules.world;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.config.ConfigType;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.spawn.SpawnMemory;
import ist.alchm.smthsHub.spawn.SpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class LobbySpawn extends Module {

    private boolean spawnJoin;
    private final SpawnMemory<SpawnPoint> memory = new SpawnMemory<>();
    private final SmthsHubPlugin plugin;

    public LobbySpawn(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.LOBBY);
        this.plugin = plugin;
        spawnJoin = getConfig(ConfigType.SETTINGS).getBoolean("join_settings.spawn_join", false);
        loadFromDisk();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        FileConfiguration config = getConfig(ConfigType.DATA);
        SpawnPoint write = memory.persistOrKeep(readStored(config));
        if (write != null) {
            config.set("spawn", write.toMap());
        }
        plugin.getConfigManager().saveFiles();
    }

    public Location getLocation() {
        return resolve(memory.value());
    }

    public void setLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        SpawnPoint point = new SpawnPoint(world.getName(), world.getKey().asString(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        memory.setCommand(point);
        getConfig(ConfigType.DATA).set("spawn", point.toMap());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        if (!spawnJoin) {
            return;
        }
        Location target = resolve(memory.value());
        if (target == null) {
            return;
        }
        event.setSpawnLocation(target);
    }

    // PlayerList.placeNewPlayer queues the login position before this event and flushes it after.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!spawnJoin) {
            return;
        }
        Location target = resolve(memory.value());
        if (target == null) {
            return;
        }
        Player player = event.getPlayer();
        if (placed(player.getLocation(), memory.value())) {
            return;
        }
        player.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location target = resolve(memory.value());
        if (target != null && !inDisabledWorld(player.getLocation())) {
            event.setRespawnLocation(target);
        }
    }

    private void loadFromDisk() {
        SpawnPoint stored = readStored(getConfig(ConfigType.DATA));
        if (stored == null) {
            stored = SpawnPoint.parseDocument(readDataFile());
        }
        memory.loadStored(stored);
    }

    private String readDataFile() {
        try {
            return Files.readString(new java.io.File(plugin.storageFolder(), "data.yml").toPath());
        } catch (IOException ex) {
            return null;
        }
    }

    private SpawnPoint readStored(FileConfiguration config) {
        if (!config.contains("spawn")) {
            return null;
        }
        ConfigurationSection section = config.getConfigurationSection("spawn");
        if (section != null) {
            SpawnPoint fromSection = SpawnPoint.fromMap(section.getValues(false));
            if (fromSection != null) {
                return fromSection;
            }
        }
        Object raw = config.get("spawn");
        if (raw instanceof Map<?, ?> map) {
            java.util.LinkedHashMap<String, Object> copy = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            SpawnPoint fromMap = SpawnPoint.fromMap(copy);
            if (fromMap != null) {
                return fromMap;
            }
        }
        try {
            Location loc = config.getLocation("spawn");
            if (loc == null) {
                return null;
            }
            World world = loc.getWorld();
            if (world == null) {
                return null;
            }
            return new SpawnPoint(world.getName(), world.getKey().asString(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Location resolve(SpawnPoint point) {
        if (point == null) {
            return null;
        }
        World world = point.worldName == null ? null : Bukkit.getWorld(point.worldName);
        if (world == null && point.worldKey != null) {
            NamespacedKey key = NamespacedKey.fromString(point.worldKey);
            if (key != null) {
                world = Bukkit.getWorld(key);
            }
        }
        if (world == null) {
            return null;
        }
        return new Location(world, point.x, point.y, point.z, point.yaw, point.pitch);
    }

    private boolean placed(Location now, SpawnPoint point) {
        World world = now.getWorld();
        if (world == null || point == null) {
            return false;
        }
        return point.covers(world.getName(), world.getKey().asString(), now.getX(), now.getY(), now.getZ());
    }
}
