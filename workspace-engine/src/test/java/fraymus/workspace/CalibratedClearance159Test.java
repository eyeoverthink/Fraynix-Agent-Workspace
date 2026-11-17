package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class CalibratedClearance159Test {
 @Test public void nominallyClearRouteIsRefusedWhenCoordinateRegionReachesObstacle()throws Exception{
  try(var seed=new Alignment041Test.Fixture()){
   var source=new CalibratedPlane159(seed.root,seed.store,seed.ledger).source(seed.reg.getString("id"));
   var raw=new JSONObject(Calibrated159Test.scene().getString("sourceText"));raw.getJSONArray("objects").put(new JSONObject().put("sourceId","guard").put("label","Independent narrow-clearance obstacle").put("shape","box").put("position",new JSONArray("[0.335,0.1,-0.075]")).put("size",new JSONArray("[0.1,0.2,0.1]")).put("massKg",0).put("friction",.5).put("restitution",0).put("rotation",new JSONArray("[0,0,0,1]")));
   var scene=WorldFrames.scene(WorldFrames.initial(SpatialScene.parse(raw.toString(),UUID.randomUUID().toString())));
   var targets=new JSONArray().put(new JSONObject().put("sourceId","A").put("positionM",new JSONArray("[0.075,0.1,-0.075]")).put("yawDeg",0)).put(new JSONObject().put("sourceId","B").put("positionM",new JSONArray("[0.7,0.1,0.2]")).put("yawDeg",0));
   assertTrue(WorldPlan.build(scene,WorldPlan.intent(scene,targets,6,.5)).getBoolean("passed"));
   var d=CalibratedAction159.definition(Calibrated159Test.input(seed.reg.getString("id"),seed.observed));
   var failure=assertThrows(IllegalArgumentException.class,()->CalibratedAction159.decision(source,scene,d));assertTrue(failure.getMessage(),failure.getMessage().contains("Uncertainty-expanded"));
  }
 }
}
