package fraymus.workspace;

import java.util.function.LongSupplier;
import java.time.*;
import org.json.JSONObject;

/** Accepts only already-verified observer outputs inside Java; never an LLM-provided observation. */
final class CameraReceipt042 {
    private final LongSupplier nanos;
    private JSONObject receipt;
    private long received,sourceAge;
    CameraReceipt042(){this(System::nanoTime);}
    CameraReceipt042(LongSupplier nanos){this.nanos=nanos;}
    synchronized void accept(String trialId,JSONObject row){
        if(receipt!=null&&receipt.getString("trialId").equals(trialId)&&row.getInt("ordinal")<=receipt.getInt("ordinal"))return;
        sourceAge=Duration.between(Instant.parse(row.getString("receivedAt")),Instant.now()).toMillis();if(sourceAge<0)sourceAge=Long.MAX_VALUE/2;
        receipt=new JSONObject().put("trialId",trialId).put("ordinal",row.getInt("ordinal")).put("imageSha256",row.getJSONObject("frame").getString("imageSha256")).put("sourceSeconds",row.getJSONObject("frame").getDouble("seconds")).put("receivedAt",row.get("receivedAt")).put("referenceValid",row.getJSONObject("reference").getBoolean("valid")).put("estimate",WorldFrames.copy(row.getJSONObject("estimate")));received=nanos.getAsLong();
    }
    synchronized void clear(){receipt=null;}
    synchronized JSONObject read(){long age=receipt==null?-1:sourceAge+Math.max(0,(nanos.getAsLong()-received)/1_000_000);return new JSONObject().put("ageMs",age).put("fresh",receipt!=null&&age<=3000&&receipt.getBoolean("referenceValid")&&receipt.getJSONObject("estimate").optBoolean("accepted")).put("evidence",receipt==null?JSONObject.NULL:WorldFrames.copy(receipt));}
}
