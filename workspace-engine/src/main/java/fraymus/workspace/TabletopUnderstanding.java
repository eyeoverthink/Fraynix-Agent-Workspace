package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Explicit evidence predicates, not a semantic recognizer or claim about consciousness. */
final class TabletopUnderstanding {
    static JSONObject probe(JSONObject f,String question){
        if(!Set.of("position","motion","visibility","calibration","prediction","limits").contains(question))throw new IllegalArgumentException("Choose a registered tabletop question");
        var out=new JSONObject().put("question",question).put("runId",f.getString("runId")).put("nativeBinding",f.getJSONObject("nativeBinding")).put("codeSha256",f.getString("codeSha256"));
        if(f.isNull("latest"))return out.put("answer","No post-seed camera samples. The target label and initial rectangle are human guidance, not an observed measurement.");
        var row=f.getJSONObject("latest");var e=row.getJSONObject("estimate");var ref=row.getJSONObject("reference");var relation=f.getJSONObject("relation");boolean recorded=!f.getString("status").equals("running")||f.getLong("ageMs")>1000;String prefix=recorded?"Recorded evidence; current physical state is unverified. ":"Latest received camera evidence. ";String answer;
        switch(question){
            case "position" -> {var p=e.getJSONArray("positionM");answer=String.format(Locale.ROOT,"%sProjected centre X %.1fmm, Y %.1fmm in the selected mat frame; %s, model radius %.1fmm. Interpolated saved tape coordinate %.2f%s. Target label was supplied by the user.",prefix,p.getDouble(0)*1000,p.getDouble(1)*1000,e.getBoolean("accepted")?"measured estimate":"prediction only",e.getDouble("radiusM")*1000,relation.getDouble("interpolatedTapeMark"),relation.getString("unit"));}
            case "motion" -> {var v=e.getJSONArray("velocityMps");double speed=Math.hypot(v.getDouble(0),v.getDouble(1));answer=String.format(Locale.ROOT,"%sEstimated projected speed %.1fmm/s: %s the declared10mm/s movement threshold. %s",prefix,speed*1000,speed>=.01?"above":"below",e.getBoolean("accepted")?"Based on accepted pixels; below threshold does not prove perfect stillness.":"Prediction only; no fresh measured motion.");}
            case "visibility" -> answer=prefix+(!ref.getBoolean("valid")?"Mat/camera reference is unverified or changed; no trusted metric update.":e.getBoolean("accepted")?"The human-seeded appearance is matched and the metric update is accepted.":"No accepted target measurement: "+row.getJSONObject("tracker").getString("status")+". Its current location remains a prediction; the cause is not automatically identified.");
            case "calibration" -> {var c=f.getJSONObject("tapeCheck");answer=String.format(Locale.ROOT,"%sIndependent saved tape span: known %.2fmm, measured %.2fmm, error %.2fmm against5mm limit. Saved check %s; latest mat reference %s. Object height/contact and certified sensor timing remain unverified.",prefix,c.getDouble("expectedM")*1000,c.getDouble("measuredM")*1000,c.getDouble("absoluteErrorM")*1000,c.getBoolean("passed")?"passed":"failed",ref.getString("status"));}
            case "prediction" -> {var p=e.getJSONArray("positionM");var v=e.getJSONArray("velocityMps");var next=new JSONArray(new double[]{p.getDouble(0)+.2*v.getDouble(0),p.getDouble(1)+.2*v.getDouble(1)});out.put("forecastHorizonS",.2).put("constantVelocityForecastM",next);answer=prefix+"A0.2second constant-velocity projection is available in the declared plane. This excludes unknown hand forces, object height, friction and collisions; model uncertainty has not been empirically certified.";}
            default -> answer="The object name is human guidance. Java measures a projected rigid appearance on a declared mat and checks saved tape scale/reference patches. It does not infer mass, friction,3D height/contact, semantic identity or consciousness. Correlation is not probability; no physical-action authority is granted.";
        }
        return out.put("answer",answer).put("recorded",recorded).put("acceptedMeasurement",e.getBoolean("accepted")).put("ageMs",f.getLong("ageMs")).put("sourceSeconds",row.getJSONObject("frame").getDouble("seconds")).put("imageSha256",row.getJSONObject("frame").getString("imageSha256")).put("receivedAt",row.getString("receivedAt"));
    }
}
