package fraymus.workspace;

import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Exposed baseline profiling. Complete156 original verification, never a reserved evaluation. */
public class Profile157 {
    static Map<String,String> inventory(Path root)throws Exception {
        var result=new TreeMap<String,String>();
        try(var files=Files.walk(root)){for(Path p:files.filter(Files::isRegularFile).toList())result.put(root.relativize(p).toString(),PerceptionStore.hash(Files.readAllBytes(p)));}
        return result;
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);var before=inventory(root);JSONObject audit;double elapsed;
        try(var observations=new WorldObservationStore(root,new WorldLedger(root))){
            var original=new OriginalAudit156(root,observations,new AssociationAudit134(root,observations),true);
            long start=System.nanoTime();audit=original.readAudit(args[1]);elapsed=(System.nanoTime()-start)/1e6;
        }
        if(!before.equals(inventory(root)))throw new AssertionError("Profile changed original project files");
        Files.writeString(Path.of(args[2]),new JSONObject().put("passed",true).put("baseline","published156-packed-original-verification")
            .put("auditId",audit.getString("id")).put("completeAuditSha256",WorldFrames.hash(audit)).put("totalMs",elapsed)
            .put("projectUnchanged",true).put("scope","Exposed original-audit profile with Java Flight Recorder; overhead included, not a performance claim or fresh footage").toString(2));
        System.out.println("Profiled complete original audit: "+elapsed+" ms; project unchanged");
    }
}
