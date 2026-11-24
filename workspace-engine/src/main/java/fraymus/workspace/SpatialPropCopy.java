package fraymus.workspace;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.Arrays;

/** Reviewed authored duplication, followed by fresh native contact and support tests in isolated worlds. */
final class SpatialPropCopy {
    private static final int SETTLE_TICKS = 240;
    private static final double PROBE_RADIUS = .035;

    private SpatialPropCopy() {}

    static JSONObject review(JSONObject parent, JSONObject args) {
        SpatialScene.keys(args,"sceneId","objectId","label","offsetM","yawDeg");
        var body=SpatialScene.object(parent,args.getString("objectId"));
        if (!body.has("visualMesh") || body.getDouble("massKg")<=0)
            throw new IllegalArgumentException("Select a dynamic prop with a separate visual surface in Spatial.");
        var delta=SpatialScene.vector(args.getJSONArray("offsetM"),6);
        if (ObserverGeometry.norm(delta)<.05 || ObserverGeometry.norm(delta)>6)
            throw new IllegalArgumentException("Place the copy 0.05–6 m from the original.");
        String label=ExplanationEvidence.required(args.getString("label"),80,"a copy name");
        double yaw=SpatialScene.number(args,"yawDeg",-180,180);
        var target=ObserverGeometry.add(ObserverGeometry.v(body.getJSONArray("position")),delta);
        var angle=Math.toRadians(yaw)/2;
        double s=Math.sin(angle), c=Math.cos(angle);
        var q=body.getJSONArray("rotation");
        double x=q.getDouble(0),y=q.getDouble(1),z=q.getDouble(2),w=q.getDouble(3);
        var rotation=new double[]{c*x+s*z,c*y+s*w,c*z-s*x,c*w-s*y};
        double norm=Math.sqrt(Arrays.stream(rotation).map(d->d*d).sum());
        for (int i=0;i<rotation.length;i++) rotation[i]/=norm;
        return new JSONObject().put("source",binding(parent)).put("objectId",body.getString("id"))
                .put("sourceId",body.getString("sourceId")).put("label",label).put("offsetM",new JSONArray(delta)).put("yawDeg",yaw)
                .put("targetM",new JSONArray(target)).put("rotation",new JSONArray(rotation)).put("meshSha256",body.getString("visualMesh"))
                .put("colliderSizeM",body.getJSONArray("size")).put("settleTicks",SETTLE_TICKS);
    }

    static JSONObject binding(JSONObject original) {
        return new JSONObject().put("sceneId",original.getString("id")).put("sourceSha256",original.getString("sourceSha256"))
                .put("definitionSha256",WorldFrames.hash(original));
    }

    static JSONObject definition(JSONObject parent,JSONObject review) {
        var source=new JSONObject(parent.getString("sourceText"));
        var input=source.getJSONArray("objects");
        JSONObject original=null;
        for (Object item : input) {
            var object=(JSONObject)item;
            if (object.getString("sourceId").equals(review.getString("sourceId"))) original=object;
        }
        if (original==null) throw new IllegalStateException("The source prop is missing.");
        var copy=WorldFrames.copy(original).put("sourceId",copyKey(review)).put("label",review.getString("label"))
                .put("position",review.getJSONArray("targetM")).put("rotation",review.getJSONArray("rotation"));
        input.put(copy);
        return source.put("title","Prop copy · "+review.getString("label"));
    }

    static String copyKey(JSONObject review) { return "prop-copy-" + WorldFrames.hash(review).substring(0,20); }

    static JSONObject verifyNative(JSONObject scene,JSONObject review,Path nativeDir) throws Exception {
        JSONObject prop=null;
        for (Object item:scene.getJSONArray("objects")) {
            var o=(JSONObject)item;
            if (o.getString("sourceId").equals(copyKey(review))) prop=o;
        }
        if (prop==null) throw new IllegalStateException("The reviewed copy is missing.");
        String id=prop.getString("id");
        var initial=ObserverGeometry.v(prop.getJSONArray("position"));
        var size=ObserverGeometry.v(prop.getJSONArray("size"));
        var mesh=SpatialGlb.asset(scene,prop);
        var overhang=mesh.overhang(size);
        try (var world=new SpatialPhysics(scene,nativeDir)) {
            PhysicsSpace.setLocalThreadPhysicsSpace(world.space);
            var body=world.bodies.get(id);
            for (var other : world.bodies.entrySet()) {
                if (!other.getKey().equals(id) && overlap(world,body,other.getValue(),-.008f)>0)
                    throw new IllegalStateException("Native collision refuses this placement. The copy intersects an existing collider.");
            }
            double inset=Math.min(PROBE_RADIUS,Math.min(size[0],Math.min(size[1],size[2]))/4);
            var probe=new PhysicsRigidBody(new BoxCollisionShape((float)inset),0);
            probe.setPhysicsLocation(SpatialPhysics.vec(initial));
            int core=overlap(world,body,probe,-.001f);
            int visual=0;
            if (overhang!=null) {
                var position=ObserverGeometry.add(initial, ObserverGeometry.rotate(overhang,prop.getJSONArray("rotation"),false));
                probe.setPhysicsLocation(SpatialPhysics.vec(position));
                visual=overlap(world,body,probe,-.001f);
            }
            if (core==0 || visual!=0) throw new IllegalStateException("Native collider probes did not match the declared geometry.");
            for (int i=0;i<SETTLE_TICKS;i++) world.step();
            var finalPosition=ObserverGeometry.v(SpatialPhysics.json(body.getPhysicsLocation()));
            String support=null;
            for (var item:world.bodies.entrySet()) {
                var other=item.getValue();
                if (other==body || other.getMass()>0) continue;
                int[] contacts={0};
                world.space.pairTest(body,other,event->{
                    double normal=event.getNormalWorldOnB().y;
                    if (event.getObjectA()!=body) normal=-normal;
                    if (normal>.7 && event.getDistance1()<=.01f) contacts[0]++;
                });
                if (contacts[0]>0) {support=SpatialScene.object(scene,item.getKey()).getString("sourceId");break;}
            }
            double drift=ObserverGeometry.norm(ObserverGeometry.sub(initial,finalPosition));
            double speed=body.getLinearVelocity().length();
            if (support==null || drift>.06 || speed>.06)
                throw new IllegalStateException("The copy needs stable support at the reviewed pose.");
            return new JSONObject().put("passed",true).put("objectId",id).put("copySourceId",copyKey(review))
                    .put("meshSha256",mesh.key()).put("visualTriangles",mesh.indices().length/3)
                    .put("collider","BoxCollisionShape").put("coreContacts",core).put("visualOnlyContacts",visual)
                    .put("visualOverhangTested",overhang!=null).put("support",support)
                    .put("ticks",SETTLE_TICKS).put("settledPositionM",new JSONArray(finalPosition)).put("driftM",drift).put("speedMps",speed);
        }
    }

    private static int overlap(SpatialPhysics world,PhysicsRigidBody first,PhysicsRigidBody second,float clearance) {
        int[] count={0};
        world.space.pairTest(first,second,event->{if (event.getDistance1()<=clearance) count[0]++;});
        return count[0];
    }
}
