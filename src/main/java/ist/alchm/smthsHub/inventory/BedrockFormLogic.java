package ist.alchm.smthsHub.inventory;

import java.util.ArrayList;
import java.util.List;

public final class BedrockFormLogic {

    public static final int HUB_SLOT = 1;
    public static final String FLOODGATE = "floodgate";

    public enum Decision {
        ACT, DELEGATE, IGNORE
    }

    private BedrockFormLogic() {
    }

    public static List<Integer> floodgateRegistrations(List<String> pluginNames) {
        List<Integer> indexes = new ArrayList<>();
        if (pluginNames == null) {
            return indexes;
        }
        for (int i = 0; i < pluginNames.size(); i++) {
            String name = pluginNames.get(i);
            if (name != null && FLOODGATE.equalsIgnoreCase(name)) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    public static Decision decide(int responseId, Integer pendingId, boolean listed, boolean holdingListener) {
        if (pendingId != null && responseId == pendingId) {
            return Decision.ACT;
        }
        if (listed && holdingListener) {
            return Decision.DELEGATE;
        }
        return Decision.IGNORE;
    }

    public static int formId(int slot, int counter) {
        if (slot < 1 || slot > 31) {
            throw new IllegalArgumentException("slot " + slot);
        }
        return slot * 1024 + Math.floorMod(counter, 1024);
    }
}
