package fraymus.factory.models;

import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Bounded metadata discovery across mounted drives; never copies or reads model weights. */
public final class ModelStorageDiscovery {
    public record Store(String path,int manifests,int complete,int missingBlobs) {}
    private ModelStorageDiscovery() {}
    public static List<Store> discover() {
        Set<Path> roots=new LinkedHashSet<>();roots.add(Path.of(System.getProperty("user.home"),".ollama","models"));
        String configured=System.getenv("OLLAMA_MODELS");if(configured!=null&&!configured.isBlank()) roots.add(Path.of(configured));
        for(var drive:java.io.File.listRoots()) for(String child:List.of(".ollama/models","ollama/models","ollama-models","models/ollama","models","Fraynix/models/ollama","AI/models")) roots.add(drive.toPath().resolve(child));
        return inspect(roots);
    }
    public static List<Store> inspect(Collection<Path> candidates) {
        List<Store> result=new ArrayList<>();
        for(Path root:candidates) {
            if(Thread.currentThread().isInterrupted()) break;
            if(!Files.isDirectory(root.resolve("blobs")) || !Files.isDirectory(root.resolve("manifests"))) continue;
            int count=0,complete=0,missing=0;
            try(var paths=Files.walk(root.resolve("manifests"),6)) {
                for(Path path:paths.filter(Files::isRegularFile).limit(10000).toList()) {
                    if(Files.size(path)>1024*1024) continue;
                    JSONObject manifest=new JSONObject(Files.readString(path));JSONArray layers=manifest.optJSONArray("layers");
                    if(layers==null) continue;count++;int absent=0;
                    List<JSONObject> blobs=new ArrayList<>();JSONObject config=manifest.optJSONObject("config");if(config!=null) blobs.add(config);
                    for(int i=0;i<layers.length();i++) blobs.add(layers.getJSONObject(i));
                    for(JSONObject blob:blobs) {
                        String digest=blob.optString("digest");
                        if(!digest.matches("sha256:[a-fA-F0-9]{64}")) {absent++;continue;}
                        Path file=root.resolve("blobs").resolve(digest.replace(':','-'));
                        if(!Files.isRegularFile(file) || blob.has("size")&&Files.size(file)!=blob.getLong("size")) absent++;
                    }
                    missing+=absent;if(absent==0) complete++;
                }
                result.add(new Store(root.toString(),count,complete,missing));
            } catch(Exception ignored) {result.add(new Store(root.toString(),count,complete,missing));}
        }
        return List.copyOf(result);
    }
}
