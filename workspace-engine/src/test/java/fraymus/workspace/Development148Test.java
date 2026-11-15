package fraymus.workspace;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.json.*;
import org.junit.*;
/** Exposed edition147 diagnostic, never fresh qualification. No camera access. */
public class Development148Test {
 @Test public void reportExposedRivalGeometry() throws Exception {
  String path=System.getenv("DEVELOPMENT148_ROOT");Assume.assumeNotNull(path);
  var root=Path.of(path);var source=Path.of("J:/Codex-Work/fraynix-workspace-edition147/build/webcam147-private/attempt-98Fe5G");
  var seed=new PixelTracker.Box(305./640,201./480,41./640,120./480);
  var search=new DiagnosticSearch148(ImageIO.read(source.resolve("0028.jpg").toFile()),seed,new JSONObject(Files.readString(root.resolve("build/development-memory148.json"))),true);
  var whole=new ReturnSearch148(ImageIO.read(source.resolve("0028.jpg").toFile()),seed,new JSONObject(Files.readString(root.resolve("build/development-memory148.json"))),true);
  var results=new JSONArray();for(int f:new int[]{28,57,65,77}){search.diagnostic=new JSONArray();search.search(ImageIO.read(source.resolve(String.format("%04d.jpg",f)).toFile()));whole.search(ImageIO.read(source.resolve(String.format("%04d.jpg",f)).toFile()));results.put(new JSONObject().put("frame",f).put("regions",search.diagnostic).put("locations148",whole.locations()));}
  Files.writeString(root.resolve("build/rival-geometry148.json"),results.toString(2));
 }
}
