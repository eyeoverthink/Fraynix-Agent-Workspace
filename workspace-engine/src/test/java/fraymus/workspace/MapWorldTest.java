package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class MapWorldTest {
    @Test public void actualPngDecodeLearnsRulesAndRejectsUnlabelledOrEditedSource() throws Exception {
        var source=MapDefinition.image(MapDefinition.example());assertEquals(16,source.getInt("width"));assertEquals(3,source.getJSONArray("palette").length());MapDefinition.rules(source);source.put("imageSha256","0".repeat(64));assertThrows(IllegalStateException.class,()->MapDefinition.rules(source));var bad=MapDefinition.example().put("walkableColors",new JSONArray(List.of("#123456")));assertThrows(IllegalArgumentException.class,()->MapDefinition.image(bad));
    }
    @Test public void nativeForceControllerTraversesActualCompiledCollidersDespiteImpulse() throws Exception {
        Path root=Files.createTempDirectory("map-native028-");try(var store=new MapStore(root)){var ref=store.importImage(MapDefinition.example());var map=store.build(ref.getString("id"),new JSONObject().put("width",12).put("height",12).put("seed",2801));assertTrue(map.getJSONObject("definition").getJSONObject("generation").getBoolean("passed"));var run=store.start(UUID.randomUUID().toString(),map.getString("id"),true,.35,false);for(int i=0;i<1200;i++){run=store.run(run.getString("id"));if(!run.getString("status").equals("running"))break;Thread.sleep(10);}assertEquals(run.optString("reason"),"completed",run.getString("status"));System.out.println("MAP_METRICS "+run.getJSONObject("verification"));assertTrue(run.getJSONObject("verification").toString(),run.getJSONObject("verification").getBoolean("passed"));assertTrue(run.getBoolean("impulseApplied"));assertEquals(0,run.getJSONObject("verification").getInt("blockedSamples"));}
    }
    @Test public void packageImportReconstructsOriginalImageAndAllCompiledBodies() throws Exception {
        Path root=Files.createTempDirectory("map-package028-");try(var store=new MapStore(root)){var ref=store.importImage(MapDefinition.example());var map=store.build(ref.getString("id"),new JSONObject().put("seed",2802));var text=store.exportMap(map.getString("id"));var imported=store.importMap(text);assertTrue(map.getJSONObject("definition").similar(imported.getJSONObject("definition")));var altered=new JSONObject(text);altered.getJSONObject("map").getJSONObject("definition").getJSONArray("cells").put(0,1);assertThrows(IllegalStateException.class,()->store.importMap(altered.toString()));}
    }
    @Test public void manualForceReallyCollidesWithNativeWallAndBrakesAfterRelease() throws Exception {
        var source=MapDefinition.image(MapDefinition.example());var map=MapDefinition.generated(source,new JSONObject().put("width",12).put("height",12).put("seed",2801));try(var world=new MapPhysics(map,Files.createTempDirectory("map-contact028-"))){for(int i=0;i<480;i++)world.step(false,-1,0);var collision=world.sample();assertTrue(collision.toString(),collision.getInt("wallContacts")>0);double boundary=-map.getInt("width")/2.0+1+.18;assertTrue(collision.getJSONArray("position").getDouble(0)>=boundary-.02);for(int i=0;i<240;i++)world.step(false,0,0);assertTrue(ObserverGeometry.norm(SpatialScene.vector(world.sample().getJSONArray("velocity"),30))<.1);assertEquals(1,world.sample().getInt("positionAssignments"));}
    }
    @Test public void originalFourDimensionalDungeonIsPreservedWithAnExplicitPlayableLayer() throws Exception {
        var def=Procedural.definition(new JSONObject().put("type","dungeon").put("dimensions",4).put("seed",2811));var data=Procedural.compute(def);var goal=new JSONObject().put("status","completed").put("title","Original4D").put("id",UUID.randomUUID().toString()).put("proceduralResult",data).put("proceduralDefinition",def).put("proceduralSha256",MeasurementStore.hash(data.toString())).put("proceduralCodeSha256",Procedural.codeIdentity());var map=MapDefinition.layer(goal,null);assertTrue(data.similar(map.getJSONObject("source").getJSONObject("original")));assertEquals(2,map.getJSONObject("source").getJSONArray("layers").length());assertTrue(map.getJSONArray("path").length()>2);assertTrue(map.similar(MapDefinition.layer(goal,map.getJSONObject("source").getJSONArray("layers")) .put("source",new JSONObject(map.getJSONObject("source").toString()))));
    }
    @Test public void controllerHandlesAnotherImageAndDeclaredScaleFrictionBounds() throws Exception {
        try(var store=new MapStore(Files.createTempDirectory("map-bounds028-"))){var source=store.importImage(MapDefinition.example(1));for(var options:List.of(new JSONObject().put("seed",2815).put("width",8).put("height",8).put("cellSizeM",.6).put("friction",1.2),new JSONObject().put("seed",2816).put("width",16).put("height",16).put("cellSizeM",1.2).put("friction",.05))){var map=store.build(source.getString("id"),options);var r=store.start(UUID.randomUUID().toString(),map.getString("id"),true,.5,false);for(int n=0;n<1600;n++){r=store.run(r.getString("id"));if(!r.getString("status").equals("running"))break;Thread.sleep(5);}assertEquals(r.optString("reason"),"completed",r.getString("status"));assertTrue(r.getJSONObject("verification").toString(),r.getJSONObject("verification").getBoolean("passed"));}}
    }
}
