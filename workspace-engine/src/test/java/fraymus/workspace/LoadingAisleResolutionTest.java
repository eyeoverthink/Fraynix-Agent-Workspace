package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.UUID;

import static org.junit.Assert.*;

public class LoadingAisleResolutionTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @Test public void parsesExactWholeCentimetresAndPreservesLegacyRequests() {
        assertEquals(40,LoadingAisleIntent.parse("Move the blue crate forty centimetres away from the loading door.").distanceCm());
        assertEquals(120,LoadingAisleIntent.parse("Move the blue crate 1200 mm away from the loading door.").distanceCm());
        assertEquals(173,LoadingAisleIntent.parse("Move the blue crate 1.73 m away from the loading door.").distanceCm());
        for (String bad : new String[]{"1.736 m","201 cm","39 cm","1.73 m from"})
            assertThrows(IllegalArgumentException.class,() -> LoadingAisleIntent.parse(
                    "Move the blue crate " + bad + " away from the loading door."));
    }

    @Test public void coarseMissAndFineSuccessUseTheSameSourceAndDestination() throws Exception {
        var source = LoadingAisleWideBays.fixture("wide-mirror");
        var parent = SpatialScene.parse(source.toString(),UUID.randomUUID().toString());
        var request = LoadingAisleIntent.parse("Move the blue crate 1.73 m away from the loading door.");
        var intent = new JSONObject().put("kind","request").put("priorSelection","");
        var base = LoadingAisleIntent.base(parent,LoadingAisle.find(parent,"cargo").getString("id"),request,intent);
        var direct = new JSONObject().put("gate","path").put("reason","The crate path is blocked by Inner barrier, Outer barrier.");
        var nativeDir = temporary.newFolder("native").toPath();
        var coarse = LoadingAisleRoute.search(parent,base,direct,nativeDir,false);
        assertNull(coarse.plan());
        assertTrue(coarse.edgesChecked()>0);
        var fine = LoadingAisleRoute.search(parent,base,direct,nativeDir,true);
        assertNotNull(fine.plan());
        var plan = fine.plan();
        LoadingAisleRoute.recordSearch(plan,coarse.edgesChecked(),fine.edgesChecked(),"no-route");
        var route = plan.getJSONObject("route");
        assertTrue(plan.similar(LoadingAisleRoute.rebuild(base,route)));
        var cells = route.getJSONArray("cells");
        assertEquals(35,cells.getJSONArray(cells.length()-1).getInt(0));
        assertEquals(173,plan.getInt("forwardCm"));
        assertTrue(route.getJSONArray("pointsM").getJSONArray(cells.length()-1).similar(base.getJSONArray("toM")));
        try (var world = new SpatialPhysics(parent,nativeDir)) {
            var proof = LoadingAisleRoute.verify(world,plan);
            assertEquals(1.73,ObserverGeometry.norm(ObserverGeometry.sub(
                    ObserverGeometry.v(plan.getJSONArray("toM")),ObserverGeometry.v(plan.getJSONArray("fromM")))),1e-8);
            assertEquals(cells.length()-1,proof.getJSONArray("segments").length());
            assertEquals(5,proof.getJSONObject("search").getInt("forwardStepCm"));
        }
    }
}
