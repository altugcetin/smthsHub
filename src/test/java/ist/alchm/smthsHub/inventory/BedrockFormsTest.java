package ist.alchm.smthsHub.inventory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BedrockFormsTest {

    public static void main(String[] args) {
        decisions();
        silenceSelection();
        twoCopies();
        idBlocks();
        packetReplies();
    }

    private static void decisions() {
        row(5, 5, false, false, BedrockFormLogic.Decision.ACT);
        row(5, 5, false, true, BedrockFormLogic.Decision.ACT);
        row(5, 5, true, false, BedrockFormLogic.Decision.ACT);
        row(5, 5, true, true, BedrockFormLogic.Decision.ACT);
        row(5, 4, false, false, BedrockFormLogic.Decision.IGNORE);
        row(5, 4, false, true, BedrockFormLogic.Decision.IGNORE);
        row(5, 4, true, false, BedrockFormLogic.Decision.IGNORE);
        row(5, 4, true, true, BedrockFormLogic.Decision.DELEGATE);
        row(5, null, false, false, BedrockFormLogic.Decision.IGNORE);
        row(5, null, false, true, BedrockFormLogic.Decision.IGNORE);
        row(5, null, true, false, BedrockFormLogic.Decision.IGNORE);
        row(5, null, true, true, BedrockFormLogic.Decision.DELEGATE);
        row(0, 1, false, true, BedrockFormLogic.Decision.IGNORE);
        row(0, 0, false, false, BedrockFormLogic.Decision.ACT);
    }

    private static void row(int response, Integer pending, boolean listed, boolean holding, BedrockFormLogic.Decision expected) {
        BedrockFormLogic.Decision actual = BedrockFormLogic.decide(response, pending, listed, holding);
        check(actual == expected, "decision " + response + " " + pending + " " + listed + " " + holding + " was " + actual);
    }

    private static void silenceSelection() {
        check(BedrockFormLogic.floodgateRegistrations(List.of("floodgate", "smthsHub", "smthsFriends")).equals(List.of(0)), "floodgate was not the only removal");
        check(BedrockFormLogic.floodgateRegistrations(List.of("smthsHub", "smthsFriends")).isEmpty(), "a hub listener was selected");
        check(BedrockFormLogic.floodgateRegistrations(List.of("Floodgate", "smthsHub")).equals(List.of(0)), "Floodgate case was missed");
        check(BedrockFormLogic.floodgateRegistrations(List.of("FLOODGATE")).equals(List.of(0)), "FLOODGATE case was missed");
    }

    private static void twoCopies() {
        List<String> channel = new ArrayList<>(List.of("floodgate", "smthsHub", "smthsFriends"));
        Copy hub = new Copy("smthsHub");
        Copy friends = new Copy("smthsFriends");
        channel = hub.silence(channel);
        channel = friends.silence(channel);
        check(channel.equals(List.of("smthsHub", "smthsFriends")), "a plugin listener was removed");
        check(hub.holding && !friends.holding, "floodgate listener was not held by exactly one copy");
        List<String> again = new ArrayList<>(List.of("floodgate", "smthsHub", "smthsFriends"));
        Copy first = new Copy("smthsFriends");
        Copy second = new Copy("smthsHub");
        again = first.silence(again);
        again = second.silence(again);
        check(again.equals(List.of("smthsHub", "smthsFriends")), "reverse order removed a plugin listener");
        check(first.holding && !second.holding, "the second copy also held the floodgate listener");
    }

    private static void idBlocks() {
        check(BedrockFormLogic.HUB_SLOT == 1, "hub slot moved");
        List<Set<Integer>> blocks = new ArrayList<>();
        for (int slot = 1; slot <= 31; slot++) {
            Set<Integer> seen = new HashSet<>();
            int low = slot * 1024;
            int high = low + 1023;
            for (int counter = 0; counter < 70000; counter++) {
                int id = BedrockFormLogic.formId(slot, counter);
                check(id >= low && id <= high, "id left its block");
                check(id < 0x8000, "proxy bit was set");
                seen.add(id);
            }
            check(seen.size() == 1024, "block was not 1024 ids");
            blocks.add(seen);
        }
        for (int i = 0; i < blocks.size(); i++) {
            for (int j = i + 1; j < blocks.size(); j++) {
                Set<Integer> overlap = new HashSet<>(blocks.get(i));
                overlap.retainAll(blocks.get(j));
                check(overlap.isEmpty(), "slots " + (i + 1) + " and " + (j + 1) + " overlapped");
            }
        }
        boolean rejected = false;
        try {
            BedrockFormLogic.formId(32, 0);
        } catch (IllegalArgumentException ex) {
            rejected = true;
        }
        check(rejected, "slot 32 was accepted");
        check(BedrockFormLogic.formId(31, 1023) == 0x7FFF, "last server id was not 32767");
        check(BedrockFormLogic.formId(1, 0) == 0x0400, "hub block did not start at 1024");
    }

    private static void packetReplies() {
        check(BedrockFormPacket.clickedButton(reply("0\n")) == 0, "0\\n was not button 0");
        check(BedrockFormPacket.clickedButton(reply("1\n")) == 1, "1\\n was not button 1");
        check(BedrockFormPacket.clickedButton(reply("\"1\"")) == 1, "quoted 1 was dropped");
        check(BedrockFormPacket.clickedButton(reply("[1]")) == 1, "bracketed 1 was dropped");
        check(BedrockFormPacket.clickedButton(reply("null\n")) == -1, "null\\n counted as a click");
        check(BedrockFormPacket.closed(reply("null\n")), "null\\n was not a close");
        byte[] empty = {0, 1};
        check(BedrockFormPacket.clickedButton(empty) == -1, "two-byte reply counted as a click");
        check(BedrockFormPacket.closed(empty), "two-byte reply was not a close");
    }

    private static byte[] reply(String body) {
        byte[] text = body.getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[text.length + 2];
        data[0] = 0;
        data[1] = 1;
        System.arraycopy(text, 0, data, 2, text.length);
        return data;
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError(failure);
        }
    }

    private static final class Copy {
        private final String name;
        private boolean holding;

        private Copy(String name) {
            this.name = name;
        }

        private List<String> silence(List<String> channel) {
            List<Integer> drop = BedrockFormLogic.floodgateRegistrations(channel);
            if (!drop.isEmpty()) {
                holding = true;
            }
            List<String> next = new ArrayList<>();
            for (int i = 0; i < channel.size(); i++) {
                if (!drop.contains(i)) {
                    next.add(channel.get(i));
                }
            }
            check(next.contains(name), "copy removed itself");
            return next;
        }
    }
}
