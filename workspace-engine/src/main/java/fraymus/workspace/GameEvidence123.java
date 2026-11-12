package fraymus.workspace;
import org.json.*;
/** Disabled optional actions must stay disabled; all prior evidence checks remain. */
final class GameEvidence123 {
 static void verify(JSONObject c,int tick){
  String policy=c.getString("policy");if(!GameStrategy123.applies(policy))return;GameEvidence122.verify(c,tick);var o=c.getJSONObject("opportunity120");
  boolean waits=GameStrategy123.waits(policy),returns=GameStrategy123.returns(policy);
  if(o.getBoolean("waitsEnabled123")!=waits||o.getBoolean("returnsEnabled123")!=returns)throw new IllegalStateException("Pickup feature configuration changed");
  if(!waits&&(o.getInt("waitTicks")!=0||o.getInt("waitStarts")!=0||o.getInt("waitClears")!=0||o.getInt("waitAborts")!=0)||!returns&&(o.getInt("revisitTicks")!=0||o.getInt("revisitStarts")!=0))throw new IllegalStateException("Disabled optional action was executed");
 }
}
