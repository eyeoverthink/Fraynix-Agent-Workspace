package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class SmallReport163Test {
 @Test public void halfDecimalRenderingFixtureKeepsExactNumericAndLabelChecks()throws Exception{
  var before=SmallBody161.authored(SmallBody161Test.scene(.05,.04));var plan=SmallBody161.plan(before,SmallBody161Test.action(.1,.06));
  var run=SmallBody161.execute(before,plan,Files.createTempDirectory("report163-native"),()->{});assertTrue(run.getJSONObject("verification").getBoolean("passed"));
  // Rendering-only fixture: deliberately expose halfway times; never admit this as motion evidence.
  var samples=run.getJSONArray("samples");int[] indexes={0,samples.length()/2,samples.length()-1};double[] times={1.005,2.675,4.675};for(int i=0;i<3;i++)samples.getJSONObject(indexes[i]).put("timeS",times[i]);
  var j=new JSONObject().put("id",UUID.randomUUID().toString()).put("before",before).put("plan",plan).put("run",run);var report=SmallReport161.build(j);var data=new JSONObject(report.getJSONObject("files").getString("motion.json"));assertEquals("4.68 s",data.getJSONArray("sampleLabels").getString(indexes[2]).substring(0,6));
  assertTrue(report.similar(SmallReport161.build(WorldFrames.copy(j))));var observed=SmallBody161Test.observed(j);assertTrue(SmallReport161.audit(j,observed).getBoolean("passed"));var wrong=WorldFrames.copy(observed);wrong.getJSONArray("states").getJSONObject(2).put("label",wrong.getJSONArray("states").getJSONObject(2).getString("label").replace("4.68","4.67"));var badLabel=wrong;assertThrows(IllegalStateException.class,()->SmallReport161.audit(j,badLabel));
  wrong=WorldFrames.copy(observed);wrong.getJSONArray("states").getJSONObject(1).put("pose","[]");var badPose=wrong;assertThrows(IllegalStateException.class,()->SmallReport161.audit(j,badPose));
  Path out=Path.of("build/report-rounding163");Files.createDirectories(out);Files.writeString(out.resolve("index.html"),report.getJSONObject("files").getString("index.html"));Files.writeString(out.resolve("expected.json"),new JSONObject().put("scope","Halfway-time rendering fixture only, not admitted physics evidence").put("observed",observed).toString());
 }
}
