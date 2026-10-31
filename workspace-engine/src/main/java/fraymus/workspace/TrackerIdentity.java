package fraymus.workspace;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Pins predictor helpers as well as the outer class. This changes identity metadata, not prediction. */
final class TrackerIdentity {
    static JSONObject components(String profile) throws Exception {if(!Tracking024Evaluation.INSTALLED.contains(profile))throw new IllegalArgumentException("Unknown tracker profile");var names=new ArrayList<>(List.of("ImageTracker","PixelTracker","PixelTracker$Box","PixelTracker$Match","TrackingEvaluation"));if(!profile.equals(TrackingEvaluation.BASELINE))names.addAll(List.of("AdaptiveTracker","AdaptiveTracker$Plane","AdaptiveTracker$Match"));if(profile.equals(Tracking024Evaluation.CANDIDATE))names.addAll(List.of("Tracking024Evaluation","FlowTracker","FlowTracker$Plane","FlowTracker$Pyramid","FlowTracker$Point","FlowTracker$Vector"));Collections.sort(names);var out=new JSONObject();for(String name:names){try(var in=TrackerIdentity.class.getResourceAsStream(name+".class")){if(in==null)throw new IllegalStateException("Tracker implementation component missing");out.put(name,PerceptionStore.hash(in.readAllBytes()));}}return out;}
    static String hash(String profile) throws Exception {var parts=components(profile);var names=new ArrayList<>(parts.keySet());Collections.sort(names);var digest=MessageDigest.getInstance("SHA-256");for(String name:names){digest.update(name.getBytes(StandardCharsets.UTF_8));digest.update((byte)0);digest.update(parts.getString(name).getBytes(StandardCharsets.UTF_8));digest.update((byte)'\n');}return HexFormat.of().formatHex(digest.digest());}
}
