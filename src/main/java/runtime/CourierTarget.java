package runtime;

import agent.GameInput;
import com.google.gson.JsonObject;
import components.PlayerController;
import jade.Window;
import scenes.LevelSceneInitializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** A bounded controller for the existing Game Lab player. It only supplies player input. */
public final class CourierTarget {
    public static final float START_X = 2.08f;
    private static final int MAX_TICKS = 720;
    private static final int SETTLE_TICKS = 24;
    private static final int CLEAR_TICKS = 8;
    private static final int HOLD_TICKS = 60;
    private static final float CRUISE_SPEED = .56f;
    private static final float CREEP_SPEED = .19f;
    private static final float RETREAT_SPEED = .32f;

    public record Request(float distance, float mass, float friction) {
        public Request {
            if (!Float.isFinite(distance) || Math.abs(distance) < .30f || Math.abs(distance) > 1f
                    || !Float.isFinite(mass) || mass < .35f || mass > 1.30f
                    || !Float.isFinite(friction) || friction < .06f || friction > .75f)
                throw new IllegalArgumentException("Choose a distance of 0.30–1.00, mass of 0.35–1.30 and friction of 0.06–0.75.");
        }

        float targetX() { return START_X + distance; }
        int direction() { return distance > 0 ? 1 : -1; }

        static Request read(JsonObject value) {
            return new Request(value.get("distance").getAsFloat(), value.get("mass").getAsFloat(),
                    value.get("friction").getAsFloat());
        }
    }

    private enum Phase { SETTLE, PUSH, RETREAT, HOLD }

    static Map<String, Object> run(Request request) {
        Window.changeScene(new LevelSceneInitializer(request));
        CourierCrateTrial trial = CourierCrateTrial.current();
        var player = trial.playerBody();
        var crate = trial.crateBody();
        PlayerController controller = trial.controller();
        int direction = request.direction();
        float goal = request.targetX();
        var rows = new ArrayList<Tick>();
        Phase phase = Phase.SETTLE;
        int clear = 0;
        int held = 0;
        rows.add(tick(trial, 0, phase, 0));
        try {
            for (int next = 1; next <= MAX_TICKS; next++) {
                if (phase == Phase.SETTLE && next > SETTLE_TICKS) phase = Phase.PUSH;
                float remaining = direction * (goal - crate.getPosition().x);
                float speed = Math.max(0, direction * crate.getLinearVelocity().x);
                // A small late-stage speed cap limits stopping error even on the low-friction surface.
                float cap = approachCap(remaining);
                float runout = releaseRunout(speed);
                if (phase == Phase.PUSH && trial.hasHorizontalImpulse() && remaining <= runout) {
                    phase = Phase.RETREAT;
                }
                int input = switch (phase) {
                    case PUSH -> direction;
                    case RETREAT -> -direction;
                    default -> 0;
                };
                controller.terminalVelocity.x = phase == Phase.RETREAT ? RETREAT_SPEED : cap;
                GameInput.external(input, false);
                Window.get().stepEmbedded();
                rows.add(tick(trial, next, phase, input));

                if (phase == Phase.RETREAT) {
                    clear = trial.contacts() == 0 ? clear + 1 : 0;
                    if (clear >= CLEAR_TICKS) phase = Phase.HOLD;
                } else if (phase == Phase.HOLD && ++held == HOLD_TICKS) {
                    break;
                }
            }
        } finally {
            GameInput.externalOff();
        }

        var result = new LinkedHashMap<>(CourierCrateTrial.sourceBindings());
        result.put("targetArtSha256", CourierCrateTrial.TARGET_HASH);
        result.put("playerId", trial.playerId());
        result.put("request", request);
        result.put("targetX", goal);
        result.put("playerType", player.getType().name());
        result.put("playerMass", player.getMass());
        result.put("playerFixedRotation", player.isFixedRotation());
        result.put("crateType", crate.getType().name());
        result.put("actualMass", crate.getMass());
        result.put("actualFriction", crate.getFixtureList().getFriction());
        result.put("firstContactX", trial.firstContactX());
        result.put("rows", rows);
        return result;
    }

    static float approachCap(float remaining) {
        return remaining > .28f ? CRUISE_SPEED : CREEP_SPEED;
    }

    static float releaseRunout(float speed) {
        return .009f + speed * .052f + speed * speed * .45f;
    }

    private static Tick tick(CourierCrateTrial trial, int number, Phase phase, int input) {
        if (number != Window.getScene().tick())
            throw new IllegalStateException("The Courier target scene changed while controlling the player.");
        var player = trial.playerBody();
        var crate = trial.crateBody();
        return new Tick(number, phase.name(), input, player.getPosition().x, crate.getPosition().x,
                crate.getLinearVelocity().x, trial.contacts(), trial.begins(), trial.horizontalImpulse());
    }

    private record Tick(int tick, String phase, int input, float playerX, float crateX, float crateVx,
                        int contacts, int begins, float impulse) {}
}
