package runtime;

import components.Component;
import components.PlayerController;
import components.Sprite;
import components.SpriteRenderer;
import components.Spritesheet;
import jade.GameObject;
import jade.Window;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;
import org.joml.Vector4f;
import physics2d.components.Box2DCollider;
import physics2d.components.PillboxCollider;
import physics2d.components.Rigidbody2D;
import scenes.LevelSceneInitializer;
import scenes.Scene;
import util.AssetPool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A fixture in original Relay Garden, driven by the same player and JBox2D world as Game Lab. */
public final class CourierCrateTrial extends Component {
    static final int SETTLE_TICKS = 24;
    static final int APPROACH_TICKS = 24;
    static final int TRIAL_TICKS = 120;
    private static final float CRATE_WIDTH = .30f;
    private static final String ATLAS = "assets/characters/courier-poses.png";
    private static final String CRATE = "assets/characters/courier-crate.png";
    private static final String TARGET = "assets/characters/courier-target.png";
    private static final String ATLAS_HASH = "cdbd0c2c0ddae75bbac1ff4056eb39c7ae18410705686b2adaaa5f229602ecc1";
    private static final String CRATE_HASH = "dbfced6bbf2dc382d002b27a82fe253634e8fa2ed0fc5d4cd4a725b74077ffe3";
    static final String TARGET_HASH = "79e68410f9e2b4f4bf1c29a0b9a83e33a395b4be9ec59684537719d043387d74";

    private final transient GameObject player;
    private final transient Rigidbody2D crate;
    private final transient CourierTarget.Request target;
    private final transient Rigidbody2D support, obstacle;
    private transient int beginContacts;
    private transient int activeContacts;
    private transient boolean horizontalImpulseSeen;
    private transient float normalImpulse;
    private transient long impulseTick = -1;
    private transient long firstContactTick = -1;
    private transient Float crateXAtFirstContact;

    private CourierCrateTrial(GameObject player, Rigidbody2D crate, CourierTarget.Request target,
                              Rigidbody2D support, Rigidbody2D obstacle) {
        this.player = player;
        this.crate = crate;
        this.target = target;
        this.support = support;
        this.obstacle = obstacle;
    }

    public static boolean selected() {
        return Boolean.getBoolean("fraynix.courierCrate");
    }

