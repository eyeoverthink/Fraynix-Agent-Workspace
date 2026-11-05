package fraymus.workspace;
import org.json.*;
import java.nio.charset.StandardCharsets;
/** Exact published068 calibration. Evaluation outcomes cannot change this resource. */
final class FrozenCalibration069 {
    static final String SHA="e63750b5cd55863d612f991f413a401d9e46db59b8ab55eed401d0de88702560";
    static JSONObject model(){try(var in=FrozenCalibration069.class.getResourceAsStream("recovery-calibration069.json")){
        var m=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));ValueCalibration068.verify(m);
        if(!m.getString("sha256").equals(SHA))throw new IllegalStateException("Published calibration differs");return m;
    }catch(Exception e){throw new IllegalStateException(e);}}
}
