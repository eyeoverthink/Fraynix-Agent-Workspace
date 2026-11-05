package fraymus.workspace;
import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Frozen research runner: production APIs receive pixels, initial box, geometry and timing.
 * Later references are read only after the complete immutable trace has been saved. */
public final class MetricFootageEvaluation {
    public static void main(String[] args)throws Exception{
        String mode=args[0];Path folder=Path.of(args[1]),project=Path.of(args[2]),out=Path.of(args[3]);String name=args[4];Files.createDirectories(out);
        var ledger=new WorldLedger(project);String world;
        if(mode.equals("calibration")){world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"EPFL037 metric evidence").getString("id");Files.writeString(out.resolve("world-id.txt"),world);}
        else world=Files.readString(out.resolve("world-id.txt"));
        var original=new JSONObject(Files.readString(folder.resolve(name+".fobs.json")));var clean=WorldFrames.copy(original);clean.put("schema","fraynix-observation-frames-036-v1");for(String k:List.of("seconds","cameraKey","timingSource"))clean.remove(k);
        try(var o=new WorldObservationStore(project,ledger)){
            var m=new MetricObservationStore(project,o,ledger);var d=WorldObservationStoreTest.importPack(o,clean);m.clock(new JSONObject().put("datasetId",d.getString("id")).put("seconds",original.getJSONArray("seconds")).put("source",original.getString("timingSource")).put("cameraKey",original.getString("cameraKey")));var geometry=m.geometry(new JSONObject(Files.readString(folder.resolve("geometry.json"))));int rev=o.state(world).getJSONObject("estimate").getInt("revision");var before=ledger.observe(world);var run=WorldObservationStoreTest.settled(o,o.start(WorldObservationStoreTest.arguments(ledger,world,d,rev)).getString("id"));if(!run.getString("status").equals("completed"))throw new IllegalStateException(run.toString());if(!before.similar(ledger.observe(world)))throw new IllegalStateException("Native state changed");Files.writeString(out.resolve(name+".runtime-first.json"),run.toString(2),StandardOpenOption.CREATE_NEW);
            // Later reference positions become visible only after the original runtime result exists.
            var references=new JSONObject(Files.readString(folder.resolve(name+".references.json")));String body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");for(Object raw:references.getJSONArray("references"))((JSONObject)raw).put("bodyId",body);var request=new JSONObject().put("runId",run.getString("id")).put("geometryId",geometry.getString("id"));var fit=request.toString();var scoring=WorldFrames.copy(request).put("references",references.getJSONArray("references")).put("source",references.getString("source"));JSONObject result;
            if(mode.equals("calibration")){result=m.fit(scoring);Files.writeString(out.resolve("profile-id.txt"),result.getString("id"));}
            else{String id=Files.readString(out.resolve("profile-id.txt"));scoring.put("profileId",id);request.put("profileId",id);result=m.evaluate(scoring);}
            var view=m.view(request);var artifact=MetricReport.build(view);Files.writeString(out.resolve(name+".report.html"),artifact.getJSONObject("files").getString("index.html"));Files.writeString(out.resolve(name+".first.json"),new JSONObject().put("mode",mode).put("sourceCase",references).put("result",result).put("view",view).put("nativeUnchanged",true).put("referenceIsolation","First rectangle only during tracking; later positions read after runtime-first.json was written").toString(2),StandardOpenOption.CREATE_NEW);System.out.println(name+" "+(result.has("metrics")?result.getJSONObject("metrics"):new JSONObject().put("multiplier",result.getDouble("multiplier")).put("references",result.getInt("referenceCount"))));
        }
    }
}
