package ist.alchm.smthsHub.debug;

public final class VarIntRead {

    public record Out(boolean ok, int value, int next) {
    }

    private VarIntRead() {
    }

    public static Out read(byte[] data, int index) {
        if (data == null || index < 0) {
            return new Out(false, 0, index);
        }
        int value = 0;
        int position = 0;
        int cursor = index;
        while (cursor < data.length) {
            int bite = data[cursor] & 0xFF;
            value |= (bite & 0x7F) << position;
            cursor++;
            if ((bite & 0x80) == 0) {
                return new Out(true, value, cursor);
            }
            position += 7;
            if (position > 28) {
                return new Out(false, 0, index);
            }
        }
        return new Out(false, 0, index);
    }
}
