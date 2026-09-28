package ist.alchm.smthsHub.inventory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class BedrockFormPacket {

    public static final String CHANNEL = "floodgate:form";

    public enum Open {
        API, RAW, CHEST
    }

    private BedrockFormPacket() {
    }

    public static Open choose(boolean floodgatePlugin, boolean listed, boolean floodgateId, boolean listening, boolean kickSilenced) {
        if (listed) {
            return Open.API;
        }
        if (!floodgateId || !listening || (floodgatePlugin && !kickSilenced)) {
            return Open.CHEST;
        }
        return Open.RAW;
    }

    public static byte[] encode(int formId, String title, List<String> buttons) {
        if (formId < 0 || formId > Short.MAX_VALUE) {
            throw new IllegalArgumentException("form id " + formId);
        }
        byte[] json = json(title, buttons).getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[json.length + 3];
        data[0] = 0;
        data[1] = (byte) (formId >> 8 & 0xFF);
        data[2] = (byte) (formId & 0xFF);
        System.arraycopy(json, 0, data, 3, json.length);
        return data;
    }

    public static String json(String title, List<String> buttons) {
        StringBuilder out = new StringBuilder();
        out.append("{\"title\":").append(quote(title));
        out.append(",\"content\":\"\",\"buttons\":[");
        for (int i = 0; i < buttons.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append("{\"text\":").append(quote(buttons.get(i))).append('}');
        }
        out.append("],\"type\":\"form\"}");
        return out.toString();
    }

    public static int formId(byte[] data) {
        return (short) ((data[0] & 0xFF) << 8 | data[1] & 0xFF);
    }

    public static String responseText(byte[] data) {
        if (data == null || data.length < 3) {
            return "";
        }
        String raw = new String(data, 2, data.length - 2, StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder(raw.length());
        raw.codePoints().forEach(cp -> {
            if (cp >= 0x20) {
                out.appendCodePoint(cp);
            }
        });
        return unwrap(out.toString().trim());
    }

    public static int clickedButton(byte[] data) {
        return integerToken(responseText(data));
    }

    public static int labelButton(String text, List<String> labels) {
        if (text == null || text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return -1;
        }
        for (int i = 0; i < labels.size(); i++) {
            if (text.equalsIgnoreCase(labels.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static boolean closed(byte[] data) {
        if (data == null || data.length < 3) {
            return true;
        }
        String text = responseText(data);
        return text.isEmpty() || "null".equalsIgnoreCase(text);
    }

    private static int integerToken(String text) {
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return -1;
        }
        String token = text;
        if (token.length() >= 2 && token.charAt(0) == '[' && token.charAt(token.length() - 1) == ']') {
            token = unwrap(token.substring(1, token.length() - 1).trim());
        }
        try {
            int id = Integer.parseInt(token);
            return id < 0 ? -1 : id;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String unwrap(String body) {
        String token = body.trim();
        if (token.length() >= 2 && token.charAt(0) == '"' && token.charAt(token.length() - 1) == '"') {
            token = token.substring(1, token.length() - 1).trim();
        }
        return token;
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '<' -> out.append("\\u003c");
                case '>' -> out.append("\\u003e");
                case '&' -> out.append("\\u0026");
                case '=' -> out.append("\\u003d");
                case '\'' -> out.append("\\u0027");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }
}
