package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RecordedReplay163Test {
    static RecordedReplay163 memo(Alignment041Test.Fixture f,boolean enabled){return new RecordedReplay163(f.root,f.store,f.ledger,new PlaneReplay160(f.root,f.store,f.ledger),enabled);}
    static JSONObject binding(Alignment041Test.Fixture f,JSONObject t)throws Exception{return new PlaneReplay160(f.root,f.store,f.ledger).binding(t.getString("id"));}
    @Test public void exactReuseColdForceAndReturnedObjectIsolation()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var b=binding(f,PlaneAccuracy160Test.movement(f));var m=memo(f,true);var original=m.read(b,false,()->{});var saved=WorldFrames.copy(original);
        assertEquals(1,m.status().getInt("fullReplays"));var next=m.read(b,false,()->{});assertTrue(original.similar(next));assertEquals(1,m.status().getInt("reused"));
        next.getJSONObject("trial").getJSONArray("trace").getJSONObject(0).put("ordinal",999);assertTrue(saved.similar(m.read(b,false,()->{})));
        assertTrue(saved.similar(m.read(b,true,()->{})));assertEquals(2,m.status().getInt("fullReplays"));var cold=memo(f,true);assertTrue(saved.similar(cold.read(b,false,()->{})));assertEquals(0,cold.status().getInt("reused"));
        var full=memo(f,false);assertTrue(saved.similar(full.read(b,false,()->{})));assertTrue(saved.similar(full.read(b,false,()->{})));assertEquals(2,full.status().getInt("fullReplays"));assertEquals(0,full.status().getInt("entries"));
    }}
    @Test public void warmChecksCatchSameSizeTimestampCorruptionAndMetadata()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=PlaneAccuracy160Test.movement(f);var b=binding(f,t);var m=memo(f,true);m.read(b,false,()->{});
        Path p=f.root.resolve("alignment-041/frames/"+t.getString("id")+"/10.image");byte[] good=Files.readAllBytes(p),bad=good.clone();bad[bad.length/2]^=1;var time=Files.getLastModifiedTime(p);Files.write(p,bad);Files.setLastModifiedTime(p,time);
        assertThrows(Exception.class,()->m.read(b,false,()->{}));assertEquals(0,m.status().getInt("entries"));Files.write(p,good);m.read(b,false,()->{});
        Path meta=p.resolveSibling("10.meta.json");String text=Files.readString(meta);Files.writeString(meta,new JSONObject(text).put("seconds",9999).toString());assertThrows(Exception.class,()->m.read(b,false,()->{}));Files.writeString(meta,text);m.read(b,false,()->{});assertEquals(2,m.status().getInt("failures"));
    }}
    @Test public void missingStateIsNotRecreatedAndOriginalWorldIsRechecked()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var b=binding(f,PlaneAccuracy160Test.movement(f));var m=memo(f,true);m.read(b,false,()->{});
        for(String name:new String[]{"alignment-041/state.json","persistent-worlds/state.json"}){Path p=f.root.resolve(name),backup=p.resolveSibling("preserved.json");Files.move(p,backup);try{assertThrows(Exception.class,()->m.read(b,false,()->{}));assertFalse(Files.exists(p));}finally{Files.move(backup,p);}m.read(b,false,()->{});}
        Path p=f.root.resolve("persistent-worlds/state.json");byte[] bytes=Files.readAllBytes(p);Files.writeString(p,"{}");assertThrows(Exception.class,()->m.read(b,false,()->{}));Files.write(p,bytes);m.read(b,false,()->{});
    }}
    @Test public void changedTrackerCannotGetWarmAuthorityWithANewBinding()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=PlaneAccuracy160Test.movement(f);var m=memo(f,true);m.read(binding(f,t),false,()->{});Path p=f.root.resolve("alignment-041/state.json");var envelope=new JSONObject(Files.readString(p));var payload=envelope.getJSONObject("payload");payload.getJSONArray("trials").getJSONObject(0).getJSONArray("trace").getJSONObject(0).getJSONObject("tracker").getJSONArray("point").put(0,.2);envelope.put("sha256",WorldFrames.hash(payload));Files.writeString(p,envelope.toString());var changed=binding(f,t);assertThrows(Exception.class,()->m.read(changed,false,()->{}));assertEquals(0,m.status().getInt("entries"));assertEquals(2,m.status().getInt("fullReplays"));
    }}
    @Test public void warmChangeBetweenSnapshotsRefusesAndClearsEntries()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=PlaneAccuracy160Test.movement(f);var b=binding(f,t);var m=memo(f,true);m.read(b,false,()->{});Path p=f.root.resolve("alignment-041/frames/"+t.getString("id")+"/10.meta.json");String text=Files.readString(p);var count=new AtomicInteger();
        assertThrows(Exception.class,()->m.read(b,false,()->{if(count.incrementAndGet()==3)try{Files.writeString(p,new JSONObject(text).put("phase","changed").toString());}catch(Exception e){throw new IllegalStateException(e);}}));assertEquals(0,m.status().getInt("entries"));Files.writeString(p,text);m.read(b,false,()->{});
    }}
    @Test public void boundedEntriesEvictAndChangedCodeInventoryForcesReconstruction()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var m=memo(f,true);JSONObject first=null;for(int i=0;i<5;i++){var b=binding(f,PlaneAccuracy160Test.movement(f));if(first==null)first=b;m.read(b,false,()->{});}assertEquals(4,m.status().getInt("entries"));assertEquals(1,m.status().getInt("evictions"));m.read(first,false,()->{});assertEquals(6,m.status().getInt("fullReplays"));
        Path origin=Path.of(RecordedReplay163.class.getProtectionDomain().getCodeSource().getLocation().toURI());assertTrue(Files.isDirectory(origin));Path marker=origin.resolve("Recorded163InventoryTest.class");assertFalse(Files.exists(marker));try{Files.write(marker,new byte[]{1,2,3});m.read(first,false,()->{});assertEquals(7,m.status().getInt("fullReplays"));}finally{Files.deleteIfExists(marker);}
    }}
    @Test public void oldLabelsCalibrationAndMissingFramesCannotReuseProof()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=PlaneAccuracy160Test.movement(f);var b=binding(f,t);var m=memo(f,true);m.read(b,false,()->{});Path state=f.root.resolve("alignment-041/state.json");byte[] original=Files.readAllBytes(state);var e=new JSONObject(new String(original,java.nio.charset.StandardCharsets.UTF_8));var payload=e.getJSONObject("payload");payload.getJSONArray("trials").getJSONObject(0).getJSONObject("evaluation").getJSONObject("references").getJSONArray("frames").getJSONObject(0).put("visible",false);e.put("sha256",WorldFrames.hash(payload));Files.writeString(state,e.toString());assertThrows(Exception.class,()->m.read(b,false,()->{}));Files.write(state,original);m.read(b,false,()->{});
        Path calibration=f.root.resolve("alignment-041/frames/"+t.getString("registrationId")+"/0.image");byte[] bytes=Files.readAllBytes(calibration);Files.write(calibration,new byte[]{0});assertThrows(Exception.class,()->m.read(b,false,()->{}));Files.write(calibration,bytes);
        // A missing original is refused before any decoding or initialization.
        Path frame=f.root.resolve("alignment-041/frames/"+t.getString("id")+"/10.image"),backup=frame.resolveSibling("original.image");Files.move(frame,backup);try{assertThrows(Exception.class,()->m.read(b,false,()->{}));}finally{Files.move(backup,frame);}
    }}
}
