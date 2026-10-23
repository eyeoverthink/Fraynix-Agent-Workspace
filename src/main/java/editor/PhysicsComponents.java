package editor;

import jade.GameObject;
import physics2d.components.*;
import org.joml.Vector2f;

/** Idempotent editor commands shared by the visible inspector and tests. */
public final class PhysicsComponents {
    private PhysicsComponents() {}
    public static void rigidbody(GameObject object) {
        if (object.getComponent(Rigidbody2D.class) == null) {
            Rigidbody2D body = new Rigidbody2D(); body.setMass(1); object.addComponent(body);
        }
    }
    public static void box(GameObject object) {
        if (object.getComponent(Box2DCollider.class) != null || object.getComponent(CircleCollider.class) != null) return;
        Box2DCollider box = new Box2DCollider();
        box.setHalfSize(new Vector2f(Math.max(0.001f, Math.abs(object.transform.scale.x)), Math.max(0.001f, Math.abs(object.transform.scale.y))));
        object.addComponent(box);
    }
    public static void circle(GameObject object) {
        if (object.getComponent(CircleCollider.class) != null || object.getComponent(Box2DCollider.class) != null) return;
        CircleCollider circle = new CircleCollider();
        circle.setRadius(Math.max(0.001f, Math.min(Math.abs(object.transform.scale.x), Math.abs(object.transform.scale.y)) * 0.5f));
        object.addComponent(circle);
    }
}
