package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class PushNativeTest {
    static JSONObject scene(String variant){return SpatialScene.parse(PushScenes.example(variant),UUID.randomUUID().toString());}
    static String id(JSONObject scene,String source){for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("sourceId").equals(source))return o.getString("id");}throw new AssertionError(source);}
    static PushPlan planner(JSONObject s){return new PushPlan(s,id(s,"crate"),id(s,"pusher"));}
    static JSONObject plan(JSONObject scene,double initial,double max){var p=planner(scene);return p.plan(p.intent(new JSONArray(new int[]{3,0,0}),initial,max));}
    @Test public void configurationSearchFindsRealStandingRoutesAroundObstacle(){var s=scene("corner");var p=plan(s,8,32);assertTrue(p.toString(),p.getBoolean("passed"));assertTrue(p.getJSONArray("pushes").length()>3);planner(s).verify(p);var edited=new JSONObject(p.toString());edited.getJSONArray("pushes").getJSONObject(0).getJSONArray("walk").put(0,0);assertThrows(IllegalStateException.class,()->planner(s).verify(edited));}
    @Test public void freeCratePathDoesNotImplyReachablePusherOrSolvablePush(){var s=scene("unreachable-standing");var p=planner(s);var cp=PushPlan.position(p.crate);int from=p.cell((int)cp[0],(int)cp[2]);assertTrue(p.clear(from,p.cell(0,0),p.crateRadius,-1));var result=plan(s,8,32);assertFalse(result.getBoolean("passed"));assertTrue(result.getString("reason").contains("standing"));assertFalse(plan(scene("blocked"),8,32).getBoolean("passed"));}
    @Test public void actualContactControllerMovesCrateAndRecoversMeasuredHighFrictionStall() throws Exception {
        for(String variant:List.of("open","corner","high-friction")){
            var s=scene(variant);var p=plan(s,8,32);assertTrue(p.toString(),p.getBoolean("passed"));var events=new JSONArray();
            try(var world=new PushPhysics(s,p,Files.createTempDirectory("push031-native-"),null,events)){
                while(!world.settled())world.step();var end=world.sample();var crate=PushPhysics.dynamic(end.getJSONArray("bodies"),id(s,"crate"));var pos=SpatialScene.vector(crate.getJSONArray("position"),30);double error=Math.hypot(pos[0]-2,pos[2]);
                System.out.println("PUSH_NATIVE_031 "+new JSONObject().put("variant",variant).put("tick",world.tick).put("errorM",error).put("controller",world.controller).put("events",events));
                assertEquals(variant+world.controller,"finished",world.controller.getString("phase"));assertTrue(variant+" error "+error,error<=.1);assertTrue(ObserverGeometry.norm(SpatialScene.vector(crate.getJSONArray("velocity"),100))<=.2);
                assertTrue(events.toList().stream().anyMatch(e->((Map<?,?>)e).get("kind").equals("action-complete")));
                if(variant.equals("high-friction")){assertTrue(world.controller.getInt("recoveries")>0);assertTrue(events.toList().stream().anyMatch(e->((Map<?,?>)e).get("kind").equals("jam")));}
            }
        }
    }
}
