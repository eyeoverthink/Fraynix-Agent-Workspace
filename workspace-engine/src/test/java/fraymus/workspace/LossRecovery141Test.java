package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LossRecovery141Test {
    @Test public void sourceReplayChecksReturnedAndFilteredOutcomesAndRejectsForgery()throws Exception{
        var images=new ArrayList<BufferedImage>();images.add(scene(32,50,80,false,false));var tracker=new LossRecovery141("body","source",images.get(0),seed(),true);var filter=new ObservationFilter(0,seed());
        var trace=new JSONArray();var rows=new JSONArray();
        for(int i=1;i<=8;i++){
            images.add(scene(180,60,120,i<=2,false));var t=tracker.update(images.get(i));trace.put(new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("bodyId","body").put("tracker",t).put("estimate",filter.step(i,t)))));
            var l=new JSONObject().put("ordinal",i).put("visibility",i<=2?"occluded":"visible");if(i>2)l.put("targetBox",new PixelTracker.Box(180/320.,60/240.,120/320.,120/240.).json());rows.put(l);
        }
        var dataset=new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","source").put("box",seed().json())));
        var run=new JSONObject().put("trackerPolicy","recovery-guard-141").put("trace",trace);var labels=new JSONObject().put("objectKey","subject").put("rows",rows);
        var audit=LossRecoveryAudit141.check(run,dataset,labels,"body",images::get);assertTrue(audit.toString(),audit.getJSONObject("additional").getInt("correct")>0);assertEquals(0,audit.getJSONObject("additional").getInt("incorrect"));assertTrue(audit.getJSONObject("filtered").getInt("correct")>0);
        String hash=WorldFrames.hash(run);int n=audit.getJSONArray("scores").getJSONObject(0).getInt("ordinal");rows.getJSONObject(n-1).put("visibility","occluded").remove("targetBox");
        assertEquals(1,LossRecoveryAudit141.check(run,dataset,labels,"body",images::get).getJSONObject("additional").getInt("incorrect"));assertEquals(hash,WorldFrames.hash(run));
        var fake=ParsedJson137.copy(run);fake.getJSONArray("trace").getJSONObject(n-1).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("recovery141").getJSONObject("recovery").put("confirmations",99);
        assertThrows(IllegalStateException.class,()->LossRecoveryAudit141.check(fake,dataset,labels,"body",images::get));
    }
    @Test public void warningWithoutActualFlowLossCannotUseSupplementalRecovery(){
        var first=ForegroundGuard139Test.scene(0,false,false,false);var seed=ForegroundGuard139Test.seed();var g=new LossRecovery141("body","object",first,seed,true);int warnings=0;
        for(int i=0;i<5;i++){
            var t=g.update(ForegroundGuard139Test.scene(0,true,false,false));var m=t.getJSONObject("recovery141");
            if(t.getJSONObject("drift139").getBoolean("suspect")&&!m.getBoolean("lossObserved")){warnings++;assertFalse(m.getJSONObject("recovery").getBoolean("attempted"));assertTrue(t.isNull("box"));}
        }
        assertTrue(warnings>0);assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
    }
    static PixelTracker.Box seed(){return PixelTracker.Box.from(new PixelTracker.Box(32/320.,50/240.,80/320.,80/240.).json());}
    static BufferedImage scene(int x,int y,int size,boolean hidden,boolean twin){
        var image=new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(35,40,45));g.fillRect(0,0,320,240);
        var pattern=new BufferedImage(24,24,BufferedImage.TYPE_INT_RGB);var random=new Random(141);
        for(int j=0;j<24;j++)for(int i=0;i<24;i++){int v=random.nextInt(130);pattern.setRGB(i,j,((35+v)<<16)|((95+v)<<8)|(20+v/2));}
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if(!hidden){g.drawImage(pattern,x,y,size,size,null);if(twin)g.drawImage(pattern,12,145,70,70,null);}g.dispose();return image;
    }
    @Test public void refinedSearchFindsScaledTranslatedSourceAndRejectsAbsentTexture(){
        var search=new RecoverySearch141(scene(32,50,80,false,false),seed());
        var r=search.search(PixelTracker.working(scene(180,60,120,false,false)));
        assertTrue(r.json().toString(),r.unique());assertTrue(PixelTracker.iou(r.box(),new PixelTracker.Box(180/320.,60/240.,120/320.,120/240.))>.8);
        assertFalse(search.search(PixelTracker.working(scene(0,0,80,true,false))).unique());
    }
    @Test public void originalSourceRivalPreventsSelection(){
        var r=new RecoverySearch141(scene(32,50,80,false,false),seed()).search(PixelTracker.working(scene(180,60,120,false,true)));
        assertFalse(r.json().toString(),r.unique());
    }
    @Test public void sourceSearchDoesNotQualifyAUniformPatch(){
        var image=new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);assertFalse(new RecoverySearch141(image,seed()).search(PixelTracker.working(image)).unique());
    }
    @Test public void lostScaledReturnNeedsRepeatedSourceAndMotionAndPreservesBaseline(){
        var first=scene(32,50,80,false,false);var guard=new LossRecovery141("body","source",first,seed(),true);var base=new ForegroundGuard139("body","source",first,seed(),true);int added=0;
        for(int i=1;i<=8;i++){
            var image=scene(180,60,120,i<=2,false);var t=guard.update(image);var b=base.update(image);var m=t.getJSONObject("recovery141");
            if(!b.isNull("box")){var plain=ParsedJson137.copy(t);plain.remove("recovery141");assertTrue(plain.similar(b));}
            if(i<5)assertFalse(m.toString(),m.getJSONObject("recovery").getBoolean("accepted"));
            if(m.getJSONObject("recovery").getBoolean("accepted")){added++;assertEquals(3,m.getJSONObject("recovery").getInt("confirmations"));assertTrue(b.isNull("box"));assertTrue(PixelTracker.iou(PixelTracker.Box.from(t.getJSONArray("box")),new PixelTracker.Box(180/320.,60/240.,120/320.,120/240.))>.8);}
            assertEquals(0,m.getInt("automaticAdmissions"));
        }
        assertTrue("Scaled return should add source-verified measurements",added>0);
    }
    @Test public void monitorPreservesEveryIncumbentOutputAndDimensionsAreChecked(){
        var first=scene(32,50,80,false,false);var monitor=new LossRecovery141("body","source",first,seed(),false);var base=new ForegroundGuard139("body","source",first,seed(),true);
        for(int i=1;i<=6;i++){var im=scene(180,60,120,i<=2,false);var t=monitor.update(im);t.remove("recovery141");assertTrue(t.similar(base.update(im)));}
        assertThrows(IllegalArgumentException.class,()->monitor.update(new BufferedImage(321,240,BufferedImage.TYPE_INT_RGB)));
    }
    @Test public void interruptedConfirmationAndCompetingReturnAddNoMeasurements(){
        var g=new LossRecovery141("body","source",scene(32,50,80,false,false),seed(),true);
        var base=new ForegroundGuard139("body","source",scene(32,50,80,false,false),seed(),true);
        for(int i=1;i<=8;i++){
            var image=scene(180,60,120,i<=2||i==5,i>=6);var t=g.update(image);var b=base.update(image);var m=t.getJSONObject("recovery141");
            assertFalse(m.toString(),m.getJSONObject("recovery").getBoolean("accepted"));
            if(i==4)assertEquals(2,m.getJSONObject("recovery").getInt("confirmations"));
            if(i==5)assertEquals(0,m.getJSONObject("recovery").getInt("confirmations"));
            // The inherited139 tracker can itself select this copy; supplemental recovery must not add to it.
            t.remove("recovery141");assertTrue(t.similar(b));
        }
    }
 @Test public void durableStoppedPrefixAndAuditPixelReplaySurviveRestartAndRejectChangedEvidence()throws Exception{
  Path root=Files.createTempDirectory("recovery141-");var ledger=new WorldLedger(root);String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"Drift control").getString("id");
  JSONObject run,audit,label,dataset;byte[] state;
  try(var s=new WorldObservationStore(root,ledger)){
   var pack=WorldObservationStoreTest.pack();dataset=WorldObservationStoreTest.importPack(s,pack);
   String id=s.start(WorldObservationStoreTest.arguments(ledger,world,dataset,0).put("trackerPolicy","recovery-guard-141")).getString("id");s.stop(id);var prefix=s.read(id).getJSONArray("trace");s.resume(id);run=WorldObservationStoreTest.settled(s,id);assertEquals(run.toString(),"completed",run.getString("status"));
   for(int i=0;i<prefix.length();i++)assertTrue(prefix.getJSONObject(i).similar(run.getJSONArray("trace").getJSONObject(i)));
   var rows=new JSONArray();for(int i=1;i<9;i++){var f=dataset.getJSONArray("frames").getJSONObject(i);rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256")).put("visibility","visible").put("targetBox",pack.getJSONArray("objects").getJSONObject(0).getJSONArray("box")).put("note","Independent static fixture geometry"));}
   var input=new JSONObject().put("schema",AssociationAudit134.VERSION).put("datasetId",dataset.getString("id")).put("datasetSha256",WorldFrames.hash(dataset)).put("objectKey","subject").put("annotator","Generated independent geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Fixed placements, not predictions").put("rows",rows);
   var service=new AssociationAudit134(root,s);label=service.save(input);audit=service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",id));assertEquals(8,audit.getJSONObject("recovery141").getInt("replayedFrames"));
   var fake=WorldFrames.copy(run);fake.getJSONArray("trace").getJSONObject(0).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("recovery141").put("anchorSha256","forged");
   String body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");
   assertThrows(IllegalStateException.class,()->LossRecoveryAudit141.check(fake,dataset,label.getJSONObject("input"),body,i->ImageIO.read(root.resolve("world-observations/datasets/"+dataset.getString("id")+"/"+i+".image").toFile())));
   state=Files.readAllBytes(root.resolve("world-observations/state.json"));
  }
  try(var s=new WorldObservationStore(root,new WorldLedger(root))){assertArrayEquals(state,Files.readAllBytes(root.resolve("world-observations/state.json")));assertTrue(run.similar(s.read(run.getString("id"))));var service=new AssociationAudit134(root,s);assertTrue(audit.similar(service.readAudit(audit.getString("id"))));
   Path file=root.resolve("world-observations/datasets/"+dataset.getString("id")+"/4.image");byte[] original=Files.readAllBytes(file),bad=original.clone();bad[20]^=1;Files.write(file,bad);assertThrows(IllegalStateException.class,()->service.readAudit(audit.getString("id")));Files.write(file,original);assertTrue(audit.similar(service.readAudit(audit.getString("id"))));}
 }

}
