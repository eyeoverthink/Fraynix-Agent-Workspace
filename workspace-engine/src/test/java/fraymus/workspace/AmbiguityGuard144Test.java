package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static fraymus.workspace.AppearanceConfirmation143Test.*;

public class AmbiguityGuard144Test {
    static BufferedImage exposed(int i,boolean twin){
        var image=scene(i==0?32:175+i%4,i==0?48:i<16?62:70,i==0?80:120,i==0?143819:143031,i>0&&(i<=4||i>=13&&i<=15),i>4&&i<20,false);
        if(twin&&i>4){var copy=scene(8,145,70,143031,false,false,false);var g=image.createGraphics();g.drawImage(copy.getSubimage(8,145,70,70),8,145,null);g.dispose();}return image;
    }
    static JSONObject donor(){
        var examples=new JSONArray();for(int i:new int[]{3,6,9,12,15,18}){
            var d=new JSONArray(PixelTracker.descriptor(PixelTracker.working(scene(32+i,48,80,143031,false,false,false)),new PixelTracker.Box((32+i)/320.,.2,.25,1./3)));
            examples.put(new JSONObject().put("descriptor",d).put("descriptorSha256",WorldFrames.hash(new JSONObject().put("values",d))).put("imageSha256","exposed143-development-"+i).put("ordinal",i));
        }return new JSONObject().put("examples",examples);
    }
    @Test public void exposedCopyOnlyCannotEraseEarlierCompetition(){
        var old=new AppearanceConfirmation143("body","object",exposed(0,true),seed(),donor(),true,true);
        var guard=new AmbiguityGuard144("body","object",exposed(0,true),seed(),donor(),true);int before=0,after=0,blocked=0;
        for(int i=1;i<24;i++){var a=old.update(exposed(i,true));var b=guard.update(exposed(i,true));var m=b.getJSONObject("ambiguity144");
            if(i>=13&&i<=15){if(a.getJSONObject("references143").getJSONObject("recovery").getBoolean("accepted"))before++;if(m.getBoolean("accepted"))after++;if(m.getBoolean("blocked"))blocked++;}
        }
        assertTrue("Reproduce the published copy-only failure",before>0);assertEquals(0,after);assertEquals(before,blocked);
    }
    @Test public void unambiguousLegitimateReturnPreservesAllParentOutputs(){
        var old=new AppearanceConfirmation143("body","object",exposed(0,false),seed(),donor(),true,true);var guard=new AmbiguityGuard144("body","object",exposed(0,false),seed(),donor(),true);int returned=0;
        for(int i=1;i<24;i++){var a=old.update(exposed(i,false));var b=guard.update(exposed(i,false));var m=b.getJSONObject("ambiguity144");assertFalse(m.toString(),m.getBoolean("blocked"));if(m.getBoolean("accepted"))returned++;b.remove("ambiguity144");assertTrue(a.similar(b));}assertTrue(returned>0);
    }
    @Test public void monitorIsExactly143IncludingFilterAndReplay(){
        var old=new AppearanceConfirmation143("body","object",exposed(0,true),seed(),donor(),true,true);var monitor=new AmbiguityGuard144("body","object",exposed(0,true),seed(),donor(),false);var a=new ObservationFilter(0,seed());var b=new ObservationFilter(0,seed());var traces=new ArrayList<JSONObject>();int would=0;
        for(int i=1;i<24;i++){var x=old.update(exposed(i,true));var y=monitor.update(exposed(i,true));traces.add(ParsedJson137.copy(y));if(y.getJSONObject("ambiguity144").getBoolean("wouldBlock"))would++;assertTrue(a.step(i,x).similar(b.step(i,y)));y.remove("ambiguity144");assertTrue(x.similar(y));}
        assertTrue(would>0);var restarted=new AmbiguityGuard144("body","object",exposed(0,true),seed(),donor(),false);for(int i=1;i<24;i++)assertTrue(traces.get(i-1).similar(restarted.update(exposed(i,true))));
    }
    static JSONObject event(int ordinal,boolean baseline,boolean competition,boolean returned){
        var base=new JSONObject().put("box",baseline?seed().json():JSONObject.NULL).put("status",baseline?"tracked":"lost");var out=ParsedJson137.copy(base);if(returned)out.put("box",seed().json());
        return out.put("references143",new JSONObject().put("baseline",base).put("baselineAccepted",baseline).put("lossObserved",!baseline).put("ordinal",ordinal).put("hypotheses",competition?new JSONArray().put(new JSONObject().put("unique",false).put("score",.97).put("rivalScore",.95).put("sourceKey","verified-key")):new JSONArray()).put("recovery",new JSONObject().put("accepted",returned)));
    }
    @Test public void ambiguityDoesNotExpireAndOnlyBaselineClearsIt(){
        var g=new AmbiguityGuard144("body","object",exposed(0,false),seed(),donor(),true);
        assertTrue(g.decide(event(1,false,true,false)).getJSONObject("ambiguity144").getBoolean("latched"));
        for(int i=2;i<=200;i++){var t=g.decide(event(i,false,false,i%2==0));assertTrue(t.isNull("box"));assertEquals(1,t.getJSONObject("ambiguity144").getInt("firstAmbiguousOrdinal"));}
        var restored=g.decide(event(201,true,false,false));assertTrue(restored.getJSONObject("ambiguity144").getBoolean("clearedByBaseline"));assertFalse(restored.isNull("box"));assertFalse(restored.getJSONObject("ambiguity144").getBoolean("latched"));
        assertTrue(g.decide(event(202,false,false,true)).getJSONObject("ambiguity144").getBoolean("accepted"));
    }
    @Test public void auditReplaysThenScoresBlockedBenefitsAndCostsWithoutGuidingTracker()throws Exception{
        var images=new ArrayList<BufferedImage>();images.add(exposed(0,true));var g=new AmbiguityGuard144("body","object",images.get(0),seed(),donor(),true);var f=new ObservationFilter(0,seed());var trace=new JSONArray();var rows=new JSONArray();
        for(int i=1;i<24;i++){images.add(exposed(i,true));var t=g.update(images.get(i));trace.put(new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("bodyId","body").put("tracker",t).put("estimate",f.step(i,t)))));rows.put(new JSONObject().put("ordinal",i).put("visibility","occluded"));}
        var data=new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","object").put("box",seed().json())));var run=new JSONObject().put("trackerPolicy","ambiguity-guard-144").put("referenceMemory137",donor()).put("trace",trace);var labels=new JSONObject().put("objectKey","subject").put("rows",rows);
        var a=AmbiguityAudit144.check(run,data,labels,"body",images::get);assertTrue(a.toString(),a.getJSONObject("blocked").getInt("incorrect")>0);assertEquals(0,a.getJSONObject("additional").getInt("returned"));int ordinal=a.getJSONArray("scores").getJSONObject(0).getInt("ordinal");
        String original=WorldFrames.hash(run);rows.getJSONObject(ordinal-1).put("visibility","visible").put("targetBox",trace.getJSONObject(ordinal-1).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("references143").getJSONObject("recovery").getJSONArray("box"));assertEquals(1,AmbiguityAudit144.check(run,data,labels,"body",images::get).getJSONObject("blocked").getInt("correct"));assertEquals(original,WorldFrames.hash(run));
        trace.getJSONObject(ordinal-1).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("ambiguity144").put("latched",false);assertThrows(IllegalStateException.class,()->AmbiguityAudit144.check(run,data,labels,"body",images::get));
    }
    @Test public void warningOnlyAndAcceptedBaselineRemainUnchanged(){
        var first=ForegroundGuard139Test.scene(0,false,false,false);var s=ForegroundGuard139Test.seed();var g=new AmbiguityGuard144("body","object",first,s,memory(923),true);var old=new AppearanceConfirmation143("body","object",first,s,memory(923),true,true);
        for(int i=0;i<5;i++){var im=ForegroundGuard139Test.scene(0,true,false,false);var a=g.update(im);assertFalse(a.getJSONObject("ambiguity144").getBoolean("latched"));a.remove("ambiguity144");assertTrue(a.similar(old.update(im)));}assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
    }
    @Test public void provenanceAndTamperChecksStillReachNewAudit()throws Exception{
        try(var f=new VerifiedAppearances135Test.Fixture()){
            var set=f.service.retain(f.audit.getString("id"));int rev=f.observations.state(f.world).getJSONObject("estimate").getInt("revision");var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,rev).put("trackerPolicy","ambiguity-guard-144").put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Separate generated fixtures, no semantic identity claim");
            var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(args).getString("id"));assertEquals("completed",run.getString("status"));var labels=f.association.save(AssociationAudit134Test.input(f.dataset));var audit=f.association.audit(new JSONObject().put("labelId",labels.getString("id")).put("runId",run.getString("id")));assertEquals(run.getInt("totalFrames"),audit.getJSONObject("ambiguity144").getInt("replayedFrames"));
            var pixel=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/12.image");byte[] original=java.nio.file.Files.readAllBytes(pixel),bad=original.clone();bad[20]^=1;java.nio.file.Files.write(pixel,bad);assertThrows(IllegalStateException.class,()->f.association.readAudit(audit.getString("id")));assertThrows(IllegalStateException.class,()->f.observations.export(run.getString("id")));java.nio.file.Files.write(pixel,original);assertTrue(audit.similar(f.association.readAudit(audit.getString("id"))));
        }
    }
}
