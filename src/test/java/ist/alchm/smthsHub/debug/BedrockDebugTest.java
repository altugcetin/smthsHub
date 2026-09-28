package ist.alchm.smthsHub.debug;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class BedrockDebugTest {

    public static void main(String[] args) {
        verdicts();
        handshakeOnly();
        frames();
        varints();
        handshakes();
        fingerprints();
        channelNotes();
        System.out.println("bedrock debug ok");
    }

    private static void verdicts() {
        Tri[] values = Tri.values();
        int seen = 0;
        for (Tri uuid : values) {
            for (Tri marker : values) {
                for (Tri listed : values) {
                    for (Tri form : values) {
                        for (Tri raw : values) {
                            for (Tri packetEvents : values) {
                                for (Tri decoded : values) {
                                    for (Tri protocol : values) {
                                        for (Tri bukkit : values) {
                                            for (Tri act : values) {
                                                for (Tri failed : values) {
                                                    DebugVerdict.Facts facts = new DebugVerdict.Facts(
                                                            uuid, marker, listed, form, raw, packetEvents, decoded, protocol, bukkit, act, failed
                                                    );
                                                    DebugVerdict.Judgment judgment = DebugVerdict.judge(facts);
                                                    DebugVerdict.Judgment expected = expected(facts);
                                                    check(judgment.verdict() == expected.verdict() && judgment.blocked().equals(expected.blocked()));
                                                    if (judgment.verdict() != Verdict.OLCULEMEDI) {
                                                        check(judgment.blocked().isEmpty());
                                                    }
                                                    seen++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        check(seen == 177147);
        check(DebugVerdict.judge(row(Tri.YES, Tri.NO, Tri.UNKNOWN, Tri.UNKNOWN, Tri.UNKNOWN, Tri.UNKNOWN, Tri.UNKNOWN, Tri.UNKNOWN, Tri.UNKNOWN, Tri.UNKNOWN, Tri.UNKNOWN)).verdict() == Verdict.PROXY_FLOODGATE_VERISI_YOK);
        check(DebugVerdict.judge(row(Tri.YES, Tri.UNKNOWN, Tri.NO, Tri.NO, Tri.NO, Tri.NO, Tri.NO, Tri.NO, Tri.NO, Tri.NO, Tri.NO)).verdict() == Verdict.OLCULEMEDI);
        check(DebugVerdict.judge(row(Tri.NO, Tri.NO, Tri.NO, Tri.YES, Tri.YES, Tri.NO, Tri.YES, Tri.NO, Tri.YES, Tri.YES, Tri.NO)).verdict() == Verdict.JAVA_OYUNCU);
        check(DebugVerdict.judge(solid()).verdict() == Verdict.SAGLAM);
        check(DebugVerdict.judge(with(solid(), 2, Tri.NO)).verdict() == Verdict.BACKEND_FLOODGATE_OKUMUYOR);
        check(DebugVerdict.judge(with(solid(), 3, Tri.NO)).verdict() == Verdict.FORM_GONDERILMEDI);
        check(DebugVerdict.judge(with(solid(), 4, Tri.NO)).verdict() == Verdict.CEVAP_BACKENDE_ULASMADI);
        check(DebugVerdict.judge(with(solid(), 5, Tri.YES)).verdict() == Verdict.PACKETEVENTS_IPTAL);
        check(DebugVerdict.judge(with(solid(), 6, Tri.NO)).verdict() == Verdict.DECODER_ONCESI_KAYIP);
        check(DebugVerdict.judge(with(solid(), 7, Tri.YES)).verdict() == Verdict.PROTOCOLLIB_IPTAL);
        check(DebugVerdict.judge(with(solid(), 8, Tri.NO)).verdict() == Verdict.BUKKIT_ONCESI_KAYIP);
        check(DebugVerdict.judge(with(solid(), 9, Tri.NO)).verdict() == Verdict.SMTHSHUB_ESLESMEDI);
        check(DebugVerdict.judge(with(solid(), 10, Tri.YES)).verdict() == Verdict.AKSIYON_HATASI);
        check(DebugVerdict.judge(with(solid(), 4, Tri.UNKNOWN)).verdict() == Verdict.OLCULEMEDI);
        check("R1".equals(DebugVerdict.judge(with(solid(), 4, Tri.UNKNOWN)).blocked()));
    }

    private static void handshakeOnly() {
        check(DebugVerdict.handshake(Tri.NO, Tri.NO, Tri.NO).verdict() == Verdict.JAVA_OYUNCU);
        check(DebugVerdict.handshake(Tri.YES, Tri.NO, Tri.NO).verdict() == Verdict.PROXY_FLOODGATE_VERISI_YOK);
        check(DebugVerdict.handshake(Tri.YES, Tri.YES, Tri.NO).verdict() == Verdict.BACKEND_FLOODGATE_OKUMUYOR);
        check(DebugVerdict.handshake(Tri.YES, Tri.YES, Tri.YES).verdict() == Verdict.HANDSHAKE_SAGLAM);
        check(DebugVerdict.handshake(Tri.UNKNOWN, Tri.NO, Tri.NO).verdict() == Verdict.OLCULEMEDI);
        check(DebugVerdict.handshake(Tri.YES, Tri.YES, Tri.UNKNOWN).verdict() == Verdict.OLCULEMEDI);
    }

    private static DebugVerdict.Facts solid() {
        return row(Tri.YES, Tri.YES, Tri.YES, Tri.YES, Tri.YES, Tri.NO, Tri.YES, Tri.NO, Tri.YES, Tri.YES, Tri.NO);
    }

    private static DebugVerdict.Facts with(DebugVerdict.Facts facts, int index, Tri value) {
        Tri[] values = new Tri[] {
                facts.floodgateUuid(), facts.marker(), facts.listed(), facts.formSent(), facts.raw(),
                facts.packetEventsCancel(), facts.decoded(), facts.protocolLibCancel(), facts.bukkit(),
                facts.act(), facts.actionFailed()
        };
        values[index] = value;
        return row(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8], values[9], values[10]);
    }

    private static DebugVerdict.Facts row(Tri uuid, Tri marker, Tri listed, Tri form, Tri raw, Tri packetEvents, Tri decoded, Tri protocol, Tri bukkit, Tri act, Tri failed) {
        return new DebugVerdict.Facts(uuid, marker, listed, form, raw, packetEvents, decoded, protocol, bukkit, act, failed);
    }

    private static DebugVerdict.Judgment expected(DebugVerdict.Facts facts) {
        Object[] steps = new Object[] {
                "uuid", facts.floodgateUuid(), null,
                "H", facts.marker(), null,
                "F", facts.listed(), Verdict.BACKEND_FLOODGATE_OKUMUYOR,
                "C", facts.formSent(), Verdict.FORM_GONDERILMEDI,
                "R1", facts.raw(), Verdict.CEVAP_BACKENDE_ULASMADI,
                "R3", facts.packetEventsCancel(), Verdict.PACKETEVENTS_IPTAL,
                "R4", facts.decoded(), Verdict.DECODER_ONCESI_KAYIP,
                "R6", facts.protocolLibCancel(), Verdict.PROTOCOLLIB_IPTAL,
                "R7", facts.bukkit(), Verdict.BUKKIT_ONCESI_KAYIP,
                "R8", facts.act(), Verdict.SMTHSHUB_ESLESMEDI,
                "aksiyon", facts.actionFailed(), Verdict.AKSIYON_HATASI
        };
        if (facts.floodgateUuid() == Tri.UNKNOWN) {
            return new DebugVerdict.Judgment(Verdict.OLCULEMEDI, "uuid");
        }
        if (facts.marker() == Tri.UNKNOWN) {
            return new DebugVerdict.Judgment(Verdict.OLCULEMEDI, "H");
        }
        if (facts.floodgateUuid() == Tri.NO && facts.marker() == Tri.NO) {
            return new DebugVerdict.Judgment(Verdict.JAVA_OYUNCU, "");
        }
        if (facts.marker() == Tri.NO) {
            return new DebugVerdict.Judgment(Verdict.PROXY_FLOODGATE_VERISI_YOK, "");
        }
        for (int i = 6; i < steps.length; i += 3) {
            String field = (String) steps[i];
            Tri value = (Tri) steps[i + 1];
            Verdict failure = (Verdict) steps[i + 2];
            if (value == Tri.UNKNOWN) {
                return new DebugVerdict.Judgment(Verdict.OLCULEMEDI, field);
            }
            boolean cancel = "R3".equals(field) || "R6".equals(field) || "aksiyon".equals(field);
            if (cancel && value == Tri.YES) {
                return new DebugVerdict.Judgment(failure, "");
            }
            if (!cancel && value == Tri.NO) {
                return new DebugVerdict.Judgment(failure, "");
            }
        }
        return new DebugVerdict.Judgment(Verdict.SAGLAM, "");
    }

    private static void frames() {
        String channel = "floodgate:form";
        check(FrameScan.reply(frame(new byte[0], channel, new byte[] {0, 0}, 0x0E), channel).id() == 0);
        check(FrameScan.reply(frame(new byte[0], channel, new byte[] {0, 1}, 0x0E), channel).id() == 1);
        check(FrameScan.reply(frame(new byte[] {9, 8, 7}, channel, new byte[] {4, 0}, 0x0E), channel).id() == 0x0400);
        check(FrameScan.reply(frame(new byte[] {1, 2}, channel, new byte[] {0, 2}, 0x0E), channel).at() > 0);
        byte[] end = frame(new byte[0], channel, new byte[0], 0x0E);
        check(FrameScan.reply(end, channel).idBytes() == 0);
        check(FrameScan.reply(frame(new byte[0], channel, new byte[] {7}, 0x0E), channel).idBytes() == 1);
        check(FrameScan.reply(frame(new byte[0], channel, new byte[] {1, 2}, 0x0E), channel).idBytes() == 2);
        check(FrameScan.reply(frame(new byte[0], channel, new byte[] {1, 2}, 0x0D), channel) == null);
        check(FrameScan.reply("floodgate:form".getBytes(StandardCharsets.UTF_8), channel) == null);
        check(FrameScan.reply(new byte[] {1, 2, 3, 4, 5}, channel) == null);
        byte[] overlong = frame(new byte[0], channel, new byte[] {0, 5}, -1);
        check(FrameScan.reply(overlong, channel).id() == 5);
        byte[] noisy = new byte[80];
        Arrays.fill(noisy, (byte) 0x11);
        System.arraycopy("floodgate:form".getBytes(StandardCharsets.UTF_8), 0, noisy, 20, 14);
        noisy[19] = 0x0D;
        check(FrameScan.reply(noisy, channel) == null);
        check(FrameScan.reply(new byte[] {(byte) 0x80}, channel) == null);
    }

    private static byte[] frame(byte[] prefix, String channel, byte[] after, int lengthByte) {
        byte[] name = channel.getBytes(StandardCharsets.UTF_8);
        int lengthSize = lengthByte < 0 ? 2 : 1;
        byte[] out = new byte[prefix.length + lengthSize + name.length + after.length];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        int cursor = prefix.length;
        if (lengthByte < 0) {
            out[cursor++] = (byte) 0x8E;
            out[cursor++] = 0;
        } else {
            out[cursor++] = (byte) lengthByte;
        }
        System.arraycopy(name, 0, out, cursor, name.length);
        System.arraycopy(after, 0, out, cursor + name.length, after.length);
        return out;
    }

    private static void varints() {
        check(VarIntRead.read(new byte[] {0}, 0).value() == 0);
        check(VarIntRead.read(new byte[] {127}, 0).value() == 127);
        check(VarIntRead.read(new byte[] {(byte) 128, 1}, 0).value() == 128);
        check(!VarIntRead.read(new byte[] {(byte) 128}, 0).ok());
        check(!VarIntRead.read(new byte[] {(byte) 128, (byte) 128, (byte) 128, (byte) 128, (byte) 128, 1}, 0).ok());
        check(VarIntRead.read(new byte[] {0x0E}, 0).value() == 14);
        byte[] five = new byte[] {(byte) 128, (byte) 128, (byte) 128, (byte) 128, 1};
        check(VarIntRead.read(five, 0).ok());
    }

    private static void handshakes() {
        HandshakeScan.Result plain = HandshakeScan.read(packet("localhost"));
        check(plain.parsed());
        check(!plain.marker());
        check(plain.hostLength() == "localhost".length());
        check(plain.parts() == 1);
        String marked = "localhost\0" + "1.1.1.1\0^Floodgate^3SECRET";
        HandshakeScan.Result hit = HandshakeScan.read(packet(marked));
        check(hit.parsed());
        check(hit.marker());
        check(hit.parts() == 3);
        check(hit.version() == '3');
        check(hit.hostLength() == marked.length());
        String buried = "x^Floodgate^9";
        check(!HandshakeScan.read(packet(buried)).marker());
        byte[] wide = new byte[300];
        Arrays.fill(wide, (byte) 'a');
        System.arraycopy("^Floodgate^7".getBytes(StandardCharsets.ISO_8859_1), 0, wide, 0, 12);
        HandshakeScan.Result longHost = HandshakeScan.read(packet(wide));
        check(longHost.parsed());
        check(longHost.hostLength() == 300);
        check(longHost.marker());
        check(longHost.version() == '7');
        check(!HandshakeScan.read(new byte[] {0x00, (byte) 0x80}).parsed());
        check(!HandshakeScan.read(new byte[] {(byte) 0x80}).parsed());
    }

    private static byte[] packet(String host) {
        return packet(host.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static byte[] packet(byte[] host) {
        byte[] out = new byte[1 + 2 + varLength(host.length) + host.length + 2 + 1];
        int cursor = 0;
        out[cursor++] = 0;
        out[cursor++] = (byte) 0x86;
        out[cursor++] = 0x06;
        cursor = writeVar(out, cursor, host.length);
        System.arraycopy(host, 0, out, cursor, host.length);
        cursor += host.length;
        out[cursor++] = (byte) 255;
        out[cursor++] = 99;
        out[cursor] = 2;
        return out;
    }

    private static int varLength(int value) {
        int size = 0;
        do {
            value >>>= 7;
            size++;
        } while (value != 0);
        return size;
    }

    private static int writeVar(byte[] out, int cursor, int value) {
        while ((value & ~0x7F) != 0) {
            out[cursor++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out[cursor++] = (byte) value;
        return cursor;
    }

    private static void fingerprints() {
        check("e3b0c44298fc1c14".equals(Fingerprint.prefix(new byte[0])));
        check("ba7816bf8f01cfea".equals(Fingerprint.prefix("abc".getBytes(StandardCharsets.UTF_8))));
    }

    private static void channelNotes() {
        check(DebugVerdict.channelFilter(Tri.YES, Tri.NO));
        check(!DebugVerdict.channelFilter(Tri.YES, Tri.YES));
        check(!DebugVerdict.channelFilter(Tri.YES, Tri.UNKNOWN));
        check(!DebugVerdict.channelFilter(Tri.NO, Tri.NO));
        check(!DebugVerdict.channelFilter(Tri.UNKNOWN, Tri.NO));
    }

    private static void check(boolean ok) {
        if (!ok) {
            throw new AssertionError("bedrock debug");
        }
    }
}
