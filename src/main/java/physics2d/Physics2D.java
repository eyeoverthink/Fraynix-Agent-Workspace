package physics2d;

import jade.GameObject;
import jade.Window;
import components.Ground;
import physics2d.components.PillboxCollider;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import jade.Transform;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.joml.Vector2f;
import physics2d.components.Box2DCollider;
import physics2d.components.CircleCollider;
import physics2d.components.Rigidbody2D;

public class Physics2D {
    private Vec2 gravity = new Vec2(0, -10.0f);
    private World world = new World(gravity);

    private double physicsTime = 0.0;
    private float physicsTimeStep = 1.0f / 60.0f;
    private int velocityIterations = 8;
    private int positionIterations = 3;
    private final java.util.Map<GameObject, java.util.List<Object>> editorStates = new java.util.IdentityHashMap<>();
    private final java.util.Map<GameObject, Body> registeredBodies = new java.util.IdentityHashMap<>();
    private final java.util.Map<GameObject, String> editorErrors = new java.util.IdentityHashMap<>();

    public int bodyCount() { return world.getBodyCount(); }
    public int contactCount() { return world.getContactCount(); }
    public java.util.Map<String, Number> settings() {
        return java.util.Map.of("gravityX", world.getGravity().x, "gravityY", world.getGravity().y,
            "fixedTimeStep", physicsTimeStep, "velocityIterations", velocityIterations, "positionIterations", positionIterations);
    }
    public String editorError() { return String.join("; ", editorErrors.values()); }

    /** Editor components can be attached or edited after a scene has already started. */
    public void synchronize(GameObject go) {
        Rigidbody2D body = go.getComponent(Rigidbody2D.class);
        if (body == null || (go.getComponent(Box2DCollider.class) == null && go.getComponent(CircleCollider.class) == null && go.getComponent(PillboxCollider.class) == null)) {
            destroyGameObject(go);
            return;
        }
        try { validate(go); editorErrors.remove(go); }
        catch (IllegalArgumentException error) { editorErrors.put(go, go.getName() + ": " + error.getMessage()); return; }
        java.util.List<Object> state = editorState(go);
        if (body.getRawBody() == null || !state.equals(editorStates.get(go))) {
            validate(go);
            destroyGameObject(go);
            add(go);
        }
    }

    private java.util.List<Object> editorState(GameObject go) {
        Rigidbody2D rb = go.getComponent(Rigidbody2D.class);
        var values = new java.util.ArrayList<Object>(java.util.List.of(rb, rb.getBodyType(), rb.getMass(),
            rb.getAngularDamping(), rb.getLinearDamping(), rb.isFixedRotation(), rb.isContinuousCollision(),
            rb.getVelocity().x, rb.getVelocity().y, go.transform.position.x, go.transform.position.y, go.transform.rotation));
        Box2DCollider box = go.getComponent(Box2DCollider.class);
        CircleCollider circle = go.getComponent(CircleCollider.class);
        PillboxCollider pill = go.getComponent(PillboxCollider.class);
        if (box != null) values.addAll(java.util.List.of("box", box.getHalfSize().x, box.getHalfSize().y,
            box.getOrigin().x, box.getOrigin().y, box.getOffset().x, box.getOffset().y));
        if (circle != null) values.addAll(java.util.List.of("circle", circle.getRadius(), circle.getOffset().x, circle.getOffset().y));
        if (pill != null) values.addAll(java.util.List.of("pill", pill.width, pill.height, pill.offset.x, pill.offset.y));
        return values;
    }

