package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Validates actual Chromium observations, independently of generated CSS or claims. */
final class ResponsiveLayout {
    static final String PROFILE="responsive-v1";
    static JSONObject verify(JSONArray views){
        var errors=new JSONArray();if(views.length()!=2)errors.put("Both780px and390px actual viewport observations are required");
        var widths=new HashSet<Integer>();
        for(Object raw:views){var v=(JSONObject)raw;SpatialScene.keys(v,"width","height","scrollWidth","table","headers","row","controls","fonts","error");int width=v.getInt("width");if(!Set.of(780,390).contains(width)||!widths.add(width))errors.put("Unexpected or duplicate viewport width "+width);if(v.optString("error").length()>0){errors.put(width+"px: "+v.getString("error"));continue;}
            if(v.getInt("scrollWidth")>width+1)errors.put(width+"px: document width "+v.getInt("scrollWidth")+" overflows viewport; constrain SVG and wrap table cells");
            var table=v.getJSONObject("table");if(table.getDouble("width")<=0||table.getDouble("left")< -1||table.getDouble("right")>width+1)errors.put(width+"px: evidence table is outside the viewport");
            var headers=v.getJSONArray("headers");var row=v.getJSONArray("row");var names=List.of("series","time_s","position_m","source_kind");if(headers.length()!=4||row.length()!=4)errors.put(width+"px: use four actual semantic header/body columns inside TABLE id=data-table with thead and tbody; never put the ID on tbody or a div");
            for(int i=0;i<Math.min(4,Math.min(headers.length(),row.length()));i++){
                var h=headers.getJSONObject(i);var cell=row.getJSONObject(i);if(!h.getString("text").equals(names.get(i)))errors.put(width+"px: source header order changed");
                if(!h.getBoolean("visible")||!cell.getBoolean("visible")||h.getDouble("top")<table.getDouble("top")-1||h.getDouble("bottom")>cell.getDouble("top")+1||Math.abs(h.getDouble("left")-cell.getDouble("left"))>2)errors.put(width+"px: header column "+names.get(i)+" overlaps/misaligns the first data row; use a normal table and sticky th, never an absolute thead");
            }
            var controls=v.getJSONArray("controls");if(controls.length()!=2)errors.put(width+"px: filter and reset measurements missing");for(Object c:controls){var control=(JSONObject)c;if(!control.getBoolean("visible")||control.getDouble("left")< -1||control.getDouble("right")>width+1||control.getDouble("height")<24)errors.put(width+"px: filter/reset is hidden, clipped or too small: height="+control.getDouble("height")+"px, bounds="+control.getDouble("left")+".."+control.getDouble("right")+". Use input,button font:inherit;padding:8px;min-height:32px");}
            var fonts=v.getJSONArray("fonts");if(fonts.length()!=8)errors.put(width+"px: table legibility measurements missing");for(Object f:fonts)if(!(f instanceof Number)||!Double.isFinite(((Number)f).doubleValue())||((Number)f).doubleValue()<12)errors.put(width+"px: table text must be at least12 CSS pixels");
        }
        return new JSONObject().put("profile",PROFILE).put("passed",errors.isEmpty()).put("errors",errors).put("views",views);
    }
}
