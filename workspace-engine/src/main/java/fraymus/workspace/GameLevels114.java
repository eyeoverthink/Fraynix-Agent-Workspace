package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Bounded source-based construction. A saved candidate is not a playtest certificate. */
public final class GameLevels114 {
    private final Path repo, root;
    private static final String SOURCE="levels/mario-original.json", RECIPE="mario-beginner114-v1";
    private static final Set<String> TYPES=Set.of("jade.Transform","components.SpriteRenderer","components.StateMachine","components.PlayerController","components.QuestionBlock","components.GoombaAI","components.TurtleAI","physics2d.components.CircleCollider","components.Ground","components.Flagpole","physics2d.components.Rigidbody2D","physics2d.components.Box2DCollider","physics2d.components.PillboxCollider");
    public GameLevels114(Path repo,Path project){this.repo=repo.toAbsolutePath().normalize();root=project.toAbsolutePath().normalize().resolve("game-levels114");}
    JSONArray source() throws Exception {return new JSONArray(read(repo.resolve(SOURCE),8_000_000));}
    static String read(Path path,long max) throws Exception {if(!Files.isRegularFile(path)||Files.size(path)>max)throw new IllegalArgumentException("Missing or oversized level file: "+path.getFileName());return Files.readString(path);}
    static String hash(Path path) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}
    static JSONObject prop(JSONObject object,String type){for(Object item:object.getJSONArray("components")){var c=(JSONObject)item;if(c.getString("type").equals(type))return c.getJSONObject("properties");}return null;}
    static JSONObject first(JSONArray source,String type){for(Object item:source){var o=(JSONObject)item;if(prop(o,type)!=null)return o;}throw new IllegalArgumentException("Required source component missing: "+type);}
    private static void assetPaths(Object value,Set<String> paths){if(value instanceof JSONObject o){for(String k:o.keySet()){if(k.equals("filepath"))paths.add(o.getString(k).replace('\\','/'));else assetPaths(o.get(k),paths);}}else if(value instanceof JSONArray a)for(Object v:a)assetPaths(v,paths);}
    private JSONObject assets(JSONArray objects) throws Exception {
        Set<String> paths=new TreeSet<>();assetPaths(objects,paths);var result=new JSONObject();
        for(String path:paths){Path p=repo.resolve(path).normalize();if(!path.startsWith("assets/")||!p.startsWith(repo.resolve("assets"))||!Files.isRegularFile(p)||Files.size(p)>32_000_000)throw new IllegalArgumentException("Unsupported or missing game asset: "+path);result.put(path,hash(p));}return result;
    }
    public synchronized JSONObject analyze() throws Exception {
        JSONArray a=source();var counts=new JSONObject();int components=0;
        for(Object item:a)for(Object component:((JSONObject)item).getJSONArray("components")){String t=((JSONObject)component).getString("type");counts.put(t,counts.optInt(t)+1);components++;}
        return new JSONObject().put("source",SOURCE).put("sourceSha256",hash(repo.resolve(SOURCE))).put("objects",a.length()).put("components",components).put("componentTypes",counts).put("assets",assets(a)).put("rootExampleObjects",new JSONArray(read(repo.resolve("level.txt"),8_000_000)).length()).put("scope","Full serialized source; behavior is implemented by Java components, not by JSON alone");
    }
    static int integer(JSONObject input,String name,int fallback,int min,int max){Object v=input.opt(name);if(v==null)return fallback;if(!(v instanceof Number n)||!Double.isFinite(n.doubleValue())||n.doubleValue()!=n.intValue()||n.intValue()<min||n.intValue()>max)throw new IllegalArgumentException(name+" must be an integer from "+min+" to "+max);return n.intValue();}
    static JSONObject parameters(JSONObject input){SpatialScene.keys(input,"seed","lengthTiles","gapTiles");return new JSONObject().put("seed",integer(input,"seed",0,0,9999)).put("lengthTiles",integer(input,"lengthTiles",56,48,72)).put("gapTiles",integer(input,"gapTiles",2,1,2));}
    private static String identity(String id,String suffix){return UUID.nameUUIDFromBytes((id+":"+suffix).getBytes(StandardCharsets.UTF_8)).toString();}
    static JSONObject clone(JSONArray output,JSONArray lineage,JSONObject template,String id,String name,double x,double y){
        int index=output.length();var o=new JSONObject(template.toString());o.put("uid",index).put("persistentId",identity(id,"object"+index)).put("name",name).put("isDead",false).put("doSerialization",true);
        int cindex=0;for(Object item:o.getJSONArray("components")){var c=(JSONObject)item;if(!TYPES.contains(c.getString("type")))throw new IllegalArgumentException("Unsupported generator component: "+c.getString("type"));c.getJSONObject("properties").put("uid",index*16+cindex).put("persistentId",identity(id,"component"+index+":"+cindex));cindex++;}
        var t=prop(o,"jade.Transform");t.getJSONObject("position").put("x",x).put("y",y);
        output.put(o);lineage.put(new JSONObject().put("id",o.getString("persistentId")).put("sourceUid",template.getInt("uid")).put("role",name));return o;
    }
    private JSONObject construct(JSONArray source,String id,JSONObject params) {
        JSONArray objects=new JSONArray(),lineage=new JSONArray();int length=params.getInt("lengthTiles"),width=params.getInt("gapTiles"),width2=params.optInt("secondGapTiles",width),seed=params.getInt("seed"),a=16+seed%3,b=32+(seed/3)%3;
        var ground=first(source,"components.Ground");if(ground.getJSONArray("components").length()!=5)throw new IllegalArgumentException("Expected plain source terrain");
        for(int x=0;x<length;x++)if(!(x>=a&&x<a+width)&&!(x>=b&&x<b+width2))for(int row=0;row<2;row++)clone(objects,lineage,ground,id,"Ground "+x+"/"+row,x*.25+.125,.125+row*.25);
        clone(objects,lineage,first(source,"components.PlayerController"),id,"Player",.875,.625);
        var question=clone(objects,lineage,first(source,"components.QuestionBlock"),id,"Power-up block",2.875+(seed%2)*.5,1.375);prop(question,"components.QuestionBlock").put("blockType","Powerup");
        var flag=first(source,"components.Flagpole");double finish=(length-5)*.25;
        for(int i=0;i<8;i++){var part=clone(objects,lineage,flag,id,"Finish "+i,finish,.625+i*.25);prop(part,"physics2d.components.Rigidbody2D").put("bodyType","Static").put("isSensor",true).put("gravityScale",0);}
        GameChallenge118.append(source,objects,lineage,id,params);
        return new JSONObject().put("objects",objects).put("lineage",lineage).put("finishX",finish).put("gaps",new JSONArray().put(new JSONObject().put("startX",a*.25).put("endX",(a+width)*.25)).put(new JSONObject().put("startX",b*.25).put("endX",(b+width2)*.25)));
    }
    public synchronized JSONObject create(JSONObject input) throws Exception {
        return createOwned(UUID.randomUUID().toString(),parameters(input),RECIPE);
    }
    static JSONObject parameters115(JSONObject input){if(GameChallenge118.active(input))return GameChallenge118.parameters(input);SpatialScene.keys(input,"seed","lengthTiles","gapTiles","secondGapTiles");return new JSONObject().put("seed",integer(input,"seed",0,0,9999)).put("lengthTiles",integer(input,"lengthTiles",56,48,72)).put("gapTiles",integer(input,"gapTiles",2,1,10)).put("secondGapTiles",integer(input,"secondGapTiles",input.optInt("gapTiles",2),1,10));}
    synchronized JSONObject create115(String id,JSONObject input)throws Exception{var p=parameters115(input);return createOwned(id,p,GameChallenge118.active(p)?GameChallenge118.RECIPE:"mario-repair115-v1");}
    private JSONObject createOwned(String id,JSONObject params,String recipe)throws Exception{
        directory(id);if(Files.exists(root.resolve(id))){var existing=verify(id);if(!existing.getString("recipe").equals(recipe)||!existing.getJSONObject("parameters").similar(params))throw new IllegalStateException("Candidate operation identity reused with different parameters");return existing;}Files.createDirectories(root);
        try(var dirs=Files.list(root)){if(dirs.filter(Files::isDirectory).count()>=32)throw new IllegalStateException("Candidate collection is full (32); export/manage candidates before creating more");}
        var source=source();String sourceHash=hash(repo.resolve(SOURCE));var built=construct(source,id,params);var manifest=new JSONObject().put("id",id).put("recipe",recipe).put("parameters",params).put("source",SOURCE).put("sourceSha256",sourceHash).put("assets",assets(built.getJSONArray("objects"))).put("objects",built.getJSONArray("objects").length()).put("lineage",built.getJSONArray("lineage")).put("gaps",built.getJSONArray("gaps")).put("finishX",built.getDouble("finishX")).put("status","constructed; playtest required");
        Path pending=root.resolve(".pending-"+id+"-"+UUID.randomUUID()),dest=root.resolve(id);Files.createDirectory(pending);
        Files.writeString(pending.resolve("level.json"),built.getJSONArray("objects").toString(2),StandardOpenOption.CREATE_NEW);manifest.put("levelSha256",hash(pending.resolve("level.json")));
        Files.writeString(pending.resolve("manifest.json"),manifest.toString(2),StandardOpenOption.CREATE_NEW);
        if(!sourceHash.equals(hash(repo.resolve(SOURCE))))throw new IllegalStateException("Source changed during construction");
        Files.move(pending,dest,StandardCopyOption.ATOMIC_MOVE);return verify(id);
    }
    private Path directory(String id){if(!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IllegalArgumentException("Choose a saved candidate ID");return root.resolve(id);}
    public synchronized JSONObject verify(String id) throws Exception {
        Path dir=directory(id);var m=new JSONObject(read(dir.resolve("manifest.json"),1_000_000));
        if(!id.equals(m.getString("id"))||!Set.of(RECIPE,"mario-repair115-v1",GameChallenge118.RECIPE).contains(m.getString("recipe"))||!SOURCE.equals(m.getString("source")))throw new IllegalArgumentException("Unsupported level manifest");
        if(m.getString("recipe").equals(GameChallenge118.RECIPE)!=GameChallenge118.active(m.getJSONObject("parameters")))throw new IllegalStateException("Challenge recipe and parameters differ");
        if(!m.getString("sourceSha256").equals(hash(repo.resolve(SOURCE))))throw new IllegalStateException("Original level changed; reconstruct a new candidate");
        if(!m.getString("levelSha256").equals(hash(dir.resolve("level.json"))))throw new IllegalStateException("Saved candidate bytes changed");
        var built=construct(source(),id,m.getString("recipe").equals(RECIPE)?parameters(m.getJSONObject("parameters")):parameters115(m.getJSONObject("parameters")));var candidate=new JSONArray(read(dir.resolve("level.json"),2_000_000));
        if(!candidate.similar(built.getJSONArray("objects"))||!m.getJSONArray("lineage").similar(built.getJSONArray("lineage"))||!m.getJSONArray("gaps").similar(built.getJSONArray("gaps"))||m.getInt("objects")!=candidate.length()||m.getDouble("finishX")!=built.getDouble("finishX"))throw new IllegalStateException("Candidate does not match its declared construction");
        if(!m.getJSONObject("assets").similar(assets(candidate)))throw new IllegalStateException("Source asset changed");
        return m.put("checked",true).put("levelPath",dir.resolve("level.json").toString());
    }
    public synchronized JSONObject list() throws Exception {
        var list=new JSONArray();if(Files.isDirectory(root))try(var files=Files.list(root)){for(Path p:files.sorted().toList()){String id=p.getFileName().toString();if(id.startsWith("."))continue;try{list.put(verify(id));}catch(Exception e){list.put(new JSONObject().put("id",id).put("checked",false).put("error",e.getMessage()));}}}
        return new JSONObject().put("candidates",list).put("limit",32).put("scope","Construction checks only; playability requires an actual trial");
    }
    synchronized JSONObject launch(EmbeddedGameClient game,String id) throws Exception {
        var m=verify(id);for(String asset:m.getJSONObject("assets").keySet())if(!m.getJSONObject("assets").getString(asset).equals(hash(repo.resolve("build/install/Fraynix-Studio").resolve(asset))))throw new IllegalStateException("Installed game asset differs: "+asset);
        return game.openLevel(Path.of(m.getString("levelPath")),id,m.getString("levelSha256"));
    }
}
