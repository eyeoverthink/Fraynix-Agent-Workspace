package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Read-only recorded-pixel comparison. The manifest has no returned locations or labels. */
public class RotationMeasure153 {
    public static void main(String[] args)throws Exception {
        Path manifest=Path.of(args[0]),out=Path.of(args[1]);var input=new JSONObject(Files.readString(manifest));
        var images=new ArrayList<BufferedImage>();var hashes=new JSONArray();
        for(Object path:input.getJSONArray("images")){byte[] bytes=Files.readAllBytes(Path.of((String)path));hashes.put(PerceptionStore.hash(bytes));images.add(WorldObservationStore.decode(bytes));}
        var seed=PixelTracker.Box.from(input.getJSONArray("seed"));var memory=input.getJSONObject("memory");var result=new JSONObject().put("sourceManifestSha256",PerceptionStore.hash(Files.readAllBytes(manifest))).put("imageHashes",hashes).put("scope","Exposed recorded pixels; no labels enter either implementation. Source images remain private.");
        JSONArray reference=null;
        for(var mode:args.length>2&&args[2].equals("fast-only")?new RotationSearch153.Arithmetic[]{RotationSearch153.Arithmetic.PRECOMPUTED}:RotationSearch153.Arithmetic.values()){
            long start=System.nanoTime();var tracker=new RotationReturn153(images.getFirst(),seed,memory,mode);var trace=new JSONArray();
            for(int i=1;i<images.size();i++){trace.put(tracker.update(images.get(i)));if(i%12==0)System.out.println(mode+" "+i+"/"+(images.size()-1));}
            double ms=(System.nanoTime()-start)/1e6;result.put(mode.name(),new JSONObject().put("ms",ms).put("trace",trace));
            if(reference!=null&&!reference.similar(trace))throw new AssertionError("Optimized trace differs from scalar reference");reference=trace;Files.writeString(out,result.toString(2));
        }
        // Alternating complete initialization plus short sequences; no warm score cache.
        if(args.length>2&&args[2].equals("paired")){
            var timings=new JSONArray();int end=Math.min(images.size(),6);
            for(int repeat=0;repeat<3;repeat++)for(int order=0;order<2;order++){
                var mode=RotationSearch153.Arithmetic.values()[(repeat+order)%2];long start=System.nanoTime();var tracker=new RotationReturn153(images.getFirst(),seed,memory,mode);
                for(int i=1;i<end;i++)tracker.update(images.get(i));timings.put(new JSONObject().put("repeat",repeat).put("mode",mode.name()).put("framesIncludingSeed",end).put("ms",(System.nanoTime()-start)/1e6));
            }result.put("pairedTimings",timings);
        }
        for(int i=0;i<input.getJSONArray("images").length();i++)if(!hashes.getString(i).equals(PerceptionStore.hash(Files.readAllBytes(Path.of(input.getJSONArray("images").getString(i))))))throw new IllegalStateException("Source changed during measurement");
        result.put("passed",true).put("exactTraceComparison",result.has("SCALAR"));Files.writeString(out,result.toString(2));
        System.out.println(new JSONObject().put("passed",true).put("exactTraceComparison",result.has("SCALAR")).put("accepted",reference.toList().stream().filter(x->((Map<?,?>)x).get("box")!=null).count()));
    }
}
