package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SpatialCarryTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();

    @Test public void identicalEndpointsNeedDifferentMotionDecisions() throws Exception {
        Path project=folder.getRoot().toPath();
        var pending=new ArrayList<JSONObject>();
        try (var store=new SpatialStore(project)) {
            for (String label:List.of("a","b","c")) {
                var open=store.importGlb(fixture(label,"turn"));
                var blocked=store.importGlb(fixture(label,"blocked"));
                assertTrue(key(open).getJSONArray("position").similar(key(blocked).getJSONArray("position")));
                assertTrue(key(open).getJSONArray("rotation").similar(key(blocked).getJSONArray("rotation")));
                var good=store.reviewCarry(request(open));
                var bad=store.reviewCarry(request(blocked));
                var openTrials=good.getJSONObject("proposal").getJSONObject("forecast").getJSONArray("trials");
                assertFalse(openTrials.getJSONObject(0).getBoolean("passed"));
                assertEquals("carry",openTrials.getJSONObject(0).getJSONObject("firstUnexpectedContact").getString("phase"));
                assertEquals(summary(openTrials),"review",good.getString("status"));
                assertEquals("refused",bad.getString("status"));
                var blockedTrials=bad.getJSONObject("proposal").getJSONObject("forecast").getJSONArray("trials");
                for (Object raw:blockedTrials) {
                    var trial=(JSONObject)raw;
                    assertFalse(trial.getBoolean("passed"));
                    assertTrue(trial.getJSONObject("firstUnexpectedContact").getString("obstacle").endsWith("-jamb"));
                }
                assertThrows(IllegalStateException.class,()->store.approveCarry(approval(blocked,bad)));
                assertThrows(IllegalStateException.class,()->store.approveCarry(approval(open,good).put("approvalSha256","changed")));
                pending.add(approval(open,good));
            }
            assertEquals(6,store.list().length());
        }
        try (var reopened=new SpatialStore(project)) {
            for (var approval:pending) {
                var lookup=new JSONObject().put("sceneId",approval.getString("sceneId")).put("id",approval.getString("id"));
                assertEquals("review",reopened.readCarry(lookup).getString("status"));
                var result=reopened.approveCarry(approval);
                assertEquals("completed",result.getString("status"));
                var actual=result.getJSONObject("verified");
                assertTrue(actual.getBoolean("passed"));
                assertTrue(actual.getBoolean("support"));
                assertEquals(240,actual.getInt("settleTicks"));
                assertTrue(actual.getDouble("positionErrorM")<=.02);
                assertTrue(actual.getDouble("angleErrorDeg")<=2);
                assertTrue(actual.getDouble("minCommandedVisualEnvelopeM")>=.008);
                assertTrue(actual.getDouble("minColliderM")>=.008);
                assertTrue(result.similar(reopened.approveCarry(approval)));
            }
            assertEquals(6,reopened.list().length());
            var approval=pending.getFirst();
            Path original=project.resolve("spatial/glb-sources").resolve(approval.getString("sceneId")+".glb");
            Files.write(original,fixture("b","turn"));
            assertThrows(IllegalStateException.class,()->reopened.approveCarry(approval));
        }
    }

    @Test public void savedReviewCanBeDiscoveredBeforeAnyClientAcknowledgement() throws Exception {
        Path project=folder.getRoot().toPath();
        JSONObject review;
        try (var store=new SpatialStore(project)) {
            assertTrue(store.latestCarry().isEmpty());
            review=store.reviewCarry(request(store.importGlb(fixture("a","turn"))));
        }
        try (var reopened=new SpatialStore(project)) {
            assertTrue(review.similar(reopened.latestCarry()));
            var id=review.getString("id");
            Path receipt=project.resolve("spatial/carry-reviews").resolve(id+".json");
            var envelope=new JSONObject(Files.readString(receipt));
            envelope.getJSONObject("record").put("status","completed");
            Files.writeString(receipt,envelope.toString());
            assertThrows(IllegalStateException.class,reopened::latestCarry);
        }
    }

    @Test public void colliderOnlyRouteCannotApproveAVisualClash() throws Exception {
        try (var store=new SpatialStore(folder.getRoot().toPath())) {
            var scene=store.importGlb(fixture("a","visual"));
            var record=store.reviewCarry(request(scene));
            assertEquals("refused",record.getString("status"));
            var candidates=record.getJSONObject("proposal").getJSONObject("forecast").getJSONArray("trials");
            var turn=candidates.getJSONObject(1);
            assertTrue(turn.isNull("firstUnexpectedContact"));
            assertTrue(turn.getDouble("minColliderM")>.008);
            assertTrue(turn.getDouble("minVisualEnvelopeM")<0);
            assertEquals("visual",turn.getJSONObject("firstClearanceWarning").getString("envelope"));
            assertThrows(IllegalStateException.class,()->store.approveCarry(approval(scene,record)));
        }
    }

    private static byte[] fixture(String label,String suffix) throws Exception {
        try (var source=SpatialCarryTest.class.getResourceAsStream("/spatial/doorways/gate-"+label+"-"+suffix+".glb")) {
            assertNotNull(source);
            return source.readAllBytes();
        }
    }

    private static JSONObject key(JSONObject scene) {
        for (Object raw:scene.getJSONArray("objects")) {
            var o=(JSONObject)raw;
            if (o.getString("sourceId").equals("key")) return o;
        }
        throw new AssertionError("Missing key fixture");
    }

    private static JSONObject request(JSONObject scene) {
        return new JSONObject().put("sceneId",scene.getString("id")).put("objectId",key(scene).getString("id"))
                .put("displacementM",new JSONArray(new double[]{3.3,0,0}));
    }

    private static JSONObject approval(JSONObject scene,JSONObject review) {
        return new JSONObject().put("sceneId",scene.getString("id")).put("id",review.getString("id"))
                .put("approvalSha256",review.getString("approvalSha256"));
    }

    private static String summary(JSONArray trials) {
        var summary=new ArrayList<JSONObject>();
        for (Object raw:trials) {
            var trial=WorldFrames.copy((JSONObject)raw);
            trial.remove("samples");
            summary.add(trial);
        }
        return summary.toString();
    }
}
