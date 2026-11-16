package fraymus.workspace;

import java.util.*;
import org.json.*;

/** Explicit post-audit source retention. Novelty and source validity are different checks. */
final class StartingReference150 {
    static final String SCHEMA="verified-starting-reference-150-v1";
    static final String KIND="verified-starting-reference-150";
    static final int SUPPORT=3;
    static final double MIN_OVERLAP=.85;

    static String identity() throws Exception {
        try(var in=StartingReference150.class.getResourceAsStream("StartingReference150.class")) {
            return PerceptionStore.hash(in.readAllBytes());
        }
    }

    static JSONObject evaluate(AssociationAudit134 association,String auditId,String storageIdentity) throws Exception {
        var audit=association.readAudit(auditId);
        if(!audit.getString("trackerPolicy").equals("appearance-continuity-135")
            ||!audit.getJSONObject("appearance135").getBoolean("available")
            ||!audit.getJSONObject("appearance135").getBoolean("enabled"))
            throw new IllegalArgumentException("Use an independently audited source appearance run, not reference-derived predictions");
        var dataset=association.verifiedDataset(audit.getString("datasetId"));
        if(dataset.getJSONArray("objects").length()!=1)throw new IllegalArgumentException("One source object required");
        var labels=association.read(audit.getJSONObject("input").getString("labelId")).getJSONObject("input");
        if(!labels.getString("exposure").equals("before-tracking"))
            throw new IllegalArgumentException("Starting reference requires source labels declared before tracking");
        var rows=new HashMap<Integer,JSONObject>();
        for(Object raw:audit.getJSONArray("scores")){var row=(JSONObject)raw;rows.put(row.getInt("ordinal"),row);}
        var support=new JSONArray();
        for(int ordinal=1;ordinal<=SUPPORT;ordinal++) {
            var row=rows.get(ordinal);
            if(row==null||!row.getString("visibility").equals("visible")||!row.getBoolean("accepted")
                ||!row.getBoolean("correct")||row.getDouble("targetOverlap")<MIN_OVERLAP
                ||row.getBoolean("confirmedDistractorMatch")||row.getDouble("otherOverlap")>=.2)
                throw new IllegalArgumentException("Starting reference requires first three consecutive fully visible, independently correct observations without competing-object overlap");
            support.put(new JSONObject().put("ordinal",ordinal).put("sourceFrame",row.getInt("sourceFrame"))
                .put("imageSha256",row.getString("imageSha256")).put("targetBox",row.getJSONArray("targetBox"))
                .put("measuredBox",row.getJSONArray("box")).put("targetOverlap",row.getDouble("targetOverlap"))
                .put("otherOverlap",row.getDouble("otherOverlap")));
        }
        // The first scored frame is independently labelled. The unlabelled frame-zero seed is not certified.
        var first=rows.get(1);var source=association.source(dataset.getString("id"),1);
        byte[] pixels=Base64.getDecoder().decode(source.getString("image"));
        if(!PerceptionStore.hash(pixels).equals(first.getString("imageSha256")))throw new IllegalStateException("Original starting-reference pixels changed");
        var box=PixelTracker.Box.from(first.getJSONArray("targetBox"));
        var descriptor=new JSONArray(PixelTracker.descriptor(PixelTracker.working(WorldObservationStore.decode(pixels)),box));
        var example=new JSONObject().put("ordinal",1).put("sourceFrame",first.getInt("sourceFrame"))
            .put("imageSha256",first.getString("imageSha256")).put("referenceBox",box.json())
            .put("proposedBox",first.getJSONArray("box")).put("proposedOverlap",first.getDouble("targetOverlap"))
            .put("descriptor",descriptor).put("descriptorSha256",WorldFrames.hash(new JSONObject().put("values",descriptor)))
            .put("evidenceKind",KIND).put("learned",false)
            .put("verification","First scored source image; three adjacent independently labelled full observations, overlap >=0.85; descriptor rebuilt from original pixels and independent box");
        return new JSONObject().put("schema",SCHEMA).put("evidenceKind",KIND)
            .put("input",new JSONObject().put("auditId",auditId).put("retentionMode",KIND))
            .put("codeSha256",storageIdentity).put("referenceCodeSha256",identity())
            .put("trackerCodeSha256",audit.getString("trackerCodeSha256"))
            .put("auditSha256",WorldFrames.hash(audit)).put("datasetId",dataset.getString("id"))
            .put("datasetSha256",WorldFrames.hash(dataset)).put("examples",new JSONArray().put(example))
            .put("startingReferenceCount",1).put("learnedExampleCount",0).put("support",support)
            .put("scope","Explicit verified starting observation, not a novel learned appearance. Source identity and same-object correspondence remain operator/annotation assumptions. All original source, code and outcome checks repeat before reuse; no evaluation predictions become references.");
    }
}
