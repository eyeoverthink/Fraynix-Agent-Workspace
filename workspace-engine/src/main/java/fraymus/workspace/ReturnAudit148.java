package fraymus.workspace;
import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Replay original images before scoring additional measurements against independent labels. */
final class ReturnAudit148 {
 interface Images { BufferedImage read(int ordinal)throws Exception; }
 static boolean uses(String policy){return Set.of("path-confirmation-148","path-monitor-148").contains(policy);}
 static JSONObject check(JSONObject run,JSONObject dataset,JSONObject labels,String body,Images images)throws Exception{
  String policy=ObservationMethods129.policy(run);if(!uses(policy))throw new IllegalArgumentException("Path confirmation policy required");
  JSONObject seed=null;for(Object raw:dataset.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("key").equals(labels.getString("objectKey")))seed=o;}
  if(seed==null)throw new IllegalStateException("Missing original object seed");
  var tracker=new ReturnConfirmation148(body,seed.getString("label"),images.read(0),PixelTracker.Box.from(seed.getJSONArray("box")),run.getJSONObject("referenceMemory137"),policy.equals("path-confirmation-148"));
  var labelMap=new HashMap<Integer,JSONObject>();for(Object raw:labels.getJSONArray("rows")){var l=(JSONObject)raw;labelMap.put(l.getInt("ordinal"),l);}
  var added=new JSONObject().put("correct",0).put("incorrect",0).put("unknown",0).put("returned",0);
  var filtered=ParsedJson137.copy(added);var proposed=ParsedJson137.copy(added);var scores=new JSONArray();int preserved=0;
  for(int i=0;i<run.getJSONArray("trace").length();i++){
   JSONObject observed=null;for(Object raw:run.getJSONArray("trace").getJSONObject(i).getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("bodyId").equals(body))observed=o;}
   if(observed==null)throw new IllegalStateException("Missing confirmation tracker");
   var actual=ParsedJson137.copy(observed.getJSONObject("tracker"));var expected=tracker.update(images.read(i+1));
   for(var t:List.of(actual,expected)){for(String key:List.of("id","label")){t.remove(key);t.getJSONObject("references143").getJSONObject("baseline").remove(key);}}
   if(!expected.similar(actual))throw new IllegalStateException("Original return confirmation or pixel replay differs at "+(i+1));
   var m=actual.getJSONObject("confirmation148");if(m.getBoolean("parentAccepted"))preserved++;
   if(!m.getBoolean("wouldRestore"))continue;
   var l=labelMap.get(i+1);String visibility=l==null?"uncertain":l.getString("visibility");
   boolean visible=Set.of("visible","partial").contains(visibility),hidden=Set.of("occluded","out-of-view").contains(visibility);
   double overlap=visible?PixelTracker.iou(PixelTracker.Box.from(m.getJSONObject("chosen").getJSONArray("box")),PixelTracker.Box.from(l.getJSONArray("targetBox"))):0;
   String outcome=visible&&overlap>=.5?"correct":visible||hidden?"incorrect":"unknown";
   increment(proposed,outcome);boolean restored=m.getBoolean("restored");if(restored)increment(added,outcome);
   boolean accepted=restored&&observed.has("estimate")&&observed.getJSONObject("estimate").getBoolean("accepted");if(accepted)increment(filtered,outcome);
   scores.put(new JSONObject().put("ordinal",i+1).put("visibility",visibility).put("targetOverlap",overlap).put("outcome",outcome).put("restored",restored).put("filterAccepted",accepted));
  }
  return new JSONObject().put("schema","return-confirmation-audit-148-v1").put("replayedFrames",run.getJSONArray("trace").length()).put("parentAcceptedPreserved",preserved).put("automaticAdmissions",0).put("proposed",proposed).put("additional",added).put("filtered",filtered).put("scores",scores).put("scope","Original pixel replay precedes independent source-label scoring. Extra raw proposals, filter measurements and monitor-only proposals are distinct. Unchanged146 measurements are preserved. Source labels never guide tracking; all original source, descriptor and code checks remain mandatory.");
 }
 private static void increment(JSONObject counts,String outcome){counts.put(outcome,counts.getInt(outcome)+1).put("returned",counts.getInt("returned")+1);}
}
