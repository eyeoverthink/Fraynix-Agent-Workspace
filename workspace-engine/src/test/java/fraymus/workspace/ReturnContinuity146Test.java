package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static fraymus.workspace.AppearanceConfirmation143Test.*;

public class ReturnContinuity146Test {
    /** Exact already-exposed145 fixture geometry/textures; this is regression evidence, not a new source. */
    static BufferedImage exposed(int i,boolean copyOnly){
        boolean hidden=i>=10&&(copyOnly||i<=11);
        var image=scene(32+2*i,48,80,i>=7?145071:145989,hidden,false,false);
        if(i>=7&&(i<=9||copyOnly)){
            var other=scene(234,148,68,145071,false,false,false);var g=image.createGraphics();
            g.drawImage(other.getSubimage(234,148,68,68),234,148,null);g.dispose();
        }
        return image;
    }
    static JSONObject memory(){return AppearanceConfirmation143Test.memory(145071);}

    @Test public void copyWithoutCompetitorWarningIsRefusedAndMonitorPreservesParent()throws Exception{
        var old=new TrajectoryRecovery145("body","object",exposed(0,true),seed(),memory(),true);
        var monitor=new ReturnContinuity146("body","object",exposed(0,true),seed(),memory(),false);
        var candidate=new ReturnContinuity146("body","object",exposed(0,true),seed(),memory(),true);
        var aFilter=new ObservationFilter(0,seed());var bFilter=new ObservationFilter(0,seed());var cFilter=new ObservationFilter(0,seed());
        int oldWrong=0,newWrong=0,screened=0;var trace=new JSONArray();
        for(int i=1;i<24;i++){
            var a=old.update(exposed(i,true));var b=monitor.update(exposed(i,true));var c=candidate.update(exposed(i,true));
            assertTrue(aFilter.step(i,a).similar(bFilter.step(i,b)));b.remove("continuity146");assertTrue(a.similar(b));
            var estimate=cFilter.step(i,c);trace.put(new JSONObject().put("ordinal",i).put("tracker",c).put("estimate",estimate));
            if(i>=10&&!a.isNull("box")){oldWrong++;assertFalse(a.getJSONObject("ambiguity144").getBoolean("latched"));}
            if(i>=10&&!c.isNull("box"))newWrong++;
            if(c.getJSONObject("continuity146").getBoolean("blocked")){screened++;assertEquals(0,c.getJSONObject("continuity146").getInt("extraSearches"));}
            if(a.getJSONObject("references143").getBoolean("baselineAccepted")){var copy=ParsedJson137.copy(c);copy.remove("continuity146");assertTrue(copy.similar(a));}
        }
        assertTrue("The exposed regression must actually exercise unguarded wrong returns",oldWrong>0);
        assertTrue(screened>0);assertEquals(0,newWrong);
        Files.createDirectories(Path.of("build/return146-development"));Files.writeString(Path.of("build/return146-development/copy.json"),new JSONObject().put("oldWrongRaw",oldWrong).put("newWrongRaw",newWrong).put("trace",trace).toString(2));
    }

    @Test public void legitimatePathReturnsAreRetainedButNeverExtendTheAnchor()throws Exception{
        var candidate=new ReturnContinuity146("body","object",exposed(0,false),seed(),memory(),true);int kept=0,refusedCorrect=0;var trace=new JSONArray();
        for(int i=1;i<24;i++){
            var t=candidate.update(exposed(i,false));var m=t.getJSONObject("continuity146");trace.put(t);
            if(m.getBoolean("checkedWithoutWarning")){
                var target=t.getJSONObject("trajectory145").getJSONObject("target");assertEquals(6,target.getInt("anchorOrdinal"));
                if(m.getBoolean("accepted")){kept++;assertTrue(PixelTracker.iou(PixelTracker.Box.from(t.getJSONArray("box")),new PixelTracker.Box((32+2*i)/320.,.2,.25,1./3))>=.5);}
                if(m.getBoolean("blocked")&&i>=12)refusedCorrect++;
            }
        }
        assertTrue("Must keep a real return, not pass by refusing every box",kept>0);
        assertTrue("Expired legitimate returns remain an explicit cost",refusedCorrect>0);
        Files.createDirectories(Path.of("build/return146-development"));Files.writeString(Path.of("build/return146-development/return.json"),new JSONObject().put("kept",kept).put("refusedCorrect",refusedCorrect).put("trace",trace).toString(2));
    }

