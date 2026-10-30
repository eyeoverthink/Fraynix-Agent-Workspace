package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
public class SceneExchangeTest {
    @Test public void exportedSceneRecreatesNativeGeometryWithNewIdentitiesAndProvenance() throws Exception {
        var root=Files.createTempDirectory("scene-exchange-");try(var scenes=new SceneStore(root)){
            var original=scenes.save(new JSONObject().put("title","Portable scene").put("parameters",new JSONObject().put("angleDeg",32).put("massKg",2).put("offsetZ",.5)));
            var file=scenes.exportScene(original.getString("id"));var imported=scenes.importScene(new JSONObject().put("package",new JSONObject(file.toString())));var copy=imported.getJSONObject("scene");
            assertNotEquals(original.getString("id"),copy.getString("id"));assertEquals(original.getString("title"),copy.getString("title"));assertTrue(original.getJSONObject("parameters").similar(copy.getJSONObject("parameters")));
            for(String role:List.of("block","ground","ramp"))assertNotEquals(original.getJSONObject("entityIds").getString(role),copy.getJSONObject("entityIds").getString(role));
            assertEquals(file.getString("geometrySha256"),scenes.exportScene(copy.getString("id")).getString("geometrySha256"));assertEquals(imported.getJSONObject("receipt").getString("id"),copy.getString("importReceiptId"));
            assertEquals(1,scenes.read(original.getString("id")).getInt("version"));
        }
        try(var scenes=new SceneStore(root)){assertEquals(2,scenes.scenes().length());}
    }
    @Test public void unsupportedFieldsGeometryAndUnitsAreRejectedBeforeCreatingScenes() throws Exception {
        try(var scenes=new SceneStore(Files.createTempDirectory("scene-exchange-reject-"))){
            var scene=scenes.save(new JSONObject());var file=scenes.exportScene(scene.getString("id"));
            file.put("script","perform arbitrary actions");assertThrows(IllegalArgumentException.class,()->scenes.importScene(new JSONObject().put("package",file)));file.remove("script");
            file.getJSONObject("geometry").getJSONArray("objects").getJSONObject(0).getJSONArray("position").put(0,99);
            assertThrows(IllegalArgumentException.class,()->scenes.importScene(new JSONObject().put("package",file)));
            var wrong=scenes.exportScene(scene.getString("id")).put("units","pixels");assertThrows(IllegalArgumentException.class,()->scenes.importScene(new JSONObject().put("package",wrong)));
            assertEquals(1,scenes.scenes().length());
        }
    }
    @Test public void activePhysicsCannotBeOverwrittenByAnImport() throws Exception {
        try(var scenes=new SceneStore(Files.createTempDirectory("scene-exchange-active-"))){
            var scene=scenes.save(new JSONObject());var file=scenes.exportScene(scene.getString("id"));
            scenes.control(new JSONObject().put("action","start").put("id",scene.getString("id")).put("expectedVersion",1).put("seconds",1));scenes.control(new JSONObject().put("action","pause"));
            assertThrows(IllegalStateException.class,()->scenes.importScene(new JSONObject().put("package",file)));assertEquals(1,scenes.scenes().length());
        }
    }
}
