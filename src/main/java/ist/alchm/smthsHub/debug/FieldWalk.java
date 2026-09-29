package ist.alchm.smthsHub.debug;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public final class FieldWalk {

    public record Miss(String step, String owner, String wanted, String error) {
    }

    private FieldWalk() {
    }

    public static Object find(Object root, Class<?> wanted, String step, List<Miss> misses) {
        if (root == null) {
            misses.add(new Miss(step, "null", wanted.getName(), "root null"));
            return null;
        }
        if (wanted == null) {
            misses.add(new Miss(step, root.getClass().getName(), "null", "aranan tip null"));
            return null;
        }
        Class<?> type = root.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !wanted.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(root);
                    if (value != null) {
                        return value;
                    }
                    misses.add(new Miss(step, type.getName() + "." + field.getName(), wanted.getName(), "null"));
                } catch (Throwable ex) {
                    misses.add(new Miss(step, type.getName() + "." + field.getName(), wanted.getName(), cause(ex)));
                }
            }
            type = type.getSuperclass();
        }
        misses.add(new Miss(step, root.getClass().getName(), wanted.getName(), "alan yok"));
        return null;
    }

    public static String text(List<Miss> misses) {
        StringBuilder out = new StringBuilder();
        for (Miss miss : misses) {
            if (!out.isEmpty()) {
                out.append("; ");
            }
            out.append(miss.step()).append(' ').append(miss.owner()).append(' ').append(miss.wanted()).append(' ').append(miss.error());
        }
        return out.toString();
    }

    public static String cause(Throwable ex) {
        String message = ex.getMessage();
        return ex.getClass().getName() + ": " + (message == null || message.isBlank() ? "mesaj yok" : message);
    }
}
