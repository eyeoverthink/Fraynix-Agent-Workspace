package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppearanceConfirmation143Test {
    static PixelTracker.Box seed(){return PixelTracker.Box.from(new PixelTracker.Box(.1,.2,.25,1./3).json());}
    static BufferedImage scene(int x,int y,int size,int texture,boolean hidden,boolean partial,boolean twin){
        var out=new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);var g=out.createGraphics();g.setColor(new Color(35,40,45));g.fillRect(0,0,320,240);
        var tile=new BufferedImage(24,24,BufferedImage.TYPE_INT_RGB);var random=new Random(texture);for(int j=0;j<24;j++)for(int i=0;i<24;i++){int v=random.nextInt(160);tile.setRGB(i,j,((30+v)<<16)|((70+v)<<8)|(20+v/2));}
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);if(!hidden){g.drawImage(tile,x,y,size,size,null);if(partial){g.setColor(new Color(190,70,60));g.fillRect(x+size*3/4,y,size/4,size);}if(twin)g.drawImage(tile,8,145,70,70,null);}g.dispose();return out;
    }
    static JSONObject memory(int texture){var d=new JSONArray(PixelTracker.descriptor(PixelTracker.working(scene(32,48,80,texture,false,false,false)),seed()));return new JSONObject().put("examples",new JSONArray().put(new JSONObject().put("descriptor",d).put("descriptorSha256",WorldFrames.hash(new JSONObject().put("values",d))).put("imageSha256","generated-fixture-not-a-production-proof").put("ordinal",1)));}
    static ReferenceSearch143.Result strongest(ReferenceSearch143.Batch b){return b.candidates().stream().max(Comparator.comparingDouble(ReferenceSearch143.Result::score)).orElseThrow();}
    static JSONObject multiple(){
        var refs=new JSONArray();
        for(int ordinal:new int[]{3,6}){
            int x=32+ordinal;var box=PixelTracker.Box.from(new PixelTracker.Box(x/320.,.2,.25,1./3).json());
            var d=new JSONArray(PixelTracker.descriptor(PixelTracker.working(scene(x,48,80,142577,false,false,false)),box));
            refs.put(new JSONObject().put("descriptor",d).put("descriptorSha256",WorldFrames.hash(new JSONObject().put("values",d))).put("imageSha256","exposed142-rendered-source-"+ordinal).put("ordinal",ordinal));
        }
        return new JSONObject().put("examples",refs);
    }
    @Test public void alternatingBestReferencesPreserveTheirOwnMotionConfirmation(){
        var first=scene(32,48,80,142243,false,false,false);var memory=multiple();
        var previous=new PartialRecovery142("body","object",first,seed(),memory,true,true);
        var tracker=new AppearanceConfirmation143("body","object",first,seed(),memory,true,true);
        int oldAdded=0,added=0;var keys=new HashSet<String>();
        for(int i=1;i<24;i++){
            var im=scene(180+i%3,60,120,142577,i<=4,i<17,false);
            var before=previous.update(im).getJSONObject("partial142").getJSONObject("recovery");
            if(before.getBoolean("accepted"))oldAdded++;if(before.optBoolean("unique"))keys.add(before.getString("sourceKey"));
            var result=tracker.update(im);var trace=result.getJSONObject("references143");
            assertTrue(trace.getJSONArray("hypotheses").length()<=3);
            if(trace.getJSONObject("recovery").getBoolean("accepted")){
                added++;assertTrue(i>=7);var chosen=trace.getJSONObject("recovery");assertEquals(3,chosen.getInt("confirmations"));assertTrue(chosen.getBoolean("coherentMotion"));
                assertTrue(PixelTracker.iou(PixelTracker.Box.from(result.getJSONArray("box")),new PixelTracker.Box((180+i%3)/320.,.25,.375,.5))>.8);
            }
        }
        assertEquals("Reproduce exposed142 failure",0,oldAdded);assertTrue(keys.size()>1);assertTrue("Independent reference lanes must recover",added>0);
    }
    @Test public void noPoolingAcrossReferencesAndNoContinuationAcrossCoverOrCopy(){
        var first=scene(32,48,80,142243,false,false,false);var t=new AppearanceConfirmation143("body","object",first,seed(),multiple(),true,true);
        for(int i=1;i<=12;i++){
            boolean hidden=i<=2||i==5;var r=t.update(scene(180+i%3,60,120,142577,hidden,true,i>=6));
            assertFalse(r.toString(),r.getJSONObject("references143").getJSONObject("recovery").getBoolean("accepted"));
        }
        var m=multiple();m.getJSONArray("examples").put(ParsedJson137.copy(m.getJSONArray("examples").getJSONObject(0)));
        assertThrows(IllegalArgumentException.class,()->new ReferenceSearch143(first,seed(),m,true));
    }
    @Test public void partialReferenceSearchFindsVerifiedAlternateAndRejectsUniformAndTwin(){
        var first=scene(32,48,80,142,false,false,false);var search=new ReferenceSearch143(first,seed(),memory(923),true);
        var r=strongest(search.search(scene(180,60,120,923,false,true,false)));assertTrue(r.json().toString(),r.unique());assertTrue(r.json().toString(),PixelTracker.iou(r.box(),new PixelTracker.Box(180/320.,.25,.375,.5))>.8);assertTrue(r.mask()!=0);assertEquals(.75,r.json().getDouble("visibleFraction"),0);
        assertFalse(strongest(search.search(scene(180,60,120,923,true,false,false))).unique());assertFalse(strongest(search.search(scene(180,60,120,923,false,true,true))).unique());
        assertFalse(strongest(new ReferenceSearch143(first,seed(),memory(923),false).search(scene(180,60,120,923,false,true,false))).unique());
    }
    @Test public void threeMotionConfirmedPartialViewsRecoverWithoutTrainingOrChangingBaseline(){
        var first=scene(32,48,80,142,false,false,false);var guard=new AppearanceConfirmation143("body","source",first,seed(),memory(923),true,true);var base=new ForegroundGuard139("body","source",first,seed(),true);int added=0;
        for(int i=1;i<=7;i++){var im=scene(180,60,120,923,i<=2,true,false);var t=guard.update(im);var b=base.update(im);var m=t.getJSONObject("references143");if(!b.isNull("box")){var copy=ParsedJson137.copy(t);copy.remove("references143");assertTrue(copy.similar(b));}if(i<5)assertFalse(m.getJSONObject("recovery").getBoolean("accepted"));if(m.getJSONObject("recovery").getBoolean("accepted")){added++;assertEquals(3,m.getJSONObject("recovery").getInt("confirmations"));assertTrue(PixelTracker.iou(PixelTracker.Box.from(t.getJSONArray("box")),new PixelTracker.Box(180/320.,.25,.375,.5))>.8);}assertEquals(0,m.getInt("automaticAdmissions"));}
        assertTrue("Alternate partial return should have a measured positive path",added>0);
    }
    @Test public void interruptionAndLookalikeCannotCompleteSupplementalConfirmation(){
        var first=scene(32,48,80,142,false,false,false);var guard=new AppearanceConfirmation143("body","source",first,seed(),memory(923),true,true);
        for(int i=1;i<=8;i++){var t=guard.update(scene(180,60,120,923,i<=2||i==5,true,i>=6));assertFalse(t.toString(),t.getJSONObject("references143").getJSONObject("recovery").getBoolean("accepted"));}
    }
    @Test public void monitorPreservesTrackerAndFilterAndDimensionsAndDefaults(){
        var first=scene(32,48,80,142,false,false,false);var monitor=new AppearanceConfirmation143("body","source",first,seed(),memory(923),false,true);var base=new ForegroundGuard139("body","source",first,seed(),true);var a=new ObservationFilter(0,seed());var b=new ObservationFilter(0,seed());
        for(int i=1;i<=6;i++){var im=scene(180,60,120,923,i<=2,true,false);var x=monitor.update(im);var y=base.update(im);assertTrue(a.step(i,x).similar(b.step(i,y)));x.remove("references143");assertTrue(x.similar(y));}
        assertThrows(IllegalArgumentException.class,()->monitor.update(new BufferedImage(321,240,BufferedImage.TYPE_INT_RGB)));assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
    }
    @Test public void malformedReferenceValuesAndChangedDescriptorsRefuse(){
        var m=memory(923);var e=m.getJSONArray("examples").getJSONObject(0);var a=e.getJSONArray("descriptor");assertThrows(JSONException.class,()->a.put(0,Double.NaN));a.put(0,100);assertThrows(IllegalArgumentException.class,()->new ReferenceSearch143(scene(32,48,80,142,false,false,false),seed(),m,true));e.put("descriptorSha256",WorldFrames.hash(new JSONObject().put("values",a)));assertThrows(IllegalArgumentException.class,()->new ReferenceSearch143(scene(32,48,80,142,false,false,false),seed(),m,true));
    }
    @Test public void originalPixelsReplayBeforeIndependentOutcomesAndForgedDecisionRefuses()throws Exception{
        var images=new ArrayList<BufferedImage>();images.add(scene(32,48,80,142,false,false,false));var memory=memory(923);var tracker=new AppearanceConfirmation143("body","source",images.get(0),seed(),memory,true,true);var filter=new ObservationFilter(0,seed());var trace=new JSONArray();var rows=new JSONArray();
        for(int i=1;i<=7;i++){images.add(scene(180,60,120,923,i<=2,true,false));var t=tracker.update(images.get(i));trace.put(new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("bodyId","body").put("tracker",t).put("estimate",filter.step(i,t)))));var l=new JSONObject().put("ordinal",i).put("visibility",i<=2?"occluded":"partial");if(i>2)l.put("targetBox",new PixelTracker.Box(180/320.,.25,.375,.5).json());rows.put(l);}
        var data=new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","source").put("box",seed().json())));var run=new JSONObject().put("trackerPolicy","reference-confirmation-143").put("referenceMemory137",memory).put("trace",trace);var labels=new JSONObject().put("objectKey","subject").put("rows",rows);
        var a=ConfirmationAudit143.check(run,data,labels,"body",images::get);assertTrue(a.toString(),a.getJSONObject("additional").getInt("correct")>0);assertEquals(0,a.getJSONObject("additional").getInt("incorrect"));int n=a.getJSONArray("scores").getJSONObject(0).getInt("ordinal");String original=WorldFrames.hash(run);rows.getJSONObject(n-1).put("visibility","occluded").remove("targetBox");assertEquals(1,ConfirmationAudit143.check(run,data,labels,"body",images::get).getJSONObject("additional").getInt("incorrect"));assertEquals(original,WorldFrames.hash(run));
        var fake=ParsedJson137.copy(run);fake.getJSONArray("trace").getJSONObject(n-1).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("references143").put("memorySha256","forged");assertThrows(IllegalStateException.class,()->ConfirmationAudit143.check(fake,data,labels,"body",images::get));
    }
    @Test public void warningAloneCannotEnableExtraRecovery(){
        var first=ForegroundGuard139Test.scene(0,false,false,false);var seed=ForegroundGuard139Test.seed();var g=new AppearanceConfirmation143("body","object",first,seed,memory(923),true,true);int warnings=0;
        for(int i=0;i<5;i++){var t=g.update(ForegroundGuard139Test.scene(0,true,false,false));var m=t.getJSONObject("references143");if(t.getJSONObject("drift139").getBoolean("suspect")&&!m.getBoolean("lossObserved")){warnings++;assertFalse(m.getJSONObject("recovery").getBoolean("attempted"));assertTrue(t.isNull("box"));}}assertTrue(warnings>0);
    }
    @Test public void auditedOriginalReferenceProvenanceIsRequiredAndTamperingRefuses()throws Exception{
        try(var f=new VerifiedAppearances135Test.Fixture()){
            var set=f.service.retain(f.audit.getString("id"));int rev=f.observations.state(f.world).getJSONObject("estimate").getInt("revision");var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,rev).put("trackerPolicy","reference-confirmation-143").put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Separated generated source fixture; not identity certification");
            var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(args).getString("id"));assertEquals("completed",run.getString("status"));new ReferenceMemory137(f.root,f.observations).verify(run);
            var labels=f.association.save(AssociationAudit134Test.input(f.dataset));var audit=f.association.audit(new JSONObject().put("labelId",labels.getString("id")).put("runId",run.getString("id")));assertEquals(run.getInt("totalFrames"),audit.getJSONObject("references143").getInt("replayedFrames"));
            var pixel=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/12.image");byte[] original=java.nio.file.Files.readAllBytes(pixel),bad=original.clone();bad[20]^=1;java.nio.file.Files.write(pixel,bad);assertThrows(IllegalStateException.class,()->f.association.readAudit(audit.getString("id")));assertThrows(IllegalStateException.class,()->f.observations.export(run.getString("id")));java.nio.file.Files.write(pixel,original);assertTrue(audit.similar(f.association.readAudit(audit.getString("id"))));
        }
    }
}
