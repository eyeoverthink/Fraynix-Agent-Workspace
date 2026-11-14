package fraymus.workspace;

import java.util.*;
import org.json.*;

/** Bounded image-plane evidence, never a semantic identity certificate or a learned appearance. */
final class TrajectoryGate145 {
    private record Sample(int ordinal, PixelTracker.Box box) {}
    private static final class Path {
        final ArrayDeque<Sample> samples=new ArrayDeque<>();
        void add(int ordinal,PixelTracker.Box box){samples.addLast(new Sample(ordinal,box));while(samples.size()>5)samples.removeFirst();}
        JSONObject forecast(int ordinal,int required){
            var out=new JSONObject().put("valid",false).put("samples",samples.size());
            if(samples.size()<required)return out.put("reason","Insufficient observed trajectory");
            var last=samples.getLast();int age=ordinal-last.ordinal();out.put("age",age).put("anchorOrdinal",last.ordinal()).put("anchorBox",last.box().json());
            if(age<0||age>12)return out.put("reason","Trajectory horizon exceeded");
            double mt=0,mx=0,my=0;int previous=-1;
            for(var s:samples){if(previous>=0&&s.ordinal()!=previous+1)return out.put("reason","Non-adjacent observations");previous=s.ordinal();mt+=s.ordinal();mx+=cx(s.box());my+=cy(s.box());}
            mt/=samples.size();mx/=samples.size();my/=samples.size();double tt=0,tx=0,ty=0;
            for(var s:samples){double dt=s.ordinal()-mt;tt+=dt*dt;tx+=dt*(cx(s.box())-mx);ty+=dt*(cy(s.box())-my);}
            double vx=tx/tt,vy=ty/tt,residual=0;
            for(var s:samples)residual=Math.max(residual,Math.hypot(cx(s.box())-(mx+vx*(s.ordinal()-mt)),cy(s.box())-(my+vy*(s.ordinal()-mt))));
            double radius=.015+residual*(1+age)+.0005*age*age;
            out.put("center",new JSONArray(new double[]{mx+vx*(ordinal-mt),my+vy*(ordinal-mt)})).put("velocity",new JSONArray(new double[]{vx,vy})).put("radius",radius).put("fitResidual",residual);
            if(Math.hypot(vx,vy)>.08||residual>.01||radius>.12)return out.put("reason","Motion or uncertainty outside fixed envelope");
            return out.put("valid",true).put("reason","Observed adjacent motion within fixed envelope");
        }
    }
    private final Path target=new Path();
    private final List<Path> rivals=new ArrayList<>();
    private boolean conflict;
    TrajectoryGate145(PixelTracker.Box seed){target.add(0,seed);}
    private static double cx(PixelTracker.Box b){return b.x()+b.w()/2;}
    private static double cy(PixelTracker.Box b){return b.y()+b.h()/2;}
    private static double distance(PixelTracker.Box b,JSONObject forecast){var c=forecast.getJSONArray("center");return Math.hypot(cx(b)-c.getDouble(0),cy(b)-c.getDouble(1));}
    private static boolean compatible(PixelTracker.Box b,JSONObject p){return p.optBoolean("valid")&&distance(b,p)<=p.getDouble("radius");}
    JSONObject step(JSONObject proposal,JSONArray locations){
        var refs=proposal.getJSONObject("references143");var ambiguity=proposal.getJSONObject("ambiguity144");int ordinal=refs.getInt("ordinal");boolean baseline=refs.getBoolean("baselineAccepted");
        if(baseline){
            if(!target.samples.isEmpty()&&target.samples.getLast().ordinal()!=ordinal-1)target.samples.clear();
            target.add(ordinal,PixelTracker.Box.from(proposal.getJSONArray("box")));rivals.clear();conflict=false;
        }
        var prediction=target.forecast(ordinal,4);var rivalEvidence=new JSONArray();
        boolean latched=ambiguity.getBoolean("latched");
        if(latched&&prediction.optBoolean("valid")){
            var assignments=new HashMap<Path,PixelTracker.Box>();int targetLocations=0;
            for(Object raw:locations){var b=PixelTracker.Box.from(((JSONObject)raw).getJSONArray("box"));boolean isTarget=compatible(b,prediction);if(isTarget)targetLocations++;
                var matches=new ArrayList<Path>();
                for(var lane:rivals){var p=lane.forecast(ordinal,2);var last=lane.samples.getLast();double gap=ordinal-last.ordinal();boolean near=p.optBoolean("valid")?distance(b,p)<=p.getDouble("radius")+.025:Math.hypot(cx(b)-cx(last.box()),cy(b)-cy(last.box()))<=.08*gap+.015;if(near)matches.add(lane);}
                if(matches.size()>1||(isTarget&&!matches.isEmpty())){conflict=true;continue;}
                if(isTarget)continue;
                if(matches.size()==1){var lane=matches.get(0);if(assignments.put(lane,b)!=null)conflict=true;}
                else if(rivals.size()<8){var lane=new Path();lane.add(ordinal,b);rivals.add(lane);}
                else conflict=true;
            }
            if(targetLocations>1)conflict=true;
            for(var e:assignments.entrySet()){
                var lane=e.getKey();if(lane.samples.getLast().ordinal()!=ordinal-1)lane.samples.clear();lane.add(ordinal,e.getValue());
            }
        }
        boolean separated=!rivals.isEmpty(),inTarget=false,scale=false;double targetError=-1;
        var recovery=refs.getJSONObject("recovery");
        PixelTracker.Box candidate=recovery.has("box")?PixelTracker.Box.from(recovery.getJSONArray("box")):null;
        if(candidate!=null&&prediction.optBoolean("valid")){
            targetError=distance(candidate,prediction);inTarget=targetError<=prediction.getDouble("radius");var b=target.samples.getLast().box();
            scale=candidate.w()/b.w()>=.75&&candidate.w()/b.w()<=1.35&&candidate.h()/b.h()>=.75&&candidate.h()/b.h()<=1.35;
        }
        for(var lane:rivals){var p=lane.forecast(ordinal,2);boolean separate=false;
            if(candidate!=null&&p.optBoolean("valid")&&prediction.optBoolean("valid")){
                double margin=distance(candidate,p)-p.getDouble("radius")-prediction.getDouble("radius")-.25*Math.max(candidate.w(),lane.samples.getLast().box().w());p.put("separationMargin",margin);separate=margin>0;
            }
            p.put("separated",separate);rivalEvidence.put(p);separated&=separate;
        }
        boolean qualified=latched&&ambiguity.getBoolean("blocked")&&inTarget&&scale&&separated&&!conflict;
        String reason=baseline?"Existing baseline accepted":!latched?"No retained ambiguity":!ambiguity.getBoolean("blocked")?"Waiting for existing appearance confirmation":conflict?"Competing paths intersected or association was ambiguous":!prediction.optBoolean("valid")?prediction.getString("reason"):!inTarget?"Return outside the observed target trajectory":!scale?"Return scale outside fixed trajectory bounds":!separated?"Competing trajectory remains plausible or unmeasured":"Confirmed appearance fits the target path and excludes measured rivals";
        return new JSONObject().put("schema","trajectory-gate-145-v1").put("qualified",qualified).put("baselineAccepted",baseline).put("target",prediction).put("rivals",rivalEvidence).put("sourceLocations",locations).put("conflict",conflict).put("targetError",targetError).put("targetCompatible",inTarget).put("scaleCompatible",scale).put("rivalsSeparated",separated).put("reason",reason)
            .put("scope","Only original-baseline observations anchor the target. Five samples per path, eight rivals, twelve-frame maximum horizon and capped uncertainty. Predictions and restored boxes never become anchors or appearances. Motion assumptions are image-plane bounds, not calibrated physical laws. Missing rivals do not erase their paths.");
    }
}
