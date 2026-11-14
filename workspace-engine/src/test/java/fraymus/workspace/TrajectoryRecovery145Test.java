package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static fraymus.workspace.AppearanceConfirmation143Test.*;

public class TrajectoryRecovery145Test {
    static PixelTracker.Box box(int i){return new PixelTracker.Box(.15+.01*i,.2,.15,.2);}
    static JSONObject event(int i,boolean baseline,PixelTracker.Box b){
        var base=new JSONObject().put("box",baseline?b.json():JSONObject.NULL);
        return base.put("references143",new JSONObject().put("ordinal",i).put("baselineAccepted",baseline).put("recovery",new JSONObject().put("box",b.json())))
            .put("ambiguity144",new JSONObject().put("latched",!baseline).put("blocked",!baseline));
    }
    static JSONArray location(PixelTracker.Box b){return new JSONArray().put(new JSONObject().put("box",b.json()).put("sourceKey","verified-reference").put("score",.98));}
    static TrajectoryGate145 anchored(){var g=new TrajectoryGate145(box(0));for(int i=1;i<=5;i++)g.step(event(i,true,box(i)),new JSONArray());return g;}
    static PixelTracker.Box rival(){return new PixelTracker.Box(.7,.65,.15,.2);}
    @Test public void adjacentObservedPathsRecoverSeparatedReturn(){
        var g=anchored();assertFalse(g.step(event(6,false,box(6)),location(rival())).getBoolean("qualified"));
        var result=g.step(event(7,false,box(7)),location(rival()));assertTrue(result.toString(),result.getBoolean("qualified"));assertEquals(2,result.getJSONArray("rivals").getJSONObject(0).getInt("samples"));
    }
    @Test public void disappearingRivalIsRetainedAndDoesNotBecomeIdentityProof(){
        var g=anchored();g.step(event(6,false,box(6)),location(rival()));g.step(event(7,false,box(7)),location(rival()));
        var copy=g.step(event(8,false,rival()),location(rival()));assertFalse(copy.getBoolean("qualified"));assertFalse(copy.getBoolean("targetCompatible"));
        var blank=g.step(event(20,false,box(20)),new JSONArray());assertEquals(1,blank.getJSONArray("rivals").length());assertFalse(blank.getBoolean("qualified"));assertFalse(blank.getJSONObject("target").getBoolean("valid"));
    }
    @Test public void crossingPathsLatchConflictInsteadOfSwitchingIdentity(){
        var g=anchored();var near=new PixelTracker.Box(.27,.2,.15,.2);g.step(event(6,false,box(6)),location(near));
        var crossing=g.step(event(7,false,box(7)),location(box(7)));assertTrue(crossing.toString(),crossing.getBoolean("conflict"));assertFalse(crossing.getBoolean("qualified"));
        assertTrue(g.step(event(8,false,box(8)),location(rival())).getBoolean("conflict"));
    }
    @Test public void restoredCandidatesNeverExtendOriginalAnchorOrMotionHorizon(){
        var g=anchored();g.step(event(6,false,box(6)),location(rival()));g.step(event(7,false,box(7)),location(rival()));
        for(int i=8;i<20;i++){var r=g.step(event(i,false,box(i)),location(rival()));assertEquals(5,r.getJSONObject("target").getInt("anchorOrdinal"));if(i>=18)assertFalse(r.getBoolean("qualified"));}
        var reset=g.step(event(20,true,box(20)),new JSONArray());assertEquals(0,reset.getJSONArray("rivals").length());assertFalse(reset.getBoolean("conflict"));assertFalse(reset.getJSONObject("target").getBoolean("valid"));
    }
    @Test public void unobservedOrUncertainTargetMotionCannotAuthorizeRecovery(){
        var g=new TrajectoryGate145(box(0));g.step(event(1,false,box(1)),location(rival()));assertFalse(g.step(event(2,false,box(2)),location(rival())).getBoolean("qualified"));
        var fit=anchored();fit.step(event(6,false,box(6)),location(rival()));var jump=new PixelTracker.Box(.5,.2,.15,.2);assertFalse(fit.step(event(7,false,jump),location(rival())).getBoolean("qualified"));
    }
    @Test public void rivalCapacityOverflowRefusesInsteadOfDroppingEvidence(){
        var g=anchored();var many=new JSONArray();
        for(int i=0;i<9;i++)many.put(new JSONObject().put("box",new PixelTracker.Box(.05+(i%3)*.3,.48+(i/3)*.17,.06,.06).json()));
        var result=g.step(event(6,false,box(6)),many);assertTrue(result.getBoolean("conflict"));assertEquals(8,result.getJSONArray("rivals").length());assertFalse(result.getBoolean("qualified"));
    }
    static BufferedImage frame(int i,boolean copyOnly){
        boolean hidden=i>=10&&(copyOnly||i<=11);int texture=i<=6?143819:143031;
        var image=scene(32+2*i,48,80,texture,hidden,false,false);
        if(i>=7&&(i<=9||copyOnly)){var other=scene(234,148,68,143031,false,false,false);var graphics=image.createGraphics();graphics.drawImage(other.getSubimage(234,148,68,68),234,148,null);graphics.dispose();}
        return image;
    }
    @Test public void pixelRecoveryAddsMeasuredReturnWhileMonitorExactlyPreserves144(){
        var memory=AmbiguityGuard144Test.donor();var old=new AmbiguityGuard144("body","object",frame(0,false),seed(),memory,true);
        var monitor=new TrajectoryRecovery145("body","object",frame(0,false),seed(),memory,false);var candidate=new TrajectoryRecovery145("body","object",frame(0,false),seed(),memory,true);
        var oldFilter=new ObservationFilter(0,seed());var monitorFilter=new ObservationFilter(0,seed());int restored=0;var traces=new ArrayList<JSONObject>();
        for(int i=1;i<=18;i++){
            var a=old.update(frame(i,false));var b=monitor.update(frame(i,false));var c=candidate.update(frame(i,false));traces.add(ParsedJson137.copy(c));
            assertTrue(oldFilter.step(i,a).similar(monitorFilter.step(i,b)));b.remove("trajectory145");assertTrue(a.similar(b));
            if(c.getJSONObject("trajectory145").getBoolean("restored")){restored++;assertTrue(a.isNull("box"));assertTrue(PixelTracker.iou(PixelTracker.Box.from(c.getJSONArray("box")),new PixelTracker.Box((32+2*i)/320.,.2,.25,1./3))>=.5);}
            if(a.getJSONObject("references143").getBoolean("baselineAccepted")){c.remove("trajectory145");assertTrue(a.similar(c));}
        }
        assertTrue("Measured separated motion must add a real pixel-level recovery",restored>0);
        var replay=new TrajectoryRecovery145("body","object",frame(0,false),seed(),memory,true);for(int i=1;i<=18;i++)assertTrue(traces.get(i-1).similar(replay.update(frame(i,false))));
    }
    @Test public void original144CopyAndUnanchoredReturnRemainRefused(){
        for(boolean copy:new boolean[]{true,false}){
            var g=new TrajectoryRecovery145("body","object",AmbiguityGuard144Test.exposed(0,copy),seed(),AmbiguityGuard144Test.donor(),true);
            for(int i=1;i<24;i++)assertFalse(g.update(AmbiguityGuard144Test.exposed(i,copy)).getJSONObject("trajectory145").getBoolean("restored"));
        }
        var g=new TrajectoryRecovery145("body","object",frame(0,true),seed(),AmbiguityGuard144Test.donor(),true);
        for(int i=1;i<=18;i++){var t=g.update(frame(i,true));if(i>=10)assertFalse(t.toString(),t.getJSONObject("trajectory145").getBoolean("restored"));}
    }
    @Test public void auditReplaysTrajectoriesAndRejectsForgedEvidence()throws Exception{
        var memory=AmbiguityGuard144Test.donor();var tracker=new TrajectoryRecovery145("body","object",frame(0,false),seed(),memory,true);var trace=new JSONArray();var rows=new JSONArray();var filter=new ObservationFilter(0,seed());
        for(int i=1;i<=18;i++){var t=tracker.update(frame(i,false));trace.put(new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("bodyId","body").put("tracker",t).put("estimate",filter.step(i,t)))));var l=new JSONObject().put("ordinal",i).put("visibility",i==10||i==11?"occluded":"visible");if(i!=10&&i!=11)l.put("targetBox",new PixelTracker.Box((32+2*i)/320.,.2,.25,1./3).json());rows.put(l);}
        var data=new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","object").put("box",seed().json())));var run=new JSONObject().put("trackerPolicy","trajectory-guard-145").put("referenceMemory137",memory).put("trace",trace);var labels=new JSONObject().put("objectKey","subject").put("rows",rows);
        var audit=TrajectoryAudit145.check(run,data,labels,"body",i->frame(i,false));assertTrue(audit.toString(),audit.getJSONObject("restored").getInt("correct")>0);assertEquals(0,audit.getJSONObject("restored").getInt("incorrect"));
        var t=trace.getJSONObject(12).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker");t.getJSONObject("trajectory145").put("targetError",42);assertThrows(IllegalStateException.class,()->TrajectoryAudit145.check(run,data,labels,"body",i->frame(i,false)));
    }
    @Test public void runtimeSourceAndExportTamperChecksRemainRequired()throws Exception{
        try(var f=new VerifiedAppearances135Test.Fixture()){
            var set=f.service.retain(f.audit.getString("id"));int rev=f.observations.state(f.world).getJSONObject("estimate").getInt("revision");var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,rev).put("trackerPolicy","trajectory-guard-145").put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Separate generated source, no semantic identity claim");
            var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(args).getString("id"));var labels=f.association.save(AssociationAudit134Test.input(f.dataset));var audit=f.association.audit(new JSONObject().put("labelId",labels.getString("id")).put("runId",run.getString("id")));assertEquals(run.getInt("totalFrames"),audit.getJSONObject("trajectory145").getInt("replayedFrames"));
            var pixel=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/12.image");var original=java.nio.file.Files.readAllBytes(pixel);var bad=original.clone();bad[20]^=1;java.nio.file.Files.write(pixel,bad);assertThrows(IllegalStateException.class,()->f.association.readAudit(audit.getString("id")));assertThrows(IllegalStateException.class,()->f.observations.export(run.getString("id")));java.nio.file.Files.write(pixel,original);assertTrue(audit.similar(f.association.readAudit(audit.getString("id"))));
        }
    }
}
