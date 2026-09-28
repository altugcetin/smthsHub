package ist.alchm.smthsHub.spawn;

public final class SpawnMemory<T> {

    private T value;
    private boolean commanded;

    public synchronized void setCommand(T value) {
        this.value = value;
        this.commanded = true;
    }

    public synchronized boolean loadStored(T stored) {
        if (commanded || stored == null) {
            return false;
        }
        this.value = stored;
        return true;
    }

    public synchronized T value() {
        return value;
    }

    public synchronized T persistOrKeep(T disk) {
        return value != null ? value : disk;
    }
}
