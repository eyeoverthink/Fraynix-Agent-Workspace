package fraymus.workspace;

import java.awt.image.BufferedImage;
import org.json.*;

/** Optional bounded trajectory evidence around the unchanged conservative144 guard. */
final class TrajectoryRecovery145 implements ImageTracker {
    private final AmbiguityGuard144 parent;
    private final TrajectorySearch145 search;
    private final TrajectoryGate145 gate;
    private final boolean enabled;
    TrajectoryRecovery145(String id,String label,BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean enabled){
        parent=new AmbiguityGuard144(id,label,first,seed,memory,true);search=new TrajectorySearch145(first,seed,memory,true);gate=new TrajectoryGate145(seed);this.enabled=enabled;
    }
    public JSONObject update(BufferedImage image){
        var proposal=parent.update(image);var ambiguity=proposal.getJSONObject("ambiguity144");var locations=new JSONArray();
        if(ambiguity.getBoolean("latched")){search.search(image);locations=search.locations();}
        var evidence=gate.step(proposal,locations);boolean restored=enabled&&evidence.getBoolean("qualified");var out=ParsedJson137.copy(proposal);var recovery=proposal.getJSONObject("references143").getJSONObject("recovery");
        if(restored)out.put("box",ParsedJson137.copy(new JSONObject().put("value",recovery.getJSONArray("box"))).getJSONArray("value")).put("lastKnownBox",recovery.getJSONArray("box")).put("status","trajectory-recovered-145").put("matchScore",recovery.getDouble("score"));
        boolean accepted=ambiguity.getBoolean("accepted")||restored;
        return out.put("trajectory145",evidence.put("enabled",enabled).put("restored",restored).put("wouldRestore",evidence.getBoolean("qualified")).put("accepted",accepted).put("blocked",ambiguity.getBoolean("blocked")&&!restored).put("withheld",out.isNull("box")).put("automaticAdmissions",0));
    }
}
