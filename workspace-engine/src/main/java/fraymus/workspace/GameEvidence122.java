package fraymus.workspace;
import org.json.*;
/** Additional bounded forecast receipt checks; original provenance and outcome gates still apply. */
final class GameEvidence122 {
 static void verify(JSONObject c,int tick){
  if(!c.getString("policy").equals("contact122"))return;
  GameEvidence121.verify(c,tick);var e=c.getJSONObject("enemyLanding122");
  long checks=e.getLong("checks"),overrides=e.getLong("overrides");int edges=e.getInt("edgeStarts");
  if(checks<0||checks>tick||overrides<0||overrides>checks||e.getLong("nanos")<0||e.getInt("horizonTicks")!=60||e.getInt("threatLimit")!=64||edges<0||edges>3||edges!=c.getJSONObject("contactRecovery119").getInt("edgeStarts122"))throw new IllegalStateException("Invalid bounded enemy landing counters");
  if(checks>0){
   double h=e.getDouble("horizontal");
   if(e.getInt("ticks")<1||e.getInt("ticks")>60||!Double.isFinite(e.getDouble("x"))||!Double.isFinite(e.getDouble("y"))||!Double.isFinite(h)||h!=0&&h!=1&&h!=-1||e.getBoolean("override")&&(!e.getBoolean("desiredContact")||e.getBoolean("chosenContact")||!e.getBoolean("chosenSupported")||overrides==0))throw new IllegalStateException("Invalid enemy landing forecast");
  }else if(e.has("override")||overrides!=0)throw new IllegalStateException("Forecast invented without observation");
 }
}
