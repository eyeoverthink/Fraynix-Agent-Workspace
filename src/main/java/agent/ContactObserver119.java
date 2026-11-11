package agent;
import components.Ground;
import jade.GameObject;
import physics2d.components.Rigidbody2D;
import org.jbox2d.collision.WorldManifold;

/** Contact normals toward the actor; sensors and non-terrain bodies are excluded. */
public final class ContactObserver119 {
    public record Contact(boolean support,boolean opposing) {}
    public static boolean retreatSupported(GameObject actor,float direction){
        var body=actor.getComponent(Rigidbody2D.class).getRawBody();float x=actor.transform.position.x-direction*.3f,y=actor.transform.position.y;boolean[] supported={false};
        body.getWorld().raycast((f,p,n,fraction)->{if(f.isSensor()||!(f.getUserData() instanceof GameObject o)||o.getComponent(Ground.class)==null||n.y<.5f)return -1;supported[0]=true;return fraction;},new org.jbox2d.common.Vec2(x,y+.04f),new org.jbox2d.common.Vec2(x,y-1.25f));
        return supported[0];
    }
    public static Contact read(GameObject actor,float direction){
        var body=actor.getComponent(Rigidbody2D.class).getRawBody();boolean support=false,opposing=false;int count=0;
        for(var edge=body.getContactList();edge!=null&&count++<32;edge=edge.next){
            var c=edge.contact;if(!c.isTouching()||!c.isEnabled())continue;
            boolean a=c.getFixtureA().getBody()==body;var own=a?c.getFixtureA():c.getFixtureB();var other=a?c.getFixtureB():c.getFixtureA();
            if(own.isSensor()||other.isSensor()||!(other.getUserData() instanceof GameObject o)||o.getComponent(Ground.class)==null)continue;
            var m=new WorldManifold();c.getWorldManifold(m);float sign=a?-1:1;
            support|=sign*m.normal.y>.5f;opposing|=sign*m.normal.x*direction<-.5f;
        }
        return new Contact(support,opposing);
    }
}
