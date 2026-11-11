package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

/** Durable native-game task. The existing Java chat scheduler advances one bounded stage at a time. */
final class GameTask115 implements AutoCloseable {
    private final Path root,repo;
    private final EmbeddedGameClient game;
    private final GameLevels114 levels;
    private final GameReference115 references;
    private final Map<String,JSONObject> tasks=new LinkedHashMap<>();
    private static final Set<String> LIVE=Set.of("running");
    GameTask115(Path repo,Path project,EmbeddedGameClient game)throws Exception{
        this.repo=repo.toAbsolutePath().normalize();root=project.resolve("game-tasks115");this.game=game;references=new GameReference115(repo,project);levels=new GameLevels114(repo,project);Files.createDirectories(root);
        try(var files=Files.list(root)){for(Path p:files.filter(x->x.toString().endsWith(".json")).sorted().toList()){
            var t=AgentFiles042.loadChecked(p);String id=t.getString("id");if(!p.getFileName().toString().equals(id+".json"))throw new IllegalStateException("Level task identity changed");
            tasks.put(id,t);if(LIVE.contains(t.getString("status"))){interruptTrial(t,"Process stopped; a fresh trial is required");t.put("status","interrupted").put("reason","Resume explicitly; completed trials are retained, incomplete simulation is restarted");save(t);}
        }}
    }
    static boolean requested(String text){return !ChatPolicy044.webCreation(text)&&!ChatPolicy044.has(text,"\\b(explain|describe|outline)\\b|\\b(do not|don.t|never)\\s+(create|build|construct|generate|reconstruct|make)\\b")&&ChatPolicy044.has(text,"\\b(make|build|create|construct|reconstruct|generate)\\b")&&ChatPolicy044.has(text,"\\b(mario|game lab|playable level|platformer)\\b");}
    static JSONObject request(String text){
        var p=new JSONObject();for(String k:List.of("seed","lengthTiles","gapTiles","secondGapTiles","repairLimit","goombaTile","turtleTile","obstacleTile","obstacleHeight","pickupTile")){
            var m=Pattern.compile("(?i)\\b"+k+"\\s*(?:=|:)\\s*([^\\s,;]+)").matcher(text);if(m.find()){try{p.put(k,Integer.parseInt(m.group(1).replaceFirst("(?<=\\d)\\.$","")));}catch(Exception e){throw new IllegalArgumentException(k+" needs a whole number");}if(m.find())throw new IllegalArgumentException("Duplicate "+k);}
        }
        if(ChatPolicy044.has(text,"\\b(no repairs|do not repair|without repairs)\\b"))p.put("repairLimit",0);
        boolean challenge=GameChallenge118.active(p);
        if(!challenge&&ChatPolicy044.has(text,"\\b(enemies|enemy|goomba|turtle|obstacle|obstacles)\\b"))throw new IllegalArgumentException("Choose declared challenge positions: goombaTile, turtleTile, or obstacleTile with obstacleHeight. The Game Lab challenge selector prepares examples.");
        int limit=GameLevels114.integer(p,"repairLimit",challenge?0:2,0,2);p.remove("repairLimit");
        if(challenge&&limit!=0)throw new IllegalArgumentException("Challenge levels require repairLimit=0; failed enemy or obstacle trials are retained without changing terrain");return new JSONObject().put("parameters",GameLevels114.parameters115(p)).put("repairLimit",limit);
    }
    private JSONObject identity()throws Exception{
        var result=new JSONObject();for(Class<?> type:List.of(GameTask115.class,GameLevels114.class,GameChallenge118.class,EmbeddedGameClient.class,GameReference115.class))try(var in=type.getResourceAsStream(type.getSimpleName()+".class")){if(in==null)throw new IllegalStateException("Compiled implementation missing");result.put(type.getSimpleName(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())));}
        for(String asset:List.of("assets/shaders/default.glsl","assets/shaders/pickingShader.glsl")){
            String hash=GameLevels114.hash(repo.resolve(asset));if(!hash.equals(GameLevels114.hash(repo.resolve("build/install/Fraynix-Studio").resolve(asset))))throw new IllegalStateException("Installed shader differs from source");result.put(asset,hash);
        }
        Path lib=repo.resolve("build/install/Fraynix-Studio/lib");try(var files=Files.list(lib)){for(Path p:files.filter(x->x.toString().endsWith(".jar")).sorted().toList())result.put(p.getFileName().toString(),GameLevels114.hash(p));}return result;
    }
    private void sources(JSONObject t)throws Exception{if(t.has("reference"))references.checkBytes(t.getJSONObject("reference"));if(!t.getJSONObject("engine").similar(identity()))throw new IllegalStateException("Game engine identity changed; old trials are not new evidence");if(!t.getJSONObject("source").similar(levels.analyze()))throw new IllegalStateException("Original scene or assets changed; old task retained");}
    private static String uuid(String value){if(!UUID.fromString(value).toString().equals(value))throw new IllegalArgumentException("Canonical task ID required");return value;}
    private JSONObject task(String id){var t=tasks.get(uuid(id));if(t==null)throw new IllegalArgumentException("Unknown level task");return t;}
    private void checkedDisk(JSONObject t)throws Exception{if(!AgentFiles042.loadChecked(root.resolve(t.getString("id")+".json")).similar(t))throw new IllegalStateException("Task checkpoint changed outside its owner; altered bytes retained");}
    private void save(JSONObject t)throws Exception{AgentFiles042.saveChecked(root.resolve(t.getString("id")+".json"),t);}
    synchronized boolean busy(){return tasks.values().stream().anyMatch(t->LIVE.contains(t.getString("status")));}
    synchronized JSONObject start(JSONObject input)throws Exception{
        SpatialScene.keys(input,"id","request");String id=uuid(input.getString("id")),text=input.getString("request");if(text.length()>4000||!requested(text))throw new IllegalArgumentException("A bounded Game Lab level request is required");
        if(tasks.containsKey(id)){var t=task(id);if(!t.getString("request").equals(text))throw new IllegalArgumentException("Task identity reused with another request");sources(t);return read(id);}
        if(busy())throw new IllegalStateException("Another level task owns the runtime");if(tasks.size()>=24)throw new IllegalStateException("Level task history is full (24); originals retained");
        var definition=request(text);JSONObject reference=null;
        var ref=Pattern.compile("(?i)\\breference\\s*=\\s*([0-9a-f-]{36})(?![0-9a-f-])").matcher(text);
        if(ref.find()){String rid=ref.group(1);if(ref.find())throw new IllegalArgumentException("Choose one image reference");if(ChatPolicy044.has(text,"(?i)\\b(seed|lengthTiles|gapTiles|secondGapTiles|goombaTile|turtleTile|obstacleTile|obstacleHeight|pickupTile)\\s*="))throw new IllegalArgumentException("Reference geometry and manual geometry cannot be mixed");reference=references.verify(rid);definition.put("parameters",reference.getJSONObject("analysis").getJSONObject("parameters"));}
        else if(ChatPolicy044.has(text,"\\b(image|picture|screenshot|reference)\\b"))throw new IllegalArgumentException("Import a supported PNG in Game Lab and include its reference ID first");
        var t=new JSONObject().put("id",id).put("request",text).put("definition",definition).put("parameters",WorldFrames.copy(definition.getJSONObject("parameters"))).put("engine",identity()).put("source",levels.analyze()).put("status","running").put("stage","construct").put("attempt",0).put("restarts",0).put("trials",new JSONArray()).put("repairs",new JSONArray()).put("reason","Constructing a separate candidate").put("workNanos",0L).put("startedAt",java.time.Instant.now().toString());if(reference!=null)t.put("reference",reference);tasks.put(id,t);save(t);return read(id);
    }
    private static boolean challenge(JSONObject t){return GameChallenge118.active(t.getJSONObject("parameters"));}
    private static JSONObject current(JSONObject t){return t.getJSONArray("trials").getJSONObject(t.getJSONArray("trials").length()-1);}
    private static String candidateId(JSONObject t){return UUID.nameUUIDFromBytes((t.getString("id")+":"+t.getInt("attempt")).getBytes(StandardCharsets.UTF_8)).toString();}
    private void capture(JSONObject t,JSONObject trial,JSONObject frame,String when)throws Exception{
        String name=t.getString("id")+"-"+(t.getJSONArray("trials").length()-1)+"-"+when+"-"+UUID.randomUUID()+".png";Path target=root.resolve(name);byte[] bytes=Base64.getDecoder().decode(frame.getString("pngBase64"));Files.write(target,bytes,StandardOpenOption.CREATE_NEW);trial.put(when+"Image",new JSONObject().put("file",name).put("sha256",GameLevels114.hash(target)));
    }
    private void openTrial(JSONObject t,String control)throws Exception{
        var candidate=levels.verify(candidateId(t));var frame=levels.launch(game,candidate.getString("id"));var state=frame.getJSONObject("state");if(state.getJSONObject("player").getBoolean("won")||state.getInt("tick")!=0)throw new IllegalStateException("Fresh candidate state required");
        var trial=new JSONObject().put("candidate",candidate).put("control",control).put("status","running").put("initial",state).put("steps",new JSONArray()).put("sessionId",state.getString("sessionId"));t.getJSONArray("trials").put(trial);capture(t,trial,frame,"start");t.put("stage","trial").put("reason","Playtesting with "+control+" control");
    }
    synchronized JSONObject advance(String id)throws Exception{
        var t=task(id);checkedDisk(t);if(!t.getString("status").equals("running"))return read(id);long started=System.nanoTime();
        try{
            sources(t);switch(t.getString("stage")){
                case "construct"->{levels.create115(candidateId(t),t.getJSONObject("parameters"));t.put("stage","open-simple");}
                case "open-simple"->openTrial(t,"simple");
                case "open-ai"->openTrial(t,"existing-ai");
                case "trial"->step(t);
                case "finish"->{var proof=verifyTrials(t);var review=levels.launch(game,candidateId(t));t.put("review",new JSONObject().put("state",review.getJSONObject("state")).put("candidateId",candidateId(t)).put("scope","Verified candidate reopened at its starting state; completed trial outcomes remain separate")).put("audit",proof).put("status","completed").put("reason","Level verified by actual finish contact; controller comparison and failed attempts retained").put("completedAt",java.time.Instant.now().toString());game.command(new JSONObject().put("action","pause"));}
                default->throw new IllegalStateException("Unknown level stage");
            }
        }catch(Exception e){interruptTrial(t,e.getMessage());t.put("status","interrupted").put("reason",String.valueOf(e.getMessage()));try{game.command(new JSONObject().put("action","pause"));}catch(Exception ignored){}}
        finally{t.put("workNanos",t.getLong("workNanos")+System.nanoTime()-started);save(t);}return read(id);
    }
    private void step(JSONObject t)throws Exception{
        var trial=current(t);var m=levels.verify(trial.getJSONObject("candidate").getString("id"));if(!m.similar(trial.getJSONObject("candidate")))throw new IllegalStateException("Candidate binding changed");
        var before=game.frame();if(!before.getString("levelId").equals(m.getString("id"))||!before.getJSONObject("state").getString("sessionId").equals(trial.getString("sessionId")))throw new IllegalStateException("Owned runtime session changed");
        var p=before.getJSONObject("state").getJSONObject("player");boolean jump=false;double x=p.getDouble("x");for(Object raw:m.getJSONArray("gaps")){var gap=(JSONObject)raw;if(x>gap.getDouble("startX")-.7&&x<gap.getDouble("endX")+.1)jump=true;}
        var action=trial.getString("control").equals("simple")?new JSONObject().put("action","step").put("ticks",12).put("horizontal",1).put("jump",jump):new JSONObject().put("action","ai-step").put("ticks",12).put("targetX",m.getDouble("finishX")+1);
        var frame=game.command(action);var after=frame.getJSONObject("state");trial.getJSONArray("steps").put(new JSONObject().put("before",before.getJSONObject("state")).put("action",action).put("after",after));
        var end=after.getJSONObject("player");boolean won=end.getBoolean("won"),failed=end.getBoolean("dead")||end.getDouble("y")<-.5||trial.getJSONArray("steps").length()>=180;
        if(!won&&!failed){t.put("reason","Playtesting "+trial.getString("control")+": tick "+after.getInt("tick")+", X "+String.format(Locale.ROOT,"%.2f",end.getDouble("x")));return;}
        trial.put("status",won?"won":"failed").put("final",after);capture(t,trial,frame,"finish");game.command(new JSONObject().put("action","pause"));
        if(trial.getString("control").equals("existing-ai")){
            if(challenge(t)&&!won){t.put("status","failed").put("reason","Challenge AI did not reach the finish; failed trial retained and no terrain repair permitted");return;}
            t.put("stage","finish");return;
        }
        if(won||challenge(t)){t.put("stage","open-ai");return;}
        int gapIndex=-1;for(int i=0;i<m.getJSONArray("gaps").length();i++){var gap=m.getJSONArray("gaps").getJSONObject(i);if(end.getDouble("y")<.35&&end.getDouble("x")>=gap.getDouble("startX")-.3&&end.getDouble("x")<=gap.getDouble("endX")+.3)gapIndex=i;}
        String field=gapIndex==0?"gapTiles":"secondGapTiles";var params=t.getJSONObject("parameters");
        if(gapIndex<0||params.getInt(field)<=2||t.getJSONArray("repairs").length()>=t.getJSONObject("definition").getInt("repairLimit")){
            t.put("status","failed").put("reason","Actual playtest failed; no justified repair remains within the request budget");return;
        }
        t.getJSONArray("repairs").put(new JSONObject().put("trial",t.getJSONArray("trials").length()-1).put("field",field).put("before",params.getInt(field)).put("after",2).put("reason","Observed fall within this gap").put("observed",end));params.put(field,2);t.put("attempt",t.getInt("attempt")+1).put("stage","construct").put("reason","Repairing the observed gap; retaining failed candidate and trace");
    }
    private JSONObject verifyTrials(JSONObject t)throws Exception{
        sources(t);if(t.has("reference"))references.verify(t.getJSONObject("reference").getString("id"));int wins=0,failed=0,steps=0,aiPickups=0,aiDamage=0,aiPickupSkips=0;boolean simpleFinal=false,aiFinal=false,simpleCompared=false;String aiOutcome="not-run",simpleOutcome="not-run";JSONArray pickupDecisions=new JSONArray();var sessions=new HashSet<String>();
        for(Object raw:t.getJSONArray("trials")){
            var trial=(JSONObject)raw;var m=levels.verify(trial.getJSONObject("candidate").getString("id"));if(!m.similar(trial.getJSONObject("candidate")))throw new IllegalStateException("Original candidate binding changed");
            String session=trial.getString("sessionId");if(!sessions.add(session))throw new IllegalStateException("Repeated trial session");var previous=trial.getJSONObject("initial");if(previous.getInt("tick")!=0||previous.getJSONObject("player").getBoolean("won"))throw new IllegalStateException("Trial did not start at the original state");
            for(Object stepRaw:trial.getJSONArray("steps")){var step=(JSONObject)stepRaw;JSONObject before=step.getJSONObject("before"),after=step.getJSONObject("after");if(!session.equals(before.getString("sessionId"))||!session.equals(after.getString("sessionId"))||previous.getInt("tick")!=before.getInt("tick")||!previous.getJSONObject("player").similar(before.getJSONObject("player"))||after.getInt("tick")-before.getInt("tick")!=12||step.getJSONObject("action").getInt("ticks")!=12)throw new IllegalStateException("Trial continuity or input budget changed");var action=step.getJSONObject("action");boolean simple=trial.getString("control").equals("simple");if(!simple&&!trial.getString("control").equals("existing-ai"))throw new IllegalStateException("Unknown controller");var expected=new JSONObject().put("ticks",12);if(simple){double x=before.getJSONObject("player").getDouble("x");boolean jump=false;for(Object rawGap:m.getJSONArray("gaps")){var gap=(JSONObject)rawGap;if(x>gap.getDouble("startX")-.7&&x<gap.getDouble("endX")+.1)jump=true;}expected.put("action","step").put("horizontal",1).put("jump",jump);}else expected.put("action","ai-step").put("targetX",m.getDouble("finishX")+1);if(!action.similar(expected)||!after.getJSONObject("player").getString("id").equals(trial.getJSONObject("initial").getJSONObject("player").getString("id")))throw new IllegalStateException("Controller action or player identity changed");previous=after;steps++;}
            for(String when:List.of("start","finish"))if(trial.has(when+"Image")){var image=trial.getJSONObject(when+"Image");String file=image.getString("file");if(!file.matches(Pattern.quote(t.getString("id"))+"-[0-9]+-(start|finish)-[0-9a-f-]{36}\\.png")||!image.getString("sha256").equals(GameLevels114.hash(root.resolve(file))))throw new IllegalStateException("Original trial image changed");}
            if(trial.getJSONArray("steps").length()>180)throw new IllegalStateException("Trial step budget exceeded");if(trial.has("final")&&!trial.getJSONObject("final").similar(previous))throw new IllegalStateException("Final state differs from original last input observation");
            if(Set.of("won","failed").contains(trial.getString("status"))&&(!trial.has("final")||!trial.has("startImage")||!trial.has("finishImage")||trial.getJSONArray("steps").isEmpty()))throw new IllegalStateException("Completed comparison lacks original final observation or images");
            if(trial.getString("status").equals("failed")&&!previous.getJSONObject("player").getBoolean("dead")&&previous.getJSONObject("player").getDouble("y")>=-.5&&trial.getJSONArray("steps").length()<180)throw new IllegalStateException("Failed comparison lacks actual death, fall or exhausted input budget");
            boolean won=trial.getString("status").equals("won");if(won){if(!previous.getJSONObject("player").getBoolean("won")||previous.getJSONObject("player").getBoolean("dead"))throw new IllegalStateException("Missing actual finish contact");wins++;}else if(trial.getString("status").equals("failed"))failed++;
            if(m.getString("id").equals(candidateId(t))){if(Set.of("won","failed").contains(trial.getString("status"))&&trial.getString("control").equals("simple")){simpleCompared=true;simpleOutcome=trial.getString("status");}if(won&&trial.getString("control").equals("simple"))simpleFinal=true;if(Set.of("won","failed").contains(trial.getString("status"))&&trial.getString("control").equals("existing-ai")){aiFinal=true;aiOutcome=trial.getString("status");var controller=previous.getJSONObject("controller");aiPickups=controller.getInt("pickups");aiDamage=controller.getInt("damage");aiPickupSkips=controller.getInt("pickupSkips");pickupDecisions=new JSONArray(controller.getJSONArray("pickupDecisions").toString());if(aiPickups<0||aiDamage<0||aiPickupSkips<0||pickupDecisions.length()>16||pickupDecisions.length()>aiPickupSkips)throw new IllegalStateException("Invalid collision outcome counters");}}
        }
        verifyRepairs(t);
        if(challenge(t)){
            if(!simpleCompared||!aiFinal||!aiOutcome.equals("won")||t.getJSONArray("repairs").length()!=0)throw new IllegalStateException("Challenge requires a retained simple comparison and actual AI finish without repairs");
        }else if(!simpleFinal||!aiFinal)throw new IllegalStateException("Final candidate requires simple-control finish and a bounded AI comparison");
        if(t.getJSONArray("repairs").length()>t.getJSONObject("definition").getInt("repairLimit"))throw new IllegalStateException("Repair budget exceeded");
        return new JSONObject().put("passed",true).put("candidateId",candidateId(t)).put("wins",wins).put("failures",failed).put("trials",t.getJSONArray("trials").length()).put("inputChunks",steps).put("repairCount",t.getJSONArray("repairs").length()).put("simpleControl",simpleOutcome).put("completionControl",challenge(t)?"risk118":"simple").put("existingAI",aiOutcome).put("aiPickups",aiPickups).put("aiDamage",aiDamage).put("aiPickupSkips",aiPickupSkips).put("pickupDecisions",pickupDecisions).put("traceSha256",WorldFrames.hash(new JSONObject().put("trials",t.getJSONArray("trials")))).put("scope","Actual finish contact on this candidate; comparison is not a guarantee for unfamiliar levels. No procedure-bank admission or model training.");
    }
    private void verifyRepairs(JSONObject t)throws Exception{
        var parameters=WorldFrames.copy(t.getJSONObject("definition").getJSONObject("parameters"));int attempt=0;
        for(int index=0;index<t.getJSONArray("trials").length();index++){
            var trial=t.getJSONArray("trials").getJSONObject(index);String expectedId=UUID.nameUUIDFromBytes((t.getString("id")+":"+attempt).getBytes(StandardCharsets.UTF_8)).toString();var m=trial.getJSONObject("candidate");if(!expectedId.equals(m.getString("id"))||!parameters.similar(m.getJSONObject("parameters")))throw new IllegalStateException("Candidate departed from original request/repair lineage");
            for(Object raw:t.getJSONArray("repairs")){var repair=(JSONObject)raw;if(repair.getInt("trial")!=index)continue;String field=repair.getString("field");int gapIndex=field.equals("gapTiles")?0:field.equals("secondGapTiles")?1:-1;if(gapIndex<0||!trial.getString("status").equals("failed")||!trial.getString("control").equals("simple")||parameters.getInt(field)!=repair.getInt("before")||repair.getInt("before")<=2||repair.getInt("after")!=2)throw new IllegalStateException("Unsupported repair");JSONObject observed=trial.getJSONObject("final").getJSONObject("player"),gap=m.getJSONArray("gaps").getJSONObject(gapIndex);if(!observed.similar(repair.getJSONObject("observed"))||observed.getDouble("y")>=.35||observed.getDouble("x")<gap.getDouble("startX")-.3||observed.getDouble("x")>gap.getDouble("endX")+.3)throw new IllegalStateException("Repair lacks its original fall evidence");parameters.put(field,2);attempt++;}
        }
        if(attempt!=t.getJSONArray("repairs").length()||attempt!=t.getInt("attempt")||!parameters.similar(t.getJSONObject("parameters")))throw new IllegalStateException("Final repair lineage changed");
    }
    synchronized JSONObject verify(String id)throws Exception{var t=task(id);var disk=AgentFiles042.loadChecked(root.resolve(id+".json"));if(!disk.similar(t))throw new IllegalStateException("Task checkpoint changed outside its owner");if(!t.getString("status").equals("completed"))throw new IllegalStateException("Task is not completed");var proof=verifyTrials(t);if(!proof.similar(t.getJSONObject("audit")))throw new IllegalStateException("Completion proof changed");return read(id);}
    private static void interruptTrial(JSONObject t,String reason){if(t.getString("stage").equals("trial")){var trial=current(t);if(trial.getString("status").equals("running"))trial.put("status","interrupted").put("reason",reason);t.put("stage",trial.getString("control").equals("simple")?"open-simple":"open-ai");}}
    synchronized JSONObject control(String id,String action)throws Exception{var t=task(id);checkedDisk(t);if(action.equals("resume")){if(!t.getString("status").equals("interrupted")||busy())throw new IllegalStateException("Only an interrupted idle task can resume");if(t.getInt("restarts")>=3)throw new IllegalStateException("Three-restart limit reached");sources(t);t.put("restarts",t.getInt("restarts")+1).put("status","running").put("reason","Resuming from checked source in a fresh trial");}
        else if(action.equals("cancel")||action.equals("interrupt")){if(!t.getString("status").equals("running"))return read(id);interruptTrial(t,"Explicit "+action);t.put("status",action.equals("cancel")?"cancelled":"interrupted").put("reason","Explicit "+action+"; original trials retained");game.close();}else throw new IllegalArgumentException("Unknown level control");save(t);return read(id);}
    synchronized JSONObject read(String id){var t=task(id);var out=WorldFrames.copy(t);out.remove("engine");out.remove("source");var list=new JSONArray();for(Object raw:t.getJSONArray("trials")){var trial=(JSONObject)raw;var v=WorldFrames.copy(trial);v.remove("steps");v.put("inputChunks",trial.getJSONArray("steps").length());v.remove("candidate");list.put(v);}out.put("trials",list);return out;}
    synchronized JSONObject list(){var out=new JSONArray();for(String id:tasks.keySet())out.put(read(id));return new JSONObject().put("tasks",out).put("limit",24);}
    @Override public synchronized void close(){for(var t:tasks.values())if(t.getString("status").equals("running"))try{control(t.getString("id"),"interrupt");}catch(Exception e){System.err.println("Level interruption: "+e.getMessage());}}
}
