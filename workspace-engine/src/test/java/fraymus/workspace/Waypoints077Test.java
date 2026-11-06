package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class Waypoints077Test {
    private JSONObject training(int i){return Procedure077.protocol().getJSONArray("training").getJSONObject(i);}
    @Test public void occupiedEqualWaypointsAreReplacedWithoutMutatingTheSource(){
        var s=training(1);var frame=WorldFrames.frame(Procedure077.scene(s));var original=frame.toString();
        assertFalse(Procedure077.plan(s,"three-stage",0,frame).getBoolean("passed"));
        var choice=Waypoints077.choose(s,"clear-three",0,frame);
        assertTrue(choice.getJSONObject("plan").getBoolean("passed"));assertTrue(choice.getDouble("deviationSquaredM2")>0);
        assertTrue(choice.getInt("candidateChecks")<=512);assertEquals(original,frame.toString());
        assertTrue(choice.similar(Waypoints077.choose(s,"clear-three",0,frame)));
        WorldPlan.verify(WorldFrames.scene(frame),choice.getJSONObject("plan"));
    }
    @Test public void finalGoalsAndAuthorityNeverChange(){
        var s=training(0);var f=WorldFrames.frame(Procedure077.scene(s));
        for(String family:Procedure077.FAMILIES){var p=Procedure077.plan(s,family,Procedure077.stages(family)-1,f);assertTrue(s.getJSONArray("targets").similar(p.getJSONObject("intent").getJSONArray("targets")));assertEquals(s.getDouble("forceLimitN"),p.getJSONObject("intent").getDouble("forceLimitN"),0);}
    }
    @Test public void geometryPreviewDoesNotBecomeMeasuredState(){
        var s=training(1);var before=s.toString();String family=Waypoints077.geometric(s);
        assertTrue(Procedure077.FAMILIES.contains(family));assertEquals(before,s.toString());
        var f=WorldFrames.frame(Procedure077.scene(s));assertEquals(0,f.getLong("elapsedTicks"));
        for(Object o:f.getJSONArray("bodies"))assertEquals(0,WorldVerify.norm(((JSONObject)o).getJSONArray("velocity")),0);
    }
    @Test public void searchRefusesAnOccupiedGoalRatherThanMovingIt(){
        var s=training(0);s.getJSONArray("targets").getJSONObject(0).put("positionM",new JSONArray(new double[]{0,.21,0}));
        var p=Procedure077.plan(s,"clear-two",1,WorldFrames.frame(Procedure077.scene(s)));assertFalse(p.getBoolean("passed"));
        assertEquals(0,p.getJSONObject("intent").getJSONArray("targets").getJSONObject(0).getJSONArray("positionM").getDouble(0),0);
    }
    @Test public void changedSourceAndInvalidStageRefuse(){
        var a=training(0);var b=training(1);assertThrows(IllegalStateException.class,()->Waypoints077.choose(a,"clear-two",0,WorldFrames.frame(Procedure077.scene(b))));
        assertThrows(IllegalArgumentException.class,()->Waypoints077.ideal(a,"clear-three",3));
        assertThrows(IllegalArgumentException.class,()->Procedure077.stages("teleport"));
    }
    @Test public void boundsAndObstacleFeatureAreExplicit(){
        assertFalse(Procedure077.admitted(30000,6,0));assertTrue(Procedure077.admitted(0,6,0));
        assertFalse(Procedure077.admitted(0,6,1200000));assertTrue(Waypoints077.blockedFraction(training(1))>0);
        var s=training(0);var target=WorldFrames.copy(s.getJSONArray("targets").getJSONObject(0));target.put("positionM",new JSONArray(new double[]{5.9,.21,0}));assertThrows(IllegalArgumentException.class,()->Waypoints077.landing(Procedure077.scene(s),target));
    }
    @Test public void frozenCasesAndControlsRemainComplete(){
        var p=Procedure077.protocol();assertEquals(3,p.getJSONArray("training").length());assertEquals(12,p.getJSONArray("cases").length());
        for(Object raw:p.getJSONArray("cases")){var s=(JSONObject)raw;if(s.getString("split").equals("unsupported"))assertThrows(IllegalArgumentException.class,()->Procedure077.supported(s));else Procedure077.supported(s);}
        assertEquals(List.of("direct","two-stage","three-stage"),Procedure077.order("legacy","direct"));
        assertEquals(5,Procedure077.order("simple","direct").size());assertEquals(5,Procedure077.order("geometry","clear-three").size());
    }
}
