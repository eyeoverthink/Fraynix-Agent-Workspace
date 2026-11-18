package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class PlaneAccuracy160Test {
    static JSONObject movement(Alignment041Test.Fixture f)throws Exception{
        var t=f.start("movement");for(int n=1;n<=8;n++)f.upload(t,n,.4,false,"start");Alignment041Test.mark(f,t,8,"transition");
        for(int n=9;n<=12;n++)f.upload(t,n,.4+(n-8)*.1333333333333333/4,false,"transition");Alignment041Test.mark(f,t,12,"end");for(int n=13;n<=20;n++)f.upload(t,n,.5333333333333333,false,"end");
        f.store.finish(t.getString("id"),false);var run=f.store.trial(t.getString("id"));f.store.evaluate(new JSONObject().put("id",t.getString("id")).put("references",Alignment041Test.references(run)));return f.store.trial(t.getString("id"));
    }
    static JSONObject reconstructed(Alignment041Test.Fixture f,JSONObject t)throws Exception{var r=new PlaneReplay160(f.root,f.store,f.ledger);return r.replay(r.binding(t.getString("id")),()->{});}
    static JSONObject input(JSONObject t){return new JSONObject().put("trialIds",new JSONArray().put(t.getString("id"))).put("options",new JSONObject());}
    static JSONObject waitFor(PlaneAccuracyStore160 store,String id)throws Exception{long end=System.nanoTime()+90_000_000_000L;while(System.nanoTime()<end){var j=store.read(id);if(!j.getString("status").equals("running"))return j;Thread.sleep(20);}throw new AssertionError("Evaluation timeout");}
    /** Typed receipt fixture only; separate app study reads the actual Chromium DOM. */
    static JSONObject receipt(JSONObject job){
        var d=PlaneAccuracyReport160.data(job);var out=new JSONObject().put("rows",d.getJSONArray("cells"));for(String k:List.of("limits","source","summary"))out.put(k,d.getString(k));var states=new JSONArray();
        for(String mode:List.of("all","error","bound")){var bars=new JSONArray();var rows=d.getJSONArray("rows");double width=720./rows.length();for(int i=0;i<rows.length();i++){var r=rows.getJSONObject(i);if(!r.getBoolean("usable"))continue;for(String k:List.of("error","bound")){if(!mode.equals("all")&&!mode.equals(k))continue;double h=280*r.getDouble(k.equals("error")?"errorM":"radiusM")/d.getDouble("scaleM");bars.put(new JSONObject().put("kind",k).put("x",(double)(float)(40+i*width+(k.equals("error")?0:width*.45))).put("y",(double)(float)(300-h)).put("width",(double)(float)(width*.35)).put("height",(double)(float)h));}}states.put(new JSONObject().put("mode",mode).put("visible",true).put("bars",bars));}return out.put("states",states);
    }
    @Test public void freshSourceReplayAndIndependentTapeDisplacementStaySeparate()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=movement(f);var r=reconstructed(f,t);var e=PlaneAccuracy160.evaluate(r,PlaneAccuracy160.options(new JSONObject()));assertTrue(e.toString(),e.getBoolean("measurementGatesPassed"));assertEquals(8,e.getInt("referenceRegionCovered"));assertEquals(1,e.getInt("independentEpisodes"));assertFalse(e.getBoolean("probabilityCalibrated"));assertFalse(e.getBoolean("actionAuthorized"));assertEquals(.05,e.getJSONArray("expectedDeltaXZ").getDouble(0),1e-10);assertEquals(f.observed.toString(),f.ledger.observe(f.world.getString("id")).toString());
    }}
    @Test public void boundsMatchRecordedReferenceAndGrowWithoutFrameCountDiscount()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var source=new CalibratedPlane159(f.root,f.store,f.ledger).source(f.reg.getString("id"));var o=PlaneAccuracy160.options(new JSONObject());var point=f.reg.getJSONObject("fit").getJSONArray("landmarks").getJSONObject(4);var old=CalibratedPlane159.bounds(source,point.getString("name"),2,.003);var region=PlaneAccuracy160.region(source,point.getJSONArray("image"),o);assertEquals(old.getJSONArray("minimumM").getDouble(0),region.getJSONArray("min").getDouble(0),1e-12);assertEquals(old.getJSONArray("maximumM").getDouble(2),region.getJSONArray("max").getDouble(1),1e-12);var wide=PlaneAccuracy160.region(source,point.getJSONArray("image"),o.put("pixelRadius",4));assertTrue(wide.getJSONArray("min").getDouble(0)<region.getJSONArray("min").getDouble(0));assertThrows(IllegalArgumentException.class,()->PlaneAccuracy160.region(source,new JSONArray("[0,0]"),o));
    }}
    @Test public void missingMeasurementsStayInDenominatorAndStrictWidthCanFailValidReport()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var r=reconstructed(f,movement(f));var o=PlaneAccuracy160.options(new JSONObject().put("maxRadiusM",.001));var e=PlaneAccuracy160.evaluate(r,o);assertFalse(e.getBoolean("measurementGatesPassed"));assertEquals(8,e.getInt("usablePairs"));var t=r.getJSONObject("trial");MatTrials041.window(t,"end").getJSONObject(2).getJSONObject("tracker").put("accepted",false);e=PlaneAccuracy160.evaluate(r,PlaneAccuracy160.options(new JSONObject()));assertEquals(7,e.getInt("usablePairs"));assertEquals(8,e.getInt("pairedSamples"));assertFalse(e.getJSONObject("aggregate").getBoolean("usable"));assertFalse(e.getBoolean("measurementGatesPassed"));for(Object raw:MatTrials041.window(t,"end"))((JSONObject)raw).getJSONObject("tracker").put("accepted",false);e=PlaneAccuracy160.evaluate(r,PlaneAccuracy160.options(new JSONObject()));assertEquals(0,e.getInt("usablePairs"));assertTrue(e.isNull("maxErrorM"));assertTrue(e.isNull("maxRadiusM"));
    }}
    @Test public void changedReferenceCannotBeUsedToJudgeTheOriginalPrediction()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var r=reconstructed(f,movement(f));var t=r.getJSONObject("trial");t.getJSONObject("evaluation").getJSONObject("references").getJSONArray("frames").getJSONObject(0).put("imageSha256","0".repeat(64));assertThrows(IllegalArgumentException.class,()->PlaneAccuracy160.evaluate(r,PlaneAccuracy160.options(new JSONObject())));
    }}
    @Test public void sourceBytesAndFrameMetadataAreRecheckedIncludingTransitions()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=movement(f);var replay=new PlaneReplay160(f.root,f.store,f.ledger);var bind=replay.binding(t.getString("id"));var frame=f.root.resolve("alignment-041/frames/"+t.getString("id")+"/10.image");var b=Files.readAllBytes(frame);Files.write(frame,new byte[]{1,2,3});assertThrows(IllegalStateException.class,()->replay.replay(bind,()->{}));Files.write(frame,b);var meta=frame.resolveSibling("10.meta.json");var original=Files.readString(meta);Files.writeString(meta,new JSONObject(original).put("seconds",9999).toString());assertThrows(IllegalStateException.class,()->replay.replay(bind,()->{}));
    }}
    @Test public void changedRecordedTrackerCannotMasqueradeAsAReconstruction()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=movement(f);Path path=f.root.resolve("alignment-041/state.json");var env=new JSONObject(Files.readString(path));var payload=env.getJSONObject("payload");payload.getJSONArray("trials").getJSONObject(0).getJSONArray("trace").getJSONObject(0).getJSONObject("tracker").getJSONArray("point").put(0,.2);env.put("sha256",WorldFrames.hash(payload));Files.writeString(path,env.toString());var replay=new PlaneReplay160(f.root,f.store,f.ledger);assertThrows(IllegalStateException.class,()->replay.replay(replay.binding(t.getString("id")),()->{}));
    }}
    @Test public void durableJobChecksActualReportAndRetainsResultsAcrossRestart()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=movement(f);var artifacts=new WorkspaceStore(f.root);String id;
        try(var store=new PlaneAccuracyStore160(f.root,f.store,f.ledger,artifacts)){var j=store.start(input(t));id=j.getString("id");j=waitFor(store,id);assertEquals(j.toString(),"awaiting-report",j.getString("status"));var effect=store.prepare(id);var actual=receipt(j);var bad=WorldFrames.copy(actual);bad.getJSONArray("rows").getJSONArray(0).put(3,"999");assertThrows(IllegalStateException.class,()->store.acknowledge(new JSONObject().put("id",id).put("resultSha256",effect.getString("resultSha256")).put("observed",bad)));j=store.acknowledge(new JSONObject().put("id",id).put("resultSha256",effect.getString("resultSha256")).put("observed",actual));assertEquals("completed",j.getString("status"));assertTrue(store.verify(id).getBoolean("passed"));}
        try(var store=new PlaneAccuracyStore160(f.root,f.store,f.ledger,artifacts)){assertTrue(store.verify(id).getBoolean("passed"));var j=store.read(id);var a=artifacts.read(j.getJSONObject("report").getString("id"));artifacts.write(new JSONObject().put("id",a.getString("id")).put("expectedVersion",a.getInt("version")).put("title",a.getString("title")).put("entrypoint","index.html").put("files",a.getJSONObject("contents")));assertThrows(IllegalStateException.class,()->store.verify(id));}
    }}
    @Test public void interruptedWorkResumesFrozenSourcesWithoutDuplicatingEpisodes()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=movement(f);var artifacts=new WorkspaceStore(f.root);String id;try(var store=new PlaneAccuracyStore160(f.root,f.store,f.ledger,artifacts)){assertThrows(IllegalArgumentException.class,()->store.start(input(t).put("trialIds",new JSONArray().put(t.getString("id")).put(t.getString("id")))));var j=store.start(input(t));id=j.getString("id");store.stop(id);assertEquals("interrupted",store.read(id).getString("status"));}
        try(var store=new PlaneAccuracyStore160(f.root,f.store,f.ledger,artifacts)){store.resume(id);var j=waitFor(store,id);assertEquals(j.toString(),"awaiting-report",j.getString("status"));assertEquals(1,j.getInt("resumes"));assertEquals(1,j.getJSONArray("results").length());assertEquals(1,store.state().getJSONArray("jobs").length());}
    }}
    @Test public void copiedFootageUnderDifferentIdsDoesNotIncreaseEpisodeCount()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var first=movement(f);var second=movement(f);try(var store=new PlaneAccuracyStore160(f.root,f.store,f.ledger,new WorkspaceStore(f.root))){assertThrows(IllegalArgumentException.class,()->store.start(input(first).put("trialIds",new JSONArray().put(first.getString("id")).put(second.getString("id")))));assertEquals(0,store.state().getJSONArray("jobs").length());}
    }}
    @Test public void reportBytesAreStableAcrossNumericPersistence()throws Exception{try(var f=new Alignment041Test.Fixture()){
        var t=movement(f);var o=PlaneAccuracy160.options(new JSONObject());var result=PlaneAccuracy160.evaluate(reconstructed(f,t),o);
        result.getJSONArray("expectedDeltaXZ").put(0,8.406046182417556E-4);
        var j=new JSONObject().put("id",UUID.randomUUID().toString()).put("options",o).put("bindings",new JSONArray().put(new PlaneReplay160(f.root,f.store,f.ledger).binding(t.getString("id")))).put("results",new JSONArray().put(result));
        var first=PlaneAccuracyReport160.build(j);assertTrue(first.similar(PlaneAccuracyReport160.build(WorldFrames.copy(j))));
        var artifacts=new WorkspaceStore(f.root);var a=artifacts.writeOnce("stable160",first);assertEquals(a.getString("id"),artifacts.writeOnce("stable160",PlaneAccuracyReport160.build(WorldFrames.copy(j))).getString("id"));
    }}
    @Test public void nonfiniteAndUnknownBudgetInputsAreRefused(){assertThrows(Exception.class,()->PlaneAccuracy160.options(new JSONObject().put("pixelRadius","NaN")));assertThrows(Exception.class,()->PlaneAccuracy160.options(new JSONObject().put("maxRadiusM",0)));assertThrows(Exception.class,()->PlaneAccuracy160.options(new JSONObject().put("trustPredictions",true)));}
}
