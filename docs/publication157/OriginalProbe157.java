package fraymus.workspace;
import java.nio.file.*;
import java.util.*;
import org.json.*;
/** Exposed development measurement, not reserved evaluation. */
public class OriginalProbe157 {
    static Map<String,String> inventory(Path root)throws Exception{
        var result=new TreeMap<String,String>();try(var paths=Files.walk(root)){for(Path p:paths.filter(Files::isRegularFile).toList())result.put(root.relativize(p).toString(),PerceptionStore.hash(Files.readAllBytes(p)));}return result;
    }
    public static void main(String[] args)throws Exception{
        Path project=Path.of(args[0]);var before=inventory(project);var measurements=new JSONArray();JSONObject expected=null;
        for(boolean precomputed:new boolean[]{false,true}){
            try(var observations=new WorldObservationStore(project,new WorldLedger(project))){
                var association=new AssociationAudit134(project,observations);var reader=new OriginalAudit157(project,observations,association,precomputed);
                long started=System.nanoTime();var result=reader.readAudit(args[1]);double ms=(System.nanoTime()-started)/1e6;
                if(expected!=null&&!result.similar(expected))throw new AssertionError("Complete original audit differs");expected=result;
                measurements.put(new JSONObject().put("precomputed",precomputed).put("totalMs",ms).put("history",observations.verificationCost136()));
                System.out.println("Original audit precomputed="+precomputed+" ms="+ms);
            }
        }
        if(!before.equals(inventory(project)))throw new AssertionError("Source project changed");
        Files.writeString(Path.of(args[2]),new JSONObject().put("passed",true).put("measurements",measurements).put("exactCompleteAudit",true).put("sourceUnchanged",true).put("scope","Exposed development probe; published156 versus precomputed original149 replay only, not the complete157 experiment").toString(2));
    }
}
