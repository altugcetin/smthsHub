package ist.alchm.smthsHub.inventory;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.config.Messages;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class BedrockMenus {

    private static final AtomicBoolean NOT_LISTENING = new AtomicBoolean();
    private static final AtomicBoolean KICK_REMAINS = new AtomicBoolean();
    private static final AtomicBoolean UNLISTED = new AtomicBoolean();
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private static SmthsHubPlugin plugin;
    private static final Map<UUID, AtomicInteger> IDS = new ConcurrentHashMap<>();
    private static final List<ForeignFormListener> FOREIGN_FORM_LISTENERS = new ArrayList<>();

    private BedrockMenus() {
    }

    public static void enable(SmthsHubPlugin hub) {
        plugin = hub;
        NOT_LISTENING.set(false);
        KICK_REMAINS.set(false);
        UNLISTED.set(false);
        Messenger messenger = hub.getServer().getMessenger();
        if (!messenger.isOutgoingChannelRegistered(hub, BedrockFormPacket.CHANNEL)) {
            messenger.registerOutgoingPluginChannel(hub, BedrockFormPacket.CHANNEL);
        }
        if (!messenger.isIncomingChannelRegistered(hub, BedrockFormPacket.CHANNEL)) {
            messenger.registerIncomingPluginChannel(hub, BedrockFormPacket.CHANNEL, new Incoming());
        }
        hub.getServer().getPluginManager().registerEvents(new Quit(), hub);
    }

    public static void disable(SmthsHubPlugin hub, boolean shutdown) {
        PENDING.clear();
        IDS.clear();
        if (!shutdown) {
            return;
        }
        restoreKickListener(hub);
        plugin = null;
        Messenger messenger = hub.getServer().getMessenger();
        if (messenger.isIncomingChannelRegistered(hub, BedrockFormPacket.CHANNEL)) {
            messenger.unregisterIncomingPluginChannel(hub, BedrockFormPacket.CHANNEL);
        }
        if (messenger.isOutgoingChannelRegistered(hub, BedrockFormPacket.CHANNEL)) {
            messenger.unregisterOutgoingPluginChannel(hub, BedrockFormPacket.CHANNEL);
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
            return true;
        }
        boolean floodgateId = MenuText.floodgateUuid(player.getUniqueId());
        if (!floodgateId) {
            return false;
        }
        boolean listening = player.getListeningPluginChannels().contains(BedrockFormPacket.CHANNEL);
        boolean silenced = !floodgatePluginEnabled || silenceFloodgate(plugin);
        BedrockFormPacket.Open path = BedrockFormPacket.choose(floodgatePluginEnabled, false, true, listening, silenced);
        if (path != BedrockFormPacket.Open.RAW) {
            if (!listening && NOT_LISTENING.compareAndSet(false, true)) {
                plugin.getLogger().warning("Bedrock menu stayed a chest: " + player.getUniqueId() + " is not listening on " + BedrockFormPacket.CHANNEL + ".");
            } else if (listening && KICK_REMAINS.compareAndSet(false, true)) {
                plugin.getLogger().warning("Bedrock menu stayed a chest: Floodgate still receives " + BedrockFormPacket.CHANNEL + " and kicks players it does not list.");
            }
            return false;
        }
        if (floodgatePluginEnabled && UNLISTED.compareAndSet(false, true)) {
            String line = Messages.BEDROCK_UNLISTED.text("%uuid%", player.getUniqueId().toString());
            if (!line.isEmpty()) {
                plugin.getLogger().warning(line);
            }
        }
        return send(plugin, player, formTitle, buttons);
    }

    public static Status status(Player player) {
        List<String> names = new ArrayList<>();
        SmthsHubPlugin hub = plugin;
        if (hub != null) {
            for (PluginMessageListenerRegistration registration : hub.getServer().getMessenger().getIncomingChannelRegistrations(BedrockFormPacket.CHANNEL)) {
                names.add(registration.getPlugin().getName());
            }
        }
        Pending pending = PENDING.get(player.getUniqueId());
        return new Status(
                MenuText.floodgateUuid(player.getUniqueId()),
                floodgateLists(player),
                player.getListeningPluginChannels().contains(BedrockFormPacket.CHANNEL),
                List.copyOf(names),
                holdsFloodgateListener(),
                pending == null ? null : pending.id
        );
    }

    static void onMessage(String channel, Player player, byte[] message) {
        if (!BedrockFormPacket.CHANNEL.equals(channel) || message == null || message.length < 2) {
            return;
        }
        Pending pending = PENDING.get(player.getUniqueId());
        Integer pendingId = pending == null ? null : pending.id;
        BedrockFormLogic.Decision decision = BedrockFormLogic.decide(
                BedrockFormPacket.formId(message),
                pendingId,
                floodgateLists(player),
                holdsFloodgateListener()
        );
        if (decision == BedrockFormLogic.Decision.DELEGATE) {
            delegateListedForm(channel, player, message);
            return;
        }
        if (decision != BedrockFormLogic.Decision.ACT || pending == null) {
            return;
        }
        int clicked = BedrockFormPacket.clickedButton(message);
        if (clicked < 0) {
            clicked = BedrockFormPacket.labelButton(BedrockFormPacket.responseText(message), labels(pending));
        }
        if (clicked < 0 || clicked >= pending.buttons.size() || !player.isOnline()) {
            PENDING.remove(player.getUniqueId(), pending);
            return;
        }
        PENDING.remove(player.getUniqueId(), pending);
        InventoryItem item = pending.buttons.get(clicked);
        SmthsHubPlugin.scheduler().runAtEntity(player, task -> {
            if (!player.isOnline()) {
                return;
            }
            for (ClickAction action : item.getClickActions()) {
                action.execute(player);
            }
        });
    }

    private static List<String> labels(Pending pending) {
        List<String> labels = new ArrayList<>(pending.buttons.size());
        for (int i = 0; i < pending.buttons.size(); i++) {
            labels.add(buttonLabel(pending.buttons.get(i).getItemStack(), i));
        }
        return labels;
    }

    private static boolean floodgateLists(Player player) {
        try {
            return FloodgateApi.getInstance().getPlayer(player.getUniqueId()) != null;
        } catch (Throwable ex) {
            return false;
        }
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

    private static boolean send(SmthsHubPlugin plugin, Player player, String title, List<InventoryItem> buttons) {
        List<String> labels = new ArrayList<>(buttons.size());
        for (int i = 0; i < buttons.size(); i++) {
            labels.add(buttonLabel(buttons.get(i).getItemStack(), i));
        }
        int id = nextId(player.getUniqueId());
        byte[] packet = BedrockFormPacket.encode(id, title, labels);
        Pending pending = new Pending(id, List.copyOf(buttons));
        PENDING.put(player.getUniqueId(), pending);
        try {
            player.sendPluginMessage(plugin, BedrockFormPacket.CHANNEL, packet);
            return true;
        } catch (RuntimeException ex) {
            PENDING.remove(player.getUniqueId(), pending);
            plugin.getLogger().warning("Bedrock menu was not sent: " + ex.getMessage());
            return false;
        }
    }

    private static synchronized boolean silenceFloodgate(SmthsHubPlugin plugin) {
        Messenger messenger = plugin.getServer().getMessenger();
        List<PluginMessageListenerRegistration> registrations = new ArrayList<>(messenger.getIncomingChannelRegistrations(BedrockFormPacket.CHANNEL));
        List<String> names = new ArrayList<>(registrations.size());
        for (PluginMessageListenerRegistration registration : registrations) {
            names.add(registration.getPlugin().getName());
        }
        for (int index : BedrockFormLogic.floodgateRegistrations(names)) {
            PluginMessageListenerRegistration registration = registrations.get(index);
            messenger.unregisterIncomingPluginChannel(registration.getPlugin(), BedrockFormPacket.CHANNEL, registration.getListener());
            remember(registration);
        }
        for (PluginMessageListenerRegistration registration : messenger.getIncomingChannelRegistrations(BedrockFormPacket.CHANNEL)) {
            if (BedrockFormLogic.FLOODGATE.equalsIgnoreCase(registration.getPlugin().getName())) {
                return false;
            }
        }
        return true;
    }

    private static void remember(PluginMessageListenerRegistration registration) {
        for (ForeignFormListener held : FOREIGN_FORM_LISTENERS) {
            if (held.plugin().equals(registration.getPlugin()) && held.listener() == registration.getListener()) {
                return;
            }
        }
        FOREIGN_FORM_LISTENERS.add(new ForeignFormListener(registration.getPlugin(), registration.getListener()));
    }

    private static synchronized void restoreKickListener(SmthsHubPlugin plugin) {
        List<ForeignFormListener> saved = List.copyOf(FOREIGN_FORM_LISTENERS);
        FOREIGN_FORM_LISTENERS.clear();
        Messenger messenger = plugin.getServer().getMessenger();
        for (ForeignFormListener foreign : saved) {
            if (foreign.plugin().isEnabled()) {
                messenger.registerIncomingPluginChannel(foreign.plugin(), BedrockFormPacket.CHANNEL, foreign.listener());
            }
        }
    }

    private static void delegateListedForm(String channel, Player player, byte[] message) {
        for (ForeignFormListener foreign : foreignFormListeners()) {
            foreign.listener().onPluginMessageReceived(channel, player, message);
        }
    }

    private static synchronized boolean holdsFloodgateListener() {
        return !FOREIGN_FORM_LISTENERS.isEmpty();
    }

    private static synchronized List<ForeignFormListener> foreignFormListeners() {
        return List.copyOf(FOREIGN_FORM_LISTENERS);
    }

    private static int nextId(UUID uuid) {
        AtomicInteger counter = IDS.computeIfAbsent(uuid, key -> new AtomicInteger());
        return BedrockFormLogic.formId(BedrockFormLogic.HUB_SLOT, counter.getAndIncrement());
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

    private record Pending(int id, List<InventoryItem> buttons) {
    }

    private record ForeignFormListener(Plugin plugin, PluginMessageListener listener) {
    }

    private static final class Incoming implements PluginMessageListener {
        @Override
        public void onPluginMessageReceived(String channel, Player player, byte[] message) {
            onMessage(channel, player, message);
        }
    }

    private static final class Quit implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            UUID uuid = event.getPlayer().getUniqueId();
            PENDING.remove(uuid);
            IDS.remove(uuid);
        }
    }
}
