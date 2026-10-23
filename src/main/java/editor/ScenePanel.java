package editor;

import imgui.ImGui;
import jade.Window;
import scenes.Scene;
import physics2d.components.*;

/** Drawn from the active scene each frame, with the same selection as Properties. */
public final class ScenePanel {
    public void imgui(Scene scene, float dt) {
        ImGui.begin("Scene");
        var objects = scene.getGameObjects().stream().filter(o -> o.doSerialization() && !o.isDead()).toList();
        ImGui.text(Window.isPlaying() ? "SIMULATION RUNNING" : "SCENE EDITOR");
        ImGui.text(objects.size() + " objects | " + scene.physics().bodyCount() + " JBox2D bodies");
        ImGui.text(String.format(java.util.Locale.ROOT, "%.0f FPS | %.2f ms", ImGui.getIO().getFramerate(), dt * 1000));
        ImGui.separator();
        var properties = Window.getImguiLayer().getPropertiesWindow();
        for (var object : objects) {
            boolean rigid = object.getComponent(Rigidbody2D.class) != null;
            String collider = object.getComponent(Box2DCollider.class) != null ? "Box" : object.getComponent(CircleCollider.class) != null ? "Circle" : "";
            String tag = rigid ? " [Rigid" + (collider.isEmpty() ? "" : " / " + collider) + "]" : collider.isEmpty() ? "" : " [" + collider + "]";
            if (ImGui.selectable(object.getName() + tag + "##" + object.getPersistentId(), properties.getActiveGameObject() == object)) properties.setActiveGameObject(object);
        }
        ImGui.end();
    }
}
