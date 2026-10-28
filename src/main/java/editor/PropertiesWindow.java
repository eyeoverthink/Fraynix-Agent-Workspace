package editor;

import components.NonPickable;
import imgui.ImGui;
import jade.GameObject;
import jade.MouseListener;
import physics2d.components.Box2DCollider;
import physics2d.components.CircleCollider;
import physics2d.components.Rigidbody2D;
import renderer.PickingTexture;
import scenes.Scene;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class PropertiesWindow {
    private GameObject activeGameObject = null;
    private PickingTexture pickingTexture;

    private float debounce = 0.2f;

    public PropertiesWindow(PickingTexture pickingTexture) {
        this.pickingTexture = pickingTexture;
    }

    public void update(float dt, Scene currentScene) {
        debounce -= dt;
        var tools=currentScene.getGameObjectWith(components.MouseControls.class);
        if(tools!=null && tools.getComponent(components.MouseControls.class).placing()) return;
        if (!MouseListener.isInGameViewport()) return;

        if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) && debounce < 0) {
            int x = (int)MouseListener.getScreenX();
            int y = (int)MouseListener.getScreenY();
            int gameObjectId = pickingTexture.readPixel(x, y);
            GameObject pickedObj = currentScene.getGameObject(gameObjectId);
            if (pickedObj != null && pickedObj.getComponent(NonPickable.class) == null) {
                activeGameObject = pickedObj;
            } else if (pickedObj == null && !MouseListener.isDragging()) {
                activeGameObject = null;
            }
            this.debounce = 0.2f;
        }
    }

    public void imgui() {
        ImGui.begin("Properties");
        Scene scene = jade.Window.getScene();
        if (ImGui.beginCombo("Object", activeGameObject == null ? "Select a scene object" : activeGameObject.getName())) {
            for (GameObject object : scene.getGameObjects()) {
                if (!object.doSerialization() || object.isDead()) continue;
                if (ImGui.selectable(object.getName() + "##" + object.getUid(), object == activeGameObject)) activeGameObject = object;
            }
            ImGui.endCombo();
        }
        ImGui.text("JBox2D physics | bodies: " + scene.physics().bodyCount());
        if (!scene.physics().editorError().isEmpty()) ImGui.textWrapped(scene.physics().editorError());
        if (activeGameObject != null) {
            if (ImGui.button("Add Component")) ImGui.openPopup("ComponentAdder");
            if (ImGui.button("Rigidbody2D + Box2D")) {
                PhysicsComponents.rigidbody(activeGameObject);
                PhysicsComponents.box(activeGameObject);
            }

            if (ImGui.beginPopupContextWindow("ComponentAdder")) {
                if (ImGui.menuItem("Add Rigidbody")) {
                    if (activeGameObject.getComponent(Rigidbody2D.class) == null) {
                        PhysicsComponents.rigidbody(activeGameObject);
                    }
                }

                if (ImGui.menuItem("Add Box Collider")) {
                    if (activeGameObject.getComponent(Box2DCollider.class) == null &&
                            activeGameObject.getComponent(CircleCollider.class) == null) {
                        PhysicsComponents.box(activeGameObject);
                    }
                }

                if (ImGui.menuItem("Add Circle Collider")) {
                    if (activeGameObject.getComponent(CircleCollider.class) == null &&
                            activeGameObject.getComponent(Box2DCollider.class) == null) {
                        PhysicsComponents.circle(activeGameObject);
                    }
                }

                ImGui.endPopup();
            }

            activeGameObject.imgui();
        } else {
            ImGui.textWrapped("Select an object in the viewport or the Object list to inspect Transform, Rigidbody2D, and collider settings.");
        }
        ImGui.end();
    }

    public GameObject getActiveGameObject() {
        return this.activeGameObject;
    }

    public void setActiveGameObject(GameObject go) {
        this.activeGameObject = go;
    }
}
