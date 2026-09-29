package ist.alchm.smthsHub.debug;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.UUID;

public final class PeHooks {

    private static PacketListenerAbstract lowest;
    private static PacketListenerAbstract monitor;

    private PeHooks() {
    }

    public static void register() {
        lowest = new Watch(PacketListenerPriority.LOWEST, false);
        monitor = new Watch(PacketListenerPriority.MONITOR, true);
        PacketEvents.getAPI().getEventManager().registerListener(lowest);
        PacketEvents.getAPI().getEventManager().registerListener(monitor);
    }

    public static void unregister() {
        if (lowest != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(lowest);
            lowest = null;
        }
        if (monitor != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(monitor);
            monitor = null;
        }
    }

    public static Object channel(Object player) {
        return PacketEvents.getAPI().getPlayerManager().getChannel(player);
    }

    public static String dump() {
        try {
            Object manager = PacketEvents.getAPI().getEventManager();
            StringBuilder out = new StringBuilder();
            walk(manager, out, 0);
            String text = out.toString().trim();
            return text.isEmpty() ? "PeHooks.dump bos" : text;
        } catch (Throwable ex) {
            return "PeHooks.dump " + FieldWalk.cause(ex);
        }
    }

    private static void walk(Object value, StringBuilder out, int depth) {
        if (value == null || depth > 4) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                walk(item, out, depth + 1);
            }
            return;
        }
        if (value instanceof PacketListenerAbstract listener) {
            String owner = owner(listener.getClass());
            out.append(listener.getClass().getName()).append(' ').append(listener.getPriority()).append(' ').append(owner).append('\n');
            return;
        }
        if (depth > 2 || value.getClass().getName().startsWith("java.")) {
            return;
        }
        Class<?> type = value.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType().isPrimitive()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object child = field.get(value);
                    if (child instanceof Collection<?> || child instanceof PacketListenerAbstract) {
                        walk(child, out, depth + 1);
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
    }

    private static String owner(Class<?> type) {
        try {
            return JavaPlugin.getProvidingPlugin(type).getName();
        } catch (Throwable ex) {
            return "";
        }
    }

    private static final class Watch extends PacketListenerAbstract {

        private final boolean monitor;

        private Watch(PacketListenerPriority priority, boolean monitor) {
            super(priority);
            this.monitor = monitor;
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            try {
                String channel = channel(event);
                if (channel == null || channel.isEmpty() || event.getUser() == null || event.getUser().getUUID() == null) {
                    return;
                }
                UUID uuid = event.getUser().getUUID();
                BedrockDebug.packetEvents(uuid, channel, event.isCancelled(), monitor);
            } catch (Throwable ignored) {
            }
        }

        private static String channel(PacketReceiveEvent event) {
            if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
                return new WrapperPlayClientPluginMessage(event).getChannelName();
            }
            if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
                return new WrapperConfigClientPluginMessage(event).getChannelName();
            }
            return null;
        }
    }
}
