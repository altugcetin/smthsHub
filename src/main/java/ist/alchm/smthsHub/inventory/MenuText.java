package ist.alchm.smthsHub.inventory;

import java.util.UUID;
import java.util.regex.Pattern;

public final class MenuText {

    private static final Pattern COLOR = Pattern.compile("(?i)§x(§[0-9A-F]){6}|&#[0-9A-F]{6}|[§&][0-9A-FK-OR]");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    private MenuText() {
    }

    public static String plain(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String stripped = TAG.matcher(COLOR.matcher(input).replaceAll("")).replaceAll("").replace("§", "").trim();
        StringBuilder folded = new StringBuilder(stripped.length());
        stripped.codePoints().forEach(cp -> folded.appendCodePoint(fold(cp)));
        return capitalize(folded);
    }

    private static String capitalize(CharSequence value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean word = true;
        for (int i = 0; i < value.length(); ) {
            int cp = Character.codePointAt(value, i);
            if (Character.isWhitespace(cp)) {
                word = true;
                out.appendCodePoint(cp);
            } else if (word) {
                out.appendCodePoint(Character.toUpperCase(cp));
                word = false;
            } else {
                out.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    private static int fold(int cp) {
        return switch (cp) {
            case 'ᴀ' -> 'a';
            case 'ʙ' -> 'b';
            case 'ᴄ' -> 'c';
            case 'ᴅ' -> 'd';
            case 'ᴇ' -> 'e';
            case 'ꜰ' -> 'f';
            case 'ɢ' -> 'g';
            case 'ʜ' -> 'h';
            case 'ɪ' -> 'i';
            case 'ᴊ' -> 'j';
            case 'ᴋ' -> 'k';
            case 'ʟ' -> 'l';
            case 'ᴍ' -> 'm';
            case 'ɴ' -> 'n';
            case 'ᴏ' -> 'o';
            case 'ᴘ' -> 'p';
            case 'ǫ' -> 'q';
            case 'ʀ' -> 'r';
            case 'ꜱ' -> 's';
            case 'ᴛ' -> 't';
            case 'ᴜ' -> 'u';
            case 'ᴠ' -> 'v';
            case 'ᴡ' -> 'w';
            case 'ʏ' -> 'y';
            case 'ᴢ' -> 'z';
            case '₀' -> '0';
            case '₁' -> '1';
            case '₂' -> '2';
            case '₃' -> '3';
            case '₄' -> '4';
            case '₅' -> '5';
            case '₆' -> '6';
            case '₇' -> '7';
            case '₈' -> '8';
            case '₉' -> '9';
            default -> cp;
        };
    }

    public static boolean floodgateUuid(UUID uuid) {
        return uuid.getMostSignificantBits() == 0L && (uuid.getLeastSignificantBits() >>> 48) == 0x0009L;
    }
}
