package components;

import jade.GameObject;
import jade.Window;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;
import physics2d.components.Rigidbody2D;
import util.AssetPool;

/** A static sensor that records a pickup or a failure from actual player contact. */
public final class CourseContact extends Component {
    public enum Kind { PICKUP, HAZARD }

    public Kind kind;
    private transient boolean triggered;

    @Override public void start() {
        var body = gameObject.getComponent(Rigidbody2D.class);
        if (kind == null || body == null || !body.isSensor()) {
            throw new IllegalStateException("Course contact requires a kind and a sensor body");
        }
    }

    @Override public void beginCollision(GameObject other, Contact contact, Vector2f normal) {
        var player = other.getComponent(PlayerController.class);
        var progress = other.getComponent(CourseProgress.class);
        if (triggered || player == null || progress == null || player.isDead() || player.hasWon()) return;
        triggered = true;
        var event = new CourseProgress.ContactEvent(gameObject.getPersistentId().toString(),
                gameObject.getName(), Window.getScene().tick());
        switch (kind) {
            case PICKUP -> {
                progress.collect(event);
                gameObject.destroy();
                AssetPool.getSound("assets/sounds/coin.ogg").play();
            }
            case HAZARD -> {
                progress.hitHazard(event);
                player.die();
            }
        }
    }
}
