package fraymus.workspace;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.ManifoldPoints;
import com.jme3.bullet.collision.PersistentManifolds;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static fraymus.workspace.ObserverGeometry.*;

/** Tests straight and in-place turns with one native gripped body and a fixed-height crossing. */
final class SpatialCarry {
    private static final double LIFT_M = .18;
    private static final double POSITION_ERROR_M = .02;
    private static final double ANGLE_ERROR_DEG = 2;
    private static final double CONTACT_M = -.002;
    private static final double CLEARANCE_M = .008;
    private static final int SETTLE = 240;
    private static final double[][] AXES = {{1,0,0},{0,1,0},{0,0,1}};
    static final String POLICY = "spatial-doorway-carry-v1";

    private record Pose(double[] p, JSONArray rotation) {
        static Pose authored(JSONObject object) {
            return new Pose(v(object.getJSONArray("position")), object.getJSONArray("rotation"));
        }

        static Pose measured(PhysicsRigidBody body) {
            var q = body.getPhysicsRotation();
            return new Pose(v(SpatialPhysics.json(body.getPhysicsLocation())),
                    new JSONArray(new double[]{q.getX(), q.getY(), q.getZ(), q.getW()}));
        }

        static Pose upright(double[] position, double yaw) {
            return new Pose(position, new JSONArray(new double[]{0,Math.sin(yaw/2),0,Math.cos(yaw/2)}));
        }

        double yaw() {
            double x=rotation.getDouble(0), y=rotation.getDouble(1), z=rotation.getDouble(2), w=rotation.getDouble(3);
            return Math.atan2(2*(w*y+x*z),1-2*(y*y+x*x));
        }
    }

    private record Box(double[] half, double[] offset) {
        static Box collider(JSONObject object) {
            return new Box(mul(v(object.getJSONArray("size")), .5),new double[3]);
        }

        static Box visual(JSONObject scene, JSONObject object) {
            if (!object.has("visualMesh")) return collider(object);
            var vertices = SpatialGlb.asset(scene,object).positions();
            var lo = new double[]{vertices[0],vertices[1],vertices[2]};
            var hi = lo.clone();
            for (int i=3;i<vertices.length;i++) {
                int axis=i%3;
                lo[axis]=Math.min(lo[axis],vertices[i]);
                hi[axis]=Math.max(hi[axis],vertices[i]);
            }
            return new Box(mul(sub(hi,lo),.5),mul(add(hi,lo),.5));
        }

        Oriented at(Pose pose) {
            var axes = new double[3][];
            for (int i=0;i<3;i++) axes[i]=unit(rotate(AXES[i],pose.rotation(),false));
            return new Oriented(add(pose.p(),rotate(offset,pose.rotation(),false)),axes,half);
        }

        double turnRadius() { return norm(add(half,new double[]{Math.abs(offset[0]),Math.abs(offset[1]),Math.abs(offset[2])})); }
    }

    private record Oriented(double[] center,double[][] axes,double[] half) {
        private double radius(double[] axis) {
            double radius=0;
            for (int i=0;i<3;i++) radius+=half[i]*Math.abs(dot(axes[i],axis));
            return radius;
        }

        /** The largest separating-axis gap is a conservative clearance, in metres. */
        double gap(Oriented other) {
            var delta=sub(other.center,center);
            double gap=Double.NEGATIVE_INFINITY;
            for (int i=0;i<3;i++) {
                gap=Math.max(gap,gapOn(other,delta,axes[i]));
                gap=Math.max(gap,gapOn(other,delta,other.axes[i]));
            }
            for (var u:axes) for (var b:other.axes) {
                var axis=new double[]{u[1]*b[2]-u[2]*b[1],u[2]*b[0]-u[0]*b[2],u[0]*b[1]-u[1]*b[0]};
                if (norm(axis)>1e-7) gap=Math.max(gap,gapOn(other,delta,unit(axis)));
            }
            return gap;
        }

        private double gapOn(Oriented other,double[] delta,double[] axis) {
            return Math.abs(dot(delta,axis))-radius(axis)-other.radius(axis);
        }
    }

