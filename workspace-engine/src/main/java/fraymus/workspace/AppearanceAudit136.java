package fraymus.workspace;

import java.util.*;
import org.json.*;

/** Downstream only. Source association audit has already checked every original frame and binding. */
final class AppearanceAudit136 {
    static JSONObject compute(JSONObject labels,JSONObject dataset,JSONObject run,String body) {
        var references=new HashMap<Integer,JSONObject>();
        for(Object raw:labels.getJSONArray("rows")){var l=(JSONObject)raw;references.put(l.getInt("ordinal"),l);}
        JSONObject seed=null;for(Object raw:dataset.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("key").equals(labels.getString("objectKey")))seed=o;}
        if(seed==null)throw new IllegalArgumentException("Original source seed missing");
        var active=new ArrayList<JSONObject>();var admissions=new JSONArray();var recoveries=new JSONArray();
        int correct=0,incorrect=0,unknown=0,recovered=0,recoveryWrong=0,recoveryUnknown=0;
        boolean found=false,enabled=false;
        var trace=run.getJSONArray("trace");
        for(int i=0;i<trace.length();i++){
            var frame=trace.getJSONObject(i);int ordinal=frame.getInt("ordinal");JSONObject object=null;
            for(Object raw:frame.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("bodyId").equals(body))object=o;}
            if(object==null)throw new IllegalArgumentException("Missing original object");
            var tracker=object.getJSONObject("tracker");var bank=tracker.optJSONObject("appearance135");
            if(bank==null){if(found)throw new IllegalArgumentException("Appearance evidence interrupted");continue;}
            if(i>0&&!found)throw new IllegalArgumentException("Appearance evidence starts late");
            if(!found){enabled=bank.getBoolean("enabled");var initial=bank.getJSONArray("examples").getJSONObject(0);
                if(!initial.getString("descriptorSha256").matches("[a-f0-9]{64}")||initial.getInt("sourceOrdinal")!=0||!initial.getJSONArray("box").similar(seed.getJSONArray("box"))||!initial.getJSONObject("checks").getString("verification").equals("initial-source-seed"))throw new IllegalArgumentException("Original appearance seed changed");
                active.add(initial);found=true;}
            if(!bank.getString("schema").equals("seed-anchored-136-v1")||bank.getBoolean("enabled")!=enabled||bank.getInt("ordinal")!=ordinal||bank.getInt("capacity")!=AppearanceFlow136.CAPACITY)throw new IllegalArgumentException("Appearance trace identity changed");
            var admission=bank.getJSONObject("admission");
            if(admission.getBoolean("accepted")){
                var e=admission.getJSONObject("example");var c=e.getJSONObject("checks");
                if(!enabled||e.getInt("sourceOrdinal")!=ordinal||tracker.isNull("box")||!e.getJSONArray("box").similar(tracker.getJSONArray("box"))||!bank.getBoolean("incumbentAccepted")
                    ||!e.getString("descriptorSha256").matches("[a-f0-9]{64}")||!c.getString("verification").equals("seed-cycle-136")
                    ||c.getInt("consecutiveStrongFrames")<3||c.getInt("previousSourceOrdinal")!=ordinal-1||c.getDouble("reverseOverlap")<.85
                    ||c.getDouble("reportedSpanX")<.2||c.getDouble("reportedSpanY")<.04||c.getDouble("originalColorAgreement")<.7||c.getDouble("nearestAppearance")>=.995)throw new IllegalArgumentException("Appearance admission conditions changed");
                for(String direction:List.of("forward","reverse")){var m=c.getJSONObject(direction);if(!m.getString("method").equals("pyramidal-flow")||m.getBoolean("ambiguous")||m.getInt("inliers")<16||m.getInt("inliers")<.6*m.getInt("seedPoints")||m.getDouble("medianForwardBackwardPixels")>.35)throw new IllegalArgumentException("Appearance motion conditions changed");}
                var a=c.getJSONObject("anchor136");var sourceBox=PixelTracker.Box.from(seed.getJSONArray("box"));var proposed=PixelTracker.Box.from(e.getJSONArray("box"));
                if(!a.getBoolean("accepted")||a.getInt("sourceOrdinal")!=0||!a.getJSONArray("sourceBox").similar(seed.getJSONArray("box"))||!a.getJSONArray("proposedBox").similar(e.getJSONArray("box")))throw new IllegalArgumentException("Immutable source anchor changed");
                double overlap=PixelTracker.iou(proposed,PixelTracker.Box.from(a.getJSONArray("forwardBox"))),back=PixelTracker.iou(sourceBox,PixelTracker.Box.from(a.getJSONArray("reverseBox")));
                if(overlap<.75||back<.75||Math.abs(overlap-a.getDouble("overlap"))>1e-12||Math.abs(back-a.getDouble("reverseOverlap"))>1e-12)throw new IllegalArgumentException("Immutable source geometry changed");
                for(String direction:List.of("forward","reverse")){var m=a.getJSONObject(direction);if(!m.getString("method").equals("pyramidal-flow")||m.getBoolean("ambiguous")||m.getInt("inliers")<16||m.getInt("inliers")<.6*m.getInt("seedPoints")||m.getDouble("medianForwardBackwardPixels")>.35)throw new IllegalArgumentException("Immutable source motion changed");}
                if(active.size()==AppearanceFlow136.CAPACITY){if(!active.remove(1).similar(admission.getJSONObject("retired")))throw new IllegalArgumentException("Retired appearance changed");}
                else if(!admission.isNull("retired"))throw new IllegalArgumentException("Unexpected appearance retirement");
                active.add(e);var scored=score(references.get(ordinal),e.getJSONArray("box"));
                switch(scored.getString("verdict")){case "correct"->correct++;case "incorrect"->incorrect++;default->unknown++;}
                admissions.put(scored.put("ordinal",ordinal).put("sourceFrame",frame.getInt("sourceFrame")).put("imageSha256",frame.getString("imageSha256")).put("example",e));
            }
            var actual=bank.getJSONArray("examples");if(actual.length()!=active.size())throw new IllegalArgumentException("Appearance bank size changed");
            for(int j=0;j<active.size();j++)if(!active.get(j).similar(actual.getJSONObject(j)))throw new IllegalArgumentException("Retained appearance provenance changed");
            var recovery=bank.getJSONObject("recovery");
            if(recovery.getBoolean("accepted")){
                int source=recovery.getInt("sourceExampleOrdinal");boolean prior=false;
                for(var e:active)if(e.getInt("sourceOrdinal")==source&&source<ordinal)prior=true;
                if(!enabled||!prior||bank.getBoolean("incumbentAccepted")||tracker.isNull("box")||!tracker.getString("status").equals("reacquired"))throw new IllegalArgumentException("Recovered appearance provenance changed");
                var scored=score(references.get(ordinal),tracker.getJSONArray("box"));boolean accepted=object.getJSONObject("estimate").getBoolean("accepted");
                switch(scored.getString("verdict")){case "correct"->{if(accepted)recovered++;}case "incorrect"->recoveryWrong++;default->recoveryUnknown++;}
                recoveries.put(scored.put("ordinal",ordinal).put("sourceFrame",frame.getInt("sourceFrame")).put("imageSha256",frame.getString("imageSha256")).put("sourceExampleOrdinal",source).put("filterAccepted",accepted));
            }
        }
        return new JSONObject().put("available",found).put("enabled",enabled).put("admissions",admissions).put("recoveries",recoveries)
            .put("summary",new JSONObject().put("admitted",admissions.length()).put("independentlyCorrect",correct).put("incorrect",incorrect).put("unknown",unknown)
                .put("bankRecoveries",recoveries.length()).put("correctAcceptedRecoveries",recovered).put("incorrectRecoveries",recoveryWrong).put("unknownRecoveries",recoveryUnknown))
            .put("scope","Original-seed and local motion checks are not identity truth. Separate labels score each admission and bank recovery; unlabelled/uncertain examples remain unknown. Initial seed is excluded from learned-example counts. Historical evidence retains its original source and code identity.");
    }

    static JSONObject frameSummary(JSONObject bank) {
        var sources=new JSONArray();for(Object raw:bank.getJSONArray("examples"))sources.put(((JSONObject)raw).getInt("sourceOrdinal"));
        return new JSONObject().put("exampleCount",sources.length()).put("exampleSources",sources)
            .put("admissionReason",bank.getJSONObject("admission").getString("reason")).put("recovery",WorldFrames.copy(bank.getJSONObject("recovery")));
    }

    private static JSONObject score(JSONObject label,JSONArray box) {
        if(label==null||label.getString("visibility").equals("uncertain"))return new JSONObject().put("verdict","unknown").put("targetOverlap",JSONObject.NULL);
        boolean visible=Set.of("visible","partial").contains(label.getString("visibility"));
        double overlap=visible?PixelTracker.iou(PixelTracker.Box.from(box),PixelTracker.Box.from(label.getJSONArray("targetBox"))):0;
        return new JSONObject().put("verdict",visible&&overlap>=.5?"correct":"incorrect").put("targetOverlap",overlap).put("visibility",label.getString("visibility"));
    }
}
