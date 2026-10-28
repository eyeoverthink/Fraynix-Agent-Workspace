package editor;

import factory.FactorySession;
import fraymus.factory.models.FactoryModelService;
import fraymus.factory.models.ModelRunSettings;
import imgui.ImGui;
import imgui.type.*;
import jade.Window;
import scenes.Scene;
import java.time.*;
import java.util.Locale;

public final class FraynixPanel implements AutoCloseable {
    private final FraynixWorkspace workspace;
    private final ImString prompt = new ImString(16384), filter = new ImString(256), search = new ImString(1024);
    private boolean attachScene = true;
    private String lastChatSceneEdit = "";
    private final ImInt contextSetting=new ImInt(),outputSetting=new ImInt(),threadSetting=new ImInt(),gpuSetting=new ImInt(),timeoutSetting=new ImInt(),keepAliveSetting=new ImInt();
    private final float[] temperatureSetting={.4f};
    private final float[] repeatSetting={1.1f};
    private boolean clearStopsSetting;
    private String profileKey="";
    private final SceneAgentPanel sceneAgent;
    private String selectedDetails = "", uiError = "";
    private final boolean smoke = Boolean.getBoolean("fraymus.studioSmoke");
    private final java.util.Set<String> renderedTabs = new java.util.LinkedHashSet<>();
    private boolean smokeSent;
    private boolean focusCreate = Boolean.getBoolean("fraymus.openCreate");
    private boolean focusModels = Boolean.getBoolean("fraynix.openModels");
    private boolean startupProbe = Boolean.getBoolean("fraynix.probeOnStartup");
    private String selectedSkill = "world.inspect";
    private final java.util.Map<String, ImString> skillFields = new java.util.HashMap<>();
    private boolean smokeToolSent;
    private boolean smokeProbeSent;
    private String lastRenderedTab="";
    public FraynixPanel(FactorySession session) { workspace = new FraynixWorkspace(session.workspace()); sceneAgent = new SceneAgentPanel(session, workspace); }
    public SceneAgentPanel sceneAgent() { return sceneAgent; }
    public FraynixWorkspace workspace() { return workspace; }
    public void imgui(Scene scene) {
        workspace.tick();
        var completed = workspace.consumeCompletedExchange();
        if (completed != null && completed.success() && !completed.sceneContext().isBlank()) {
            String applied = ChatSceneBridge.applyIfPresent(scene, completed.reply(), completed.thinking());
            if (!applied.isBlank()) lastChatSceneEdit = applied;
        }
        sceneAgent.tick(scene);
        var session = Window.getFactorySession();
        if(startupProbe&&workspace.historyReady()&&!workspace.busy()&&workspace.snapshot().models().stream().anyMatch(m->m.available()&&m.name().equals(workspace.selectedModel()))){
            workspace.modelOperation("Test connection");startupProbe=false;
        }
        if (smoke && !smokeToolSent && session.monitor() != null && !session.isRunning() && !session.isMonitoring()) {
            session.requestSkill(scene, "world.inspect", java.util.Map.of()); smokeToolSent = true;
        }
        if (smoke && !smokeSent && workspace.historyReady() && workspace.snapshot().models().stream().anyMatch(m->m.available()
            &&m.name().equals(System.getProperty("fraymus.smokeModel","fraynix-3b:latest")))) {
            workspace.select(System.getProperty("fraymus.smokeModel", "fraynix-3b:latest"));
            workspace.newConversation();
            workspace.send("Reply with STUDIO_OK. This is a local editor integration test.", "");
            smokeSent = true;
        }
        if(smoke&&smokeSent&&!workspace.busy()&&!workspace.exchanges().isEmpty()&&!smokeProbeSent){
            workspace.modelOperation("Test connection");smokeProbeSent=true;
        }
        ImGui.begin("Fraynix");
        ImGui.text("FRAYNIX STUDIO  /  LIVE CHAT");
        ImGui.textWrapped("Streaming Ollama | runtime profiles | connection receipts");
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
        String next = java.util.stream.Stream.of("Chat", "Create", "Models", "Kernel", "Tools", "Jobs").filter(n -> !renderedTabs.contains(n)).findFirst().orElse(System.getProperty("fraynix.captureTab","Tools"));
        boolean select = smoke && next.equals(name) || focusModels&&name.equals("Models") || (focusCreate || !System.getProperty("fraymus.sceneAgentSmoke", "").isBlank()) && name.equals("Create");
        boolean open = ImGui.beginTabItem(name, select ? imgui.flag.ImGuiTabItemFlags.SetSelected : 0);
        if (open) { renderedTabs.add(name);lastRenderedTab=name;if(name.equals("Models"))focusModels=false; }
        return open;
    }
    public boolean smokeReady() {
        if (!smoke) return true;
        if (!workspace.error().isBlank()) throw new IllegalStateException(workspace.error());
        var session = Window.getFactorySession();
        if (!session.skillError().isBlank()) throw new IllegalStateException(session.skillError());
        if (!smokeToolSent || session.skillResult() == null || !smokeSent || !smokeProbeSent || workspace.busy() || workspace.exchanges().isEmpty() || renderedTabs.size() < 6 || session.monitor() == null) return false;
        if(workspace.lastOperation()==null)return false;
        if(!workspace.lastOperation().success())throw new IllegalStateException(workspace.lastOperation().result());
        if(!lastRenderedTab.equals(System.getProperty("fraynix.captureTab","Tools")))return false;
        var exchange = workspace.exchanges().get(workspace.exchanges().size() - 1);
        if (!exchange.success()) throw new IllegalStateException(exchange.reply());
        if(!"true".equals(exchange.evidence().get("done")))throw new IllegalStateException("Missing Ollama completion frame");
        return true;
    }
    public void reportSmoke() {
        var exchange = workspace.exchanges().get(workspace.exchanges().size() - 1);
        System.out.println("STUDIO_UI_SMOKE_PASSED tabs=" + renderedTabs + " availableModels="
            + workspace.snapshot().models().stream().filter(FactoryModelService.Model::available).count()
            + " chatModel=" + exchange.model() + " endpoint=" + exchange.endpoint() + " response=" + exchange.reply().replace('\n', ' ')
            + " documents=" + Window.getFactorySession().monitor().status().get("documents")
            + " skill=" + Window.getFactorySession().skillResult().skill() + " skillJournalIndex=" + Window.getFactorySession().skillResult().journalIndex());
        System.out.println("LIVE_CHAT_NATIVE_PASSED frames="+exchange.evidence().get("frames")+" responseSha256="+exchange.evidence().get("responseSha256")
            +" nonceVerified="+workspace.lastOperation().success()+" captureTab="+lastRenderedTab);
    }
    private void chat(Scene scene) {
        ImGui.beginDisabled(workspace.busy()||sceneAgent.busy());
        ImGui.setNextItemWidth(-1);
        if (ImGui.beginCombo("##ChatModel", workspace.selectedModel().isEmpty() ? "Select local model" : workspace.selectedModel())) {
            for (var model : workspace.snapshot().models()) if (model.available()) {
                if (ImGui.selectable(model.name(), workspace.selectedModel().equals(model.name()))) workspace.select(model.name());
            }
            ImGui.endCombo();
        }
        if(ImGui.button("New conversation"))workspace.newConversation();
        ImGui.sameLine();if(ImGui.button("Test connection"))runModelAction("Test connection");
        if(ImGui.button("Resume latest saved chat"))workspace.resumeSavedConversation();
        ImGui.endDisabled();
        workspace.snapshot().models().stream().filter(m->m.name().equals(workspace.selectedModel())).findFirst().ifPresent(m->{
            ImGui.textDisabled(m.endpoint());
            ImGui.textWrapped("Uses this model's own SYSTEM instructions. Settings and load controls are in Models.");
        });
        ImGui.beginChild("Conversation", 0, Math.max(100, ImGui.getContentRegionAvailY() - (workspace.lastOperation()==null?230:310)), true);
        boolean changed = workspace.consumeChatChanged();
        if (workspace.restoring()) ImGui.text("Restoring saved conversation...");
        long count = 0;
        for (var exchange : workspace.visibleExchanges()) {
            count++;
            ImGui.pushID((int)count);
            ImGui.textDisabled("YOU  /  " + exchange.at()); ImGui.textWrapped(exchange.prompt());
            if(!exchange.sceneContext().isBlank()&&ImGui.collapsingHeader("Scene attachment ("+exchange.sceneContext().length()+" characters)"))ImGui.textWrapped(exchange.sceneContext());
            ImGui.spacing(); ImGui.textDisabled((exchange.success() ? "MODEL RESPONSE" : "REQUEST FAILED") + "  /  " + exchange.millis() + " ms");
            ImGui.textWrapped(exchange.reply());
            if(!exchange.thinking().isBlank()&&ImGui.collapsingHeader("Model reasoning"))ImGui.textWrapped(exchange.thinking());
            if(exchange.success() && (!ChatSceneBridge.extractJsonCandidates(exchange.reply()).isEmpty() || !ChatSceneBridge.extractJsonCandidates(exchange.thinking()).isEmpty())){
                if(ImGui.button("Apply scene JSON to live scene")){
                    String applied = ChatSceneBridge.applyIfPresent(scene, exchange.reply(), exchange.thinking());
                    lastChatSceneEdit = applied.isBlank() ? "No valid scene modifications found in JSON block." : applied;
                }
            }
            if(ImGui.collapsingHeader("Request evidence")){
                ImGui.textWrapped(exchange.model()+" | "+exchange.endpoint());
                if(exchange.evidence().isEmpty())ImGui.textWrapped("Historical reply; detailed stream evidence was not recorded by the earlier build.");
                else exchange.evidence().forEach((key,value)->ImGui.textWrapped(key+": "+value));
                ImGui.textWrapped("HTTP evidence records transport results; it is not a signed attestation of model weights.");
            }
            ImGui.separator();ImGui.popID();
        }
        if (count == 0 && !workspace.busy()) ImGui.textWrapped("Chat with your selected local model. Messages are saved with this project and restored on restart.");
        if (workspace.busy()) {
            if(!workspace.pendingPrompt().isBlank()){ImGui.textDisabled("YOU"); ImGui.textWrapped(workspace.pendingPrompt());}
            ImGui.textWrapped(workspace.activeAction()+" | "+workspace.elapsedMillis()/1000+"s");
            var live=workspace.live();
            if(live.frames()==0)ImGui.textWrapped("Waiting for Ollama to load/process the request. No response frames yet.");
            else ImGui.text("Received "+live.frames()+" HTTP stream frames");
            if(!live.text().isBlank())ImGui.textWrapped(live.text());
            else if(!live.thinking().isBlank())ImGui.textWrapped("Model is generating reasoning ("+live.thinking().length()+" characters).");
        }
        if (changed) ImGui.setScrollHereY(1);
        ImGui.endChild();
        if (ImGui.checkbox("Attach current scene", attachScene)) attachScene = !attachScene;
        ImGui.textDisabled("Scene data appears in a collapsed attachment");
        if (!lastChatSceneEdit.isBlank()) ImGui.textWrapped(lastChatSceneEdit);
        ImGui.inputTextMultiline("##Message", prompt, -1, 90);
        ImGui.beginDisabled(workspace.busy() || sceneAgent.busy() || !workspace.historyReady() || prompt.get().isBlank() || workspace.selectedModel().isEmpty());
        if (ImGui.button("Send to Fraynix")) {
            try { workspace.send(prompt.get(), attachScene ? sceneContext(scene) : ""); prompt.set(""); uiError = ""; }
            catch (RuntimeException error) { uiError = error.getMessage(); }
        }
        ImGui.endDisabled();
        if(workspace.busy()){ImGui.sameLine();if(ImGui.button("Cancel request"))workspace.cancel();}
        showOperation();
        if (!workspace.error().isBlank()) ImGui.textWrapped(workspace.error());
        if (!uiError.isBlank()) ImGui.textWrapped(uiError);
        ImGui.textDisabled("Scene JSON in Chat responses is automatically deserialized (Gson) and applied to the live scene");
    }
    private void models() {
        ImGui.beginChild("ModelControls",0,0,false);
        ImGui.beginDisabled(workspace.refreshing());
        if (ImGui.button("Refresh services")) workspace.refresh();
        ImGui.endDisabled();
        for (var store : workspace.snapshot().stores()) {
            ImGui.textWrapped(store.endpoint() + " | " + store.status());
            ImGui.text(store.advertised() + " local tags | " + store.loaded() + " resident models");
        }
        if (!workspace.snapshot().warning().isBlank()) ImGui.textWrapped(workspace.snapshot().warning());
        ImGui.separator();
        ImGui.beginDisabled(workspace.busy()||sceneAgent.busy()||!workspace.historyReady());
        ImGui.setNextItemWidth(-1);
        if(ImGui.beginCombo("##ManageModel",workspace.selectedModel())){
            for(var model:workspace.snapshot().models())if(model.available()&&ImGui.selectable(model.name(),workspace.selectedModel().equals(model.name())))workspace.select(model.name());
            ImGui.endCombo();
        }
        if(ImGui.button("Test connection##Models"))runModelAction("Test connection");
        ImGui.sameLine();if(ImGui.button("Load model"))runModelAction("Load model");
        ImGui.sameLine();if(ImGui.button("Unload model"))runModelAction("Unload model");
        ImGui.endDisabled();
        if(workspace.busy()){
            ImGui.textWrapped(workspace.activeAction()+" | "+workspace.elapsedMillis()/1000+"s");
            if(ImGui.button("Cancel model request"))workspace.cancel();
            ImGui.textWrapped("Cancellation closes this client's request; server termination is not confirmed.");
        }
        showOperation();
        if(ImGui.collapsingHeader("Runtime settings",imgui.flag.ImGuiTreeNodeFlags.DefaultOpen))runtimeSettings();
        if(ImGui.collapsingHeader("Installed model details")){
        ImGui.inputTextWithHint("##ModelFilter", "Filter models", filter);
        ImGui.beginChild("InstalledModels", 0, Math.max(100, ImGui.getContentRegionAvailY() * .55f), true);
        for (var model : workspace.snapshot().models()) {
            if (!model.name().toLowerCase(Locale.ROOT).contains(filter.get().toLowerCase(Locale.ROOT))) continue;
            String state = !model.available() ? "offline" : model.loaded() ? "resident" : "available";
            if (ImGui.selectable(model.name() + "  [" + state + "]", selectedDetails.equals(model.name()))&&!workspace.busy()&&!sceneAgent.busy()) {
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
        }
        ImGui.textWrapped("Available means advertised by a live local server. Resident means loaded according to /api/ps. Quality and speed are measured only by actual requests.");
        ImGui.endChild();
    }
    private void runtimeSettings(){
        try{
            var model=workspace.selected();String key=model.endpoint()+"\n"+model.name();
            if(!profileKey.equals(key)){loadSettings(workspace.settings());profileKey=key;}
            ImGui.beginDisabled(workspace.busy()||sceneAgent.busy()||!workspace.historyReady());
            ImGui.setNextItemWidth(190);ImGui.inputInt("Context tokens",contextSetting);
            ImGui.setNextItemWidth(190);ImGui.inputInt("Output tokens",outputSetting);
            ImGui.setNextItemWidth(190);ImGui.inputInt("CPU threads (0 = auto)",threadSetting);
            ImGui.setNextItemWidth(190);ImGui.inputInt("GPU layers (-1 = auto)",gpuSetting);
            ImGui.setNextItemWidth(190);ImGui.inputInt("Idle timeout / seconds",timeoutSetting);
            ImGui.setNextItemWidth(190);ImGui.inputInt("Keep loaded / seconds",keepAliveSetting);
            ImGui.setNextItemWidth(190);ImGui.sliderFloat("Temperature",temperatureSetting,0,2,"%.2f");
            ImGui.setNextItemWidth(190);ImGui.sliderFloat("Repetition penalty",repeatSetting,.5f,2,"%.2f");
            if(ImGui.checkbox("Ignore custom stop strings",clearStopsSetting))clearStopsSetting=!clearStopsSetting;
            if(ImGui.button("Save runtime settings")){
                try{workspace.saveSettings(new ModelRunSettings(contextSetting.get(),outputSetting.get(),threadSetting.get(),gpuSetting.get(),timeoutSetting.get(),keepAliveSetting.get(),temperatureSetting[0],repeatSetting[0],clearStopsSetting));uiError="";}
                catch(Exception failure){uiError=failure.getMessage();}
            }
            ImGui.sameLine();if(ImGui.button("Defaults"))loadSettings(ModelRunSettings.defaults(model.bytes()));
            ImGui.endDisabled();
            ImGui.textWrapped("Saved per project and exact model/endpoint. Applies to Chat and Load; Create has its own request settings. Large contexts consume additional RAM/VRAM.");
            if(model.bytes()>20_000_000_000L)ImGui.textWrapped("Large model: a cold load may take several minutes. Server-side load timeout is configured in Ollama separately.");
        }catch(IllegalStateException unavailable){ImGui.textWrapped("Select an online model to configure it.");}
        if(!uiError.isBlank())ImGui.textWrapped(uiError);
    }
    private void loadSettings(ModelRunSettings settings){
        contextSetting.set(settings.context());outputSetting.set(settings.outputTokens());threadSetting.set(settings.threads());gpuSetting.set(settings.gpuLayers());
        timeoutSetting.set(settings.idleSeconds());keepAliveSetting.set(settings.keepAliveSeconds());temperatureSetting[0]=(float)settings.temperature();
        repeatSetting[0]=(float)settings.repeatPenalty();clearStopsSetting=settings.clearCustomStops();
    }
    private void runModelAction(String action){try{workspace.modelOperation(action);uiError="";}catch(Exception failure){uiError=failure.getMessage();}}
    private void showOperation(){
        var result=workspace.lastOperation();if(result==null)return;
        ImGui.textWrapped(result.action()+" | "+(result.success()?"completed":"failed"));ImGui.textWrapped(result.result());
        if(ImGui.collapsingHeader("Model operation evidence")){
            ImGui.textWrapped(result.model()+" | "+result.endpoint());result.evidence().forEach((key,value)->ImGui.textWrapped(key+": "+value));
        }
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
        if(workspace.busy()){
            ImGui.textWrapped(workspace.activeAction()+" | "+workspace.elapsedMillis()/1000+"s");
            if(ImGui.button("Cancel inference"))workspace.cancel();
        }
        showOperation();
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
        ImGui.textWrapped("Model operations: "+workspace.operationPath());
    }
    public static String sceneContext(Scene scene) {
        var selected = Window.getImguiLayer().getPropertiesWindow().getActiveGameObject();
        return Window.getFactorySession().observe(scene, selected == null ? null : selected.getPersistentId(), Window.isPlaying(),
            util.AssetPool.getSpritesheet("assets/images/spritesheets/decorationsAndBlocks.png").size()).json();
    }
    @Override public void close() { sceneAgent.close(); workspace.close(); }
}
