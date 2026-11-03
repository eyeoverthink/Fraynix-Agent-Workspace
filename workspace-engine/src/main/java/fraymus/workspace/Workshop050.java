package fraymus.workspace;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.*;
import org.json.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Bounded construction and native experiments. No model-written executable code. */
final class Workshop050 {
    static final String PROCEDURE="construction-workshop-050-v1";
    static final int TICKS=2880, STRIDE=24;
    private final Path root,nativeDir;
    Workshop050(Path project){root=project.resolve("workshop-050");nativeDir=project.resolve("native");}
    static JSONObject config(JSONObject input){
        SpatialScene.keys(input,"frictionScale","startShiftM","massKg","rampStock","blockStock","wallStock");
        var c=new JSONObject().put("frictionScale",1).put("startShiftM",0).put("massKg",1).put("rampStock",1).put("blockStock",3).put("wallStock",3);
        for(String k:input.keySet())c.put(k,input.get(k));
        SpatialScene.number(c,"frictionScale",.5,1.8);SpatialScene.number(c,"startShiftM",-.35,.35);SpatialScene.number(c,"massKg",.4,2);
        for(String k:List.of("rampStock","blockStock","wallStock")){double n=SpatialScene.number(c,k,0,12);if(n!=Math.rint(n))throw new IllegalArgumentException("Stock must be a whole count");}
        if(c.getInt("rampStock")<1||c.getInt("blockStock")<3||c.getInt("wallStock")<3)throw new IllegalArgumentException("Assembly needs 1 ramp, 3 blocks and 3 bin walls; no parts are created to fill a shortage");
        return c;
    }
    static JSONObject design(String material,double angle){
        if(!Set.of("smooth","rough").contains(material))throw new IllegalArgumentException("Unknown surface");
        RampDefinition.range(angle,15,40,"ramp angle");
        return new JSONObject().put("material",material).put("angleDeg",angle).put("friction",material.equals("smooth")?.15:.65).put("materialCost",material.equals("smooth")?2:1);
    }
    static String identity()throws Exception{
        try(var in=Workshop050.class.getResourceAsStream("Workshop050.class")){return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));}
    }
    static JSONObject prediction(JSONObject c,JSONObject d){
        double a=Math.toRadians(d.getDouble("angleDeg")),mu=d.getDouble("friction")*c.getDouble("frictionScale");
        double acceleration=Math.max(0,9.81*(Math.sin(a)-mu*Math.cos(a))),length=4.8-c.getDouble("startShiftM");
        return new JSONObject().put("method","Coulomb sliding on an inclined plane; no landing/collision forecast").put("accelerationMps2",acceleration).put("rampTravelM",length).put("predictedStall",acceleration==0).put("rampExitTimeS",acceleration==0?JSONObject.NULL:Math.sqrt(2*length/acceleration));
    }
    static JSONObject specification(JSONObject c,JSONObject d){
        c=config(c);d=design(d.getString("material"),d.getDouble("angleDeg"));
        return new JSONObject().put("schema",PROCEDURE).put("config",c).put("design",d).put("prediction",prediction(c,d));
    }
    static JSONArray geometry(JSONObject spec){
        var c=spec.getJSONObject("config");var d=spec.getJSONObject("design");double a=Math.toRadians(d.getDouble("angleDeg")),cy=3*Math.sin(a)+.15,end=3*Math.cos(a);
        var q=new JSONArray(new double[]{0,0,-Math.sin(a/2),Math.cos(a/2)});var iq=new JSONArray(new double[]{0,0,0,1});var objects=new JSONArray();
        objects.put(RampDefinition.object("ground","Ground",new double[]{0,-.1,0},iq,new double[]{20,.2,8},0));
        objects.put(RampDefinition.object("ramp","Assembled ramp",new double[]{0,cy,0},q,new double[]{6,.2,3},0));
        objects.put(RampDefinition.object("back","Bin end",new double[]{end+2,.6,0},iq,new double[]{.2,1.2,3.2},0));
        for(int i:new int[]{-1,1})objects.put(RampDefinition.object("side"+i,"Bin side",new double[]{end+1,.6,i*1.6},iq,new double[]{2,1.2,.2},0));
        for(int i=0;i<3;i++){double s=-1.8+c.getDouble("startShiftM");objects.put(RampDefinition.object("block-"+i,"Block "+(i+1),new double[]{s*Math.cos(a)+.301*Math.sin(a),cy-s*Math.sin(a)+.301*Math.cos(a),(i-1)*.8},q,new double[]{.4,.4,.4},c.getDouble("massKg")));}
        return objects;
    }
    JSONObject prepare(String id,JSONObject c,JSONObject d)throws Exception{
        var spec=specification(c,d);String code=identity();var expected=new JSONObject().put("id",id).put("codeSha256",code).put("specification",spec);
        Path p=path(id,"prediction");if(Files.exists(p)){var stored=readEnvelope(p);if(!WorldFrames.hash(expected).equals(WorldFrames.hash(stored)))throw new IllegalStateException("Trial prediction binding changed");return stored;}
        write(p,expected);return expected;
    }
    JSONObject run(String id)throws Exception{
        var before=readEnvelope(path(id,"prediction"));if(!before.getString("codeSha256").equals(identity()))throw new IllegalStateException("Workshop implementation changed");
        Path result=path(id,"result");if(Files.exists(result))return read(id);
        var spec=before.getJSONObject("specification");var objects=geometry(spec);BulletRamp.load(nativeDir);
        var space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);space.setGravity(new Vector3f(0,-9.81f,0));space.setAccuracy((float)BulletRamp.DT);
        var bodies=new LinkedHashMap<String,PhysicsRigidBody>();var samples=new JSONArray();
        try{
            for(Object raw:objects){var o=(JSONObject)raw;var s=o.getJSONArray("size");var b=new PhysicsRigidBody(new BoxCollisionShape(s.getFloat(0)/2,s.getFloat(1)/2,s.getFloat(2)/2),o.getFloat("massKg"));var p=o.getJSONArray("position");var q=o.getJSONArray("rotation");b.setPhysicsLocation(new Vector3f(p.getFloat(0),p.getFloat(1),p.getFloat(2)));b.setPhysicsRotation(new Quaternion(q.getFloat(0),q.getFloat(1),q.getFloat(2),q.getFloat(3)));b.setRestitution(0);b.setFriction(o.getDouble("massKg")==0?1:(float)(spec.getJSONObject("design").getDouble("friction")*spec.getJSONObject("config").getDouble("frictionScale")));if(b.getMass()>0){b.setDamping(0,0);b.setSleepingThresholds(0,0);b.setCcdMotionThreshold(.05f);b.setCcdSweptSphereRadius(.15f);}space.addCollisionObject(b);bodies.put(o.getString("id"),b);}
            for(int tick=0;tick<=TICKS;tick++){
                if(Thread.currentThread().isInterrupted())throw new InterruptedException("Workshop trial interrupted; no completed result stored");
                if(tick%STRIDE==0){var states=new JSONArray();for(int i=0;i<3;i++){var b=bodies.get("block-"+i);var p=b.getPhysicsLocation();var v=b.getLinearVelocity();var q=b.getPhysicsRotation();int[] contacts={0};space.contactTest(b,e->{if(e.getDistance1()<=.01f)contacts[0]++;});states.put(new JSONObject().put("id","block-"+i).put("position",SpatialPhysics.json(p)).put("velocity",SpatialPhysics.json(v)).put("rotation",new JSONArray(new float[]{q.getX(),q.getY(),q.getZ(),q.getW()})).put("contacts",contacts[0]));}samples.put(new JSONObject().put("tick",tick).put("timeS",tick*BulletRamp.DT).put("bodies",states));}
                if(tick<TICKS){PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)BulletRamp.DT,0);}
            }
        }finally{space.destroy();}
        var out=new JSONObject().put("id",id).put("codeSha256",before.getString("codeSha256")).put("predictionSha256",WorldFrames.hash(before)).put("specification",spec).put("objects",objects).put("samples",samples).put("backend",BulletRamp.BACKEND).put("scope","Disposable native simulation; declared materials, not measured physical calibration");
        out.put("verification",verify(out));write(result,out);return out;
    }
    static JSONObject verify(JSONObject r){
        var spec=r.getJSONObject("specification");var expected=specification(spec.getJSONObject("config"),spec.getJSONObject("design"));if(!WorldFrames.hash(spec).equals(WorldFrames.hash(expected))||!r.getJSONArray("objects").similar(geometry(spec)))throw new IllegalStateException("Assembly or prediction specification differs");
        var samples=r.getJSONArray("samples");if(samples.length()!=TICKS/STRIDE+1)throw new IllegalStateException("Incomplete native observation horizon");
        double angle=Math.toRadians(spec.getJSONObject("design").getDouble("angleDeg")),end=3*Math.cos(angle);int delivered=0;double squares=0;int n=0;
        var initial=samples.getJSONObject(0).getJSONArray("bodies");
        for(int k=0;k<samples.length();k++){var s=samples.getJSONObject(k);if(s.getInt("tick")!=k*STRIDE||Math.abs(s.getDouble("timeS")-k*STRIDE*BulletRamp.DT)>1e-9)throw new IllegalStateException("Trace clock changed");var b=s.getJSONArray("bodies");if(b.length()!=3)throw new IllegalStateException("Three original bodies required");
            for(int i=0;i<3;i++){var body=b.getJSONObject(i);if(!body.getString("id").equals("block-"+i))throw new IllegalStateException("Body identity changed");double[] p=SpatialScene.vector(body.getJSONArray("position"),30),v=SpatialScene.vector(body.getJSONArray("velocity"),100);var q=body.getJSONArray("rotation");if(q.length()!=4)throw new IllegalStateException("Rotation missing");for(Object x:q)if(!(x instanceof Number num)||!Double.isFinite(num.doubleValue()))throw new IllegalStateException("Nonfinite rotation");if(body.getInt("contacts")<0)throw new IllegalStateException("Invalid contact count");
                if(k==samples.length()-1&&p[0]>end+.1&&p[0]<end+1.8&&p[1]>.1&&p[1]<.5&&Math.abs(p[2])<1.3&&Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2])<.12)delivered++;
                var start=initial.getJSONObject(i).getJSONArray("position");double distance=(p[0]-start.getDouble(0))*Math.cos(angle)-(p[1]-start.getDouble(1))*Math.sin(angle),t=s.getDouble("timeS"),predicted=.5*spec.getJSONObject("prediction").getDouble("accelerationMps2")*t*t;
                if(t>0&&predicted<spec.getJSONObject("prediction").getDouble("rampTravelM")-.4&&distance<spec.getJSONObject("prediction").getDouble("rampTravelM")-.4){squares+=(distance-predicted)*(distance-predicted);n++;}
            }
        }
        return new JSONObject().put("delivered",delivered).put("passed",delivered==3).put("rampRmseM",n==0?JSONObject.NULL:Math.sqrt(squares/n)).put("predictionComparisons",n).put("nativeTicks",TICKS).put("reason",delivered==3?"All three original blocks settled inside the collection bin":"Delivery incomplete; retain this trial and revise within the budget");
    }
    JSONObject read(String id)throws Exception{
        var r=readEnvelope(path(id,"result"));var before=readEnvelope(path(id,"prediction"));if(!r.getString("predictionSha256").equals(WorldFrames.hash(before))||!WorldFrames.hash(r.getJSONObject("specification")).equals(WorldFrames.hash(before.getJSONObject("specification"))))throw new IllegalStateException("Prediction receipt changed");
        if(!WorldFrames.hash(verify(r)).equals(WorldFrames.hash(r.getJSONObject("verification"))))throw new IllegalStateException("Outcome differs from retained trace");return r;
    }
    private Path path(String id,String suffix){UUID.fromString(id);return root.resolve(id+"-"+suffix+".json");}
    private static JSONObject readEnvelope(Path file)throws Exception{if(Files.size(file)>2_000_000)throw new IllegalStateException("Workshop evidence exceeds bound");var e=new JSONObject(Files.readString(file));var v=e.getJSONObject("value");if(!WorldFrames.hash(v).equals(e.getString("sha256")))throw new IllegalStateException("Workshop evidence checksum changed; original retained");return v;}
    private static void write(Path file,JSONObject value)throws Exception{Files.createDirectories(file.getParent());var bytes=new JSONObject().put("value",value).put("sha256",WorldFrames.hash(value)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);Path pending=Files.createTempFile(file.getParent(),"workshop-",".pending");try{try(var ch=FileChannel.open(pending,StandardOpenOption.WRITE)){var b=ByteBuffer.wrap(bytes);while(b.hasRemaining())ch.write(b);ch.force(true);}Files.move(pending,file,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(pending);}}
}
