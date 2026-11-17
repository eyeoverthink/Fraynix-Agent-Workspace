package fraymus.workspace;

import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Complete forced verifications, alternating access paths; unchanged evidence and output required. */
public class ReplayMeasure155 {
    static Map<String,String> inventory(Path root)throws Exception{
        var result=new TreeMap<String,String>();try(var paths=Files.walk(root)){for(Path p:paths.filter(Files::isRegularFile).toList()){
            ReplayEvidence151.unlinkedRealPath(p);result.put(root.relativize(p).toString(),PerceptionStore.hash(Files.readAllBytes(p)));
        }}return result;
    }
    public static void main(String[] args)throws Exception{
        Path project=Path.of(args[0]),out=Path.of(args[2]);String id=args[1];int repeats=Integer.parseInt(args[3]);
        var before=inventory(project);var expected=new JSONObject(Files.readString(project.resolve("continuity-experiments155/"+id+".json")));
        var results=new JSONArray();
        for(int repeat=0;repeat<repeats;repeat++)for(int order=0;order<2;order++){
            boolean snapshot=(repeat+order)%2==1;var ledger=new WorldLedger(project);
            try(var observations=new WorldObservationStore(project,ledger)){
                var association=new AssociationAudit134(project,observations);var replay=new ReplayEvidence151(project,association);
                try(var service=new ContinuityExperiment155(project,observations,association,replay,snapshot)){
                    var result=service.read(id,true);if(!result.getJSONObject("experiment").similar(expected))throw new AssertionError("Access-path replay changed original evidence");
                    results.put(new JSONObject().put("repeat",repeat).put("gridSearch",snapshot).put("totalMs",result.getDouble("totalMs")).put("cost",result.getJSONObject("cost155")));
                    System.out.println("Completed repeat "+repeat+", gridSearch="+snapshot+", ms="+result.getDouble("totalMs"));
                }
            }
        }
        if(!inventory(project).equals(before))throw new AssertionError("Original project changed during measurement");
        Files.writeString(out,new JSONObject().put("passed",true).put("experimentId",id).put("fullEvidenceExactlyEqual",true).put("projectUnchanged",true).put("measurements",results)
            .put("scope","Alternating complete forced verification with new process-local stores. Every original source/outcome check executed. Only coordinate arithmetic scheduling differs; every pose, original verification and complete output are exact. No camera capture.").toString(2));
    }
}
