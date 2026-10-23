package editor;

import jade.Camera;
import org.joml.Vector2f;
import org.joml.Vector4f;

public final class ViewportCoordinates {
    private ViewportCoordinates() {}
    public static Vector2f world(float mouseX, float mouseY, Vector2f origin, Vector2f size, Camera camera) {
        if (size.x <= 0 || size.y <= 0) return new Vector2f(camera.position);
        camera.adjustProjection();
        camera.getViewMatrix();
        Vector4f point = new Vector4f((mouseX - origin.x) / size.x * 2 - 1,
            1 - (mouseY - origin.y) / size.y * 2, 0, 1);
        point.mul(camera.getInverseProjection()).mul(camera.getInverseView());
        return new Vector2f(point.x, point.y);
    }
}
