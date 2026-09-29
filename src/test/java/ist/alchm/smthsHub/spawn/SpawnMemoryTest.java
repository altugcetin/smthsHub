package ist.alchm.smthsHub.spawn;

import ist.alchm.smthsHub.inventory.MenuText;

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
