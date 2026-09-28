package ist.alchm.smthsHub.spawn;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SpawnPoint {

    public final String worldName;
    public final String worldKey;
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;

    public SpawnPoint(String worldName, String worldKey, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.worldKey = worldKey;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("==", "org.bukkit.Location");
        if (worldName != null && !worldName.isEmpty()) {
            map.put("world", worldName);
        }
        if (worldKey != null && !worldKey.isEmpty()) {
            map.put("world_key", worldKey);
        }
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        return map;
    }

    public static SpawnPoint fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        String world = text(map.get("world"));
        String worldKey = text(map.get("world_key"));
        if ((world == null || world.isEmpty()) && (worldKey == null || worldKey.isEmpty())) {
            return null;
        }
        if (!map.containsKey("x") || !map.containsKey("y") || !map.containsKey("z")) {
            return null;
        }
        return new SpawnPoint(world, worldKey, number(map.get("x")), number(map.get("y")), number(map.get("z")),
                (float) number(map.get("yaw")), (float) number(map.get("pitch")));
    }

    public static SpawnPoint parseDocument(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        boolean inSpawn = false;
        String world = null;
        String worldKey = null;
        Double x = null;
        Double y = null;
        Double z = null;
        float yaw = 0;
        float pitch = 0;
        for (String line : text.split("\\R")) {
            if (!inSpawn) {
                if (line.startsWith("spawn:")) {
                    inSpawn = true;
                }
                continue;
            }
            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0))) {
                break;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = trimmed.substring(0, colon).trim();
            String value = unquote(trimmed.substring(colon + 1).trim());
            switch (key) {
                case "world" -> world = value;
                case "world_key" -> worldKey = value;
                case "x" -> x = Double.parseDouble(value);
                case "y" -> y = Double.parseDouble(value);
                case "z" -> z = Double.parseDouble(value);
                case "yaw" -> yaw = (float) Double.parseDouble(value);
                case "pitch" -> pitch = (float) Double.parseDouble(value);
                default -> {
                }
            }
        }
        if (x == null || y == null || z == null) {
            return null;
        }
        if ((world == null || world.isEmpty()) && (worldKey == null || worldKey.isEmpty())) {
            return null;
        }
        return new SpawnPoint(world, worldKey, x, y, z, yaw, pitch);
    }

    public boolean covers(String name, String key, double px, double py, double pz) {
        boolean world = (worldName != null && worldName.equalsIgnoreCase(name))
                || (worldKey != null && worldKey.equals(key));
        if (!world) {
            return false;
        }
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        return dx * dx + dy * dy + dz * dz < 1.0E-4;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char start = value.charAt(0);
            char end = value.charAt(value.length() - 1);
            if ((start == '\'' && end == '\'') || (start == '"' && end == '"')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
