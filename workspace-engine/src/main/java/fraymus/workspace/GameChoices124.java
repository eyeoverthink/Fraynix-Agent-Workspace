package fraymus.workspace;
import org.json.*;import java.nio.file.*;import java.time.*;import java.util.*;import java.security.MessageDigest;

/** Durable optional-action choices around the unchanged native task verifier. */
final class GameChoices124 implements AutoCloseable {
 private final GameTask115 tasks;private final Path root;private final String host,algorithm,boot=UUID.randomUUID().toString();
 private final Map<String,JSONObject> records=new LinkedHashMap<>();private final Map<String,Long> clocks=new HashMap<>();private final Map<String,String> damaged=new LinkedHashMap<>();
 GameChoices124(Path repo,Path project,EmbeddedGameClient game)throws Exception{this(repo,project,game,"java-chat");}
 GameChoices124(Path repo,Path project,EmbeddedGameClient game,String context)throws Exception{
  tasks=new GameTask115(repo,project,game);root=project.resolve("game-choices124");Files.createDirectories(root);
  host=WorldFrames.hash(new JSONObject().put("context",context).put("computer",System.getenv().getOrDefault("COMPUTERNAME","")).put("cpu",System.getenv().getOrDefault("PROCESSOR_IDENTIFIER","")).put("processors",Runtime.getRuntime().availableProcessors()).put("heap",Runtime.getRuntime().maxMemory()).put("java",System.getProperty("java.runtime.version")).put("javaHome",System.getProperty("java.home")).put("os",System.getProperty("os.name")+System.getProperty("os.version")));
  var code=new JSONObject();for(Class<?> c:List.of(GameChoices124.class,GameBudgetPolicy124.class))try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){code.put(c.getSimpleName(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())));}algorithm=WorldFrames.hash(code);
  try(var files=Files.list(root)){for(Path p:files.filter(x->x.toString().endsWith(".json")).sorted().toList())try{
   var r=AgentFiles042.loadChecked(p);String id=uuid(r.getString("id"));if(!p.getFileName().toString().equals(id+".json"))throw new IllegalStateException("Choice identity changed");records.put(id,r);
   if(r.getString("timingStatus").equals("pending")){r.put("restarted",true).put("eligible",false).put("timingStatus","interrupted timing retained; not a complete measurement");save(r);}
  }catch(Exception e){damaged.put(p.getFileName().toString(),e.getMessage());}}
 }
 private static String uuid(String s){if(!UUID.fromString(s).toString().equals(s))throw new IllegalArgumentException("Canonical task ID required");return s;}
 private Path file(String id){return root.resolve(uuid(id)+".json");}
 private void save(JSONObject r)throws Exception{AgentFiles042.saveChecked(file(r.getString("id")),r);}
 private JSONObject record(String id)throws Exception{
  uuid(id);if(damaged.containsKey(id+".json"))throw new IllegalStateException("Choice checkpoint integrity needs attention; original retained");var r=records.get(id);
  if(r!=null&&!AgentFiles042.loadChecked(file(id)).similar(r))throw new IllegalStateException("Choice checkpoint changed outside its owner; original retained");return r;
 }
 private static boolean fresh(JSONObject r,Instant now){long age=Duration.between(Instant.parse(r.getString("createdAt")),now).toSeconds();return age>=0&&age<=7*86400;}
 private JSONObject fact(JSONObject r)throws Exception{
  var m=tasks.measure123(r.getString("id"));if(!r.getString("resolved").equals(tasks.read(r.getString("id")).getString("request"))||!r.getJSONObject("parameters").similar(m.getJSONObject("parameters"))||!r.getString("trace").equals(m.getJSONObject("audit").getString("traceSha256")))throw new IllegalStateException("Measured task binding changed");
  var f=m.getJSONObject("final");var c=f.getJSONObject("controller");var fact=new JSONObject().put("id",r.getString("id")).put("strategy",m.getString("strategy")).put("seconds",r.getDouble("seconds")).put("pickups",c.getInt("pickups")).put("safe",m.getBoolean("completionCertified")&&!f.getJSONObject("player").getBoolean("dead")&&c.getInt("damage")==0).put("renderer",WorldFrames.hash(f.getJSONObject("renderer")));
  if(!fact.similar(r.getJSONObject("fact")))throw new IllegalStateException("Timing or outcome receipt changed");return fact;
 }
 private JSONObject choice(JSONObject request,long began)throws Exception{
  var samples=new JSONArray();var refs=new JSONArray();var rejected=new JSONArray();double budget=request.getDouble("allowance");
  if(budget>0&&damaged.isEmpty())for(String id:records.keySet()){
   var r=record(id);if(r.getBoolean("budgeted")||!r.getJSONObject("parameters").similar(request.getJSONObject("parameters")))continue;
   if(!r.optBoolean("eligible")||!host.equals(r.getString("host"))||!algorithm.equals(r.getString("algorithm"))||!fresh(r,Instant.now())){rejected.put(new JSONObject().put("id",id).put("reason","Incomplete, restarted, aged or different-context evidence"));continue;}
   try{var f=fact(r);samples.put(f);refs.put(new JSONObject().put("id",id).put("recordSha256",GameLevels114.hash(file(id))).put("fact",f));}catch(Exception e){rejected.put(new JSONObject().put("id",id).put("reason",String.valueOf(e.getMessage())));}
  }
  // An invalid matched source prevents cherry-picking the remaining favorable observations.
  boolean invalid=rejected.toList().stream().anyMatch(x->!((Map<?,?>)x).get("reason").equals("Incomplete, restarted, aged or different-context evidence"));if(invalid||!damaged.isEmpty()){samples=new JSONArray();refs=new JSONArray();}
  long analysis=System.nanoTime();GameBudgetPolicy124.choose(samples,budget,0);long analysisNanos=System.nanoTime()-analysis;double overhead=(System.nanoTime()-began+analysisNanos)/1e9;
  var result=GameBudgetPolicy124.choose(samples,budget,overhead);return new JSONObject().put("result",result).put("samples",samples).put("sources",refs).put("rejected",rejected).put("damagedRecords",damaged.size()).put("overheadSeconds",overhead).put("analysisNanos",analysisNanos);
 }
 private void verifyChoice(JSONObject r,boolean resuming)throws Exception{
  if(!r.getBoolean("budgeted"))return;if(!algorithm.equals(r.getString("algorithm")))throw new IllegalStateException("Choice implementation changed; historical evidence is retained");
  var q=GameBudgetPolicy124.request(r.getString("original"));if(!q.getJSONObject("parameters").similar(r.getJSONObject("parameters"))||!tasks.read(r.getString("id")).getJSONObject("parameters").similar(r.getJSONObject("parameters")))throw new IllegalStateException("Choice geometry binding changed");var d=r.getJSONObject("decision");var samples=new JSONArray();
  if(resuming&&!host.equals(r.getString("host")))throw new IllegalStateException("Resource context changed; start a fresh request");
  for(Object raw:d.getJSONArray("sources")){var ref=(JSONObject)raw;var source=record(ref.getString("id"));if(source==null||!source.getBoolean("eligible")||source.getBoolean("budgeted")||!source.getString("host").equals(r.getString("host"))||!source.getString("algorithm").equals(algorithm)||!source.getJSONObject("parameters").similar(r.getJSONObject("parameters"))||!fresh(source,resuming?Instant.now():Instant.parse(r.getString("createdAt")))||!GameLevels114.hash(file(source.getString("id"))).equals(ref.getString("recordSha256")))throw new IllegalStateException("Original choice source changed or expired");var actual=fact(source);if(!actual.similar(ref.getJSONObject("fact")))throw new IllegalStateException("Choice source outcome changed");samples.put(actual);}
  if(!samples.similar(d.getJSONArray("samples")))throw new IllegalStateException("Choice sample set changed");var expected=GameBudgetPolicy124.choose(samples,q.getDouble("allowance"),d.getDouble("overheadSeconds"));if(!expected.similar(d.getJSONObject("result"))||!GameBudgetPolicy124.resolve(r.getString("original"),expected.getString("selected")).equals(r.getString("resolved")))throw new IllegalStateException("Budget decision departed from its request or evidence");
 }
 synchronized JSONObject start(JSONObject input)throws Exception{
  long begin=System.nanoTime();SpatialScene.keys(input,"id","request");String id=uuid(input.getString("id")),text=input.getString("request");if(text.length()>4000||!GameTask115.requested(text))throw new IllegalArgumentException("A bounded level request is required");var prior=record(id);
  if(prior!=null){if(!prior.getString("original").equals(text))throw new IllegalArgumentException("Goal identity reused with another request");verifyChoice(prior,false);tasks.start(new JSONObject().put("id",id).put("request",prior.getString("resolved")));return read(id);}
  for(Object old:tasks.list().getJSONArray("tasks"))if(((JSONObject)old).getString("id").equals(id)){
   if(GameBudgetPolicy124.request(text).getBoolean("budgeted"))throw new IllegalStateException("Missing original choice checkpoint; retained task cannot be relabelled");
   return tasks.start(input); // Legacy receipts have no complete clock; never invent one.
  }
  if(tasks.busy())throw new IllegalStateException("Another level task owns the runtime");var q=GameBudgetPolicy124.request(text);boolean budgeted=q.getBoolean("budgeted");JSONObject decision=budgeted?choice(q,begin):null;String resolved=budgeted?GameBudgetPolicy124.resolve(text,decision.getJSONObject("result").getString("selected")):text;
  var task=tasks.start(new JSONObject().put("id",id).put("request",resolved));var r=new JSONObject().put("id",id).put("original",text).put("resolved",resolved).put("budgeted",budgeted).put("parameters",task.getJSONObject("parameters")).put("host",host).put("algorithm",algorithm).put("createdAt",Instant.now().toString()).put("boot",boot).put("restarted",false).put("eligible",false).put("timingStatus","pending");if(decision!=null)r.put("decision",decision);records.put(id,r);clocks.put(id,begin);save(r);return read(id);
 }
 private void checkRenderer(JSONObject r,JSONObject t)throws Exception{
  if(r==null||!r.getBoolean("budgeted")||r.getJSONObject("decision").getJSONObject("result").getString("selected").equals("direct"))return;
  for(Object raw:t.getJSONArray("trials")){var initial=((JSONObject)raw).getJSONObject("initial");String renderer=WorldFrames.hash(initial.getJSONObject("renderer"));for(Object f:r.getJSONObject("decision").getJSONArray("samples"))if(Set.of("direct",r.getJSONObject("decision").getJSONObject("result").getString("selected")).contains(((JSONObject)f).getString("strategy"))&&!((JSONObject)f).getString("renderer").equals(renderer)){r.put("blocked","Renderer changed; start a fresh direct request or collect fresh evidence").put("eligible",false);save(r);if(t.getString("status").equals("running"))tasks.control(t.getString("id"),"interrupt");throw new IllegalStateException(r.getString("blocked"));}}
 }
 synchronized JSONObject advance(String id)throws Exception{var r=record(id);var before=tasks.read(id);checkRenderer(r,before);if(r!=null&&r.has("blocked"))throw new IllegalStateException(r.getString("blocked"));var t=tasks.advance(id);checkRenderer(r,t);if(t.getString("status").equals("failed"))finish(r,tasks.measure123(id));return read(id);}
 private void finish(JSONObject r,JSONObject m)throws Exception{
  if(r==null||!r.getString("timingStatus").equals("pending"))return;var start=clocks.remove(r.getString("id"));if(start==null){r.put("timingStatus","No uninterrupted clock available").put("eligible",false);save(r);return;}
  double seconds=(System.nanoTime()-start)/1e9;var f=m.getJSONObject("final");var c=f.getJSONObject("controller");var fact=new JSONObject().put("id",r.getString("id")).put("strategy",m.getString("strategy")).put("seconds",seconds).put("pickups",c.getInt("pickups")).put("safe",m.getBoolean("completionCertified")&&!f.getJSONObject("player").getBoolean("dead")&&c.getInt("damage")==0).put("renderer",WorldFrames.hash(f.getJSONObject("renderer")));
  r.put("seconds",seconds).put("fact",fact).put("trace",m.getJSONObject("audit").getString("traceSha256")).put("timingStatus","Measured through first verified terminal outcome; final receipt write and later shutdown excluded").put("eligible",!r.getBoolean("budgeted")&&!r.getBoolean("restarted")&&seconds>0&&seconds<=600);save(r);
 }
 synchronized JSONObject verify(String id)throws Exception{var r=record(id);var t=tasks.verify(id);if(r!=null){if(!r.getString("resolved").equals(t.getString("request")))throw new IllegalStateException("Resolved request changed");verifyChoice(r,false);checkRenderer(r,t);finish(r,tasks.measure123(id));}return read(id);}
 synchronized JSONObject measure(String id)throws Exception{var r=record(id);var m=tasks.measure123(id);if(r!=null){verifyChoice(r,false);finish(r,m);m.put("choice124",WorldFrames.copy(r));}return m;}
 synchronized JSONObject control(String id,String action)throws Exception{var r=record(id);if(action.equals("resume")&&r!=null){if(r.has("blocked"))throw new IllegalStateException(r.getString("blocked"));verifyChoice(r,true);r.put("restarted",true).put("eligible",false).put("timingStatus","Restarted request: retained, excluded from calibration");clocks.remove(id);save(r);}tasks.control(id,action);return read(id);}
 synchronized JSONObject read(String id)throws Exception{var t=tasks.read(id);var r=record(id);if(r!=null){t.put("pickupChoice124",WorldFrames.copy(r));if(r.has("blocked"))t.put("reason",r.getString("blocked"));if(t.has("audit")&&r.getBoolean("budgeted"))t.getJSONObject("audit").put("pickupChoice124",WorldFrames.copy(r.getJSONObject("decision").getJSONObject("result")));}return t;}
 synchronized JSONObject list()throws Exception{var result=tasks.list();var list=new JSONArray();for(Object raw:result.getJSONArray("tasks"))list.put(read(((JSONObject)raw).getString("id")));result.put("tasks",list).put("choiceRecordsNeedingAttention",damaged.size());return result;}
 synchronized boolean busy(){return tasks.busy();}
 @Override public synchronized void close(){tasks.close();clocks.clear();for(var r:records.values())if(r.getString("timingStatus").equals("pending"))try{r.put("eligible",false).put("restarted",true).put("timingStatus","Interrupted clock: excluded from calibration");save(r);}catch(Exception e){System.err.println("Choice timing could not close: "+e.getMessage());}}
}
