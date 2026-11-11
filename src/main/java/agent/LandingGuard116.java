package agent;

/** Bounded, input-only landing forecast. Every surface query uses current collision geometry. */
public final class LandingGuard116 {
    @FunctionalInterface public interface Surface { float height(float x, float top, float bottom); }
    public record Landing(boolean supported, float x, float y, int ticks) {}
    public record Decision(float horizontal, boolean override, Landing planned, String reason) {}
    private static final float DT = 1f / 60;

    public static Landing forecast(float x, float y, float vx, float vy, float halfHeight,
                                   float acceleration, float gravity, float maxX, float maxY,
                                   float brake, float input, Surface surface) {
        for (int tick = 1; tick <= 90; tick++) {
            if (input == 0) vx = Math.copySign(Math.max(0, Math.abs(vx) - brake), vx);
            else {
                if (vx * input < 0) vx += input * brake;
                vx += input * acceleration * DT;
            }
            vx = Math.max(-maxX, Math.min(maxX, vx));
            vy = Math.max(-maxY, Math.min(maxY, vy + gravity * DT));
            float nx = x + vx * DT, ny = y + vy * DT;
            float top = y - halfHeight + .03f, bottom = ny - halfHeight - .02f;
            float a = surface.height(nx - .11f, top, bottom);
            float b = surface.height(nx, top, bottom);
            float c = surface.height(nx + .11f, top, bottom);
            if (Float.isFinite(a) || Float.isFinite(b) || Float.isFinite(c)) {
                boolean supported = Float.isFinite(a) && Float.isFinite(b) && Float.isFinite(c)
                    && Math.max(a, Math.max(b, c)) - Math.min(a, Math.min(b, c)) < .04f;
                return new Landing(supported, nx, ny, tick);
            }
            x = nx; y = ny;
        }
        return new Landing(false, x, y, 90);
    }

    public static Decision choose(float x, float y, float vx, float vy, float halfHeight,
                                  float acceleration, float gravity, float maxX, float maxY,
                                  float brake, float desired, float destination, Surface surface) {
        Landing original = forecast(x,y,vx,vy,halfHeight,acceleration,gravity,maxX,maxY,brake,desired,surface);
        if (original.supported()) return new Decision(desired,false,original,"Desired input has a supported landing");
        Landing best = null; float chosen = desired;
        for (float input : new float[]{0, -1, 1}) {
            if (input == desired) continue;
            Landing candidate = forecast(x,y,vx,vy,halfHeight,acceleration,gravity,maxX,maxY,brake,input,surface);
            if (candidate.supported() && (best == null || Math.abs(candidate.x()-destination) < Math.abs(best.x()-destination))) {
                best = candidate; chosen = input;
            }
        }
        return best == null ? new Decision(desired,false,original,"No supported landing within the forecast budget")
            : new Decision(chosen,true,best,"Adjust input toward a supported landing; reobserve next tick");
    }
}
