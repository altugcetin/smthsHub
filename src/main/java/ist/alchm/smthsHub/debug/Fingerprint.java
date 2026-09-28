package ist.alchm.smthsHub.debug;

import java.security.MessageDigest;

public final class Fingerprint {

    private Fingerprint() {
    }

    public static String prefix(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder out = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                out.append(Character.forDigit((hash[i] >> 4) & 0xF, 16));
                out.append(Character.forDigit(hash[i] & 0xF, 16));
            }
            return out.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
