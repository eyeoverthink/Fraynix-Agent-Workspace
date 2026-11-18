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
    private static final float PACING_DISTANCE = 1.75f;

    public enum Pace {
        NORMAL("normal", Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        STEADY("steady", 1.25f, 1.95f),
        CAREFUL("careful", .90f, 1.80f);

        private final String id;
        private final float speed;
        private final float flightSpeed;
        Pace(String id, float speed, float flightSpeed) {
            this.id = id;
            this.speed = speed;
            this.flightSpeed = flightSpeed;
        }
        public static Pace parse(String id) {
            for (Pace pace : values()) if (pace.id.equals(id)) return pace;
            throw new IllegalArgumentException("Unknown rail approach pace");
        }
    }
    private static final float CLEARANCE = .035f;
    // Forecast and solver can disagree during a descending crossing.
    private static final float GROUP_FORECAST_RESERVE = .10f;
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
                            float fromX, float launchSpeed, float entrySpeed,
                            float measuredClearance, float predictedClearance, int overlapSamples) {}
    private record Flight(float[] clearances, int landAfter, float landX) {}
    private static final class CrossingSamples {
        final Rail rail;
        final float predictedClearance;
        long entryTick;
        int count;
        float entrySpeed;
        float minClearance = Float.POSITIVE_INFINITY;

        CrossingSamples(Rail rail, float predictedClearance) {
            this.rail = rail;
            this.predictedClearance = predictedClearance;
        }
    }
    @FunctionalInterface interface Surface {
        float top(float x, float fromY, float toY);
    }

    private final Scene scene;
    private final List<Rail> rails;
    private final List<Crossing> crossings = new ArrayList<>();
    private final List<CrossingSamples> active = new ArrayList<>();
    private final Pace pace;
    private final boolean groupRails;
    private Rail nearest;
    private Plan checkedPlan;
    private float direction;
    private long launchTick;
    private float launchX, launchSpeed;
    private int forecasts, refused, launches, rejectedMeasurements;
    private String state = "Watching for rails";

    public RailNavigation(Scene scene, Pace pace, boolean groupRails) {
        this.scene = scene;
        this.pace = pace;
        this.groupRails = groupRails;
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
        if (!active.isEmpty()) {
            measure(pose, tick - 1, Math.abs(body.getLinearVelocity().x));
            if (!active.isEmpty() && tick - launchTick < FORECAST_TICKS) {
                float horizontal = flightInput(body.getLinearVelocity().x, direction, pace.flightSpeed);
                return new Input(horizontal, tick - launchTick < HOLD_TICKS, state);
            }
            if (!active.isEmpty()) {
                rejectedMeasurements += active.size();
                active.clear();
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
        float x = body.getPosition().x;
        float y = body.getPosition().y;
        float vx = body.getLinearVelocity().x;
        float vy = body.getLinearVelocity().y;
        if (distance <= PACING_DISTANCE && vx * travel > pace.speed) {
            return new Input(0, false, "Settling to the selected rail approach pace");
        }
        if (distance > START_DISTANCE) return null;
        Motor motor = new Motor(control.walkSpeed, control.terminalVelocity.x, control.terminalVelocity.y,
                control.jumpImpulse, control.jumpBoost, scene.physics().getGravity().y * .7f);
        Bounds relative = pose.shift(-x, -y);
        Surface floor = (px, top, bottom) -> {
            var hit = scene.physics().raycastSolid(player, new Vector2f(px, top), new Vector2f(px, bottom));
            return hit.hit && hit.normal.y > .5f ? hit.point.y : Float.NaN;
        };
        var headroom = scene.physics().raycastSolid(player, new Vector2f(x, pose.top() + .02f),
                new Vector2f(x, pose.top() + .7f));
        forecasts++;
        List<Rail> candidates = nearbyRails(pose, travel);
        Flight flight = headroom.hit ? null : forecastFlight(x, y, vx, vy, travel, relative,
                candidates.stream().map(Rail::bounds).toList(), motor, floor,
                pace.flightSpeed, control.slowDownForce, groupRails);
        if (flight == null) {
            refused++;
            state = "No supported jump over " + nearest.name();
            float stoppingDistance = vx * vx / (2 * control.slowDownForce / DT);
            return distance <= stoppingDistance + .09f ? new Input(0, false, state) : null;
        }
        if (crossings.size() + candidates.size() > ATTEMPT_LIMIT) {
            state = "Rail evidence limit reached";
            return new Input(0, false, state);
        }
        for (int i = 0; i < candidates.size(); i++) {
            if (Float.isFinite(flight.clearances()[i])) {
                active.add(new CrossingSamples(candidates.get(i), flight.clearances()[i]));
            }
        }
        checkedPlan = new Plan(flight.clearances()[0], flight.landAfter(), flight.landX());
        direction = travel;
        launchTick = tick;
        launchX = x;
        launchSpeed = Math.abs(vx);
        launches++;
        state = active.size() == 1 ? "Jumping over " + nearest.name() : "Jumping over " + active.size() + " rails";
        return new Input(travel, true, state);
    }

    private List<Rail> nearbyRails(Bounds pose, float travel) {
        if (!groupRails) return List.of(nearest);
        List<Rail> candidates = new ArrayList<>();
        candidates.add(nearest);
        for (Rail rail : rails) {
            float distance = pose.ahead(rail.bounds(), travel);
            if (rail != nearest && distance >= 0 && distance <= 3.5f) candidates.add(rail);
        }
        return candidates;
    }

    private void measure(Bounds actor, long tick, float speed) {
        String cleared = null;
        boolean rejected = false;
        var iterator = active.iterator();
        while (iterator.hasNext()) {
            CrossingSamples sample = iterator.next();
            Bounds rail = sample.rail.bounds();
            if (actor.overlapsX(rail)) {
                if (sample.count == 0) {
                    sample.entryTick = tick;
                    sample.entrySpeed = speed;
                }
                sample.count++;
                sample.minClearance = Math.min(sample.minClearance, actor.bottom() - rail.top());
            }
            if (!actor.beyond(rail, direction)) continue;
            float minimum = groupRails ? CLEARANCE : 0;
            if (sample.count > 0 && sample.minClearance > minimum) {
                crossings.add(new Crossing(sample.rail.id(), sample.rail.name(), launchTick,
                        sample.entryTick, tick, launchX, launchSpeed, sample.entrySpeed,
                        sample.minClearance, sample.predictedClearance, sample.count));
                cleared = sample.rail.name();
            } else {
                rejectedMeasurements++;
                rejected = true;
            }
            iterator.remove();
        }
        if (active.isEmpty()) {
            state = rejected ? "Rail clearance could not be verified" : "Cleared " + cleared;
        }
    }

    static Plan forecast(float x, float y, float vx, float vy, float direction,
                         Bounds actor, Bounds rail, Motor motor, Surface floor) {
        Flight flight = forecastFlight(x, y, vx, vy, direction, actor, List.of(rail), motor, floor,
                Float.POSITIVE_INFINITY, 0, false);
        return flight == null ? null : new Plan(flight.clearances()[0], flight.landAfter(), flight.landX());
    }

    private static Flight forecastFlight(float x, float y, float vx, float vy, float direction,
                                         Bounds actor, List<Bounds> rails, Motor motor, Surface floor,
                                         float speed, float brake, boolean groupRails) {
        float minimum = groupRails ? CLEARANCE + GROUP_FORECAST_RESERVE : CLEARANCE;
        float[] clearances = new float[rails.size()];
        java.util.Arrays.fill(clearances, Float.POSITIVE_INFINITY);
        for (int tick = 0; tick < FORECAST_TICKS; tick++) {
            float nextX = x + vx * DT;
            float nextY = y + vy * DT;
            Bounds before = actor.shift(x, y);
            Bounds after = actor.shift(nextX, nextY);
            for (int i = 0; i < rails.size(); i++) {
                Bounds rail = rails.get(i);
                if (!after.overlapsX(rail)) continue;
                clearances[i] = Math.min(clearances[i], after.bottom() - rail.top());
                if (clearances[i] < minimum) return null;
            }
            if (tick >= HOLD_TICKS && vy <= 0) {
                float top = before.bottom() + .035f;
                float bottom = after.bottom() - .02f;
                float a = floor.top(after.left(), top, bottom);
                float b = floor.top(nextX, top, bottom);
                float c = floor.top(after.right(), top, bottom);
                if (Float.isFinite(a) || Float.isFinite(b) || Float.isFinite(c)) {
                    boolean supported = Float.isFinite(a) && Float.isFinite(b) && Float.isFinite(c)
                            && Math.max(a, Math.max(b, c)) - Math.min(a, Math.min(b, c)) < .04f;
                    if (!supported || !Float.isFinite(clearances[0])) return null;
                    for (int i = 0; i < rails.size(); i++) {
                        Bounds rail = rails.get(i);
                        if (Float.isFinite(clearances[i]) && !after.beyond(rail, direction)) return null;
                        if (groupRails && !Float.isFinite(clearances[i]) && after.ahead(rail, direction) < .25f
                                && rail.top() > after.bottom()) return null;
                    }
                    return new Flight(clearances, tick + 1, nextX);
                }
            }
            x = nextX;
            y = nextY;
            // The next controller input observes the pre-step velocity, then applies acceleration or drag.
            float horizontal = flightInput(vx, direction, speed);
            vx = horizontal == 0 ? Math.copySign(Math.max(0, Math.abs(vx) - brake), vx)
                    : clamp(vx + horizontal * motor.acceleration() * DT, motor.maxX());
            if (tick == 0) vy = motor.impulse();
            else if (tick < HOLD_TICKS) vy = (HOLD_TICKS - tick) / 2.2f * motor.boost();
            else vy = (tick == HOLD_TICKS ? vy * .35f : vy) + motor.gravity() * DT;
            vy = clamp(vy, motor.maxY());
        }
        return null;
    }

    private static float flightInput(float vx, float direction, float speed) {
        return vx * direction > speed ? 0 : direction;
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
        result.put("pace", pace.id);
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
