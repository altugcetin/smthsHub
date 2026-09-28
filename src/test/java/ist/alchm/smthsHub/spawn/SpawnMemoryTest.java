package ist.alchm.smthsHub.spawn;

import ist.alchm.smthsHub.inventory.BedrockFormPacket;
import ist.alchm.smthsHub.inventory.MenuText;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpawnMemoryTest {

    public static void main(String[] args) {
        upstreamSaveDoesNotReplaceDisk();
        upstreamLoadClobbersCommand();
        upstreamDisableBeforeLoadWipesDisk();
        commandWinsOverLaterLoad();
        commandIsWhatGetsPersisted();
        nullMemoryKeepsDisk();
        failedLoadDoesNotErase();
        savedLobbyFile();
        worldKeyOnly();
        commandNotReplacedByParsedDisk();
        menuLabels();
        bedrockUuid();
        formPacketMatchesCumulus();
        formResponse();
        serverFormIdLeavesProxyBitClear();
        unlistedBedrockFormPath();
    }

    private static void upstreamSaveDoesNotReplaceDisk() {
        UpstreamSpawn spawn = new UpstreamSpawn();
        spawn.disk = "old";
        spawn.enable();
        spawn.tick();
        spawn.setLobby("new");
        check("old".equals(spawn.disk), "setlobby save wrote the new spawn");
    }

    private static void upstreamLoadClobbersCommand() {
        UpstreamSpawn spawn = new UpstreamSpawn();
        spawn.disk = "old";
        spawn.enable();
        spawn.setLobby("new");
        spawn.tick();
        check("old".equals(spawn.memory), "deferred load kept the command spawn");
    }

    private static void upstreamDisableBeforeLoadWipesDisk() {
        UpstreamSpawn spawn = new UpstreamSpawn();
        spawn.disk = "old";
        spawn.enable();
        spawn.disable();
        check(spawn.disk == null, "disable before load kept the saved spawn");
    }

    private static void commandWinsOverLaterLoad() {
        SpawnMemory<String> memory = new SpawnMemory<>();
        memory.setCommand("new");
        check(!memory.loadStored("old"), "loadStored applied over a command");
        check("new".equals(memory.value()), "command spawn was replaced");
    }

    private static void commandIsWhatGetsPersisted() {
        SpawnMemory<String> memory = new SpawnMemory<>();
        memory.setCommand("new");
        check("new".equals(memory.persistOrKeep("old")), "persist kept the disk spawn");
    }

    private static void nullMemoryKeepsDisk() {
        SpawnMemory<String> memory = new SpawnMemory<>();
        check("old".equals(memory.persistOrKeep("old")), "null memory erased the disk spawn");
    }

    private static void failedLoadDoesNotErase() {
        SpawnMemory<String> memory = new SpawnMemory<>();
        check(!memory.loadStored(null), "null load was treated as a stored spawn");
        check(memory.persistOrKeep("old").equals("old"), "failed load changed the disk spawn");
    }

    private static void savedLobbyFile() {
        String yaml = """
                chat_locked: false
                spawn:
                  ==: org.bukkit.Location
                  world: lobby
                  x: 0.566793733789196
                  y: 65.0
                  z: 0.4285665574000912
                  pitch: 1.950112
                  yaw: -179.9035
                """;
        SpawnPoint point = SpawnPoint.parseDocument(yaml);
        check(point != null, "data.yml spawn was not read");
        check("lobby".equals(point.worldName), "world name was lost");
        check(point.covers("lobby", "minecraft:lobby", 0.566793733789196, 65.0, 0.4285665574000912), "parsed spawn does not cover itself");
        check(!point.covers("lobby", "minecraft:lobby", 0.537361397123215, 57.0, 38.9540514270691), "player.dat position counted as the saved lobby");
        Map<String, Object> written = point.toMap();
        check("lobby".equals(written.get("world")), "rewrite dropped the world name");
        SpawnPoint again = SpawnPoint.fromMap(written);
        check(again != null && again.y == 65.0 && again.z == point.z, "map round trip changed the lobby");
    }

    private static void worldKeyOnly() {
        SpawnPoint point = SpawnPoint.fromMap(Map.of(
                "world_key", "minecraft:lobby",
                "x", 10.5,
                "y", 70,
                "z", 4
        ));
        check(point != null && point.covers("other", "minecraft:lobby", 10.5, 70, 4), "world_key spawn was rejected");
        check(point.toMap().get("world_key").equals("minecraft:lobby"), "world_key was not written");
    }

    private static void commandNotReplacedByParsedDisk() {
        SpawnMemory<SpawnPoint> memory = new SpawnMemory<>();
        SpawnPoint commanded = new SpawnPoint("lobby", "minecraft:lobby", 12, 80, 3, 0, 0);
        SpawnPoint disk = SpawnPoint.parseDocument("spawn:\n  world: lobby\n  x: 0.5\n  y: 65.0\n  z: 0.5\n");
        memory.setCommand(commanded);
        check(!memory.loadStored(disk), "disk spawn replaced /setlobby");
        check(memory.value().covers("lobby", "minecraft:lobby", 12, 80, 3), "commanded spawn was lost");
        check(memory.persistOrKeep(disk).y == 80, "persist wrote the old 65 spawn");
    }

    private static void menuLabels() {
        check("Blocksmp".equals(MenuText.plain("&#02F31B&lʙʟᴏᴄᴋsᴍᴘ")), "small caps label was kept");
        check("Sunucu Secimi".equals(MenuText.plain("sᴜɴᴜᴄᴜ sᴇᴄɪᴍɪ")), "menu title small caps were kept");
        check("Lobi01".equals(MenuText.plain("ʟᴏʙɪ₀₁")), "subscript lobby number was kept");
        check("Lobi01".equals(MenuText.plain("&#FFF00F<bold>ʟᴏʙɪ</bold>&r₀₁")), "bold tag stayed in the label");
        check("Text".equals(MenuText.plain("§x§0§2§F§3§1§Btext")), "section hex label was not stripped");
        check("Hello".equals(MenuText.plain("§aHello")), "legacy color was not stripped");
    }

    private static void bedrockUuid() {
        UUID id = UUID.fromString("00000000-0000-0000-0009-01fb32f2e9a2");
        check(MenuText.floodgateUuid(id), "logged Bedrock uuid was not recognized");
        check(!MenuText.floodgateUuid(UUID.randomUUID()), "random uuid counted as Floodgate");
    }

    private static void formPacketMatchesCumulus() {
        String plain = "{\"title\":\"s\",\"content\":\"\",\"buttons\":[{\"text\":\"a\"}],\"type\":\"form\"}";
        String escaped = "{\"title\":\"a\\\"b\\\\c\",\"content\":\"\",\"buttons\":[{\"text\":\"x\\u003cy\\u003e\\u0026\\u003d\\u0027\\n\"}],\"type\":\"form\"}";
        check(plain.equals(BedrockFormPacket.json("s", List.of("a"))), "simple form json drifted from Cumulus");
        check(escaped.equals(BedrockFormPacket.json("a\"b\\c", List.of("x<y>&='\n"))), "form escaping drifted from Cumulus");
        String menu = BedrockFormPacket.json("ʟᴏʙɪ sᴇᴄɪᴍɪ", List.of("ʙʟᴏᴄᴋsᴍᴘ", "ʟᴏʙɪ"));
        check(menu.contains("\"title\":\"ʟᴏʙɪ sᴇᴄɪᴍɪ\"") && menu.contains("{\"text\":\"ʙʟᴏᴄᴋsᴍᴘ\"},{\"text\":\"ʟᴏʙɪ\"}"), "menu labels were escaped");
        byte[] packet = BedrockFormPacket.encode(1, "s", List.of("a"));
        check(packet[0] == 0 && packet[1] == 0 && packet[2] == 1, "form header is not type, id high, id low");
        check(plain.equals(new String(packet, 3, packet.length - 3, StandardCharsets.UTF_8)), "packet json was shifted");
    }

    private static void formResponse() {
        byte[] click = new byte[] {0, 1, '2'};
        check(BedrockFormPacket.formId(click) == 1, "response form id");
        check(BedrockFormPacket.clickedButton(click) == 2, "response button");
        byte[] quotedBody = "\"0\"".getBytes(StandardCharsets.UTF_8);
        byte[] quoted = new byte[quotedBody.length + 2];
        quoted[0] = 0;
        quoted[1] = 1;
        System.arraycopy(quotedBody, 0, quoted, 2, quotedBody.length);
        check(BedrockFormPacket.clickedButton(quoted) == 0, "quoted button was dropped");
        byte[] prefixed = new byte[] {0, 0, 0, '0'};
        check(BedrockFormPacket.clickedButton(prefixed) == 0, "control byte hid the button");
        byte[] arrayBody = "[0]".getBytes(StandardCharsets.UTF_8);
        byte[] array = new byte[arrayBody.length + 2];
        array[0] = 0;
        array[1] = 1;
        System.arraycopy(arrayBody, 0, array, 2, arrayBody.length);
        check(BedrockFormPacket.clickedButton(array) == 0, "bracketed button was dropped");
        check(BedrockFormPacket.labelButton("Blocksmp", List.of("Blocksmp", "Pillars")) == 0, "button text was not matched");
        check(BedrockFormPacket.clickedButton(labelBytes("Lobi01")) == -1, "lobby label counted as a button index");
        byte[] body = "null".getBytes(StandardCharsets.UTF_8);
        byte[] closed = new byte[body.length + 2];
        closed[0] = 0;
        closed[1] = 1;
        System.arraycopy(body, 0, closed, 2, body.length);
        check(BedrockFormPacket.clickedButton(closed) == -1, "closed form counted as a click");
        check(BedrockFormPacket.clickedButton(new byte[] {0, 1}) == -1, "empty response counted as a click");
    }

    private static byte[] labelBytes(String text) {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[body.length + 2];
        data[1] = 1;
        System.arraycopy(body, 0, data, 2, body.length);
        return data;
    }

    private static void serverFormIdLeavesProxyBitClear() {
        boolean rejected = false;
        try {
            BedrockFormPacket.encode(0x8000, "s", List.of("a"));
        } catch (IllegalArgumentException ex) {
            rejected = true;
        }
        check(rejected, "proxy form bit was accepted");
        byte[] max = BedrockFormPacket.encode(Short.MAX_VALUE, "s", List.of("a"));
        check((max[1] & 0xFF) == 0x7F && (max[2] & 0xFF) == 0xFF, "max server form id bytes");
        check(BedrockFormPacket.formId(new byte[] {(byte) 0x7F, (byte) 0xFF}) == Short.MAX_VALUE, "max server form id read");
    }

    private static void unlistedBedrockFormPath() {
        check(BedrockFormPacket.choose(true, true, false, true, false) == BedrockFormPacket.Open.API, "listed player left the api");
        check(BedrockFormPacket.choose(true, false, true, true, true) == BedrockFormPacket.Open.RAW, "unlisted bedrock id was not sent");
        check(BedrockFormPacket.choose(true, false, true, true, false) == BedrockFormPacket.Open.CHEST, "kick listener still opened a form");
        check(BedrockFormPacket.choose(true, false, true, false, true) == BedrockFormPacket.Open.CHEST, "unheard channel opened a form");
        check(BedrockFormPacket.choose(true, false, false, true, true) == BedrockFormPacket.Open.CHEST, "java player got a form");
        check(BedrockFormPacket.choose(false, false, true, true, false) == BedrockFormPacket.Open.RAW, "proxy-only floodgate was not sent");
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError(failure);
        }
    }

    private static final class UpstreamSpawn {
        String memory;
        String disk;
        boolean pendingLoad;

        void enable() {
            pendingLoad = true;
        }

        void tick() {
            if (pendingLoad) {
                memory = disk;
                pendingLoad = false;
            }
        }

        void setLobby(String value) {
            memory = value;
        }

        void disable() {
            disk = memory;
        }
    }
}
