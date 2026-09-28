package ist.alchm.smthsHub.debug;

public final class DebugVerdict {

    public record Facts(
            Tri floodgateUuid,
            Tri marker,
            Tri listed,
            Tri formSent,
            Tri raw,
            Tri packetEventsCancel,
            Tri decoded,
            Tri protocolLibCancel,
            Tri bukkit,
            Tri act,
            Tri actionFailed
    ) {
    }

    public record Judgment(Verdict verdict, String blocked) {
    }

    private DebugVerdict() {
    }

    public static Judgment handshake(Tri floodgateUuid, Tri marker, Tri listed) {
        if (floodgateUuid == Tri.UNKNOWN || marker == Tri.UNKNOWN) {
            return blocked(floodgateUuid == Tri.UNKNOWN ? "uuid" : "H");
        }
        if (floodgateUuid == Tri.NO && marker == Tri.NO) {
            return done(Verdict.JAVA_OYUNCU);
        }
        if (marker == Tri.NO) {
            return done(Verdict.PROXY_FLOODGATE_VERISI_YOK);
        }
        if (listed == Tri.UNKNOWN) {
            return blocked("F");
        }
        if (listed == Tri.NO) {
            return done(Verdict.BACKEND_FLOODGATE_OKUMUYOR);
        }
        return done(Verdict.HANDSHAKE_SAGLAM);
    }

    public static Judgment judge(Facts facts) {
        if (facts.floodgateUuid == Tri.UNKNOWN) {
            return blocked("uuid");
        }
        if (facts.marker == Tri.UNKNOWN) {
            return blocked("H");
        }
        if (facts.floodgateUuid == Tri.NO && facts.marker == Tri.NO) {
            return done(Verdict.JAVA_OYUNCU);
        }
        if (facts.marker == Tri.NO) {
            return done(Verdict.PROXY_FLOODGATE_VERISI_YOK);
        }
        if (facts.listed == Tri.UNKNOWN) {
            return blocked("F");
        }
        if (facts.listed == Tri.NO) {
            return done(Verdict.BACKEND_FLOODGATE_OKUMUYOR);
        }
        if (facts.formSent == Tri.UNKNOWN) {
            return blocked("C");
        }
        if (facts.formSent == Tri.NO) {
            return done(Verdict.FORM_GONDERILMEDI);
        }
        if (facts.raw == Tri.UNKNOWN) {
            return blocked("R1");
        }
        if (facts.raw == Tri.NO) {
            return done(Verdict.CEVAP_BACKENDE_ULASMADI);
        }
        if (facts.packetEventsCancel == Tri.UNKNOWN) {
            return blocked("R3");
        }
        if (facts.packetEventsCancel == Tri.YES) {
            return done(Verdict.PACKETEVENTS_IPTAL);
        }
        if (facts.decoded == Tri.UNKNOWN) {
            return blocked("R4");
        }
        if (facts.decoded == Tri.NO) {
            return done(Verdict.DECODER_ONCESI_KAYIP);
        }
        if (facts.protocolLibCancel == Tri.UNKNOWN) {
            return blocked("R6");
        }
        if (facts.protocolLibCancel == Tri.YES) {
            return done(Verdict.PROTOCOLLIB_IPTAL);
        }
        if (facts.bukkit == Tri.UNKNOWN) {
            return blocked("R7");
        }
        if (facts.bukkit == Tri.NO) {
            return done(Verdict.BUKKIT_ONCESI_KAYIP);
        }
        if (facts.act == Tri.UNKNOWN) {
            return blocked("R8");
        }
        if (facts.act == Tri.NO) {
            return done(Verdict.SMTHSHUB_ESLESMEDI);
        }
        if (facts.actionFailed == Tri.UNKNOWN) {
            return blocked("aksiyon");
        }
        if (facts.actionFailed == Tri.YES) {
            return done(Verdict.AKSIYON_HATASI);
        }
        return done(Verdict.SAGLAM);
    }

    public static boolean channelFilter(Tri probeBukkit, Tri formBukkit) {
        return probeBukkit == Tri.YES && formBukkit == Tri.NO;
    }

    private static Judgment done(Verdict verdict) {
        return new Judgment(verdict, "");
    }

    private static Judgment blocked(String field) {
        return new Judgment(Verdict.OLCULEMEDI, field);
    }
}
