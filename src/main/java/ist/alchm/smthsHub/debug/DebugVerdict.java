package ist.alchm.smthsHub.debug;

public final class DebugVerdict {

    public record Facts(
            Tri listed,
            Tri floodgateListener,
            Tri formSent,
            Tri r1Measuring,
            Tri formReply,
            Tri packetEventsCancel,
            Tri decoded,
            Tri protocolLibCancel,
            Tri bukkit,
            Tri action
    ) {
    }

    public record Judgment(Verdict verdict, String blocked) {
    }

    private DebugVerdict() {
    }

    public static Judgment judge(Facts facts) {
        if (facts.listed == Tri.UNKNOWN) {
            return blocked("F");
        }
        if (facts.listed == Tri.NO) {
            return done(Verdict.FLOODGATE_TANIMIYOR);
        }
        if (facts.floodgateListener == Tri.UNKNOWN) {
            return blocked("F-dinleyici");
        }
        if (facts.floodgateListener == Tri.NO) {
            return done(Verdict.FLOODGATE_DINLEYICISI_YOK);
        }
        if (facts.formSent == Tri.UNKNOWN) {
            return blocked("C");
        }
        if (facts.formSent == Tri.NO) {
            return done(Verdict.FORM_GONDERILMEDI);
        }
        if (facts.r1Measuring != Tri.YES) {
            return blocked("R1");
        }
        if (facts.formReply == Tri.UNKNOWN) {
            return blocked("R1-cevap");
        }
        if (facts.formReply == Tri.NO) {
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
        if (facts.action == Tri.UNKNOWN) {
            return blocked("aksiyon");
        }
        if (facts.action == Tri.NO) {
            return done(Verdict.FLOODGATE_ESLESTIRMEDI);
        }
        return done(Verdict.SAGLAM);
    }

    private static Judgment done(Verdict verdict) {
        return new Judgment(verdict, "");
    }

    private static Judgment blocked(String field) {
        return new Judgment(Verdict.OLCULEMEDI, field);
    }
}
