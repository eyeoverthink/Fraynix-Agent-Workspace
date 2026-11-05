package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class Construction051Test {
    static JSONObject source(){return SpatialScene.parse(Construction051.kit().toString(),UUID.randomUUID().toString());}
    static JSONObject scene(){return WorldFrames.scene(WorldFrames.initial(source()));}
    static JSONObject plan(JSONObject scene,double x,double z,double force){return WorldPlan.build(scene,Construction051.intent(scene,new JSONObject().put("centerX",x).put("centerZ",z),force,4));}
    static JSONObject nativeRun(JSONObject scene,JSONObject plan,Path root)throws Exception{
        var events=new JSONArray();var samples=new JSONArray();var run=new JSONObject().put("id",UUID.randomUUID().toString()).put("goalId",UUID.randomUUID().toString()).put("controllerVersion",WorldPhysics.VERSION).put("orientationControl",true).put("definition",scene).put("plan",plan).put("events",events).put("samples",samples);
        try(var physics=new WorldPhysics(scene,plan,root.resolve("native"),true,null,events)){
            run.put("initialState",physics.sample().put("integration",new JSONArray()));
            while(!physics.settled()){physics.step();if(physics.tick%12==0||physics.settled())samples.put(physics.sample().put("integration",physics.takeLedger()));}
            run.put("final",samples.getJSONObject(samples.length()-1)).put("status",physics.controller.getString("phase").equals("finished")?"completed":"failed");
            if(physics.controller.has("reason"))run.put("reason",physics.controller.getString("reason"));
        }
        run.put("verification",WorldVerify.verify(run));Files.writeString(root.resolve(run.getString("id")+".json"),run.toString());return run;
    }
    @Test public void nativeBridgeIsReleasedAndReassembledFromActualPriorFrame()throws Exception{
        Path root=Files.createTempDirectory("construction051-native-");var firstScene=scene();var p=plan(firstScene,0,0,40);assertTrue(p.toString(),p.getBoolean("passed"));
        var first=nativeRun(firstScene,p,root);System.out.println("First assembly "+first.optString("reason")+" "+first.getJSONObject("verification"));assertEquals("completed",first.getString("status"));assertTrue(first.getJSONObject("verification").getBoolean("passed"));assertEquals(3,first.getJSONObject("verification").getInt("grabs"));
        var structure=Construction051.verifyStructure(first);System.out.println("First support "+structure);assertTrue(structure.toString(),structure.getBoolean("passed"));
        var frame=WorldFrames.after(WorldFrames.frame(firstScene),first);var nextScene=WorldFrames.scene(frame);var nextPlan=plan(nextScene,.45,.3,40);assertTrue(nextPlan.toString(),nextPlan.getBoolean("passed"));assertEquals(4,nextPlan.getJSONArray("orderedTargets").length());
        var second=nativeRun(nextScene,nextPlan,root);System.out.println("Second assembly "+second.optString("reason")+" "+second.getJSONObject("verification"));assertEquals("completed",second.getString("status"));assertTrue(second.getJSONObject("verification").getBoolean("passed"));assertTrue(Construction051.verifyStructure(second).getBoolean("passed"));assertEquals(4,second.getJSONObject("verification").getInt("releases"));
        var initial=second.getJSONObject("initialState").getJSONArray("bodies");for(Object raw:frame.getJSONArray("bodies")){var b=(JSONObject)raw;assertEquals(0,WorldVerify.distance(b.getJSONArray("position"),ContactPhysics.dynamic(initial,b.getString("id")).getJSONArray("position")),2e-6);}
    }
    @Test public void insufficientForceCannotEarnConstructionSuccess()throws Exception{
        var s=scene();var r=nativeRun(s,plan(s,0,0,2),Files.createTempDirectory("construction051-weak-"));assertEquals("failed",r.getString("status"));assertFalse(r.getJSONObject("verification").getBoolean("passed"));assertFalse(Construction051.verifyStructure(r).getBoolean("passed"));
    }
    @Test public void sourceDesignAndTraceChangesAreRefused()throws Exception{
        var s=scene();assertThrows(IllegalArgumentException.class,()->plan(s,3,0,40));var p=plan(s,0,0,40);p.getJSONObject("intent").getJSONObject("construction051").put("centerX",.4);assertThrows(IllegalStateException.class,()->WorldPlan.verify(s,p));
        var kit=Construction051.kit();kit.getJSONArray("objects").getJSONObject(1).put("massKg",1);var bad=WorldFrames.scene(WorldFrames.initial(SpatialScene.parse(kit.toString(),UUID.randomUUID().toString())));assertThrows(IllegalArgumentException.class,()->plan(bad,0,0,40));
        var r=nativeRun(s,plan(s,0,0,40),Files.createTempDirectory("construction051-evidence-"));r.getJSONArray("samples").getJSONObject(0).getJSONArray("integration").getJSONArray(0).getJSONArray(2).put(0,900);assertThrows(RuntimeException.class,()->WorldVerify.verify(r));
    }
    @Test public void legacyWorldUpgradeIsExplicitExactAndSourceBound()throws Exception{
        Path root=Files.createTempDirectory("construction051-legacy-");Path file=root.resolve("persistent-worlds/state.json");Files.createDirectories(file.getParent());try(var in=getClass().getResourceAsStream("/legacy-world050.json")){Files.write(file,in.readAllBytes());}
        byte[] original=Files.readAllBytes(file);var ledger=new WorldLedger(root);String id=ledger.list().getJSONObject(0).getString("worldId");var before=ledger.observe(id);assertTrue(before.getBoolean("requiresUpgrade051"));assertArrayEquals(original,Files.readAllBytes(file));
        var upgraded=ledger.upgrade051(id,before.getInt("expectedRevision"),before.getString("expectedHash"));assertEquals(before.getInt("expectedRevision")+1,upgraded.getInt("expectedRevision"));var restored=new WorldLedger(root).observe(id);assertFalse(restored.getBoolean("requiresUpgrade051"));assertTrue(before.getJSONObject("frame").similar(restored.getJSONObject("frame")));assertThrows(ConcurrentModificationException.class,()->ledger.upgrade051(id,before.getInt("expectedRevision"),before.getString("expectedHash")));
        var data=new JSONObject(Files.readString(file));data.getJSONObject("payload").getJSONArray("worlds").getJSONObject(0).put("codeSha256","0".repeat(64));data.put("sha256",WorldFrames.hash(data.getJSONObject("payload")));Files.writeString(file,data.toString());assertThrows(IllegalStateException.class,()->new WorldLedger(root));
    }
    @Test public void heldNativeConstructionResumesFromAnExactCheckpoint()throws Exception{
        Path root=Files.createTempDirectory("construction051-resume-");var s=scene();var p=plan(s,0,0,40);String id=UUID.randomUUID().toString();int pausedTick;
        try(var store=new WorldNativeStore(root)){
            store.start(id,UUID.randomUUID().toString(),s,p,true,true);
            long until=System.currentTimeMillis()+15000;while(store.snapshot().optJSONObject("sample",new JSONObject()).optInt("tick")<480&&System.currentTimeMillis()<until)Thread.sleep(20);
            assertTrue(store.snapshot().getJSONObject("sample").getInt("tick")>=480);store.pause();
            while(store.status(id).equals("running")&&System.currentTimeMillis()<until)Thread.sleep(20);
            assertEquals("paused",store.status(id));pausedTick=store.run(id).getJSONObject("checkpoint").getInt("tick");assertTrue(pausedTick>0);
        }
        try(var store=new WorldNativeStore(root)){
            store.resume(id,s,false);long until=System.currentTimeMillis()+30000;while(store.status(id).equals("running")&&System.currentTimeMillis()<until)Thread.sleep(20);
            var r=store.run(id);assertEquals(r.optString("reason"),"completed",r.getString("status"));var verified=WorldVerify.verify(r);assertTrue(verified.toString(),verified.getBoolean("passed"));assertEquals(1,verified.getInt("resumes"));assertEquals(0,verified.getDouble("maxRestoreVelocityError"),2e-6);assertTrue(Construction051.verifyStructure(r).getBoolean("passed"));assertTrue(r.getJSONObject("final").getInt("tick")>pausedTick);
        }
    }
    @Test public void chatConstructionIsTypedAndSelectedWorldBound()throws Exception{
        String request="Assemble the bridge in the selected construction world at centerX 0.45, centerZ -0.2. Save skill as bridge-reviewed.";
        assertEquals("construction",ChatPolicy044.route(request,new JSONObject(),false));assertNotEquals("construction",ChatPolicy044.route("Build a website about bridge construction",new JSONObject(),false));
        var fields=Requested044.fields(request,"construction");assertEquals(.45,fields.getDouble("centerX"),0);assertEquals(-.2,fields.getDouble("centerZ"),0);assertEquals("bridge-reviewed",fields.getString("saveSkillName"));
        assertThrows(IllegalArgumentException.class,()->Requested044.preserve(new JSONObject().put("centerX",0),fields));assertThrows(IllegalArgumentException.class,()->Construction051.options(new JSONObject().put("centerX","0.3")));
        var schema=Tools044.definition("construction.start").getJSONObject("function").getJSONObject("parameters");assertFalse(schema.getBoolean("additionalProperties"));for(String key:List.of("worldId","success","commit","targets","code"))assertFalse(schema.getJSONObject("properties").has(key));
        var skill=new JSONObject().put("name","bridge-saved").put("version",1).put("kind",Construction051.KIND).put("defaults",new JSONObject().put("forceLimitN",40).put("torqueLimitNm",4));
        var bridge=new Procedure044((route,args)->route.equals("/api/state")?new JSONObject().put("procedures",new JSONArray().put(skill)):new JSONObject().put("args",args));
        var input=new JSONObject().put("args",new JSONObject().put("name","bridge-saved").put("version",1)).put("bindings",new JSONObject().put("world",new JSONObject().put("worldId","selected").put("expectedRevision",2).put("expectedHash","known"))).put("request","Reuse the bridge-saved procedure at centerX 0.2");
        var actual=bridge.run(input).getJSONObject("record").getJSONObject("args");assertEquals("selected",actual.getString("worldId"));assertEquals(2,actual.getInt("expectedRevision"));assertEquals(.2,actual.getDouble("centerX"),0);input.getJSONObject("args").put("worldId","foreign");assertThrows(IllegalArgumentException.class,()->bridge.run(input));
    }
}