    @Test public void competed145RestorationsStayExactAndReplaySurvivesInterruption(){
        var memory=AmbiguityGuard144Test.donor();var old=new TrajectoryRecovery145("body","object",TrajectoryRecovery145Test.frame(0,false),seed(),memory,true);
        var candidate=new ReturnContinuity146("body","object",TrajectoryRecovery145Test.frame(0,false),seed(),memory,true);var trace=new JSONArray();int restored=0;
        for(int i=1;i<=18;i++){
            var a=old.update(TrajectoryRecovery145Test.frame(i,false));var b=candidate.update(TrajectoryRecovery145Test.frame(i,false));trace.put(ParsedJson137.copy(b));
            if(a.getJSONObject("trajectory145").getBoolean("restored")){restored++;b.remove("continuity146");assertTrue(a.similar(b));}
        }
        assertTrue(restored>0);var resumed=new ReturnContinuity146("body","object",TrajectoryRecovery145Test.frame(0,false),seed(),memory,true);
        for(int i=1;i<=18;i++)assertTrue(trace.getJSONObject(i-1).similar(resumed.update(TrajectoryRecovery145Test.frame(i,false))));
    }

    @Test public void originalPixelAuditSeparatesNewRefusalsAndRejectsForgedDecision()throws Exception{
        var tracker=new ReturnContinuity146("body","object",exposed(0,true),seed(),memory(),true);var trace=new JSONArray();var rows=new JSONArray();var filter=new ObservationFilter(0,seed());
        for(int i=1;i<24;i++){
            var t=tracker.update(exposed(i,true));trace.put(new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("bodyId","body").put("tracker",t).put("estimate",filter.step(i,t)))));
            var l=new JSONObject().put("ordinal",i).put("visibility",i>=10?"occluded":"visible");if(i<10)l.put("targetBox",new PixelTracker.Box((32+2*i)/320.,.2,.25,1./3).json());rows.put(l);
        }
        var dataset=new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","object").put("box",seed().json())));
        var run=new JSONObject().put("trackerPolicy","return-guard-146").put("referenceMemory137",memory()).put("trace",trace);var labels=new JSONObject().put("objectKey","subject").put("rows",rows);
        var audit=ReturnAudit146.check(run,dataset,labels,"body",i->exposed(i,true));assertTrue(audit.getJSONObject("screened").getInt("incorrect")>0);assertEquals(0,audit.getJSONObject("filtered").getInt("incorrect"));
        trace.getJSONObject(22).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("continuity146").put("blocked",false);
        assertThrows(IllegalStateException.class,()->ReturnAudit146.check(run,dataset,labels,"body",i->exposed(i,true)));
    }

    @Test public void runtimeStillVerifiesOriginalSourcesAndExport()throws Exception{
        try(var f=new VerifiedAppearances135Test.Fixture()){
            var set=f.service.retain(f.audit.getString("id"));int rev=f.observations.state(f.world).getJSONObject("estimate").getInt("revision");
            var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,rev).put("trackerPolicy","return-guard-146").put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Separate generated test source");
            var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(args).getString("id"));var labels=f.association.save(AssociationAudit134Test.input(f.dataset));
            var audit=f.association.audit(new JSONObject().put("labelId",labels.getString("id")).put("runId",run.getString("id")));assertEquals(run.getInt("totalFrames"),audit.getJSONObject("continuity146").getInt("replayedFrames"));
            var pixel=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/12.image");var original=Files.readAllBytes(pixel);var bad=original.clone();bad[20]^=1;Files.write(pixel,bad);
            try{assertThrows(IllegalStateException.class,()->f.association.readAudit(audit.getString("id")));assertThrows(IllegalStateException.class,()->f.observations.export(run.getString("id")));}finally{Files.write(pixel,original);}
            assertTrue(audit.similar(f.association.readAudit(audit.getString("id"))));
        }
    }
}
