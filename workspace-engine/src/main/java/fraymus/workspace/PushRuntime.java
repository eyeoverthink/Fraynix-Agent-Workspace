package fraymus.workspace;
import org.json.*;
import java.nio.file.*;
/** Routes explicit versioned tasks without altering either historical native owner. */
final class PushRuntime implements AutoCloseable {
 static final String BASELINE="baseline-031",CONTACT=ContactPhysics.VERSION;
 private final PushStore baseline;private final ContactStore contact;private final Path file;private JSONObject routing;
 PushRuntime(Path project) throws Exception {baseline=new PushStore(project);contact=new ContactStore(project);file=project.resolve("pushing/router.json");routing=Files.exists(file)?new JSONObject(Files.readString(file)):new JSONObject().put("selected",BASELINE).put("scenes",new JSONObject());}
 static String identity(String kind){if(kind.equals(BASELINE))return PushCheckpoint.identity();if(kind.equals(CONTACT))return ContactCheckpoint.identity();throw new IllegalArgumentException("Unknown native controller");}
 private boolean candidate(String id){if(contact.hasRun(id))return true;if(baseline.hasRun(id))return false;throw new IllegalArgumentException("Unknown native pushing run");}
 synchronized boolean hasRun(String id){return baseline.hasRun(id)||contact.hasRun(id);}
 synchronized String status(String id){return candidate(id)?contact.status(id):baseline.status(id);}
 synchronized JSONObject run(String id){return candidate(id)?contact.run(id):baseline.run(id);}
 synchronized JSONArray list(){var out=baseline.list();for(Object raw:contact.list())out.put(raw);return out;}
 synchronized void available(){baseline.available();contact.available();}
 private void select(String kind,String scene) throws Exception {identity(kind);routing.put("selected",kind);routing.getJSONObject("scenes").put(scene,kind);PushLearningStore.atomic(file,routing);}
 synchronized JSONObject start(String id,String goal,JSONObject scene,JSONObject plan,boolean realtime) throws Exception {return start(id,goal,scene,plan,realtime,BASELINE);}
 synchronized JSONObject start(String id,String goal,JSONObject scene,JSONObject plan,boolean realtime,String kind) throws Exception {available();select(kind,scene.getString("id"));return kind.equals(CONTACT)?contact.start(id,goal,scene,plan,realtime):baseline.start(id,goal,scene,plan,realtime);}
 synchronized JSONObject resume(String id,JSONObject scene,boolean realtime) throws Exception {available();boolean c=candidate(id);select(c?CONTACT:BASELINE,scene.getString("id"));return c?contact.resume(id,scene,realtime):baseline.resume(id,scene,realtime);}
 synchronized JSONObject snapshot(){return routing.getString("selected").equals(CONTACT)?contact.snapshot():baseline.snapshot();}
 synchronized JSONObject stateForScene(String id){return routing.getJSONObject("scenes").optString(id,BASELINE).equals(CONTACT)?contact.stateForScene(id):baseline.stateForScene(id);}
 synchronized JSONObject pause(){return routing.getString("selected").equals(CONTACT)?contact.pause():baseline.pause();}
 synchronized JSONObject cancel(String id) throws Exception {return candidate(id)?contact.cancel(id):baseline.cancel(id);}
 static JSONObject verify(JSONObject run){if(run.has("controllerVersion"))ContactVerify.check(run);return PushVerify.verify(run);}
 @Override public void close(){contact.close();baseline.close();}
}
