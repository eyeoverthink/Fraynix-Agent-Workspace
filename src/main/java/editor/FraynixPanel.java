package editor;

import factory.FactorySession;
import fraymus.factory.models.FactoryModelService;
import imgui.ImGui;
import imgui.type.ImString;
import jade.Window;
import scenes.Scene;
import java.time.*;
import java.util.Locale;

public final class FraynixPanel implements AutoCloseable {
    private final FraynixWorkspace workspace;
    private final ImString prompt = new ImString(16384), filter = new ImString(256), search = new ImString(1024);
    private boolean attachScene = true;
    private final SceneAgentPanel sceneAgent;
    private String selectedDetails = "", uiError = "";
    private final boolean smoke = Boolean.getBoolean("fraymus.studioSmoke");
    private final java.util.Set<String> renderedTabs = new java.util.LinkedHashSet<>();
    private boolean smokeSent;
    private boolean focusCreate = Boolean.getBoolean("fraymus.openCreate");
    private String selectedSkill = "world.inspect";
    private final java.util.Map<String, ImString> skillFields = new java.util.HashMap<>();
    private boolean smokeToolSent;
    public FraynixPanel(FactorySession session) { workspace = new FraynixWorkspace(session.workspace()); sceneAgent = new SceneAgentPanel(session, workspace); }
    public SceneAgentPanel sceneAgent() { return sceneAgent; }
    public FraynixWorkspace workspace() { return workspace; }
    public void imgui(Scene scene) {
        workspace.tick();
        sceneAgent.tick(scene);
        var session = Window.getFactorySession();
        if (smoke && !smokeToolSent && session.monitor() != null && !session.isRunning() && !session.isMonitoring()) {
            session.requestSkill(scene, "world.inspect", java.util.Map.of()); smokeToolSent = true;
        }
        if (smoke && !smokeSent && workspace.historyReady() && !workspace.selectedModel().isBlank()) {
            workspace.select(System.getProperty("fraymus.smokeModel", "fraynix-3b:latest"));
            workspace.send("Reply with STUDIO_OK. This is a local editor integration test.", "");
            smokeSent = true;
        }
        ImGui.begin("Fraynix");
        ImGui.text("FRAYNIX  /  LOCAL WORKSPACE");
        ImGui.textWrapped("Kernel embedded | LLMs served by local Ollama processes");
        if (workspace.snapshot().observedAt().equals(Instant.EPOCH)) ImGui.text("Discovering local services...");
        else ImGui.text(workspace.snapshot().stores().stream().filter(FactoryModelService.Store::online).count() + " stores online | "
            + workspace.snapshot().models().stream().filter(FactoryModelService.Model::available).count() + " local models | checked "
            + Duration.between(workspace.snapshot().observedAt(), Instant.now()).getSeconds() + "s ago");
        if (!workspace.healthError().isEmpty()) ImGui.textWrapped(workspace.healthError());
        ImGui.separator();
        if (ImGui.beginTabBar("FraynixWorkspaceTabs")) {
            if (tab("Chat")) { chat(scene); ImGui.endTabItem(); }
            if (tab("Create")) { sceneAgent.imgui(scene); ImGui.endTabItem(); }
            if (tab("Models")) { models(); ImGui.endTabItem(); }
            if (tab("Kernel")) { kernel(); ImGui.endTabItem(); }
            if (tab("Tools")) { tools(scene); ImGui.endTabItem(); }
            if (tab("Jobs")) { jobs(scene); ImGui.endTabItem(); }
            ImGui.endTabBar();
            focusCreate = false;
        }
        ImGui.end();
    }
    private boolean tab(String name) {
        String next = java.util.stream.Stream.of("Chat", "Create", "Models", "Kernel", "Tools", "Jobs").filter(n -> !renderedTabs.contains(n)).findFirst().orElse("Tools");
        boolean select = smoke && next.equals(name) || (focusCreate || !System.getProperty("fraymus.sceneAgentSmoke", "").isBlank()) && name.equals("Create");
        boolean open = ImGui.beginTabItem(name, select ? imgui.flag.ImGuiTabItemFlags.SetSelected : 0);
        if (open) renderedTabs.add(name);
        return open;
    }
    public boolean smokeReady() {
        if (!smoke) return true;
        if (!workspace.error().isBlank()) throw new IllegalStateException(workspace.error());
        var session = Window.getFactorySession();
        if (!session.skillError().isBlank()) throw new IllegalStateException(session.skillError());
        if (!smokeToolSent || session.skillResult() == null || !smokeSent || workspace.busy() || workspace.exchanges().isEmpty() || renderedTabs.size() < 6 || session.monitor() == null) return false;
        var exchange = workspace.exchanges().get(workspace.exchanges().size() - 1);
        if (!exchange.success()) throw new IllegalStateException(exchange.reply());
        return true;
    }
    public void reportSmoke() {
        var exchange = workspace.exchanges().get(workspace.exchanges().size() - 1);
        System.out.println("STUDIO_UI_SMOKE_PASSED tabs=" + renderedTabs + " availableModels="
            + workspace.snapshot().models().stream().filter(FactoryModelService.Model::available).count()
            + " chatModel=" + exchange.model() + " endpoint=" + exchange.endpoint() + " response=" + exchange.reply().replace('\n', ' ')
            + " documents=" + Window.getFactorySession().monitor().status().get("documents")
            + " skill=" + Window.getFactorySession().skillResult().skill() + " skillJournalIndex=" + Window.getFactorySession().skillResult().journalIndex());
    }
    private void chat(Scene scene) {
        ImGui.setNextItemWidth(-1);
        if (ImGui.beginCombo("##ChatModel", workspace.selectedModel().isEmpty() ? "Select local model" : workspace.selectedModel())) {
            for (var model : workspace.snapshot().models()) if (model.available()) {
                if (ImGui.selectable(model.name(), workspace.selectedModel().equals(model.name()))) workspace.select(model.name());
            }
            ImGui.endCombo();
        }
        ImGui.beginChild("Conversation", 0, Math.max(100, ImGui.getContentRegionAvailY() - 220), true);
        boolean changed = workspace.consumeChatChanged();
        if (workspace.restoring()) ImGui.text("Restoring saved conversation...");
        long count = 0;
        for (var exchange : workspace.exchanges()) if (exchange.model().equals(workspace.selectedModel())) {
            count++;
            ImGui.textDisabled("YOU  /  " + exchange.at()); ImGui.textWrapped(exchange.prompt());
            ImGui.spacing(); ImGui.textDisabled((exchange.success() ? "FRAYNIX" : "REQUEST FAILED") + "  /  " + exchange.millis() + " ms");
            ImGui.textWrapped(exchange.reply());
            ImGui.textDisabled(exchange.endpoint()); ImGui.separator();
        }
        if (count == 0 && !workspace.busy()) ImGui.textWrapped("Chat with your selected local model. Messages are saved with this project and restored on restart.");
        if (workspace.busy()) {
            ImGui.textDisabled("YOU"); ImGui.textWrapped(workspace.pendingPrompt());
            ImGui.text("Waiting for local model: " + workspace.elapsedMillis() / 1000 + "s");
        }
        if (changed) ImGui.setScrollHereY(1);
        ImGui.endChild();
        if (ImGui.checkbox("Include current scene observations", attachScene)) attachScene = !attachScene;
        ImGui.inputTextMultiline("##Message", prompt, -1, 90);
        ImGui.beginDisabled(workspace.busy() || sceneAgent.busy() || !workspace.historyReady() || prompt.get().isBlank() || workspace.selectedModel().isEmpty());
        if (ImGui.button("Send to Fraynix")) {
            try { workspace.send(prompt.get(), attachScene ? sceneContext(scene) : ""); prompt.set(""); uiError = ""; }
            catch (RuntimeException error) { uiError = error.getMessage(); }
        }
        ImGui.endDisabled();
        if (!workspace.error().isBlank()) ImGui.textWrapped(workspace.error());
        if (!uiError.isBlank()) ImGui.textWrapped(uiError);
        ImGui.textDisabled("Scene editing: use Create for validated proposals");
    }
    private void models() {
        ImGui.beginDisabled(workspace.refreshing());
        if (ImGui.button("Refresh services")) workspace.refresh();
        ImGui.endDisabled();
        for (var store : workspace.snapshot().stores()) {
            ImGui.textWrapped(store.endpoint() + " | " + store.status());
            ImGui.text(store.advertised() + " local tags | " + store.loaded() + " resident models");
        }
        if (!workspace.snapshot().warning().isBlank()) ImGui.textWrapped(workspace.snapshot().warning());
        ImGui.inputTextWithHint("##ModelFilter", "Filter models", filter);
        ImGui.beginChild("InstalledModels", 0, Math.max(100, ImGui.getContentRegionAvailY() * .55f), true);
        for (var model : workspace.snapshot().models()) {
            if (!model.name().toLowerCase(Locale.ROOT).contains(filter.get().toLowerCase(Locale.ROOT))) continue;
            String state = !model.available() ? "offline" : model.loaded() ? "resident" : "available";
            if (ImGui.selectable(model.name() + "  [" + state + "]", selectedDetails.equals(model.name()))) {
                selectedDetails = model.name();
                if (model.available()) workspace.select(model.name());
            }
        }
        ImGui.endChild();
        workspace.snapshot().models().stream().filter(m -> m.name().equals(selectedDetails.isBlank() ? workspace.selectedModel() : selectedDetails)).findFirst().ifPresent(model -> {
            ImGui.textWrapped(model.name());
            ImGui.text(model.family() + " | " + model.parameters() + " | " + model.quantization());
            ImGui.text(String.format(Locale.ROOT, "Disk %.2f GiB | Resident VRAM %.2f GiB", model.bytes() / 1073741824.0, model.vramBytes() / 1073741824.0));
            ImGui.text("Context " + model.context() + (model.loaded() ? " (resident)" : " (catalog)"));
            ImGui.textWrapped(model.endpoint()); ImGui.textWrapped(model.source());
        });
        ImGui.textWrapped("Available means advertised by a live local server. Resident means loaded according to /api/ps. Quality and speed are measured only by actual requests.");
    }
    private void kernel() {
        var session = Window.getFactorySession(); var monitor = session.monitor();
        ImGui.textWrapped("Actual Fraynix OfflineKernel, using this project's persistent workspace.");
        if (monitor == null) ImGui.text("Loading kernel status...");
        else {
            ImGui.text("Observed " + monitor.at());
            for (String key : new String[]{"offlineEnforced", "documents", "trackedArtifacts", "journalEntries", "journalIntegrity", "pendingJournalFiles", "unfinishedSkillCalls", "repairLessons", "repairTemplates", "f0Available"}) {
                ImGui.text(key + ": " + monitor.status().getOrDefault(key, "unavailable"));
            }
            if (ImGui.collapsingHeader("Runtime details")) monitor.status().forEach((key, value) -> ImGui.textWrapped(key + ": " + value));
        }
        ImGui.inputTextWithHint("##MemoryQuery", "Search this project's kernel memory", search);
        ImGui.beginDisabled(session.isMonitoring() || session.isRunning());
        if (ImGui.button("Search / refresh kernel")) session.requestMonitor(search.get());
        ImGui.endDisabled();
        if (!session.monitorError().isBlank()) ImGui.textWrapped(session.monitorError());
        if (monitor != null) for (var hit : monitor.hits()) {
            ImGui.separator(); ImGui.textWrapped(String.format(Locale.ROOT, "%.3f  %s", hit.score(), hit.path())); ImGui.textWrapped(hit.preview());
        }
        ImGui.separator();
        ImGui.textWrapped("The legacy full-system shell and its other organs are not started by this editor. This window runs the shared factory engine, kernel, and local model adapter.");
    }
    private void tools(Scene scene) {
        var session = Window.getFactorySession(); var monitor = session.monitor();
        ImGui.textWrapped("Compiled local tools. Calls use this project's captured world and files; each result is recorded in the verified journal.");
        if (monitor == null) { ImGui.textWrapped(session.monitorError().isBlank() ? "Loading tool registry..." : session.monitorError()); return; }
        if (ImGui.beginCombo("##LocalSkill", selectedSkill)) {
            for (var skill : monitor.skills()) if (ImGui.selectable(skill.id(), selectedSkill.equals(skill.id()))) selectedSkill = skill.id();
            ImGui.endCombo();
        }
        var definition = monitor.skills().stream().filter(s -> s.id().equals(selectedSkill)).findFirst().orElseThrow();
        ImGui.textWrapped(definition.description());
        ImGui.textWrapped("Requires: " + definition.capabilities());
        ImGui.textWrapped("Operator grants: world read, memory read, workspace file read, new report creation.");
        var inputs = new java.util.TreeMap<String, String>();
        new java.util.TreeMap<>(definition.input().fields()).forEach((name, field) -> {
            var value = skillFields.computeIfAbsent(selectedSkill + "." + name, ignored -> new ImString(Math.min(field.maxLength() + 1, 8192)));
            ImGui.setNextItemWidth(-1); ImGui.inputText(name, value); inputs.put(name, value.get());
        });
        ImGui.textDisabled("File paths are relative to the workspace shown in Jobs.");
        ImGui.beginDisabled(session.isRunning() || session.isMonitoring());
        if (ImGui.button("Run local tool")) {
            try { session.requestSkill(scene, selectedSkill, inputs); uiError = ""; }
            catch (RuntimeException error) { uiError = error.getMessage(); }
        }
        ImGui.endDisabled();
        if (session.isRunning()) ImGui.text("Kernel operation running...");
        if (!session.skillError().isBlank()) ImGui.textWrapped(session.skillError());
        if (!uiError.isBlank()) ImGui.textWrapped(uiError);
        var result = session.skillResult();
        if (result != null) {
            ImGui.separator(); ImGui.textWrapped(result.skill() + " | " + result.status() + " | journal entry " + result.journalIndex());
            result.output().forEach((key, value) -> {
                if (value.length() > 3000) { if (ImGui.treeNode(key)) { ImGui.textWrapped(value.substring(0, 3000) + "... (full result saved in journal)"); ImGui.treePop(); } }
                else ImGui.textWrapped(key + ": " + value);
            });
        }
        if (ImGui.collapsingHeader("Persisted tool receipts")) for (int i = 0; i < monitor.skillReceipts().size(); i++) {
            if (ImGui.treeNode("Tool receipt " + (i + 1))) { ImGui.textWrapped(monitor.skillReceipts().get(i)); ImGui.treePop(); }
        }
        if (ImGui.collapsingHeader("Implementation evidence")) ImGui.textWrapped("v" + definition.version() + " | SHA-256 of dispatcher class: " + definition.implementationSha256());
    }
    private void jobs(Scene scene) {
        var session = Window.getFactorySession();
        session.drawControls(scene);
        var monitor = session.monitor();
        if (monitor != null && ImGui.collapsingHeader("Persisted inspection receipts")) {
            for (int i = 0; i < monitor.receipts().size(); i++) {
                if (ImGui.treeNode("Receipt " + (i + 1))) { ImGui.textWrapped(monitor.receipts().get(i)); ImGui.treePop(); }
            }
        }
        ImGui.separator(); ImGui.textWrapped("Workspace: " + session.workspace());
        ImGui.textWrapped("Chat journal: " + workspace.journalPath());
    }
    public static String sceneContext(Scene scene) {
        var selected = Window.getImguiLayer().getPropertiesWindow().getActiveGameObject();
        return Window.getFactorySession().observe(scene, selected == null ? null : selected.getPersistentId(), Window.isPlaying(),
            util.AssetPool.getSpritesheet("assets/images/spritesheets/decorationsAndBlocks.png").size()).json();
    }
    @Override public void close() { sceneAgent.close(); workspace.close(); }
}
