package fraymus.workspace;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

/** Compares the poses either side of an approved resize using one calibrated launch. */
final class ColliderApproachComparison {
    private static final String FORMAT = "fraynix.fixed-collider-approach-v1";

    record Plan(JSONObject binding, PlacementExchange.Stage original, PlacementExchange.Stage resized,
                SpatialPlacement.ProbeApproach approach) {
        JSONObject run(Path nativeDir) throws Exception {
            var data = runCase(nativeDir,approach).put("binding",binding);
            return data.put("sha256",WorldFrames.hash(data));
        }

        JSONObject runCase(Path nativeDir, SpatialPlacement.ProbeApproach shared) throws Exception {
            var a = verify(original,nativeDir,shared);
            var b = verify(resized,nativeDir,shared);
            return new JSONObject().put("original",a).put("resized",b).put("result",result(a,b));
        }

        private JSONObject verify(PlacementExchange.Stage stage, Path nativeDir,
                                  SpatialPlacement.ProbeApproach shared) throws Exception {
            var review = stage.evidence().getJSONObject("review");
            var body = SpatialLevelPlacement.propByKey(stage.scene(),stage.propKey());
            var first = SpatialLevelPlacement.verifyNative(stage.scene(),review,nativeDir,body,shared);
            var repeat = SpatialLevelPlacement.verifyNative(stage.scene(),review,nativeDir,body,shared);
            if (!first.similar(repeat))
                throw new IllegalStateException("The two same-approach runs did not agree.");
            check(first,shared.json());
            return first;
        }

        private void check(JSONObject trials, JSONObject launch) {
            PlacementExchange.checkTrials(trials,true);
            for (String kind : new String[]{"hit","miss"}) {
                var trial = trials.getJSONObject(kind);
                var initial = trial.getJSONArray("samples").getJSONObject(0);
                var start = launch.getJSONArray(kind + "StartM");
                if (!start.similar(trial.getJSONArray("probeStartM")) || !start.similar(initial.getJSONArray("probeM"))
                        || !launch.getJSONArray("velocityMps").similar(initial.getJSONArray("probeVelocityMps"))
                        || Math.abs(launch.getDouble("probeSpeedMps")-trial.getDouble("probeIncomingMps")) > 1e-7
                        || Math.abs(launch.getDouble("radiusM")-trial.getDouble("probeRadiusM")) > 1e-7)
                    throw new IllegalStateException("The actual probe launch differs from the controlled approach.");
            }
            if (Math.abs(trials.getDouble("routeLengthM") - binding.getDouble("aisleM")) > 1e-9)
                throw new IllegalStateException("The protected walking aisle differs.");
        }

        boolean validCase(JSONObject receipt, JSONObject launch) {
            var a = receipt.getJSONObject("original");
            var b = receipt.getJSONObject("resized");
            check(a,launch);
            check(b,launch);
            return result(a,b).similar(receipt.getJSONObject("result"));
        }

        JSONObject read(Path path) {
            try {
                var receipt = new JSONObject(new String(SpriteWorkshop.read(path,PlacementExchange.BYTE_LIMIT),
                        StandardCharsets.UTF_8));
                SpatialScene.keys(receipt,"binding","original","resized","result","sha256");
                String hash = receipt.getString("sha256");
                receipt.remove("sha256");
                if (!WorldFrames.hash(receipt).equals(hash) || !binding.similar(receipt.getJSONObject("binding")))
                    return null;
                return validCase(receipt,binding.getJSONObject("approach")) ? receipt.put("sha256",hash) : null;
            } catch (IOException | RuntimeException changed) {
                return null;
            }
        }
    }

    private ColliderApproachComparison() {}

    static Plan plan(PlacementExchange.Bundle bundle, JSONObject replay) {
        if (!bundle.recipient()) return null;
        var stages = new ArrayList<>(bundle.previous());
        stages.add(bundle.stage());
        for (int i=stages.size()-1;i>0;i--) {
            var resized = stages.get(i);
            var review = resized.evidence().getJSONObject("review");
            if (review.has("collider"))
                return bind(bundle.id(),replay.getString("sha256"),i,stages.get(i-1),resized,review);
        }
        return null;
    }

    private static Plan bind(String archive, String replay, int index, PlacementExchange.Stage original,
                             PlacementExchange.Stage resized, JSONObject review) {
        String prop = resized.propKey();
        var box = review.getJSONObject("collider");
        var prior = new JSONObject(original.scene().getString("sourceText"));
        var next = new JSONObject(resized.scene().getString("sourceText"));
        var body = SpatialLevelPlacement.propByKey(prior,prop);
        body.put("size",box.getJSONArray("proposedM"));
        var originalBody = SpatialLevelPlacement.propByKey(original.scene(),prop);
        if (!prior.similar(next) || !originalBody.getJSONArray("size").similar(box.getJSONArray("previousM")))
            throw new IllegalArgumentException("The compared scenes must differ only by the reviewed collider size.");

        var approach = SpatialPlacement.approach(review,envelope(box.getJSONArray("previousM"),box.getJSONArray("proposedM")));
        var normalizedOriginal = new PlacementExchange.Stage(original.scene(),original.evidence(),prop,true);
        var binding = new JSONObject().put("format",FORMAT).put("archiveSha256",archive).put("replaySha256",replay)
                .put("beforeStage",index-1).put("afterStage",index)
                .put("beforeSceneId",original.scene().getString("id")).put("afterSceneId",resized.scene().getString("id"))
                .put("meshSha256",box.getString("meshSha256")).put("beforeColliderM",box.getJSONArray("previousM"))
                .put("afterColliderM",box.getJSONArray("proposedM")).put("targetM",review.getJSONArray("targetM"))
                .put("yawDeg",review.getDouble("yawDeg")).put("aisleM",resized.evidence().getJSONObject("measured").getDouble("routeLengthM"))
                .put("approach",approach.json());
        return new Plan(binding,normalizedOriginal,resized,approach);
    }

    static double[] envelope(org.json.JSONArray before, org.json.JSONArray after) {
        var a = ObserverGeometry.v(before);
        var b = ObserverGeometry.v(after);
        return new double[]{Math.max(a[0],b[0]),a[1],Math.max(a[2],b[2])};
    }

    private static JSONObject result(JSONObject original, JSONObject resized) {
        var a = original.getJSONObject("hit");
        var b = resized.getJSONObject("hit");
        int delta = a.getInt("firstContactTick")-b.getInt("firstContactTick");
        return new JSONObject().put("resizedEarlierByTicks",delta)
                .put("resizedEarlierByMs",delta*BulletRamp.DT*1000)
                .put("contactDurationDeltaTicks",b.getInt("contactTicks")-a.getInt("contactTicks"))
                .put("peakSpeedDeltaMps",b.getDouble("maxPropPlanarSpeedMps")-a.getDouble("maxPropPlanarSpeedMps"));
    }
}