    private record Obstacle(String id, String name, Oriented collider, Oriented visual) {}
    private record Clearance(double collider, double visual, String closest, String closestVisual) {}
    private record Course(JSONObject scene, JSONObject prop, JSONObject floor, Box collider,Box visual,List<Obstacle> obstacles,
                          Pose start, double[] target) {
        static Course read(JSONObject scene, JSONObject request) {
            var prop=SpatialScene.object(scene,request.getString("objectId"));
            var start=Pose.authored(prop);
            var delta=SpatialScene.vector(request.getJSONArray("displacementM"),6);
            if (norm(delta)<.5 || norm(delta)>6 || delta[1]!=0)
                throw new IllegalArgumentException("Choose a horizontal carry of 0.5 to 6 m.");
            var target=add(start.p(),delta);
            for (double coordinate:target) RampDefinition.range(coordinate,-12,12,"carry target");
            if (prop.getDouble("massKg")<=0 || !upright(start))
                throw new IllegalArgumentException("Choose an upright dynamic body.");
            var collider=Box.collider(prop);
            var visual=Box.visual(scene,prop);
            var obstacles=new ArrayList<Obstacle>();
            JSONObject floor=null;
            for (Object raw:scene.getJSONArray("objects")) {
                var object=(JSONObject)raw;
                String id=object.getString("id");
                if (id.equals(prop.getString("id"))) continue;
                if (object.getDouble("massKg")>0) throw new IllegalArgumentException("Carry review needs one moving body.");
                var pose=Pose.authored(object);
                var bounds=Box.collider(object);
                if (isFloor(object,pose,bounds,start,target,Math.max(collider.turnRadius(),visual.turnRadius()),collider.half()[1])) {
                    if (floor!=null) throw new IllegalArgumentException("Choose a scene with one continuous support deck.");
                    floor=object;
                } else {
                    obstacles.add(new Obstacle(id,object.getString("sourceId"),bounds.at(pose),Box.visual(scene,object).at(pose)));
                }
            }
            if (floor==null || obstacles.isEmpty())
                throw new IllegalArgumentException("Carry review needs a continuous flat deck and an obstacle.");
            if (visual.offset()[1]-visual.half()[1] < -collider.half()[1]-.002)
                throw new IllegalArgumentException("The visible prop extends below its resting collider.");
            return new Course(scene,prop,floor,collider,visual,List.copyOf(obstacles),start,target);
        }

        private static boolean isFloor(JSONObject object, Pose pose, Box box, Pose start,double[] target,double radius,double halfHeight) {
            if (object.has("visualMesh") || !upright(pose) || Math.abs(Math.IEEEremainder(pose.yaw(),2*Math.PI))>1e-6
                    || Math.abs(pose.p()[1]+box.half()[1]-(start.p()[1]-halfHeight))>.004) return false;
            for (int i:new int[]{0,2}) {
                if (Math.max(Math.abs(start.p()[i]-pose.p()[i]),Math.abs(target[i]-pose.p()[i]))+radius>box.half()[i]) return false;
            }
            return true;
        }

        Clearance clearance(Pose pose) {
            var body=collider.at(pose);
            var surface=visual.at(pose);
            double minimumBox=Double.POSITIVE_INFINITY, minimumMesh=Double.POSITIVE_INFINITY;
            String closest="", closestVisual="";
            for (var obstacle:obstacles) {
                double box=body.gap(obstacle.collider()), mesh=surface.gap(obstacle.visual());
                if (box<minimumBox) { minimumBox=box; closest=obstacle.name(); }
                if (mesh<minimumMesh) { minimumMesh=mesh; closestVisual=obstacle.name(); }
            }
            return new Clearance(minimumBox,minimumMesh,closest,closestVisual);
        }
    }

    static JSONObject forecast(JSONObject scene, JSONObject request,Path nativeDir) throws Exception {
        var course=Course.read(scene,request);
        var trials=new JSONArray();
        String selected="";
        for (int yaw:new int[]{0,90,-90}) {
            var trial=simulate(course,yaw,nativeDir);
            trials.put(trial);
            if (selected.isEmpty() && trial.getBoolean("passed")) selected=trial.getString("route");
        }
        return new JSONObject().put("policy",POLICY).put("trials",trials)
                .put("selectedRoute",selected).put("passed",!selected.isEmpty())
                .put("positionToleranceM",POSITION_ERROR_M).put("angleToleranceDeg",ANGLE_ERROR_DEG)
                .put("settleTicks",SETTLE).put("minimumClearanceM",CLEARANCE_M);
    }

