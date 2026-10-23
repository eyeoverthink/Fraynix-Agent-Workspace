package editor;

import fraymus.factory.models.*;
import fraymus.OllamaIntegration;
import fraymus.model.ModelAdapter;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** UI-owned lifecycle; HTTP, persistence and profile loading stay on workers. */
public final class FraynixWorkspace implements AutoCloseable {
    private final FactoryModelService service;
    private final ConversationStore journal;
    private final Path operationPath;
    private final ExecutorService health = Executors.newSingleThreadExecutor(task -> daemon(task,"fraynix-model-health"));
    private final ExecutorService inference = Executors.newSingleThreadExecutor(task -> daemon(task,"fraynix-inference"));
    private record Restored(List<ConversationStore.Exchange> exchanges,ModelProfileStore profiles) {}
    private record CompletedChat(ConversationStore.Exchange exchange,String saveError) {}
    public record Operation(String action,String model,String endpoint,String result,boolean success,Map<String,String> evidence) {}
    private CompletableFuture<Restored> restoring;
    private CompletableFuture<FactoryModelService.Snapshot> refreshing;
    private CompletableFuture<CompletedChat> sending;
    private CompletableFuture<Operation> operation;
    private ModelProfileStore profiles;
    private final AtomicReference<OllamaIntegration.StreamUpdate> live = new AtomicReference<>(new OllamaIntegration.StreamUpdate("","",0,"",false));
    private OllamaIntegration.RequestControl control;
    private FactoryModelService.Snapshot snapshot = new FactoryModelService.Snapshot(Instant.EPOCH,List.of(),List.of(),"");
    private List<ConversationStore.Exchange> exchanges=List.of();
    private String model="",error="",healthError="",pendingPrompt="",activeAction="",conversation="legacy";
    private long lastRefresh,sentAt;
    private boolean chatChanged,historyReady;
    private Operation lastOperation;

