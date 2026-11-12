package fraymus.workspace;
import org.json.*;import java.util.*;
/** Checks bounded waiting/return receipts in addition to every existing trial check. */
final class GameEvidence120 {
 static void verify(JSONObject c,int tick){
  if(!Set.of("opportunity120","revisit121","observe121","contact122").contains(c.getString("policy")))return;GameEvidence119.verify(c,tick);var o=c.getJSONObject("opportunity120");
  int waits=o.getInt("waitStarts"),held=o.getInt("waitTicks"),clears=o.getInt("waitClears"),aborts=o.getInt("waitAborts"),returns=o.getInt("revisitStarts"),returnTicks=o.getInt("revisitTicks");
  if(waits<0||waits>3||held<0||held>360||held>tick||clears<0||aborts<0||clears+aborts>waits||returns<0||returns>3||returnTicks<0||returnTicks>720||returnTicks>tick||c.getLong("opportunityChecks")<0||c.getLong("opportunityChecks")>tick||c.getLong("opportunityNanos")<0)throw new IllegalStateException("Invalid opportunity budgets");
  if(!Set.of("TRAVEL","WAIT","REVISIT").contains(o.getString("phase")))throw new IllegalStateException("Unknown opportunity phase");
  var seenWait=new HashSet<String>();var seenReturn=new HashSet<String>();var activeWait=new HashMap<String,Integer>();var activeReturn=new HashMap<String,Integer>();int last=-1,w=0,v=0,clear=0,abort=0;var rows=o.getJSONArray("decisions");if(rows.length()>24)throw new IllegalStateException("Decision bound exceeded");
  for(Object raw:rows){var row=(JSONObject)raw;String id=row.getString("objectId");UUID.fromString(id);int at=row.getInt("tick");if(at<last||at<0||at>tick||row.getString("reason").isBlank())throw new IllegalStateException("Invalid opportunity observation");last=at;
   switch(row.getString("action")){
    case "wait-started"->{if(!seenWait.add(id)||!activeWait.isEmpty()||row.getInt("plannedTicks")<1||row.getInt("plannedTicks")>180)throw new IllegalStateException("Unbounded or repeated waiting");activeWait.put(id,at);w++;}
    case "wait-cleared","wait-aborted","wait-ended"->{Integer start=activeWait.remove(id);if(start==null||at-start>180)throw new IllegalStateException("Wait lacks bounded start");if(row.getString("action").equals("wait-cleared"))clear++;if(row.getString("action").equals("wait-aborted"))abort++;}
    case "return-started"->{if(!seenReturn.add(id)||!activeReturn.isEmpty())throw new IllegalStateException("Repeated return");activeReturn.put(id,at);v++;}
    case "return-ended"->{Integer start=activeReturn.remove(id);if(start==null||at-start>240)throw new IllegalStateException("Return lacks bounded start");}
    default->throw new IllegalStateException("Unknown opportunity decision");
   }
  }
  if(w!=waits||v!=returns||clear!=clears||abort!=aborts||held>waits*180||returnTicks>returns*240)throw new IllegalStateException("Opportunity totals disagree with decisions");
  if(!activeWait.isEmpty()&&!activeReturn.isEmpty()||activeWait.values().stream().anyMatch(t->tick-t>180)||activeReturn.values().stream().anyMatch(t->tick-t>240))throw new IllegalStateException("Overlapping or expired opportunity");
  if(o.getString("phase").equals("WAIT")&&activeWait.size()!=1||o.getString("phase").equals("REVISIT")&&activeReturn.size()!=1)throw new IllegalStateException("Opportunity phase lacks its source");
 }
}
