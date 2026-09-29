package ist.alchm.smthsHub.debug;

public final class ReportText {

    private ReportText() {
    }

    public static String field(String key, String value, String unknown, String reason) {
        String token = unknown == null || unknown.isBlank() ? "ölçülemedi" : unknown;
        if (value != null && !value.isBlank()) {
            return key + "=" + value;
        }
        String why = reason == null || reason.isBlank() ? "sebep yok" : reason;
        return key + "=" + token + "(" + why + ")";
    }
}
