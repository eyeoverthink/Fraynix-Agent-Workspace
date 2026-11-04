package fraymus.workspace;

import org.json.*;
import java.time.Instant;
import java.util.zip.GZIPInputStream;

/** A fixed published calibration, verified once; no fitting to decision outcomes. */
final class Calibration065 {
    static final String SHA="bee148d4270d4e45c5c5777fd292fcc933f3993c2849760b706c93dc08503c4f";
    private static final class Loaded {static final JSONObject INFO=verify(bundle());}
    static JSONObject bundle(){
        try(var in=Calibration065.class.getResourceAsStream("calibration065.json.gz")){
            byte[] bytes=in.readAllBytes();if(!MapDefinition.sha(bytes).equals(SHA))throw new IllegalStateException("Calibration resource differs");
            try(var gzip=new GZIPInputStream(new java.io.ByteArrayInputStream(bytes))){return new JSONObject(new String(gzip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        }catch(Exception e){throw new IllegalStateException("Cannot verify published calibration",e);}
    }
    static JSONObject verify(JSONObject b){
        var model=b.getJSONObject("intervalModel064");var rows=b.getJSONArray("calibration064");
        if(!b.getJSONObject("manifest064").similar(ContactStudy064.manifest())||!b.getJSONObject("evaluation064").getBoolean("qualified")||
            !WorldFrames.hash(model).equals(b.getString("intervalModelSha256064"))||
            !WorldFrames.hash(new JSONObject().put("rows",rows)).equals(model.getString("calibrationSha256"))||
            ContactStudy064.calibrate(rows)!=model.getDouble("radiusM"))throw new IllegalStateException("Published calibration evidence differs");
        var pinned=Instant.parse(model.getString("pinnedAt"));var ids=new java.util.HashSet<String>();
        for(Object raw:rows){var r=((JSONObject)raw).getJSONObject("record");if(!ids.add(r.getString("id"))||r.getInt("ticks")!=180||r.getInt("stride")!=4||!r.getString("variant").equals("baseline")||Instant.parse(r.getString("observedAt")).isAfter(pinned))throw new IllegalStateException("Calibration clock or identity differs");}
        return new JSONObject().put("resourceSha256",SHA).put("radiusM",model.getDouble("radiusM")).put("modelSha256",b.getString("intervalModelSha256064")).put("calibrationSha256",model.getString("calibrationSha256")).put("calibrationCount",60).put("sourceRelease",b.getString("sourceRelease")).put("sourcePath",b.getString("sourcePath")).put("originalRawSha256",b.getString("originalRawSha256")).put("sourceStudyId",b.getString("id"));
    }
    static JSONObject info(){return WorldFrames.copy(Loaded.INFO);}
    static double radius(){return Loaded.INFO.getDouble("radiusM");}
}
