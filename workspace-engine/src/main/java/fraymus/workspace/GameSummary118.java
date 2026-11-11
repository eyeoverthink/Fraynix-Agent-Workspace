package fraymus.workspace;
import org.json.*;
import java.util.*;
/** Chat reports the already verified native outcome; it does not infer success from a forecast. */
final class GameSummary118 {
    static String describe(JSONObject a){
        var reasons=new LinkedHashSet<String>();for(Object raw:a.getJSONArray("pickupDecisions")){var row=(JSONObject)raw;reasons.add((row.has("action")?row.getString("action")+": ":"")+row.getString("reason"));}
        return "Constructed and playtested a separate level. Actual finish contact verified. "+a.getInt("repairCount")+" measured repairs; "+a.getInt("wins")+" successful and "+a.getInt("failures")+" failed trials retained. Candidate "+a.getString("candidateId")+". Simple control: "+a.getString("simpleControl")+". Game AI comparison: "+a.getString("existingAI")+"; "+a.getInt("aiPickups")+" power-ups collected, "+a.getInt("aiDamage")+" damage events. "+a.getInt("aiPickupSkips")+" optional pickups declined. "+a.optInt("aiPickupDeferrals",0)+" temporary pickup deferrals; "+a.optInt("aiContactAttempts",0)+" contact recovery attempts. "+(reasons.isEmpty()?"":String.join("; ",reasons)+". ")+"The verified candidate is open at its start. No model training or saved-skill admission claimed.";
    }
}
