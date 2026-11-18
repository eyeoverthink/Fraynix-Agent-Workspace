package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** A fresh reconstruction of original observations, never a relabelled historical proof. */
final class PlaneReplay160 {
    private final Path project;
    private final Alignment041Store alignment;
    private final CalibratedPlane159 planes;
    PlaneReplay160(Path project,Alignment041Store alignment,WorldLedger worlds){this.project=project;this.alignment=alignment;planes=new CalibratedPlane159(project,alignment,worlds);}
    static String identity(){var s=new StringBuilder("independent-displacement-160-v1");for(Class<?> c:List.of(PlaneReplay160.class,PlaneAccuracy160.class,PlaneAccuracyReport160.class,PlaneAccuracyStore160.class,PixelTracker.class,RigidTargetTracker.class,WorldFrames.class,Alignment041Store.class,MatTarget041.class,MatReferenceGuard.class,TabletopFilter.class,PlaneCalibration.class,MatTrials041.class,MatRegistration041.class,CalibratedPlane159.class))try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){s.append(PerceptionStore.hash(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(s.toString());}
    JSONObject binding(String id)throws Exception{
        var t=alignment.trial(id);
        if(!t.getString("status").equals("completed")||!t.getString("kind").equals("movement")||!t.has("evaluation"))throw new IllegalArgumentException("Select a completed movement trial with independent endpoint labels");
        var r=alignment.registration(t.getString("registrationId"));
        if(!r.similar(t.getJSONObject("registration"))||!r.getJSONObject("nativeBinding").similar(t.getJSONObject("nativeBinding")))throw new IllegalStateException("Original registration binding changed");
        var frames=new JSONArray().put(t.getJSONObject("seed").getString("imageSha256"));for(Object raw:t.getJSONArray("trace"))frames.put(((JSONObject)raw).getJSONObject("frame").getString("imageSha256"));
        return new JSONObject().put("episodeFingerprint",WorldFrames.hash(new JSONObject().put("frames",frames))).put("trialId",id).put("trialSha256",WorldFrames.hash(t)).put("registrationId",r.getString("id")).put("registrationSha256",r.getString("sha256"));
    }
    JSONObject replay(JSONObject frozen,Runnable checkpoint)throws Exception{
        if(!binding(frozen.getString("trialId")).similar(frozen))throw new IllegalStateException("Frozen trial or independent labels changed");
        var t=alignment.trial(frozen.getString("trialId"));var source=planes.source(t.getString("registrationId"));var r=source.getJSONObject("registration");
        var original=t.getJSONObject("evaluation");var expected=WorldFrames.copy(original);expected.remove("createdAt");
        if(!MatTrials041.evaluate(t,original.getJSONObject("references")).similar(expected))throw new IllegalStateException("Original independent evaluation differs");
        var plane=new PlaneCalibration(r.getJSONObject("geometry"));var seed=pixels(t.getString("id"),0,t.getJSONObject("seed"));
        var tracker=new MatTarget041(seed,t.getJSONArray("box"),t.getJSONArray("negativeBoxes"),plane);var box=MatTarget041.box(t.getJSONArray("box"));
        var filter=new TabletopFilter(t.getJSONObject("seed").getDouble("seconds"),plane.project((box[0]+box[2])/2,(box[1]+box[3])/2));
        var guard=new MatReferenceGuard(pixels(r.getString("id"),0,r.getJSONObject("frame")),r.getJSONObject("geometry"));
        var hashes=new JSONArray().put(t.getJSONObject("seed").getString("imageSha256"));var trace=t.getJSONArray("trace");
        if(trace.length()>300)throw new IllegalStateException("Original frame limit exceeded");double previous=t.getJSONObject("seed").getDouble("seconds");
        for(int i=0;i<trace.length();i++){
            checkpoint.run();var row=trace.getJSONObject(i);var frame=row.getJSONObject("frame");double seconds=frame.getDouble("seconds");
            if(row.getInt("ordinal")!=i+1||seconds<=previous||!row.getString("phase").equals(frame.getString("phase")))throw new IllegalStateException("Original sequence or source clock changed");previous=seconds;
            var image=pixels(t.getString("id"),i+1,frame);if(image.getWidth()!=source.getInt("imageWidth")||image.getHeight()!=source.getInt("imageHeight"))throw new IllegalStateException("Frame geometry changed");
            var reference=guard.check(image);var track=tracker.update(image);boolean accepted=reference.getBoolean("valid")&&track.getBoolean("accepted");double[] p=null;
            if(accepted){var point=PlaneCalibration.point(track.getJSONArray("point"));p=plane.project(point[0],point[1]);}
            var estimate=filter.step(seconds,p,accepted,plane.definition().getDouble("anchorSigmaM"));var oldTrack=WorldFrames.copy(row.getJSONObject("tracker"));oldTrack.remove("processingMs");track.remove("processingMs");
            if(!track.similar(oldTrack)||!estimate.similar(row.getJSONObject("estimate"))||!reference.similar(row.getJSONObject("reference")))throw new IllegalStateException("Fresh reconstruction differs from original observation at frame "+(i+1));
            hashes.put(frame.getString("imageSha256"));
        }
        checkpoint.run();if(!binding(t.getString("id")).similar(frozen))throw new IllegalStateException("Source changed during reconstruction");
        return new JSONObject().put("trial",t).put("source",source).put("frameHashes",hashes).put("recordedEngine",t.getString("codeSha256")).put("currentObserverEngine",Alignment041Store.identity()).put("reconstructionEngine",identity()).put("exactNumericalReplay",true).put("historicalProofRelabelled",false);
    }
    private BufferedImage pixels(String id,int ordinal,JSONObject ref)throws Exception{
        UUID.fromString(id);var p=project.resolve("alignment-041/frames").resolve(id).resolve(ordinal+".image");ReplayEvidence157.unlinkedRealPath(p);var m=p.resolveSibling(ordinal+".meta.json");ReplayEvidence157.unlinkedRealPath(m);
        if(Files.size(p)>1_000_000||Files.size(m)>20_000)throw new IllegalStateException("Original frame budget exceeded");var b=Files.readAllBytes(p);
        if(b.length!=ref.getInt("bytes")||!PerceptionStore.hash(b).equals(ref.getString("imageSha256"))||!new JSONObject(Files.readString(m)).similar(ref))throw new IllegalStateException("Original frame bytes or metadata changed");return WorldObservationStore.decode(b);
    }
}
