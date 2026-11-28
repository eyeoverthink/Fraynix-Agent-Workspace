package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static fraymus.workspace.ObserverGeometry.*;
import static fraymus.workspace.SpatialScene.array;

/** A local camera, oriented-box rasterizer and depth-tested graphics for measured routes. */
final class RouteShot {
    static final int WIDTH = MotionMediaContainers.WIDTH;
    static final int HEIGHT = MotionMediaContainers.HEIGHT;
    static final int FPS = MotionMediaContainers.FPS;
    static final int FRAMES = FPS * 6;
    static final String FORMAT = "fraynix.loading-route-shot-v1";
    static final String EDITABLE_FORMAT = "fraynix.loading-route-shot-v2";
    private static final double NEAR = .05;
    private static final double[] UP = {0,1,0};
    private static final int[][] FACES = {
            {0,2,6,4},{1,5,7,3},{0,4,5,1},{2,3,7,6},{0,1,3,2},{4,6,7,5}
    };
    private static final double[] LIGHT = {.73,.86,.62,1,.79,.91};

    record Key(int frame, double[] offset, double fov) {}
    record Vertex(double x, double y, double z, double u, double v) {}
    record Pixel(double x, double y, double iz, double uiz, double viz) {}
    record Take(BufferedImage clean, BufferedImage annotated, JSONObject proof) {}

    private record Camera(double[] eye, double[] forward, double[] right, double[] up, double focal) {
        Vertex vertex(double[] point, double u, double v) {
            double[] delta = sub(point,eye);
            return new Vertex(dot(delta,right),dot(delta,up),dot(delta,forward),u,v);
        }

        Pixel project(Vertex point) {
            double iz = 1/point.z;
            return new Pixel(WIDTH/2.0 + focal*point.x*iz, HEIGHT/2.0 - focal*point.y*iz,
                    iz, point.u*iz, point.v*iz);
        }

        double[] ray(int x,int y) {
            return add(forward,add(mul(right,(x+.5-WIDTH/2.0)/focal),mul(up,(HEIGHT/2.0-y-.5)/focal)));
        }
    }

    private final JSONArray boxes;
    private final String crateId;
    private final Set<String> shelves;
    private final double[] forward, right;
    private final double floor, crateHeight;
    private final double[][] path;
    private final double[] distance;
    private final List<Key> keys;
    private final BufferedImage label;
    private final JSONArray crateRotation;
    private final RouteShotOverlays overlays;

    RouteShot(JSONObject scene, JSONObject review, JSONArray cameraKeys,RouteShotOverlays overlays) {
        if (scene.has("visualMeshes"))
            throw new IllegalArgumentException("The route shot supports box-geometry bays.");
        boxes = scene.getJSONArray("objects");
        crateId = review.getString("crate");
        var crate = LoadingAisle.find(scene,crateId);
        crateHeight = crate.getJSONArray("size").getDouble(1);
        crateRotation = crate.getJSONArray("rotation");
        this.overlays = overlays;
        forward = v(review.getJSONArray("directionM"));
        right = new double[]{forward[2],0,-forward[0]};
        floor = review.getJSONObject("aisle").getDouble("floorY");
        var protectedRows = review.getJSONArray("protected");
        var protectedShelves = new java.util.HashSet<String>();
        for (Object raw : protectedRows) {
            String id = ((JSONObject)raw).getString("sourceId");
            if (overlays!=null || LoadingAisle.find(scene,id).getString("label").toLowerCase(Locale.ROOT).contains("shelf"))
                protectedShelves.add(id);
        }
        shelves = Set.copyOf(protectedShelves);
        var points = review.getJSONObject("route").getJSONArray("pointsM");
        path = new double[points.length()][];
        distance = new double[path.length];
        for (int i=0;i<path.length;i++) {
            path[i] = v(points.getJSONArray(i));
            if (i>0) distance[i] = distance[i-1] + norm(sub(path[i],path[i-1]));
        }
        if (path.length<2 || distance[path.length-1] < .01)
            throw new IllegalArgumentException("A verified route with measurable travel is required.");
        keys = readKeys(cameraKeys);
        label = overlays==null?label(review.getString("crateLabel")):overlays.texture();
    }

    static JSONArray defaultKeys(JSONObject review) {
        var direction = v(review.getJSONArray("directionM"));
        var aisle = review.getJSONObject("aisle");
        double[] across = {direction[2],0,-direction[0]};
        double side = dot(sub(v(review.getJSONArray("fromM")),v(aisle.getJSONArray("entryM"))),across)<0?-1:1;
        return new JSONArray()
                .put(key(0,side*1.3,5.2,-1.4,49))
                .put(key(46,side*4.35,.82,.3,38))
                .put(key(84,side*4.35,.75,.48,37))
                .put(key(FRAMES-1,side*.9,1.7,2.45,44));
    }

