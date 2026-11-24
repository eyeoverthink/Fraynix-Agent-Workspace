package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

/** Free-space carving by registered first hits from the positive-Z edge of the search cube. */
final class SpatialReconstructionDepth {
    private static final int NO_HIT=-1;
    private static final int UNOBSERVED=-2;
    private static final double GRID_TOLERANCE_UM=500;

    private SpatialReconstructionDepth() {}

    static JSONObject carve(JSONObject input,boolean[][][] hull,boolean[][] silhouette,double cellM) {
        SpatialScene.keys(input,"axis","origin","unit","samples");
        if (!input.getString("axis").equals("+z") || !input.getString("origin").equals("positive-grid-face")
                || !input.getString("unit").equals("um"))
            throw new IllegalArgumentException("Depth needs +Z orthographic rays from the positive grid face, in micrometres.");
        JSONArray values=input.getJSONArray("samples");
        int n=hull.length;
        if (values.length()!=n*n) throw new IllegalArgumentException("Provide one row-major depth sample per positive-Z silhouette cell.");
        double stepUm=cellM*1_000_000;
        int hits=0,unknown=0,misses=0,removed=0;
        double maxResidual=0;
        for (int row=0;row<n;row++) for (int x=0;x<n;x++) {
            var value=values.get(row*n+x);
            if (!(value instanceof Number number)) throw new IllegalArgumentException("Use integer depth samples.");
            double raw=number.doubleValue();
            if (!Double.isFinite(raw) || raw!=Math.rint(raw) || raw<UNOBSERVED || raw>=n*stepUm)
                throw new IllegalArgumentException("Depth sample is outside the search cube.");
            if (raw==UNOBSERVED) {
                unknown++;
                continue;
            }
            if (raw==NO_HIT) {
                if (silhouette[row][x]) throw new IllegalArgumentException("Depth declares empty space at a silhouette foreground pixel.");
                misses++;
                continue;
            }
            int face=(int)Math.round(raw/stepUm);
            double residual=Math.abs(raw-face*stepUm);
            if (face<=0 || face>=n || residual>GRID_TOLERANCE_UM)
                throw new IllegalArgumentException("Depth does not align with a supported interior grid face.");
            int y=n-1-row;
            int hitZ=n-1-face;
            if (!silhouette[row][x] || !hull[x][y][hitZ])
                throw new IllegalArgumentException("Depth hit is outside the silhouette hull.");
            for (int z=hitZ+1;z<n;z++) {
                if (hull[x][y][z]) {
                    hull[x][y][z]=false;
                    removed++;
                }
            }
            hits++;
            maxResidual=Math.max(maxResidual,residual);
        }
        if (hits==0) throw new IllegalArgumentException("Provide at least one observed first-surface depth.");
        return new JSONObject().put("axis","+z").put("origin","positive-grid-face").put("unit","um")
                .put("samplesSha256",WorldFrames.hash(input)).put("measuredRays",hits).put("unobservedRays",unknown)
                .put("backgroundRays",misses).put("removedVoxels",removed)
                .put("gridFit","nearest-face").put("maxInputGridResidualUm",maxResidual).put("inputGridToleranceUm",GRID_TOLERANCE_UM);
    }
}
