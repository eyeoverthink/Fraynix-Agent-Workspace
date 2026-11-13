package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import static org.junit.Assert.*;

public class VerifiedAppearances135Test {
    static class Fixture extends ObservationLabels132Test.Fixture {
        final JSONObject data,run,labels,audit;
        final AssociationAudit134 association;
        final VerifiedAppearances135 service;
        Fixture()throws Exception {
            var pack=WorldObservationStoreTest.pack();var frames=new JSONArray();
            for(int i=0;i<=12;i++){
                var out=new ByteArrayOutputStream();ImageIO.write(AppearanceFlow135Test.view(i,40+i,50,false,false),"png",out);byte[] bytes=out.toByteArray();
                frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(bytes)).put("imageSha256",PerceptionStore.hash(bytes)));
            }
            pack.put("frames",frames);pack.getJSONArray("objects").getJSONObject(0).put("box",AppearanceFlow135Test.seed().json());
            data=WorldObservationStoreTest.importPack(observations,pack);
            labels=AssociationAudit134Test.input(data);var rows=new JSONArray();
            for(int i=1;i<=12;i++){var frame=data.getJSONArray("frames").getJSONObject(i);rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",frame.getString("imageSha256")).put("visibility","visible").put("targetBox",new PixelTracker.Box((40+i)/480.,50/300.,100/480.,100/300.).json()).put("note","Independent generated geometry"));}
            labels.put("rows",rows);association=new AssociationAudit134(root,observations);var saved=association.save(labels);
            run=WorldObservationStoreTest.settled(observations,observations.start(WorldObservationStoreTest.arguments(ledger,world,data,0).put("trackerPolicy","appearance-continuity-135")).getString("id"));
            assertEquals("completed",run.getString("status"));audit=association.audit(new JSONObject().put("labelId",saved.getString("id")).put("runId",run.getString("id")));service=new VerifiedAppearances135(root,association);
        }
    }
    @Test public void actualSourceDescriptorsRetainAndReverifyAfterRestartWithoutChangingRunOrWorld()throws Exception{
        try(var f=new Fixture()){
            var before=f.ledger.observe(f.world);var record=f.service.retain(f.audit.getString("id"));assertTrue(record.getJSONArray("examples").length()>=2);
            for(Object raw:record.getJSONArray("examples")){var e=(JSONObject)raw;var source=f.association.source(f.data.getString("id"),e.getInt("ordinal"));var descriptor=new JSONArray(PixelTracker.descriptor(PixelTracker.working(WorldObservationStore.decode(Base64.getDecoder().decode(source.getString("image")))),PixelTracker.Box.from(e.getJSONArray("referenceBox"))));assertTrue(descriptor.similar(e.getJSONArray("descriptor")));}
            assertTrue(record.similar(f.service.retain(f.audit.getString("id"))));
            assertTrue(record.similar(new VerifiedAppearances135(f.root,new AssociationAudit134(f.root,f.observations)).read(record.getString("id"))));
            assertTrue(before.similar(f.ledger.observe(f.world)));assertTrue(f.run.similar(f.observations.read(f.run.getString("id"))));
        }
    }
    @Test public void partialAndIncorrectProposalsAreExcludedFromReferenceSupportedSet()throws Exception{
        try(var f=new Fixture()){
            var first=f.audit.getJSONObject("appearance135").getJSONArray("admissions");assertTrue(first.length()>=2);
            var changed=WorldFrames.copy(f.labels);int a=first.getJSONObject(0).getInt("ordinal"),b=first.getJSONObject(1).getInt("ordinal");
            changed.getJSONArray("rows").getJSONObject(a-1).put("visibility","partial");changed.getJSONArray("rows").getJSONObject(b-1).put("targetBox",new JSONArray("[.7,.6,.9,.9]"));
            var label=f.association.save(changed);var audit=f.association.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",f.run.getString("id")));
            if(first.length()==2)assertThrows(IllegalArgumentException.class,()->f.service.retain(audit.getString("id")));
            else {var record=f.service.retain(audit.getString("id"));assertEquals(first.length()-2,record.getJSONArray("examples").length());assertEquals(2,record.getInt("excludedAdmissions"));for(Object raw:record.getJSONArray("examples")){int ordinal=((JSONObject)raw).getInt("ordinal");assertNotEquals(a,ordinal);assertNotEquals(b,ordinal);}}
            assertTrue(f.run.similar(f.observations.read(f.run.getString("id"))));
        }
    }
    @Test public void alteredOriginalPixelsAndRetainedDescriptorAreRefused()throws Exception{
        try(var f=new Fixture()){
            var record=f.service.retain(f.audit.getString("id"));String id=record.getString("id");Path pixel=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/12.image");byte[] original=Files.readAllBytes(pixel),bad=original.clone();bad[20]^=1;Files.write(pixel,bad);assertThrows(IllegalStateException.class,()->f.service.read(id));Files.write(pixel,original);assertTrue(record.similar(f.service.read(id)));
            Path file=f.root.resolve("verified-appearances135/"+id+".json");var damaged=WorldFrames.copy(record);damaged.getJSONArray("examples").getJSONObject(0).getJSONArray("descriptor").put(0,999);Files.writeString(file,damaged.toString());assertThrows(IllegalStateException.class,()->f.service.read(id));assertThrows(IllegalArgumentException.class,()->f.service.read("../bad"));
        }
    }
}
