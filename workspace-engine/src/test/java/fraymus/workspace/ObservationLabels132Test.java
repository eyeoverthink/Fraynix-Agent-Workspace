package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;

public class ObservationLabels132Test {
    static JSONObject input(JSONObject d) {
        var rows = new JSONArray();
        for (int i=1;i<=4;i++) {
            var f=d.getJSONArray("frames").getJSONObject(i);
            rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256"))
                .put("visibility","visible").put("contact","observed").put("pointImage",new JSONArray("[0.4166666666666667,0.71875]"))
                .put("radiusPx",3).put("note","Analytic synthetic texture lower-centre; not a real footpoint"));
        }
        return new JSONObject().put("schema",ObservationLabels132.VERSION).put("datasetId",d.getString("id")).put("datasetSha256",WorldFrames.hash(d))
            .put("objectKey","subject").put("annotator","Java control").put("method","synthetic-control").put("exposure","before-tracking")
            .put("provenance","Generated geometry; this fixture validates software, not physical accuracy").put("rows",rows);
    }
    static JSONObject hidden(JSONObject r,String visibility) {r.put("visibility",visibility).put("contact",visibility);r.remove("pointImage");r.remove("radiusPx");return r;}
    static class Fixture implements AutoCloseable {
        final Path root=Files.createTempDirectory("labels132-");final WorldLedger ledger=new WorldLedger(root);
        final WorldObservationStore observations=new WorldObservationStore(root,ledger);
        final MetricObservationStore metrics=new MetricObservationStore(root,observations,ledger);
        final ObservationLabels132 store=new ObservationLabels132(root,observations,metrics);
        final JSONObject dataset=WorldObservationStoreTest.importPack(observations,WorldObservationStoreTest.pack());
        final String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"Label isolation").getString("id");
        Fixture()throws Exception{}
        JSONObject run()throws Exception {int revision=observations.state(world).getJSONObject("estimate").getInt("revision");return WorldObservationStoreTest.settled(observations,observations.start(WorldObservationStoreTest.arguments(ledger,world,dataset,revision)).getString("id"));}
        public void close(){observations.close();}
    }
    @Test public void sourceOnlyViewWorksBeforeTrackingAndNeverLeaksResults()throws Exception {
        try(var f=new Fixture()) {
            var source=f.store.source(f.dataset.getString("id"),2);
            assertFalse(source.has("tracker"));assertFalse(source.has("estimate"));assertFalse(source.has("run"));
            assertEquals(f.dataset.getJSONArray("frames").getJSONObject(2).getString("imageSha256"),source.getJSONObject("frame").getString("imageSha256"));
            var before=f.ledger.observe(f.world);f.run();assertTrue(source.similar(f.store.source(f.dataset.getString("id"),2)));assertTrue(before.similar(f.ledger.observe(f.world)));
        }
    }
    @Test public void hiddenPartialAndMissingHaveSeparateDenominators()throws Exception {
        try(var f=new Fixture()) {
            var labels=input(f.dataset);hidden(labels.getJSONArray("rows").getJSONObject(0),"occluded");
            labels.getJSONArray("rows").getJSONObject(2).put("visibility","partial");hidden(labels.getJSONArray("rows").getJSONObject(3),"out-of-view");
            var run=f.run();run.getJSONArray("trace").getJSONObject(1).getJSONArray("objects").getJSONObject(0).getJSONObject("estimate").put("accepted",false);
            String body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");
            var a=ObservationLabels132.compute(labels,f.dataset,run,body,null);var summary=a.getJSONObject("summary");
            assertEquals(2,summary.getInt("unsupportedHiddenMeasurements"));assertEquals(2,summary.getInt("observableContacts"));assertEquals(1,summary.getInt("contactAvailable"));assertEquals(1,summary.getInt("contactMissing"));
            assertEquals(1,a.getJSONObject("visibility").getJSONObject("partial").getInt("accepted"));assertFalse(a.getJSONArray("scores").getJSONObject(1).has("filteredAnchorErrorPx"));
        }
    }
    @Test public void labelsDoNotRewriteTrackerAndRawFilteredAnchorsAreSeparate()throws Exception {
        try(var f=new Fixture()) {
            var labels=input(f.dataset);var run=f.run();String body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");
            var object=run.getJSONArray("trace").getJSONObject(0).getJSONArray("objects").getJSONObject(0);object.getJSONObject("estimate").put("position",new JSONArray("[0.5,0.4]"));
            String before=run.toString();var a=ObservationLabels132.compute(labels,f.dataset,run,body,null);
            labels.getJSONArray("rows").getJSONObject(0).put("pointImage",new JSONArray("[0.9,0.9]"));var b=ObservationLabels132.compute(labels,f.dataset,run,body,null);
            assertEquals(before,run.toString());assertNotEquals(a.getJSONArray("scores").getJSONObject(0).getDouble("rawAnchorErrorPx"),a.getJSONArray("scores").getJSONObject(0).getDouble("filteredAnchorErrorPx"),1e-6);
            assertTrue(b.getJSONArray("scores").getJSONObject(0).getDouble("rawAnchorErrorPx")>a.getJSONArray("scores").getJSONObject(0).getDouble("rawAnchorErrorPx"));
        }
    }
    @Test public void rejectsUnboundContradictoryAndInventedContacts()throws Exception {
        try(var f=new Fixture()) {
            var bad=input(f.dataset);bad.getJSONArray("rows").getJSONObject(0).put("visibility","occluded");assertThrows(IllegalArgumentException.class,()->f.store.save(bad));
            bad.getJSONArray("rows").getJSONObject(0).put("visibility","visible").put("ordinal",1.1);assertThrows(IllegalArgumentException.class,()->f.store.save(bad));
            bad.getJSONArray("rows").getJSONObject(0).put("ordinal",0);assertThrows(IllegalArgumentException.class,()->f.store.save(bad));
            bad.getJSONArray("rows").getJSONObject(0).put("ordinal",1).put("sourceFrame",1.1);assertThrows(IllegalArgumentException.class,()->f.store.save(bad));
            bad.getJSONArray("rows").getJSONObject(0).put("sourceFrame",1).put("imageSha256","0".repeat(64));assertThrows(IllegalArgumentException.class,()->f.store.save(bad));
            var unknown=input(f.dataset).put("objectKey","unknown");assertThrows(IllegalArgumentException.class,()->f.store.save(unknown));
            var duplicate=input(f.dataset);duplicate.getJSONArray("rows").put(1,duplicate.getJSONArray("rows").getJSONObject(0));assertThrows(IllegalArgumentException.class,()->f.store.save(duplicate));
            var point=input(f.dataset);point.getJSONArray("rows").getJSONObject(0).put("pointImage",new JSONArray("[1.01,0]"));assertThrows(IllegalArgumentException.class,()->f.store.save(point));
            point.getJSONArray("rows").getJSONObject(0).put("pointImage",new JSONArray("[0.5,0.5]")).put("radiusPx",0);assertThrows(IllegalArgumentException.class,()->f.store.save(point));
            var extra=input(f.dataset).put("predictions",new JSONArray());assertThrows(IllegalArgumentException.class,()->f.store.save(extra));
        }
    }
    @Test public void freezeAuditRestartAndTamperingPreserveEvidenceAndNativeWorld()throws Exception {
        try(var f=new Fixture()) {
            var nativeBefore=f.ledger.observe(f.world);var label=f.store.save(input(f.dataset));assertTrue(label.similar(f.store.save(input(f.dataset))));var run=f.run();
            assertTrue(label.similar(f.store.importRecord(label)));var forged=WorldFrames.copy(label);forged.getJSONObject("input").put("annotator","Changed author");assertThrows(IllegalArgumentException.class,()->f.store.importRecord(forged));assertEquals(1,f.store.state().getJSONArray("labels").length());
            var clock=new JSONArray();for(int i=0;i<9;i++)clock.put(i*.1);f.metrics.clock(new JSONObject().put("datasetId",f.dataset.getString("id")).put("seconds",clock).put("source","Synthetic clock").put("cameraKey","test-fixed"));
            String geo=f.metrics.geometry(PlaneCalibrationTest.mat().put("imagePoints",new JSONArray("[[0,0],[1,0],[1,1],[0,1]]"))).getString("id");
            var args=new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id")).put("geometryId",geo);var audit=f.store.audit(args);
            assertEquals(4,audit.getJSONObject("summary").getInt("projectedContactAvailable"));
            var restarted=new ObservationLabels132(f.root,f.observations,new MetricObservationStore(f.root,f.observations,f.ledger));assertTrue(label.similar(restarted.read(label.getString("id"))));assertTrue(audit.similar(restarted.readAudit(audit.getString("id"))));
            assertTrue(run.similar(f.observations.read(run.getString("id"))));assertTrue(nativeBefore.similar(f.ledger.observe(f.world)));
            Path file=f.root.resolve("observation-labels132/audits/"+audit.getString("id")+".json");String original=Files.readString(file);Files.writeString(file,original.replace("\"observableContacts\": 4","\"observableContacts\": 3"));assertThrows(IllegalStateException.class,()->f.store.readAudit(audit.getString("id")));Files.writeString(file,original);
            Path labels=f.root.resolve("observation-labels132/labels/"+label.getString("id")+".json");String text=Files.readString(labels);Files.writeString(labels,text.replace("Java control","Forged control"));assertThrows(IllegalStateException.class,()->f.store.read(label.getString("id")));Files.writeString(labels,text);
            Path image=f.root.resolve("world-observations/datasets/"+f.dataset.getString("id")+"/2.image");byte[] bytes=Files.readAllBytes(image);bytes[20]^=1;Files.write(image,bytes);assertThrows(IllegalStateException.class,()->f.store.readAudit(audit.getString("id")));assertArrayEquals(bytes,Files.readAllBytes(image));
        }
    }
    @Test public void differentDatasetAndIncompleteRunCannotBeScored()throws Exception {
        try(var f=new Fixture()) {
            var label=f.store.save(input(f.dataset));var other=WorldObservationStoreTest.importPack(f.observations,WorldObservationStoreTest.pack());
            var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(WorldObservationStoreTest.arguments(f.ledger,f.world,other,0)).getString("id"));
            assertThrows(IllegalArgumentException.class,()->f.store.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id"))));
            var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,f.observations.state(f.world).getJSONObject("estimate").getInt("revision"));String id=f.observations.start(args).getString("id");f.observations.stop(id);
            assertThrows(IllegalArgumentException.class,()->f.store.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",id)));
        }
    }
    @Test public void capacityRetainsAllVersionsAndAllowsIdempotentReads()throws Exception {
        try(var f=new Fixture()) {
            JSONObject first=null;
            for(int i=0;i<64;i++){var value=input(f.dataset).put("provenance","Synthetic version "+i);var record=f.store.save(value);if(i==0)first=record;}
            assertEquals(64,f.store.state().getJSONArray("labels").length());
            assertTrue(first.similar(f.store.save(first.getJSONObject("input"))));assertThrows(IllegalStateException.class,()->f.store.save(input(f.dataset).put("provenance","Version 65 refused")));
            assertEquals(64,f.store.state().getJSONArray("labels").length());
        }
    }
    @Test public void growingHistoryDoesNotChangeAuditAndUnlabelledSourceBytesStillVerify()throws Exception {
        try(var f=new Fixture()) {
            var labels=f.store.save(input(f.dataset));var run=f.run();
            var audit=f.store.audit(new JSONObject().put("labelId",labels.getString("id")).put("runId",run.getString("id")));
            for(int i=0;i<3;i++)assertEquals("completed",f.run().getString("status"));
            assertTrue(audit.similar(f.store.readAudit(audit.getString("id"))));
            Path image=f.root.resolve("world-observations/datasets/"+f.dataset.getString("id")+"/8.image");
            byte[] original=Files.readAllBytes(image),changed=original.clone();changed[20]^=1;Files.write(image,changed);
            assertThrows(IllegalStateException.class,()->f.store.readAudit(audit.getString("id")));assertArrayEquals(changed,Files.readAllBytes(image));
            Files.write(image,original);assertTrue(audit.similar(f.store.readAudit(audit.getString("id"))));
        }
    }
}
