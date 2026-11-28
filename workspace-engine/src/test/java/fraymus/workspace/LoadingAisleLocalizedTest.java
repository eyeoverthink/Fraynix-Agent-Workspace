package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class LoadingAisleLocalizedTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    private record Bay(String variant, String move, String outcome) {}

    @Test public void measuredLocalizedAndFullGridComparisonKeepsVerifiedOutcomes() throws Exception {
        var nativeDir = temporary.newFolder("native").toPath();
        int beforeTotal = 0, afterTotal = 0;
        for (var bay : List.of(new Bay("wide-fine","120 cm","local"),
                new Bay("wide-mirror","173 cm","local"), new Bay("wide-coarse","120 cm","coarse"),
                new Bay("wide-blocked","120 cm","refused"), new Bay("wide-remote","200 cm","full"))) {
            var source = LoadingAisleWideBays.fixture(bay.variant);
            var parent = SpatialScene.parse(source.toString(),UUID.randomUUID().toString());
            var request = LoadingAisleIntent.parse("Move the blue crate " + bay.move + " away from the loading door.");
            var object = LoadingAisle.find(parent,"cargo").getString("id");
            var intent = new JSONObject().put("kind","request").put("priorSelection","");
            var direct = LoadingAisleIntent.base(parent,object,request,intent);
            var blocked = assertThrows(LoadingAisle.Obstruction.class,() -> verify(parent,direct,nativeDir));
            var coarse = LoadingAisleRoute.search(parent,direct,blocked.evidence(),nativeDir,false);
            var fine = coarse.plan()==null ? LoadingAisleRoute.search(parent,direct,blocked.evidence(),nativeDir,true) : null;
            var baseline = fine==null ? coarse.plan() : fine.plan();
            var result = LoadingAisleIntent.search(parent,object,request,intent,nativeDir);
            assertEquals(bay.variant,baseline==null,result.plan()==null);

            int fullGridEdges = fine==null ? 0 : fine.edgesChecked();
            int before = coarse.edgesChecked()+fullGridEdges;
            var report = new JSONObject().put("bay",bay.variant).put("outcome",bay.outcome)
                    .put("baselineCoarse",coarse.edgesChecked()).put("baselineFullFine",fullGridEdges)
                    .put("baselineTotal",before);
            int after;
            if (baseline==null) {
                assertEquals("refused",bay.outcome);
                var refusal = result.refusal();
                var local = refusal.getJSONObject("localized");
                int reused = refusal.getInt("reusedFineEdges");
                int fineEdges = refusal.getInt("fineRouteEdgesChecked");
                assertTrue(reused>0);
                assertEquals(fullGridEdges,fineEdges+reused);
                after = refusal.getInt("routeEdgesChecked")+local.getInt("edgesChecked")+fineEdges;
                report.put("local",local).put("reused",reused).put("newFine",fineEdges);
            } else {
                var earlier = verify(parent,baseline,nativeDir);
                var accepted = result.plan();
                assertTrue(baseline.getJSONArray("toM").similar(accepted.getJSONArray("toM")));
                var route = accepted.getJSONObject("route");
                assertEquals(earlier.getJSONObject("route").getDouble("distanceM"),route.getDouble("distanceM"),1e-9);
                assertEquals(earlier.getInt("shelvesPreserved"),accepted.getJSONObject("preview").getInt("shelvesPreserved"));
                assertEquals(earlier.getDouble("widthM"),accepted.getJSONObject("preview").getDouble("widthM"),0);
                var stats = route.getJSONObject("search");
                int fineEdges = stats.getInt("fineEdgesChecked");
                var local = stats.optJSONObject("localized");
                int localEdges = local==null ? 0 : local.getInt("edgesChecked");
                after = stats.getInt("coarseEdgesChecked") + fineEdges;
                if (bay.outcome.equals("local")) {
                    assertEquals("accepted",local.getString("result"));
                    assertTrue(after<before*.65);
                } else if (bay.outcome.equals("full")) {
                    assertEquals("budget",local.getString("result"));
                    int reused = stats.getInt("reusedFineEdges");
                    assertTrue(reused>0);
                    assertEquals(fullGridEdges,fineEdges+reused);
                    after += localEdges;
                } else {
                    assertNull(local);
                    assertEquals(0,fineEdges);
                }
                report.put("search",stats).put("distanceM",route.getDouble("distanceM"))
                        .put("baselineLegs",earlier.getJSONObject("route").getInt("segmentCount"))
                        .put("acceptedLegs",accepted.getJSONObject("preview").getJSONObject("route").getInt("segmentCount"));
            }
            report.put("adaptiveTotal",after);
            beforeTotal += before;
            afterTotal += after;
            System.out.println("LOCAL_AB " + report);
        }
        assertTrue(afterTotal<beforeTotal*.80);
    }

    private static JSONObject verify(JSONObject parent, JSONObject plan, Path nativeDir) throws Exception {
        var revision = MapCheckpoint.canonical(LoadingAisle.revision(parent,plan));
        var child = SpatialScene.parse(revision,parent.getString("id"));
        return LoadingAisle.measure(parent,child,plan,nativeDir);
    }
}
