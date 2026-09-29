package ist.alchm.smthsHub.debug;

import java.util.ArrayList;
import java.util.List;

public final class FormTrace {

    private final List<String> lines = new ArrayList<>();

    public void accept(String channel, byte[] message, long time) {
        if (!"floodgate:form".equals(channel)) {
            return;
        }
        lines.add(line(time, message));
    }

    public List<String> lines() {
        return List.copyOf(lines);
    }

    public static String line(long time, byte[] message) {
        if (message == null) {
            return time + " R7 floodgate:form id=ölçülemedi(R7 FormTrace payload null)";
        }
        if (message.length < 2) {
            return time + " R7 floodgate:form id=ölçülemedi(R7 FormTrace payload " + message.length + " bayt)";
        }
        int id = ((message[0] & 0xFF) << 8) | (message[1] & 0xFF);
        return time + " R7 floodgate:form id=" + id;
    }
}
