package agent;

import components.CourseContact;
import components.PlayerController;
import jade.GameObject;
import org.jbox2d.collision.AABB;
import org.jbox2d.dynamics.Body;
import org.joml.Vector2f;
import physics2d.components.Rigidbody2D;
import physics2d.enums.BodyType;
import scenes.Scene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads static rail fixtures and offers a bounded jump through the existing input lease. */
public final class RailNavigation {
    static final int HOLD_TICKS = 28;
    static final int FORECAST_TICKS = 90;
    private static final int RAIL_LIMIT = 32;
    private static final int ATTEMPT_LIMIT = 64;
    private static final float START_DISTANCE = .65f;
    private static final float CLEARANCE = .035f;
    private static final float DT = Scene.FIXED_STEP;

    record Bounds(float left, float right, float bottom, float top) {
        Bounds shift(float x, float y) { return new Bounds(left+x, right+x, bottom+y, top+y); }
        boolean overlapsX(Bounds other) { return right >= other.left && left <= other.right; }
        float ahead(Bounds obstacle, float direction) {
            return direction > 0 ? obstacle.left-right : left-obstacle.right;
        }
        boolean beyond(Bounds obstacle, float direction) {
            return direction > 0 ? left > obstacle.right : right < obstacle.left;
        }
    }
    private record Rail(String id, String name, Bounds bounds) {}
    record Motor(float acceleration, float maxX, float maxY, float impulse, float boost, float gravity) {}
    record Plan(float clearance, int landAfter, float landX) {}
    public record Input(float horizontal, boolean jump, String reason) {}
    private record Crossing(String id, String name, long launchTick, long entryTick, long exitTick,
                            float fromX, float measuredClearance, float predictedClearance, int overlapSamples) {}
    @FunctionalInterface interface Surface {
        float top(float x, float fromY, float toY);
    }

    private final Scene scene;
    private final List<Rail> rails;
    private final List<Crossing> crossings = new ArrayList<>();
    private Rail nearest;
    private Rail active;
    private Plan checkedPlan;
    private float direction;
    private long launchTick, entryTick;
    private float launchX, minClearance;
    private int overlapSamples, forecasts, refused, launches, rejectedMeasurements;
    private String state = "Watching for rails";

    public RailNavigation(Scene scene) {
        this.scene = scene;
        List<Rail> found = new ArrayList<>();
        for (GameObject object : scene.getGameObjects()) {
            CourseContact contact = object.getComponent(CourseContact.class);
            if (contact == null || contact.kind != CourseContact.Kind.HAZARD || object.isDead()) continue;
            Rigidbody2D body = object.getComponent(Rigidbody2D.class);
            if (body == null || body.getBodyType() != BodyType.Static || body.getRawBody() == null) continue;
            Bounds bounds = bounds(body.getRawBody(), true);
            if (bounds != null) found.add(new Rail(object.getPersistentId().toString(), object.getName(), bounds));
            if (found.size() > RAIL_LIMIT) throw new IllegalStateException("Original rail observation limit exceeded");
        }
        rails = List.copyOf(found);
    }

    public boolean owns(Scene scene) { return this.scene == scene; }

    public Input choose(GameObject player, PlayerController control, float targetX, float desired, long tick) {
        Rigidbody2D rigid = player.getComponent(Rigidbody2D.class);
        Body body = rigid.getRawBody();
        if (body == null || rails.isEmpty()) return null;
        Bounds pose = bounds(body, false);
        if (pose == null) return null;
        if (active != null) {
            measure(pose, tick - 1);
            if (active != null && tick - launchTick < FORECAST_TICKS) {
                return new Input(direction, tick - launchTick < HOLD_TICKS, "Clearing " + active.name());
            }
            if (active != null) {
                active = null;
                rejectedMeasurements++;
                state = "Rail crossing exceeded its observation window";
            }
        }

        float travel = Math.signum(targetX - body.getPosition().x);
        if (travel == 0 || desired != travel) return null;
        nearest = rails.stream().filter(rail -> {
            float distance = pose.ahead(rail.bounds(), travel);
            return distance > 0 && distance <= 2f
                && (targetX - (travel > 0 ? rail.bounds().left() : rail.bounds().right())) * travel >= 0
                && rail.bounds().top() - pose.bottom() > -.4f
                && rail.bounds().bottom() - pose.bottom() < .45f;
        }).min(Comparator.comparingDouble(rail -> pose.ahead(rail.bounds(), travel))).orElse(null);
        if (nearest == null || !control.onGround || launches >= ATTEMPT_LIMIT) return null;

        float distance = pose.ahead(nearest.bounds(), travel);
        if (distance > START_DISTANCE) return null;
        Motor motor = new Motor(control.walkSpeed, control.terminalVelocity.x, control.terminalVelocity.y,
                control.jumpImpulse, control.jumpBoost, scene.physics().getGravity().y * .7f);
        float x = body.getPosition().x;
        float y = body.getPosition().y;
        float vx = body.getLinearVelocity().x;
        float vy = body.getLinearVelocity().y;
        Bounds relative = pose.shift(-x, -y);
        Surface floor = (px, top, bottom) -> {
            var hit = scene.physics().raycastSolid(player, new Vector2f(px, top), new Vector2f(px, bottom));
            return hit.hit && hit.normal.y > .5f ? hit.point.y : Float.NaN;
        };
        var headroom = scene.physics().raycastSolid(player, new Vector2f(x, pose.top() + .02f),
                new Vector2f(x, pose.top() + .7f));
        forecasts++;
        Plan candidate = headroom.hit ? null
                : forecast(x, y, vx, vy, travel, relative, nearest.bounds(), motor, floor);
        if (candidate == null) {
            refused++;
            state = "No supported jump over " + nearest.name();
            // Continue approaching only while there is enough room to brake.
            float stoppingDistance = vx * vx / (2 * control.slowDownForce / DT);
            return distance <= stoppingDistance + .09f ? new Input(0, false, state) : null;
        }
        checkedPlan = candidate;
        active = nearest;
        direction = travel;
        launchTick = tick;
        launchX = x;
        entryTick = 0;
        overlapSamples = 0;
        minClearance = Float.MAX_VALUE;
        launches++;
        state = "Jumping over " + active.name();
        return new Input(travel, true, state);
    }