    private static JSONObject key(int frame,double x,double y,double z,double fov) {
        return new JSONObject().put("frame",frame).put("offsetM",new JSONArray(new double[]{x,y,z})).put("fovDeg",fov);
    }

    static List<Key> readKeys(JSONArray input) {
        if (input.length()<2 || input.length()>8)
            throw new IllegalArgumentException("Use 2–8 camera keys.");
        var keys = new ArrayList<Key>();
        int previous = -1;
        for (Object value : input) {
            if (!(value instanceof JSONObject row)) throw new IllegalArgumentException("A camera key must be an object.");
            SpatialScene.keys(row,"frame","offsetM","fovDeg");
            double frame = SpatialScene.number(row,"frame",0,FRAMES-1);
            if (frame != Math.rint(frame) || frame <= previous)
                throw new IllegalArgumentException("Camera key frames must be unique increasing integers.");
            double[] offset = SpatialScene.vector(row.getJSONArray("offsetM"),9);
            if (offset[1] < .35) throw new IllegalArgumentException("Keep camera keys at least 0.35 m above the crate center.");
            keys.add(new Key((int)frame,offset,SpatialScene.number(row,"fovDeg",25,80)));
            previous = (int)frame;
        }
        if (keys.getFirst().frame != 0 || previous != FRAMES-1)
            throw new IllegalArgumentException("The camera track needs keys at frames 0 and 143.");
        return keys;
    }

    private double[] position(int frame) {
        if (frame==0) return path[0];
        if (frame==FRAMES-1) return path[path.length-1];
        double at = frame * distance[distance.length-1] / (FRAMES-1);
        int end = 1;
        while (end<distance.length-1 && distance[end]<at) end++;
        double span = distance[end]-distance[end-1];
        return interpolate(path[end-1],path[end],span>0?(at-distance[end-1])/span:0);
    }

    private Camera camera(int frame, double[] target) {
        int end = 1;
        while (end<keys.size()-1 && keys.get(end).frame<frame) end++;
        var a = keys.get(end-1);
        var b = keys.get(end);
        double t = (frame-a.frame)/(double)(b.frame-a.frame);
        t = t*t*(3-2*t);
        double[] local = interpolate(a.offset,b.offset,t);
        double[] eye = add(target,add(mul(right,local[0]),add(mul(UP,local[1]),mul(forward,local[2]))));
        var aim = add(target,mul(UP,crateHeight*.36));
        var view = unit(sub(aim,eye));
        var horizontal = cross(view,UP);
        var across = norm(horizontal)>1e-8 ? unit(horizontal) : right;
        double fov = a.fov + t*(b.fov-a.fov);
        return new Camera(eye,view,across,cross(across,view),HEIGHT/2.0/Math.tan(Math.toRadians(fov)/2));
    }

