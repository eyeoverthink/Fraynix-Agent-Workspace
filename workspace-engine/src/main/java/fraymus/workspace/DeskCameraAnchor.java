package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Fits a camera to labelled 3D-to-pixel correspondences and tests excluded landmarks. */
final class DeskCameraAnchor {
    static final int WIDTH = MotionMediaContainers.WIDTH;
    static final int HEIGHT = MotionMediaContainers.HEIGHT;
    private static final double LIMIT_PX = 5;

    private record Landmark(String name, double[] world, double[] pixel) {}

    record Pose(double[][] matrix, double[] centroid, double scale, double[] center, JSONObject evidence) {
        double[] project(double[] point) {
            double[] normalized = normalized(point, centroid, scale);
            double depth = dot(matrix[2], normalized) + 1;
            require(depth > .01, "A point is behind the registered camera.");
            return new double[]{WIDTH * (dot(matrix[0], normalized) + matrix[0][3]) / depth,
                    HEIGHT * (dot(matrix[1], normalized) + matrix[1][3]) / depth};
        }
    }

    private DeskCameraAnchor() {}

    static Pose fit(JSONArray training, JSONArray witness) {
        List<Landmark> fit = landmarks(training, 7);
        List<Landmark> held = landmarks(witness, 2);
        var names = new HashSet<String>();
        var worldKeys = new HashSet<String>();
        for (Landmark mark : java.util.stream.Stream.concat(fit.stream(), held.stream()).toList())
            require(names.add(mark.name) && worldKeys.add(java.util.Arrays.toString(mark.world)),
                    "A landmark was reused in calibration or held-out verification.");
        double[] center = new double[3];
        for (Landmark mark : fit) for (int axis = 0; axis < 3; axis++) center[axis] += mark.world[axis] / fit.size();
        double scale = fit.stream().mapToDouble(m -> length(minus(m.world,center))).max().orElse(0);
        require(scale > .05, "Calibration landmarks need to cover the scene.");
        var normal = new double[11][12];
        for (Landmark mark : fit) {
            double[] q = normalized(mark.world,center,scale);
            double u = mark.pixel[0] / WIDTH, v = mark.pixel[1] / HEIGHT;
            add(normal, new double[]{q[0],q[1],q[2],1,0,0,0,0,-u*q[0],-u*q[1],-u*q[2]}, u);
            add(normal, new double[]{0,0,0,0,q[0],q[1],q[2],1,-v*q[0],-v*q[1],-v*q[2]}, v);
        }
        double[] result = solve(normal);
        double[][] matrix = {{result[0],result[1],result[2],result[3]},
                {result[4],result[5],result[6],result[7]}, {result[8],result[9],result[10],1}};
        var eyeRows = new double[3][4];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(matrix[i],0,eyeRows[i],0,3);
            eyeRows[i][3] = -matrix[i][3];
        }
        double[] eye = solve(eyeRows);
        for (int axis = 0; axis < 3; axis++) eye[axis] = eye[axis]*scale+center[axis];
        require(length(minus(eye,center)) < 100, "The fitted camera is implausibly far from its landmarks.");
        var evidence = new JSONObject().put("cameraM",new JSONArray(eye)).put("tolerancePx",LIMIT_PX)
                .put("normalizedProjection",new JSONArray(matrix)).put("normalizationCenterM",new JSONArray(center))
                .put("normalizationScaleM",scale);
        Pose pose = new Pose(matrix,center,scale,eye,evidence);
        JSONObject observedFit=score(pose,fit), observedHeld=score(pose,held);
        double fitMax=observedFit.getDouble("maxPx"), heldMax=observedHeld.getDouble("maxPx");
        require(Math.max(fitMax,heldMax)<=LIMIT_PX,String.format(java.util.Locale.ROOT,
                "Landmark agreement exceeds five pixels (fit %.1f px, held-out %.1f px).",fitMax,heldMax));
        require(determinant(matrix) > 1e-9, "The fitted projection has a reflected or degenerate camera orientation.");
        evidence.put("fit",observedFit).put("heldOut",observedHeld);
        return pose;
    }

    private static double determinant(double[][] m) {
        return m[0][0] * (m[1][1]*m[2][2] - m[1][2]*m[2][1])
                - m[0][1] * (m[1][0]*m[2][2] - m[1][2]*m[2][0])
                + m[0][2] * (m[1][0]*m[2][1] - m[1][1]*m[2][0]);
    }

    private static JSONObject score(Pose pose, List<Landmark> points) {
        var rows = new JSONArray();
        double maximum = 0, square = 0;
        for (Landmark mark : points) {
            double[] projected = pose.project(mark.world);
            double error = Math.hypot(projected[0]-mark.pixel[0],projected[1]-mark.pixel[1]);
            maximum = Math.max(maximum,error);
            square += error*error;
            rows.put(new JSONObject().put("name",mark.name).put("errorPx",error)
                    .put("observedPx",new JSONArray(mark.pixel)).put("projectedPx",new JSONArray(projected)));
        }
        return new JSONObject().put("count",points.size()).put("maxPx",maximum).put("rmsPx",Math.sqrt(square/points.size())).put("rows",rows);
    }

    private static List<Landmark> landmarks(JSONArray list, int minimum) {
        require(list.length() >= minimum && list.length() <= 24, "Provide seven fit points and at least two held-out landmarks.");
        var result = new ArrayList<Landmark>();
        for (Object value : list) {
            JSONObject mark = (JSONObject)value;
            SpatialScene.keys(mark,"name","worldM","pixel");
            double[] world = SpatialScene.vector(mark.getJSONArray("worldM"), 1000);
            var image = mark.getJSONArray("pixel");
            require(image.length()==2 && image.get(0) instanceof Number && image.get(1) instanceof Number,
                    "Use numeric x and y pixel positions.");
            double x=image.getDouble(0), y=image.getDouble(1);
            require(x>=0 && x<WIDTH && y>=0 && y<HEIGHT, "A landmark is outside the recording frame.");
            result.add(new Landmark(SpatialScene.text(mark,"name",60),world,new double[]{x,y}));
        }
        return result;
    }

    private static void add(double[][] normal, double[] row, double target) {
        for (int y=0;y<row.length;y++) {
            for (int x=0;x<row.length;x++) normal[y][x]+=row[y]*row[x];
            normal[y][row.length]+=row[y]*target;
        }
    }

    private static double[] solve(double[][] a) {
        int n = a.length;
        for (int col=0;col<n;col++) {
            int pivot=col;
            for(int row=col+1;row<n;row++) if(Math.abs(a[row][col])>Math.abs(a[pivot][col])) pivot=row;
            require(Math.abs(a[pivot][col])>1e-10,"Landmarks do not constrain a camera pose. Include more than one plane.");
            var tmp=a[col];a[col]=a[pivot];a[pivot]=tmp;
            double unit=a[col][col];
            for(int j=col;j<=n;j++) a[col][j]/=unit;
            for(int row=0;row<n;row++) if(row!=col) {
                double factor=a[row][col];
                for(int j=col;j<=n;j++) a[row][j]-=factor*a[col][j];
            }
        }
        var values=new double[n];
        for(int i=0;i<n;i++) values[i]=a[i][n];
        return values;
    }

    static double[] minus(double[] a,double[] b) {return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};}

    static double length(double[] v) {return Math.sqrt(dot(v,v));}

    private static double dot(double[] a,double[] b) {return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}

    private static double[] normalized(double[] p,double[] center,double scale) {
        return new double[]{(p[0]-center[0])/scale,(p[1]-center[1])/scale,(p[2]-center[2])/scale};
    }

    private static void require(boolean valid,String message) {
        if(!valid) throw new IllegalArgumentException(message);
    }
}