    public static void validate(GameObject go) {
        if (go.transform == null) throw new IllegalArgumentException("Scene object needs a Transform");
        for (float value : new float[]{go.transform.position.x, go.transform.position.y, go.transform.rotation,
                go.transform.scale.x, go.transform.scale.y}) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite object transform");
        }
        Rigidbody2D rb = go.getComponent(Rigidbody2D.class);
        if (rb != null && (rb.getBodyType() == null || !Float.isFinite(rb.getMass()) || rb.getMass() < 0)) throw new IllegalArgumentException("Invalid rigidbody");
        if (rb != null) {
            if (rb.getVelocity() == null) throw new IllegalArgumentException("Missing body velocity");
            for (float value : new float[]{rb.getVelocity().x, rb.getVelocity().y, rb.getLinearDamping(), rb.getAngularDamping()}) {
                if (!Float.isFinite(value)) throw new IllegalArgumentException("Invalid body velocity or damping");
            }
            if (rb.getLinearDamping() < 0 || rb.getAngularDamping() < 0) throw new IllegalArgumentException("Damping must be nonnegative");
        }
        Box2DCollider box = go.getComponent(Box2DCollider.class);
        CircleCollider circle = go.getComponent(CircleCollider.class);
        if (box != null && circle != null) throw new IllegalArgumentException("An object needs one collider type");
        PillboxCollider pill=go.getComponent(PillboxCollider.class);
        if (pill!=null && (box!=null || circle!=null)) throw new IllegalArgumentException("Pillbox requires its own compound fixtures");
        if (pill!=null && (!Float.isFinite(pill.width) || !Float.isFinite(pill.height) || pill.width<=.01f
            || pill.height<=pill.width/2 || !Float.isFinite(pill.offset.x) || !Float.isFinite(pill.offset.y)))
            throw new IllegalArgumentException("Invalid pillbox geometry");
        if (box != null && (!(box.getHalfSize().x > 0) || !(box.getHalfSize().y > 0)
                || !Float.isFinite(box.getHalfSize().x) || !Float.isFinite(box.getHalfSize().y))) throw new IllegalArgumentException("Invalid box size");
        if (box != null && (!Float.isFinite(box.getOrigin().x) || !Float.isFinite(box.getOrigin().y))) throw new IllegalArgumentException("Invalid box origin");
        if (circle != null && (!(circle.getRadius() > 0) || !Float.isFinite(circle.getRadius()))) throw new IllegalArgumentException("Invalid circle radius");
        for (physics2d.components.Collider collider : new physics2d.components.Collider[]{box, circle}) {
            if (collider != null && (!Float.isFinite(collider.getOffset().x) || !Float.isFinite(collider.getOffset().y))) throw new IllegalArgumentException("Invalid collider offset");
        }
    }

    public void add(GameObject go) {
        Rigidbody2D rb = go.getComponent(Rigidbody2D.class);
        if (rb != null && rb.getRawBody() == null) {
            validate(go);
            Transform transform = go.transform;

            BodyDef bodyDef = new BodyDef();
            bodyDef.angle = (float)Math.toRadians(transform.rotation);
            bodyDef.position.set(transform.position.x, transform.position.y);
            bodyDef.angularDamping = rb.getAngularDamping();
            bodyDef.linearDamping = rb.getLinearDamping();
            bodyDef.fixedRotation = rb.isFixedRotation();
            bodyDef.bullet = rb.isContinuousCollision();
            bodyDef.linearVelocity.set(rb.getVelocity().x, rb.getVelocity().y);
            bodyDef.gravityScale = rb.gravityScale;
            bodyDef.angularVelocity = rb.angularVelocity;
            bodyDef.userData = go;

            switch (rb.getBodyType()) {
                case Kinematic: bodyDef.type = BodyType.KINEMATIC; break;
                case Static: bodyDef.type = BodyType.STATIC; break;
                case Dynamic: bodyDef.type = BodyType.DYNAMIC; break;
            }

            Shape shape;
            CircleCollider circleCollider;
            Box2DCollider boxCollider;

            if ((circleCollider = go.getComponent(CircleCollider.class)) != null) {
                if (!Float.isFinite(circleCollider.getRadius()) || circleCollider.getRadius() <= 0) {
                    throw new IllegalArgumentException("Circle radius must be positive and finite");
                }
                CircleShape circle = new CircleShape();
                circle.m_radius = circleCollider.getRadius();
                circle.m_p.set(circleCollider.getOffset().x, circleCollider.getOffset().y);
                shape = circle;
            } else if ((boxCollider = go.getComponent(Box2DCollider.class)) != null) {
                Vector2f halfSize = new Vector2f(boxCollider.getHalfSize()).mul(0.5f);
                Vector2f offset = boxCollider.getOffset();
                Vector2f origin = new Vector2f(boxCollider.getOrigin());
                if (!Float.isFinite(halfSize.x) || !Float.isFinite(halfSize.y) || halfSize.x <= 0 || halfSize.y <= 0) {
                    throw new IllegalArgumentException("Box size must be positive and finite");
                }
                PolygonShape polygon = new PolygonShape();
                polygon.setAsBox(halfSize.x, halfSize.y, new Vec2(origin.x + offset.x, origin.y + offset.y), 0);
                shape = polygon;
            } else if (go.getComponent(PillboxCollider.class) != null) {
                Body body = world.createBody(bodyDef); rb.setRawBody(body);
                PillboxCollider pill = go.getComponent(PillboxCollider.class);
                pill.recalculateColliders(); addPillboxCollider(rb, pill);
                registeredBodies.put(go, body); editorStates.put(go, editorState(go)); return;
            } else { return; } // A rigidbody can be attached before its collider in the editor.

            if (!Float.isFinite(rb.getMass()) || rb.getMass() < 0) throw new IllegalArgumentException("Invalid body mass");

            Body body = this.world.createBody(bodyDef);
            rb.setRawBody(body);
            FixtureDef fixture = new FixtureDef(); fixture.shape = shape;
            fixture.density = rb.getMass() > 0 ? rb.getMass() : 1;
            fixture.friction = rb.getFriction(); fixture.isSensor = rb.isSensor(); fixture.userData = go;
            body.createFixture(fixture);
            registeredBodies.put(go, body);
            editorStates.put(go, editorState(go));
        }
    }

    public void destroyGameObject(GameObject go) {
        Body tracked = registeredBodies.remove(go);
        if (tracked != null) world.destroyBody(tracked);
        editorStates.remove(go);
        editorErrors.remove(go);
        Rigidbody2D rb = go.getComponent(Rigidbody2D.class);
        if (rb != null) {
            if (rb.getRawBody() != null) {
                if (tracked == null) world.destroyBody(rb.getRawBody());
                rb.setRawBody(null);
            }
        }
    }

    public void update(float dt) {
        if (!Float.isFinite(dt) || dt < 0) throw new IllegalArgumentException("Invalid elapsed time");
        physicsTime += dt;
        while (physicsTime + 1e-7 >= physicsTimeStep) {
            physicsTime -= physicsTimeStep;
            world.step(physicsTimeStep, velocityIterations, positionIterations);
        }
    }

    public Physics2D() { world.setContactListener(new JadeContactListener()); }
    public Vector2f getGravity() { return new Vector2f(world.getGravity().x, world.getGravity().y); }

    public void setIsSensor(Rigidbody2D rb) {
        Body body = rb.getRawBody();
        if (body == null) return;

        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            fixture.setSensor(true);
            fixture = fixture.m_next;
        }
    }

    public void setNotSensor(Rigidbody2D rb) {
        Body body = rb.getRawBody();
        if (body == null) return;

        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            fixture.setSensor(false);
            fixture = fixture.m_next;
        }
    }

    public void resetCircleCollider(Rigidbody2D rb, CircleCollider circleCollider) {
        Body body = rb.getRawBody();
        if (body == null) return;

        int size = fixtureListSize(body);
        for (int i = 0; i < size; i++) {
            body.destroyFixture(body.getFixtureList());
        }

        addCircleCollider(rb, circleCollider);
        body.resetMassData();
    }

    public void resetBox2DCollider(Rigidbody2D rb, Box2DCollider boxCollider) {
        Body body = rb.getRawBody();
        if (body == null) return;

        int size = fixtureListSize(body);
        for (int i = 0; i < size; i++) {
            body.destroyFixture(body.getFixtureList());
        }

        addBox2DCollider(rb, boxCollider);
        body.resetMassData();
    }

    public void resetPillboxCollider(Rigidbody2D rb, PillboxCollider pb) {
        Body body = rb.getRawBody();
        if (body == null) return;

        int size = fixtureListSize(body);
        for (int i = 0; i < size; i++) {
            body.destroyFixture(body.getFixtureList());
        }

        addPillboxCollider(rb, pb);
        body.resetMassData();
    }

    public void addPillboxCollider(Rigidbody2D rb, PillboxCollider pb) {
        Body body = rb.getRawBody();
        assert body != null : "Raw body must not be null";

        addBox2DCollider(rb, pb.getBox());
        addCircleCollider(rb, pb.getBottomCircle());
    }

    public void addBox2DCollider(Rigidbody2D rb, Box2DCollider boxCollider) {
        Body body = rb.getRawBody();
        assert body != null : "Raw body must not be null";

        PolygonShape shape = new PolygonShape();
        Vector2f halfSize = new Vector2f(boxCollider.getHalfSize()).mul(0.5f);
        Vector2f offset = boxCollider.getOffset();
        Vector2f origin = new Vector2f(boxCollider.getOrigin());
        shape.setAsBox(halfSize.x, halfSize.y, new Vec2(origin.x+offset.x, origin.y+offset.y), 0);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = rb.getFriction();
        fixtureDef.userData = rb.gameObject;
        fixtureDef.isSensor = rb.isSensor();
        body.createFixture(fixtureDef);
    }

    public void addCircleCollider(Rigidbody2D rb, CircleCollider circleCollider) {
        Body body = rb.getRawBody();
        assert body != null : "Raw body must not be null";

        CircleShape shape = new CircleShape();
        shape.setRadius(circleCollider.getRadius());
        shape.m_p.set(circleCollider.getOffset().x, circleCollider.getOffset().y);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = rb.getFriction();
        fixtureDef.userData = rb.gameObject;
        fixtureDef.isSensor = rb.isSensor();
        body.createFixture(fixtureDef);
    }

    public RaycastInfo raycast(GameObject requestingObject, Vector2f point1, Vector2f point2) {
        RaycastInfo callback = new RaycastInfo(requestingObject);
        world.raycast(callback, new Vec2(point1.x, point1.y),
                new Vec2(point2.x, point2.y));
        return callback;
    }
    public RaycastInfo raycastSolid(GameObject requestingObject, Vector2f point1, Vector2f point2) {
        RaycastInfo callback=new RaycastInfo(requestingObject,true);
        world.raycast(callback,new Vec2(point1.x,point1.y),new Vec2(point2.x,point2.y));
        return callback;
    }

    private int fixtureListSize(Body body) {
        int size = 0;
        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            size++;
            fixture = fixture.m_next;
        }
        return size;
    }

    public boolean isLocked() {
        return world.isLocked();
    }

    public static boolean checkOnGround(
            GameObject gameObject,
            float innerPlayerWidth,
            float height) {
        Vector2f raycastBegin = new Vector2f(gameObject.transform.position);
        raycastBegin.sub(innerPlayerWidth / 2.0f, 0.0f);
        Vector2f raycastEnd = new Vector2f(raycastBegin).add(0.0f, height);

        RaycastInfo info = Window.getPhysics().raycast(gameObject, raycastBegin, raycastEnd);

        Vector2f raycast2Begin = new Vector2f(raycastBegin).add(innerPlayerWidth, 0.0f);
        Vector2f raycast2End = new Vector2f(raycastEnd).add(innerPlayerWidth, 0.0f);
        RaycastInfo info2 = Window.getPhysics().raycast(gameObject, raycast2Begin, raycast2End);

        return (info.hit && info.hitObject != null && info.hitObject.getComponent(Ground.class) != null) ||
                (info2.hit && info2.hitObject != null && info2.hitObject.getComponent(Ground.class) != null);
    }

}