    Take frame(int frame) {
        if (frame<0 || frame>=FRAMES) throw new IllegalArgumentException("Choose frame 0–143.");
        double[] center = position(frame);
        Camera camera = camera(frame,center);
        Render render = new Render(camera,overlays!=null);
        var moved = new JSONArray();
        for (Object raw : boxes) {
            var box = (JSONObject) raw;
            String id = box.getString("sourceId");
            if (id.equals(crateId)) box = new JSONObject(box.toString()).put("position",array(center));
            moved.put(box);
            render.box(box,color(box));
        }
        var clean = new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        System.arraycopy(render.rgb,0,pixels(clean),0,render.rgb.length);
        boolean labelEnabled = overlays==null || overlays.label().enabled();
        if (overlays==null || overlays.line().enabled()) drawRoute(render);
        double[] localAnchor = overlays==null?new double[]{0,crateHeight/2+.13,0}:overlays.label().localOffsetM();
        double[] anchor = add(center,rotate(localAnchor,crateRotation,false));
        double[] labelCenter = overlays==null?add(center,mul(UP,crateHeight/2+.13)):anchor;
        int x = -1, y = -1;
        double nearest = 0;
        if (labelEnabled) {
            double width = overlays==null?.58:overlays.label().widthM();
            double height = overlays==null?.15:overlays.label().heightM();
            var edgeX = mul(camera.right,width/2);
            var edgeY = mul(camera.up,height/2);
            render.quad(new double[][]{add(sub(labelCenter,edgeX),edgeY),add(add(labelCenter,edgeX),edgeY),
                    sub(add(labelCenter,edgeX),edgeY),sub(sub(labelCenter,edgeX),edgeY)},0,label);
            var point = camera.vertex(labelCenter,0,0);
            var sample = camera.project(point);
            x = (int)Math.floor(sample.x);
            y = (int)Math.floor(sample.y);
            if (point.z<NEAR || x<0 || x>=WIDTH || y<0 || y>=HEIGHT)
                throw new IllegalArgumentException("Keep the label's attachment point inside the camera frame.");
            nearest = point.z-1e-7;
        }
        int labelPixels = 0;
        for (boolean pixel : render.labelMask) if (pixel) labelPixels++;
        String blocker = "";
        var ray = labelEnabled?camera.ray(x,y):null;
        for (Object raw : moved) {
            var box = (JSONObject)raw;
            var local = rotate(sub(camera.eye,v(box.getJSONArray("position"))),box.getJSONArray("rotation"),true);
            var size = v(box.getJSONArray("size"));
            if (Math.abs(local[0])<size[0]/2 && Math.abs(local[1])<size[1]/2 && Math.abs(local[2])<size[2]/2)
                throw new IllegalArgumentException("The camera track crosses "+box.getString("label")+". Move a camera key.");
            double hit = ray==null?Double.POSITIVE_INFINITY:hit(box,camera.eye,ray);
            if (hit<nearest) {
                nearest = hit;
                blocker = box.getString("sourceId");
            }
        }
        boolean centerVisible = labelEnabled && render.labelMask[y*WIDTH+x];
        if (labelEnabled && centerVisible != blocker.isEmpty())
            throw new IllegalStateException("Depth raster and independent box ray disagree at frame "+frame+".");
        var proof = new JSONObject().put("frame",frame).put("timeSeconds",frame/(double)FPS)
                .put("cratePositionM",array(center)).put("cameraPositionM",array(camera.eye))
                .put("labelSamplePx",new JSONArray(new int[]{x,y})).put("labelPixels",labelPixels)
                .put("centerVisible",centerVisible).put("centerOccluder",blocker)
                .put("fullyHiddenByShelf",labelPixels==0 && shelves.contains(blocker));
        if (overlays!=null) proof.put("labelEnabled",labelEnabled).put("labelAnchorM",array(anchor))
                .put("routePixels",render.routePixels());
        return new Take(clean,render.image,proof);
    }

    private void drawRoute(Render render) {
        double elevation = overlays==null?.018:overlays.line().elevationM();
        double halfWidth = overlays==null?.022:overlays.line().widthM()/2;
        int color = overlays==null?0x63dac7:overlays.line().color();
        render.drawingRoute = true;
        for (int i=1;i<path.length;i++) {
            double[] a = {path[i-1][0],floor+elevation,path[i-1][2]};
            double[] b = {path[i][0],floor+elevation,path[i][2]};
            double[] delta = sub(b,a);
            if (norm(delta)<1e-8) continue;
            double[] edge = mul(unit(cross(delta,UP)),halfWidth);
            render.quad(new double[][]{add(a,edge),add(b,edge),sub(b,edge),sub(a,edge)},color,null);
        }
        render.drawingRoute = false;
    }

    private int color(JSONObject box) {
        String id = box.getString("sourceId");
        if (id.equals(crateId)) return id.equals("flight-case")?0xe7a44e:0x428cf4;
        if (shelves.contains(id)) return 0x7886a0;
        return switch (id) {
            case "floor" -> 0x40566b;
            case "door" -> 0x8099b0;
            case "spare" -> 0xd97263;
            default -> 0x586580;
        };
    }