    public static void attach(Scene scene, CourierTarget.Request target, CourierRecovery.Setup recovery) {
        if (!content.RelayGardenResources.enabled() ||
                content.RelayGardenResources.course() != fraymus.neural.RelayCourse.GARDEN)
            throw new IllegalStateException("The Courier crate fixture needs the checked Relay Garden.");
        verifyImage(ATLAS, ATLAS_HASH);
        verifyImage(CRATE, CRATE_HASH);
        GameObject player = scene.getGameObjectWith(PlayerController.class);
        if (player == null || player.getComponent(PillboxCollider.class) == null)
            throw new IllegalStateException("The Relay player pillbox is required.");

        Rigidbody2D support = null, obstacle = null;
        if (target != null) {
            player.transform.position.x = recovery == null
                    ? CourierTarget.START_X - target.direction() * .73f : recovery.spawnX();
            player.transform.scale.x = Math.copySign(player.transform.scale.x, target.direction());
            support = addContinuousDeck(scene, recovery == null ? .1f : recovery.supportFriction());
            if (recovery != null && recovery.obstacle()) obstacle = addHurdle(scene, recovery.obstacleX());
            addTargetMarker(scene, target.targetX());
        }
        var object = scene.createGameObject("Courier cargo crate");
        object.transform.position.set(target == null ? 1.62f : CourierTarget.START_X, .5f + CRATE_WIDTH / 2);
        object.transform.scale.set(CRATE_WIDTH);
        object.transform.zIndex = 3;
        var sprite = new Sprite();
        sprite.setTexture(AssetPool.getTexture(CRATE));
        var artwork = new SpriteRenderer();
        artwork.setSprite(sprite);
        object.addComponent(artwork);
        var collider = new Box2DCollider();
        collider.setHalfSize(new Vector2f(CRATE_WIDTH));
        object.addComponent(collider);
        var body = new Rigidbody2D();
        body.setMass(target == null ? 8 : target.mass() / (CRATE_WIDTH * CRATE_WIDTH));
        if (target != null) body.setInitialFriction(target.friction());
        body.setFixedRotation(true);
        body.setLinearDamping(1.1f);
        object.addComponent(body);
        object.addComponent(new CourierCrateTrial(player, body, target, support, obstacle));
        scene.addGameObjectToScene(object);

        // Keep the player and its original state machine. Only the visible art is substituted.
        player.getComponent(SpriteRenderer.class).setColor(new Vector4f(0, 0, 0, 0));
        var visual = scene.createGameObject("Teal Courier artwork");
        visual.transform.zIndex = 4;
        var courier = new SpriteRenderer();
        visual.addComponent(courier);
        var texture = AssetPool.getTexture(ATLAS);
        if (texture.getWidth() != 288 || texture.getHeight() != 336)
            throw new IllegalStateException("The Courier atlas dimensions changed.");
        visual.addComponent(new CourierMotion(player, courier, new Spritesheet(texture, 96, 112, 9, 0)));
        scene.addGameObjectToScene(visual);
        scene.camera().position.set(.1f, target == null ? -.15f : -.42f);
        scene.camera().getProjectionSize().set(3.85f, 3.85f * 9 / 16);
        scene.camera().clearColor.set(.04f, .07f, .13f, 1);
        scene.camera().adjustProjection();
    }

    private static Rigidbody2D addContinuousDeck(Scene scene, float friction) {
        // Tile artwork stays in place. One flat fixture gives the delivery a controlled surface.
        for (GameObject tile : scene.getGameObjects()) {
            if (tile.getName().startsWith("Garden deck ") && tile.transform.position.x < 4.5f) {
                tile.removeComponent(components.Ground.class);
                tile.removeComponent(Box2DCollider.class);
                tile.removeComponent(Rigidbody2D.class);
            }
        }
        var deck = scene.createGameObject("Courier flat test deck");
        deck.transform.position.set(2.25f, .25f);
        var collider = new Box2DCollider();
        collider.setHalfSize(new Vector2f(4.5f, .5f));
        var body = new Rigidbody2D();
        body.setBodyType(physics2d.enums.BodyType.Static);
        body.setInitialFriction(friction);
        deck.addComponent(new components.Ground());
        deck.addComponent(collider);
        deck.addComponent(body);
        scene.addGameObjectToScene(deck);
        return body;
    }

    private static Rigidbody2D addHurdle(Scene scene, float x) {
        var hurdle = scene.createGameObject("Courier approach hurdle");
        hurdle.transform.position.set(x, .565f);
        hurdle.transform.scale.set(.14f, .13f);
        hurdle.transform.zIndex = 1;
        var atlas = AssetPool.getTexture(CRATE);
        var sprite = new Sprite();
        sprite.setTexture(atlas);
        var art = new SpriteRenderer();
        art.setSprite(sprite);
        hurdle.addComponent(art);
        var collider = new Box2DCollider();
        collider.setHalfSize(new Vector2f(.14f, .13f));
        var body = new Rigidbody2D();
        body.setBodyType(physics2d.enums.BodyType.Static);
        hurdle.addComponent(new components.Ground());
        hurdle.addComponent(collider);
        hurdle.addComponent(body);
        scene.addGameObjectToScene(hurdle);
        return body;
    }

