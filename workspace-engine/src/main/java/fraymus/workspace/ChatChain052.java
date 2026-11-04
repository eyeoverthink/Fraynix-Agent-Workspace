package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Presentation/context projections of the original checked task. Never an execution authority. */
final class ChatChain052 {
    private ChatChain052() {}

    static String objectiveHash(JSONObject g) {
        return WorldFrames.hash(new JSONObject().put("goal", g.getString("goal")));
    }

    static void verifyObjective(JSONObject g) {
        for (Object raw : g.getJSONArray("history")) {
            var row = (JSONObject) raw;
            if (row.getString("kind").equals("queued") && row.getJSONObject("detail").has("objectiveSha256")) {
                if (!objectiveHash(g).equals(row.getJSONObject("detail").getString("objectiveSha256")))
                    throw new IllegalStateException("Original task objective changed; start a new task");
            }
        }
    }

    /** Reduce discovery and historical diagnostics; keep selected identity and execution requirements exact. */
    static JSONObject context(JSONObject g) {
        var c = WorldFrames.copy(g.getJSONObject("context044"));
        if (c.has("available044")) {
            var original = c.getJSONObject("available044");
            var bound=c.optJSONObject("procedureBinding");
            if(bound!=null)for(Object raw:original.optJSONArray("procedures",new JSONArray())){
                var procedure=(JSONObject)raw;
                if(procedure.optString("name").equals(bound.getString("name"))&&procedure.optInt("version")==bound.getInt("version"))
                    c.put("selectedProcedure052",procedureContext(procedure));
            }
            var catalog = new JSONObject();
            for (String key : original.keySet()) {
                var entries = original.getJSONArray(key);
                var selected = new JSONArray();
                int count = Math.min(entries.length(), 4);
                for (int i = 0; i < count; i++) {
                    int index = i < (count+1)/2 ? i : entries.length()-(count-i);
                    var entry = entries.getJSONObject(index);
                    var item = new JSONObject().put("catalogIndex", index);
                    for (String field : List.of("id","name","title","version","kind","sourceVersion"))
                        if (entry.has(field)) item.put(field, entry.get(field));
                    selected.put(item);
                }
                catalog.put(key, new JSONObject().put("total",entries.length()).put("omittedEntries",entries.length()-count)
                        .put("sha256",WorldFrames.hash(new JSONObject().put("entries",entries))).put("identitySummaries",selected));
            }
            c.put("available044",catalog).put("catalogScope052","Discovery identities only; full catalog through registered read tools. Exact selected bindings remain above.");
        }
        c.put("retainedDiagnostics052", diagnostics(g));
        return c;
    }

    private static JSONObject procedureContext(JSONObject procedure) {
        var selected=WorldFrames.copy(procedure);
        if(selected.has("payload057")) {
            var original=selected.getJSONObject("payload057");
            var summary=new JSONObject().put("originalSummarySha256",WorldFrames.hash(original));
            for(String key:List.of("version","codeSha256","sourceRunSha256","originalSha256","complete","repeatable","attempts","totalTicks","observedWallMillis","scope"))
                if(original.has(key))summary.put(key,original.get(key));
            var outcomes=new JSONArray();
            for(Object raw:original.getJSONArray("results")) {
                var result=(JSONObject)raw;
                outcomes.put(new JSONObject().put("caseId",result.getString("caseId"))
                    .put("stable",result.getJSONObject("metrics").getBoolean("stable"))
                    .put("repeatAgrees",result.getBoolean("repeatAgrees")));
            }
            summary.put("outcomes",outcomes).put("projectionScope","Historical measurements summarized for planning only. Java retains the complete procedure and report, resolves its exact name/version and reruns the unchanged payload policy. Defaults, policy, prerequisites, actions and recovery are not reduced.");
            selected.remove("payload057");selected.put("historicalPayloadEvidence057",summary);
        }
        return selected;
    }

    /** Never silently erase an essential field or leave a partial JSON document. */
    static String exact(Object value, int limit) {
        String s=String.valueOf(value);
        if(s.length()>limit) throw new IllegalStateException("Task context exceeds the checked "+limit+" character budget; narrow the task or source selection. No requirements were silently removed.");
        return s;
    }

    /** The recent window is a bounded observation, never the original action receipt. */
    static JSONObject recentObservation(JSONObject result) {
        String text=result.toString();
        if(text.length()<=6000)return WorldFrames.copy(result);
        var summary=new JSONObject().put("observationSummary052",true).put("originalChars",text.length())
                .put("originalSha256",WorldFrames.hash(result))
                .put("scope","Large observation omitted from recent context; use its original source/result record. This summary is not a complete observation or proof.");
        for(String key:List.of("id","worldId","datasetId","version","expectedRevision","expectedHash","status","reason","error"))
            if(result.has(key))summary.put(key,result.get(key));
        exact(summary,6000);
        return summary;
    }

    static JSONArray diagnostics(JSONObject g) {
        var groups = new LinkedHashMap<String,JSONObject>();
        for(Object raw:g.optJSONArray("history",new JSONArray())) {
            var row=(JSONObject)raw;
            if(!Set.of("repair","plan-rejected","interrupted").contains(row.getString("kind"))) continue;
            var detail=row.getJSONObject("detail");
            String message=detail.optString("reason",detail.optString("diagnostic",""));
            String key=message.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
            var group=groups.computeIfAbsent(key,k->new JSONObject().put("message",message).put("occurrences",new JSONArray()));
            group.getJSONArray("occurrences").put(new JSONObject().put("kind",row.getString("kind")).put("at",row.getString("at")).put("sha256",row.getString("sha256")));
        }
        return new JSONArray(groups.values());
    }