    private void measure(Bounds actor, long tick) {
        Bounds rail = active.bounds();
        if (actor.overlapsX(rail)) {
            if (overlapSamples == 0) entryTick = tick;
            overlapSamples++;
            minClearance = Math.min(minClearance, actor.bottom() - rail.top());
        }
        if (!actor.beyond(rail, direction)) return;
        if (overlapSamples > 0 && minClearance > 0) {
            crossings.add(new Crossing(active.id(), active.name(), launchTick, entryTick, tick, launchX,
                    minClearance, checkedPlan.clearance(), overlapSamples));
            state = "Cleared " + active.name();
        } else {
            rejectedMeasurements++;
            state = "Rail clearance could not be verified";
        }
        active = null;
    }

    static Plan forecast(float x, float y, float vx, float vy, float direction,
                         Bounds actor, Bounds rail, Motor motor, Surface floor) {
        int overlaps = 0;
        float clearance = Float.MAX_VALUE;
        for (int tick = 0; tick < FORECAST_TICKS; tick++) {
            float nextX = x + vx * DT;
            float nextY = y + vy * DT;
            Bounds before = actor.shift(x, y);
            Bounds after = actor.shift(nextX, nextY);
            if (after.overlapsX(rail)) {
                overlaps++;
                clearance = Math.min(clearance, after.bottom() - rail.top());
                if (clearance < CLEARANCE) return null;
            }
            if (tick >= HOLD_TICKS && vy <= 0) {
                float a = floor.top(after.left(), before.bottom()+.035f, after.bottom()-.02f);
                float b = floor.top(nextX, before.bottom()+.035f, after.bottom()-.02f);
                float c = floor.top(after.right(), before.bottom()+.035f, after.bottom()-.02f);
                if (Float.isFinite(a) || Float.isFinite(b) || Float.isFinite(c)) {
                    boolean supported = Float.isFinite(a) && Float.isFinite(b) && Float.isFinite(c)
                            && Math.max(a, Math.max(b,c))-Math.min(a, Math.min(b,c)) < .04f;
                    return supported && overlaps > 0 && after.beyond(rail, direction)
                            ? new Plan(clearance, tick + 1, nextX) : null;
                }
            }
            x = nextX;
            y = nextY;
            vx = clamp(vx + direction * motor.acceleration() * DT, motor.maxX());
            if (tick == 0) vy = motor.impulse();
            else if (tick < HOLD_TICKS) vy = (HOLD_TICKS - tick) / 2.2f * motor.boost();
            else vy = (tick == HOLD_TICKS ? vy * .35f : vy) + motor.gravity() * DT;
            vy = clamp(vy, motor.maxY());
        }
        return null;
    }

    private static float clamp(float value, float max) {
        return Math.max(-max, Math.min(max, value));
    }

    private static Bounds bounds(Body body, boolean sensors) {
        float left = Float.MAX_VALUE, right = -Float.MAX_VALUE;
        float bottom = Float.MAX_VALUE, top = -Float.MAX_VALUE;
        var box = new AABB();
        for (var fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext()) {
            if (fixture.isSensor() != sensors) continue;
            fixture.getShape().computeAABB(box, body.getTransform(), 0);
            left = Math.min(left, box.lowerBound.x);
            right = Math.max(right, box.upperBound.x);
            bottom = Math.min(bottom, box.lowerBound.y);
            top = Math.max(top, box.upperBound.y);
        }
        return left <= right && bottom <= top ? new Bounds(left,right,bottom,top) : null;
    }

    public Map<String, Object> observation() {
        var result = new LinkedHashMap<String, Object>();
        result.put("rails", rails);
        result.put("state", state);
        result.put("forecasts", forecasts);
        result.put("refusedForecasts", refused);
        result.put("launches", launches);
        result.put("rejectedMeasurements", rejectedMeasurements);
        result.put("crossings", List.copyOf(crossings));
        if (nearest != null) result.put("nearby", nearest.name());
        if (checkedPlan != null) result.put("lastPlan", checkedPlan);
        return result;
    }
}
