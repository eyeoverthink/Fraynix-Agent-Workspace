package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static fraymus.workspace.AppearanceConfirmation143Test.seed;

/** Exposed generated control of the complete tracker/filter/audit chain.
 * The fixture memory is not a production reference-admission proof. */
public class IdentityMotion149Test {
    @Test public void rebuiltMotionPassesNormalFilterReplaysAndRetainsIndependentOutcomeChecks() throws Exception {
        var images=new ArrayList<BufferedImage>();
        for(int i=0;i<=29;i++)images.add(IdentityReturn149Test.frame(i,false));
        var tracker=new IdentityReturn149("body","object",images.get(0),seed(),IdentityReturn149Test.refs(),true);
        var filter=new ObservationFilter(0,seed());var trace=new JSONArray();var rows=new JSONArray();int accepted=0;
        for(int i=1;i<images.size();i++){
            var t=tracker.update(images.get(i));var estimate=filter.step(i,t);
            trace.put(new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("bodyId","body").put("tracker",t).put("estimate",estimate))));
            var l=new JSONObject().put("ordinal",i).put("visibility",i>=5&&i<23?"occluded":"visible");
            if(i<5||i>=23)l.put("targetBox",new PixelTracker.Box((i<5?32+2*i:112+i-23)/320.,.2,.25,1./3).json());
            rows.put(l);
            if(t.getJSONObject("identity149").getBoolean("restored")){
                assertTrue("Fresh pixel evidence must reach the unchanged observation filter",estimate.getBoolean("accepted"));accepted++;
            }
            if(i>=5&&i<23)assertFalse("Hidden predictions cannot become measurements",estimate.getBoolean("accepted"));
        }
        assertTrue("Positive new motion segment required",accepted>0);
        // Discard every live tracker/filter object, round-trip the durable prefix,
        // and reconstruct from original frames exactly as the runtime does.
        var prefix=new JSONArray(trace.toString());
        tracker=new IdentityReturn149("body","object",images.get(0),seed(),IdentityReturn149Test.refs(),true);
        filter=new ObservationFilter(0,seed());
        for(int i=1;i<images.size();i++){
            var t=tracker.update(images.get(i));var estimate=filter.step(i,t);
            var old=prefix.getJSONObject(i-1).getJSONArray("objects").getJSONObject(0);
            assertTrue("Rebuilt identity differs at "+i,old.getJSONObject("tracker").similar(t));
            assertTrue("Rebuilt filter differs at "+i,old.getJSONObject("estimate").similar(estimate));
        }
        var data=new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","object").put("box",seed().json())));
        var run=new JSONObject().put("trackerPolicy","identity-return-149").put("referenceMemory137",IdentityReturn149Test.refs()).put("trace",trace);
        var labels=new JSONObject().put("objectKey","subject").put("rows",rows);
        var audit=IdentityAudit149.check(run,data,labels,"body",images::get);
        assertEquals(accepted,audit.getJSONObject("filtered").getInt("correct"));assertEquals(0,audit.getJSONObject("filtered").getInt("incorrect"));
        String original=WorldFrames.hash(run);int ordinal=audit.getJSONArray("scores").getJSONObject(0).getInt("ordinal");
        rows.getJSONObject(ordinal-1).put("targetBox",new JSONArray("[0.01,0.01,0.2,0.2]"));
        var relabelled=IdentityAudit149.check(run,data,labels,"body",images::get);
        assertEquals(1,relabelled.getJSONObject("filtered").getInt("incorrect"));assertEquals(original,WorldFrames.hash(run));
        var forged=ParsedJson137.copy(run);forged.getJSONArray("trace").getJSONObject(ordinal-1).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("identity149").getJSONObject("chosen").getJSONObject("segment").put("startOrdinal",1);
        assertThrows(IllegalStateException.class,()->IdentityAudit149.check(forged,data,labels,"body",images::get));
        var result=new JSONObject().put("passed",true).put("acceptedNewMeasurements",accepted).put("audit",audit).put("relabelledAudit",relabelled).put("scope","Exposed generated Java tracker/filter/audit chain and in-memory source reconstruction. Not fresh camera qualification or a positive native-app process interruption. Separate app tests cover source verification, interruption and persistence on recorded footage.");
        Files.createDirectories(Path.of("build/identity149-filter"));Files.writeString(Path.of("build/identity149-filter/result.json"),result.toString(2));
    }
}
