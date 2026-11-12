package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Bounds and labels for native recovery observations; existing source/outcome checks remain external. */
final class GameEvidence119 {
    static void verify(JSONObject c,int tick){
        if(!Set.of("recover119","opportunity120","revisit121","observe121").contains(c.getString("policy")))return;
        int deferred=c.getInt("pickupDeferrals"),skipped=c.getInt("pickupSkips"),attempts=c.getJSONObject("contactRecovery119").getInt("attempts");
        long checks=c.getLong("contactChecks"),overrides=c.getLong("contactOverrides");
        if(deferred<0||deferred>tick*4L||skipped<0||attempts<0||attempts>3||checks<0||checks>tick||overrides<0||overrides>checks||c.getLong("contactNanos")<0)throw new IllegalStateException("Invalid recovery counters");
        if(!Set.of("OBSERVE","WAIT","RELEASE","JUMP","REFUSED").contains(c.getJSONObject("contactRecovery119").getString("phase")))throw new IllegalStateException("Unknown contact phase");
        var byObject=new HashMap<String,Integer>();var previous=new HashMap<String,Integer>();int d=0,s=0,last=-1;
        var decisions=c.getJSONArray("pickupDecisions");if(decisions.length()>16||decisions.length()>(long)deferred+skipped)throw new IllegalStateException("Invalid pickup observation count");
        for(Object raw:decisions){var row=(JSONObject)raw;String id=row.getString("objectId");UUID.fromString(id);int at=row.getInt("tick");
            if(at<last||at>tick||at<0)throw new IllegalStateException("Pickup decision time changed");last=at;
            switch(row.getString("action")){
                case "deferred for fresh observation" -> {d++;int n=byObject.merge(id,1,Integer::sum);Integer before=previous.put(id,at);if(n>3||before!=null&&at-before<60||!row.getString("reason").equals("Enemy motion intersects the pickup approach"))throw new IllegalStateException("Invalid bounded pickup deferral");}
                case "declined for run" -> s++;
                default -> throw new IllegalStateException("Unknown pickup decision action");
            }
        }
        if(d>deferred||s>skipped||decisions.length()<16&&(d!=deferred||s!=skipped))throw new IllegalStateException("Pickup counters disagree with retained decisions");
    }
}
