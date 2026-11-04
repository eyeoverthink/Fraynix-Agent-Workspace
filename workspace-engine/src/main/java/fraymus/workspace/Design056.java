package fraymus.workspace;
import org.json.*;
/** Bounded support-layout redesign; native movement and load evidence decide feasibility. */
final class Design056 {
 private Design056(){}
 static JSONObject policy(int required){return policy(required,true);}
 static JSONObject policy(int required,boolean allow){return new JSONObject().put("version","load-redesign-056-v1").put("requiredLoadN",Load056.required(new JSONObject().put("requiredLoadN",required))).put("protocol",Load056.protocol()).put("allowRedesign",allow);}
 static JSONArray proposals(JSONObject scene,JSONObject base){
  if(!Construction054.is(scene))throw new IllegalArgumentException("Load redesign requires declared054/055 box stock");var intent=base.getJSONObject("intent");var original=intent.getJSONObject("construction051");double start=original.optDouble("supportSpanM",Construction054.config(scene).getDouble("spanM")),maximum=Math.min(2,WorldPlan.sourceObject(scene,"beam").getJSONArray("size").getDouble(0)-WorldPlan.sourceObject(scene,"support-a").getJSONArray("size").getDouble(0)-.16);if(start>maximum+1e-9)throw new IllegalArgumentException("Original span exceeds allowed beam-end margin");
  double[] spans={start,(start+maximum)/2,maximum};String[] names={"maximum-force","wider-supports","widest-supports"};var out=new JSONArray();
  for(int i=0;i<3;i++){var config=WorldFrames.copy(original);if(i>0)config.put("supportSpanM",spans[i]);var plan=WorldPlan.build(scene,Construction051.intent(scene,config,intent.getDouble("forceLimitN"),intent.getDouble("torqueLimitNm")));out.put(new JSONObject().put("id",names[i]).put("plan",plan).put("hypothesis053",new JSONObject().put("forceLimitN",intent.getDouble("forceLimitN")).put("supportSpanM",spans[i]).put("objection","Wider supports are a hypothesis; retain original motion checks and test the same off-centre loads before selection.")));}return out;
 }
 static JSONObject choose(JSONObject study){for(Object raw:study.getJSONArray("candidates")){var c=(JSONObject)raw;if(c.getString("status").equals("completed")&&c.optJSONObject("metrics",new JSONObject()).optBoolean("passed")&&c.optJSONObject("structure053",new JSONObject()).optBoolean("passed")&&c.optJSONObject("load056",new JSONObject()).optBoolean("qualifies"))return c;}return null;}
}
