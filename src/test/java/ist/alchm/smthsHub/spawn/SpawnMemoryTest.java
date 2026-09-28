package ist.alchm.smthsHub.spawn;

public final class SpawnMemoryTest {

    public static void main(String[] args) {
        upstreamSaveDoesNotReplaceDisk();
        upstreamLoadClobbersCommand();
        upstreamDisableBeforeLoadWipesDisk();
        commandWinsOverLaterLoad();
        commandIsWhatGetsPersisted();
        nullMemoryKeepsDisk();
        failedLoadDoesNotErase();
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
