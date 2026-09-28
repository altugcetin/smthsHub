package ist.alchm.smthsHub.debug;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.inventory.BedrockMenus;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BedrockDebug {

    static final String PROBE = "smthshub:probe";
    private static final String FORM = "floodgate:form";
    private static final String INIT = "smthshub-debug-init";
    private static final String HANDSHAKE = "smthshub-debug-handshake";
    private static final String R1 = "smthshub-debug-r1";
    private static final String R4 = "smthshub-debug-r4";
    private static final String OUT = "smthshub-debug-out";
    private static final Key INIT_KEY = Key.key("smthshub", "debug-init");
    private static final AttributeKey<Hold> HOLD = AttributeKey.valueOf("smthshub.debug.handshake");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private static final String[] ANTI = {
            "anticheat", "anti-cheat", "nocheat", "vulcan", "grim", "matrix", "spartan",
            "intave", "karhu", "polar", "verus", "negativity", "redeye", "skillissue"
    };

    private static final AtomicBoolean ACTIVE = new AtomicBoolean();
    private static final ConcurrentLinkedQueue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, UUID> CHANNELS = new ConcurrentHashMap<>();

    private static SmthsHubPlugin hub;
    private static Listener joins;
    private static String fingerprint = "";
    private static boolean fingerprintKnown;
    private static boolean peInstalled;
    private static boolean plInstalled;
    private static final PluginMessageListener PROBE_LISTENER = (channel, player, message) -> {
        if (PROBE.equals(channel) && player != null) {
            UUID uuid = player.getUniqueId();
            QUEUE.add(() -> {
                Session session = SESSIONS.get(uuid);
                if (session != null) {
                    session.probeBukkit = Tri.YES;
                    session.line("R7", PROBE, "", Tri.UNKNOWN);
                }
            });
        }
    };

    private BedrockDebug() {
    }

    public static boolean active() {
        return ACTIVE.get();
    }

    public static void start(SmthsHubPlugin plugin) {
        hub = plugin;
        ACTIVE.set(true);
        readKey();
        registerProbe();
        installServer();
        installBridges();
        arm();
        say(Messages.lookup("DEBUG.ARMED"));
    }

    public static void arm() {
        if (!ACTIVE.get() || hub == null) {
            return;
        }
        if (joins != null) {
            org.bukkit.event.HandlerList.unregisterAll(joins);
        }
        joins = new Joins();
        hub.getServer().getPluginManager().registerEvents(joins, hub);
        SmthsHubPlugin.scheduler().runTimer(task -> {
            flush();
            repositionOnline();
        }, 5L, 5L);
    }

    public static void stop() {
        ACTIVE.set(false);
        QUEUE.clear();
        if (joins != null) {
            org.bukkit.event.HandlerList.unregisterAll(joins);
            joins = null;
        }
        if (hub != null) {
            try {
                hub.getServer().getMessenger().unregisterIncomingPluginChannel(hub, PROBE);
            } catch (Throwable ignored) {
            }
        }
        try {
            if (peInstalled) {
                PeHooks.unregister();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (plInstalled) {
                PlHooks.unregister();
            }
        } catch (Throwable ignored) {
        }
        peInstalled = false;
        plInstalled = false;
        paperInit(true);
        for (Channel parent : serverChannels()) {
            remove(parent, INIT);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Channel channel = channel(player);
            if (channel != null) {
                channel.eventLoop().execute(() -> {
                    remove(channel, R1);
                    remove(channel, R4);
                    remove(channel, OUT);
                    remove(channel, HANDSHAKE);
                });
            }
        }
        CHANNELS.clear();
        SESSIONS.clear();
    }

    public static void formApi(Player player) {
        if (!ACTIVE.get()) {
            return;
        }
        session(player).form("API", null);
    }

    public static void formRaw(Player player, int id) {
        if (!ACTIVE.get()) {
            return;
        }
        session(player).form("RAW", id);
    }

    public static void formSkipped(Player player) {
        if (!ACTIVE.get()) {
            return;
        }
        Session session = session(player);
        session.formSent = Tri.NO;
        session.formPath = "CHEST";
        QUEUE.add(session::emitForm);
    }

    public static void messenger(Player player) {
        if (!ACTIVE.get()) {
            return;
        }
        Session session = session(player);
        session.r7Counts.merge(FORM, 1, Integer::sum);
        session.line("R7", FORM, "", Tri.UNKNOWN);
    }

    public static void decision(Player player, String name, Integer responseId, Integer pendingId) {
        if (!ACTIVE.get()) {
            return;
        }
        Session session = session(player);
        session.decision = name;
        session.responseId = responseId;
        session.pendingId = pendingId;
        session.act = "ACT".equals(name) ? Tri.YES : "IGNORE".equals(name) ? Tri.NO : Tri.UNKNOWN;
        session.line("R8", FORM, name
                + " cevap=" + (responseId == null ? word(Tri.UNKNOWN) : responseId)
                + " bekleyen=" + (pendingId == null ? word(Tri.UNKNOWN) : pendingId), Tri.UNKNOWN);
        if (!"ACT".equals(name)) {
            QUEUE.add(session::emitForm);
        }
    }

    public static void actionRan(Player player) {
        if (!ACTIVE.get()) {
            return;
        }
        Session session = session(player);
        session.actionFailed = Tri.NO;
        QUEUE.add(session::emitForm);
    }

    public static void actionFailed(Player player, Throwable error) {
        if (!ACTIVE.get()) {
            return;
        }
        Session session = session(player);
        session.actionFailed = Tri.YES;
        session.stack = trace(error);
        QUEUE.add(session::emitForm);
    }

    public static void packetEvents(UUID uuid, String channel, boolean cancelled, boolean monitor) {
        if (!ACTIVE.get()) {
            return;
        }
        QUEUE.add(() -> {
            Session session = SESSIONS.get(uuid);
            if (session == null) {
                return;
            }
            session.packetEvents(channel, cancelled, monitor);
        });
    }

    public static void protocolLib(UUID uuid, String channel, boolean cancelled, boolean monitor) {
        if (!ACTIVE.get()) {
            return;
        }
        QUEUE.add(() -> {
            Session session = SESSIONS.get(uuid);
            if (session == null) {
                return;
            }
            session.protocolLib(channel, cancelled, monitor);
        });
    }

    private static void flush() {
        Runnable job;
        while ((job = QUEUE.poll()) != null) {
            try {
                job.run();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void say(String line) {
        if (hub != null && line != null && !line.isEmpty()) {
            hub.getLogger().info(line);
        }
    }

    private static Session session(Player player) {
        return SESSIONS.computeIfAbsent(player.getUniqueId(), id -> new Session(player));
    }

    private static void joined(Player player) {
        Session session = session(player);
        Channel channel = channel(player);
        if (channel != null) {
            CHANNELS.put(channel.id().asLongText(), player.getUniqueId());
            Hold hold = channel.attr(HOLD).get();
            if (hold != null) {
                session.hold = hold;
            }
            channel.eventLoop().execute(() -> place(channel));
        }
        session.uuidForm = player.getUniqueId().getMostSignificantBits() == 0 ? Tri.YES : Tri.NO;
        session.listed = listed(player.getUniqueId());
        session.joined = true;
        session.writeJoin();
        SmthsHubPlugin.scheduler().runLater(task -> inject(player), 40L);
    }

    private static void left(UUID uuid) {
        Session session = SESSIONS.remove(uuid);
        if (session != null && session.channelId != null) {
            CHANNELS.remove(session.channelId);
        }
    }

    private static void inject(Player player) {
        if (!ACTIVE.get() || !player.isOnline()) {
            return;
        }
        Session session = SESSIONS.get(player.getUniqueId());
        Channel channel = channel(player);
        if (session == null || channel == null) {
            return;
        }
        Integer packetId = packetId(channel);
        if (packetId == null) {
            session.probeInjected = Tri.UNKNOWN;
            session.line("selftest", PROBE, word(Tri.UNKNOWN), Tri.UNKNOWN);
            return;
        }
        session.probeInjected = Tri.YES;
        channel.eventLoop().execute(() -> {
            ByteBuf buf = channel.alloc().buffer();
            try {
                writeVarInt(buf, packetId);
                byte[] name = PROBE.getBytes(StandardCharsets.UTF_8);
                writeVarInt(buf, name.length);
                buf.writeBytes(name);
                buf.writeByte(7);
                String anchor = channel.pipeline().get("decompress") != null ? "decompress" : "splitter";
                if (channel.pipeline().get(anchor) == null) {
                    buf.release();
                    QUEUE.add(() -> session.line("selftest", PROBE, word(Tri.UNKNOWN), Tri.UNKNOWN));
                    return;
                }
                channel.pipeline().context(anchor).fireChannelRead(buf);
            } catch (Throwable ex) {
                buf.release();
                QUEUE.add(() -> session.line("selftest", PROBE, word(Tri.UNKNOWN), Tri.UNKNOWN));
            }
        });
    }

    private static void readKey() {
        fingerprintKnown = false;
        fingerprint = "";
        try {
            java.io.File dir = new java.io.File(hub.getDataFolder().getParentFile(), "floodgate");
            java.io.File config = new java.io.File(dir, "config.yml");
            String keyName = "key.pem";
            if (config.isFile()) {
                String found = YamlConfiguration.loadConfiguration(config).getString("key-file-name");
                if (found != null && !found.isEmpty()) {
                    keyName = new java.io.File(found).getName();
                }
            }
            java.io.File key = new java.io.File(dir, keyName);
            if (!key.isFile()) {
                return;
            }
            String prefix = Fingerprint.prefix(Files.readAllBytes(key.toPath()));
            if (prefix != null) {
                fingerprint = prefix;
                fingerprintKnown = true;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void registerProbe() {
        if (!hub.getServer().getMessenger().isIncomingChannelRegistered(hub, PROBE)) {
            hub.getServer().getMessenger().registerIncomingPluginChannel(hub, PROBE, PROBE_LISTENER);
        }
    }

    private static void installServer() {
        paperInit(false);
        for (Channel parent : serverChannels()) {
            try {
                if (parent.pipeline().get(INIT) == null) {
                    parent.pipeline().addLast(INIT, new Init());
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void installBridges() {
        peInstalled = false;
        plInstalled = false;
        Plugin packetEvents = hub.getServer().getPluginManager().getPlugin("packetevents");
        if (packetEvents != null && packetEvents.isEnabled()) {
            try {
                PeHooks.register();
                peInstalled = true;
            } catch (Throwable ignored) {
            }
        }
        Plugin protocol = hub.getServer().getPluginManager().getPlugin("ProtocolLib");
        if (protocol != null && protocol.isEnabled()) {
            try {
                PlHooks.register(hub);
                plInstalled = true;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void paperInit(boolean remove) {
        try {
            Class<?> holder = Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder");
            if (remove) {
                holder.getMethod("removeListener", Key.class).invoke(null, INIT_KEY);
                return;
            }
            Object has = holder.getMethod("hasListener", Key.class).invoke(null, INIT_KEY);
            if (Boolean.TRUE.equals(has)) {
                return;
            }
            Class<?> listener = Class.forName("io.papermc.paper.network.ChannelInitializeListener");
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[] {listener}, (unused, method, args) -> {
                if ("afterInitChannel".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof Channel channel) {
                    attachHandshake(channel);
                    return null;
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(unused);
                }
                if ("equals".equals(method.getName())) {
                    return unused == (args == null ? null : args[0]);
                }
                if ("toString".equals(method.getName())) {
                    return INIT;
                }
                return null;
            });
            holder.getMethod("addListener", Key.class, listener).invoke(null, INIT_KEY, proxy);
        } catch (Throwable ignored) {
        }
    }

    private static void attachHandshake(Channel channel) {
        if (!ACTIVE.get() || channel == null) {
            return;
        }
        Runnable place = () -> {
            try {
                if (channel.pipeline().get(HANDSHAKE) != null || channel.pipeline().get("splitter") == null) {
                    return;
                }
                if (channel.pipeline().get("floodgate_data_handler") != null) {
                    channel.pipeline().addBefore("floodgate_data_handler", HANDSHAKE, new Handshake());
                } else {
                    channel.pipeline().addAfter("splitter", HANDSHAKE, new Handshake());
                }
            } catch (Throwable ignored) {
            }
        };
        if (channel.eventLoop().inEventLoop()) {
            channel.eventLoop().execute(place);
        } else {
            channel.eventLoop().execute(place);
        }
    }

    private static void place(Channel channel) {
        try {
            String inbound = channel.pipeline().get("decompress") != null ? "decompress" : "splitter";
            move(channel, R1, inbound, new Raw());
            if (channel.pipeline().get("decoder") != null) {
                move(channel, R4, "decoder", new Decoded());
            }
            if (channel.pipeline().get("encoder") != null) {
                moveBefore(channel, OUT, "encoder", new Encoded());
            }
        } catch (Throwable ignored) {
        }
    }

    private static void move(Channel channel, String name, String after, io.netty.channel.ChannelHandler handler) {
        if (channel.pipeline().get(after) == null) {
            return;
        }
        List<String> names = channel.pipeline().names();
        int anchor = names.indexOf(after);
        int mine = names.indexOf(name);
        if (mine == anchor + 1) {
            return;
        }
        if (mine >= 0) {
            channel.pipeline().remove(name);
        }
        channel.pipeline().addAfter(after, name, handler);
    }

    private static void moveBefore(Channel channel, String name, String before, io.netty.channel.ChannelHandler handler) {
        List<String> names = channel.pipeline().names();
        int anchor = names.indexOf(before);
        int mine = names.indexOf(name);
        if (anchor < 0) {
            return;
        }
        if (mine == anchor - 1) {
            return;
        }
        if (mine >= 0) {
            channel.pipeline().remove(name);
        }
        channel.pipeline().addBefore(before, name, handler);
    }

    private static void repositionOnline() {
        if (!ACTIVE.get()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Channel channel = channel(player);
            if (channel != null) {
                channel.eventLoop().execute(() -> place(channel));
            }
        }
    }

    private static void remove(Channel channel, String name) {
        try {
            if (channel != null && channel.pipeline().get(name) != null) {
                channel.pipeline().remove(name);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void inbound(Channel channel, byte[] data) {
        UUID uuid = CHANNELS.get(channel.id().asLongText());
        if (uuid == null) {
            return;
        }
        String custom = FrameScan.customChannel(data);
        FrameScan.Reply form = FrameScan.reply(data, FORM);
        FrameScan.Reply probe = FrameScan.reply(data, PROBE);
        QUEUE.add(() -> {
            Session session = SESSIONS.get(uuid);
            if (session == null) {
                return;
            }
            if (custom != null) {
                session.r1Counts.merge(custom, 1, Integer::sum);
            }
            if (form != null) {
                session.raw = Tri.YES;
                session.formR1 = true;
                session.line("R1", FORM, form.idBytes() == 2 ? Integer.toString(form.id()) : word(Tri.UNKNOWN), Tri.UNKNOWN);
                session.followRaw();
            }
            if (probe != null) {
                session.probeRaw = Tri.YES;
                session.line("R1", PROBE, "", Tri.UNKNOWN);
            }
        });
    }

    private static void outbound(Channel channel, byte[] data) {
        UUID uuid = CHANNELS.get(channel.id().asLongText());
        if (uuid == null) {
            return;
        }
        FrameScan.Send send = FrameScan.send(data, FORM);
        if (send == null) {
            return;
        }
        QUEUE.add(() -> {
            Session session = SESSIONS.get(uuid);
            if (session == null) {
                return;
            }
            session.wireType = send.type();
            session.wireBytes = send.afterChannel();
            if (send.idBytes() == 2) {
                session.wireId = send.id();
            }
            session.line("C", FORM, "tip=" + (send.type() < 0 ? word(Tri.UNKNOWN) : send.type())
                    + " id=" + (send.idBytes() == 2 ? send.id() : word(Tri.UNKNOWN))
                    + " boyut=" + send.afterChannel(), Tri.UNKNOWN);
        });
    }

    private static void decoded(Channel channel, Object packet) {
        UUID uuid = CHANNELS.get(channel.id().asLongText());
        if (uuid == null) {
            return;
        }
        String type = packet.getClass().getName();
        String channelName = payloadChannel(packet);
        QUEUE.add(() -> {
            Session session = SESSIONS.get(uuid);
            if (session == null) {
                return;
            }
            if (FORM.equals(channelName)) {
                session.decoded = Tri.YES;
                session.line("R4", FORM, type, Tri.UNKNOWN);
            } else if (PROBE.equals(channelName)) {
                session.probeDecoded = Tri.YES;
                session.line("R4", PROBE, type, Tri.UNKNOWN);
            } else if (type.contains("CustomPayload") || type.contains("Payload")) {
                session.decodedUnread = true;
                session.line("R4", channelName == null ? word(Tri.UNKNOWN) : channelName, type, Tri.UNKNOWN);
            }
        });
    }

    private static String payloadChannel(Object packet) {
        try {
            Object payload = packet.getClass().getMethod("payload").invoke(packet);
            if (payload == null) {
                return null;
            }
            Object id;
            try {
                id = payload.getClass().getMethod("id").invoke(payload);
            } catch (NoSuchMethodException ex) {
                id = payload.getClass().getMethod("type").invoke(payload);
            }
            return id == null ? null : String.valueOf(id);
        } catch (Throwable ex) {
            return null;
        }
    }

    private static byte[] copy(ByteBuf buf) {
        int size = Math.min(buf.readableBytes(), 8192);
        if (size <= 0) {
            return new byte[0];
        }
        byte[] data = new byte[size];
        buf.getBytes(buf.readerIndex(), data);
        return data;
    }

    private static byte[] copyHandshake(ByteBuf buf) {
        int size = Math.min(buf.readableBytes(), 65535);
        byte[] data = new byte[size];
        if (size > 0) {
            buf.getBytes(buf.readerIndex(), data);
        }
        return data;
    }

    private static Tri listed(UUID uuid) {
        Plugin plugin = hub.getServer().getPluginManager().getPlugin("floodgate");
        if (plugin == null || !plugin.isEnabled()) {
            return Tri.NO;
        }
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, plugin.getClass().getClassLoader());
            Object instance = api.getMethod("getInstance").invoke(null);
            Object player = api.getMethod("getPlayer", UUID.class).invoke(instance, uuid);
            return player == null ? Tri.NO : Tri.YES;
        } catch (Throwable ex) {
            return Tri.UNKNOWN;
        }
    }

    private static Channel channel(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object listener = typed(handle, "PacketListener");
            if (listener == null) {
                return null;
            }
            Object connection = typed(listener, "Connection");
            if (connection == null) {
                return null;
            }
            Object channel = typed(connection, "Channel");
            return channel instanceof Channel found ? found : null;
        } catch (Throwable ex) {
            return null;
        }
    }

    private static Object typed(Object target, String simpleName) {
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().getSimpleName().contains(simpleName) && !simpleName.equals("Channel")) {
                    continue;
                }
                if ("Channel".equals(simpleName) && !io.netty.channel.Channel.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                if (!"Channel".equals(simpleName) && !field.getType().getSimpleName().contains(simpleName)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value != null) {
                        return value;
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static List<Channel> serverChannels() {
        List<Channel> found = new ArrayList<>();
        try {
            Object server = hub.getServer().getClass().getMethod("getServer").invoke(hub.getServer());
            Object connection = typed(server, "ServerConnectionListener");
            if (connection == null) {
                return found;
            }
            Class<?> type = connection.getClass();
            while (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    if (!List.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    if (!field.getGenericType().getTypeName().contains("ChannelFuture")) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(connection);
                    if (value instanceof List<?> list) {
                        for (Object item : list) {
                            Object channel = item.getClass().getMethod("channel").invoke(item);
                            if (channel instanceof Channel parent) {
                                found.add(parent);
                            }
                        }
                    }
                }
                type = type.getSuperclass();
            }
        } catch (Throwable ignored) {
        }
        return found;
    }

    private static Integer packetId(Channel channel) {
        try {
            Object decoder = channel.pipeline().get("decoder");
            if (decoder == null) {
                return null;
            }
            Object protocol = fieldValue(decoder, "protocolInfo");
            if (protocol == null) {
                return null;
            }
            Object codec = protocol.getClass().getMethod("codec").invoke(protocol);
            Object map = fieldValue(codec, "toId");
            if (map == null) {
                return null;
            }
            Object keys = map.getClass().getMethod("keySet").invoke(map);
            if (!(keys instanceof Iterable<?> iterable)) {
                return null;
            }
            for (Object key : iterable) {
                String text = String.valueOf(key);
                if (text.contains("serverbound") && text.contains("custom_payload")) {
                    Object id = map.getClass().getMethod("getInt", Object.class).invoke(map, key);
                    if (id instanceof Integer value && value >= 0) {
                        return value;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object fieldValue(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ex) {
                type = type.getSuperclass();
            } catch (Throwable ex) {
                return null;
            }
        }
        return null;
    }

    private static void writeVarInt(ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    private static String word(Tri value) {
        if (value == Tri.YES) {
            return Messages.lookup("DEBUG.YES");
        }
        if (value == Tri.NO) {
            return Messages.lookup("DEBUG.NO");
        }
        return Messages.lookup("DEBUG.UNKNOWN");
    }

    private static String trace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static String pluginVersion(String name) {
        Plugin plugin = hub.getServer().getPluginManager().getPlugin(name);
        if (plugin == null) {
            return Messages.lookup("DEBUG.NONE");
        }
        try {
            return plugin.getDescription().getVersion();
        } catch (Throwable ex) {
            return Messages.lookup("DEBUG.UNKNOWN");
        }
    }

    private static String owner(Class<?> type) {
        try {
            return JavaPlugin.getProvidingPlugin(type).getName();
        } catch (Throwable ex) {
            return Messages.lookup("DEBUG.UNKNOWN");
        }
    }

    private record Hold(HandshakeScan.Result result, Tri before, String pipeline) {
    }

    private static final class Joins implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            joined(event.getPlayer());
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            left(event.getPlayer().getUniqueId());
        }
    }

    private static final class Init extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                if (msg instanceof Channel child) {
                    attachHandshake(child);
                }
            } catch (Throwable ignored) {
            }
            ctx.fireChannelRead(msg);
        }
    }

    private static final class Handshake extends ChannelInboundHandlerAdapter {
        private boolean done;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                if (!done && msg instanceof ByteBuf buf) {
                    done = true;
                    HandshakeScan.Result result = HandshakeScan.read(copyHandshake(buf));
                    List<String> names = ctx.pipeline().names();
                    int mine = names.indexOf(HANDSHAKE);
                    int gate = names.indexOf("floodgate_data_handler");
                    Tri before = gate < 0 ? Tri.UNKNOWN : mine >= 0 && mine < gate ? Tri.YES : Tri.NO;
                    ctx.channel().attr(HOLD).set(new Hold(result, before, String.join(" > ", names)));
                }
            } catch (Throwable ignored) {
            }
            ctx.fireChannelRead(msg);
            if (done) {
                try {
                    ctx.pipeline().remove(this);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static final class Raw extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                if (msg instanceof ByteBuf buf) {
                    inbound(ctx.channel(), copy(buf));
                }
            } catch (Throwable ignored) {
            }
            ctx.fireChannelRead(msg);
        }
    }

    private static final class Decoded extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                if (msg != null && !(msg instanceof ByteBuf)) {
                    decoded(ctx.channel(), msg);
                }
            } catch (Throwable ignored) {
            }
            ctx.fireChannelRead(msg);
        }
    }

    private static final class Encoded extends ChannelDuplexHandler {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            try {
                if (msg instanceof ByteBuf buf) {
                    outbound(ctx.channel(), copy(buf));
                }
            } catch (Throwable ignored) {
            }
            ctx.write(msg, promise);
        }
    }

    private static final class Session {
        private final UUID uuid;
        private final String name;
        private final long opened = System.currentTimeMillis();
        private final Map<String, Integer> r1Counts = new ConcurrentHashMap<>();
        private final Map<String, Integer> r7Counts = new ConcurrentHashMap<>();
        private final List<String> hops = new ArrayList<>();
        private String channelId;
        private Hold hold;
        private Tri uuidForm = Tri.UNKNOWN;
        private Tri listed = Tri.UNKNOWN;
        private Tri formSent = Tri.UNKNOWN;
        private Tri raw = Tri.UNKNOWN;
        private Tri decoded = Tri.UNKNOWN;
        private Tri act = Tri.UNKNOWN;
        private Tri actionFailed = Tri.UNKNOWN;
        private Tri peCancel = Tri.UNKNOWN;
        private Tri plCancel = Tri.UNKNOWN;
        private Tri probeRaw = Tri.UNKNOWN;
        private Tri probeDecoded = Tri.UNKNOWN;
        private Tri probeBukkit = Tri.UNKNOWN;
        private Tri probeInjected = Tri.NO;
        private boolean joined;
        private boolean formR1;
        private boolean decodedUnread;
        private boolean peSaw;
        private boolean plSaw;
        private boolean followup;
        private boolean formReported;
        private boolean joinReported;
        private String formPath;
        private Integer sentId;
        private Integer wireId;
        private int wireType = -1;
        private int wireBytes = -1;
        private String decision;
        private Integer responseId;
        private Integer pendingId;
        private String stack;
        private java.io.File file;

        private Session(Player player) {
            this.uuid = player.getUniqueId();
            this.name = player.getName();
            Channel channel = channel(player);
            if (channel != null) {
                this.channelId = channel.id().asLongText();
            }
            java.io.File dir = new java.io.File(hub.storageFolder(), "debug");
            dir.mkdirs();
            file = new java.io.File(dir, uuid + "-" + STAMP.format(Instant.ofEpochMilli(opened)) + ".log");
        }

        private void form(String path, Integer id) {
            formPath = path;
            sentId = id;
            formSent = Tri.YES;
            line("C", FORM, "yol=" + path + " id=" + (id == null ? word(Tri.UNKNOWN) : id), Tri.UNKNOWN);
            SmthsHubPlugin.scheduler().runLater(task -> {
                if (!formReported) {
                    if (raw == Tri.UNKNOWN) {
                        raw = Tri.NO;
                    }
                    emitForm();
                }
            }, 600L);
        }

        private void packetEvents(String channel, boolean cancelled, boolean monitor) {
            if (!FORM.equals(channel) && !PROBE.equals(channel)) {
                return;
            }
            line(monitor ? "R3" : "R2", channel, "", cancelled ? Tri.YES : Tri.NO);
            if (FORM.equals(channel)) {
                peSaw = true;
                if (monitor) {
                    peCancel = cancelled ? Tri.YES : Tri.NO;
                }
            }
        }

        private void protocolLib(String channel, boolean cancelled, boolean monitor) {
            if (!FORM.equals(channel) && !PROBE.equals(channel)) {
                return;
            }
            line(monitor ? "R6" : "R5", channel, "", cancelled ? Tri.YES : Tri.NO);
            if (FORM.equals(channel)) {
                plSaw = true;
                if (monitor) {
                    plCancel = cancelled ? Tri.YES : Tri.NO;
                }
            }
        }

        private void followRaw() {
            if (followup) {
                return;
            }
            followup = true;
            SmthsHubPlugin.scheduler().runLater(task -> {
                if (!formReported) {
                    emitForm();
                }
            }, 40L);
        }

        private void line(String hop, String channel, String detail, Tri cancel) {
            String text = name + " " + hop + " " + channel
                    + (detail == null || detail.isEmpty() ? "" : " " + detail)
                    + (cancel == Tri.UNKNOWN ? "" : " iptal=" + word(cancel));
            hops.add(text);
            say(text);
        }

        private void writeJoin() {
            if (joinReported) {
                return;
            }
            joinReported = true;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                listed = listed(uuid);
            }
            DebugVerdict.Judgment judgment = DebugVerdict.handshake(uuidForm, marker(), listed);
            String report = report(judgment, false);
            publish(report);
        }

        private void emitForm() {
            flush();
            if (formReported) {
                return;
            }
            formReported = true;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                listed = listed(uuid);
            }
            closeGaps();
            DebugVerdict.Judgment judgment = DebugVerdict.judge(facts());
            publish(report(judgment, true));
        }

        private void closeGaps() {
            if (!peInstalled) {
                peCancel = Tri.NO;
            } else if (!peSaw && raw == Tri.YES) {
                peCancel = Tri.UNKNOWN;
            } else if (!peSaw && raw == Tri.NO) {
                peCancel = Tri.NO;
            }
            if (decoded == Tri.UNKNOWN && !decodedUnread && raw == Tri.YES && peCancel == Tri.NO) {
                decoded = Tri.NO;
            }
            if (!plInstalled) {
                plCancel = Tri.NO;
            } else if (!plSaw && decoded == Tri.YES) {
                plCancel = Tri.UNKNOWN;
            } else if (!plSaw) {
                plCancel = Tri.NO;
            }
        }

        private DebugVerdict.Facts facts() {
            if (!peInstalled) {
                peCancel = Tri.NO;
            }
            if (!plInstalled) {
                plCancel = Tri.NO;
            }
            Tri bukkit;
            if (r7Counts.getOrDefault(FORM, 0) > 0) {
                bukkit = Tri.YES;
            } else if (decoded == Tri.YES && plCancel == Tri.NO) {
                bukkit = Tri.NO;
            } else {
                bukkit = Tri.UNKNOWN;
            }
            return new DebugVerdict.Facts(uuidForm, marker(), listed, formSent, raw, peCancel, decoded, plCancel, bukkit, act, actionFailed);
        }

        private Tri marker() {
            if (hold == null || hold.result == null || !hold.result.parsed()) {
                return Tri.UNKNOWN;
            }
            return hold.result.marker() ? Tri.YES : Tri.NO;
        }

        private void publish(String report) {
            say(report);
            try {
                Files.writeString(file.toPath(), report + System.lineSeparator(), StandardCharsets.UTF_8,
                        file.exists() ? java.nio.file.StandardOpenOption.CREATE : java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (Throwable ignored) {
            }
        }

        private String report(DebugVerdict.Judgment judgment, boolean form) {
            StringBuilder out = new StringBuilder();
            out.append(name).append(' ').append(uuid).append('\n');
            out.append("smthsHub ").append(hub.getDescription().getVersion())
                    .append(" server ").append(Bukkit.getVersion())
                    .append(" floodgate ").append(pluginVersion("floodgate"))
                    .append(" packetevents ").append(pluginVersion("packetevents"))
                    .append(" ProtocolLib ").append(pluginVersion("ProtocolLib"))
                    .append('\n');
            out.append(Messages.lookup("DEBUG.HEAD_H")).append('\n');
            if (hold == null || hold.result == null || !hold.result.parsed()) {
                out.append(Messages.lookup("DEBUG.REJOIN")).append('\n');
            } else {
                HandshakeScan.Result result = hold.result;
                out.append("host=").append(result.hostLength())
                        .append(" parca=").append(result.parts())
                        .append(" isaretci=").append(word(result.marker() ? Tri.YES : Tri.NO))
                        .append(" surum=").append(result.version() < 0 ? word(Tri.UNKNOWN) : result.version())
                        .append(" floodgate_oncesi=").append(word(hold.before))
                        .append('\n');
                out.append(hold.pipeline).append('\n');
            }
            out.append(Messages.lookup("DEBUG.HEAD_F")).append('\n');
            BedrockMenus.Status status = null;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                try {
                    status = BedrockMenus.status(player);
                } catch (Throwable ignored) {
                }
            }
            out.append("uuid=").append(word(uuidForm))
                    .append(" listeli=").append(word(listed))
                    .append(" floodgate_dinleyici=").append(status == null ? word(Tri.UNKNOWN) : String.join(", ", status.plugins()))
                    .append(" bizde=").append(status == null ? word(Tri.UNKNOWN) : word(status.holding() ? Tri.YES : Tri.NO))
                    .append('\n');
            if (fingerprintKnown) {
                out.append(Messages.lookup("DEBUG.FINGERPRINT", "%fingerprint%", fingerprint)).append('\n');
            } else {
                out.append(Messages.lookup("DEBUG.FINGERPRINT", "%fingerprint%", word(Tri.UNKNOWN))).append('\n');
            }
            out.append(Messages.lookup("DEBUG.HEAD_C")).append('\n');
            out.append("yol=").append(formPath == null ? word(Tri.UNKNOWN) : formPath)
                    .append(" id=").append(sentId == null ? word(Tri.UNKNOWN) : sentId)
                    .append(" tel_tip=").append(wireType < 0 ? word(Tri.UNKNOWN) : wireType)
                    .append(" tel_id=").append(wireId == null ? word(Tri.UNKNOWN) : wireId)
                    .append(" tel_boyut=").append(wireBytes < 0 ? word(Tri.UNKNOWN) : wireBytes)
                    .append('\n');
            out.append(Messages.lookup("DEBUG.HEAD_R")).append('\n');
            if (hops.isEmpty()) {
                out.append(word(Tri.UNKNOWN)).append('\n');
            } else {
                for (String hop : hops) {
                    out.append(hop).append('\n');
                }
            }
            out.append("R1 ");
            if (r1Counts.isEmpty()) {
                out.append(word(Tri.UNKNOWN)).append('\n');
            } else {
                r1Counts.forEach((channel, count) -> out.append(channel).append('=').append(count).append(' '));
                out.append('\n');
            }
            out.append("R7 ");
            if (r7Counts.isEmpty()) {
                out.append(Messages.lookup("DEBUG.NONE")).append('\n');
            } else {
                r7Counts.forEach((channel, count) -> out.append(channel).append('=').append(count).append(' '));
                out.append('\n');
            }
            out.append(Messages.lookup("DEBUG.HEAD_SELF")).append('\n');
            out.append("enjekte=").append(word(probeInjected))
                    .append(" R1=").append(word(probeRaw))
                    .append(" R4=").append(word(probeDecoded))
                    .append(" R7=").append(word(probeBukkit))
                    .append('\n');
            if (DebugVerdict.channelFilter(probeBukkit, r7Counts.getOrDefault(FORM, 0) > 0 ? Tri.YES : Tri.NO) && formSent == Tri.YES) {
                out.append(Messages.lookup("DEBUG.FILTER")).append('\n');
            }
            Channel channel = player == null ? null : channel(player);
            List<String> names = channel == null ? List.of() : channel.pipeline().names();
            out.append(Messages.lookup("DEBUG.HEAD_PIPE")).append('\n');
            if (names.isEmpty()) {
                out.append(word(Tri.UNKNOWN)).append('\n');
            } else {
                for (String pipe : names) {
                    io.netty.channel.ChannelHandler handler = channel.pipeline().get(pipe);
                    String type = handler == null ? word(Tri.UNKNOWN) : handler.getClass().getName();
                    String plugin = handler == null ? word(Tri.UNKNOWN) : owner(handler.getClass());
                    out.append(pipe).append(' ').append(type).append(' ').append(plugin).append('\n');
                }
            }
            out.append("R1-decoder ");
            out.append(names.isEmpty() ? word(Tri.UNKNOWN) : slice(names, channel.pipeline().get("decompress") != null ? "decompress" : "splitter", "decoder")).append('\n');
            out.append("decoder-packet_handler ");
            out.append(names.isEmpty() ? word(Tri.UNKNOWN) : slice(names, "decoder", "packet_handler")).append('\n');
            out.append(Messages.lookup("DEBUG.HEAD_PE")).append('\n');
            out.append(peInstalled ? textOrUnknown(peDump()) : Messages.lookup("DEBUG.NONE")).append('\n');
            out.append(Messages.lookup("DEBUG.HEAD_PL")).append('\n');
            out.append(plInstalled ? textOrUnknown(plDump()) : Messages.lookup("DEBUG.NONE")).append('\n');
            out.append(Messages.lookup("DEBUG.HEAD_PLUGINS")).append('\n');
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                out.append(plugin.getName()).append(' ').append(plugin.getDescription().getVersion()).append('\n');
            }
            out.append(Messages.lookup("DEBUG.HEAD_ANTI")).append('\n');
            StringBuilder anti = new StringBuilder();
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                String lower = plugin.getName().toLowerCase();
                for (String token : ANTI) {
                    if (lower.contains(token)) {
                        anti.append(plugin.getName()).append(' ');
                        break;
                    }
                }
            }
            out.append(anti.length() == 0 ? Messages.lookup("DEBUG.NONE") : anti.toString().trim()).append('\n');
            String sentence = Messages.lookup("DEBUG.VERDICT." + judgment.verdict().name(),
                    "%field%", judgment.blocked(),
                    "%response%", responseId == null ? word(Tri.UNKNOWN) : responseId.toString(),
                    "%pending%", pendingId == null ? word(Tri.UNKNOWN) : pendingId.toString());
            if (sentence.isEmpty()) {
                sentence = word(Tri.UNKNOWN);
            }
            out.append(name).append(" HÜKÜM ").append(judgment.verdict().name()).append(' ').append(sentence).append('\n');
            if (form && formSent == Tri.YES && (judgment.verdict() == Verdict.CEVAP_BACKENDE_ULASMADI || raw != Tri.YES)) {
                out.append(Messages.lookup("DEBUG.PRESS")).append('\n');
            }
            if (stack != null) {
                out.append(stack).append('\n');
            }
            out.append(Messages.lookup("DEBUG.FILE", "%file%", file.getAbsolutePath())).append('\n');
            return out.toString().trim();
        }
    }

    private static String slice(List<String> names, String from, String to) {
        int start = names.indexOf(from);
        int end = names.indexOf(to);
        if (start < 0 || end < 0 || end <= start) {
            return Messages.lookup("DEBUG.UNKNOWN");
        }
        if (end == start + 1) {
            return Messages.lookup("DEBUG.NONE");
        }
        return String.join(", ", names.subList(start + 1, end));
    }

    private static String peDump() {
        try {
            return PeHooks.dump();
        } catch (Throwable ex) {
            return null;
        }
    }

    private static String plDump() {
        try {
            return PlHooks.dump();
        } catch (Throwable ex) {
            return null;
        }
    }

    private static String textOrUnknown(String text) {
        return text == null || text.isEmpty() ? Messages.lookup("DEBUG.UNKNOWN") : text;
    }
}