    static JSONObject replay(JSONObject scene,JSONObject request,JSONObject forecast,Path nativeDir) throws Exception {
        String route=forecast.getString("selectedRoute");
        for (Object raw:forecast.getJSONArray("trials")) {
            var expected=(JSONObject)raw;
            if (!route.equals(expected.getString("route")) || !expected.getBoolean("passed")) continue;
            var actual=simulate(Course.read(scene,request),expected.getInt("turnDeg"),nativeDir);
            if (!expected.similar(actual)) throw new IllegalStateException("Fresh native carry replay differs from the approved forecast.");
            return actual;
        }
        throw new IllegalStateException("No verified route is available to approve.");
    }

    private static JSONObject simulate(Course course,int turn,Path nativeDir) throws Exception {
        try (var world=new SpatialPhysics(course.scene,nativeDir)) {
            PhysicsSpace.setLocalThreadPhysicsSpace(world.space);
            return new Trial(course,world,turn).run();
        }
    }

    private record Leg(String phase,Pose destination,int ticks,double yawChange) {
        static Leg create(String phase,Pose source,Pose destination) {
            double angle=Math.IEEEremainder(destination.yaw()-source.yaw(),2*Math.PI);
            double seconds=Math.max(norm(sub(destination.p(),source.p()))/.9,Math.abs(angle)/1.4);
            return new Leg(phase,destination,Math.max(1,(int)Math.ceil(seconds/BulletRamp.DT)),angle);
        }
    }

    private static final class Trial {
        private final Course course;
        private final SpatialPhysics world;
        private final PhysicsRigidBody body;
        private final int turn;
        private final long deckBody;
        private final JSONArray samples=new JSONArray();
        private JSONObject contact, firstClash, firstCommandedWarning;
        private double minCollider=Double.POSITIVE_INFINITY, minVisual=Double.POSITIVE_INFINITY;
        private double minCommandedCollider=Double.POSITIVE_INFINITY, minCommandedVisual=Double.POSITIVE_INFINITY;

        Trial(Course course,SpatialPhysics world,int turn) {
            this.course=course;
            this.world=world;
            this.body=world.bodies.get(course.prop.getString("id"));
            this.turn=turn;
            this.deckBody=world.bodies.get(course.floor.getString("id")).nativeId();
        }

