package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import static org.junit.Assert.*;

public class StartingReference150Test {
    static class Fixture extends ObservationLabels132Test.Fixture {
        final JSONObject data,run,labels,audit;
        final AssociationAudit134 association;
        final VerifiedAppearances135 service;
        Fixture()throws Exception {
            var pack=WorldObservationStoreTest.pack();var frames=new JSONArray();
            var out=new ByteArrayOutputStream();ImageIO.write(AppearanceFlow135Test.view(0,40,50,false,false),"png",out);
            byte[] bytes=out.toByteArray();
            for(int i=0;i<=8;i++)frames.put(new JSONObject().put("sourceFrame",i+1)
                .put("image",Base64.getEncoder().encodeToString(bytes)).put("imageSha256",PerceptionStore.hash(bytes)));
            pack.put("frames",frames);pack.getJSONArray("objects").getJSONObject(0).put("box",AppearanceFlow135Test.seed().json());
            data=WorldObservationStoreTest.importPack(observations,pack);
            labels=AssociationAudit134Test.input(data);var rows=new JSONArray();
            for(int i=1;i<=8;i++)rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",i+1)
                .put("imageSha256",PerceptionStore.hash(bytes)).put("visibility","visible")
                .put("targetBox",AppearanceFlow135Test.seed().json()).put("note","Independent stationary generated geometry, no novelty"));
            labels.put("rows",rows);association=new AssociationAudit134(root,observations);var saved=association.save(labels);
            run=WorldObservationStoreTest.settled(observations,observations.start(WorldObservationStoreTest.arguments(ledger,world,data,0)
                .put("trackerPolicy","appearance-continuity-135")).getString("id"));
            assertEquals("completed",run.getString("status"));audit=audit(saved);
            service=new VerifiedAppearances135(root,association);
        }
        JSONObject audit(JSONObject label)throws Exception {
            return association.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id")));
        }
    }

    @Test public void zeroNoveltyStillRetainsVerifiedStartingEvidenceAndRestarts()throws Exception {
        try(var f=new Fixture()) {
            assertEquals(0,f.audit.getJSONObject("appearance135").getJSONArray("admissions").length());
            assertThrows(IllegalArgumentException.class,()->f.service.retain(f.audit.getString("id")));
            var before=f.ledger.observe(f.world);var original=f.observations.read(f.run.getString("id"));
            var set=f.service.retainStartingReference(f.audit.getString("id"));
            assertEquals(StartingReference150.SCHEMA,set.getString("schema"));assertEquals(0,set.getInt("learnedExampleCount"));
            assertEquals(1,set.getJSONArray("examples").length());assertEquals(3,set.getJSONArray("support").length());
            var e=set.getJSONArray("examples").getJSONObject(0);assertEquals(1,e.getInt("ordinal"));assertFalse(e.getBoolean("learned"));
            var pixels=f.observations.image(f.data.getString("id"),1);
            var expected=new JSONArray(PixelTracker.descriptor(PixelTracker.working(WorldObservationStore.decode(pixels)),PixelTracker.Box.from(e.getJSONArray("referenceBox"))));
            assertTrue(expected.similar(e.getJSONArray("descriptor")));
            assertTrue(set.similar(f.service.retainStartingReference(f.audit.getString("id"))));
            assertTrue(set.similar(new VerifiedAppearances135(f.root,new AssociationAudit134(f.root,f.observations)).read(set.getString("id"))));
            assertTrue(before.similar(f.ledger.observe(f.world)));assertTrue(original.similar(f.observations.read(f.run.getString("id"))));
            var references=new ReferenceMemory137(f.root,f.observations);var memory=references.resolve(set.getString("id"),f.dataset.getString("id"),"Separated generated source, declared same object","identity-return-149");
            assertTrue(memory.getJSONArray("examples").getJSONObject(0).has("structure149"));
            assertEquals(StartingReference150.KIND,references.list().getJSONArray("sets").getJSONObject(0).getString("evidenceKind"));
        }
    }

    @Test public void partialHiddenWrongMissingAndCompetingStartingEvidenceRefuse()throws Exception {
        try(var f=new Fixture()) {
            for(String problem:new String[]{"partial","occluded","uncertain","wrong","missing","competitor"}) {
                var labels=WorldFrames.copy(f.labels);var rows=labels.getJSONArray("rows");var row=rows.getJSONObject(1);
                switch(problem) {
                    case "partial" -> row.put("visibility","partial");
                    case "occluded","uncertain" -> {row.put("visibility",problem);row.remove("targetBox");}
                    case "wrong" -> row.put("targetBox",new JSONArray("[.7,.6,.9,.9]"));
                    case "missing" -> rows.remove(1);
                    case "competitor" -> row.put("otherBoxes",new JSONArray().put(row.getJSONArray("targetBox")));
                }
                var audit=f.audit(f.association.save(labels));
                assertThrows(problem,IllegalArgumentException.class,()->f.service.retainStartingReference(audit.getString("id")));
            }
        }
    }

    @Test public void noLaterGoodSamplesCanReplaceAnInvalidStart()throws Exception {
        try(var f=new Fixture()) {
            var labels=WorldFrames.copy(f.labels);labels.getJSONArray("rows").getJSONObject(0).put("visibility","partial");
            var audit=f.audit(f.association.save(labels));assertEquals(8,audit.getJSONObject("summary").getInt("correct"));
            assertThrows(IllegalArgumentException.class,()->f.service.retainStartingReference(audit.getString("id")));
        }
    }

    @Test public void sourceBytesDescriptorsAndCodeCannotBeRelabelledAfterRetention()throws Exception {
        try(var f=new Fixture()) {
            var set=f.service.retainStartingReference(f.audit.getString("id"));String id=set.getString("id");
            Path pixel=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/1.image");
            byte[] original=Files.readAllBytes(pixel),bad=original.clone();bad[20]^=1;Files.write(pixel,bad);
            assertThrows(IllegalStateException.class,()->f.service.read(id));Files.write(pixel,original);assertTrue(set.similar(f.service.read(id)));
            for(String field:new String[]{"descriptor","referenceCodeSha256","learnedExampleCount"}) {
                var forged=WorldFrames.copy(set);forged.remove("id");
                if(field.equals("descriptor"))forged.getJSONArray("examples").getJSONObject(0).getJSONArray("descriptor").put(0,.987);
                else if(field.equals("learnedExampleCount"))forged.put(field,1);else forged.put(field,"0".repeat(64));
                String changedId=WorldFrames.hash(forged);forged.put("id",changedId);
                Files.writeString(f.root.resolve("verified-appearances135/"+changedId+".json"),forged.toString());
                assertThrows(IllegalStateException.class,()->f.service.read(changedId));
            }
        }
    }

    @Test public void separateRunUsesStartingReferenceExportsSourcesAndRefusesLeaks()throws Exception {
        try(var f=new Fixture()) {
            var set=f.service.retainStartingReference(f.audit.getString("id"));var references=new ReferenceMemory137(f.root,f.observations);
            assertThrows(IllegalArgumentException.class,()->references.resolve(set.getString("id"),f.data.getString("id"),"Same dataset cannot be evaluation"));
            int revision=f.observations.state(f.world).getJSONObject("estimate").getInt("revision");
            var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,revision).put("trackerPolicy","identity-return-149")
                .put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Separated generated control; not semantic identity proof");
            var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(args).getString("id"));
            assertEquals("completed",run.getString("status"));references.verify(run);
            assertTrue(references.bundle(run).getJSONObject("set").similar(set));
            var export=f.observations.export(run.getString("id"));assertNotNull(export);
            var source=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/2.image");
            byte[] bad=Files.readAllBytes(source);bad[20]^=1;Files.write(source,bad);
            assertThrows(IllegalStateException.class,()->references.verify(run));
            assertThrows(IllegalStateException.class,()->f.observations.export(run.getString("id")));
        }
    }
}
