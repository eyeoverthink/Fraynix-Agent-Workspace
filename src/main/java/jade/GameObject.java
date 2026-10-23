package jade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.Component;
import components.ComponentDeserializer;
import components.SpriteRenderer;
import imgui.ImGui;
import util.AssetPool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameObject {
    private static final java.util.concurrent.atomic.AtomicInteger ID_COUNTER = new java.util.concurrent.atomic.AtomicInteger();
    private int uid = -1;
    private UUID persistentId = UUID.randomUUID();

    private String name;
    private List<Component> components;
    public transient Transform transform;
    private boolean doSerialization = true;
    private boolean isDead = false;

    public GameObject(String name) {
        this.name = name;
        this.components = new ArrayList<>();

        this.uid = ID_COUNTER.getAndIncrement();
    }

    public <T extends Component> T getComponent(Class<T> componentClass) {
        for (Component c : components) {
            if (componentClass.isAssignableFrom(c.getClass())) {
                try {
                    return componentClass.cast(c);
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    assert false : "Error: Casting component.";
                }
            }
        }

        return null;
    }

    public <T extends Component> void removeComponent(Class<T> componentClass) {
        for (int i=0; i < components.size(); i++) {
            Component c = components.get(i);
            if (componentClass.isAssignableFrom(c.getClass())) {
                components.remove(i);
                return;
            }
        }
    }

    public void addComponent(Component c) {
        c.generateId();
        this.components.add(c);
        c.gameObject = this;
    }

    public void update(float dt) {
        for (int i=0; i < components.size(); i++) {
            components.get(i).update(dt);
        }
    }

    public void editorUpdate(float dt) {
        for (int i=0; i < components.size(); i++) {
            components.get(i).editorUpdate(dt);
        }
    }

    public void start() {
        for (int i=0; i < components.size(); i++) {
            components.get(i).start();
        }
    }

    public void imgui() {
        for (Component c : components) {
            int flags = c instanceof physics2d.components.Rigidbody2D || c instanceof physics2d.components.Collider
                ? imgui.flag.ImGuiTreeNodeFlags.DefaultOpen : 0;
            ImGui.pushID(c.getUid());
            if (ImGui.collapsingHeader(c.getClass().getSimpleName(), flags))
                c.imgui();
            ImGui.popID();
        }
    }

    public void destroy() {
        this.isDead = true;
        for (int i=0; i < components.size(); i++) {
            components.get(i).destroy();
        }
    }

    public GameObject copy() {
        // TODO: come up with cleaner solution
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
                .create();
        String objAsJson = gson.toJson(this);
        GameObject obj = gson.fromJson(objAsJson, GameObject.class);

        obj.generateUid();
        obj.persistentId = UUID.randomUUID();
        for (Component c : obj.getAllComponents()) {
            c.regenerateIdentity();
        }

        SpriteRenderer sprite = obj.getComponent(SpriteRenderer.class);
        if (sprite != null && sprite.getTexture() != null) {
            sprite.setTexture(AssetPool.getTexture(sprite.getTexture().getFilepath()));
        }

        return obj;
    }

    public boolean isDead() {
        return this.isDead;
    }

    public static void init(int maxId) {
        ID_COUNTER.accumulateAndGet(maxId, Math::max);
    }

    public int getUid() {
        return this.uid;
    }

    public List<Component> getAllComponents() {
        return this.components;
    }

    public void setNoSerialize() {
        this.doSerialization = false;
    }

    public void generateUid() {
        this.uid = ID_COUNTER.getAndIncrement();
    }

    public UUID getPersistentId() { return persistentId; }
    public String getName() { return name; }

    public void restoreIdentity(int uid, UUID persistentId) {
        if (uid < 0) throw new IllegalArgumentException("Negative object id");
        this.uid = uid;
        this.persistentId = persistentId;
        ID_COUNTER.accumulateAndGet(uid + 1, Math::max);
    }

    public boolean doSerialization() {
        return this.doSerialization;
    }
}
