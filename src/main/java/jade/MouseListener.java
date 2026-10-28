package jade;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class MouseListener {
    private static MouseListener instance;
    private double scrollX, scrollY;
    private double xPos, yPos, lastY, lastX, worldX, worldY, lastWorldX, lastWorldY;
    private boolean mouseButtonPressed[] = new boolean[9];
    private final long[] pressSequence = new long[9];
    private boolean isDragging;

    private int mouseButtonDown = 0;

    private Vector2f gameViewportPos = new Vector2f();
    private Vector2f gameViewportSize = new Vector2f();

    private MouseListener() {
        this.scrollX = 0.0;
        this.scrollY = 0.0;
        this.xPos = 0.0;
        this.yPos = 0.0;
        this.lastX = 0.0;
        this.lastY = 0.0;
    }

    public static MouseListener get() {
        if (MouseListener.instance == null) {
            MouseListener.instance = new MouseListener();
        }

        return MouseListener.instance;
    }

    public static void mousePosCallback(long window, double xpos, double ypos) {
        if (get().mouseButtonDown > 0) {
            get().isDragging = true;
        }

        get().lastX = get().xPos;
        get().lastY = get().yPos;
        get().lastWorldX = get().worldX;
        get().lastWorldY = get().worldY;
        get().xPos = xpos;
        get().yPos = ypos;
        calcOrthoX();
        calcOrthoY();
    }

    public static void mouseButtonCallback(long window, int button, int action, int mods) {
        if (button < 0 || button >= get().mouseButtonPressed.length) return;
        if (action == GLFW_PRESS) {
            if (!get().mouseButtonPressed[button]) get().pressSequence[button]++;
            if (!get().mouseButtonPressed[button]) get().mouseButtonDown++;

            if (button < get().mouseButtonPressed.length) {
                get().mouseButtonPressed[button] = true;
            }
        } else if (action == GLFW_RELEASE) {
            if (get().mouseButtonPressed[button]) get().mouseButtonDown--;

            if (button < get().mouseButtonPressed.length) {
                get().mouseButtonPressed[button] = false;
                get().isDragging = false;
            }
        }
    }

    public static void mouseScrollCallback(long window, double xOffset, double yOffset) {
        get().scrollX = xOffset;
        get().scrollY = yOffset;
    }

    public static void endFrame() {
        refreshWorldPosition();
        get().scrollX = 0;
        get().scrollY = 0;
        get().lastX = get().xPos;
        get().lastY = get().yPos;
        get().lastWorldX = get().worldX;
        get().lastWorldY = get().worldY;
    }
    public static long pressSequence(int button) { return get().pressSequence[button]; }

    public static float getX() {
        return (float)get().xPos;
    }

    public static float getY() {
        return (float)get().yPos;
    }

    public static float getDx() {
        return (float)(get().lastX - get().xPos);
    }

    public static float getWorldDx() {
        refreshWorldPosition();
        return (float)(get().lastWorldX - get().worldX);
    }

    public static float getDy() {
        return (float)(get().lastY - get().yPos);
    }

    public static float getWorldDy() {
        refreshWorldPosition();
        return (float)(get().lastWorldY - get().worldY);
    }

    public static float getScrollX() {
        return (float)get().scrollX;
    }

    public static float getScrollY() {
        return (float)get().scrollY;
    }

    public static boolean isDragging() {
        return get().isDragging;
    }

    public static boolean mouseButtonDown(int button) {
        if (button < get().mouseButtonPressed.length) {
            return get().mouseButtonPressed[button];
        } else {
            return false;
        }
    }

    public static float getScreenX() {
        float currentX = getX() - get().gameViewportPos.x;
        currentX = (currentX / get().gameViewportSize.x) * 3840.0f;
        return currentX;
    }

    public static float getScreenY() {
        float currentY = getY() - get().gameViewportPos.y;
        currentY = 2160.0f - ((currentY / get().gameViewportSize.y) * 2160.0f);
        return currentY;
    }

    public static float getOrthoX() {
        refreshWorldPosition();
        return (float)get().worldX;
    }

    private static void calcOrthoX() {
        refreshWorldPosition();
    }

    public static float getOrthoY() {
        refreshWorldPosition();
        return (float)get().worldY;
    }

    public static boolean isInGameViewport() {
        return get().gameViewportSize.x > 0 && get().gameViewportSize.y > 0
            && getX() >= get().gameViewportPos.x && getY() >= get().gameViewportPos.y
            && getX() < get().gameViewportPos.x + get().gameViewportSize.x
            && getY() < get().gameViewportPos.y + get().gameViewportSize.y;
    }

    private static void refreshWorldPosition() {
        if (Window.getScene() == null || Window.getScene().camera() == null) return;
        Vector2f world = editor.ViewportCoordinates.world(getX(), getY(), get().gameViewportPos,
            get().gameViewportSize, Window.getScene().camera());
        get().worldX = world.x;
        get().worldY = world.y;
    }

    private static void calcOrthoY() {
        refreshWorldPosition();
    }

    public static void setGameViewportPos(Vector2f gameViewportPos) {
        get().gameViewportPos.set(gameViewportPos);
    }

    public static void setGameViewportSize(Vector2f gameViewportSize) {
        get().gameViewportSize.set(gameViewportSize);
    }
}
