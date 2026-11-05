package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Clock-aligned evidence, without changing observations into native world authority. */
final class EvidenceReplay049 {
    static final String SCHEMA = "fraynix.evidence-stream-049-v1";
    static final int MAX_BYTES = 1_048_576;
    private record Clock(double start, double end, double slope, double offset, double uncertainty) {
        double map(double device) {
            if (device < start || device > end) throw new IllegalArgumentException("Event outside clock anchors; extrapolation refused");
            return device * slope + offset;
        }
    }
    private record Source(String id, String kind, String clockId, Clock clock, double period,
                          double beamX, double beamError) {}
    private record Event(Source source, long seq, double device, double time, String state,
                         double x, double y, double error) {}
    private record Crossing(Source camera, Event before, Event after, String kind, double low, double high) {}

    static JSONObject analyze(JSONObject input) {
        SpatialScene.keys(input, "schema", "title", "provenance", "source", "frame", "target", "sources", "events");
        if (!SCHEMA.equals(input.getString("schema"))) throw new IllegalArgumentException("Unsupported evidence stream schema");
        text(input, "title", 120); text(input, "source", 1000); text(input, "target", 80);
        if (!Set.of("synthetic", "recorded").contains(input.getString("provenance")))
            throw new IllegalArgumentException("Declare synthetic or recorded provenance");
        var frame = input.getJSONObject("frame");
        SpatialScene.keys(frame, "id", "unit", "calibrationSha256");
        text(frame, "id", 80); hash(frame, "calibrationSha256");
        if (!"m".equals(frame.getString("unit"))) throw new IllegalArgumentException("Replay coordinates must use metres");
        JSONArray definitions = input.getJSONArray("sources"), rawEvents = input.getJSONArray("events");
        if (definitions.isEmpty() || definitions.length() > 4 || rawEvents.isEmpty() || rawEvents.length() > 4096)
            throw new IllegalArgumentException("Use 1..4 sources and 1..4096 events");
        Map<String, Source> sources = new LinkedHashMap<>();
        var clocks = new JSONArray();
        for (Object raw : definitions) {
            var d = (JSONObject)raw;
            String kind = d.getString("kind");
            if (!Set.of("camera", "beam").contains(kind)) throw new IllegalArgumentException("Unknown source kind");
            if (kind.equals("beam")) SpatialScene.keys(d, "id", "kind", "clockId", "clock", "periodMs", "xM", "uncertaintyM");
            else SpatialScene.keys(d, "id", "kind", "clockId", "clock", "periodMs");
            String id = text(d, "id", 60), clockId = text(d, "clockId", 60);
            if (!id.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("Use simple source identifiers");
            var c = d.getJSONObject("clock");
            SpatialScene.keys(c, "deviceStartMs", "deviceEndMs", "sessionStartMs", "sessionEndMs", "uncertaintyMs", "evidenceSha256");
            hash(c, "evidenceSha256");
            double ds = number(c,"deviceStartMs",0,9e12), de = number(c,"deviceEndMs",0,9e12);
            double ss = number(c,"sessionStartMs",0,3_600_000), se = number(c,"sessionEndMs",0,3_600_000);
            if (de <= ds || se <= ss) throw new IllegalArgumentException("Clock anchors must increase");
            double slope = (se-ss)/(de-ds);
            if (slope < .95 || slope > 1.05) throw new IllegalArgumentException("Clock rate differs by more than 5%; check units or resets");
            double uncertainty = number(c,"uncertaintyMs",0,1000);
            var clock = new Clock(ds,de,slope,ss-ds*slope,uncertainty);
            var s = new Source(id,kind,clockId,clock,number(d,"periodMs",1,10_000),
                kind.equals("beam")?number(d,"xM",-1000,1000):0,
                kind.equals("beam")?number(d,"uncertaintyM",0,10):0);
            if (sources.put(id,s)!=null) throw new IllegalArgumentException("Duplicate source identity");
            clocks.put(new JSONObject().put("sourceId",id).put("ratePpm",(slope-1)*1e6)
                .put("offsetMs",clock.offset).put("uncertaintyMs",uncertainty));
        }
        Map<String, List<Event>> streams = new LinkedHashMap<>();
        sources.keySet().forEach(id -> streams.put(id,new ArrayList<>()));
        var aligned = new JSONArray();
        var gaps = new JSONArray();
        List<Event> all = new ArrayList<>();
        long sequenceDrops=0;
        int missing=0, predictions=0, measured=0, reacquisitions=0;
        for (Object raw : rawEvents) {
            var v=(JSONObject)raw;
            Source s=sources.get(v.getString("sourceId"));
            if (s==null) throw new IllegalArgumentException("Event has an unknown source");
            String state=v.getString("state");
            if (s.kind.equals("beam")) {
                SpatialScene.keys(v,"sourceId","seq","deviceMs","state");
                if (!Set.of("clear","blocked").contains(state)) throw new IllegalArgumentException("Beam state must be clear or blocked");
            } else if (state.equals("missing")) SpatialScene.keys(v,"sourceId","seq","deviceMs","state");
            else if (state.equals("measured")) SpatialScene.keys(v,"sourceId","seq","deviceMs","state","xM","yM","uncertaintyM");
            else if (state.equals("predicted")) SpatialScene.keys(v,"sourceId","seq","deviceMs","state","xM","yM","uncertaintyM","issuedDeviceMs");
            else throw new IllegalArgumentException("Unknown camera evidence state");
            double seq=number(v,"seq",0,1_000_000_000), device=number(v,"deviceMs",0,9e12);
            if (seq!=Math.rint(seq)) throw new IllegalArgumentException("Sequence must be an integer");
            double time=s.clock.map(device);
            boolean positioned=s.kind.equals("camera")&&!state.equals("missing");
            var event=new Event(s,(long)seq,device,time,state,positioned?number(v,"xM",-1000,1000):0,
                positioned?number(v,"yM",-1000,1000):0,positioned?number(v,"uncertaintyM",0,10):0);
            if (state.equals("predicted")) {
                double issued=number(v,"issuedDeviceMs",0,9e12);
                s.clock.map(issued);
                if (issued>=device) throw new IllegalArgumentException("Prediction must have been issued before its target time");
                predictions++;
            } else if (state.equals("missing")) missing++;
            else if (state.equals("measured")) measured++;
            var stream=streams.get(s.id);
            if (!stream.isEmpty()) {
                Event before=stream.getLast();
                if (event.seq<=before.seq || device<=before.device) throw new IllegalArgumentException("Source sequence/clock reset or duplicate; begin a new recording");
                long dropped=event.seq-before.seq-1;
                sequenceDrops+=dropped;
                if (dropped>0 || time-before.time>s.period*1.5) gaps.put(new JSONObject().put("sourceId",s.id)
                    .put("fromMs",before.time).put("toMs",time).put("missingSequenceCount",dropped));
                if (state.equals("measured")&&Set.of("missing","predicted").contains(before.state)) reacquisitions++;
            }
            stream.add(event); all.add(event);
        }
        for (var stream:streams.values()) if(stream.isEmpty()) throw new IllegalArgumentException("Each declared source requires events");
        all.sort(Comparator.comparingDouble(Event::time).thenComparing(e->e.source.id).thenComparingLong(Event::seq));
        for (Event e:all) {
            var row=new JSONObject().put("sourceId",e.source.id).put("kind",e.source.kind).put("seq",e.seq)
                .put("sessionMs",e.time).put("timeUncertaintyMs",e.source.clock.uncertainty).put("state",e.state);
            if (e.source.kind.equals("camera")&&!e.state.equals("missing"))row.put("xM",e.x).put("yM",e.y).put("uncertaintyM",e.error);
            aligned.put(row);
        }
        var associations=new JSONArray();
        var crossings=new JSONArray();
        int blockedBaselines=0, unobservedTransitions=0;
        for (Source beam:sources.values()) if(beam.kind.equals("beam")) {
            List<Crossing> candidates=new ArrayList<>();
            for (Source camera:sources.values()) if(camera.kind.equals("camera")) {
                var stream=streams.get(camera.id);
                for(int i=1;i<stream.size();i++) {
                    Event a=stream.get(i-1), b=stream.get(i);
                    if(!contiguous(a,b)||a.state.equals("missing")||b.state.equals("missing"))continue;
                    boolean forward=a.x+a.error<beam.beamX-beam.beamError&&b.x-b.error>beam.beamX+beam.beamError;
                    boolean reverse=b.x+b.error<beam.beamX-beam.beamError&&a.x-a.error>beam.beamX+beam.beamError;
                    if(!forward&&!reverse)continue;
                    String kind=a.state.equals("measured")&&b.state.equals("measured")?"measured":"predicted";
                    var c=new Crossing(camera,a,b,kind,a.time-camera.clock.uncertainty,b.time+camera.clock.uncertainty);
                    candidates.add(c);
                    crossings.put(new JSONObject().put("cameraId",camera.id).put("beamId",beam.id).put("evidence",kind)
                        .put("fromMs",c.low).put("toMs",c.high).put("fromSeq",a.seq).put("toSeq",b.seq));
                }
            }
            var stream=streams.get(beam.id);
            if(stream.getFirst().state.equals("blocked"))blockedBaselines++;
            for(int i=1;i<stream.size();i++) {
                Event before=stream.get(i-1), e=stream.get(i);
                if(!before.state.equals("clear")||!e.state.equals("blocked"))continue;
                var result=new JSONObject().put("beamId",beam.id).put("seq",e.seq);
                if(!contiguous(before,e)) {
                    unobservedTransitions++;
                    associations.put(result.put("status","gap-before-block").put("fromMs",before.time).put("toMs",e.time));continue;
                }
                double low=before.time-beam.clock.uncertainty, high=e.time+beam.clock.uncertainty;
                result.put("fromMs",low).put("toMs",high);
                var matches=candidates.stream().filter(c->c.low<=high&&c.high>=low).toList();
                String status="witness-only";
                if(matches.size()>1)status="ambiguous";
                else if(matches.size()==1) {
                    Crossing c=matches.getFirst();
                    int witnesses=0;
                    for(int j=1;j<stream.size();j++){
                        Event a=stream.get(j-1), b=stream.get(j);
                        if(a.state.equals("clear")&&b.state.equals("blocked")&&contiguous(a,b)
                            &&c.low<=b.time+beam.clock.uncertainty&&c.high>=a.time-beam.clock.uncertainty)witnesses++;
                    }
                    status=witnesses>1?"ambiguous":c.camera.clockId.equals(beam.clockId)?"shared-clock":c.kind.equals("measured")?"measured-crossing-supported":"prediction-near-witness";
                    result.put("cameraId",c.camera.id).put("cameraEvidence",c.kind);
                }
                associations.put(result.put("status",status));
            }
        }
        var displacement=new JSONArray();
        for(Source camera:sources.values())if(camera.kind.equals("camera")) {
            var visible=streams.get(camera.id).stream().filter(e->e.state.equals("measured")).toList();
            if(visible.size()>=2){Event a=visible.getFirst(), b=visible.getLast();displacement.put(new JSONObject().put("cameraId",camera.id)
                .put("distanceM",Math.hypot(b.x-a.x,b.y-a.y)).put("declaredUncertaintyM",a.error+b.error)
                .put("firstSeq",a.seq).put("lastSeq",b.seq));}
        }
        return new JSONObject().put("schema",SCHEMA).put("title",input.getString("title")).put("provenance",input.getString("provenance"))
            .put("target",input.getString("target")).put("frame",WorldFrames.copy(frame)).put("clocks",clocks).put("events",aligned)
            .put("gaps",gaps).put("crossings",crossings).put("associations",associations).put("displacement",displacement)
            .put("startMs",all.getFirst().time).put("endMs",all.getLast().time)
            .put("counts",new JSONObject().put("measured",measured).put("predicted",predictions).put("missing",missing)
                .put("droppedSequenceNumbers",sequenceDrops).put("reacquisitionTransitions",reacquisitions)
                .put("initialBlockedBaselines",blockedBaselines).put("unobservedBeamTransitions",unobservedTransitions))
            .put("scope","Declared recorded evidence. A beam supports an event, not target identity or a hidden trajectory. Clock/position uncertainties are supplied bounds, not validated confidence intervals.");
    }
    private static boolean contiguous(Event a,Event b){return b.seq==a.seq+1&&b.time-a.time<=b.source.period*1.5;}
    private static String text(JSONObject o,String key,int max){String s=o.getString(key);if(s.isBlank()||s.length()>max)throw new IllegalArgumentException("Invalid "+key);return s;}
    private static void hash(JSONObject o,String key){if(!o.getString(key).matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Declare a SHA-256 for "+key);}
    private static double number(JSONObject o,String key,double low,double high){Object raw=o.get(key);if(!(raw instanceof Number))throw new IllegalArgumentException("Numeric "+key+" required");double n=((Number)raw).doubleValue();if(!Double.isFinite(n)||n<low||n>high)throw new IllegalArgumentException("Out-of-range "+key);return n;}
}