        JSONObject run() {
            sample("start",true);
            for (int i=0;i<72 && contact==null;i++) step("rest");
            int settled=0, stableTicks=0;
            if (contact==null) {
                world.grab(course.prop.getString("id"));
                body.setAngularFactor(new Vector3f(0,1,0));
                var origin=course.start;
                double yaw=origin.yaw(), crossing=yaw+Math.toRadians(turn);
                var up=new double[]{0,LIFT_M,0};
                var moves=List.of(
                        Pose.upright(add(origin.p(),up),yaw),
                        Pose.upright(add(origin.p(),up),crossing),
                        Pose.upright(add(course.target,up),crossing),
                        Pose.upright(add(course.target,up),yaw),
                        Pose.upright(course.target,yaw));
                var phases=List.of("lift","turn","carry","restore yaw","lower");
                for (int i=0;i<moves.size() && contact==null;i++) {
                    var leg=Leg.create(phases.get(i),origin,moves.get(i));
                    move(origin,leg);
                    origin=leg.destination;
                }
                release();
                while (settled<SETTLE && contact==null) {
                    step("settle");
                    settled++;
                    if (onDeck() && body.getLinearVelocity().length()<=.04 && body.getAngularVelocity().length()<=.04)
                        stableTicks++;
                    else stableTicks=0;
                }
            }
            sample("final",true);
            var pose=Pose.measured(body);
            double positionError=norm(sub(pose.p(),course.target));
            double angleError=angleBetween(pose.rotation(),course.start.rotation());
            double speed=body.getLinearVelocity().length(), angularSpeed=body.getAngularVelocity().length();
            boolean support=onDeck();
            boolean clear=minCollider>=CLEARANCE_M && minVisual>=CLEARANCE_M
                    && minCommandedCollider>=CLEARANCE_M && minCommandedVisual>=CLEARANCE_M;
            boolean passed=contact==null && settled==SETTLE && stableTicks>=120 && support && clear && positionError<=POSITION_ERROR_M
                    && angleError<=ANGLE_ERROR_DEG && speed<=.04 && angularSpeed<=.04;
            String reason=passed?"Supported at the target pose":contact!=null?"Unexpected native contact":
                    !clear?"Requested clearance was unproven":"Final pose or support failed";
            return new JSONObject().put("route",turn==0?"straight":turn>0?"turn +90°":"turn −90°")
                    .put("turnDeg",turn).put("passed",passed).put("reason",reason)
                    .put("firstUnexpectedContact",contact==null?JSONObject.NULL:contact)
                    .put("firstClearanceWarning",firstClash==null?JSONObject.NULL:firstClash)
                    .put("firstCommandedWarning",firstCommandedWarning==null?JSONObject.NULL:firstCommandedWarning)
                    .put("minColliderM",minCollider).put("minVisualEnvelopeM",minVisual)
                    .put("minCommandedColliderM",finiteOrNull(minCommandedCollider))
                    .put("minCommandedVisualEnvelopeM",finiteOrNull(minCommandedVisual))
                    .put("finalPositionM",new JSONArray(pose.p())).put("finalRotation",pose.rotation())
                    .put("positionErrorM",positionError).put("angleErrorDeg",angleError).put("support",support)
                    .put("linearSpeedMps",speed).put("angularSpeedRadps",angularSpeed)
                    .put("ticks",world.tick).put("settleTicks",settled).put("consecutiveStableTicks",stableTicks).put("samples",samples);
        }

        private void move(Pose from,Leg leg) {
            var delta=sub(leg.destination.p(),from.p());
            double bound=norm(delta)/(2*leg.ticks);
            double rotation= Math.abs(leg.yawChange)/(2*leg.ticks);
            for (int i=1;i<=leg.ticks+96 && contact==null;i++) {
                double fraction=Math.min(1,i/(double)leg.ticks);
                if (i<=leg.ticks) {
                    double mid=(i-.5)/leg.ticks;
                    var swept=course.clearance(Pose.upright(add(from.p(),mul(delta,mid)),from.yaw()+leg.yawChange*mid));
                    double collider=swept.collider()-bound-course.collider.turnRadius()*rotation;
                    double visual=swept.visual()-bound-course.visual.turnRadius()*rotation;
                    minCommandedCollider=Math.min(minCommandedCollider,collider);
                    minCommandedVisual=Math.min(minCommandedVisual,visual);
                    if (firstCommandedWarning==null && Math.min(collider,visual)<CLEARANCE_M)
                        firstCommandedWarning=warning(leg.phase,world.tick+1,collider,visual,swept);
                }
                var target=add(from.p(),mul(delta,fraction));
                double targetYaw=from.yaw()+leg.yawChange*fraction;
                var pose=Pose.measured(body);
                double yawError=Math.IEEEremainder(targetYaw-pose.yaw(),2*Math.PI);
                body.setAngularVelocity(new Vector3f(0,(float)Math.clamp(18*yawError,-2,2),0));
                world.grip.setPivotInB(SpatialPhysics.vec(target));
                world.anchor=target;
                step(leg.phase);
            }
        }

        private void release() {
            world.release();
            body.setAngularFactor(new Vector3f(1,1,1));
        }

        private void step(String phase) {
            if (Thread.currentThread().isInterrupted() || world.tick>=6000)
                throw new IllegalStateException("Carry exceeded its execution budget.");
            world.step();
            sample(phase,false);
        }

