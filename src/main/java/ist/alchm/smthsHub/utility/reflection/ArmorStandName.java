package ist.alchm.smthsHub.utility.reflection;

import ist.alchm.smthsHub.SmthsHubPlugin;
import org.bukkit.entity.ArmorStand;
import org.bukkit.plugin.java.JavaPlugin;

public class ArmorStandName {

    private static final JavaPlugin PLUGIN = JavaPlugin.getProvidingPlugin(SmthsHubPlugin.class);

    public static String getName(ArmorStand stand) {
        return stand.getCustomName();
    }
}
