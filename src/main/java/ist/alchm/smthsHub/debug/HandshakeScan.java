package ist.alchm.smthsHub.debug;

import java.nio.charset.StandardCharsets;

public final class HandshakeScan {

    public static final byte[] MARKER = "^Floodgate^".getBytes(StandardCharsets.ISO_8859_1);

    public record Result(boolean parsed, int hostLength, int parts, boolean marker, int version) {
    }

    private HandshakeScan() {
    }

    public static Result read(byte[] packet) {
        if (packet == null) {
            return new Result(false, -1, -1, false, -1);
        }
        VarIntRead.Out packetId = VarIntRead.read(packet, 0);
        if (!packetId.ok()) {
            return fail();
        }
        VarIntRead.Out protocol = VarIntRead.read(packet, packetId.next());
        if (!protocol.ok()) {
            return fail();
        }
        VarIntRead.Out length = VarIntRead.read(packet, protocol.next());
        if (!length.ok() || length.value() < 0) {
            return fail();
        }
        int start = length.next();
        if (start > packet.length || length.value() > packet.length - start) {
            return fail();
        }
        int parts = 1;
        boolean marker = false;
        int version = -1;
        int part = start;
        int end = start + length.value();
        for (int i = start; i <= end; i++) {
            if (i == end || packet[i] == 0) {
                if (starts(packet, part, i)) {
                    marker = true;
                    int after = part + MARKER.length;
                    if (after < i) {
                        version = packet[after] & 0xFF;
                    }
                }
                if (i < end && packet[i] == 0) {
                    parts++;
                }
                part = i + 1;
            }
        }
        return new Result(true, length.value(), parts, marker, version);
    }

    private static boolean starts(byte[] packet, int from, int to) {
        if (to - from < MARKER.length) {
            return false;
        }
        for (int i = 0; i < MARKER.length; i++) {
            if (packet[from + i] != MARKER[i]) {
                return false;
            }
        }
        return true;
    }

    private static Result fail() {
        return new Result(false, -1, -1, false, -1);
    }
}
