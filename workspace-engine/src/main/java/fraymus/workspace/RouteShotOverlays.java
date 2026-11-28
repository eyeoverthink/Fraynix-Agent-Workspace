package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.util.Locale;

import static fraymus.workspace.SpatialScene.array;

/** Editable geometry and a deterministic bitmap typeface for portable 3D overlays. */
record RouteShotOverlays(Label label, Line line) {
    record Label(boolean enabled, String text, double[] localOffsetM, double widthM, double heightM, int color) {}
    record Line(boolean enabled, double widthM, double elevationM, int color) {}

    private static final String GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-. ";
    private static final int[][] ROWS = {
            {14,17,17,31,17,17,17},{30,17,17,30,17,17,30},{14,17,16,16,16,17,14},
            {30,17,17,17,17,17,30},{31,16,16,30,16,16,31},{31,16,16,30,16,16,16},
            {14,17,16,23,17,17,15},{17,17,17,31,17,17,17},{31,4,4,4,4,4,31},
            {7,2,2,2,2,18,12},{17,18,20,24,20,18,17},{16,16,16,16,16,16,31},
            {17,27,21,21,17,17,17},{17,25,21,19,17,17,17},{14,17,17,17,17,17,14},
            {30,17,17,30,16,16,16},{14,17,17,17,21,18,13},{30,17,17,30,20,18,17},
            {15,16,16,14,1,1,30},{31,4,4,4,4,4,4},{17,17,17,17,17,17,14},
            {17,17,17,17,17,10,4},{17,17,17,21,21,21,10},{17,17,10,4,10,17,17},
            {17,17,10,4,4,4,4},{31,1,2,4,8,16,31},{14,17,19,21,25,17,14},
            {4,12,4,4,4,4,14},{14,17,1,2,4,8,31},{30,1,1,14,1,1,30},
            {2,6,10,18,31,2,2},{31,16,16,30,1,1,30},{14,16,16,30,17,17,14},
            {31,1,2,4,8,8,8},{14,17,17,14,17,17,14},{14,17,17,15,1,1,14},
            {0,0,0,31,0,0,0},{0,0,0,0,0,12,12},{0,0,0,0,0,0,0}
    };

    static JSONObject defaults(JSONObject parent,JSONObject review) {
        var crate = LoadingAisle.find(parent,review.getString("crate"));
        String text = review.getString("crateLabel").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9. -]"," ").strip();
        if (text.length()>20) text = text.substring(0,20).strip();
        return new RouteShotOverlays(
                new Label(true,text.isEmpty()?"CRATE":text,new double[]{0,crate.getJSONArray("size").getDouble(1)/2+.13,0},.58,.15,0xffd271),
                new Line(true,.044,.018,0x63dac7)).json();
    }

    static RouteShotOverlays parse(JSONObject input) {
        SpatialScene.keys(input,"label","routeLine");
        var tag = input.getJSONObject("label");
        var guide = input.getJSONObject("routeLine");
        SpatialScene.keys(tag,"enabled","text","localOffsetM","widthM","heightM","color");
        SpatialScene.keys(guide,"enabled","widthM","elevationM","color");
        String text = tag.getString("text").strip().toUpperCase(Locale.ROOT);
        if (!text.matches("[A-Z0-9. -]{1,20}")) throw new IllegalArgumentException("Use 1–20 label characters: letters, numbers, spaces, dot or dash.");
        return new RouteShotOverlays(new Label(flag(tag),text,SpatialScene.vector(tag.getJSONArray("localOffsetM"),1.4),
                SpatialScene.number(tag,"widthM",.20,1.40),SpatialScene.number(tag,"heightM",.07,.45),color(tag)),
                new Line(flag(guide),SpatialScene.number(guide,"widthM",.012,.14),
                        SpatialScene.number(guide,"elevationM",.008,.08),color(guide)));
    }

    private static boolean flag(JSONObject row) {
        if (!(row.get("enabled") instanceof Boolean value)) throw new IllegalArgumentException("Overlay visibility must be true or false.");
        return value;
    }

    private static int color(JSONObject row) {
        String value = row.getString("color");
        if (!value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Give overlays an RGB color such as #63dac7.");
        return Integer.parseInt(value.substring(1),16);
    }

    JSONObject json() {
        return new JSONObject().put("label",new JSONObject().put("enabled",label.enabled).put("text",label.text)
                .put("localOffsetM",array(label.localOffsetM)).put("widthM",label.widthM).put("heightM",label.heightM)
                .put("color",hex(label.color))).put("routeLine",new JSONObject().put("enabled",line.enabled)
                .put("widthM",line.widthM).put("elevationM",line.elevationM).put("color",hex(line.color)));
    }

    private static String hex(int color) {
        return String.format(Locale.ROOT,"#%06x",color);
    }

    BufferedImage texture() {
        int width = 256, height = 64;
        var image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((java.awt.image.DataBufferInt)image.getRaster().getDataBuffer()).getData();
        java.util.Arrays.fill(pixels,label.color);
        for (int y=4;y<height-4;y++) java.util.Arrays.fill(pixels,y*width+4,(y+1)*width-4,0x182334);
        int columns = label.text.length()*6-1;
        int scale = Math.min(6,232/columns);
        int left = (width-columns*scale)/2, top = (height-7*scale)/2;
        for (int i=0;i<label.text.length();i++) {
            var rows = ROWS[GLYPHS.indexOf(label.text.charAt(i))];
            for (int y=0;y<7;y++) {
                for (int x=0;x<5;x++) {
                    if ((rows[y] & (1<<(4-x)))==0) continue;
                    int startX = left+(6*i+x)*scale;
                    for (int dy=0;dy<scale;dy++) {
                        int start = (top+y*scale+dy)*width+startX;
                        java.util.Arrays.fill(pixels,start,start+scale,label.color);
                    }
                }
            }
        }
        return image;
    }
}
