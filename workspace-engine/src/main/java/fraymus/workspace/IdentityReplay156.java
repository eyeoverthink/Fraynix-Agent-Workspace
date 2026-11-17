package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Fresh full-object evidence starts a new observed segment; expired forecasts never become evidence. */
final class IdentityReplay156 implements ImageTracker {
    private record Observation(int ordinal,PixelTracker.Box box){}
    private static final class Lane {
        FlowTracker flow; PixelTracker.Box pending; final ArrayDeque<Observation> observations=new ArrayDeque<>();int start;
        void clear(){flow=null;pending=null;observations.clear();start=0;}
    }
    private final ReturnConfirmation148 parent;
    private final StructureReplay156 search;
    private final Map<String,Lane> lanes=new LinkedHashMap<>();
    private final boolean enabled;
    private final String id,label;
    private boolean ambiguityRetained;
    private int ordinal;
    IdentityReplay156(String id,String label,BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean enabled,boolean packed){
        this.id=id;this.label=label;this.enabled=enabled;parent=new ReturnConfirmation148(id,label,first,seed,parentMemory(memory),true);search=new StructureReplay156(first,seed,memory,packed);
        // A competing full match in the original image is never forgotten after it disappears.
        assessAmbiguity(search.search(first),seed);
    }
    static JSONObject parentMemory(JSONObject memory){
        var original=ParsedJson137.copy(memory);for(Object raw:original.getJSONArray("examples"))((JSONObject)raw).remove("structure149");return original;
    }
    private void assessAmbiguity(List<StructureReplay156.Match> matches,PixelTracker.Box baseline){
        for(var m:matches)if(m.strong()){
            if(!m.unique()||(baseline!=null&&PixelTracker.iou(m.box(),baseline)<.25))ambiguityRetained=true;
            for(var other:matches)if(other.strong()&&PixelTracker.iou(m.box(),other.box())<.25)ambiguityRetained=true;
        }
    }
    public JSONObject update(BufferedImage image){
        ordinal++;var previous=parent.update(image);var out=ParsedJson137.copy(previous);var refs=previous.getJSONObject("references143");
        boolean baseline=refs.getBoolean("baselineAccepted"),lost=refs.getBoolean("lossObserved");
        var matches=search.search(image);assessAmbiguity(matches,baseline&&!previous.isNull("box")?PixelTracker.Box.from(previous.getJSONArray("box")):null);
        var hypotheses=new JSONArray();JSONObject chosen=null;
        for(var match:matches){var lane=lanes.computeIfAbsent(match.key(),k->new Lane());boolean eligible=lost&&!baseline&&!ambiguityRetained&&match.strong()&&match.unique();
            boolean coherent=false;var motion=new JSONObject();
            if(eligible&&lane.flow!=null&&PixelTracker.iou(lane.pending,match.box())>=.55){
                var tracked=lane.flow.update(image);motion=tracked.getJSONObject("motion");
                coherent=!tracked.isNull("box")&&"pyramidal-flow".equals(motion.getString("method"))&&!motion.getBoolean("ambiguous")&&motion.getInt("inliers")>=8&&motion.getDouble("medianForwardBackwardPixels")<=1.2&&PixelTracker.iou(PixelTracker.Box.from(tracked.getJSONArray("box")),match.box())>=.7;
            }
            if(!eligible)lane.clear();else{
                if(!coherent)lane.clear();if(lane.start==0)lane.start=ordinal;
                lane.observations.addLast(new Observation(ordinal,match.box()));while(lane.observations.size()>5)lane.observations.removeFirst();
                lane.pending=match.box();lane.flow=new FlowTracker(id,label,image,match.box());
            }
            boolean qualified=eligible&&lane.observations.size()>=4;
            var evidence=match.json().put("eligible",eligible).put("coherentMotion",coherent).put("motion",motion).put("confirmations",lane.observations.size()).put("requiredConfirmations",4).put("qualified",qualified).put("segment",qualified?segment(lane,match.key()):JSONObject.NULL);
            hypotheses.put(evidence);if(qualified&&(chosen==null||match.score()>chosen.getDouble("score")))chosen=evidence;
        }
        boolean wouldRestore=previous.isNull("box")&&chosen!=null,restored=enabled&&wouldRestore;
        if(restored)out.put("box",chosen.getJSONArray("box")).put("lastKnownBox",chosen.getJSONArray("box")).put("status","fresh-identity-149").put("matchScore",chosen.getDouble("score"));
        return out.put("identity149",new JSONObject().put("schema","fresh-identity-149-v1").put("enabled",enabled).put("parentAccepted",!previous.isNull("box")).put("baselineAccepted",baseline).put("ordinal",ordinal).put("ambiguityRetained",ambiguityRetained).put("hypotheses",hypotheses).put("chosen",chosen==null?JSONObject.NULL:chosen).put("evaluations",search.evaluations()).put("wouldRestore",wouldRestore).put("restored",restored).put("accepted",!out.isNull("box")).put("automaticAdmissions",0).put("physicalActionAuthorized",false).put("scope","148 measurements preserved. Four adjacent full-object source matches and optical-flow checks create a new observed segment. Every new sample must independently match the same immutable reference. Known competing matches remain a veto; predictions never count as identity evidence. Image/sample motion is not calibrated physical velocity. Unseen identical substitutions cannot be distinguished; no physical action authority or automatic appearance admission."));
    }
    private static JSONObject segment(Lane lane,String key){
        var samples=new JSONArray();double st=0,sx=0,sy=0,stt=0,stx=0,sty=0;int n=lane.observations.size(),origin=lane.observations.getFirst().ordinal;
        for(var o:lane.observations){double t=o.ordinal-origin,x=o.box.x()+o.box.w()/2,y=o.box.y()+o.box.h()/2;samples.put(new JSONObject().put("ordinal",o.ordinal).put("box",o.box.json()));st+=t;sx+=x;sy+=y;stt+=t*t;stx+=t*x;sty+=t*y;}
        double denominator=n*stt-st*st,vx=(n*stx-st*sx)/denominator,vy=(n*sty-st*sy)/denominator;
        double ax=(sx-vx*st)/n,ay=(sy-vy*st)/n,residual=0;
        for(var o:lane.observations){double t=o.ordinal-origin;residual=Math.max(residual,Math.hypot(o.box.x()+o.box.w()/2-ax-vx*t,o.box.y()+o.box.h()/2-ay-vy*t));}
        int next=lane.observations.getLast().ordinal+1;
        return new JSONObject().put("sourceKey",key).put("startOrdinal",lane.start).put("observations",samples).put("velocityX",vx).put("velocityY",vy).put("maximumFitResidual",residual).put("nextSample",new JSONObject().put("ordinal",next).put("centerX",ax+vx*(next-origin)).put("centerY",ay+vy*(next-origin)).put("measured",false)).put("timeUnit","sample").put("forecastUsedForAdmission",false).put("calibratedUncertainty",false);
    }
}
