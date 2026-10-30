package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class BodyActuatorTest {
    record Fixture(Path root,SceneStore scenes,ObserverStore observers,JSONObject scene,JSONObject observer) implements AutoCloseable {
        public void close(){scenes.close();}
        JSONObject observation(){return observers.observe(new JSONObject().put("id",observer.getString("id")));}
        JSONObject action(String action){return new JSONObject().put("action",action).put("id",observer.getString("id")).put("operationId",UUID.randomUUID().toString()).put("observationId",observation().getString("id"));}
        JSONObject grab() throws Exception{return observers.body(action("grab").put("entityId",scene.getJSONObject("entityIds").getString("block")));}
        JSONObject waitFor(JSONObject operation) throws Exception {
            long deadline=System.currentTimeMillis()+6000;
            while(operation.getString("status").equals("pending")&&System.currentTimeMillis()<deadline){Thread.sleep(15);operation=scenes.bodyOperation(operation.getString("id"));}
            assertEquals(operation.toString(),"completed",operation.getString("status"));return operation;
        }
        JSONObject move(JSONObject grab,double[] target) throws Exception{return observers.body(action("move").put("runId",grab.getString("runId")).put("gripId",grab.getString("id")).put("target",new JSONArray(target)));}
        JSONObject release(JSONObject grab) throws Exception{return waitFor(observers.body(action("release").put("runId",grab.getString("runId")).put("gripId",grab.getString("id"))));}
    }
    static Fixture fixture() throws Exception {
        var root=Files.createTempDirectory("body-native-");var scenes=new SceneStore(root);var scene=scenes.save(new JSONObject().put("parameters",new JSONObject().put("angleDeg",0)));
        var observers=new ObserverStore(root,scenes);var observer=observers.open(new JSONObject().put("sceneId",scene.getString("id")));
        return new Fixture(root,scenes,observers,scene,observer);
    }
    static double[] p(JSONObject state){return ObserverGeometry.v(state.getJSONArray("position"));}
    @Test public void grabCreatesActualNativeConstraintWithoutTeleportOrKinematicBody() throws Exception {
        try(var f=fixture()){
            var original=new JSONArray(RampDefinition.from(f.scene.getJSONObject("parameters")).start());var grab=f.grab();var after=grab.getJSONObject("after");
            assertEquals(0,after.getInt("tick"));assertTrue(after.getJSONObject("grip").getBoolean("held"));
            assertEquals("native point-to-point constraint",after.getJSONObject("grip").getString("kind"));
            assertEquals(1.25,after.getJSONObject("grip").getDouble("impulseClampPerAxisNs"),0);
            for(int i=0;i<3;i++)assertEquals(original.getDouble(i),after.getJSONArray("position").getDouble(i),1e-6);
            var block=after.getJSONArray("objects").getJSONObject(2);assertEquals("dynamic",block.getString("bodyType"));assertEquals(1,block.getDouble("massKg"),0);
            assertEquals(1,f.scenes.read(f.scene.getString("id")).getInt("version"));f.release(grab);
        }
    }
    @Test public void moveAdvancesIntermediateNativeTicksAndReleasePreservesMomentumThenFalls() throws Exception {
        try(var f=fixture()){
            var grab=f.grab();var start=p(grab.getJSONObject("after"));var target=new double[]{start[0],start[1]+.75,start[2]};
            var pending=f.move(grab,target);assertEquals("pending",pending.getString("status"));
            Thread.sleep(160);var mid=f.scenes.snapshot().getJSONObject("current");assertTrue(mid.getInt("tick")>0);assertTrue(mid.getInt("tick")<pending.getInt("totalTicks"));assertTrue(mid.getJSONArray("position").getDouble(1)>start[1]+.02);
            var moved=f.waitFor(pending);assertTrue(moved.getBoolean("reached"));assertEquals(.75,p(moved.getJSONObject("after"))[1]-start[1],.08);
            var release=f.release(grab);assertTrue(release.getJSONArray("velocityAtConstraintRemoval").similar(release.getJSONObject("before").getJSONArray("velocity")));
            assertFalse(release.getJSONObject("after").getJSONObject("grip").getBoolean("held"));assertEquals(120,release.getInt("endTick")-release.getInt("startTick"));
            assertTrue(p(release.getJSONObject("after"))[1]<p(release.getJSONObject("before"))[1]-.25);
            var run=f.scenes.runRecord(grab.getString("runId"));assertEquals("completed",run.getString("status"));assertTrue(run.getJSONObject("manipulationEvaluation").getBoolean("allMovesReached"));assertFalse(run.getJSONObject("evaluation").getBoolean("applicable"));
        }
    }
    @Test public void collisionProducesMeasuredBlockedOutcomeWithoutPassingThroughRamp() throws Exception {
        try(var f=fixture()){
            var grab=f.grab();var start=p(grab.getJSONObject("after"));var moved=f.waitFor(f.move(grab,new double[]{start[0],.26,start[2]}));
            assertFalse(moved.getBoolean("reached"));assertEquals("blocked-or-force-limited",moved.getString("outcome"));
            assertTrue(p(moved.getJSONObject("after"))[1]>.45);assertTrue(moved.getJSONObject("after").getInt("contacts")>0);
            f.release(grab);assertFalse(f.scenes.runRecord(grab.getString("runId")).getJSONObject("manipulationEvaluation").getBoolean("allMovesReached"));
        }
    }
    @Test public void staticTargetsAndOutOfReachGrabsHaveNoNativeEffect() throws Exception {
        try(var f=fixture()){
            var staticRequest=f.action("grab").put("entityId",f.scene.getJSONObject("entityIds").getString("ramp"));
            assertThrows(IllegalArgumentException.class,()->f.observers.body(staticRequest));assertEquals("idle",f.scenes.snapshot().getString("status"));
            f.observers.action(new JSONObject().put("id",f.observer.getString("id")).put("observationId",f.observation().getString("id")).put("operationId",UUID.randomUUID().toString()).put("action","move").put("up",5));
            assertThrows(IllegalArgumentException.class,()->f.grab());assertEquals("idle",f.scenes.snapshot().getString("status"));
        }
    }
    @Test public void staleSourceWrongOwnerAndWrongGripCannotBorrowAnAttachment() throws Exception {
        try(var f=fixture()){
            var old=f.observation();var grab=f.grab();var target=p(grab.getJSONObject("after"));target[1]+=.5;
            var bad=f.action("move").put("runId",grab.getString("runId")).put("gripId",UUID.randomUUID().toString()).put("target",new JSONArray(target));
            assertThrows(IllegalStateException.class,()->f.observers.body(bad));
            bad.put("gripId",grab.getString("id")).put("observationId",old.getString("id"));
            assertThrows(ConcurrentModificationException.class,()->f.observers.body(bad));
            var other=f.observers.open(new JSONObject().put("sceneId",f.scene.getString("id")).put("newObserver",true));
            bad.put("id",other.getString("id")).put("observationId",f.observers.observe(new JSONObject().put("id",other.getString("id"))).getString("id"));
            assertThrows(IllegalStateException.class,()->f.observers.body(bad));assertEquals(0,f.scenes.snapshot().getJSONObject("current").getInt("tick"));f.release(grab);
        }
    }
    @Test public void identicalOperationRetriesReturnReceiptAndDoNotMoveTwice() throws Exception {
        try(var f=fixture()){
            var grab=f.grab();var target=p(grab.getJSONObject("after"));target[1]+=.5;
            var input=f.action("move").put("runId",grab.getString("runId")).put("gripId",grab.getString("id")).put("target",new JSONArray(target));
            var moved=f.waitFor(f.observers.body(input));int tick=moved.getInt("endTick");
            assertEquals(moved.getString("id"),f.observers.body(input).getString("id"));assertEquals(tick,f.scenes.snapshot().getJSONObject("current").getInt("tick"));
            input.getJSONArray("target").put(1,target[1]+.1);assertThrows(IllegalArgumentException.class,()->f.observers.body(input));f.release(grab);
        }
    }
    @Test public void cancellationStopsPendingMotionAndRemovesTheConstraint() throws Exception {
        try(var f=fixture()){
            var grab=f.grab();var target=p(grab.getJSONObject("after"));target[1]+=1.5;var pending=f.move(grab,target);Thread.sleep(80);
            assertThrows(IllegalStateException.class,()->f.scenes.cancelOwnedBody(UUID.randomUUID().toString(),grab.getString("runId")));
            var cancelled=f.scenes.cancelOwnedBody(f.observer.getString("id"),grab.getString("runId"));
            assertEquals("cancelled",cancelled.getString("status"));assertFalse(cancelled.getJSONObject("current").getJSONObject("grip").getBoolean("held"));
            assertEquals("cancelled",f.scenes.bodyOperation(pending.getString("id")).getString("status"));
            int tick=cancelled.getJSONObject("current").getInt("tick");Thread.sleep(70);assertEquals(tick,f.scenes.snapshot().getJSONObject("current").getInt("tick"));
        }
    }
    @Test public void moveBudgetReservesAnActionAndTicksForRealRelease() throws Exception {
        try(var f=fixture()){
            var grab=f.grab();
            for(int i=0;i<22;i++){var target=p(f.scenes.snapshot().getJSONObject("current"));target[1]+=.02;f.waitFor(f.move(grab,target));}
            var target=p(f.scenes.snapshot().getJSONObject("current"));target[1]+=.02;
            assertThrows(IllegalStateException.class,()->f.move(grab,target));
            var released=f.release(grab);assertFalse(released.getJSONObject("after").getJSONObject("grip").getBoolean("held"));
            assertEquals(24,f.scenes.runRecord(grab.getString("runId")).getJSONArray("actuatorEvents").length());
        }
    }
    @Test public void interruptedIntentIsNotRetriedAndCompletedManipulationReplaysNativeCommands() throws Exception {
        Path root;String opId,runId;try(var f=fixture()){
            root=f.root;var grab=f.grab();runId=grab.getString("runId");var target=p(grab.getJSONObject("after"));target[1]+=.6;
            opId=f.move(grab,target).getString("id");Thread.sleep(60);
        }
        try(var scenes=new SceneStore(root)){assertEquals("interrupted",scenes.bodyOperation(opId).getString("status"));assertEquals("interrupted",scenes.runRecord(runId).getString("status"));assertEquals("idle",scenes.snapshot().getString("status"));}
        try(var f=fixture()){
            var grab=f.grab();var target=p(grab.getJSONObject("after"));target[1]+=.5;f.waitFor(f.move(grab,target));f.release(grab);
            var original=f.scenes.runRecord(grab.getString("runId"));f.scenes.control(new JSONObject().put("action","reset"));var replay=f.scenes.replay(original.getString("id"));
            assertTrue(replay.getJSONObject("replayEvaluation").toString(),replay.getJSONObject("replayEvaluation").getBoolean("passed"));assertEquals(original.getJSONObject("final").getInt("tick"),replay.getJSONObject("final").getInt("tick"));
        }
    }
}
