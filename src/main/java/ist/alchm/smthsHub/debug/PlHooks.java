package ist.alchm.smthsHub.debug;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.Plugin;

public final class PlHooks {

    private static PacketAdapter lowest;
    private static PacketAdapter monitor;

    private PlHooks() {
    }

    public static void register(Plugin plugin) {
        lowest = listen(plugin, ListenerPriority.LOWEST, false);
        monitor = listen(plugin, ListenerPriority.MONITOR, true);
        ProtocolLibrary.getProtocolManager().addPacketListener(lowest);
        ProtocolLibrary.getProtocolManager().addPacketListener(monitor);
    }

    public static void unregister() {
        if (lowest != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(lowest);
            lowest = null;
        }
        if (monitor != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(monitor);
            monitor = null;
        }
    }

    public static String dump() {
        try {
            StringBuilder out = new StringBuilder();
            for (com.comphenix.protocol.events.PacketListener listener : ProtocolLibrary.getProtocolManager().getPacketListeners()) {
                if (!(listener instanceof PacketAdapter adapter)) {
                    return null;
                }
                boolean payload = false;
                for (PacketType type : adapter.getReceivingWhitelist().getTypes()) {
                    if (type != null && type.name().contains("CUSTOM_PAYLOAD")) {
                        payload = true;
                        break;
                    }
                }
                if (!payload) {
                    continue;
                }
                String owner = adapter.getPlugin() == null ? "" : adapter.getPlugin().getName();
                out.append(adapter.getClass().getName()).append(' ').append(owner).append('\n');
            }
            String text = out.toString().trim();
            return text.isEmpty() ? null : text;
        } catch (Throwable ex) {
            return null;
        }
    }

    private static PacketAdapter listen(Plugin plugin, ListenerPriority priority, boolean monitor) {
        return new Watch(plugin, priority, monitor);
    }

    private static final class Watch extends PacketAdapter {

        private final boolean monitor;

        private Watch(Plugin plugin, ListenerPriority priority, boolean monitor) {
            super(plugin, priority, PacketType.Play.Client.CUSTOM_PAYLOAD, PacketType.Configuration.Client.CUSTOM_PAYLOAD);
            this.monitor = monitor;
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            try {
                if (event.getPlayer() == null) {
                    return;
                }
                String channel = channel(event.getPacket());
                if (channel == null || channel.isEmpty()) {
                    return;
                }
                BedrockDebug.protocolLib(event.getPlayer().getUniqueId(), channel, event.isCancelled(), monitor);
            } catch (Throwable ignored) {
            }
        }

        private static String channel(Object packet) {
            String[] methods = {"getMinecraftKeys", "getStrings"};
            for (String method : methods) {
                try {
                    Object modifier = packet.getClass().getMethod(method).invoke(packet);
                    Object value = modifier.getClass().getMethod("read", int.class).invoke(modifier, 0);
                    if (value != null) {
                        return String.valueOf(value);
                    }
                } catch (Throwable ignored) {
                }
            }
            return null;
        }
    }
}
