package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.nio.file.*;

/** Recorded image evidence supports inspection, not an invented metric world pose. */
final class ObservationTask158 {
    static final String KIND="verified-observation-task-158-v1";
    static final String LIMIT="Recorded image-plane estimates; positional error is uncalibrated. No metric mapping, live pose or guaranteed physical identity. Physical placement refused.";
    interface Evidence { JSONObject read(String id)throws Exception; }
    private final Evidence evidence;
    private final AssociationAudit134 association;
    private final WorldObservationStore observations;
    private final WorldLedger worlds;
    private final Path project;
    ObservationTask158(Path project,ContinuityExperiment157 replay,AssociationAudit134 association,WorldObservationStore observations,WorldLedger worlds){this(project,new ObservationEvidence158(project,replay,new ReplayEvidence157(project,observations,association)),association,observations,worlds);}
    ObservationTask158(Path project,Evidence evidence,AssociationAudit134 association,WorldObservationStore observations,WorldLedger worlds){this.project=project;this.evidence=evidence;this.association=association;this.observations=observations;this.worlds=worlds;}
    static boolean own(JSONObject w){return KIND.equals(w.optString("profile158"));}
    static boolean requested(String s){return ChatPolicy044.has(s,"verified observation task|observation[- ]world[- ]browser");}
    static String code(){var text=new StringBuilder(KIND);for(Class<?> c:List.of(ObservationTask158.class,ObservationEvidence158.class,ObservationReport158.class,BrowserOutcome126.class,WorkflowRunner.class,Domain044.class,ChatPolicy044.class))try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){text.append(PerceptionStore.hash(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(text.toString());}
    static JSONObject definition(JSONObject input){
        SpatialScene.keys(input,"profile158","evidenceId","browserContract126","request","title","modelName","saveSkillName","reviewAfterWorld158");
        if(!KIND.equals(input.getString("profile158"))||!input.getString("evidenceId").matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Select verified edition157 evidence");
        var d=WorldFrames.copy(input);for(String k:List.of("request","title","modelName"))Creation.text(d,k,k.equals("request")?4000:140);
        d.put("browserContract126",BrowserOutcome126.contract(d.getJSONObject("browserContract126")));
        String name=d.optString("saveSkillName","");if(!name.isEmpty()&&!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Choose a3..56 character procedure name");d.put("saveSkillName",name);
        if(d.has("reviewAfterWorld158")&&!(d.get("reviewAfterWorld158") instanceof Boolean))throw new IllegalArgumentException("Review must be boolean");return d;
    }
    void verifyLineages(JSONArray proofs,String originalId)throws Exception{
        if(proofs==null||proofs.isEmpty()||proofs.length()>16)throw new IllegalStateException("Original revalidation history required");
        for(Object raw:proofs){var p=(JSONObject)raw;String id=p.getString("id");var content=WorldFrames.copy(p);content.remove("id");
            if(!id.matches("[a-f0-9]{64}")||!WorldFrames.hash(content).equals(id)||!p.getString("originalId").equals(originalId))throw new IllegalStateException("Revalidation lineage identity changed");
            Path file=project.resolve("observation-revalidations158").resolve(id+".json");ReplayEvidence157.unlinkedRealPath(file);
            if(Files.size(file)>20_000||!new JSONObject(Files.readString(file)).similar(p))throw new IllegalStateException("Original revalidation lineage changed");
        }
    }
    JSONObject source(JSONObject d,boolean current)throws Exception{
        var verified=evidence.read(d.getString("evidenceId"));var e=verified.getJSONObject("experiment");
        if(!e.getString("schema").equals("continuity-experiment-157-v1")||!e.getString("id").equals(d.getString("evidenceId"))||e.getBoolean("physicalActionAuthorized"))throw new IllegalStateException("Unsupported observation evidence");
        var audit=observationsAudit(e);var label=association.read(audit.getJSONObject("input").getString("labelId"));
        var run=observations.read(audit.getJSONObject("input").getString("runId"));
        if(!WorldFrames.hash(run).equals(e.getString("runSha256")))throw new IllegalStateException("Original run changed");
        var dataset=association.verifiedDataset(e.getString("datasetId"));if(!WorldFrames.hash(dataset).equals(e.getString("datasetSha256")))throw new IllegalStateException("Original dataset changed");
        String key=label.getJSONObject("input").getString("objectKey"),body=null;
        for(Object raw:run.getJSONArray("bindings")){var b=(JSONObject)raw;if(b.getString("objectKey").equals(key)){if(body!=null)throw new IllegalStateException("Duplicate body binding");body=b.getString("bodyId");}}
        if(body==null||!key.equals(e.getString("objectKey")))throw new IllegalStateException("Observation has no unique world body");
        var binding=run.getJSONObject("nativeBinding");var scene=worlds.source(binding);JSONObject object=null;
        for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("id").equals(body))object=o;}
        if(object==null)throw new IllegalStateException("Bound world body is missing");
        if(current){var now=worlds.observe(binding.getString("worldId"));if(now.has("pending")||now.getInt("expectedRevision")!=binding.getInt("expectedRevision")||!now.getString("expectedHash").equals(binding.getString("expectedHash")))throw new IllegalStateException("Bound world changed; select a fresh observation, preserve this task");}
        var context=context(e,dataset);
        if(!e.similar(evidence.read(d.getString("evidenceId")).getJSONObject("experiment")))throw new IllegalStateException("Observation changed while binding task");
        return new JSONObject().put("evidenceId",e.getString("id")).put("evidenceSha256",WorldFrames.hash(e)).put("runId",run.getString("id")).put("runSha256",WorldFrames.hash(run)).put("datasetId",dataset.getString("id")).put("datasetSha256",WorldFrames.hash(dataset)).put("objectKey",key).put("bodyId",body).put("nativeBinding",WorldFrames.copy(binding)).put("worldSourceSha256",WorldFrames.hash(scene)).put("worldObject",WorldFrames.copy(object)).put("context",context).put("revalidation158",verified.getJSONObject("revalidation"));
    }
    private JSONObject observationsAudit(JSONObject e)throws Exception{
        // Replay157 already reconstructed this audit and all sources; read its declared input only.
        String id=e.getString("auditId");if(!id.matches("[a-f0-9]{64}"))throw new IllegalStateException("Invalid audit identity");
        Path file=project.resolve("association-labels134/audits").resolve(id+".json");ReplayEvidence157.unlinkedRealPath(file);
        if(Files.size(file)>1_000_000)throw new IllegalStateException("Audit exceeds bound");var audit=new JSONObject(Files.readString(file));
        if(!WorldFrames.hash(audit).equals(e.getString("auditSha256")))throw new IllegalStateException("Verified audit changed after replay");return audit;
    }
    static JSONObject context(JSONObject e,JSONObject dataset){
        var trace=e.getJSONArray("trace");var frames=dataset.getJSONArray("frames");var rows=new JSONArray();
        if(trace.isEmpty()||trace.length()!=frames.length()-1||trace.length()>95)throw new IllegalStateException("Complete bounded observation trace required");
        int accepted=0,withheld=0,segment=0;boolean previous=false;
        for(int i=0;i<trace.length();i++){
            var t=trace.getJSONObject(i);var f=frames.getJSONObject(i+1);boolean has=!t.isNull("box");
            if(t.getInt("ordinal")!=i+1||!t.getString("imageSha256").equals(f.getString("imageSha256")))throw new IllegalStateException("Trace image order changed");
            var row=new JSONObject().put("ordinal",i+1).put("imageSha256",f.getString("imageSha256")).put("accepted",has).put("status",has?"image estimate":"withheld");
            if(has){PixelTracker.Box.from(t.getJSONArray("box"));if(!previous)segment++;accepted++;row.put("box",new JSONArray(t.getJSONArray("box").toString())).put("segment",segment);}else{withheld++;row.put("box",JSONObject.NULL).put("segment",JSONObject.NULL);}previous=has;rows.put(row);
        }
        var uncertainty=new JSONObject().put("units","normalized image coordinates").put("positionErrorBound",JSONObject.NULL).put("errorCalibrated",false).put("metricMapping",false).put("livePose",false).put("identityGuaranteed",false).put("meaning",LIMIT);
        return new JSONObject().put("rows",rows).put("accepted",accepted).put("withheld",withheld).put("segments",segment).put("uncertainty",uncertainty).put("clockScope","Recorded ordered frames, not current physical time").put("physicalActionAuthorized",false);
    }
    static JSONObject worldDecision(JSONObject source){return new JSONObject().put("nativeBinding",source.getJSONObject("nativeBinding")).put("bodyId",source.getString("bodyId")).put("worldSourceSha256",source.getString("worldSourceSha256")).put("worldPosition",source.getJSONObject("worldObject").getJSONArray("position")).put("observationUse","inspect and explain").put("physicalPlacement","refused").put("reason",LIMIT).put("uncertainty",source.getJSONObject("context").getJSONObject("uncertainty"));}
}
