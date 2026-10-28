package scenes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.Component;
import components.ComponentDeserializer;
import imgui.ImGui;
import jade.Camera;
import jade.GameObject;
import jade.GameObjectDeserializer;
import jade.Transform;
import org.joml.Vector2f;
import physics2d.Physics2D;
import renderer.Renderer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scene {

    private Renderer renderer;
    private Camera camera;
    private boolean isRunning;
    private List<GameObject> gameObjects;
    private Physics2D physics2D;
    private final List<GameObject> pendingObjects = new ArrayList<>();
    private double elapsed;
    private long tick;
    public static final float FIXED_STEP = 1f / 60f;
    public long tick() { return tick; }

    private SceneInitializer sceneInitializer;

    public Scene(SceneInitializer sceneInitializer) {
        this.sceneInitializer = sceneInitializer;
        this.physics2D = new Physics2D();
        this.renderer = new Renderer();
        this.gameObjects = new ArrayList<>();
        this.isRunning = false;
    }

    public void init() {
        this.camera = new Camera(new Vector2f(0, 0));
        this.sceneInitializer.loadResources(this);
        this.sceneInitializer.init(this);
    }

    public void start() {
        for (int i=0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.start();
            this.renderer.add(go);
            this.physics2D.add(go);
        }
        isRunning = true;
    }

    public void addGameObjectToScene(GameObject go) {
        if (!isRunning) {
            gameObjects.add(go);
        } else {
            pendingObjects.add(go);
        }
    }

    public void destroy() {
        for (GameObject go : gameObjects) {
            go.destroy();
            physics2D.destroyGameObject(go);
        }
        renderer.dispose();
    }

    public List<GameObject> getGameObjects() {
        return this.gameObjects;
    }

    public GameObject getGameObject(int gameObjectId) {
        Optional<GameObject> result = this.gameObjects.stream()
                .filter(gameObject -> gameObject.getUid() == gameObjectId)
                .findFirst();
        return result.orElse(null);
    }

    public void editorUpdate(float dt) {
        flushPending();
        this.camera.adjustProjection();

        for (int i=0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.editorUpdate(dt);
            this.physics2D.synchronize(go);

            if (go.isDead()) {
                gameObjects.remove(i);
                this.renderer.destroyGameObject(go);
                this.physics2D.destroyGameObject(go);
                i--;
            }
        }
    }

    public void update(float dt) {
        if (!Float.isFinite(dt) || dt < 0) throw new IllegalArgumentException("Invalid elapsed time");
        elapsed += Math.min(dt, .25f);
        while (elapsed + 1e-7 >= FIXED_STEP) {
            elapsed -= FIXED_STEP;
            fixedUpdate();
        }
    }

    private void flushPending() {
        if (pendingObjects.isEmpty()) return;
        var pending = new ArrayList<>(pendingObjects); pendingObjects.clear();
        for (GameObject object : pending) {
            gameObjects.add(object); object.start(); renderer.add(object); physics2D.add(object);
        }
    }

    private void fixedUpdate() {
        flushPending();
        tick++;
        agent.GameInput.tick(this);
        this.camera.adjustProjection();
        for (GameObject object : gameObjects) {
            physics2d.components.Rigidbody2D body = object.getComponent(physics2d.components.Rigidbody2D.class);
            if (body != null && body.getRawBody() == null) this.physics2D.add(object);
        }
        this.physics2D.update(FIXED_STEP);

        for (int i=0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.update(FIXED_STEP);

            if (go.isDead()) {
                gameObjects.remove(i);
                this.renderer.destroyGameObject(go);
                this.physics2D.destroyGameObject(go);
                i--;
            }
        }
    }

    public void render() {
        this.renderer.render();
    }

    public Camera camera() {
        return this.camera;
    }

    public Physics2D physics() { return physics2D; }

    public void imgui() {
        this.sceneInitializer.imgui();
    }

    public GameObject createGameObject(String name) {
        GameObject go = new GameObject(name);
        go.addComponent(new Transform());
        go.transform = go.getComponent(Transform.class);
        return go;
    }

    public void save() {
        try { save(WindowSavePath.path()); }
        catch (IOException error) { throw new java.io.UncheckedIOException("Scene save failed", error); }
    }

    public void save(Path path) throws IOException {
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
                .create();

        Path target = path.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "scene-", ".pending");
        try {
            List<GameObject> objsToSerialize = new ArrayList<>();
            for (GameObject obj : this.gameObjects) {
                if (obj.doSerialization()) {
                    objsToSerialize.add(obj);
                }
            }
            Files.writeString(temporary, gson.toJson(objsToSerialize), StandardCharsets.UTF_8);
            try (java.nio.channels.FileChannel file = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) { file.force(true); }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch(java.nio.file.AtomicMoveNotSupportedException error) { Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public void load() {
        Path path = Paths.get(System.getProperty("fraynix.level", "level.txt"));
        Path authored=WindowSavePath.path();
        if (Files.isRegularFile(authored)) path=authored;
        if (!Files.exists(path)) return;
        try { load(path); }
        catch (IOException error) { throw new java.io.UncheckedIOException("Scene load failed", error); }
    }

    public void load(Path path) throws IOException {
        if (!gameObjects.isEmpty() || isRunning) throw new IllegalStateException("Load requires a new empty scene");
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
                .create();

        String inFile = Files.readString(path, StandardCharsets.UTF_8);

        if (!inFile.equals("")) {
            int maxGoId = -1;
            int maxCompId = -1;
            GameObject[] objs = gson.fromJson(inFile, GameObject[].class);
            // Older clone saves reused editor handles. Repair those transient handles once on import;
            // entity/component persistent UUIDs remain unchanged.
            var objectHandles = new java.util.HashSet<Integer>();
            var componentHandles = new java.util.HashSet<Integer>();
            for (int i=0; i < objs.length; i++) {
                while (!objectHandles.add(objs[i].getUid())) objs[i].generateUid();
                addGameObjectToScene(objs[i]);

                for (Component c : objs[i].getAllComponents()) {
                    while (!componentHandles.add(c.getUid())) c.regenerateEditorId();
                    if (c.getUid() > maxCompId) {
                        maxCompId = c.getUid();
                    }
                }
                if (objs[i].getUid() > maxGoId) {
                    maxGoId = objs[i].getUid();
                }
            }

            maxGoId++;
            maxCompId++;
            GameObject.init(maxGoId);
            Component.init(maxCompId);
        }
    }

    public <T extends Component> GameObject getGameObjectWith(Class<T> type) {
        return gameObjects.stream().filter(o -> !o.isDead() && o.getComponent(type) != null).findFirst().orElse(null);
    }
    public GameObject getGameObject(String name) {
        return gameObjects.stream().filter(o -> o.getName().equals(name)).findFirst().orElse(null);
    }
    public String serializeAuthoring() {
        return sceneGson().toJson(gameObjects.stream().filter(GameObject::doSerialization).filter(o -> !o.isDead()).toList());
    }
    private static Gson sceneGson() {
        return new GsonBuilder().enableComplexMapKeySerialization()
            .registerTypeAdapter(Component.class, new ComponentDeserializer())
            .registerTypeAdapter(GameObject.class, new GameObjectDeserializer()).create();
    }
    public void loadJson(String json) {
        if (!gameObjects.isEmpty() || isRunning) throw new IllegalStateException("Load requires an empty scene");
        var ids = new java.util.HashSet<Integer>();
        for (GameObject object : sceneGson().fromJson(json, GameObject[].class)) {
            for (Component component : object.getAllComponents())
                while (!ids.add(component.getUid())) component.regenerateEditorId();
            addGameObjectToScene(object);
        }
    }


    private static final class WindowSavePath {
        static Path path() { return jade.Window.getFactorySession().workspace().resolve("authored-scene.json"); }
    }

}
