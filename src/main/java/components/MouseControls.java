package components;

import jade.GameObject;
import jade.MouseListener;
import jade.Window;
import util.Settings;
import util.GridPlacement;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class MouseControls extends Component {
    GameObject holdingObject = null;

    public void pickupObject(GameObject go) {
        this.holdingObject = go;
        Window.getScene().addGameObjectToScene(go);
    }

    public void place() {
        this.holdingObject = null;
    }

    @Override
    public void editorUpdate(float dt) {
        if (holdingObject != null) {
            if (!MouseListener.isInGameViewport()) return;
            holdingObject.transform.position.x = MouseListener.getOrthoX();
            holdingObject.transform.position.y = MouseListener.getOrthoY();
            if (Settings.SNAP_TO_GRID) {
                holdingObject.transform.position.x = GridPlacement.cellCenter(holdingObject.transform.position.x, Settings.GRID_WIDTH);
                holdingObject.transform.position.y = GridPlacement.cellCenter(holdingObject.transform.position.y, Settings.GRID_HEIGHT);
            }

            if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
                place();
            }
        }
    }
}
