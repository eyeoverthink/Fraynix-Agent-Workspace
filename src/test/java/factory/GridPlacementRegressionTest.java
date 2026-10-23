package factory;

import editor.ViewportCoordinates;
import jade.Camera;
import jade.MouseListener;
import org.joml.Vector2f;
import org.junit.Test;
import util.GridPlacement;
import static org.junit.Assert.*;
import static org.lwjgl.glfw.GLFW.*;

public class GridPlacementRegressionTest {
    @Test public void tileEdgesMatchTheGridOnBothSidesOfZero() {
        for (float cursor : new float[]{-.01f, -.26f, -250.01f, .01f, .26f, 250.01f}) {
            float center = GridPlacement.cellCenter(cursor, .25f);
            float left = center - .125f, right = center + .125f;
            assertEquals(Math.rint(left / .25), left / .25, .0001);
            assertEquals(Math.rint(right / .25), right / .25, .0001);
            assertTrue(cursor >= left && cursor < right);
        }
        assertEquals(-.125f, GridPlacement.cellCenter(-.01f, .25f), .00001f);
    }
    @Test public void rectangularCellsUseTheirOwnAxisSpacing() {
        assertEquals(-.5f, GridPlacement.edge(-.1f, .5f), .00001f);
        assertEquals(-.25f, GridPlacement.edge(-.1f, .25f), .00001f);
        assertEquals(.75f, GridPlacement.nearestCenter(.72f, .5f), .00001f);
    }
    @Test public void pickingTracksViewportMoveResizePanAndZoom() {
        Camera camera = new Camera(new Vector2f(-250, -1));
        Vector2f first = ViewportCoordinates.world(700, 360, new Vector2f(100, 60), new Vector2f(1200, 600), camera);
        assertEquals(-247, first.x, .0001f); assertEquals(.5f, first.y, .0001f);
        Vector2f moved = ViewportCoordinates.world(650, 350, new Vector2f(350, 200), new Vector2f(600, 300), camera);
        assertEquals(first.x, moved.x, .0001f); assertEquals(first.y, moved.y, .0001f);
        camera.position.set(10, -2); camera.setZoom(2);
        Vector2f zoomed = ViewportCoordinates.world(650, 350, new Vector2f(350, 200), new Vector2f(600, 300), camera);
        assertEquals(16, zoomed.x, .0001f); assertEquals(1, zoomed.y, .0001f);
    }
    @Test public void duplicatePressAndReleaseDoNotLeavePlacementDragging() {
        MouseListener.mouseButtonCallback(0, GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS, 0);
        MouseListener.mouseButtonCallback(0, GLFW_MOUSE_BUTTON_LEFT, GLFW_PRESS, 0);
        MouseListener.mouseButtonCallback(0, GLFW_MOUSE_BUTTON_LEFT, GLFW_RELEASE, 0);
        assertFalse(MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT));
        assertFalse(MouseListener.isDragging());
    }
}
