package runtime;

import agent.GameInput;
import com.google.gson.JsonObject;
import jade.Window;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import scenes.LevelSceneInitializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Gives the native Courier a bounded approach and one opportunity to recover. */
public final class CourierRecovery {
    private static final int MAX_TICKS = 1050;
    private static final int SETTLE_TICKS = 24;
    private static final int OBSERVE_TICKS = 60;
    private static final int EARLY_CLEAR = 16;
    private static final int FINAL_CLEAR = 8;
    private static final float SHORT_PROGRESS = .25f;
    private static final float KNOCK_SPEED = .26f;

    public record Setup(int direction, float supportFriction, boolean obstacle, boolean disturbance) {
        public Setup {
            if ((direction != 1 && direction != -1) || (supportFriction != .065f && supportFriction != .32f))
                throw new IllegalArgumentException("Choose a direction and one of the two support surfaces.");
        }

        CourierTarget.Request delivery() { return new CourierTarget.Request(direction * .77f, .78f, .28f); }
        float spawnX() { return CourierTarget.START_X - direction * (obstacle ? 1.27f : .73f); }
        float obstacleX() { return CourierTarget.START_X - direction * .70f; }

        static Setup read(JsonObject args) {
            return new Setup(args.get("direction").getAsInt(), args.get("supportFriction").getAsFloat(),
                    args.get("obstacle").getAsBoolean(), args.get("disturbance").getAsBoolean());
        }
    }

    private enum Phase { SETTLE, APPROACH, PUSH, INTERRUPT, OBSERVE, RECOVER, RETREAT, HOLD }

    static Map<String, Object> compare(Setup setup) {
        var baseline = run(setup, false);
        var adaptive = run(setup, true);
        var result = new LinkedHashMap<>(CourierCrateTrial.sourceBindings());
        result.put("targetArtSha256", CourierCrateTrial.TARGET_HASH);
        result.put("setup", setup);
        result.put("targetX", setup.delivery().targetX());
        result.put("baseline", baseline);
        result.put("adaptive", adaptive);
        return result;
    }

