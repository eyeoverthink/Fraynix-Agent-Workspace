package jade;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class KeyListener {
    private static KeyListener instance;
    private boolean keyPressed[] = new boolean[350];
    private boolean keyBeginPress[] = new boolean[350];

    private KeyListener() {

    }

    public static KeyListener get() {
        if (KeyListener.instance == null) {
            KeyListener.instance = new KeyListener();
        }

        return KeyListener.instance;
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (key < 0 || key >= get().keyPressed.length) return;
        if (action == GLFW_PRESS) {
            if ((mods & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL)!=0 && key==org.lwjgl.glfw.GLFW.GLFW_KEY_S) {
                Window.levels().save();return;
            }
            if ((mods & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL)!=0 && key==org.lwjgl.glfw.GLFW.GLFW_KEY_O) {
                Window.levels().choose("Import");return;
            }
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) agent.GameInput.stop("Human takeover");
            get().keyPressed[key] = true;
            get().keyBeginPress[key] = true;
        } else if (action == GLFW_RELEASE) {
            get().keyPressed[key] = false;
            get().keyBeginPress[key] = false;
        }
    }

    public static boolean isKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode < get().keyPressed.length && get().keyPressed[keyCode];
    }

    public static boolean keyBeginPress(int keyCode) {
        if (keyCode < 0 || keyCode >= get().keyBeginPress.length) return false;
        boolean result = get().keyBeginPress[keyCode];
        if (result) {
            get().keyBeginPress[keyCode] = false;
        }
        return result;
    }
}
