package ist.alchm.smthsHub.module.modules.world;

import com.tcoded.folialib.impl.PlatformScheduler;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.config.ConfigType;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.spawn.SpawnMemory;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerRespawnEvent;

public class LobbySpawn extends Module {

    private static final int MAX_LOAD_ATTEMPTS = 20;

    private boolean spawnJoin;
    private final SpawnMemory<Location> memory = new SpawnMemory<>();
    private int loadAttempts;
    private final SmthsHubPlugin plugin;
    private final PlatformScheduler scheduler;

    public LobbySpawn(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.LOBBY);
        this.plugin = plugin;
        this.scheduler = SmthsHubPlugin.scheduler();
        spawnJoin = getConfig(ConfigType.SETTINGS).getBoolean("join_settings.spawn_join", false);
        loadFromDisk();
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        FileConfiguration config = getConfig(ConfigType.DATA);
        Location write = memory.persistOrKeep(readStored(config));
        if (write != null) {
            config.set("spawn", write);
        }
        plugin.getConfigManager().saveFiles();
    }

    public Location getLocation() {
        return memory.value();
    }

    public void setLocation(Location location) {
        Location copy = location.clone();
        memory.setCommand(copy);
        getConfig(ConfigType.DATA).set("spawn", copy);
    }

    // Paper 26.2 leaves PlayerJoinEvent teleport undefined and keeps the player.dat position.
    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        if (!spawnJoin) {
            return;
        }
        Location target = memory.value();
        if (target == null) {
            return;
        }
        event.setSpawnLocation(target.clone());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location target = memory.value();
        if (target != null && !inDisabledWorld(player.getLocation())) {
            event.setRespawnLocation(target);
        }
    }

    private void loadFromDisk() {
        FileConfiguration config = getConfig(ConfigType.DATA);
        if (!config.contains("spawn")) {
            return;
        }
        Location stored = readStored(config);
        if (stored == null || stored.getWorld() == null) {
            retryLoad();
            return;
        }
        memory.loadStored(stored);
    }

    private void retryLoad() {
        if (loadAttempts >= MAX_LOAD_ATTEMPTS) {
            plugin.getLogger().warning("Lobby spawn world is not loaded; the saved spawn was left unchanged.");
            return;
        }
        loadAttempts++;
        scheduler.runLater(task -> loadFromDisk(), 10L);
    }

    private Location readStored(FileConfiguration config) {
        if (!config.contains("spawn")) {
            return null;
        }
        try {
            return config.getLocation("spawn");
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
