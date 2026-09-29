package ist.alchm.smthsHub.inventory;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.debug.BedrockDebug;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class BedrockMenus {

    private static final String FORM = "floodgate:form";
    private static SmthsHubPlugin plugin;

    private BedrockMenus() {
    }

    public static void enable(SmthsHubPlugin hub) {
        plugin = hub;
    }

    public static void disable(SmthsHubPlugin hub, boolean shutdown) {
        if (shutdown) {
            plugin = null;
        }
    }

    public static boolean tryOpen(SmthsHubPlugin plugin, Player player, String title, Map<Integer, InventoryItem> icons) {
        List<InventoryItem> buttons = buttons(icons);
        if (buttons.isEmpty()) {
            return false;
        }
        String formTitle = plain(title);
        if (formTitle.isEmpty()) {
            formTitle = "Menu";
        }
        boolean floodgatePluginEnabled = plugin.getServer().getPluginManager().isPluginEnabled("floodgate");
        if (floodgatePluginEnabled && FloodgateForms.open(plugin, player, formTitle, buttons)) {
            if (BedrockDebug.active()) {
                BedrockDebug.formApi(player);
            }
            return true;
        }
        if (BedrockDebug.active() && MenuText.floodgateUuid(player.getUniqueId())) {
            BedrockDebug.formSkipped(player);
        }
        return false;
    }

    public static Status status(Player player) {
        List<String> names = new ArrayList<>();
        SmthsHubPlugin hub = plugin;
        if (hub != null) {
            for (PluginMessageListenerRegistration registration : hub.getServer().getMessenger().getIncomingChannelRegistrations(FORM)) {
                names.add(registration.getPlugin().getName() + ":" + registration.getListener().getClass().getName());
            }
        }
        return new Status(
                MenuText.floodgateUuid(player.getUniqueId()),
                listed(player),
                player.getListeningPluginChannels().contains(FORM),
                List.copyOf(names),
                false,
                null
        );
    }

    static String plain(String input) {
        return MenuText.plain(input);
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

    private static boolean listed(Player player) {
        try {
            return FloodgateApi.getInstance().getPlayer(player.getUniqueId()) != null;
        } catch (Throwable ex) {
            return false;
        }
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

    public record Status(boolean floodgateUuid, boolean listed, boolean listening, List<String> plugins, boolean holding, Integer pendingId) {
    }
}
