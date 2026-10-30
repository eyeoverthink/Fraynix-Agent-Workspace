package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import static org.junit.Assert.*;

public class ObserverStoreTest {
    static JSONObject open(ObserverStore store,JSONObject scene) throws Exception {return store.open(new JSONObject().put("sceneId",scene.getString("id")));}
    static JSONObject observe(ObserverStore store,JSONObject observer){return store.observe(new JSONObject().put("id",observer.getString("id")));}
    static JSONObject action(JSONObject observer,JSONObject observation,String name){
        return new JSONObject().put("id",observer.getString("id")).put("observationId",observation.getString("id")).put("operationId",UUID.randomUUID().toString()).put("action",name);
    }
    static JSONObject capture(JSONObject observation) throws Exception {
        // Decoder fixture only; actual rendered pixels are verified by the desktop integration.
        var image=new BufferedImage(960,540,BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
        return new JSONObject().put("observationId",observation.getString("id")).put("operationId",UUID.randomUUID().toString()).put("worldSha256",observation.getString("worldSha256")).put("pngBase64",Base64.getEncoder().encodeToString(out.toByteArray()));
    }
    @Test public void defaultsSeeActualNativeEntitiesAndDoNotCreateAnAvatar() throws Exception {
        var root=Files.createTempDirectory("observer-default-");try(var scenes=new SceneStore(root)){
            var scene=scenes.save(new JSONObject());var store=new ObserverStore(root,scenes);var observer=open(store,scene);var packet=observe(store,observer);
            assertEquals(observer.getString("id"),open(store,scene).getString("id"));assertEquals(1,store.list().length());
            assertEquals(3,packet.getJSONObject("world").getJSONArray("objects").length());assertEquals("authored-geometry",packet.getJSONObject("world").getString("source"));
            assertTrue(packet.getJSONArray("visibleEntities").toList().stream().anyMatch(o->((Map<?,?>)o).get("id").equals(scene.getJSONObject("entityIds").getString("block"))));
            assertEquals(scene.getInt("version"),scenes.read(scene.getString("id")).getInt("version"));assertTrue(observer.getString("motion").contains("no avatar"));
        }
    }
    @Test public void movementIsBoundedVersionedAndIdempotent() throws Exception {
        var root=Files.createTempDirectory("observer-move-");try(var scenes=new SceneStore(root)){
            var scene=scenes.save(new JSONObject());var store=new ObserverStore(root,scenes);var observer=open(store,scene);var packet=observe(store,observer);double x=observer.getJSONObject("pose").getJSONArray("position").getDouble(0);
            var input=action(observer,packet,"move").put("right",1);var result=store.action(input);
            assertEquals(x+1,result.getJSONObject("observer").getJSONObject("pose").getJSONArray("position").getDouble(0),1e-12);assertEquals(2,result.getJSONObject("observer").getInt("version"));
            assertTrue(store.action(input).getBoolean("duplicate"));assertEquals(2,store.read(observer.getString("id")).getInt("version"));assertEquals(1,store.history(observer.getString("id")).getJSONArray("events").length());
            assertThrows(ConcurrentModificationException.class,()->store.action(action(observer,packet,"move").put("right",1)));
            var fresh=observe(store,observer);assertThrows(IllegalArgumentException.class,()->store.action(action(observer,fresh,"move").put("forward",6)));
            assertThrows(IllegalArgumentException.class,()->store.action(action(observer,fresh,"move").put("pitchDeg",1)));
            assertEquals(2,store.read(observer.getString("id")).getInt("version"));assertEquals(1,scenes.read(scene.getString("id")).getInt("version"));
        }
    }
    @Test public void focusAndInspectRequireRealVisibleIdentifiers() throws Exception {
        var root=Files.createTempDirectory("observer-inspect-");try(var scenes=new SceneStore(root)){
            var scene=scenes.save(new JSONObject());var store=new ObserverStore(root,scenes);var observer=open(store,scene);
            var packet=observe(store,observer);String block=scene.getJSONObject("entityIds").getString("block");
            var inspected=store.action(action(observer,packet,"inspect").put("entityId",block)).getJSONObject("receipt").getJSONObject("inspected");assertEquals(block,inspected.getString("id"));assertEquals("Block",inspected.getString("label"));
            assertThrows(IllegalArgumentException.class,()->store.action(action(observer,observe(store,observer),"inspect").put("entityId",UUID.randomUUID().toString())));
            store.action(action(observer,observe(store,observer),"look").put("yawDeg",90));store.action(action(observer,observe(store,observer),"look").put("yawDeg",90));
            assertThrows(IllegalArgumentException.class,()->store.action(action(observer,observe(store,observer),"inspect").put("entityId",block)));
            store.action(action(observer,observe(store,observer),"focus").put("entityId",block));assertTrue(observe(store,observer).getJSONArray("visibleEntities").toList().stream().anyMatch(o->((Map<?,?>)o).get("id").equals(block)));
        }
    }
    @Test public void sceneRevisionsAndAdvancingPhysicsCannotBorrowOldInspection() throws Exception {
        var root=Files.createTempDirectory("observer-world-");try(var scenes=new SceneStore(root)){
            var scene=scenes.save(new JSONObject());var store=new ObserverStore(root,scenes);var observer=open(store,scene);var authored=observe(store,observer);
            scenes.control(new JSONObject().put("action","start").put("id",scene.getString("id")).put("expectedVersion",1).put("seconds",1));scenes.control(new JSONObject().put("action","pause"));
            var paused=observe(store,observer);int before=paused.getJSONObject("world").getInt("tick");scenes.control(new JSONObject().put("action","step"));
            assertThrows(ConcurrentModificationException.class,()->store.action(action(observer,paused,"focus").put("entityId",scene.getJSONObject("entityIds").getString("block"))));
            var current=observe(store,observer);assertEquals(before+1,current.getJSONObject("world").getInt("tick"));assertEquals("native-physics",current.getJSONObject("world").getString("source"));
            scenes.control(new JSONObject().put("action","reset"));scenes.save(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("parameters",new JSONObject().put("massKg",2)));
            assertThrows(ConcurrentModificationException.class,()->store.action(action(observer,authored,"move").put("right",1)));
            assertEquals(1,store.read(observer.getString("id")).getInt("version"));
        }
    }
    @Test public void pngSourcesPersistAndReplayIsReadOnlyAfterRestartAndEdits() throws Exception {
        var root=Files.createTempDirectory("observer-replay-");String id,frameId,hash;JSONObject packet;
        try(var scenes=new SceneStore(root)){
            var scene=scenes.save(new JSONObject());var store=new ObserverStore(root,scenes);var observer=open(store,scene);id=observer.getString("id");packet=observe(store,observer);var request=capture(packet);var frame=store.capture(request);
            frameId=frame.getString("id");hash=frame.getString("pngSha256");assertEquals(frameId,store.capture(request).getString("id"));assertEquals(1,store.read(id).getInt("captureCount"));
            store.action(action(observer,packet,"move").put("right",1));scenes.save(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("parameters",new JSONObject().put("angleDeg",10)));
        }
        try(var scenes=new SceneStore(root)){
            var restored=new ObserverStore(root,scenes);assertEquals(2,restored.read(id).getInt("version"));var replay=restored.replay(frameId);
            assertTrue(replay.getJSONObject("packet").getBoolean("readOnly"));assertEquals(hash,replay.getJSONObject("capture").getString("pngSha256"));assertTrue(packet.getJSONObject("world").similar(replay.getJSONObject("packet").getJSONObject("world")));
            assertEquals(1,replay.getJSONObject("packet").getJSONObject("world").getInt("sceneVersion"));assertEquals(1,restored.history(id).getJSONArray("captures").length());
            assertThrows(IllegalStateException.class,()->restored.action(action(restored.read(id),packet,"move").put("right",1)));
        }
    }
    @Test public void invalidOrMismatchedCaptureHasNoCommittedReceipt() throws Exception {
        var root=Files.createTempDirectory("observer-image-");try(var scenes=new SceneStore(root)){
            var store=new ObserverStore(root,scenes);var observer=open(store,scenes.save(new JSONObject()));var packet=observe(store,observer);
            assertThrows(IllegalArgumentException.class,()->store.capture(capture(packet).put("worldSha256","wrong")));
            assertThrows(IllegalArgumentException.class,()->store.capture(capture(packet).put("pngBase64",Base64.getEncoder().encodeToString(new byte[100]))));
            var narrow=store.observe(new JSONObject().put("id",observer.getString("id")).put("width",640).put("height",480));assertThrows(IllegalArgumentException.class,()->store.capture(capture(narrow)));
            assertEquals(0,store.history(observer.getString("id")).getJSONArray("captures").length());
        }
    }
    @Test public void observationExpiryAndCrossObserverInputsAreRejected() throws Exception {
        var root=Files.createTempDirectory("observer-lease-");
        class Time extends Clock{long now=100000;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return Instant.ofEpochMilli(now);}}
        var time=new Time();try(var scenes=new SceneStore(root)){
            var scene=scenes.save(new JSONObject());var store=new ObserverStore(root,scenes,time);var observer=open(store,scene);var packet=observe(store,observer);
            var other=store.open(new JSONObject().put("sceneId",scene.getString("id")).put("newObserver",true));
            assertThrows(IllegalArgumentException.class,()->store.action(action(other,packet,"move").put("right",1)));
            time.now+=120001;assertThrows(IllegalStateException.class,()->store.action(action(observer,packet,"move").put("right",1)));assertEquals(1,store.read(observer.getString("id")).getInt("version"));
        }
    }
    @Test public void geometricOcclusionDoesNotExposeTheHiddenBox() {
        var pose=new JSONObject().put("position",new JSONArray(List.of(0,0,5))).put("yawDeg",0).put("pitchDeg",0).put("fovDeg",60);
        var rotation=new JSONArray(List.of(0,0,0,1));
        var front=RampDefinition.object("front","Front",new double[]{0,0,0},rotation,new double[]{3,3,1},0);
        var back=RampDefinition.object("back","Back",new double[]{0,0,-3},rotation,new double[]{1,1,1},0);
        var found=ObserverGeometry.visible(pose,new JSONObject().put("width",960).put("height",540),new JSONArray(List.of(front,back)));
        assertEquals(1,found.length());assertEquals("front",found.getJSONObject(0).getString("id"));assertEquals(4.5,ObserverGeometry.hit(front,new double[]{0,0,5},new double[]{0,0,-1}),1e-12);
    }
}
