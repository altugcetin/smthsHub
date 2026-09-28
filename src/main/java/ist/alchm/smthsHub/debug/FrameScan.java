package ist.alchm.smthsHub.debug;

import java.nio.charset.StandardCharsets;

public final class FrameScan {

    public record Reply(int at, int idBytes, int id) {
    }

    public record Send(int type, int idBytes, int id, int afterChannel) {
    }

    private FrameScan() {
    }

    public static Reply reply(byte[] data, String channel) {
        int at = channelAt(data, channel);
        if (at < 0) {
            return null;
        }
        int body = bodyAt(data, at, channel);
        int avail = data.length - body;
        int idBytes = Math.max(0, Math.min(2, avail));
        int id = 0;
        if (idBytes == 2) {
            id = ((data[body] & 0xFF) << 8) | (data[body + 1] & 0xFF);
        }
        return new Reply(at, idBytes, id);
    }

    public static Send send(byte[] data, String channel) {
        int at = channelAt(data, channel);
        if (at < 0) {
            return null;
        }
        int body = bodyAt(data, at, channel);
        int avail = data.length - body;
        if (avail < 1) {
            return new Send(-1, 0, 0, avail);
        }
        int type = data[body] & 0xFF;
        int idBytes = avail >= 3 ? 2 : Math.max(0, avail - 1);
        int id = 0;
        if (idBytes == 2) {
            id = ((data[body + 1] & 0xFF) << 8) | (data[body + 2] & 0xFF);
        }
        return new Send(type, idBytes, id, avail);
    }

    public static String customChannel(byte[] data) {
        if (data == null) {
            return null;
        }
        VarIntRead.Out packetId = VarIntRead.read(data, 0);
        if (!packetId.ok()) {
            return null;
        }
        VarIntRead.Out length = VarIntRead.read(data, packetId.next());
        if (!length.ok() || length.value() < 3 || length.value() > 64) {
            return null;
        }
        int start = length.next();
        if (start > data.length || length.value() > data.length - start) {
            return null;
        }
        String name = new String(data, start, length.value(), StandardCharsets.UTF_8);
        int colon = name.indexOf(':');
        if (colon <= 0 || colon >= name.length() - 1) {
            return null;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-' || c == ':' || c == '/';
            if (!ok) {
                return null;
            }
        }
        return name;
    }

    private static int channelAt(byte[] data, String channel) {
        if (data == null || channel == null || channel.isEmpty()) {
            return -1;
        }
        byte[] name = channel.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < data.length; i++) {
            VarIntRead.Out length = VarIntRead.read(data, i);
            if (!length.ok() || length.value() != name.length) {
                continue;
            }
            int start = length.next();
            if (start > data.length || name.length > data.length - start) {
                continue;
            }
            boolean match = true;
            for (int n = 0; n < name.length; n++) {
                if (data[start + n] != name[n]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }

    private static int bodyAt(byte[] data, int at, String channel) {
        VarIntRead.Out length = VarIntRead.read(data, at);
        return length.next() + channel.getBytes(StandardCharsets.UTF_8).length;
    }
}
