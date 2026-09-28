package ist.alchm.smthsHub.inventory;

import ist.alchm.smthsHub.SmthsHubPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class BedrockMenus {

    private static final Pattern LEGACY_COLOR = Pattern.compile("(?i)[§&][0-9A-FK-OR]");

    private BedrockMenus() {
    }

    public static boolean tryOpen(SmthsHubPlugin plugin, Player player, String title, Map<Integer, InventoryItem> icons) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("floodgate")) {
            return false;
        }
        List<InventoryItem> buttons = buttons(icons);
        if (buttons.isEmpty()) {
            return false;
        }
        return FloodgateForms.open(plugin, player, plain(title), buttons);
    }

    static String plain(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return LEGACY_COLOR.matcher(input).replaceAll("").trim();
    }

    static String buttonLabel(ItemStack item, int index) {
        String label = "";
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                label = plain(meta.getDisplayName());
            }
        }
        if (label.isEmpty()) {
            label = "Item " + (index + 1);
        }
        return label;
    }

    private static List<InventoryItem> buttons(Map<Integer, InventoryItem> icons) {
        List<InventoryItem> buttons = new ArrayList<>();
        icons.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .filter(item -> !item.getClickActions().isEmpty())
                .forEach(buttons::add);
        return buttons;
    }
}
