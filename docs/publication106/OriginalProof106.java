package fraymus.workspace;
import java.nio.file.*;
import org.json.*;
/** Diagnostic against actual retained trial originals; not an execution fixture. */
public class OriginalProof106 {
 public static void main(String[] args)throws Exception {
  var original=new JSONObject(Files.readString(Path.of("build/numerical106/result.json")));
  var binding=original.getJSONArray("tasks").getJSONObject(1).getJSONArray("children").getJSONObject(0).getJSONObject("workflow").getJSONObject("implementation103");
  String refusal="";try{Implementation103.current(binding);throw new AssertionError("Old runtime binding accepted");}catch(IllegalStateException e){refusal=e.getMessage();}
  if(!refusal.equals("Implementation reuse protocol changed; start a newly reviewed request"))throw new AssertionError(refusal);
  var g=new JSONObject(Files.readString(Path.of("build/tiny106-original-goal.json")));var files=g.getJSONObject("pendingFiles");String raw=files.getString("inputs/source.json"),persisted=g.getJSONObject("sourceData").toString();
  if(raw.equals(persisted)||!new JSONObject(raw).similar(g.getJSONObject("sourceData")))throw new AssertionError("Expected equal numeric values, unequal serialized text");
  String mismatch="";try{Implementation103.inputs(g,files);throw new AssertionError("Original bad input accepted");}catch(IllegalStateException e){mismatch=e.getMessage();}
  if(!mismatch.equals("Reused artifact inputs differ from the frozen fresh source"))throw new AssertionError(mismatch);
  System.out.println(new JSONObject().put("passed",true).put("originalSource",original.getString("source")).put("oldImplementationCode",binding.getString("codeSha256")).put("currentImplementationCode",Implementation103.code()).put("oldBindingRefusal",refusal).put("originalInputRefusal",mismatch).put("originalReservedInput",raw).put("persistedSourceData",persisted).put("scope","Actual earlier binding refused by unchanged protocol verifier after legitimate GoalRunner producer change. Original numeric values agree; exact JSON bytes differ and remain refused. No old proof identity rewritten.").toString(2));
 }
}
