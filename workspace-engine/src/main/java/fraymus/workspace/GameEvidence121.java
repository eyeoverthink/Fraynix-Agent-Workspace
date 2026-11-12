package fraymus.workspace;
import org.json.*;import java.util.*;
/** Validates bounded motion/return receipts in addition to original trial provenance. */
final class GameEvidence121 {
 static void verify(JSONObject c,int tick){
  String policy=c.getString("policy");if(!Set.of("revisit121","observe121","contact122").contains(policy))return;
  GameEvidence120.verify(c,tick);var o=c.getJSONObject("opportunity120");var m=o.getJSONObject("motion121");long checks=m.getLong("checks");int n=m.getInt("reversals");var events=m.getJSONArray("events");
  if(checks!=c.getLong("opportunityChecks")||checks<0||checks>tick||n<0||n>checks*64||events.length()!=Math.min(16,n)||m.getInt("tracked")<0||m.getInt("tracked")>64||m.getInt("forecastRefusals")<0||m.getInt("forecastRefusals")>checks*4||m.getDouble("returnReach")!=2.75||m.getInt("stabilityTicks")!=30||m.getBoolean("returnsEnabled")!=!policy.equals("observe121"))throw new IllegalStateException("Invalid bounded motion history");
  if(policy.equals("observe121")&&(o.getInt("revisitStarts")!=0||o.getInt("revisitTicks")!=0))throw new IllegalStateException("Return-disabled comparison applied a return");
  int last=-1;var seen=new HashSet<String>();
  for(Object raw:events){var e=(JSONObject)raw;String id=e.getString("objectId");UUID.fromString(id);int at=e.getInt("tick");double before=e.getDouble("beforeVx"),after=e.getDouble("afterVx");
   if(at<last||at<1||at>tick||!seen.add(id+":"+at)||!Double.isFinite(before)||!Double.isFinite(after)||Math.abs(before)<=.05||Math.abs(after)<=.05||Math.signum(before)==Math.signum(after)||!Double.isFinite(e.getDouble("x"))||!Double.isFinite(e.getDouble("y")))throw new IllegalStateException("Invalid observed enemy reversal");last=at;
  }
 }
}
