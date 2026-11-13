package fraymus.workspace;
import java.nio.file.*;
import org.json.*;
/** Scores saved numerical traces after tracking; no later labels reach the tracker. */
public final class AuditStudy130 {
 public static void main(String[] args)throws Exception{
  Path input=Path.of(args[0]),output=Path.of(args[1]);var data=new JSONObject(Files.readString(input));var refs=new JSONArray();for(Object raw:data.getJSONArray("rows"))((JSONObject)raw).put("bodyId","subject");for(Object raw:data.getJSONArray("scores")){var r=(JSONObject)raw;refs.put(new JSONObject().put("ordinal",r.getInt("ordinal")).put("bodyId","subject").put("positionM",r.getJSONArray("referenceM")));}
  var audit=PhysicalAudit130.compute(data,new JSONObject().put("source","Original EPFL250mm grid; seed excluded; same-camera correlated references").put("referenceRadiusM",Math.sqrt(2)*.125).put("references",refs));audit.put("inputSha256",PerceptionStore.hash(Files.readAllBytes(input))).put("auditCodeSha256",PhysicalAudit130.identity());Files.writeString(output,audit.toString(2),StandardOpenOption.CREATE_NEW);
 }
}
