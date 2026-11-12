package fraymus.workspace;
import java.awt.image.BufferedImage;
import java.util.*;
import org.json.JSONObject;

/** Explicit per-run research choice; the incumbent remains the default. */
final class ObservationMethods129 {
 static final String DEFAULT="legacy-flow",CANDIDATE="scale-aware-129";
 static String policy(JSONObject input){String p=input.optString("trackerPolicy",DEFAULT);if(!Set.of(DEFAULT,CANDIDATE).contains(p))throw new IllegalArgumentException("Choose an installed tracking method");return p;}
 static ImageTracker create(String policy,String id,String label,BufferedImage first,PixelTracker.Box box){return switch(policy){case DEFAULT->new FlowTracker(id,label,first,box);case CANDIDATE->new ScaleAwareFlow129(id,label,first,box);default->throw new IllegalArgumentException("Unknown tracking method");};}
 static String identity(String version){var classes=new TreeMap<String,Class<?>>();for(Class<?> c:List.of(ObservationMethods129.class,ObservationFilter.class,WorldObservationStore.class,FlowTracker.class,ScaleAwareFlow129.class,AdaptiveTracker.class,PixelTracker.class))collect(c,classes);var s=new StringBuilder(version);for(var entry:classes.entrySet())try(var in=entry.getValue().getResourceAsStream(entry.getKey().substring(entry.getKey().lastIndexOf('.')+1)+".class")){if(in==null)throw new IllegalStateException("Missing tracking implementation class");s.append(entry.getKey()).append(PerceptionStore.hash(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(s.toString());}
 private static void collect(Class<?> c,Map<String,Class<?>> found){if(found.putIfAbsent(c.getName(),c)==null)for(Class<?> nested:c.getDeclaredClasses())collect(nested,found);}
}
