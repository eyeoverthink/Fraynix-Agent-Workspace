package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class MapRecoveryMaterialsTest {
    private static JSONObject await(MapStore store,String id,String status) throws Exception {
        for(int n=0;n<2000;n++){var run=store.run(id);if(run.getString("status").equals(status))return run;
            if(run.getString("status").equals("interrupted")&&!status.equals("interrupted"))fail(run.toString());Thread.sleep(10);}
        throw new AssertionError("Run did not become "+status+": "+store.run(id));
    }
    private static JSONObject material(int tile,double friction,double drag){return new JSONObject().put("tile",tile).put("name","Declared test surface").put("friction",friction).put("dragNsPerM",drag);}
    private static JSONObject options(JSONObject source,double friction,double drag){return new JSONObject().put("width",12).put("height",12).put("seed",2801).put("materials",new JSONArray().put(material(source.getJSONArray("palette").toList().indexOf("#d9efe5"),friction,drag)));}

    @Test public void nativeBodyRestoresMovingRotatingStateBeforeFirstContinuedStep() throws Exception {
        var source=MapDefinition.image(MapDefinition.example());var map=MapDefinition.generated(source,options(source,.06,0));
        Path nativeDir=Files.createTempDirectory("map-restore030-");JSONObject checkpoint;JSONObject before;
        try(var world=new MapPhysics(map,nativeDir)){
            for(int n=0;n<173;n++)world.step(false,1,0);
            before=world.sample();assertTrue(before.getJSONArray("velocity").getDouble(0)>.1);
            checkpoint=world.checkpoint(before);
        }
        try(var restored=new MapPhysics(map,nativeDir,checkpoint)){
            var actual=restored.sample();for(String key:List.of("position","velocity","angularVelocity"))
                assertTrue(key+actual,actual.getJSONArray(key).similar(before.getJSONArray(key)));
            // Bullet may round orientation through its native transform representation.
            for(int k=0;k<4;k++)assertEquals(before.getJSONArray("rotation").getDouble(k),actual.getJSONArray("rotation").getDouble(k),1e-6);
            assertEquals(173,restored.tick);assertEquals(before.getDouble("actuatorWorkJ"),actual.getDouble("actuatorWorkJ"),0);
            restored.step(false,0,0);assertEquals(174,restored.tick);assertEquals(1,restored.sample().getInt("positionAssignments"));
        }
    }

    @Test public void persistedInProgressRunResumesSameIdentityWithOneImpulseAndWholeWorkTrace() throws Exception {
        Path root=Files.createTempDirectory("map-progress030-");String id=UUID.randomUUID().toString();JSONObject saved;
        try(var store=new MapStore(root)){
            var ref=store.importImage(MapDefinition.example());var map=store.build(ref.getString("id"),options(ref,.6,.25));
            store.start(id,map.getString("id"),true,.35,true);
            for(int n=0;n<1200;n++){if(store.snapshot().optJSONObject("sample",new JSONObject()).optInt("tick")>=600)break;Thread.sleep(5);}
            store.pause();saved=await(store,id,"paused");assertTrue(saved.getJSONObject("checkpoint").getInt("tick")>=600);
        }
        try(var store=new MapStore(root)){
            store.resume(id,false);var result=await(store,id,"completed");assertTrue(result.getJSONObject("verification").toString(),result.getJSONObject("verification").getBoolean("passed"));
            assertEquals(1,result.getInt("resumes"));assertEquals(saved.getString("id"),result.getString("id"));
            assertEquals(1,result.getJSONArray("events").toList().stream().filter(x->((Map<?,?>)x).get("kind").equals("impulse")).count());
            var event=result.getJSONArray("events").toList().stream().map(x->(Map<?,?>)x).filter(x->x.get("kind").equals("resume")).findFirst().orElseThrow();
            assertEquals(saved.getJSONObject("checkpoint").getInt("tick"),((Number)event.get("tick")).intValue());
            assertEquals(result.getJSONObject("final").getInt("tick"),MapWork.audit(result).getInt("integrationTicks"));
            assertTrue(MapWork.audit(result).getDouble("dragWorkJ")<0);
        }
    }

    @Test public void interruptedManualInputIsNotReplayed() throws Exception {
        Path root=Files.createTempDirectory("map-manual030-");String id=UUID.randomUUID().toString();
        try(var store=new MapStore(root)){
            var ref=store.importImage(MapDefinition.example());var map=store.build(ref.getString("id"),options(ref,.06,0));store.start(id,map.getString("id"),false,0,true);
            store.control(id,1,0);Thread.sleep(180);store.pause();await(store,id,"paused");
        }
        try(var store=new MapStore(root)){
            store.resume(id,true);Thread.sleep(200);store.pause();var run=await(store,id,"paused");
            var samples=run.getJSONArray("samples");var last=samples.getJSONObject(samples.length()-1);
            assertTrue("No held +X input after resume: "+last,last.getJSONArray("force").getDouble(0)<=.01);
            assertThrows(IllegalStateException.class,()->store.control(UUID.randomUUID().toString(),1,0));
        }
    }

    @Test public void editedCheckpointIsPreservedAndRefusedRatherThanRespawned() throws Exception {
        Path root=Files.createTempDirectory("map-edit030-");String id=UUID.randomUUID().toString();
        try(var store=new MapStore(root)){
            var ref=store.importImage(MapDefinition.example());var map=store.build(ref.getString("id"),options(ref,.6,0));store.start(id,map.getString("id"),false,0,true);Thread.sleep(120);store.pause();await(store,id,"paused");
        }
        var file=root.resolve("maps/checkpoints/"+id+".json");var edited=new JSONObject(Files.readString(file));edited.getJSONObject("payload").getJSONObject("physics").getJSONArray("velocity").put(0,7);Files.writeString(file,edited.toString());
        try(var store=new MapStore(root)){assertThrows(IllegalStateException.class,()->store.resume(id,false));assertNotEquals("running",store.run(id).getString("status"));}
        assertTrue(new JSONObject(Files.readString(file)).similar(edited));
    }

    @Test public void codeAndSourceMismatchesAreRefusedEvenWithRecomputedEnvelopeHash() throws Exception {
        Path root=Files.createTempDirectory("map-binding030-");String id=UUID.randomUUID().toString();JSONObject record;
        try(var store=new MapStore(root)){
            var ref=store.importImage(MapDefinition.example());record=store.build(ref.getString("id"),options(ref,.6,0));store.start(id,record.getString("id"),false,0,true);Thread.sleep(100);store.pause();await(store,id,"paused");
        }
        var file=root.resolve("maps/checkpoints/"+id+".json");var p=new JSONObject(Files.readString(file)).getJSONObject("payload");
        p.put("codeSha256","0".repeat(64));MapCheckpoint.write(file,p);assertThrows(IllegalStateException.class,()->MapCheckpoint.read(file,record,id));
        p.put("codeSha256",MapCheckpoint.identity()).put("mapSha256","f".repeat(64));MapCheckpoint.write(file,p);assertThrows(IllegalStateException.class,()->MapCheckpoint.read(file,record,id));
    }

    @Test public void declaredMaterialsDriveNativePhysicsAndWorkIsIndependentlyReconstructed() throws Exception {
        try(var store=new MapStore(Files.createTempDirectory("map-surfaces030-"))){
            var ref=store.importImage(MapDefinition.example());var results=new JSONArray();
            for(double[] surface:new double[][]{{.06,.02},{.85,.25},{1.1,6}}){
                var map=store.build(ref.getString("id"),options(ref,surface[0],surface[1]));
                var run=store.start(UUID.randomUUID().toString(),map.getString("id"),true,.35,false);run=await(store,run.getString("id"),"completed");
                assertTrue(run.getJSONObject("verification").toString(),run.getJSONObject("verification").getBoolean("passed"));
                var work=MapWork.audit(run);assertTrue(work.getDouble("positiveWorkJ")>0);assertEquals(work.getDouble("actuatorWorkJ"),work.getDouble("positiveWorkJ")-work.getDouble("brakingWorkJ"),1e-8);results.put(work);
                var edited=new JSONObject(run.toString());edited.getJSONArray("samples").getJSONObject(0).put("actuatorWorkJ",500);assertThrows(IllegalStateException.class,()->MapWork.audit(edited));
                var text=store.exportMap(map.getString("id"));assertTrue(store.importMap(text).getJSONObject("definition").similar(map.getJSONObject("definition")));
            }
            System.out.println("MATERIAL_TRIALS_030 "+results);
            assertNotEquals(results.getJSONObject(0).getDouble("actuatorWorkJ"),results.getJSONObject(2).getDouble("actuatorWorkJ"),.01);
        }
    }

    @Test public void materialDeclarationsRejectInvalidAndDuplicateOrBlockedTiles() throws Exception {
        var source=MapDefinition.image(MapDefinition.example());var opts=options(source,.6,0);
        var duplicate=new JSONObject(opts.toString());duplicate.getJSONArray("materials").put(duplicate.getJSONArray("materials").getJSONObject(0));assertThrows(IllegalArgumentException.class,()->MapDefinition.generated(source,duplicate));
        for(var invalid:List.of(material(0,-1,0),material(0,.6,9),material(7,.6,0)))assertThrows(RuntimeException.class,()->MapDefinition.generated(source,new JSONObject().put("materials",new JSONArray().put(invalid))));
        assertThrows(JSONException.class,()->material(0,.6,Double.NaN));
    }

    @Test public void restoredAndUninterruptedNativeTrajectoriesAgreeWithinDeclaredPrecision() throws Exception {
        var ref=MapDefinition.image(MapDefinition.example());var map=MapDefinition.generated(ref,options(ref,.85,.25));
        var nativeDir=Files.createTempDirectory("map-compare030-");JSONObject state,baseline;
        try(var world=new MapPhysics(map,nativeDir)){for(int k=0;k<600;k++)world.step(true,0,0);state=world.checkpoint(world.sample());for(int k=0;k<600;k++)world.step(true,0,0);baseline=world.sample();}
        try(var world=new MapPhysics(map,nativeDir,state)){for(int k=0;k<600;k++)world.step(true,0,0);var actual=world.sample();
            double positionError=ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(actual.getJSONArray("position"),30),SpatialScene.vector(baseline.getJSONArray("position"),30)));
            double velocityError=ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(actual.getJSONArray("velocity"),30),SpatialScene.vector(baseline.getJSONArray("velocity"),30)));
            assertTrue(positionError<.005);assertTrue(velocityError<.01);assertEquals(baseline.getDouble("actuatorWorkJ"),actual.getDouble("actuatorWorkJ"),Math.max(.001,baseline.getDouble("positiveWorkJ")*.01));
            System.out.println("RESTORE_COMPARISON_030 "+new JSONObject().put("positionErrorM",positionError).put("velocityErrorMps",velocityError).put("workDifferenceJ",actual.getDouble("actuatorWorkJ")-baseline.getDouble("actuatorWorkJ")));
        }
    }

    @Test public void declaredRestitutionChangesAnActualNativeBounce() throws Exception {
        var ref=MapDefinition.image(MapDefinition.example());double[] heights=new double[2];
        for(int trial=0;trial<2;trial++){
            var opts=options(ref,.6,0);opts.getJSONArray("materials").getJSONObject(0).put("restitution",trial==0?0:.85);
            try(var world=new MapPhysics(MapDefinition.generated(ref,opts),Files.createTempDirectory("map-bounce030-"))){
                world.avatar.applyCentralImpulse(new com.jme3.math.Vector3f(0,3,0));boolean impacted=false;
                // Observe the rebound after measured contact, not an assumed fixed impact time.
                for(int tick=0;tick<300;tick++){world.step(false,0,0);if(tick>20&&world.sample().getInt("contacts")>0)impacted=true;if(impacted)heights[trial]=Math.max(heights[trial],world.avatar.getPhysicsLocation().y);}
            }
        }
        assertTrue(Arrays.toString(heights),heights[1]>heights[0]+.05);System.out.println("BOUNCE_HEIGHTS_030 "+Arrays.toString(heights));
    }

    @Test public void originalPublishedReportCanBeReverifiedWithoutRewritingHistoricalContents() throws Exception {
        Path repo=Path.of("").toAbsolutePath();while(repo!=null&&!Files.exists(repo.resolve("docs/readmes/029/topology-integration.json")))repo=repo.getParent();assertNotNull(repo);
        var prior=new JSONObject(Files.readString(repo.resolve("docs/readmes/029/topology-integration.json"))).getJSONObject("first");
        MapReport.checkSaved(prior.getJSONObject("goal"),prior.getJSONObject("map"),prior.getJSONObject("run"),prior.getJSONObject("artifact"));
        var edited=new JSONObject(prior.getJSONObject("artifact").toString());edited.getJSONObject("contents").put("index.html","Edited historical report");
        assertThrows(IllegalStateException.class,()->MapReport.checkSaved(prior.getJSONObject("goal"),prior.getJSONObject("map"),prior.getJSONObject("run"),edited));
    }
}
