package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/** Edition024 adapter retains the edition023 validator/scorer and discloses temporal evidence. */
final class Tracking024Evaluation {
    static final String CANDIDATE="flow-v3";
    static final java.util.List<String> INSTALLED=List.of(TrackingEvaluation.BASELINE,TrackingEvaluation.CANDIDATE,CANDIDATE);
    static String candidate(JSONObject job){String value=job.optString("candidateProfile",TrackingEvaluation.CANDIDATE);if(!INSTALLED.contains(value)||value.equals(TrackingEvaluation.BASELINE))throw new IllegalArgumentException("Unknown candidate profile");return value;}
    static java.util.List<String> profiles(JSONObject job){return List.of(TrackingEvaluation.BASELINE,candidate(job));}
    static ImageTracker tracker(String profile,String id,String label,java.awt.image.BufferedImage image,PixelTracker.Box box){return profile.equals(CANDIDATE)?new FlowTracker(id,label,image,box):TrackingEvaluation.tracker(profile,id,label,image,box);}
    static String codeHash(String profile) throws Exception {if(!profile.equals(CANDIDATE))return TrackingEvaluation.codeHash(profile);try(var in=FlowTracker.class.getResourceAsStream("FlowTracker.class")){return PerceptionStore.hash(in.readAllBytes());}}
    static JSONObject evaluate(JSONObject pack,String profile,BooleanSupplier cancelled,IntConsumer progress) throws Exception {
        if(!profile.equals(CANDIDATE))return TrackingEvaluation.evaluate(pack,profile,cancelled,progress);
        var frames=pack.getJSONArray("frames");var first=frames.getJSONObject(0);var initial=TrackingEvaluation.image(TrackingEvaluation.data(first,"image"),false);var initialBox=TrackingEvaluation.truth(TrackingEvaluation.image(TrackingEvaluation.data(first,"mask"),true),pack.getInt("objectId"));
        var tracker=tracker(profile,"research-object",pack.getString("sequence"),initial,initialBox);var results=new JSONArray();
        for(int i=1;i<frames.length();i++){
            if(cancelled.getAsBoolean())throw new InterruptedException("Evaluation cancelled");var f=frames.getJSONObject(i);long start=System.nanoTime();var prediction=tracker.update(TrackingEvaluation.image(TrackingEvaluation.data(f,"image"),false));double elapsed=(System.nanoTime()-start)/1e6;
            // Only after prediction: later annotations never enter the temporal tracker.
            var reference=TrackingEvaluation.truth(TrackingEvaluation.image(TrackingEvaluation.data(f,"mask"),true),pack.getInt("objectId"));var predicted=prediction.isNull("box")?null:PixelTracker.Box.from(prediction.getJSONArray("box"));
            results.put(new JSONObject().put("ordinal",i).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256")).put("maskSha256",f.getString("maskSha256")).put("reference",reference==null?JSONObject.NULL:reference.json()).put("prediction",prediction).put("iou",reference==null||predicted==null?0:PixelTracker.iou(reference,predicted)).put("processingMillis",elapsed));progress.accept(i);
        }
        return new JSONObject().put("profile",profile).put("codeSha256",codeHash(profile)).put("sequence",pack.getString("sequence")).put("split",pack.getString("split")).put("initialization",new JSONObject().put("sourceFrame",first.getInt("sourceFrame")).put("reference",initialBox.json()).put("scored",false)).put("frames",results).put("metrics",TrackingEvaluation.metrics(results));
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Usage: Tracking024Evaluation pack.ftrack.json output.json");var pack=new JSONObject(Files.readString(Path.of(args[0])));TrackingEvaluation.validate(pack);var out=new JSONObject();
        for(String profile:List.of(TrackingEvaluation.BASELINE,TrackingEvaluation.CANDIDATE,CANDIDATE)){out.put(profile,evaluate(pack,profile,()->false,i->{}));System.out.println(profile+" "+out.getJSONObject(profile).getJSONObject("metrics"));Files.writeString(Path.of(args[1]),out.toString(2));}
    }
}
