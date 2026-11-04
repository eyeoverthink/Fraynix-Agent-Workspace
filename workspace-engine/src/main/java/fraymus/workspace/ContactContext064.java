package fraymus.workspace;
import org.json.*;

/** Preserve complete records; omit only repeated witness history from model planning. */
final class ContactContext064 {
    static JSONObject project(JSONObject original){var copy=WorldFrames.copy(original);var contact=copy.optJSONObject("contact064");if(contact==null||!contact.has("lastCompleted"))return copy;
        if(copy.has("recentWitnesses")){var rows=copy.getJSONArray("recentWitnesses");copy.remove("recentWitnesses");copy.put("contactWitnessHistory064",new JSONObject().put("count",rows.length()).put("sha256",WorldFrames.hash(new JSONObject().put("rows",rows))).put("meaning","Historical witness rows omitted from planning; complete original facts and native receipts remain in task context and checked report. Requirements and gates unchanged."));}return copy;
    }
}
