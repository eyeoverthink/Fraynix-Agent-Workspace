package fraymus.workspace;

import java.time.*;
import java.util.*;
import org.json.*;

/** Cheap resource visibility only; original proposal and outcome checks retain authority. */
final class CalibrationResources088 {
    static JSONObject summarize(JSONArray samples, JSONObject profile, String code, Instant now) {
        if (samples.length()>64) throw new IllegalArgumentException("Bounded cost journal required");
        int matching=0, foreign=0, stale=0, invalid=0, otherCode=0, otherReader=0;
        var seen=new HashSet<String>();
        for (Object raw:samples) try {
            var s=(JSONObject)raw; String id=s.getString("id"),kind=s.getString("kind");
            if(id.isBlank()||!seen.add(id)||!Set.of("fit","check").contains(kind)) { invalid++; continue; }
            double millis=s.getDouble("millis");
            if(!Double.isFinite(millis)||millis<=0) { invalid++; continue; }
            var at=Instant.parse(s.getString("at"));
            if(at.isAfter(now)||at.isBefore(now.minus(Duration.ofDays(14)))) { stale++; continue; }
            var shape=s.getJSONObject("shape");
            if(!code.equals(shape.getString("code"))) { otherCode++; continue; }
            boolean normal=kind.equals("fit")?CalibrationCosts087.normalReader(s.optString("readerMode","parallel")):
                CalibrationCosts087.normalReader(s.getJSONObject("before").getString("mode"))&&CalibrationCosts087.normalReader(s.getJSONObject("after").getString("mode"));
            if(!normal) { otherReader++; continue; }
            if(profile.similar(shape.getJSONObject("profile"))) matching++; else foreign++;
        } catch(RuntimeException malformed) { invalid++; }
        return new JSONObject().put("version","calibration-resources-088-v1").put("profile",WorldFrames.copy(profile))
            .put("total",samples.length()).put("matchingProfile",matching).put("differentProfile",foreign)
            .put("staleOrFuture",stale).put("invalidOrDuplicate",invalid).put("differentCode",otherCode).put("differentReader",otherReader)
            .put("scope","Journal metadata only. Matching resources do not establish workload compatibility or verify original sources. Proposal review and original fitting checks remain required. CPU and heap do not measure disk contention, thermal changes or other applications.");
    }
}