    private static void addTargetMarker(Scene scene, float x) {
        verifyImage(TARGET, TARGET_HASH);
        var marker = scene.createGameObject("Courier cargo target");
        marker.transform.position.set(x, .66f);
        marker.transform.scale.set(.35f);
        marker.transform.zIndex = 2;
        var sprite = new Sprite();
        sprite.setTexture(AssetPool.getTexture(TARGET));
        var renderer = new SpriteRenderer();
        renderer.setSprite(sprite);
        marker.addComponent(renderer);
        scene.addGameObjectToScene(marker);
    }

    private static void verifyImage(String path, String sha) {
        String actual;
        try {
            actual = content.RelayGardenGenerator.hash(Path.of(path));
        } catch (Exception error) {
            throw new IllegalStateException("Courier image unavailable: " + path, error);
        }
        if (!sha.equals(actual)) throw new IllegalStateException("Courier image changed: " + path);
    }

    @Override
    public void start() {
        if (target == null) return;
        // The Relay pillbox's authored density is used on this trial's existing fixtures.
        var authored = player.getComponent(Rigidbody2D.class);
        var body = authored.getRawBody();
        for (var fixture = body.getFixtureList(); fixture != null; fixture = fixture.getNext())
            fixture.setDensity(authored.getMass());
        body.resetMassData();
    }

    @Override
    public void beginCollision(GameObject other, Contact contact, Vector2f normal) {
        if (other != player) return;
        beginContacts++;
        activeContacts++;
        if (firstContactTick < 0) {
            firstContactTick = Window.getScene().tick();
            crateXAtFirstContact = crate.getRawBody().getPosition().x;
        }
    }

    @Override
    public void endCollision(GameObject other, Contact contact, Vector2f normal) {
        if (other == player) activeContacts--;
    }

    @Override
    public void postSolve(GameObject other, Contact contact, Vector2f normal, ContactImpulse impulse) {
        if (other != player || Math.abs(normal.x) < .65f) return;
        long tick = Window.getScene().tick();
        if (impulseTick != tick) {
            normalImpulse = 0;
            impulseTick = tick;
        }
        for (int index = 0; index < impulse.count; index++) normalImpulse += impulse.normalImpulses[index];
        horizontalImpulseSeen |= normalImpulse > 0;
    }

    static CourierCrateTrial current() {
        var crate = Window.getScene().getGameObjectWith(CourierCrateTrial.class);
        if (crate == null) throw new IllegalStateException("Open Courier crate test first.");
        return crate.getComponent(CourierCrateTrial.class);
    }

    Body playerBody() {
        return player.getComponent(Rigidbody2D.class).getRawBody();
    }

    Body crateBody() { return crate.getRawBody(); }
    Body supportBody() { return support.getRawBody(); }
    Body obstacleBody() { return obstacle == null ? null : obstacle.getRawBody(); }
    PlayerController controller() { return player.getComponent(PlayerController.class); }
    String playerId() { return player.getPersistentId().toString(); }
    Float firstContactX() { return crateXAtFirstContact; }
    int contacts() { return activeContacts; }
    int begins() { return beginContacts; }
    boolean hasHorizontalImpulse() { return horizontalImpulseSeen; }
    float horizontalImpulse() { return impulseTick == Window.getScene().tick() ? normalImpulse : 0; }

    private Row sample(int input) {
        var courier = playerBody();
        var cargo = crate.getRawBody();
        long tick = Window.getScene().tick();
        return new Row(tick, input, courier.getPosition().x, courier.getPosition().y,
                cargo.getPosition().x, cargo.getPosition().y, cargo.getLinearVelocity().x,
                beginContacts, impulseTick == tick ? normalImpulse : 0);
    }

    Map<String, Object> live() {
        var cargo = crate.getRawBody();
        var value = Map.<String, Object>of("crateX", cargo.getPosition().x, "crateVx", cargo.getLinearVelocity().x,
                "firstContactTick", firstContactTick, "beginContacts", beginContacts);
        if (target == null) return value;
        var observed = new java.util.LinkedHashMap<>(value);
        observed.put("targetX", target.targetX());
        observed.put("activeContacts", activeContacts);
        return observed;
    }

