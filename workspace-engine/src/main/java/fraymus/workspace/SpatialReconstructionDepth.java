package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Combines registered first hits without allowing one view to erase another's surface evidence. */
final class SpatialReconstructionDepth {
    private static final int NO_HIT=-1;
    private static final int UNOBSERVED=-2;
    private static final double GRID_TOLERANCE_UM=500;

    private enum Direction {
        POSITIVE("+z","positive-grid-face",-1),
        NEGATIVE("-z","negative-grid-face",1);

        final String axis,origin;
        final int advance;

        Direction(String axis,String origin,int advance) {
            this.axis=axis;
            this.origin=origin;
            this.advance=advance;
        }

        int voxel(int n,int face) { return this==POSITIVE?n-1-face:face; }

        static Direction read(String axis) {
            return switch (axis) {
                case "+z" -> POSITIVE;
                case "-z" -> NEGATIVE;
                default -> throw new IllegalArgumentException("Depth grids support +Z and -Z orthographic rays.");
            };
        }
    }

    private record Hit(int x,int y,int z) {}
    private record Plan(Direction direction,List<Hit> hits,JSONObject proof) {}

    private SpatialReconstructionDepth() {}

    static JSONObject carve(JSONObject input,boolean[][][] hull,boolean[][] silhouette,double cellM) {
        if (Direction.read(input.getString("axis"))!=Direction.POSITIVE)
            throw new IllegalArgumentException("The single-depth format requires +Z orthographic rays.");
        Plan plan=read(input,hull,silhouette,cellM);
        return apply(List.of(plan),hull).getJSONArray("views").getJSONObject(0);
    }

    static JSONObject fuse(JSONArray inputs,boolean[][][] hull,boolean[][] silhouette,double cellM) {
        if (inputs.length()<1 || inputs.length()>2)
            throw new IllegalArgumentException("Provide one or two distinct registered depth grids.");
        var plans=new EnumMap<Direction,Plan>(Direction.class);
        for (Object raw:inputs) {
            var input=(JSONObject)raw;
            var direction=Direction.read(input.getString("axis"));
            if (plans.containsKey(direction))
                throw new IllegalArgumentException("Use each depth direction once.");
            plans.put(direction,read(input,hull,silhouette,cellM));
        }
        return apply(List.copyOf(plans.values()),hull);
    }

    private static Plan read(JSONObject input,boolean[][][] hull,boolean[][] silhouette,double cellM) {
        SpatialScene.keys(input,"axis","origin","unit","samples");
        var direction=Direction.read(input.getString("axis"));
        if (!input.getString("origin").equals(direction.origin) || !input.getString("unit").equals("um"))
            throw new IllegalArgumentException("Register depth at its facing grid plane in integer micrometres.");
        JSONArray values=input.getJSONArray("samples");
        int n=hull.length;
        if (values.length()!=n*n) throw new IllegalArgumentException("Provide one world-ordered depth sample per silhouette cell.");
        double stepUm=cellM*1_000_000;
        int unknown=0,misses=0;
        double maxResidual=0;
        var hits=new ArrayList<Hit>();
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
            int y=n-1-row, hitZ=direction.voxel(n,face);
            if (!silhouette[row][x] || !hull[x][y][hitZ])
                throw new IllegalArgumentException("Depth hit is outside the silhouette hull.");
            hits.add(new Hit(x,y,hitZ));
            maxResidual=Math.max(maxResidual,residual);
        }
        if (hits.isEmpty()) throw new IllegalArgumentException("Provide at least one observed first-surface depth in each supplied grid.");
        var proof=new JSONObject().put("axis",direction.axis).put("origin",direction.origin).put("unit","um")
                .put("samplesSha256",WorldFrames.hash(input)).put("measuredRays",hits.size()).put("unobservedRays",unknown)
                .put("backgroundRays",misses).put("gridFit","nearest-face")
                .put("maxInputGridResidualUm",maxResidual).put("inputGridToleranceUm",GRID_TOLERANCE_UM);
        return new Plan(direction,hits,proof);
    }

    private static JSONObject apply(List<Plan> plans,boolean[][][] hull) {
        int n=hull.length;
        var free=new boolean[n][n][n];
        var evidence=new JSONArray();
        for (Plan plan:plans) {
            int advance=plan.direction.advance, supportedRemovals=0;
            for (Hit hit:plan.hits) for (int z=hit.z-advance;z>=0&&z<n;z-=advance) {
                if (!hull[hit.x][hit.y][z]) continue;
                free[hit.x][hit.y][z]=true;
                supportedRemovals++;
            }
            evidence.put(plan.proof.put("removedVoxels",supportedRemovals));
        }
        for (Plan plan:plans) for (Hit hit:plan.hits) if (free[hit.x][hit.y][hit.z])
            throw new IllegalArgumentException("Conflicting depth: observed free space intersects a first hit at grid cell "+
                    hit.x+", "+hit.y+", "+hit.z+" ("+plan.direction.axis+").");
        int total=0;
        for (int x=0;x<n;x++) for (int y=0;y<n;y++) for (int z=0;z<n;z++) if (free[x][y][z]) {
            hull[x][y][z]=false;
            total++;
        }
        return new JSONObject().put("views",evidence).put("removedVoxels",total)
                .put("registration","world-x-positive-columns").put("rules","Validate all hits against the same hull before applying the combined free space.");
    }
}
