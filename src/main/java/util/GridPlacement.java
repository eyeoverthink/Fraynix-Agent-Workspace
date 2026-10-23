package util;

/** One grid convention for rendering, tile placement, and gizmo snapping. */
public final class GridPlacement {
    private GridPlacement() {}
    public static float edge(float coordinate, float step) {
        check(coordinate, step);
        return (float)(Math.floor((double)coordinate / step) * step);
    }
    public static float cellCenter(float coordinate, float step) { return edge(coordinate, step) + step * 0.5f; }
    public static float nearestCenter(float coordinate, float step) {
        check(coordinate, step);
        return (float)(Math.round((coordinate - step * 0.5) / step) * step + step * 0.5);
    }
    private static void check(float coordinate, float step) {
        if (!Float.isFinite(coordinate) || !Float.isFinite(step) || step <= 0) throw new IllegalArgumentException("Invalid grid coordinate or spacing");
    }
}
