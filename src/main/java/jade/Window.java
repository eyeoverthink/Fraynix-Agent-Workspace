package jade;

import observers.EventSystem;
import observers.Observer;
import observers.events.Event;
import observers.events.EventType;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import renderer.*;
import scenes.LevelEditorSceneInitializer;
import scenes.Scene;
import scenes.SceneInitializer;
import util.AssetPool;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window implements Observer {
    private int width, height;
    private String title;
    private long glfwWindow;
    private ImGuiLayer imguiLayer;
    private Framebuffer framebuffer;
    private PickingTexture pickingTexture;
    private boolean runtimePlaying = false;
    private final factory.FactorySession factorySession = new factory.FactorySession(java.nio.file.Paths.get(
        System.getProperty("fraynix.studio.workspace", System.getenv().getOrDefault("FRAYNIX_STUDIO_WORKSPACE",
            System.getProperty("fraymus.factoryWorkspace", java.nio.file.Paths.get(System.getProperty("user.home"), ".fraynix-studio", "projects", "default").toString())))));
    private boolean factorySmoke, factorySmokeExit, smokeStarted;
    private long smokeDeadline;
    private boolean smokeRestored;
    private int smokeInspectorFrames;
    private editor.SceneAgentNativeSmoke sceneAgentSmoke;
    private int startupCaptureFrames;
    private boolean startupCaptured;

    private static Window window = null;

    private static Scene currentScene;

    private Window() {
        this.width = 1920;
        this.height = 1080;
        this.title = "Fraynix Studio | Live chat";
        EventSystem.addObserver(this);
    }

    public static void changeScene(SceneInitializer sceneInitializer) {
        if (currentScene != null) {
            currentScene.destroy();
        }

        getImguiLayer().getPropertiesWindow().setActiveGameObject(null);
        currentScene = new Scene(sceneInitializer);
        currentScene.load();
        currentScene.init();
        currentScene.start();
    }

    public static Window get() {
        if (Window.window == null) {
            Window.window = new Window();
        }

        return Window.window;
    }

    public static Scene getScene() {
        return get().currentScene;
    }

    public static factory.FactorySession getFactorySession() { return get().factorySession; }
    public static boolean isPlaying() { return get().runtimePlaying; }

    public void configureFactorySmoke(boolean closeAfter) { factorySmoke = true; factorySmokeExit = closeAfter; }

    public static void installFactoryScene(Scene replacement) {
        Scene before = currentScene;
        GameObject selected = getImguiLayer().getPropertiesWindow().getActiveGameObject();
        org.joml.Vector2f camera = before != null && before.camera() != null ? new org.joml.Vector2f(before.camera().position) : null;
        float zoom = camera == null ? 1 : before.camera().getZoom();
        currentScene = replacement;
        try {
            replacement.init(); replacement.start();
            if (camera != null) { replacement.camera().position.set(camera); replacement.camera().setZoom(zoom); replacement.camera().adjustProjection(); }
        } catch (RuntimeException | Error failure) {
            currentScene = before; replacement.destroy(); throw failure;
        }
        if (before != null) before.destroy();
        getImguiLayer().getPropertiesWindow().setActiveGameObject(selected == null ? null : replacement.getGameObjects().stream()
            .filter(object -> object.getPersistentId().equals(selected.getPersistentId())).findFirst().orElse(null));
        get().runtimePlaying = false;
    }

    public void run() {
        System.out.println("Fraynix Studio | LWJGL " + Version.getVersion());

        init();
        loop();

        if (currentScene != null && !runtimePlaying) {
            try { factorySession.save(currentScene); }
            catch (java.io.IOException | RuntimeException error) { System.err.println("Factory checkpoint failed on exit: " + error); }
        }
        factorySession.close();
        imguiLayer.destroyImGui();

        // Free the memory
        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);

        // Terminate GLFW and the free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init() {
        // Setup an error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);

        // Create the window
        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if (glfwWindow == NULL) {
            throw new IllegalStateException("Failed to create the GLFW window.");
        }

        glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
        glfwSetWindowSizeCallback(glfwWindow, (w, newWidth, newHeight) -> {
            Window.setWidth(newWidth);
            Window.setHeight(newHeight);
        });

        // Make the OpenGL context current
        glfwMakeContextCurrent(glfwWindow);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(glfwWindow);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        this.framebuffer = new Framebuffer(3840, 2160);
        this.pickingTexture = new PickingTexture(3840, 2160);
        glViewport(0, 0, 3840, 2160);

        this.imguiLayer = new ImGuiLayer(glfwWindow, pickingTexture);
        this.imguiLayer.initImGui();

        if (factorySession.hasCheckpoint()) {
            try {
                installFactoryScene(factorySession.restore());
                System.out.println("FACTORY_AUTORESTORE revision=" + factorySession.world().snapshot().revision());
            }
            catch (java.io.IOException error) {
                System.err.println("Factory checkpoint unavailable; opening the packaged scene: " + error);
                Window.changeScene(new LevelEditorSceneInitializer());
                factorySession.reportFailure(error);
            }
        } else { Window.changeScene(new LevelEditorSceneInitializer()); }
    }

    public void loop() {
        double beginTime = glfwGetTime();
        double endTime;
        float dt = 1.0f / 60.0f;

        Shader defaultShader = AssetPool.getShader("assets/shaders/default.glsl");
        Shader pickingShader = AssetPool.getShader("assets/shaders/pickingShader.glsl");

        while (!glfwWindowShouldClose(glfwWindow)) {
            // Poll events
            glfwPollEvents();

            // Render pass 1. Render to picking texture
            glDisable(GL_BLEND);
            pickingTexture.enableWriting();

            glViewport(0, 0, 3840, 2160);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Renderer.bindShader(pickingShader);
            currentScene.render();

            pickingTexture.disableWriting();
            glEnable(GL_BLEND);

            // Render pass 2. Render actual game
            DebugDraw.beginFrame();
            imguiLayer.getFraynixPanel().sceneAgent().drawPreview();

            this.framebuffer.bind();
            glClearColor(1, 1, 1, 1);
            glClear(GL_COLOR_BUFFER_BIT);

            if (dt >= 0) {
                DebugDraw.draw();
                Renderer.bindShader(defaultShader);
                if (runtimePlaying) {
                    currentScene.update(dt);
                } else {
                    currentScene.editorUpdate(dt);
                }
                currentScene.render();
            }
            this.framebuffer.unbind();

            this.imguiLayer.update(dt, currentScene);
            runFactorySmoke();
            String startupCapture=System.getProperty("fraynix.captureOnStartup","");
            var workspace=imguiLayer.getFraynixPanel().workspace();
            if(!startupCaptured&&!startupCapture.isBlank()&&workspace.historyReady()&&!workspace.busy()
                &&!workspace.snapshot().observedAt().equals(java.time.Instant.EPOCH)&&++startupCaptureFrames>=60){
                try{editor.StudioFrameCapture.save(java.nio.file.Path.of(startupCapture),width,height);startupCaptured=true;System.out.println("STUDIO_STARTUP_CAPTURE_SAVED");}
                catch(java.io.IOException failure){startupCaptured=true;System.err.println("Startup frame capture failed: "+failure.getMessage());}
            }
            glfwSwapBuffers(glfwWindow);
            MouseListener.endFrame();

            endTime = glfwGetTime();
            dt = (float)(endTime - beginTime);
            beginTime = endTime;
        }
    }

    private void runFactorySmoke() {
        if (!factorySmoke) return;
        try {
            if (smokeRestored) {
                if (++smokeInspectorFrames < 3) return;
                if (!System.getProperty("fraymus.sceneAgentSmoke", "").isBlank()) {
                    if (System.nanoTime() > smokeDeadline) throw new IllegalStateException("Scene agent native smoke timed out");
                    if (sceneAgentSmoke == null) sceneAgentSmoke = new editor.SceneAgentNativeSmoke();
                    if (!sceneAgentSmoke.tick()) return;
                }
                if (Boolean.getBoolean("fraymus.studioSmoke")) {
                    if (System.nanoTime() > smokeDeadline) throw new IllegalStateException("Studio smoke timed out");
                    if (!imguiLayer.getFraynixPanel().smokeReady()) return;
                    imguiLayer.getFraynixPanel().reportSmoke();
                    String capture = System.getProperty("fraymus.studioCapture", "");
                    if (!capture.isBlank()) editor.StudioFrameCapture.save(java.nio.file.Path.of(capture), width, height);
                }
                factorySmoke = false;
                System.out.println("FACTORY_PHYSICS_INSPECTOR_RENDERED");
                if (factorySmokeExit) glfwSetWindowShouldClose(glfwWindow, true);
                return;
            }
            if (!smokeStarted) {
                factorySession.inspect(currentScene);
                smokeStarted = true;
                smokeDeadline = System.nanoTime() + 30_000_000_000L;
            } else if (!factorySession.isRunning()) {
                if (!factorySession.status().contains("validated")) throw new IllegalStateException(factorySession.status());
                java.util.Set<java.util.UUID> ids = new java.util.HashSet<>();
                for (jade.GameObject object : currentScene.getGameObjects()) if (object.doSerialization()) ids.add(object.getPersistentId());
                installFactoryScene(factorySession.restore());
                java.util.Set<java.util.UUID> restored = new java.util.HashSet<>();
                for (jade.GameObject object : currentScene.getGameObjects()) if (object.doSerialization()) restored.add(object.getPersistentId());
                if (!ids.equals(restored)) throw new IllegalStateException("Native scene restore changed persistent identities");
                long expectedBodies = currentScene.getGameObjects().stream().filter(o -> o.getComponent(physics2d.components.Rigidbody2D.class) != null
                    && (o.getComponent(physics2d.components.Box2DCollider.class) != null || o.getComponent(physics2d.components.CircleCollider.class) != null)).count();
                if (currentScene.physics().bodyCount() != expectedBodies) throw new IllegalStateException("Restored physics bodies were not registered");
                System.out.println("FACTORY_UI_SMOKE_PASSED restoredEntities=" + restored.size()
                    + " physicsBodies=" + currentScene.physics().bodyCount()
                    + " identitySet=" + restored.stream().map(java.util.UUID::toString).sorted().collect(java.util.stream.Collectors.joining(",")));
                currentScene.getGameObjects().stream().filter(jade.GameObject::doSerialization).findFirst()
                    .ifPresent(o -> getImguiLayer().getPropertiesWindow().setActiveGameObject(o));
                smokeRestored = true;
                smokeDeadline = System.nanoTime() + 180_000_000_000L;
            } else if (System.nanoTime() > smokeDeadline) {
                throw new IllegalStateException("Factory UI smoke timed out");
            }
        } catch (Exception error) { throw new IllegalStateException("Factory UI smoke failed", error); }
    }

    public static int getWidth() {
        return get().width;
    }

    public static int getHeight() {
        return get().height;
    }

    public static void setWidth(int newWidth) {
        get().width = newWidth;
    }

    public static void setHeight(int newHeight) {
        get().height = newHeight;
    }

    public static Framebuffer getFramebuffer() {
        return get().framebuffer;
    }

    public static float getTargetAspectRatio() {
        return 16.0f / 9.0f;
    }

    public static ImGuiLayer getImguiLayer() {
        return get().imguiLayer;
    }

    @Override
    public void onNotify(GameObject object, Event event) {
        switch (event.type) {
            case GameEngineStartPlay:
                this.runtimePlaying = true;
                currentScene.save();
                Window.changeScene(new LevelEditorSceneInitializer());
                break;
            case GameEngineStopPlay:
                this.runtimePlaying = false;
                Window.changeScene(new LevelEditorSceneInitializer());
                break;
            case LoadLevel:
                Window.changeScene(new LevelEditorSceneInitializer());
                break;
            case SaveLevel:
                try {
                    currentScene.save();
                    factorySession.save(currentScene);
                } catch (java.io.IOException | RuntimeException error) { factorySession.reportFailure(error); }
                break;
        }
    }
}