    static JSONObject timing(JSONObject g) {
        int calls=0,failed=0,terminal=0;double millis=0;boolean complete=false;
        for(Object raw:g.getJSONArray("history")) {
            var row=(JSONObject)raw;var d=row.getJSONObject("detail");
            if(row.getString("kind").equals("queued")&&d.has("objectiveSha256"))complete=true;
            if(row.getString("kind").equals("model-call")){calls++;millis+=d.getDouble("durationMs");if(!d.getBoolean("returned"))failed++;}
            if(row.getString("kind").equals("terminal-check"))terminal++;
        }
        return new JSONObject().put("completeSinceAdmission",complete&&g.optInt("turn")==calls).put("modelCalls",calls).put("failedModelCalls",failed).put("modelMillis",millis)
                .put("terminalChecksWithoutModel",terminal).put("scope","Measured Java planner-request wall time, including local admission and inference. Not simulated time or an estimate of time saved.");
    }

    static JSONObject evidence(JSONObject reply) {
        var out=new JSONObject();var raw=reply.opt("evidence");
        var e=raw instanceof JSONObject j?j:raw instanceof Map<?,?> m?new JSONObject(m):new JSONObject();
        for(String k:List.of("total_duration","load_duration","prompt_eval_count","prompt_eval_duration","eval_count","eval_duration","done_reason"))
            if(e.has(k))out.put(k,e.get(k));
        return out;
    }

    static String summary(JSONObject g,JSONObject meta) {
        String family=g.getString("family044");
        var out=new StringBuilder("Java completed the "+family+" task and checked its actual receipts. ");
        var child=meta.optJSONObject("javaGoal");
        if(child!=null){
            out.append(child.optString("reason","Native task completed")).append(". ");
            if(child.optBoolean("payloadDesign058")){var r=child.getJSONObject("payload057");out.append("Measured-failure redesign: ").append(r.getJSONObject("binding").getString("selectedCandidate")).append("; fresh design checks ").append(r.getBoolean("freshDesignPassed")?"passed":"failed").append("; unfamiliar payload checks ").append(r.getBoolean("selectedReservedPassed")?"passed":"failed").append(". Decision fixed before reserved evaluation; original baseline failures retained. ");}else if(child.has("payload057")){out.append("Dynamic payloads and new load positions: ");for(Object raw:child.getJSONObject("payload057").getJSONArray("results")){var r=(JSONObject)raw;out.append(r.getString("caseId")).append(r.getJSONObject("metrics").getBoolean("stable")?" stable; ":" unstable; ");}out.append("All seven cases repeated; original failures retained. These outcomes do not establish material strength. ");}
            if(child.has("load056"))out.append("Measured load limits and verified redesign: ").append(Load056.label(child.getJSONObject("load056"))).append(". Original failures retained; this is rigid-body stability, not material strength. ");if(child.has("design053")){var d=child.getJSONObject("counterfactual045");if(child.optBoolean("directFirst055"))out.append("Tried simpler control first; selected ").append(d.getString("selectedCandidate")).append(" after original native verification. ");else out.append("Compared ").append(d.getJSONArray("candidates").length()).append(" assembly strategies; selected ").append(d.getString("selectedCandidate")).append(" by verified feasibility and measured effort. ");if(d.has("baseline054"))out.append(d.getJSONObject("baseline054").getString("verdict")).append(". ");}
            var m=child.optJSONObject("metrics",new JSONObject());
            if(child.has("load055")){out.append("Independent load experiments: ");for(Object raw:child.getJSONObject("load055").getJSONArray("results")){var result=(JSONObject)raw;out.append(result.getString("caseId")).append(result.getJSONObject("metrics").getBoolean("stable")?" stable; ":" unstable; ");}out.append("These isolated rigid-body experiments do not establish material strength. ");}
            var v=child.optJSONObject("constructionVerification051");
            if(v!=null)out.append(m.optInt("releases")).append(" releases; ").append(v.optInt("supportContacts")).append(" support contacts; settled structure ").append(v.optBoolean("passed")?"passed":"failed").append(". ");
            if(child.has("worldCommit"))out.append("World revision ").append(child.getJSONObject("worldCommit").getInt("revision")).append(" saved. ");
            if(m.has("meanSpeedMps"))out.append(String.format(Locale.ROOT,"Mean speed %.6f m/s. ",m.getDouble("meanSpeedMps")));
            if(m.has("qualityPassed")&&!m.getBoolean("qualityPassed"))out.append("Data quality checks did not pass; inspect the retained flags. ");
            if(child.has("savedSkill")){var s=child.getJSONObject("savedSkill");out.append("Saved skill: ").append(s.getString("name")).append(" v").append(s.getInt("version")).append(". ");}
        }else if(meta.has("alignmentFacts"))out.append("Selected alignment: ").append(meta.getJSONObject("alignmentFacts").optString("registrationId")).append(". No independent physical evaluation was performed by this read task. ");
        out.append("Open Mission for the full source evidence and checks. Native simulation and physical observations keep their original scope.");
        return out.toString();
    }
}
