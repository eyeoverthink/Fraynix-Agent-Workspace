package components;

import jade.GameObject;
import jade.MouseListener;
import jade.Window;
import util.Settings;
import util.GridPlacement;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class MouseControls extends Component {
    private GameObject selectedAsset;
    private boolean placement;
    private long lastPress;

    public void pickupObject(GameObject go) {
        selectedAsset=go;
        selectTool();
    }

    public void place() {
        enablePlacement();
    }
    public void enablePlacement() { if(selectedAsset!=null) { placement=true; lastPress=MouseListener.pressSequence(GLFW_MOUSE_BUTTON_LEFT); } }
    public void selectTool() { placement=false; }
    public boolean placing() { return placement; }
    public boolean hasAsset() { return selectedAsset!=null; }
    public String selectedLabel() { return selectedAsset==null?"No asset selected":selectedAsset.getName(); }
    public GameObject placeAt(scenes.Scene scene,float x,float y) {
        if(!placement || selectedAsset==null) throw new IllegalStateException("Choose an asset and enable Place selected");
        GameObject object=selectedAsset.copy(); object.transform.position.set(x,y);
        if(Settings.SNAP_TO_GRID) object.transform.position.set(GridPlacement.cellCenter(x,Settings.GRID_WIDTH),GridPlacement.cellCenter(y,Settings.GRID_HEIGHT));
        scene.addGameObjectToScene(object); return object;
    }

    @Override
    public void editorUpdate(float dt) {
        if(jade.KeyListener.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)
            || MouseListener.mouseButtonDown(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT)) selectTool();
        long press=MouseListener.pressSequence(GLFW_MOUSE_BUTTON_LEFT);
        if(placement && press!=lastPress && MouseListener.isInGameViewport()) placeAt(Window.getScene(),MouseListener.getOrthoX(),MouseListener.getOrthoY());
        lastPress=press;
    }
}