    private static BufferedImage label(String text) {
        var image = new BufferedImage(232,60,BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            g.setColor(new Color(0xffd271));
            g.fillRect(0,0,232,60);
            g.setColor(new Color(0x263143));
            g.fillRect(3,3,226,54);
            g.setColor(new Color(0xffe19a));
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            String name = text.toUpperCase(Locale.ROOT);
            if (name.length()>22) name = name.substring(0,21)+"…";
            int size = 28;
            do { g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,size--)); }
            while (size>11 && g.getFontMetrics().stringWidth(name)>206);
            g.drawString(name,(232-g.getFontMetrics().stringWidth(name))/2,39);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static double[] cross(double[] a,double[] b) {
        return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};
    }

    private static double[] interpolate(double[] a,double[] b,double t) {
        return add(a,mul(sub(b,a),t));
    }

    private static int[] pixels(BufferedImage image) {
        return ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
    }

    private static int shade(int color,double amount) {
        return ((int)((color>>16&255)*amount)<<16) | ((int)((color>>8&255)*amount)<<8) | (int)((color&255)*amount);
    }

    private static final class Render {
        final Camera camera;
        final BufferedImage image = new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        final int[] rgb = pixels(image);
        final float[] depth = new float[WIDTH*HEIGHT];
        final boolean[] labelMask = new boolean[WIDTH*HEIGHT];

        final boolean[] routeMask;
        boolean drawingRoute;

        Render(Camera camera,boolean auditRoute) {
            this.camera = camera;
            routeMask = auditRoute?new boolean[WIDTH*HEIGHT]:null;
            Arrays.fill(rgb,0x182333);
        }

        int routePixels() {
            int count = 0;
            for (boolean pixel : routeMask) if (pixel) count++;
            return count;
        }

        void box(JSONObject box,int color) {
            var center = v(box.getJSONArray("position"));
            var size = v(box.getJSONArray("size"));
            var q = box.getJSONArray("rotation");
            var points = new double[8][];
            for (int i=0;i<8;i++) {
                double[] local = {(i&1)==0?-size[0]/2:size[0]/2,(i&2)==0?-size[1]/2:size[1]/2,
                        (i&4)==0?-size[2]/2:size[2]/2};
                points[i] = add(center,rotate(local,q,false));
            }
            for (int i=0;i<FACES.length;i++) {
                int[] f = FACES[i];
                quad(new double[][]{points[f[0]],points[f[1]],points[f[2]],points[f[3]]},shade(color,LIGHT[i]),null);
            }
        }

        void quad(double[][] points,int color,BufferedImage texture) {
            var input = new Vertex[]{camera.vertex(points[0],0,0),camera.vertex(points[1],1,0),
                    camera.vertex(points[2],1,1),camera.vertex(points[3],0,1)};
            var clipped = new ArrayList<Pixel>(5);
            Vertex previous = input[3];
            for (Vertex next : input) {
                if ((previous.z>=NEAR)!=(next.z>=NEAR)) {
                    double t = (NEAR-previous.z)/(next.z-previous.z);
                    var crossing = new Vertex(previous.x+(next.x-previous.x)*t,previous.y+(next.y-previous.y)*t,
                            NEAR,previous.u+(next.u-previous.u)*t,previous.v+(next.v-previous.v)*t);
                    clipped.add(camera.project(crossing));
                }
                if (next.z>=NEAR) clipped.add(camera.project(next));
                previous = next;
            }
            for (int i=2;i<clipped.size();i++) triangle(clipped.getFirst(),clipped.get(i-1),clipped.get(i),color,texture);
        }

        private static double edge(Pixel a,Pixel b,double x,double y) {
            return (b.x-a.x)*(y-a.y)-(b.y-a.y)*(x-a.x);
        }

        void triangle(Pixel a,Pixel b,Pixel c,int color,BufferedImage texture) {
            double area = edge(a,b,c.x,c.y);
            if (Math.abs(area)<1e-9) return;
            double inverseArea = 1/area;
            int x0 = Math.max(0,(int)Math.floor(Math.min(a.x,Math.min(b.x,c.x))));
            int x1 = Math.min(WIDTH-1,(int)Math.ceil(Math.max(a.x,Math.max(b.x,c.x))));
            int y0 = Math.max(0,(int)Math.floor(Math.min(a.y,Math.min(b.y,c.y))));
            int y1 = Math.min(HEIGHT-1,(int)Math.ceil(Math.max(a.y,Math.max(b.y,c.y))));
            for (int y=y0;y<=y1;y++) {
                for (int x=x0;x<=x1;x++) {
                    double w0 = edge(b,c,x+.5,y+.5)*inverseArea;
                    double w1 = edge(c,a,x+.5,y+.5)*inverseArea;
                    double w2 = 1-w0-w1;
                    if (Math.min(w0,Math.min(w1,w2)) < -1e-9) continue;
                    double iz = a.iz*w0+b.iz*w1+c.iz*w2;
                    int index = y*WIDTH+x;
                    if (iz<=depth[index]) continue;
                    depth[index] = (float)iz;
                    if (routeMask!=null) routeMask[index] = drawingRoute;
                    if (texture!=null) {
                        int u = (int)((a.uiz*w0+b.uiz*w1+c.uiz*w2)/iz*(texture.getWidth()-1));
                        int v = (int)((a.viz*w0+b.viz*w1+c.viz*w2)/iz*(texture.getHeight()-1));
                        rgb[index] = texture.getRGB(u,v);
                        labelMask[index] = true;
                    } else {
                        rgb[index] = color;
                    }
                }
            }
        }
    }
}
