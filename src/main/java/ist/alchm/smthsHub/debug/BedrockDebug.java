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
import java.util.concurrent.CopyOnWriteArrayList;
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
    private static UUID only;
    private static String fingerprint = "";
    private static boolean fingerprintKnown;
    private static boolean peInstalled;
    private static boolean plInstalled;
    private static String peHook = "";
    private static String plHook = "";
    private static String lastRegs = "";
    private static final List<String> REG_EVENTS = new CopyOnWriteArrayList<>();
    private static final PluginMessageListener PROBE_LISTENER = (channel, player, message) -> {
        if (PROBE.equals(channel) && player != null) {
            UUID uuid = player.getUniqueId();
            QUEUE.add(() -> {
                Session session = SESSIONS.get(uuid);
                if (session != null) {
                    session.probeBukkit = Tri.YES;
                    session.line("R7", PROBE, "t=" + System.currentTimeMillis(), Tri.UNKNOWN);
                }
            });
        }
    };
    private static final PluginMessageListener FORM_LISTENER = (channel, player, message) -> {
        if (!FORM.equals(channel) || player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        byte[] copy = message == null ? null : message.clone();
        long time = System.currentTimeMillis();
        QUEUE.add(() -> {
            Session session = SESSIONS.get(uuid);
            if (session == null) {
                return;
            }
            session.r7Counts.merge(FORM, 1, Integer::sum);
            String text = FormTrace.line(time, copy);
            session.hops.add(text);
            say(text);
            if (copy != null && copy.length >= 2) {
                session.r7Id = ((copy[0] & 0xFF) << 8) | (copy[1] & 0xFF);
            }
        });
    };

    private BedrockDebug() {
    }

    public static boolean active() {
        return ACTIVE.get();
    }

    public static void bind(SmthsHubPlugin plugin) {
        hub = plugin;
    }

    public static void on(UUID uuid) {
        only = uuid;
        if (ACTIVE.get()) {
            return;
        }
        ACTIVE.set(true);
        readKey();
        installServer();
        installBridges();
        arm();
        say(plain("DEBUG.ARMED", "debug acik"));
    }

    public static void off() {
        stop();
        say(plain("DEBUG.STOPPED", "debug kapali"));
    }

    public static void selftest(Player player) {
        inject(player);
    }

    public static String report(Player player) {
        if (!ACTIVE.get()) {
            return ReportText.field("debug", "", unknown(), "debug kapali");
        }
        flush();
        Session session = SESSIONS.get(player.getUniqueId());
        if (session == null) {
            return ReportText.field("oturum", "", unknown(), "oyuncu debug acildiktan sonra girmedi");
        }
        String text = session.report(DebugVerdict.judge(session.facts()), true);
        session.publish(text);
        return text;
    }

    public static void arm() {
        if (!ACTIVE.get() || hub == null) {
            return;
        }
        registerChannels();
        if (joins != null) {
            org.bukkit.event.HandlerList.unregisterAll(joins);
        }
        joins = new Joins();
        hub.getServer().getPluginManager().registerEvents(joins, hub);
        SmthsHubPlugin.scheduler().runTimer(task -> {
            flush();
            watchRegs();
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
            try {
                hub.getServer().getMessenger().unregisterIncomingPluginChannel(hub, FORM);
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
        lastRegs = "";
    }

    public static void formApi(Player player) {
        if (!ACTIVE.get()) {
            return;
        }
        session(player).form("API", null);
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

    public static void actionRan(Player player) {
        if (!ACTIVE.get()) {
            return;
        }
        Session session = session(player);
        session.action = Tri.YES;
        session.actionFailed = Tri.NO;
        QUEUE.add(session::emitForm);
    }

    public static void actionFailed(Player player, Throwable error) {
        if (!ACTIVE.get()) {
            return;
        }
        Session session = session(player);
        session.action = Tri.NO;
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
        if (only != null && !only.equals(player.getUniqueId())) {
            return;
        }
        Session session = session(player);
        List<FieldWalk.Miss> misses = new ArrayList<>();
        String[] step = new String[] {""};
        Channel channel = channel(player, misses, step);
        session.channelReason = FieldWalk.text(misses);
        session.channelStep = step[0];
        if (channel != null) {
            CHANNELS.put(channel.id().asLongText(), player.getUniqueId());
            Hold hold = channel.attr(HOLD).get();
            if (hold != null) {
                session.hold = hold;
            }
            channel.eventLoop().execute(() -> place(channel));
        }
        session.uuidForm = player.getUniqueId().getMostSignificantBits() == 0 ? Tri.YES : Tri.NO;
        Listing listing = listing(player.getUniqueId());
        session.listed = listing.value;
        session.listedReason = listing.reason;
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
        List<FieldWalk.Miss> misses = new ArrayList<>();
        Channel channel = channel(player, misses, new String[1]);
        if (session == null) {
            return;
        }
        if (channel == null) {
            session.probeInjected = Tri.UNKNOWN;
            session.injectReason = "selftest kanal " + FieldWalk.text(misses);
            session.line("selftest", PROBE, session.injectReason, Tri.UNKNOWN);
            return;
        }
        Integer packetId = packetId(channel);
        if (packetId == null) {
            session.probeInjected = Tri.UNKNOWN;
            session.injectReason = "selftest packetId decoder.protocolInfo.codec.toId custom_payload yok";
            session.line("selftest", PROBE, session.injectReason, Tri.UNKNOWN);
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
                    QUEUE.add(() -> {
                        session.injectReason = "selftest " + anchor + " handler yok";
                        session.line("selftest", PROBE, session.injectReason, Tri.UNKNOWN);
                    });
                    return;
                }
                channel.pipeline().context(anchor).fireChannelRead(buf);
            } catch (Throwable ex) {
                buf.release();
                QUEUE.add(() -> {
                    session.injectReason = "selftest " + FieldWalk.cause(ex);
                    session.line("selftest", PROBE, session.injectReason, Tri.UNKNOWN);
                });
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

    private static void registerChannels() {
        var messenger = hub.getServer().getMessenger();
        if (!messenger.isIncomingChannelRegistered(hub, PROBE)) {
            messenger.registerIncomingPluginChannel(hub, PROBE, PROBE_LISTENER);
        }
        if (!messenger.isIncomingChannelRegistered(hub, FORM)) {
            messenger.registerIncomingPluginChannel(hub, FORM, FORM_LISTENER);
        }
    }

    private static void watchRegs() {
        if (!ACTIVE.get() || hub == null) {
            return;
        }
        String now = regSnapshot();
        if (lastRegs.isEmpty()) {
            lastRegs = now;
            return;
        }
        if (lastRegs.equals(now)) {
            return;
        }
        String event = System.currentTimeMillis() + " " + lastRegs + " -> " + now;
        REG_EVENTS.add(event);
        say(event);
        lastRegs = now;
    }

    private static String regSnapshot() {
        List<String> names = new ArrayList<>();
        try {
            for (org.bukkit.plugin.messaging.PluginMessageListenerRegistration registration : hub.getServer().getMessenger().getIncomingChannelRegistrations(FORM)) {
                names.add(registration.getPlugin().getName() + ":" + registration.getListener().getClass().getName());
            }
        } catch (Throwable ex) {
            return "olculemedi(" + FieldWalk.cause(ex) + ")";
        }
        names.sort(String::compareTo);
        return names.isEmpty() ? "yok" : String.join(", ", names);
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
                peHook = "";
            } catch (Throwable ex) {
                peHook = "PeHooks.register " + FieldWalk.cause(ex);
            }
        } else {
            peHook = "PacketEvents plugin yok";
        }
        Plugin protocol = hub.getServer().getPluginManager().getPlugin("ProtocolLib");
        if (protocol != null && protocol.isEnabled()) {
            try {
                PlHooks.register(hub);
                plInstalled = true;
                plHook = "";
            } catch (Throwable ex) {
                plHook = "PlHooks.register " + FieldWalk.cause(ex);
            }
        } else {
            plHook = "ProtocolLib plugin yok";
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
            session.frames++;
            if (custom != null) {
                session.r1Counts.merge(custom, 1, Integer::sum);
                if (session.formAt > 0) {
                    session.line("R1", custom, "t=" + System.currentTimeMillis(), Tri.UNKNOWN);
                }
            }
            if (form != null) {
                session.raw = Tri.YES;
                session.formR1 = true;
                if (form.idBytes() == 2) {
                    session.replyWireId = form.id();
                }
                String idText = form.idBytes() == 2 ? Integer.toString(form.id()) : "kisa";
                String match = session.wireId == null || form.idBytes() != 2 ? "" : (session.wireId == form.id() ? " C=ayni" : " C=" + session.wireId);
                session.line("R1", FORM, "t=" + System.currentTimeMillis() + " id=" + idText + match, Tri.UNKNOWN);
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
            session.line("C", FORM, "tip=" + (send.type() < 0 ? unknown() + "(C FrameScan tip yok)" : send.type())
                    + " id=" + (send.idBytes() == 2 ? send.id() : unknown() + "(C FrameScan id bayti yok)")
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
            String stamp = "t=" + System.currentTimeMillis() + " " + type;
            if (FORM.equals(channelName)) {
                session.decoded = Tri.YES;
                session.line("R4", FORM, stamp, Tri.UNKNOWN);
            } else if (PROBE.equals(channelName)) {
                session.probeDecoded = Tri.YES;
                session.line("R4", PROBE, stamp, Tri.UNKNOWN);
            } else if (type.contains("CustomPayload") || type.contains("Payload")) {
                session.decodedUnread = true;
                session.line("R4", channelName == null ? "kanal-yok" : channelName, stamp, Tri.UNKNOWN);
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

    private record Listing(Tri value, String reason) {
    }

    private static Listing listing(UUID uuid) {
        Plugin plugin = hub.getServer().getPluginManager().getPlugin("floodgate");
        if (plugin == null || !plugin.isEnabled()) {
            return new Listing(Tri.NO, "floodgate plugin yok");
        }
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, plugin.getClass().getClassLoader());
            Object instance = api.getMethod("getInstance").invoke(null);
            Object player = api.getMethod("getPlayer", UUID.class).invoke(instance, uuid);
            if (player == null) {
                return new Listing(Tri.NO, "FloodgateApi.getPlayer null");
            }
            return new Listing(Tri.YES, "");
        } catch (Throwable ex) {
            return new Listing(Tri.UNKNOWN, "FloodgateApi " + FieldWalk.cause(ex));
        }
    }

    private static Channel channel(Player player) {
        return channel(player, new ArrayList<>(), new String[1]);
    }

    private static Channel channel(Player player, List<FieldWalk.Miss> misses, String[] step) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Class<?> listenerType = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
            Class<?> connectionType = Class.forName("net.minecraft.network.Connection");
            Object listener = FieldWalk.find(handle, listenerType, "a-listener", misses);
            Object connection = listener == null ? null : FieldWalk.find(listener, connectionType, "a-connection", misses);
            Object channel = connection == null ? null : FieldWalk.find(connection, Channel.class, "a-channel", misses);
            if (channel instanceof Channel found) {
                step[0] = "a";
                return found;
            }
        } catch (Throwable ex) {
            misses.add(new FieldWalk.Miss("a", player.getClass().getName(), Channel.class.getName(), FieldWalk.cause(ex)));
        }
        Plugin packetEvents = hub == null ? null : hub.getServer().getPluginManager().getPlugin("packetevents");
        if (packetEvents == null || !packetEvents.isEnabled()) {
            misses.add(new FieldWalk.Miss("b", "PacketEvents", Channel.class.getName(), "plugin yok"));
            return null;
        }
        try {
            Object channel = PeHooks.channel(player);
            if (channel instanceof Channel found) {
                step[0] = "b";
                return found;
            }
            misses.add(new FieldWalk.Miss("b", "PacketEvents.getPlayerManager", Channel.class.getName(), channel == null ? "getChannel null" : channel.getClass().getName()));
        } catch (Throwable ex) {
            misses.add(new FieldWalk.Miss("b", "PacketEvents.getPlayerManager", Channel.class.getName(), FieldWalk.cause(ex)));
        }
        return null;
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
        String key = value == Tri.YES ? "DEBUG.YES" : value == Tri.NO ? "DEBUG.NO" : "DEBUG.UNKNOWN";
        String fallback = value == Tri.YES ? "evet" : value == Tri.NO ? "hayir" : "ölçülemedi";
        String text = Messages.lookup(key);
        return text == null || text.isBlank() ? fallback : text;
    }

    private static String unknown() {
        return word(Tri.UNKNOWN);
    }

    private static String plain(String path, String fallback) {
        String text = Messages.lookup(path);
        return text == null || text.isBlank() ? fallback : text;
    }

    private static String viaFault = "";

    private static String via(UUID uuid) {
        viaFault = "";
        Plugin plugin = hub.getServer().getPluginManager().getPlugin("ViaVersion");
        if (plugin == null || !plugin.isEnabled()) {
            return "yok";
        }
        try {
            Class<?> type = Class.forName("com.viaversion.viaversion.api.Via", true, plugin.getClass().getClassLoader());
            Object api = type.getMethod("getAPI").invoke(null);
            int player = (Integer) api.getClass().getMethod("getPlayerVersion", UUID.class).invoke(api, uuid);
            Object serverVersion = api.getClass().getMethod("getServerVersion").invoke(api);
            int server = (Integer) serverVersion.getClass().getMethod("getVersion").invoke(serverVersion);
            String line = "oyuncu=" + player + " sunucu=" + server;
            if (player != server) {
                line += " ViaVersion çeviri yapıyor";
            }
            return line;
        } catch (Throwable ex) {
            viaFault = "Via " + FieldWalk.cause(ex);
            return "";
        }
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
        private Tri action = Tri.UNKNOWN;
        private Tri actionFailed = Tri.UNKNOWN;
        private Tri peCancel = Tri.UNKNOWN;
        private Tri plCancel = Tri.UNKNOWN;
        private Tri probeRaw = Tri.UNKNOWN;
        private Tri probeDecoded = Tri.UNKNOWN;
        private Tri probeBukkit = Tri.UNKNOWN;
        private Tri probeInjected = Tri.NO;
        private boolean joined;
        private int frames;
        private int r7Id = -1;
        private int replyWireId = -1;
        private long formAt;
        private String channelStep = "";
        private String channelReason = "";
        private String listedReason = "";
        private String injectReason = "";
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
            formAt = System.currentTimeMillis();
            line("C", FORM, "yol=" + path + " id=" + (id == null ? "api" : id) + " t=" + formAt, Tri.UNKNOWN);
            SmthsHubPlugin.scheduler().runLater(task -> {
                if (!formReported) {
                    if (!formR1 && frames > 0) {
                        raw = Tri.NO;
                    }
                    emitForm();
                }
            }, 1200L);
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
                Listing listing = listing(uuid);
                listed = listing.value;
                listedReason = listing.reason;
            }
            DebugVerdict.Judgment judgment = DebugVerdict.judge(facts());
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
                Listing listing = listing(uuid);
                listed = listing.value;
                listedReason = listing.reason;
            }
            closeGaps();
            DebugVerdict.Judgment judgment = DebugVerdict.judge(facts());
            publish(report(judgment, true));
        }

        private void closeGaps() {
            if (!peInstalled) {
                peCancel = peHook.startsWith("PacketEvents plugin yok") ? Tri.NO : Tri.UNKNOWN;
            } else if (!peSaw && raw == Tri.YES) {
                peCancel = Tri.UNKNOWN;
            } else if (!peSaw && raw == Tri.NO) {
                peCancel = Tri.NO;
            }
            if (decoded == Tri.UNKNOWN && !decodedUnread && raw == Tri.YES && peCancel == Tri.NO) {
                decoded = Tri.NO;
            }
            if (!plInstalled) {
                plCancel = plHook.startsWith("ProtocolLib plugin yok") ? Tri.NO : Tri.UNKNOWN;
            } else if (!plSaw && decoded == Tri.YES) {
                plCancel = Tri.UNKNOWN;
            } else if (!plSaw) {
                plCancel = Tri.NO;
            }
            if (r7Counts.getOrDefault(FORM, 0) > 0 && action == Tri.UNKNOWN) {
                action = Tri.NO;
            }
        }

        private DebugVerdict.Facts facts() {
            closeGaps();
            Tri measuring = frames > 0 ? Tri.YES : r1Present() ? Tri.NO : Tri.UNKNOWN;
            Tri reply;
            if (formR1) {
                reply = Tri.YES;
            } else if (formSent == Tri.YES && measuring == Tri.YES && System.currentTimeMillis() - formAt >= 60000L) {
                reply = Tri.NO;
            } else if (formSent == Tri.YES && raw == Tri.NO && measuring == Tri.YES) {
                reply = Tri.NO;
            } else {
                reply = Tri.UNKNOWN;
            }
            Tri listener = floodgateListener();
            Tri r3 = !peInstalled && peHook.startsWith("PacketEvents plugin yok") ? Tri.NO : peCancel;
            Tri r6 = !plInstalled && plHook.startsWith("ProtocolLib plugin yok") ? Tri.NO : plCancel;
            Tri bukkit;
            if (r7Counts.getOrDefault(FORM, 0) > 0) {
                bukkit = Tri.YES;
            } else if (decoded == Tri.YES && r6 == Tri.NO) {
                bukkit = Tri.NO;
            } else {
                bukkit = Tri.UNKNOWN;
            }
            return new DebugVerdict.Facts(listed, listener, formSent, measuring, reply, r3, decoded, r6, bukkit, action);
        }

        private boolean r1Present() {
            Player player = Bukkit.getPlayer(uuid);
            Channel channel = player == null ? null : channel(player);
            return channel != null && channel.pipeline().get(R1) != null;
        }

        private Tri floodgateListener() {
            String regs = regSnapshot();
            if (regs.startsWith("olculemedi")) {
                return Tri.UNKNOWN;
            }
            for (String part : regs.split(", ")) {
                int colon = part.indexOf(':');
                String plugin = colon < 0 ? part : part.substring(0, colon);
                if ("floodgate".equalsIgnoreCase(plugin)) {
                    return Tri.YES;
                }
            }
            return Tri.NO;
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
                        .append(" surum=").append(result.version() < 0 ? unknown() + "(H HandshakeScan surum bayti yok)" : result.version())
                        .append(" floodgate_oncesi=").append(hold.before == Tri.UNKNOWN ? unknown() + "(H floodgate_data_handler yok)" : word(hold.before))
                        .append('\n');
                out.append(hold.pipeline).append('\n');
            }
            Player player = Bukkit.getPlayer(uuid);
            Channel channel = player == null ? null : channel(player);
            List<String> names = channel == null ? List.of() : channel.pipeline().names();
            out.append(Messages.lookup("DEBUG.HEAD_F")).append('\n');
            String regs = regSnapshot();
            String listedValue = listed == Tri.UNKNOWN ? "" : word(listed);
            String listedWhy = listed == Tri.UNKNOWN ? "F FloodgateApi " + listedReason : listedReason;
            out.append(ReportText.field("uuid", uuid.toString(), unknown(), "F uuid bos")).append(' ');
            out.append(ReportText.field("listeli", listedValue, unknown(), listedWhy.isBlank() ? "F FloodgateApi.getPlayer" : listedWhy)).append(' ');
            out.append(ReportText.field("kayitlar", regs.startsWith("olculemedi") ? "" : regs, unknown(), regs.startsWith("olculemedi") ? regs : "F messenger floodgate:form")).append('\n');
            out.append(ReportText.field("kanal", channelStep.isBlank() ? "" : channelStep, unknown(), channelReason.isBlank() ? "kanal adimi yok" : channelReason)).append('\n');
            String viaLine = via(uuid);
            out.append(ReportText.field("via", viaLine, unknown(), viaFault.isBlank() ? "Via getPlayerVersion" : viaFault)).append('\n');
            out.append(ReportText.field("kayit_olay", REG_EVENTS.isEmpty() ? "yok" : String.join(" | ", REG_EVENTS), unknown(), "kayit izlenmedi")).append('\n');
            if (fingerprintKnown) {
                out.append(Messages.lookup("DEBUG.FINGERPRINT", "%fingerprint%", fingerprint)).append('\n');
            } else {
                out.append(Messages.lookup("DEBUG.FINGERPRINT", "%fingerprint%", unknown() + "(H key.pem okunamadi)")).append('\n');
            }
            out.append(Messages.lookup("DEBUG.HEAD_C")).append('\n');
            out.append(ReportText.field("yol", formPath, unknown(), "C form henuz yok")).append(' ');
            out.append(ReportText.field("id", sentId == null ? (formPath == null ? "" : "api") : sentId.toString(), unknown(), "C id yok")).append(' ');
            out.append(ReportText.field("tel_tip", wireType < 0 ? "" : Integer.toString(wireType), unknown(), "C tel_tip " + Encoded.class.getName() + " form ciktisi yok")).append(' ');
            out.append(ReportText.field("tel_id", wireId == null ? "" : wireId.toString(), unknown(), "C tel_id " + Encoded.class.getName() + " id bayti yok")).append(' ');
            out.append(ReportText.field("tel_boyut", wireBytes < 0 ? "" : Integer.toString(wireBytes), unknown(), "C tel_boyut " + Encoded.class.getName())).append('\n');
            int seenId = replyWireId >= 0 ? replyWireId : r7Id;
            String idCompare = wireId == null || seenId < 0 ? "" : (wireId == seenId ? "ayni" : "farkli C=" + wireId + " gelen=" + seenId);
            out.append(ReportText.field("tel_karsilastirma", idCompare, unknown(), "tel id icin C ve R7 gerekli")).append('\n');
            out.append(Messages.lookup("DEBUG.HEAD_R")).append('\n');
            appendHop(out, "R1", r1Text(names), r1Reason(names));
            appendHop(out, "R2", hopInstalled(peInstalled, peHook, "PacketEvents plugin yok"), hopReason("R2", peInstalled, peHook));
            appendHop(out, "R3", triText(peCancel, peInstalled, peHook, "PacketEvents plugin yok"), triReason("R3", peCancel, peInstalled, peHook, "PeHooks paket gorulmedi"));
            appendHop(out, "R4", decoded == Tri.UNKNOWN ? "" : word(decoded), decoded == Tri.UNKNOWN ? "R4 " + Decoded.class.getName() + " custom payload yok" : "");
            appendHop(out, "R5", hopInstalled(plInstalled, plHook, "ProtocolLib plugin yok"), hopReason("R5", plInstalled, plHook));
            appendHop(out, "R6", triText(plCancel, plInstalled, plHook, "ProtocolLib plugin yok"), triReason("R6", plCancel, plInstalled, plHook, "PlHooks paket gorulmedi"));
            appendHop(out, "R7", r7Text(), r7Reason());
            appendHop(out, "karar", judgment.verdict() == Verdict.OLCULEMEDI ? "" : word(judgment.verdict() == Verdict.SAGLAM ? Tri.YES : Tri.NO), judgment.verdict() == Verdict.OLCULEMEDI ? "karar " + judgment.blocked() : "");
            if (hops.isEmpty()) {
                out.append(ReportText.field("zaman", "", unknown(), "zaman cizelgesi bos")).append('\n');
            } else {
                for (String hop : hops) {
                    out.append(hop).append('\n');
                }
            }
            if (judgment.verdict() == Verdict.CEVAP_BACKENDE_ULASMADI) {
                StringBuilder other = new StringBuilder();
                r1Counts.forEach((key, count) -> other.append(key).append('=').append(count).append(' '));
                out.append(ReportText.field("diger", other.length() == 0 ? "yok" : other.toString().trim(), unknown(), "R1 custom payload yok")).append('\n');
            }
            out.append(Messages.lookup("DEBUG.HEAD_SELF")).append('\n');
            out.append(ReportText.field("enjekte", probeInjected == Tri.UNKNOWN ? "" : word(probeInjected), unknown(), injectReason.isBlank() ? "selftest packetId decoder.protocolInfo.codec.toId" : injectReason)).append(' ');
            out.append(ReportText.field("self_R1", probeRaw == Tri.UNKNOWN ? "" : word(probeRaw), unknown(), "selftest R1 " + Raw.class.getName())).append(' ');
            out.append(ReportText.field("self_R4", probeDecoded == Tri.UNKNOWN ? "" : word(probeDecoded), unknown(), "selftest R4 " + Decoded.class.getName())).append(' ');
            out.append(ReportText.field("self_R7", probeBukkit == Tri.UNKNOWN ? "" : word(probeBukkit), unknown(), "selftest R7 " + PROBE_LISTENER.getClass().getName())).append('\n');
            if (probeBukkit == Tri.YES && formSent == Tri.YES && r7Counts.getOrDefault(FORM, 0) == 0) {
                out.append(plain("DEBUG.FILTER", "smthshub:probe Bukkit'e ulasiyor, floodgate:form ulasmiyor")).append('\n');
            }
            out.append(Messages.lookup("DEBUG.HEAD_PIPE")).append('\n');
            if (names.isEmpty()) {
                out.append(ReportText.field("pipeline", "", unknown(), channelReason.isBlank() ? "pipeline kanal yok" : channelReason)).append('\n');
            } else {
                for (String pipe : names) {
                    io.netty.channel.ChannelHandler handler = channel.pipeline().get(pipe);
                    String type = handler == null ? "" : handler.getClass().getName();
                    String plugin = handler == null ? "" : owner(handler.getClass());
                    out.append(ReportText.field(pipe, (type + " " + plugin).trim(), unknown(), "pipeline handler null")).append('\n');
                }
            }
            String betweenIn = names.isEmpty() ? "" : slice(names, channel.pipeline().get("decompress") != null ? "decompress" : "splitter", "decoder");
            String betweenOut = names.isEmpty() ? "" : slice(names, "decoder", "packet_handler");
            out.append(ReportText.field("R1-decoder", betweenIn, unknown(), "pipeline R1-decoder araligi yok")).append('\n');
            out.append(ReportText.field("decoder-packet_handler", betweenOut, unknown(), "pipeline decoder-packet_handler araligi yok")).append('\n');
            out.append(Messages.lookup("DEBUG.HEAD_PE")).append('\n');
            out.append(ReportText.field("R2", hopInstalled(peInstalled, peHook, "PacketEvents plugin yok"), unknown(), hopReason("R2", peInstalled, peHook))).append(' ');
            out.append(ReportText.field("R3", hopInstalled(peInstalled, peHook, "PacketEvents plugin yok"), unknown(), hopReason("R3", peInstalled, peHook))).append('\n');
            out.append(peInstalled ? textOrUnknown(peDump()) : plain("DEBUG.NONE", "yok")).append('\n');
            out.append(Messages.lookup("DEBUG.HEAD_PL")).append('\n');
            out.append(plInstalled ? textOrUnknown(plDump()) : plain("DEBUG.NONE", "yok")).append('\n');
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
            String antiText = anti.length() == 0 ? plain("DEBUG.NONE", "yok") : anti.toString().trim();
            out.append(antiText.isBlank() ? "yok" : antiText).append('\n');
            String remover = "olay kaydi yok";
            for (int i = REG_EVENTS.size() - 1; i >= 0; i--) {
                if (REG_EVENTS.get(i).toLowerCase().contains("floodgate")) {
                    remover = REG_EVENTS.get(i);
                    break;
                }
            }
            String sentence = Messages.lookup("DEBUG.VERDICT." + judgment.verdict().name(),
                    "%field%", judgment.blocked().isBlank() ? "yok" : judgment.blocked(),
                    "%remover%", remover);
            if (sentence == null || sentence.isBlank()) {
                sentence = judgment.verdict().name() + (judgment.blocked().isBlank() ? "" : " " + judgment.blocked());
            }
            out.append(name).append(" HÜKÜM ").append(judgment.verdict().name()).append(' ').append(sentence).append('\n');
            if (form && formSent == Tri.YES && (judgment.verdict() == Verdict.CEVAP_BACKENDE_ULASMADI || raw != Tri.YES)) {
                out.append(Messages.lookup("DEBUG.PRESS")).append('\n');
            }
            if (stack != null) {
                out.append(stack).append('\n');
            }
            String fileLine = Messages.lookup("DEBUG.FILE", "%file%", file.getAbsolutePath());
            out.append(fileLine == null || fileLine.isBlank() ? "dosya=" + file.getAbsolutePath() : fileLine).append('\n');
            return out.toString().trim();
        }

        private static void appendHop(StringBuilder out, String key, String value, String reason) {
            out.append(ReportText.field(key, value, unknown(), reason)).append('\n');
        }

        private String r1Text(List<String> names) {
            if (frames <= 0) {
                return "";
            }
            int index = names.indexOf(R1);
            String prev = index > 0 ? names.get(index - 1) : "yok";
            String next = index >= 0 && index + 1 < names.size() ? names.get(index + 1) : "yok";
            return "evet konum=" + prev + " ile " + next + " arasi cerceve=" + frames;
        }

        private String r1Reason(List<String> names) {
            if (frames > 0) {
                return "";
            }
            if (names.contains(R1)) {
                return "R1 olcmuyor " + Raw.class.getName() + " cerceve=0";
            }
            return "R1 " + Raw.class.getName() + " " + (channelReason.isBlank() ? "kanal yok" : channelReason);
        }

        private String r7Text() {
            if (r7Counts.getOrDefault(FORM, 0) > 0) {
                return "evet";
            }
            if (decoded == Tri.YES && plCancel == Tri.NO) {
                return "hayir";
            }
            return "";
        }

        private String r7Reason() {
            if (r7Counts.getOrDefault(FORM, 0) > 0 || (decoded == Tri.YES && plCancel == Tri.NO)) {
                return "";
            }
            return "R7 " + FORM_LISTENER.getClass().getName() + " floodgate:form gelmedi";
        }

        private static String hopInstalled(boolean installed, String hook, String absent) {
            if (installed) {
                return "evet";
            }
            if (hook.startsWith(absent)) {
                return "hayir";
            }
            return "";
        }

        private static String hopReason(String hop, boolean installed, String hook) {
            if (installed) {
                return "";
            }
            return hop + " " + (hook.isBlank() ? "kurulum yok" : hook);
        }

        private static String triText(Tri value, boolean installed, String hook, String absent) {
            if (!installed && hook.startsWith(absent)) {
                return "hayir";
            }
            if (value == Tri.YES) {
                return "evet";
            }
            if (value == Tri.NO) {
                return "hayir";
            }
            return "";
        }

        private static String triReason(String hop, Tri value, boolean installed, String hook, String unseen) {
            if (value != Tri.UNKNOWN || (!installed && hook.startsWith("PacketEvents plugin yok")) || (!installed && hook.startsWith("ProtocolLib plugin yok"))) {
                return "";
            }
            if (!installed) {
                return hop + " " + hook;
            }
            return hop + " " + unseen;
        }
    }

    private static String slice(List<String> names, String from, String to) {
        int start = names.indexOf(from);
        int end = names.indexOf(to);
        if (start < 0 || end < 0 || end <= start) {
            return "";
        }
        if (end == start + 1) {
            return "yok";
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
        return text == null || text.isBlank() ? unknown() + "(dump bos)" : text;
    }
}