    private static Run run(Setup setup, boolean adaptive) {
        Window.changeScene(new LevelSceneInitializer(setup.delivery(), setup));
        var trial = CourierCrateTrial.current();
        var player = trial.playerBody();
        var cargo = trial.crateBody();
        var obstacle = trial.obstacleBody();
        float obstacleX = obstacle == null ? 0 : obstacle.getPosition().x;
        var controller = trial.controller();
        int direction = setup.direction();
        float goal = setup.delivery().targetX();
        var rows = new ArrayList<Tick>();
        Phase phase = Phase.SETTLE;
        Kick incident = null;
        int jumpTicks = 0, clear = 0, quiet = 0;
        int obstacleCrossedTick = 0;
        rows.add(sample(trial, 0, phase, 0, false));
        try {
            for (int n = 1; n <= MAX_TICKS; n++) {
                float playerX = player.getPosition().x;
                float cargoX = cargo.getPosition().x;
                float distance = direction * (goal - cargoX);
                float crateSpeed = Math.max(0, direction * cargo.getLinearVelocity().x);
                boolean jump = false;
                float cap = adaptive ? CourierTarget.approachCap(distance) : .31f;

                if (phase == Phase.SETTLE && n > SETTLE_TICKS)
                    phase = obstacle == null ? Phase.PUSH : Phase.APPROACH;
                if (phase == Phase.APPROACH) {
                    float beforeBarrier = direction * (obstacleX - playerX);
                    if (jumpTicks == 0 && controller.onGround && beforeBarrier <= .265f && beforeBarrier > .15f)
                        jumpTicks = 1;
                    if (jumpTicks > 0) {
                        cap = .91f;
                        jump = jumpTicks++ <= 10;
                        if (beforeBarrier < -.23f && controller.onGround) phase = Phase.PUSH;
                    }
                }
                if (phase == Phase.PUSH && setup.disturbance() &&
                        direction * (cargoX - CourierTarget.START_X) >= SHORT_PROGRESS) {
                    phase = Phase.INTERRUPT;
                    clear = 0;
                }
                if ((phase == Phase.PUSH || phase == Phase.RECOVER) && trial.hasHorizontalImpulse() &&
                        distance <= CourierTarget.releaseRunout(crateSpeed)) {
                    phase = Phase.RETREAT;
                    clear = 0;
                }

                int input = switch (phase) {
                    case APPROACH, PUSH, RECOVER -> direction;
                    case RETREAT, INTERRUPT -> -direction;
                    default -> 0;
                };
                if (phase == Phase.OBSERVE && incident == null) {
                    if (trial.contacts() != 0) throw new IllegalStateException("The obstacle study must separate before disturbance.");
                    incident = disturb(cargo, n, direction);
                }
                controller.terminalVelocity.x = input == -direction ? .36f : cap;
                GameInput.external(input, jump);
                Window.get().stepEmbedded();
                rows.add(sample(trial, n, phase, input, jump));
                if (obstacle != null && obstacleCrossedTick == 0 &&
                        direction * (player.getPosition().x - obstacleX) >= 0) obstacleCrossedTick = n;

                if (phase == Phase.INTERRUPT || phase == Phase.RETREAT) {
                    clear = trial.contacts() == 0 ? clear + 1 : 0;
                    int required = phase == Phase.INTERRUPT ? EARLY_CLEAR : FINAL_CLEAR;
                    if (clear >= required) {
                        phase = phase == Phase.INTERRUPT ? Phase.OBSERVE : Phase.HOLD;
                        quiet = 0;
                    }
                } else if (phase == Phase.OBSERVE && ++quiet == OBSERVE_TICKS) {
                    boolean stoppedShort = direction * (goal - cargo.getPosition().x) > .18f &&
                            Math.abs(cargo.getLinearVelocity().x) <= .008f && trial.contacts() == 0;
                    if (!adaptive || !stoppedShort) break;
                    phase = Phase.RECOVER;
                } else if (phase == Phase.HOLD && ++quiet == 60) {
                    break;
                }
            }
        } finally {
            GameInput.externalOff();
        }
        return new Run(trial.playerId(), player.getType().name(), player.getMass(), player.isFixedRotation(),
                cargo.getType().name(), cargo.getMass(), cargo.getFixtureList().getFriction(),
                trial.supportBody().getFixtureList().getFriction(), obstacle != null,
                obstacle == null ? "none" : obstacle.getType().name(), obstacleX,
                obstacle == null ? 0 : obstacleHeight(obstacle),
                obstacleCrossedTick, trial.firstContactX(), incident, rows);
    }

    private static float obstacleHeight(Body obstacle) {
        var shape = (PolygonShape)obstacle.getFixtureList().getShape();
        float bottom = Float.POSITIVE_INFINITY, top = Float.NEGATIVE_INFINITY;
        for (int index = 0; index < shape.getVertexCount(); index++) {
            float y = shape.getVertex(index).y;
            bottom = Math.min(bottom, y);
            top = Math.max(top, y);
        }
        return top - bottom;
    }

    private static Kick disturb(Body cargo, int tick, int direction) {
        float x = cargo.getPosition().x;
        float before = cargo.getLinearVelocity().x;
        float impulse = -direction * KNOCK_SPEED * cargo.getMass();
        cargo.applyLinearImpulse(new Vec2(impulse, 0), cargo.getWorldCenter());
        return new Kick(tick, x, before, cargo.getLinearVelocity().x, impulse);
    }

    private static Tick sample(CourierCrateTrial trial, int tick, Phase phase, int input, boolean jump) {
        if (tick != Window.getScene().tick())
            throw new IllegalStateException("The recovery study lost its native scene.");
        var player = trial.playerBody();
        var cargo = trial.crateBody();
        return new Tick(tick, phase.name(), input, jump, player.getPosition().x, player.getPosition().y,
                cargo.getPosition().x, cargo.getLinearVelocity().x, trial.contacts(), trial.begins(),
                trial.horizontalImpulse());
    }

    private record Tick(int tick, String phase, int input, boolean jump, float playerX, float playerY,
                        float crateX, float crateVx, int contacts, int begins, float impulse) {}
    private record Kick(int tick, float x, float speedBefore, float speedAfter, float impulse) {}
    private record Run(String playerId, String playerType, float playerMass, boolean playerFixedRotation,
                       String crateType, float mass, float friction, float supportFriction,
                       boolean obstacle, String obstacleType, float obstacleX, float obstacleHeight, int obstacleCrossedTick,
                       Float firstContactX, Kick disturbance, ArrayList<Tick> rows) {}
}
