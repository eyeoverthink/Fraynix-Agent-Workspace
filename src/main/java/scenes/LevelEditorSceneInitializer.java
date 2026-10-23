package scenes;

import components.*;
import imgui.ImGui;
import imgui.ImVec2;
import jade.*;
import org.joml.Vector2f;
import physics2dtmp.PhysicsSystem2D;
import physics2dtmp.rigidbody.Rigidbody2D;
import util.AssetPool;

public class LevelEditorSceneInitializer extends SceneInitializer {

    private Spritesheet sprites;
    private GameObject levelEditorStuff;

    public LevelEditorSceneInitializer() {

    }

    @Override
    public void init(Scene scene) {
        sprites = AssetPool.getSpritesheet("assets/images/spritesheets/decorationsAndBlocks.png");
        Spritesheet gizmos = AssetPool.getSpritesheet("assets/images/gizmos.png");

        levelEditorStuff = scene.createGameObject("LevelEditor");
        levelEditorStuff.setNoSerialize();
        levelEditorStuff.addComponent(new MouseControls());
        levelEditorStuff.addComponent(new GridLines());
        levelEditorStuff.addComponent(new EditorCamera(scene.camera()));
        levelEditorStuff.addComponent(new GizmoSystem(gizmos));
        scene.addGameObjectToScene(levelEditorStuff);
    }

    @Override
    public void loadResources(Scene scene) {
        AssetPool.getShader("assets/shaders/default.glsl");

        AssetPool.addSpritesheet("assets/images/spritesheets/decorationsAndBlocks.png",
                new Spritesheet(AssetPool.getTexture("assets/images/spritesheets/decorationsAndBlocks.png"),
                        16, 16, 81, 0));
        AssetPool.addSpritesheet("assets/images/gizmos.png",
                new Spritesheet(AssetPool.getTexture("assets/images/gizmos.png"),
                        24, 48, 3, 0));
        AssetPool.getTexture("assets/images/blendImage2.png");

        for (GameObject g : scene.getGameObjects()) {
            if (g.getComponent(SpriteRenderer.class) != null) {
                SpriteRenderer spr = g.getComponent(SpriteRenderer.class);
                if (spr.getTexture() != null) {
                    spr.setTexture(AssetPool.getTexture(spr.getTexture().getFilepath()));
                }
            }
        }
    }

    @Override
    public void imgui() {
        ImGui.begin("Editor Tools");
        ImGui.text("PLACEMENT & CAMERA");
        if (ImGui.checkbox("Show grid", util.Settings.SHOW_GRID)) util.Settings.SHOW_GRID = !util.Settings.SHOW_GRID;
        ImGui.sameLine();
        if (ImGui.checkbox("Snap to grid", util.Settings.SNAP_TO_GRID)) util.Settings.SNAP_TO_GRID = !util.Settings.SNAP_TO_GRID;
        ImGui.text("Grid cell " + util.Settings.GRID_WIDTH + " x " + util.Settings.GRID_HEIGHT + " world units");
        if (ImGui.button("Move (E)")) levelEditorStuff.getComponent(GizmoSystem.class).setUsingGizmo(0);
        ImGui.sameLine();
        if (ImGui.button("Scale (R)")) levelEditorStuff.getComponent(GizmoSystem.class).setUsingGizmo(1);
        var camera = Window.getScene().camera();
        float[] position = {camera.position.x, camera.position.y};
        if (ImGui.dragFloat2("Camera position", position, .01f)) camera.position.set(position[0], position[1]);
        float[] zoom = {camera.getZoom()};
        if (ImGui.sliderFloat("Camera zoom", zoom, .1f, 5f)) { camera.setZoom(zoom[0]); camera.adjustProjection(); }
        if (ImGui.button("Reset camera")) { camera.position.set(0, 0); camera.setZoom(1); camera.adjustProjection(); }
        ImGui.end();

        ImGui.begin("Assets");
        ImGui.text(sprites.size() + " sprites | decorationsAndBlocks.png | 16 x 16 px");
        ImGui.textDisabled("Choose a sprite, then click in the viewport to place it.");

        ImVec2 windowPos = new ImVec2();
        ImGui.getWindowPos(windowPos);
        ImVec2 windowSize = new ImVec2();
        ImGui.getWindowSize(windowSize);
        ImVec2 itemSpacing = new ImVec2();
        ImGui.getStyle().getItemSpacing(itemSpacing);

        float windowX2 = windowPos.x + windowSize.x;
        for (int i=0; i < sprites.size(); i++) {
            Sprite sprite = sprites.getSprite(i);
            float spriteWidth = sprite.getWidth() * 4;
            float spriteHeight = sprite.getHeight() * 4;
            int id = sprite.getTexId();
            Vector2f[] texCoords = sprite.getTexCoords();

            ImGui.pushID(i);
            if (ImGui.imageButton(id, spriteWidth, spriteHeight, texCoords[2].x, texCoords[0].y, texCoords[0].x, texCoords[2].y)) {
                GameObject object = Prefabs.generateSpriteObject(sprite, 0.25f, 0.25f);
                levelEditorStuff.getComponent(MouseControls.class).pickupObject(object);
            }
            if (ImGui.isItemHovered()) ImGui.setTooltip("Sprite " + i + " | 0.25 x 0.25 world units");
            ImGui.popID();

            ImVec2 lastButtonPos = new ImVec2();
            ImGui.getItemRectMax(lastButtonPos);
            float lastButtonX2 = lastButtonPos.x;
            float nextButtonX2 = lastButtonX2 + itemSpacing.x + spriteWidth;
            if (i + 1 < sprites.size() && nextButtonX2 < windowX2) {
                ImGui.sameLine();
            }
        }

        ImGui.end();
    }
}
