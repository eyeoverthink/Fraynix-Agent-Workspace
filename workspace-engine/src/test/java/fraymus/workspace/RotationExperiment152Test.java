package fraymus.workspace;

import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RotationExperiment152Test {
    @Test public void exactReplayLabelIsolationForgeryAndSourceRefusal()throws Exception{
        try(var f=new StartingReference150Test.Fixture()){
            var moved=WorldFrames.copy(f.labels);moved.put("provenance","Independent negative control labels; unchanged original pixels");
            for(Object raw:moved.getJSONArray("rows"))((JSONObject)raw).put("targetBox",new JSONArray("[.7,.6,.9,.9]"));
            var otherAudit=f.audit(f.association.save(moved));
            var memo=new ReplayEvidence151(f.root,f.association);var service=new RotationExperiment152(f.root,f.observations,f.association,memo);
            var nativeBefore=f.ledger.observe(f.world);byte[] state=Files.readAllBytes(f.root.resolve("world-observations/state.json"));
            var first=service.run(f.audit.getString("id")).getJSONObject("experiment");var other=service.run(otherAudit.getString("id")).getJSONObject("experiment");
            assertTrue(first.getJSONArray("trace").similar(other.getJSONArray("trace"))); // Labels cannot alter search.
            assertTrue(first.similar(service.read(first.getString("id")).getJSONObject("experiment")));
            assertTrue(first.similar(new RotationExperiment152(f.root,f.observations,f.association,new ReplayEvidence151(f.root,f.association)).read(first.getString("id")).getJSONObject("experiment")));
            var forged=WorldFrames.copy(first);forged.remove("id");forged.getJSONObject("summary").put("correct",1000);String id=WorldFrames.hash(forged);forged.put("id",id);
            Files.writeString(f.root.resolve("rotation-experiments152/"+id+".json"),forged.toString());assertThrows(IllegalStateException.class,()->service.read(id));
            var source=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/2.image");byte[] bytes=Files.readAllBytes(source),bad=bytes.clone();bad[20]^=1;var time=Files.getLastModifiedTime(source);Files.write(source,bad);Files.setLastModifiedTime(source,time);
            assertThrows(Exception.class,()->service.read(first.getString("id")));Files.write(source,bytes);Files.setLastModifiedTime(source,time);
            assertTrue(first.similar(service.read(first.getString("id")).getJSONObject("experiment")));
            assertArrayEquals(state,Files.readAllBytes(f.root.resolve("world-observations/state.json")));assertTrue(nativeBefore.similar(f.ledger.observe(f.world)));
        }
    }
}
