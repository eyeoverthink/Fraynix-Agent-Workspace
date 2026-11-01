package fraymus.workspace;
import org.json.*;
/** Additional sensor audit; the original independent native outcome verifier stays unchanged. */
final class ContactVerify {
 static JSONObject check(JSONObject run){
  if(!run.getString("controllerVersion").equals(ContactPhysics.VERSION))throw new IllegalStateException("Contact controller identity changed");int observed=0,solverOnly=0;double impulse=0;
  for(Object raw:run.getJSONArray("samples")){var sample=(JSONObject)raw;var observation=sample.getJSONObject("nativeContact");if(!observation.getString("method").equals("Persistent native solver manifold, actual positive normal impulse"))throw new IllegalStateException("Native contact authority changed");int count=0;for(Object value:observation.getJSONArray("points")){var p=(JSONObject)value;double d=p.getDouble("distanceM"),n=p.getDouble("normalImpulseNs");if(!Double.isFinite(d)||!Double.isFinite(n)||n<0)throw new IllegalStateException("Invalid native manifold observation");if(d<=.015&&n>1e-6){count++;impulse+=n;}}if(count!=sample.getInt("contacts"))throw new IllegalStateException("Native solver points do not reconstruct contact count");if(count>0){observed++;if(observation.getInt("legacyProbeContacts")==0)solverOnly++;}}
  return new JSONObject().put("passed",true).put("solverContactSamples",observed).put("solverOnlySamples",solverOnly).put("sampledNormalImpulseNs",impulse).put("scope","Sampled actual solver impulses/contact observations, not total physical energy or a photographic detector");
 }
}
