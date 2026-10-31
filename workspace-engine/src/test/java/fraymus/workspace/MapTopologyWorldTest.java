package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class MapTopologyWorldTest {
    private static JSONObject options(){return new JSONObject().put("width",12).put("height",12).put("seed",2801).put("constraints",new JSONObject().put("connected",true).put("minFloorFraction",.3).put("maxFloorFraction",.75).put("minRouteSteps",18).put("maxRouteSteps",64));}
    @Test public void acceptedGlobalGridReallyTraversesAndRoundTripsAfterRestart() throws Exception {
        Path root=Files.createTempDirectory("topology-native029-");String id;JSONObject map;
        try(var store=new MapStore(root)){var ref=store.importImage(MapDefinition.example());map=store.build(ref.getString("id"),options());id=map.getString("id");assertTrue(MapTopology.verify(map.getJSONObject("definition")).getBoolean("passed"));var run=store.start(UUID.randomUUID().toString(),id,true,-.3,false);
            for(int i=0;i<1200;i++){run=store.run(run.getString("id"));if(!run.getString("status").equals("running"))break;Thread.sleep(10);}assertTrue(run.toString(),run.getJSONObject("verification").getBoolean("passed"));assertTrue(run.getJSONObject("verification").getJSONObject("topology").getBoolean("passed"));assertEquals(0,run.getJSONObject("verification").getInt("blockedSamples"));
            var imported=store.importMap(store.exportMap(id));assertTrue(map.getJSONObject("definition").similar(imported.getJSONObject("definition")));}
        try(var restored=new MapStore(root)){assertTrue(map.similar(restored.read(id)));}
    }
    @Test public void forgedTopologyClaimsWithARecomputedOuterHashDoNotPass() throws Exception {
        var m=MapDefinition.generated(MapDefinition.image(MapDefinition.example()),options());m.getJSONObject("topology").getJSONObject("audit").getJSONObject("metrics").put("components",99);assertThrows(IllegalStateException.class,()->MapTopology.verify(m));
        var forged=new JSONObject().put("id","forged").put("title","Rehashed forged global claims").put("definition",m).put("sha256",MeasurementStore.hash(m.toString()));try(var store=new MapStore(Files.createTempDirectory("topology-forged029-"))){assertThrows(IllegalStateException.class,()->store.importMap(new JSONObject().put("format","fraynix.playable-map").put("version",1).put("map",forged).toString()));}
        var bad=options();bad.getJSONObject("constraints").put("connected","true");assertThrows(IllegalArgumentException.class,()->MapDefinition.generated(MapDefinition.image(MapDefinition.example()),bad));
    }
    @Test public void infeasibleGlobalRequirementsNeverCompileAnInventedWorld() throws Exception {
        var o=options();o.getJSONObject("constraints").put("minRouteSteps",0).put("maxRouteSteps",3);var map=MapDefinition.generated(MapDefinition.image(MapDefinition.example()),o);assertFalse(map.getJSONObject("generation").getBoolean("passed"));assertFalse(map.has("objects"));assertFalse(MapTopology.verify(map).getBoolean("passed"));assertEquals(1,map.getJSONObject("topology").getInt("candidateCount"));
    }
    @Test public void original028PublishedPackageStillReconstructsExactly() throws Exception {
        Path base=Path.of("").toAbsolutePath();while(base!=null&&!Files.exists(base.resolve("docs/readmes/028/examples/original/map.json")))base=base.getParent();assertNotNull(base);var original=new JSONObject(Files.readString(base.resolve("docs/readmes/028/examples/original/map.json")));assertFalse(original.getJSONObject("definition").has("topology"));
        try(var store=new MapStore(Files.createTempDirectory("legacy028-import-"))){var actual=store.importMap(new JSONObject().put("format","fraynix.playable-map").put("version",1).put("map",original).toString());assertTrue(original.getJSONObject("definition").similar(actual.getJSONObject("definition")));}
    }
}
