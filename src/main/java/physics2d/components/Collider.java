package physics2d.components;

import components.Component;
import org.joml.Vector2f;

public abstract class Collider extends Component {
    protected Vector2f offset = new Vector2f();

    public Vector2f getOffset() {
        return this.offset;
    }
    public void setOffset(Vector2f offset) { this.offset.set(offset); }

    @Override public void imgui() {
        super.imgui();
        editor.JImGui.drawVec2Control("Collider offset", offset);
    }

    protected Vector2f worldCenter(Vector2f origin) {
        float x = offset.x + origin.x, y = offset.y + origin.y;
        double angle = Math.toRadians(gameObject.transform.rotation);
        return new Vector2f(gameObject.transform.position.x + (float)(x * Math.cos(angle) - y * Math.sin(angle)),
            gameObject.transform.position.y + (float)(x * Math.sin(angle) + y * Math.cos(angle)));
    }
}
