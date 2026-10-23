package components;

import editor.PropertiesWindow;
import jade.MouseListener;

public class TranslateGizmo extends Gizmo {
    private final org.joml.Vector2f dragPosition = new org.joml.Vector2f();
    private jade.GameObject dragged;

    public TranslateGizmo(Sprite arrowSprite, PropertiesWindow propertiesWindow) {
        super(arrowSprite, propertiesWindow);
    }

    @Override
    public void editorUpdate(float dt) {
        super.editorUpdate(dt);
        if (activeGameObject != null && (xAxisActive || yAxisActive)) {
            if (dragged != activeGameObject) { dragged = activeGameObject; dragPosition.set(activeGameObject.transform.position); }
            if (xAxisActive && !yAxisActive) {
                dragPosition.x -= MouseListener.getWorldDx();
                activeGameObject.transform.position.x = util.Settings.SNAP_TO_GRID
                    ? util.GridPlacement.nearestCenter(dragPosition.x, util.Settings.GRID_WIDTH) : dragPosition.x;
            } else if (yAxisActive) {
                dragPosition.y -= MouseListener.getWorldDy();
                activeGameObject.transform.position.y = util.Settings.SNAP_TO_GRID
                    ? util.GridPlacement.nearestCenter(dragPosition.y, util.Settings.GRID_HEIGHT) : dragPosition.y;
            }
        } else { dragged = null; }
    }
}
