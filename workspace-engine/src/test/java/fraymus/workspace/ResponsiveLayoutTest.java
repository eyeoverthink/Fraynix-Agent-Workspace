package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class ResponsiveLayoutTest {
    /** Typed geometry fixtures; Electron tests independently measure actual layout. */
    static JSONArray valid(){var views=new JSONArray();for(int width:new int[]{780,390}){var headers=new JSONArray();var row=new JSONArray();var fonts=new JSONArray();var labels=List.of("series","time_s","position_m","source_kind");double column=(width-32)/4.0;for(int i=0;i<4;i++){headers.put(new JSONObject().put("text",labels.get(i)).put("visible",true).put("left",16+i*column).put("top",100).put("bottom",130));row.put(new JSONObject().put("visible",true).put("left",16+i*column).put("top",130));fonts.put(14);}for(int i=0;i<4;i++)fonts.put(14);var controls=new JSONArray();for(int i=0;i<2;i++)controls.put(new JSONObject().put("visible",true).put("left",16).put("right",150).put("height",32));views.put(new JSONObject().put("width",width).put("height",900).put("scrollWidth",width).put("table",new JSONObject().put("width",width-32).put("left",16).put("right",width-16).put("top",100)).put("headers",headers).put("row",row).put("fonts",fonts).put("controls",controls));}return views;}
    @Test public void actualDimensionsAndAlignedSemanticColumnsAreRequired(){assertTrue(ResponsiveLayout.verify(valid()).getBoolean("passed"));var only=new JSONArray().put(valid().getJSONObject(0));assertFalse(ResponsiveLayout.verify(only).getBoolean("passed"));}
    @Test public void matchingDataCountsCannotHideAnOverlappingOrDetachedHeader(){var a=valid();a.getJSONObject(1).getJSONArray("headers").getJSONObject(0).put("top",0).put("bottom",145);var result=ResponsiveLayout.verify(a);assertFalse(result.getBoolean("passed"));assertTrue(result.getJSONArray("errors").toString().contains("overlaps/misaligns"));}
    @Test public void horizontalOverflowTinyTextAndClippedControlsAreRejected(){var a=valid();a.getJSONObject(1).put("scrollWidth",800);a.getJSONObject(1).getJSONArray("fonts").put(0,8);a.getJSONObject(0).getJSONArray("controls").getJSONObject(0).put("right",900);var result=ResponsiveLayout.verify(a);assertFalse(result.getBoolean("passed"));assertTrue(result.getJSONArray("errors").length()>=3);}
    @Test public void hiddenMeasurementsDuplicateWidthsAndWrongHeaderOrderCannotPass(){var a=valid();a.getJSONObject(1).put("width",780);a.getJSONObject(0).getJSONArray("headers").getJSONObject(0).put("text","Invented");a.getJSONObject(0).getJSONArray("row").getJSONObject(0).put("visible",false);assertFalse(ResponsiveLayout.verify(a).getBoolean("passed"));}
}
