package ist.alchm.smthsHub.module.modules.hologram;

import com.tcoded.folialib.impl.PlatformScheduler;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.config.ConfigType;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import ist.alchm.smthsHub.utility.reflection.ArmorStandName;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager extends Module {

    private final PlatformScheduler scheduler = SmthsHubPlugin.scheduler();

    private Set<Hologram> holograms;

    public HologramManager(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.HOLOGRAMS);
    }

    @Override
    public void onEnable() {
        holograms = ConcurrentHashMap.newKeySet();
        loadHolograms();
    }

    @Override
    public void onDisable() {
        saveHologramsData();
        removeAllHologramEntities();
    }

    public void loadHolograms() {
        scheduler.runLater(() -> {
            FileConfiguration config = getConfig(ConfigType.DATA);

            if (config.contains("holograms")) {
                for (String key : config.getConfigurationSection("holograms").getKeys(false)) {
                    List<String> lines = config.getStringList("holograms." + key + ".lines");

                    Location loc = (Location) config.get("holograms." + key + ".location");
                    if (loc == null) {
                        continue;
                    }

                    deleteNearbyHolograms(loc);

                    Hologram holo = createHologram(key, loc);
                    holo.setLines(lines);
                }
            }
        }, 40L);
    }

    public void saveHolograms() {
        saveHologramsData();
        deleteAllHolograms();
    }

    private void saveHologramsData() {
        FileConfiguration config = getConfig(ConfigType.DATA);
        holograms.forEach(hologram -> {
            config.set("holograms." + hologram.getName() + ".location", hologram.getLocation());
            List<String> lines = new ArrayList<>();
            for (ArmorStand stand : hologram.getStands()) {
                lines.add(ArmorStandName.getName(stand));
            }

            config.set("holograms." + hologram.getName() + ".lines", lines);
        });

        getPlugin().getConfigManager().getFile(ConfigType.DATA).save();
    }

    public Set<Hologram> getHolograms() {
        return holograms;
    }

    public boolean hasHologram(String name) {
        return getHolograms().stream().anyMatch(hologram -> hologram.getName().equalsIgnoreCase(name));
    }

    public Hologram getHologram(String name) {
        return getHolograms().stream().filter(hologram -> hologram.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public Hologram createHologram(String name, Location location) {
        Hologram holo = new Hologram(getPlugin(), name, location);
        holograms.add(holo);
        return holo;
    }

    public void deleteHologram(String name) {
        Hologram holo = getHologram(name);
        holo.remove();

        holograms.remove(holo);
        getConfig(ConfigType.DATA).set("holograms." + name, null);
        getPlugin().getConfigManager().getFile(ConfigType.DATA).save();
    }

    public void deleteAllHolograms() {
        for (Hologram hologram : holograms) {
            hologram.remove();
        }

        holograms.clear();
    }

    private void removeAllHologramEntities() {
        for (Hologram hologram : holograms) {
            for (ArmorStand stand : hologram.getStands()) {
                if (stand != null && stand.isValid()) {
                    stand.remove();
                }
            }
        }
    }

    public void deleteNearbyHolograms(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        scheduler.runAtLocation(location, task -> world.getNearbyEntities(location, 0.5, 20, 0.5).stream()
                .filter(entity -> entity instanceof ArmorStand)
                .forEach(entity -> scheduler.runAtEntity(entity, nextTask -> entity.remove())));
    }
}
