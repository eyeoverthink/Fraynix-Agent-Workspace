package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** An owned reconstruction receipt is reusable evidence, never permission to skip a new task's native checks. */
final class RebuildCertificate075 {
    static final String VERSION="owned-at-rest-reconstruction-075-v1";
    @FunctionalInterface interface Original {JSONObject read(JSONObject attempt)throws Exception;}
    final Path root;
    RebuildCertificate075(Path root){this.root=root;}
    static String verifier(){
        var v=new JSONObject().put("version",VERSION).put("native",WorldNativeCheckpoint.identity()).put("restore","tick0-zero-velocities-no-held-object-original-controller");
        for(Class<?> c:new Class<?>[]{RebuildCertificate075.class,Rehearsal073.class})try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){v.put(c.getSimpleName(),MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}
        return WorldFrames.hash(v);
    }
    static String key(JSONObject bundle){return WorldFrames.hash(new JSONObject().put("bundle",WorldFrames.hash(bundle)).put("verifier",verifier()));}
    private Path path(String key){if(!key.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid certificate key");return root.resolve(key+".json");}
    private static JSONObject origin(JSONObject job,String id){
        for(Object r:job.getJSONArray("rows")){var a=((JSONObject)r).getJSONObject("results").optJSONObject("cached");if(a!=null)for(Object raw:a.getJSONArray("attempts")){var t=(JSONObject)raw;if(t.getString("id").equals(id))return t;}}
        throw new IllegalStateException("Certificate lacks an owned cached reconstruction attempt");
    }
    static JSONObject verify(JSONObject job,JSONObject bundle,JSONObject certificate,Original originals)throws Exception {
        var e=bundle.getJSONObject("episode");Rehearsal073.rest(e.getJSONObject("run"));
        if(!certificate.getString("version").equals(VERSION)||!certificate.getString("key").equals(key(bundle))||!certificate.getString("verifier").equals(verifier())||!certificate.getString("nativeIdentity").equals(WorldNativeCheckpoint.identity())||!e.getString("nativeIdentity").equals(WorldNativeCheckpoint.identity())||!certificate.getString("studyId").equals(job.getString("id"))||!certificate.getString("bundleSha256").equals(WorldFrames.hash(bundle)))throw new IllegalStateException("Certificate source, owner, native identity or restore/verifier contract changed");
        var a=origin(job,certificate.getString("originAttemptId"));
        if(!a.getString("stage").equals("rebuild")||!a.getString("family").equals("original-memory")||!a.getString("status").equals("completed")||!a.getJSONObject("metrics").getBoolean("passed")||!a.getString("sha256").equals(certificate.getString("runSha256")))throw new IllegalStateException("Certificate is not an originally verified reconstruction");
        var run=originals.read(a);var original=e.getJSONObject("run");
        if(!run.getString("id").equals(a.getString("id"))||!run.getString("goalId").equals(job.getString("id"))||!run.getString("status").equals("completed")||!WorldFrames.hash(run).equals(a.getString("sha256"))||!WorldVerify.verify(run).getBoolean("passed")||!run.getJSONObject("definition").similar(original.getJSONObject("definition"))||!run.getJSONObject("plan").similar(original.getJSONObject("plan")))throw new IllegalStateException("Original reconstruction trace or plan changed");
        if(Instant.parse(certificate.getString("createdAt")).isBefore(Instant.parse(run.getString("createdAt")))||Instant.parse(run.getString("createdAt")).isBefore(Instant.parse(a.getString("plannedAt"))))throw new IllegalStateException("Certificate chronology changed");
        if(a.optBoolean("unobservedTailCharged")||a.getInt("chargedTicks")!=run.getJSONObject("final").getInt("tick")||a.getInt("observedTicks")!=a.getInt("chargedTicks")||!WorldVerify.verify(run).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Original reconstruction cost or verification changed");
        var comparison=Rehearsal073.comparison(original,run);if(!comparison.getBoolean("passed")||!comparison.similar(certificate.getJSONObject("comparison")))throw new IllegalStateException("Original reconstruction comparison changed");
        return run;
    }
    JSONObject lookup(JSONObject job,JSONObject bundle,Original originals){
        long began=System.nanoTime();String key=key(bundle);var out=new JSONObject().put("key",key).put("checkedAt",Instant.now().toString());
        try{
            if(!Files.exists(path(key)))return out.put("status","miss").put("reason","No verified certificate for the exact original source and native contract");
            var c=AgentFiles042.loadChecked(path(key));var run=verify(job,bundle,c,originals);
            return out.put("status","hit").put("certificate",c).put("certificateSha256",WorldFrames.hash(c)).put("run",run).put("reason","Owned original reconstruction revalidated; new-task forecast and actual execution still required");
        }catch(Exception e){return out.put("status","invalid").put("reason",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
        finally{out.put("validationMillis",(System.nanoTime()-began)/1e6);}
    }
    JSONObject install(JSONObject job,JSONObject bundle,JSONObject attempt,Original originals)throws Exception {
        var run=originals.read(attempt);var c=new JSONObject().put("version",VERSION).put("key",key(bundle)).put("verifier",verifier()).put("nativeIdentity",WorldNativeCheckpoint.identity()).put("studyId",job.getString("id")).put("bundleSha256",WorldFrames.hash(bundle)).put("originAttemptId",attempt.getString("id")).put("runSha256",WorldFrames.hash(run)).put("createdAt",Instant.now().toString()).put("comparison",Rehearsal073.comparison(bundle.getJSONObject("episode").getJSONObject("run"),run));
        verify(job,bundle,c,originals);Path p=path(c.getString("key"));
        if(Files.exists(p)){Path history=root.resolve("superseded");Files.createDirectories(history);Files.copy(p,history.resolve(UUID.randomUUID()+".json"));}
        AgentFiles042.saveChecked(p,c);return c;
    }
    static JSONObject receipt(JSONObject lookup){var r=WorldFrames.copy(lookup);r.remove("run");return r;}
}