        private void sample(String phase,boolean force) {
            var pose=Pose.measured(body);
            var clearance=course.clearance(pose);
            minCollider=Math.min(minCollider,clearance.collider());
            minVisual=Math.min(minVisual,clearance.visual());
            if (firstClash==null && Math.min(clearance.collider(),clearance.visual())<CLEARANCE_M) {
                firstClash=warning(phase,world.tick,clearance.collider(),clearance.visual(),clearance);
            }
            if (contact==null) contact=obstacleImpact(phase);
            if (contact==null && clearance.collider()<=.015) {
                for (var obstacle:course.obstacles) {
                    float depth=penetration(world.bodies.get(obstacle.id()));
                    if (depth<CONTACT_M) {
                        contact=new JSONObject().put("tick",world.tick).put("phase",phase)
                                .put("object",course.prop.getString("sourceId")).put("obstacle",obstacle.name()).put("depthM",-depth);
                        break;
                    }
                }
            }
            if (force || world.tick%16==0 || contact!=null && contact.getInt("tick")==world.tick) {
                samples.put(new JSONObject().put("tick",world.tick).put("phase",phase).put("gripped",world.grip!=null)
                        .put("positionM",new JSONArray(pose.p())).put("rotation",pose.rotation())
                        .put("angularSpeedRadps",body.getAngularVelocity().length())
                        .put("colliderM",clearance.collider()).put("visualEnvelopeM",clearance.visual()));
            }
        }

        private JSONObject warning(String phase,int tick,double collider,double visual,Clearance clearance) {
            boolean box=collider<=visual;
            return new JSONObject().put("tick",tick).put("phase",phase)
                    .put("obstacle",box?clearance.closest():clearance.closestVisual())
                    .put("envelope",box?"collider":"visual").put("gapM",Math.min(collider,visual));
        }

        private float penetration(PhysicsRigidBody other) {
            float[] depth={0};
            world.space.pairTest(body,other,event->depth[0]=Math.min(depth[0],event.getDistance1()));
            return depth[0];
        }

        private JSONObject obstacleImpact(String phase) {
            for (long manifold:world.space.listManifoldIds()) {
                long a=PersistentManifolds.getBodyAId(manifold), b=PersistentManifolds.getBodyBId(manifold);
                long other=a==body.nativeId()?b:b==body.nativeId()?a:0;
                if (other==0 || other==deckBody) continue;
                for (long point:PersistentManifolds.listPointIds(manifold)) {
                    float impulse=ManifoldPoints.getAppliedImpulse(point);
                    float distance=ManifoldPoints.getDistance1(point);
                    if (impulse<=1e-5f || distance>.006f) continue;
                    for (var obstacle:course.obstacles) {
                        if (world.bodies.get(obstacle.id()).nativeId()==other)
                            return new JSONObject().put("tick",world.tick).put("phase",phase)
                                    .put("object",course.prop.getString("sourceId")).put("obstacle",obstacle.name())
                                    .put("depthM",Math.max(0,-distance)).put("impulseNs",impulse);
                    }
                }
            }
            return null;
        }

        private boolean onDeck() {
            var normal=new Vector3f();
            for (long manifold:world.space.listManifoldIds()) {
                long a=PersistentManifolds.getBodyAId(manifold), b=PersistentManifolds.getBodyBId(manifold);
                if (!(a==body.nativeId() && b==deckBody || b==body.nativeId() && a==deckBody)) continue;
                for (long point:PersistentManifolds.listPointIds(manifold)) {
                    ManifoldPoints.getNormalWorldOnB(point,normal);
                    double up=a==body.nativeId()?normal.y:-normal.y;
                    float distance=ManifoldPoints.getDistance1(point);
                    if (up>.7 && Math.abs(distance)<=.006f && ManifoldPoints.getAppliedImpulse(point)>1e-6f)
                        return true;
                }
            }
            return false;
        }
    }

    private static boolean upright(Pose pose) {
        return norm(sub(rotate(AXES[1],pose.rotation(),false),AXES[1]))<1e-6;
    }

    private static double angleBetween(JSONArray a,JSONArray b) {
        double aa=0,bb=0,ab=0;
        for (int i=0;i<4;i++) {
            double x=a.getDouble(i),y=b.getDouble(i);
            aa+=x*x;
            bb+=y*y;
            ab+=x*y;
        }
        return Math.toDegrees(2*Math.acos(Math.min(1,Math.abs(ab)/Math.sqrt(aa*bb))));
    }

    private static Object finiteOrNull(double value) { return Double.isFinite(value)?value:JSONObject.NULL; }
}
