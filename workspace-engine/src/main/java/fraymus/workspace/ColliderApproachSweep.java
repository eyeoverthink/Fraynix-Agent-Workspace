package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** A finite comparison matrix. Each case uses identical probe launches for the two boxes. */
final class ColliderApproachSweep {
    private static final String FORMAT = "fraynix.collider-approach-sweep-v1";
    private final ColliderApproachComparison.Plan pair;
    private final List<Case> cases;
    private final JSONObject binding;

    private record Case(double headingDeg, double lateralOffsetM, SpatialPlacement.ProbeApproach approach) {
        JSONObject definition() {
            return new JSONObject().put("headingDeg",headingDeg).put("lateralOffsetM",lateralOffsetM)
                    .put("approach",approach.json());
        }
    }

    ColliderApproachSweep(ColliderApproachComparison.Plan pair) {
        this.pair = pair;
        var shape = pair.binding();
        var review = pair.resized().evidence().getJSONObject("review");
        var envelope = ColliderApproachComparison.envelope(shape.getJSONArray("beforeColliderM"),shape.getJSONArray("afterColliderM"));
        cases = List.of(new Case(0,0,pair.approach()),
                new Case(-25,0,SpatialPlacement.angledApproach(review,envelope,-25,0)),
                new Case(25,0,SpatialPlacement.angledApproach(review,envelope,25,0)),
                new Case(0,-.12,SpatialPlacement.angledApproach(review,envelope,0,-.12)),
                new Case(0,.12,SpatialPlacement.angledApproach(review,envelope,0,.12)));
        var definitions = new JSONArray();
        for (var variant : cases) definitions.put(variant.definition());
        binding = new JSONObject().put("format",FORMAT).put("pair",shape).put("cases",definitions);
    }

    JSONObject run(Path nativeDir) throws Exception {
        var measured = new JSONArray();
        for (var variant : cases) {
            try {
                measured.put(pair.runCase(nativeDir,variant.approach()));
            } catch (IllegalArgumentException | IllegalStateException failure) {
                throw new IllegalStateException("Approach at "+variant.headingDeg()+"° and "+variant.lateralOffsetM()
                        +" m offset failed verification. "+failure.getMessage(),failure);
            }
        }
        var receipt = new JSONObject().put("binding",binding).put("cases",measured).put("summary",summarize(measured));
        return receipt.put("sha256",WorldFrames.hash(receipt));
    }

    JSONObject read(Path path) {
        try {
            var receipt = new JSONObject(new String(SpriteWorkshop.read(path,PlacementExchange.BYTE_LIMIT),StandardCharsets.UTF_8));
            SpatialScene.keys(receipt,"binding","cases","summary","sha256");
            String hash = receipt.getString("sha256");
            receipt.remove("sha256");
            if (!WorldFrames.hash(receipt).equals(hash) || !binding.similar(receipt.getJSONObject("binding"))) return null;
            var measured = receipt.getJSONArray("cases");
            if (measured.length() != cases.size()) return null;
            for (int i=0;i<cases.size();i++) {
                var trial = measured.getJSONObject(i);
                SpatialScene.keys(trial,"original","resized","result");
                if (!pair.validCase(trial,cases.get(i).approach().json())) return null;
            }
            return summarize(measured).similar(receipt.getJSONObject("summary")) ? receipt.put("sha256",hash) : null;
        } catch (IOException | RuntimeException changed) {
            return null;
        }
    }

    private static JSONObject summarize(JSONArray measured) {
        int earlier=0, tied=0, later=0, min=Integer.MAX_VALUE, max=Integer.MIN_VALUE;
        for (Object entry : measured) {
            int delta = ((JSONObject)entry).getJSONObject("result").getInt("resizedEarlierByTicks");
            min = Math.min(min,delta);
            max = Math.max(max,delta);
            if (delta>0) earlier++;
            else if (delta<0) later++;
            else tied++;
        }
        return new JSONObject().put("approaches",measured.length()).put("sphereTrials",measured.length()*8)
                .put("earlier",earlier).put("tied",tied).put("later",later)
                .put("minLeadTicks",min).put("maxLeadTicks",max);
    }
}