    public FraynixWorkspace(Path workspace) { this(workspace,new FactoryModelService()); }
    FraynixWorkspace(Path workspace,FactoryModelService service) {
        this.service=service;
        journal=new ConversationStore(workspace);operationPath=workspace.resolve("models/operations.jsonl");
        restoring=CompletableFuture.supplyAsync(()->{
            try{return new Restored(journal.load(),new ModelProfileStore(workspace));}
            catch(IOException failure){throw new CompletionException(failure);}
        },inference);
    }
    private static Thread daemon(Runnable task,String name){var thread=new Thread(task,name);thread.setDaemon(true);return thread;}
    public FactoryModelService.Snapshot snapshot(){return snapshot;}
    public List<ConversationStore.Exchange> exchanges(){return exchanges;}
    public List<ConversationStore.Exchange> visibleExchanges(){return exchanges.stream().filter(x->x.model().equals(model)
        &&x.evidence().getOrDefault("conversationId","legacy").equals(conversation)).toList();}
    public String selectedModel(){return model;}
    public void select(String selected){
        if(busy())throw new IllegalStateException("Finish or cancel the current model operation first");
        model=selected;
        conversation=exchanges.stream().filter(x->x.model().equals(model)).reduce((a,b)->b)
            .map(x->x.evidence().getOrDefault("conversationId","legacy")).orElse("legacy");
        chatChanged=true;
    }
    public void newConversation(){if(busy())return;conversation=UUID.randomUUID().toString();chatChanged=true;}
    public void resumeSavedConversation(){if(!busy())select(model);}
    public String error(){return error;}
    public String healthError(){return healthError;}
    public boolean busy(){return sending!=null||operation!=null;}
    public boolean restoring(){return restoring!=null;}
    public boolean historyReady(){return historyReady;}
    public String pendingPrompt(){return pendingPrompt;}
    public String activeAction(){return activeAction;}
    public OllamaIntegration.StreamUpdate live(){return live.get();}
    public Operation lastOperation(){return lastOperation;}
    public long elapsedMillis(){return busy()?(System.nanoTime()-sentAt)/1_000_000:0;}
    public Path journalPath(){return journal.path();}
    public Path profilesPath(){return profiles==null?null:profiles.path();}
    public Path operationPath(){return operationPath;}
    public boolean consumeChatChanged(){boolean changed=chatChanged;chatChanged=false;return changed;}
    public boolean refreshing(){return refreshing!=null;}
    public FactoryModelService.Model selected(){return snapshot.models().stream().filter(m->m.name().equals(model)&&m.available()).findFirst()
        .orElseThrow(()->new IllegalStateException("Select an online local model"));}
    public ModelRunSettings settings(){var selected=selected();return profiles==null?ModelRunSettings.defaults(selected.bytes()):profiles.get(selected.endpoint(),selected.name(),selected.bytes());}
    public void refresh(){if(refreshing!=null)return;lastRefresh=System.nanoTime();refreshing=CompletableFuture.supplyAsync(service::observe,health);}
    public void tick(){
        if(restoring!=null&&restoring.isDone()){
            try{var restored=restoring.join();exchanges=restored.exchanges();profiles=restored.profiles();historyReady=true;chatChanged=true;
                if(!exchanges.isEmpty()){var last=exchanges.get(exchanges.size()-1);model=last.model();
                    conversation=last.evidence().getOrDefault("conversationId",UUID.randomUUID().toString());}}
            catch(CompletionException failure){error=message(failure);}
            restoring=null;
        }
        if(refreshing!=null&&refreshing.isDone()){
            try{snapshot=refreshing.join();healthError="";
                if(model.isEmpty())model=snapshot.models().stream().filter(FactoryModelService.Model::available)
                    .sorted(Comparator.comparing(m->!m.name().equalsIgnoreCase("fraynix-3b:latest"))).findFirst().map(FactoryModelService.Model::name).orElse("");
            }catch(CompletionException failure){healthError=message(failure);}
            refreshing=null;
        }
        if(sending!=null&&sending.isDone()){
            try{var result=sending.join();var copy=new ArrayList<>(exchanges);copy.add(result.exchange());exchanges=List.copyOf(copy);error=result.saveError();chatChanged=true;}
            catch(CompletionException failure){error=message(failure);}
            sending=null;finished();
        }
        if(operation!=null&&operation.isDone()){
            try{lastOperation=operation.join();error=lastOperation.success()?"":lastOperation.result();}
            catch(CompletionException failure){error=message(failure);}
            operation=null;finished();
        }
        if(refreshing==null&&System.nanoTime()-lastRefresh>5_000_000_000L)refresh();
    }
    private static String message(Throwable failure){while(failure.getCause()!=null)failure=failure.getCause();return String.valueOf(failure.getMessage());}
    private void finished(){control=null;pendingPrompt="";activeAction="";lastRefresh=0;}
    private void begin(String action){
        if(busy()||restoring())throw new IllegalStateException("Wait for the current model operation");
        if(!historyReady)throw new IllegalStateException("Project chat/profile recovery is required before new writes");
        control=new OllamaIntegration.RequestControl();live.set(new OllamaIntegration.StreamUpdate("","",0,"",false));
        sentAt=System.nanoTime();activeAction=action;error="";
    }
    public void cancel(){if(control!=null)control.cancel();}
    public void send(String prompt,String sceneContext){
        if(prompt.isBlank())throw new IllegalArgumentException("Enter a message");
        var selected=selected();var settings=settings();String input=ChatContext.input(prompt,sceneContext);
        // Estimate only: tokenizer and model SYSTEM sizes vary. Keep whole recent exchanges, never cut JSON attachments.
        int historyBudget=Math.max(0,(settings.context()-settings.outputTokens()-1024)*3-input.length());
        var history=ChatContext.history(exchanges,model,selected.endpoint(),conversation,historyBudget);
        String thread=conversation;begin("Chat / "+selected.name());pendingPrompt=prompt;
        var request=control;long start=sentAt;
        sending=CompletableFuture.supplyAsync(()->{
            String text,thinking;boolean success;var evidence=new TreeMap<String,String>();
            evidence.put("conversationId",thread);evidence.put("context",String.valueOf(settings.context()));evidence.put("historyMessages",String.valueOf(history.size()));
            try{var result=service.streamChat(selected,input,history,settings,request,live::set);
                text=result.text();thinking=result.thinking();evidence.putAll(result.evidence());success=true;}
            catch(Exception failure){text="Request failed: "+message(failure);thinking=live.get().thinking();success=false;
                if(!live.get().text().isBlank())text+="\n\nPartial answer (not a completed response):\n"+live.get().text();
                evidence.put("done","false");evidence.put("cancelled",String.valueOf(request.cancelled()));}
            var exchange=new ConversationStore.Exchange(selected.name(),prompt,text,selected.endpoint(),success,(System.nanoTime()-start)/1_000_000,
                Instant.now(),sceneContext,thinking,evidence);
            String saveError="";try{journal.append(exchange);}catch(IOException failure){saveError="Response visible but not saved: "+failure.getMessage();}
            return new CompletedChat(exchange,saveError);
        },inference);
    }
    public void saveSettings(ModelRunSettings settings){
        var selected=selected();begin("Saving runtime settings");
        operation=CompletableFuture.supplyAsync(()->{
            try{profiles.save(selected.endpoint(),selected.name(),settings);return new Operation("Save settings",selected.name(),selected.endpoint(),"Saved for this project. Used by Chat and model loading; Create has its own request settings.",true,Map.of());}
            catch(IOException failure){return new Operation("Save settings",selected.name(),selected.endpoint(),failure.getMessage(),false,Map.of());}
        },inference);
    }
    public void modelOperation(String action){
        if(!Set.of("Test connection","Load model","Unload model").contains(action))throw new IllegalArgumentException("Unknown operation");
        var selected=selected();var settings=settings();begin(action+" / "+selected.name());var request=control;
        operation=CompletableFuture.supplyAsync(()->{
            Operation result;
            try{String nonce="LOCAL_"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
                var diagnostic=new ModelRunSettings(settings.context(),Math.min(256,settings.context()),settings.threads(),settings.gpuLayers(),settings.idleSeconds(),settings.keepAliveSeconds(),0,1.0,true);
                var response=action.equals("Test connection")?
                    service.streamChat(selected,"Connection verification: reply with only "+nonce+". Do not greet or explain.",List.of(),diagnostic,request,live::set):
                    service.manageModel(selected,action.equals("Load model"),settings,request);
                boolean verified=!action.equals("Test connection")||response.text().toUpperCase(Locale.ROOT).contains(nonce);
                var proof=new TreeMap<>(response.evidence());if(action.equals("Test connection")){proof.put("expectedNonce",nonce);proof.put("nonceComparison","case-insensitive ASCII");proof.put("diagnosticOptions","temperature=0, repeat_penalty=1, custom stops disabled, output=256");}
                result=new Operation(action,selected.name(),selected.endpoint(),action.equals("Test connection")?
                    (verified?"Verified fresh nonce from local HTTP response: ":"Model answered but did not echo the requested nonce: ")+response.text():
                    "Ollama completed the "+action.toLowerCase(Locale.ROOT)+" request. Residency refresh follows.",verified,Map.copyOf(proof));
            }catch(Exception failure){result=new Operation(action,selected.name(),selected.endpoint(),message(failure),false,Map.of("cancelled",String.valueOf(request.cancelled())));}
            try{Files.createDirectories(operationPath.getParent());
                Files.writeString(operationPath,factory.SceneJson.GSON.toJson(Map.of("at",Instant.now().toString(),"action",result.action(),"model",result.model(),
                    "endpoint",result.endpoint(),"success",result.success(),"result",result.result(),"evidence",result.evidence()))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            }catch(IOException failure){return new Operation(action,selected.name(),selected.endpoint(),result.result()+"; receipt save failed: "+failure.getMessage(),false,result.evidence());}
            return result;
        },inference);
    }
    @Override public void close(){cancel();health.shutdownNow();inference.shutdown();}
}
