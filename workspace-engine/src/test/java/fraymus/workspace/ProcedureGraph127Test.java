package fraymus.workspace;
import org.json.*;import org.junit.*;import static org.junit.Assert.*;
/** Real native graph with explicitly labelled unit-host observations. */
public class ProcedureGraph127Test {
 @Test public void originalGraphSurvivesRetirementButNewExecutionNeedsActiveNodes()throws Exception {
  SkillGraph046Test.originalVerifiedSkill();
  try(var f=SkillGraph046Test.fresh()){var w=SkillGraph046Test.finish(f,f.workflows.start(f.input(f.world())).getString("id"));var p=w.getJSONObject("savedSkill");var parent=SkillGraph046Test.skill;var original=f.workflows.read(w.getString("id"));var inspect=f.workflows.inspectProcedure127(new JSONObject().put("id",p.getString("id")));assertTrue(inspect.toString(),inspect.getJSONObject("evidence").getBoolean("passed"));
   f.workflows.changeProcedure127(new JSONObject().put("id",parent.getString("id")).put("action","retire").put("reason","Explicit native graph dependency check").put("expectedHash",inspect.getString("expectedHash")));assertTrue(original.similar(f.workflows.read(w.getString("id"))));assertFalse(f.workflows.inspectProcedure127(new JSONObject().put("id",p.getString("id"))).getJSONObject("evidence").getBoolean("passed"));assertThrows(IllegalStateException.class,()->f.workflows.start(f.input(f.world())));
   var current=f.workflows.inspectProcedure127(new JSONObject().put("id",parent.getString("id")));assertTrue(current.toString(),current.getJSONObject("evidence").getBoolean("passed"));f.workflows.changeProcedure127(new JSONObject().put("id",parent.getString("id")).put("action","reactivate").put("reason","Restore original verified node").put("expectedHash",current.getString("expectedHash")));assertTrue(f.workflows.inspectProcedure127(new JSONObject().put("id",p.getString("id"))).getJSONObject("evidence").getBoolean("passed"));
  }
 }
}
