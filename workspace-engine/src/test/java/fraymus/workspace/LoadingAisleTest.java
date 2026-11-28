package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class LoadingAisleTest {
    private static final String REQUEST =
            "Move the selected crate 40 cm away from the loading door. Keep the 1 m aisle clear and leave the shelves alone.";

    private static JSONObject parse(JSONObject source) {
        return SpatialScene.parse(source.toString(), UUID.randomUUID().toString());
    }

    @Test public void loadedDoorControlsDirectionAndOnlyTheExpectedCrateMoves() {
        var scene = parse(LoadingAisle.fixture(37,"none"));
        var selected = LoadingAisle.find(scene,"cargo");
        var plan = LoadingAisle.plan(scene,selected.getString("id"),REQUEST);
        var delta = plan.getJSONArray("deltaM");
        assertEquals(.4*Math.sin(Math.toRadians(37)),delta.getDouble(0),1e-9);
        assertEquals(.4*Math.cos(Math.toRadians(37)),delta.getDouble(2),1e-9);
        assertEquals(2,plan.getJSONArray("protected").length());
        var draft = LoadingAisle.revision(scene,plan);
        LoadingAisle.checkScope(scene,parse(draft),plan);
        // The edited crate remains correct even while one unapproved shelf field is altered.
        LoadingAisle.find(draft,"shelf-east").put("friction",.9);
        var error = assertThrows(IllegalStateException.class,() -> LoadingAisle.checkScope(scene,parse(draft),plan));
        assertTrue(error.getMessage().contains("shelf"));
    }

    @Test public void additionalEditsAndAmbiguousDirectionAreRefused() {
        var scene = parse(LoadingAisle.fixture(0,"none"));
        String selected = LoadingAisle.find(scene,"cargo").getString("id");
        assertThrows(IllegalArgumentException.class,() -> LoadingAisle.plan(scene,selected,REQUEST+" Delete the east shelf."));
        assertThrows(IllegalArgumentException.class,() -> LoadingAisle.plan(scene,selected,
                "Move the selected crate 40 cm from the loading door."));
    }
}