    static Map<String, Object> sourceBindings() {
        return Map.of("atlasSha256", ATLAS_HASH, "crateArtSha256", CRATE_HASH,
                "levelSha256", System.getProperty("fraynix.level.sha256"),
                "engine", "Game Lab / JBox2D", "tickHz", 60);
    }

    static Map<String, Object> runPair() {
        Run control = run(false);
        Run push = run(true);
        return Map.of(
                "playerId", current().player.getPersistentId().toString(),
                "atlasSha256", ATLAS_HASH, "crateArtSha256", CRATE_HASH,
                "levelSha256", System.getProperty("fraynix.level.sha256"),
                "engine", "Game Lab / JBox2D", "tickHz", 60,
                "control", control, "push", push);
    }

    private static Run run(boolean finishApproach) {
        Window.changeScene(new LevelSceneInitializer());
        var trial = current();
        var courier = trial.playerBody();
        var cargo = trial.crate.getRawBody();
        var rows = new ArrayList<Row>(TRIAL_TICKS + 1);
        rows.add(trial.sample(0));
        try {
            for (int tick = 1; tick <= TRIAL_TICKS; tick++) {
                int input = tick > SETTLE_TICKS &&
                        (finishApproach || tick <= SETTLE_TICKS + APPROACH_TICKS) ? 1 : 0;
                agent.GameInput.external(input, false);
                Window.get().stepEmbedded();
                rows.add(trial.sample(input));
            }
        } finally {
            agent.GameInput.externalOff();
        }
        return new Run(courier.getType().name(), courier.getMass(), courier.isFixedRotation(),
                cargo.getType().name(), cargo.getMass(), trial.crateXAtFirstContact, rows);
    }

    private record Row(long tick, int input, float playerX, float playerY, float crateX, float crateY,
                       float crateVx, int beginContacts, float horizontalNormalImpulse) {}
    private record Run(String playerType, float playerMass, boolean playerFixedRotation,
                       String crateType, float crateMass, Float crateXAtFirstContact, List<Row> rows) {}

    private static final class CourierMotion extends Component {
        private final transient GameObject player;
        private final transient SpriteRenderer renderer;
        private final transient Spritesheet poses;
        private transient int pose = -1;
        private transient float distance;
        private transient float previousX;
        private transient boolean previouslyGrounded;
        private transient long landingTick = -60;

        CourierMotion(GameObject player, SpriteRenderer renderer, Spritesheet poses) {
            this.player = player;
            this.renderer = renderer;
            this.poses = poses;
        }

        @Override
        public void start() {
            previousX = player.transform.position.x;
            display(0);
        }

        @Override
        public void update(float dt) {
            var controller = player.getComponent(PlayerController.class);
            var body = player.getComponent(Rigidbody2D.class).getRawBody();
            boolean grounded = controller.onGround;
            long tick = Window.getScene().tick();
            if (grounded && !previouslyGrounded && tick > 2) landingTick = tick;
            float travel = Math.abs(body.getPosition().x - previousX);
            if (grounded) distance += travel;
            previousX = body.getPosition().x;
            int index;
            if (!grounded) index = body.getLinearVelocity().y > 1 ? 6 : 7;
            else if (tick - landingTick < 5) index = 8;
            else if (travel > .0008f) index = 2 + (int)(distance / .09f) % 4;
            else index = tick % 133 >= 127 ? 1 : 0;
            display(index);
            previouslyGrounded = grounded;
        }

        private void display(int index) {
            if (pose != index) {
                renderer.setSprite(poses.getSprite(index));
                pose = index;
            }
            // The larger visual quad still follows the original collider's foot position.
            gameObject.transform.position.set(player.transform.position).add(0, .075f);
            gameObject.transform.scale.set(Math.copySign(.39f, player.transform.scale.x), .46f);
        }
    }
}
