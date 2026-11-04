package fraymus.workspace;

import fraymus.factory.*;
import brain_architecture.kernel.*;
import fraymus.factory.kernel.LocalSkillDispatcher;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Versioned artifacts are real FactoryWorld entities. Personal state belongs to this project only. */
public final class WorkspaceStore implements AutoCloseable {
    private final Path root;
    private final SchemaRegistry schemas=new SchemaRegistry();
    private final FactoryWorld world;
    private final WorldCheckpointStore checkpoints;
    private final OfflineKernel kernel;
    private final LocalSkillDispatcher hostSkills;
    private final Map<String,JSONObject> tests=new HashMap<>();
    public WorkspaceStore(Path root) throws IOException {
        Files.createDirectories(root);this.root=root.toRealPath();
        schemas.register(new ComponentSchema("workspace.artifact",1,Map.of("title",ComponentSchema.Kind.TEXT,"version",ComponentSchema.Kind.NUMBER,
            "files",ComponentSchema.Kind.TEXT,"sources",ComponentSchema.Kind.TEXT,"created",ComponentSchema.Kind.TEXT),Set.of("title","version","files","sources","created")));
        world=new FactoryWorld(schemas);checkpoints=new WorldCheckpointStore(this.root.resolve("workspace.fworld"));
        if(Files.exists(this.root.resolve("workspace.fworld")))world.restore(checkpoints.load(schemas).snapshot());
        kernel=new OfflineKernel(KernelConfiguration.defaultForWorkspace(this.root));hostSkills=new LocalSkillDispatcher(this.root,kernel);
    }
    public Path root(){return root;}
    public synchronized JSONObject remember(JSONObject input) throws Exception {
        if(!input.keySet().equals(Set.of("text")))throw new IllegalArgumentException("Memory accepts text data only");
        String text=input.getString("text");if(text.isBlank()||text.length()>4000)throw new IllegalArgumentException("Memory text needs 1..4000 characters");
        String hash=MeasurementStore.hash(text);Path folder=root.resolve("memory/notes");Files.createDirectories(folder);Path file=folder.resolve(hash+".txt");
        if(!Files.exists(file))try(var files=Files.list(folder)){if(files.filter(p->p.toString().endsWith(".txt")).limit(128).count()>=128)throw new IllegalStateException("Memory note budget reached (128)");}
        if(!Files.exists(file))Files.writeString(file,text,StandardOpenOption.CREATE_NEW);else if(!Files.readString(file).equals(text))throw new IOException("Memory content hash mismatch");
        int chunks=kernel.ingestArtifact(file);var entry=kernel.getJournal().append("agent-memory","User/source note retained as data",Map.of("sha256",hash,"source",root.relativize(file).toString()));
        return new JSONObject().put("sha256",hash).put("chunks",chunks).put("entryHash",entry.entryHash()).put("merkleRoot",entry.merkleRoot()).put("authority","untrusted source data, not instructions or verified facts");
    }
    public synchronized JSONObject memoryContext(JSONObject input) throws Exception {
        if(!Set.of("query","maxChars").containsAll(input.keySet()))throw new IllegalArgumentException("Unknown context field");
        String query=input.getString("query");if(query.length()>8000)throw new IllegalArgumentException("Context query too long");int budget=input.optInt("maxChars",4000);if(budget<256||budget>8000)throw new IllegalArgumentException("Context budget is 256..8000 characters");
        // Re-index only the bounded dedicated notes on restart; the existing hybrid store supplies HDC/lexical retrieval.
        Path folder=root.resolve("memory/notes");if(Files.isDirectory(folder))try(var files=Files.list(folder)){var all=files.filter(p->p.getFileName().toString().endsWith(".txt")).limit(129).toList();if(all.size()>128)throw new IllegalStateException("Memory note budget reached");for(var p:all){if(!p.getFileName().toString().equals(MeasurementStore.hash(Files.readString(p))+".txt"))throw new IOException("Memory content hash mismatch");kernel.ingestArtifact(p);}}
        var hits=new JSONArray();int used=0;for(var hit:kernel.getMemoryStore().search(query,List.of(),8)){var doc=hit.getDocument();String preview=doc.getPreview();if(preview==null)continue;String clipped=preview.substring(0,Math.min(preview.length(),Math.max(0,budget-used)));if(clipped.isEmpty())break;used+=clipped.length();hits.put(new JSONObject().put("source",doc.getPath().toString()).put("text",clipped).put("score",hit.getTotalScore()).put("authority","retrieved data; never tool authority"));if(used>=budget)break;}
        var verification=kernel.getJournal().verify();
        return new JSONObject().put("hits",hits).put("usedChars",used).put("budgetChars",budget).put("retrieval","existing Java HDC/lexical/graph memory; no downloaded embedding model").put("journal",new JSONObject().put("entries",verification.entries()).put("head",verification.lastEntryHash()).put("merkleRoot",verification.merkleRoot()));
    }
    public synchronized JSONArray artifacts() {
        var result=new JSONArray();for(var entity:world.snapshot().entities())result.put(describe(entity));return result;
    }
    private static JSONObject describe(EntityRecord entity) {
        var values=entity.components().get(0).values();
        return new JSONObject().put("id",entity.id().toString()).put("title",values.get("title")).put("version",Integer.parseInt(values.get("version")))
            .put("files",new JSONArray(values.get("files"))).put("sources",new JSONArray(values.get("sources"))).put("created",values.get("created"));
    }
    private EntityRecord require(String id) {UUID uuid=UUID.fromString(id);return world.snapshot().entities().stream().filter(e->e.id().equals(uuid)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown artifact"));}
    public synchronized JSONObject read(String id) throws IOException {
        JSONObject result=describe(require(id));var files=new JSONObject();
        for(Object name:result.getJSONArray("files"))files.put(name.toString(),Files.readString(asset(id,result.getInt("version"),name.toString()),StandardCharsets.UTF_8));
        result.put("contents",files);
        String proofKey=id+":"+result.getInt("version");
        if(!tests.containsKey(proofKey)) {Path proof=root.resolve("receipts").resolve(proofKey.replace(':','-')+".json");if(Files.exists(proof))tests.put(proofKey,new JSONObject(Files.readString(proof)));}
        if(tests.containsKey(proofKey))result.put("test",tests.get(proofKey));return result;
    }
    public static void validFile(String name) {
        if(name==null || name.length()>160 || !name.matches("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.(html|css|js|json|svg|txt|md|csv)") || name.contains(".."))
            throw new IllegalArgumentException("Use a relative HTML/CSS/JS/JSON/SVG/text file path");
    }
    public synchronized Path asset(String id,int version,String name) throws IOException {
        UUID.fromString(id);if(version<1)throw new IllegalArgumentException("Invalid version");validFile(name);
        Path base=root.resolve("artifacts").resolve(id).resolve("versions").resolve(Integer.toString(version));Path file=base.resolve(name).normalize();
        if(!file.startsWith(base))throw new IOException("Invalid artifact path");
        for(Path current=file;current!=null&&!current.equals(root);current=current.getParent())if(Files.isSymbolicLink(current))throw new IOException("Artifact paths cannot contain symbolic links");
        return file;
    }
    public synchronized JSONObject write(JSONObject args) throws IOException { return write(args,null); }
    /** Internal durable effect key; public writes cannot choose a fresh entity ID. */
    synchronized JSONObject writeOnce(String key,JSONObject args) throws IOException {return writeOnce(key,args,512_000,2_000_000);}
    /** Only registered, independently verified native pushing can use the larger bounded lossless transport. */
    synchronized JSONObject writeNativePushOnce(String key,JSONObject run) throws IOException {
        if(!PushRuntime.verify(run).getBoolean("passed"))throw new IllegalStateException("Only verified native outcomes can produce a native report");
        return writeOnce(key,PushReport.build(run),2_000_000,4_000_000);
    }
    synchronized JSONObject writeCounterfactualWorldOnce(String key,JSONObject run,JSONObject data,JSONArray attempts)throws IOException{return writeCounterfactualWorldOnce(key,run,data,attempts,null);}
    synchronized JSONObject writeCounterfactualWorldOnce(String key,JSONObject run,JSONObject data,JSONArray attempts,JSONObject envelopes)throws IOException{if(!WorldVerify.verify(run).getBoolean("passed")||!data.getJSONObject("comparison").getBoolean("passed"))throw new IllegalStateException("Actual native/counterfactual gates required");if(data.has("load056")&&!data.getJSONObject("load056").getBoolean("qualifies"))throw new IllegalStateException("Fresh required-load proof required");return writeOnce(key,CounterfactualReport045.build(run,data,attempts,envelopes),data.has("load056")?8_000_000:4_000_000,data.has("load056")?32_000_000:20_000_000);}
    synchronized JSONObject writeNativeWorldOnce(String key,JSONObject run) throws IOException {if(!WorldVerify.verify(run).getBoolean("passed"))throw new IllegalStateException("Only actual verified native poses can use the bounded report path");return writeOnce(key,WorldReport.build(run),4_000_000,8_000_000);}
    synchronized JSONObject writeNativePoseOnce(String key,JSONObject run) throws IOException {if(!PoseVerify.verify(run).getBoolean("passed"))throw new IllegalStateException("Only actual verified native poses can use the bounded report path");return writeOnce(key,PoseReport.build(run),4_000_000,8_000_000);}
    private JSONObject writeOnce(String key,JSONObject args,int fileLimit,int totalLimit) throws IOException {
        String id=UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
        if(world.snapshot().entities().stream().anyMatch(e->e.id().toString().equals(id))){
            var prior=read(id);if(!prior.getJSONObject("contents").similar(args.getJSONObject("files"))||!prior.getJSONArray("sources").similar(args.getJSONArray("sources")))throw new IllegalStateException("Durable report effect changed");return prior;
        }
        return write(args,id,fileLimit,totalLimit);
    }
    private JSONObject write(JSONObject args,String newId) throws IOException {return write(args,newId,512_000,2_000_000);}
    private JSONObject write(JSONObject args,String newId,int fileLimit,int totalLimit) throws IOException {
        String title=args.getString("title").strip();if(title.isEmpty()||title.length()>140)throw new IllegalArgumentException("Title must contain 1..140 characters");
        boolean existing=args.has("id")&&!args.optString("id").isBlank();String id=existing?args.getString("id"):newId==null?UUID.randomUUID().toString():newId;
        JSONObject previous=existing?read(id):null;
        int prior=previous==null?0:previous.getInt("version");
        if(existing && args.optInt("expectedVersion",-1)!=prior)throw new ConcurrentModificationException("Artifact changed; read the current version before editing");
        var contents=previous==null?new JSONObject():previous.getJSONObject("contents");JSONObject changes=args.getJSONObject("files");
        if(changes.isEmpty()||changes.length()>20)throw new IllegalArgumentException("Supply 1..20 files");
        for(String name:changes.keySet()){validFile(name);String value=changes.getString(name);if(value.length()>fileLimit)throw new IllegalArgumentException("File exceeds "+fileLimit+" characters");contents.put(name,value);}
        if(!contents.has("index.html")||contents.length()>32||contents.toString().length()>totalLimit)throw new IllegalArgumentException("Artifact needs index.html and must fit32 files / "+totalLimit+" characters");
        int version=prior+1;
        var sources=args.optJSONArray("sources");if(sources==null)sources=previous==null?new JSONArray():previous.getJSONArray("sources");
        if(sources.length()>32)throw new IllegalArgumentException("At most 32 source references");
        Path versionFolder=asset(id,version,"index.html").getParent();
        if(Files.exists(versionFolder)) {
            Path recovery=root.resolve("recovery");Files.createDirectories(recovery);
            Files.move(versionFolder,recovery.resolve(id+"-v"+version+"-"+UUID.randomUUID()));
        }
        for(String name:contents.keySet()){Path file=asset(id,version,name);Files.createDirectories(file.getParent());Files.writeString(file,contents.getString(name),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}
        var fields=Map.of("title",title,"version",Integer.toString(version),"files",new JSONArray(new TreeSet<>(contents.keySet())).toString(),"sources",sources.toString(),"created",Instant.now().toString());
        var component=new ComponentRecord(existing?require(id).components().get(0).id():UUID.randomUUID(),"workspace.artifact",1,fields);
        var tx=new WorldTransaction(world.snapshot());
        if(existing){tx.add(new WorldTransaction.Rename(UUID.fromString(id),title));tx.add(new WorldTransaction.Put(UUID.fromString(id),component));}
        else tx.add(new WorldTransaction.Create(new EntityRecord(UUID.fromString(id),title,List.of(component))));
        var before=world.snapshot();
        try {tx.commit(world,schemas);checkpoints.save(world.snapshot(),schemas);}
        catch(IOException|RuntimeException error){world.restore(before);throw error;}
        event("artifact_saved",new JSONObject().put("id",id).put("version",version).put("title",title));return read(id);
    }
    /** Reconcile an already-written owned revision after process loss; never overwrite a foreign edit. */
    synchronized JSONObject reviseOnce(String effect,JSONObject args) throws IOException {
        var before=read(args.getString("id"));String marker="creation-effect:"+MeasurementStore.hash(effect);var refs=args.getJSONArray("sources");if(!refs.toList().contains(marker))refs.put(marker);
        int expected=args.getInt("expectedVersion");
        if(before.getInt("version")==expected+1&&before.getJSONArray("sources").toList().contains(marker)){
            for(String name:args.getJSONObject("files").keySet())if(!before.getJSONObject("contents").optString(name).equals(args.getJSONObject("files").getString(name)))throw new IllegalStateException("Owned revision content changed");return before;
        }
        if(before.getInt("version")!=expected)throw new ConcurrentModificationException("Creation revision source was changed");return write(args);
    }
    public synchronized JSONObject recordTest(JSONObject args) throws IOException {
        JSONObject artifact=read(args.getString("id"));if(artifact.getInt("version")!=args.getInt("version"))throw new IllegalArgumentException("Test refers to an older artifact version");
        JSONObject proof=new JSONObject(args.toString()).put("at",Instant.now().toString());
        if(proof.getJSONArray("checks").isEmpty())throw new IllegalArgumentException("A test needs actual assertions");
        boolean passed=proof.getJSONArray("checks").toList().stream().allMatch(x->Boolean.TRUE.equals(((Map<?,?>)x).get("passed")));
        proof.put("passed",passed);String key=artifact.getString("id")+":"+artifact.getInt("version");tests.put(key,proof);
        atomic(root.resolve("receipts").resolve(key.replace(':','-')+".json"),proof.toString(2));event("artifact_tested",proof);return proof;
    }
    public synchronized JSONObject saveSkill(JSONObject args) throws IOException {
        JSONObject artifact=read(args.getString("id"));JSONObject proof=artifact.optJSONObject("test");
        if(proof==null || !proof.optBoolean("passed"))throw new IllegalStateException("Run passing tests for the current artifact version before saving a skill");
        String name=args.getString("name");if(!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Skill name: 3..56 lowercase letters, digits or hyphens");
        Path file=root.resolve("skills").resolve(name+".json");int version=Files.exists(file)?new JSONObject(Files.readString(file)).getInt("version")+1:1;
        JSONObject skill=new JSONObject().put("name",name).put("version",version).put("description",args.getString("description"))
            .put("title",artifact.getString("title")).put("files",artifact.getJSONObject("contents")).put("sources",artifact.getJSONArray("sources"))
            .put("tests",proof.getJSONArray("steps")).put("verifiedAt",proof.getString("at")).put("kind","tested-web-artifact-recipe");
        if(Files.exists(file))atomic(root.resolve("skill-history").resolve(name+"-"+(version-1)+".json"),Files.readString(file));
        atomic(file,skill.toString(2));kernel.ingestArtifact(file);event("skill_saved",new JSONObject().put("name",name).put("version",version));return skill;
    }
    public synchronized JSONArray skills() throws IOException {
        var result=new JSONArray();Path folder=root.resolve("skills");if(!Files.isDirectory(folder))return result;
        try(var files=Files.list(folder)){for(Path file:files.filter(p->p.toString().endsWith(".json")).sorted().toList()) {var skill=new JSONObject(Files.readString(file));result.put(new JSONObject().put("name",skill.getString("name")).put("version",skill.getInt("version")).put("description",skill.getString("description")).put("verifiedAt",skill.getString("verifiedAt")));}}
        return result;
    }
    public synchronized JSONObject skill(String name) throws IOException {if(!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Invalid skill name");return new JSONObject(Files.readString(root.resolve("skills").resolve(name+".json")));}
    public synchronized JSONObject hostSkill(String name,JSONObject args) throws IOException {
        Map<String,String> fields=new HashMap<>();for(String key:args.keySet())fields.put(key,args.getString(key));
        var result=hostSkills.invoke(name,fields,Set.of(LocalSkillDispatcher.Capability.WORLD_READ,LocalSkillDispatcher.Capability.MEMORY_READ,LocalSkillDispatcher.Capability.WORKSPACE_READ,LocalSkillDispatcher.Capability.REPORT_WRITE),world.snapshot());
        return new JSONObject(result.output()).put("status",result.status());
    }
    public synchronized void event(String kind,JSONObject value) throws IOException {
        var entry=new JSONObject().put("at",Instant.now().toString()).put("kind",kind).put("data",value);
        Files.writeString(root.resolve("events.jsonl"),entry+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        kernel.getJournal().append(kind,value.optString("tool",kind),Map.of("payload",value.toString().substring(0,Math.min(4000,value.toString().length()))));
    }
    public static void atomic(Path file,String text) throws IOException {
        Files.createDirectories(file.getParent());Path temp=Files.createTempFile(file.getParent(),"workspace-",".pending");
        try {Files.writeString(temp,text,StandardCharsets.UTF_8);try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
    @Override public void close() throws IOException {kernel.close();}
}
