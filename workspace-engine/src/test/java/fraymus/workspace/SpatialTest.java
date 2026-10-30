package fraymus.workspace;

import org.json.*;
import org.junit.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class SpatialTest {
    static String source() throws Exception {try(var in=SpatialTest.class.getResourceAsStream("/spatial/declared-test-scene-v1.json")){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
    static String object(JSONObject s){return s.getJSONArray("objects").getJSONObject(2).getString("id");}
    @Test public void normalizeDeclaredScaleAndPersistIdentity() throws Exception {
        var dir=Files.createTempDirectory("spatial-identities");JSONObject s;try(var store=new SpatialStore(dir)){s=store.importScene(source());assertEquals(-2,s.getJSONArray("objects").getJSONObject(2).getJSONArray("position").getDouble(0),0);assertFalse(s.getJSONObject("scaleCheck").getBoolean("independentlyCalibrated"));}
        try(var store=new SpatialStore(dir)){assertEquals(object(s),object(store.read(s.getString("id"))));assertEquals(source(),store.read(s.getString("id")).getString("sourceText"));}
        var mm=new JSONObject(source()).put("units","mm");mm.getJSONObject("scaleReference").put("b",new JSONArray(new int[]{1000,0,0}));for(Object raw:mm.getJSONArray("objects"))for(String k:List.of("size","position")){var a=((JSONObject)raw).getJSONArray(k);for(int i=0;i<3;i++)a.put(i,a.getDouble(i)*10);}assertTrue(s.getJSONArray("objects").getJSONObject(2).getJSONArray("position").similar(SpatialScene.parse(mm.toString(),s.getString("id")).getJSONArray("objects").getJSONObject(2).getJSONArray("position")));
    }
    @Test public void rejectMismatchedScaleAndExecutableMeshFields() throws Exception {
        var in=new JSONObject(source());in.getJSONObject("scaleReference").put("knownLengthM",2);assertThrows(IllegalArgumentException.class,()->SpatialScene.parse(in.toString(),UUID.randomUUID().toString()));
        for(String field:List.of("script","mesh","code")){var x=new JSONObject(source()).put(field,"do something");assertThrows(IllegalArgumentException.class,()->SpatialScene.parse(x.toString(),UUID.randomUUID().toString()));}
        var x=new JSONObject(source());x.getJSONArray("objects").getJSONObject(2).put("shape","mesh");assertThrows(IllegalArgumentException.class,()->SpatialScene.parse(x.toString(),UUID.randomUUID().toString()));
    }
    @Test public void rejectInvalidBodiesAndImplicitUnits() throws Exception {
        for(String change:List.of("units","quaternion","duplicate","numeric","dynamic")){var x=new JSONObject(source());switch(change){case "units"->x.remove("units");case "quaternion"->x.getJSONArray("objects").getJSONObject(2).put("rotation",new JSONArray(new int[]{0,0,0,2}));case "duplicate"->x.getJSONArray("objects").getJSONObject(2).put("sourceId","floor");case "numeric"->x.getJSONArray("objects").getJSONObject(2).put("massKg","1");case "dynamic"->x.getJSONArray("objects").getJSONObject(2).put("massKg",0);}assertThrows(RuntimeException.class,()->SpatialScene.parse(x.toString(),UUID.randomUUID().toString()));}
    }
    @Test public void occlusionDoesNotEraseKnownIdentity() throws Exception {
        var s=SpatialScene.parse(source(),UUID.randomUUID().toString());var pose=new JSONObject().put("position",new JSONArray(new double[]{2,.4,0})).put("yawDeg",-90).put("pitchDeg",0).put("fovDeg",45);var visible=ObserverGeometry.visible(pose,new JSONObject().put("width",800).put("height",600),s.getJSONArray("objects"));assertEquals(3,s.getJSONArray("objects").length());for(Object raw:visible)assertNotEquals(object(s),((JSONObject)raw).getString("id"));
    }
    @Test public void nativeForecastFindsObstacleRecoveryWithoutActuation() throws Exception {
        try(var store=new SpatialStore(Files.createTempDirectory("spatial-forecast"))){var s=store.importScene(source());var before=store.snapshot();var p=store.preview(s,object(s),new JSONArray(new double[]{4,0,0}));System.out.println("SPATIAL_PLAN "+p.getString("route")+" "+p.getJSONArray("trials"));assertTrue(before.similar(store.snapshot()));assertFalse(p.getString("route").equals("direct"));assertFalse(p.getJSONArray("trials").getJSONObject(0).getBoolean("passed"));assertTrue(p.getJSONObject("forecast").getJSONObject("metrics").getBoolean("passed"));System.out.println("SPATIAL_FORECAST "+p.getString("route")+" "+p.getJSONArray("trials"));}
    }
    @Test public void actualNativeReplayAndIndependentResidualRejectTamper() throws Exception {
        var dir=Files.createTempDirectory("spatial-actual");try(var store=new SpatialStore(dir)){var s=store.importScene(source());var p=store.preview(s,object(s),new JSONArray(new double[]{4,0,0}));JSONObject actual;try(var world=new SpatialPhysics(s,dir.resolve("native"))){actual=world.execute(object(s),p.getJSONArray("waypoints"),()->false,snap->{},false);}var r=new JSONObject().put("status","completed").put("definition",s).put("plan",p).put("actual",actual);var m=SpatialStore.verify(r);System.out.println("SPATIAL_VERIFY "+m);assertTrue(m.toString(),m.getBoolean("passed"));assertTrue(m.getDouble("forecastMaxErrorM")<=.02);var row=actual.getJSONArray("samples").getJSONObject(12);row.getJSONArray("position").put(0,9);assertFalse(SpatialStore.verify(r).getBoolean("passed"));}
    }
    @Test public void cancellationReleasesConstraintAndRejectsStaticGrab() throws Exception {
        var dir=Files.createTempDirectory("spatial-cancel");var s=SpatialScene.parse(source(),UUID.randomUUID().toString());try(var world=new SpatialPhysics(s,dir)){assertThrows(IllegalArgumentException.class,()->world.grab(s.getJSONArray("objects").getJSONObject(0).getString("id")));assertThrows(InterruptedException.class,()->world.execute(object(s),new JSONArray().put(new JSONArray(new int[]{2,1,0})),()->true,x->{},false));}
    }
    @Test public void explicitObjectAndHorizontalTaskBinding() throws Exception {
        var s=SpatialScene.parse(source(),UUID.randomUUID().toString());assertThrows(IllegalArgumentException.class,()->SpatialStore.intent(s,object(s),new JSONArray(new int[]{4,1,0})));assertThrows(IllegalArgumentException.class,()->SpatialStore.intent(s,UUID.randomUUID().toString(),new JSONArray(new int[]{4,0,0})));var i=SpatialStore.intent(s,object(s),new JSONArray(new int[]{4,0,0}));assertEquals(2,i.getJSONArray("targetM").getDouble(0),0);
    }
}
