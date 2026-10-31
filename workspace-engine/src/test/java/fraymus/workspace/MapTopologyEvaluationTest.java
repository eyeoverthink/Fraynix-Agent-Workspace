package fraymus.workspace;

import fraymus.geometry.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Reserved acceptance comparison. Every source, seed, rejection and native outcome is retained. */
public class MapTopologyEvaluationTest {
    @Test public void reservedReferencesKeepAllDenominatorsAndRunActualAcceptedWorlds() throws Exception {
        Path repo=Path.of("").toAbsolutePath();while(repo!=null&&!Files.exists(repo.resolve("docs/TOPOLOGY_PROFILE_029.json")))repo=repo.getParent();assertNotNull(repo);var frozen=new JSONObject(Files.readString(repo.resolve("docs/TOPOLOGY_PROFILE_029.json")));
        // The published 029 runtime is historical. 030 deliberately changes native recovery and materials.
        // Protect every frozen source identity, and require the unchanged topology kernels byte-for-byte.
        var inventory=new JSONArray(Files.readString(repo.resolve("docs/readmes/029/file-inventory.json")));
        for(String name:frozen.getJSONObject("sourceSha256").keySet()){
            JSONObject historical=null;for(Object raw:inventory)if(((JSONObject)raw).getString("path").equals(name))historical=(JSONObject)raw;
            assertNotNull("Missing frozen source identity: "+name,historical);
            assertEquals(frozen.getJSONObject("sourceSha256").getString(name),historical.getString("sha256"));
            if(name.endsWith("/ReferenceMap.java")||name.endsWith("/TopologyMap.java")||name.endsWith("/MapTopology.java")){
                String text=Files.readString(repo.resolve(name)).replace("\r\n","\n");assertEquals("Frozen topology kernel changed: "+name,historical.getString("sha256"),MapDefinition.sha(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
        }
        var manifest=MapReferences.cases();assertEquals(frozen.getString("reservedManifestSha256"),MapDefinition.sha(Files.readString(repo.resolve("workspace-engine/src/main/resources/maps029/reserved-cases.json")).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var cases=new JSONArray();int baselineAccepted=0,selectedAccepted=0,nativePassed=0;Path project=Files.createTempDirectory("reserved-topology029-");
        try(var store=new MapStore(project)){
            for(Object raw:manifest.getJSONArray("cases")){var sourceCase=(JSONObject)raw;var reference=store.importImage(MapReferences.example(sourceCase.getString("id")));assertEquals(sourceCase.getString("sha256"),reference.getString("imageSha256"));
                for(Object value:sourceCase.getJSONArray("seeds")){
                    int seed=((Number)value).intValue();var options=new JSONObject(manifest.getJSONObject("output").toString()).put("seed",seed);long started=System.nanoTime();var baseline=store.build(reference.getString("id"),options);double baselineMs=(System.nanoTime()-started)/1e6;var b=baseline.getJSONObject("definition");
                    var pins=new ArrayList<ReferenceMap.Pin>();for(Object p:b.getJSONArray("pins")){var pin=(JSONObject)p;pins.add(new ReferenceMap.Pin(pin.getInt("x"),pin.getInt("y"),pin.getInt("tile")));}
                    var original=new ReferenceMap.Result(b.getInt("width"),b.getInt("height"),seed,b.getJSONArray("cells").toList().stream().mapToInt(x->((Number)x).intValue()).toArray(),pins,b.getJSONArray("path").toList().stream().map(x->((Number)x).intValue()).toList(),b.getJSONObject("generation").getInt("conflicts"),0,0,b.getJSONObject("generation").getBoolean("passed"),"");
                    var constraints=MapTopology.constraints(manifest.getJSONObject("constraints"));var audit=TopologyMap.audit(MapDefinition.rules(reference),original,constraints);if(audit.passed())baselineAccepted++;
                    started=System.nanoTime();var selected=store.build(reference.getString("id"),new JSONObject(options.toString()).put("constraints",manifest.getJSONObject("constraints")));double selectedMs=(System.nanoTime()-started)/1e6;
                    var item=new JSONObject().put("reference",sourceCase.getString("id")).put("imageSha256",reference.getString("imageSha256")).put("seed",seed).put("constraints",manifest.getJSONObject("constraints")).put("baseline",baseline).put("baselineAudit",MapTopology.json(audit)).put("baselineMs",baselineMs).put("selected",selected).put("selectedMs",selectedMs);
                    if(selected.getJSONObject("definition").getJSONObject("generation").getBoolean("passed")){
                        selectedAccepted++;var run=store.start(UUID.randomUUID().toString(),selected.getString("id"),true,seed%2==0?-.3:.35,false);
                        for(int i=0;i<1200;i++){run=store.run(run.getString("id"));if(!run.getString("status").equals("running"))break;Thread.sleep(10);}item.put("nativeRun",run);if(run.optJSONObject("verification",new JSONObject()).optBoolean("passed"))nativePassed++;
                    }else item.put("nativeNotStarted",true);cases.put(item);
                }
            }
        }
        var report=new JSONObject().put("date",java.time.Instant.now()).put("profile",frozen).put("project",project.toString()).put("cases",cases).put("caseCount",cases.length()).put("baselineGlobalAccepted",baselineAccepted).put("selectedGlobalAccepted",selectedAccepted).put("nativePassed",nativePassed).put("kernelSha256",MapTopology.identity()).put("passed",cases.length()==12&&selectedAccepted>0&&nativePassed==selectedAccepted).put("scope","Regression of the published029 reference set, now seen development data, under the030 native runtime. Four labelled synthetic PNGs × three published seeds. Original028 baseline and finite029 selection are judged against the same frozen global criteria. All counts and failures remain visible; accepted029 worlds execute actual native forces/collisions. Completion rates are descriptive, not model training, scientific generalisation or automatic policy promotion.");
        Path output=Path.of("build/reports/topology030-regression.json");Files.createDirectories(output.getParent());Files.writeString(output,report.toString(2));System.out.println("TOPOLOGY_RESERVED_RESULT "+new JSONObject().put("cases",cases.length()).put("baselineGlobalAccepted",baselineAccepted).put("selectedGlobalAccepted",selectedAccepted).put("nativePassed",nativePassed));assertTrue(report.getString("scope"),report.getBoolean("passed"));
    }
}
