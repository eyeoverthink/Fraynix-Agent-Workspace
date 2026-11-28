package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static fraymus.workspace.ObserverGeometry.*;
import static fraymus.workspace.SpatialScene.array;

/** Resolve a human reference and search bounded alternatives using the actual scene verifier. */
final class LoadingAisleIntent {
    private static final Pattern REQUEST = Pattern.compile(
            "(?i)^((?:please\\s+)?(?:move|shift)\\s+)(?:(?:the|a)\\s+)?([^\\r\\n.!?]{0,100}?)\\bcrate\\b");
    private static final List<Double> OFFSETS = List.of(.20,.35,.50,.65);

    record Request(String original, String canonical, String reference, int distanceCm) {}

    static Request parse(String original) {
        var text = original.strip();
        var match = REQUEST.matcher(text);
        if (!match.find())
            throw new IllegalArgumentException("Name or select a crate and give a distance from 40 to 200 cm away from the loading door.");
        String canonical = match.group(1) + "the selected crate" + text.substring(match.end());
        int distanceCm = LoadingAisle.requestDistanceCm(canonical);
        String reference = normalize(match.group(2));
        if (reference.matches(".*\\b(?:and|then)\\b.*"))
            throw new IllegalArgumentException("This review accepts one crate and one move.");
        return new Request(original,canonical,reference,distanceCm);
    }

    static List<JSONObject> options(JSONObject parent, Request request, String selectedId) {
        var all = crates(parent);
        if (all.isEmpty()) throw new IllegalArgumentException("No dynamic crate is labelled in this bay.");
        if (List.of("selected","this").contains(request.reference)) {
            var selected = all.stream().filter(o -> o.getString("id").equals(selectedId)).toList();
            return selected.isEmpty() ? all : selected;
        }
        if (request.reference.isEmpty()) return all;
        var named = all.stream().filter(c -> label(c).equals(request.reference + " crate") ||
                normalize(c.getString("sourceId")).equals(request.reference)).toList();
        return named.isEmpty() ? all : named;
    }

    static String resolved(JSONObject parent, Request request, String selectedId) {
        var matches = options(parent,request,selectedId);
        if (matches.size()!=1) return "";
        var only = matches.getFirst();
        if (request.reference.isEmpty() || label(only).equals(request.reference+" crate") ||
                normalize(only.getString("sourceId")).equals(request.reference) ||
                (List.of("selected","this").contains(request.reference) && only.getString("id").equals(selectedId)))
            return only.getString("id");
        return "";
    }

    static JSONObject question(JSONObject parent, Request request, String selectedId) {
        var options = new JSONArray();
        for (var item : options(parent,request,selectedId))
            options.put(new JSONObject().put("id",item.getString("id"))
                    .put("label",item.getString("label")).put("sourceId",item.getString("sourceId")));
        return state(parent,request,"clarify").put("choices",options).put("priorSelection",selectedId);
    }

    static String choice(JSONObject question, String objectId, String answer) {
        var options = question.getJSONArray("choices");
        var matches = new ArrayList<String>();
        String reply = normalize(answer).replaceFirst("^(?:use|choose|pick|move)\\s+","").replaceFirst("^the\\s+","");
        for (Object raw : options) {
            var o = (JSONObject)raw;
            String label = label(o);
            if ((!objectId.isEmpty() && objectId.equals(o.getString("id"))) ||
                    (objectId.isEmpty() && !reply.isEmpty() && (label.equals(reply) ||
                            label.equals(reply+" crate") || normalize(o.getString("sourceId")).equals(reply))))
                matches.add(o.getString("id"));
        }
        return matches.size()==1 ? matches.getFirst() : "";
    }

    static List<JSONObject> crates(JSONObject parent) {
        var all = new ArrayList<JSONObject>();
        for (Object raw : parent.getJSONArray("objects")) {
            var item = (JSONObject)raw;
            if (LoadingAisle.isCrate(item)) all.add(item);
        }
        return all;
    }

    private static String label(JSONObject item) { return normalize(item.getString("label")); }

    private static String normalize(String text) {
        return text.strip().toLowerCase(Locale.ROOT).replaceAll("[.!?]+$","").replaceAll("\\s+"," ");
    }

    static JSONObject state(JSONObject parent, Request request, String status) {
        return new JSONObject().put("status",status).put("request",request.original)
                .put("parentId",parent.getString("id")).put("parentSha256",parent.getString("sourceSha256"));
    }

    static JSONObject base(JSONObject parent, String objectId, Request request, JSONObject intent) {
        return LoadingAisle.plan(parent,objectId,request.canonical,request.distanceCm)
                .put("request",request.original).put("intent",intent);
    }

    private static JSONObject alternate(JSONObject parent, JSONObject direct, double lateral, JSONObject obstruction) {
        if (OFFSETS.stream().noneMatch(offset -> Math.abs(lateral)==offset))
            throw new IllegalArgumentException("The lateral offset is outside the approved alternate choices.");
        var plan = new JSONObject(direct.toString());
        LoadingAisle.adjust(parent,plan,lateral);
        return plan.put("alternate",new JSONObject().put("lateralM",lateral).put("direct",obstruction));
    }

    static JSONObject rebuild(JSONObject parent, JSONObject saved, JSONObject intent) {
        var plan = base(parent,saved.getString("objectId"),parse(saved.getString("request")),intent);
        if (saved.has("alternate")) {
            var alt = saved.getJSONObject("alternate");
            plan = alternate(parent,plan,alt.getDouble("lateralM"),alt.getJSONObject("direct"));
        }
        else if (saved.has("route")) plan = LoadingAisleRoute.rebuild(plan,saved.getJSONObject("route"));
        return plan.put("preview",saved.getJSONObject("preview"));
    }

    record Search(JSONObject plan, JSONObject refusal) {}

    static Search search(JSONObject parent, String objectId, Request request, JSONObject intent, Path nativeDir)
            throws Exception {
        var direct = base(parent,objectId,request,intent);
        try {
            return new Search(withPreview(parent,direct,nativeDir),null);
        } catch (LoadingAisle.Obstruction blocked) {
            var reason = blocked.evidence();
            int considered = 0, routeEdgesChecked = 0, fineEdgesChecked = 0;
            if (!blocked.gate.equals("initial-aisle")) {
                var n = v(direct.getJSONArray("directionM"));
                var entry = v(direct.getJSONObject("aisle").getJSONArray("entryM"));
                var start = v(direct.getJSONArray("fromM"));
                double outward = dot(sub(start,entry),new double[]{n[2],0,-n[0]}) < 0 ? -1 : 1;
                for (double offset : OFFSETS) {
                    for (double side : new double[]{outward,-outward}) {
                        considered++;
                        try {
                            return new Search(withPreview(parent,alternate(parent,direct,offset*side,reason),nativeDir),null);
                        } catch (LoadingAisle.Obstruction | LoadingAisle.FloorBounds rejected) {
                            // These candidates failed a measured gate or the floor footprint check.
                        }
                    }
                }
                var coarse = LoadingAisleRoute.search(parent,direct,reason,nativeDir,false);
                routeEdgesChecked = coarse.edgesChecked();
                boolean coarseFound = coarse.plan()!=null;
                if (coarseFound) {
                    try {
                        LoadingAisleRoute.recordSearch(coarse.plan(),routeEdgesChecked,0,"accepted");
                        return new Search(withPreview(parent,coarse.plan(),nativeDir),null);
                    } catch (LoadingAisle.Obstruction rejected) {
                        // A consolidated segment or the final supported destination failed verification.
                    }
                }
                var fine = LoadingAisleRoute.search(parent,direct,reason,nativeDir,true);
                fineEdgesChecked = fine.edgesChecked();
                if (fine.plan()!=null) {
                    try {
                        LoadingAisleRoute.recordSearch(fine.plan(),routeEdgesChecked,fineEdgesChecked,
                                coarseFound ? "verification-failed" : "no-route");
                        return new Search(withPreview(parent,fine.plan(),nativeDir),null);
                    } catch (LoadingAisle.Obstruction blockedRoute) {
                        reason = new JSONObject(reason.toString()).put("routeRefusal",blockedRoute.evidence());
                    }
                }
            }
            return new Search(null,state(parent,request,"blocked").put("objectId",objectId)
                    .put("crateLabel",direct.getString("crateLabel")).put("intent",intent).put("direct",reason)
                    .put("alternatesConsidered",considered).put("routeEdgesChecked",routeEdgesChecked)
                    .put("fineRouteEdgesChecked",fineEdgesChecked));
        }
    }

    private static JSONObject withPreview(JSONObject parent, JSONObject plan, Path nativeDir) throws Exception {
        var child = SpatialScene.parse(MapCheckpoint.canonical(LoadingAisle.revision(parent,plan)),parent.getString("id"));
        return plan.put("preview",LoadingAisle.measure(parent,child,plan,nativeDir));
    }

    static void checkPreview(JSONObject parent, JSONObject plan, JSONObject measured, Path nativeDir) throws Exception {
        if (!plan.has("intent")) return;
        if (!measured.similar(plan.getJSONObject("preview")))
            throw new IllegalStateException("The fresh native measurement differs from the reviewed preview.");
        var detour = plan.has("route") ? plan.optJSONObject("route") : plan.optJSONObject("alternate");
        if (detour != null) {
            var direct = base(parent,plan.getString("objectId"),parse(plan.getString("request")),plan.getJSONObject("intent"));
            try {
                withPreview(parent,direct,nativeDir);
            } catch (LoadingAisle.Obstruction obstruction) {
                if (obstruction.evidence().similar(detour.getJSONObject("direct"))) return;
                throw new IllegalStateException("The obstacle that justified the alternate changed.");
            }
            throw new IllegalStateException("The original path now passes. Review the direct move.");
        }
    }

    static JSONObject fixture(String variant) {
        if (LoadingAisleWideBays.isFixture(variant)) return LoadingAisleWideBays.fixture(variant);
        if (LoadingAisleRoute.isFixture(variant)) return LoadingAisleRoute.fixture(variant);
        int yaw = switch (variant) {
            case "crossdock" -> -29;
            case "quayside" -> 68;
            case "closed" -> 121;
            case "pinched" -> -73;
            default -> throw new IllegalArgumentException("Choose a known multi-crate loading bay.");
        };
        double angle = Math.toRadians(yaw);
        var rotation = new JSONArray(new double[]{0,Math.sin(angle/2),0,Math.cos(angle/2)});
        double length = variant.equals("quayside") ? 4.2 : 3.4;
        var origin = variant.equals("quayside") ? new double[]{2.8,0,-1.1} : new double[]{-1.6,0,.75};
        String title = switch (variant) {
            case "crossdock" -> "Cross-dock · −29°";
            case "quayside" -> "Quayside · 68°";
            case "closed" -> "Closed bay · 121°";
            default -> "Narrow bay · −73°";
        };
        var base = LoadingAisle.fixture(0,"none").put("title",title);
        var objects = base.getJSONArray("objects");
        LoadingAisle.find(base,"cargo").put("label","Blue crate").put("position",array(new double[]{1.08,.30,1.05}));
        LoadingAisle.find(base,"spare").put("label","Red crate").put("position",array(new double[]{-1.12,.26,2.22}))
                .put("size",array(new double[]{.54,.52,.52})).put("massKg",9);
        LoadingAisle.find(base,"shelf-east").put("position",array(new double[]{2.46,.75,1.42}));
        LoadingAisle.find(base,"shelf-west").put("position",array(new double[]{-2.45,.75,2.46}));
        LoadingAisle.find(base,"floor").put("size",array(new double[]{6.8,.2,6.4}));
        LoadingAisle.find(base,"aisle-end").put("position",array(new double[]{0,-.015,length}));
        objects.put(LoadingAisle.box("green","Green crate",new double[]{2.29,.23,3.65},
                new double[]{.47,.46,.46},11,new JSONArray(new double[]{0,0,0,1})));
        double[] trolley = switch (variant) {
            case "crossdock" -> new double[]{.96,.28,1.83};
            case "quayside" -> new double[]{-1.00,.28,2.94};
            case "pinched" -> new double[]{1.08,.28,1.83};
            default -> new double[]{0,.28,1.96};
        };
        objects.put(LoadingAisle.box("trolley","Parked trolley",trolley,new double[]{.30,.56,.38},0,
                new JSONArray(new double[]{0,0,0,1})));
        if (variant.equals("crossdock")) {
            // A fourth crate has the same label as green to exercise exact disambiguation.
            objects.put(LoadingAisle.box("green-extra","Green crate",new double[]{-2.65,.23,.75},
                    new double[]{.46,.46,.46},7,new JSONArray(new double[]{0,0,0,1})));
        }
        if (variant.equals("pinched")) {
            var upright = new JSONArray(new double[]{0,0,0,1});
            objects.put(LoadingAisle.box("inside-rail","Inside rail",new double[]{.62,.28,1.23},
                    new double[]{.10,.56,1.10},0,upright));
            objects.put(LoadingAisle.box("outside-rail","Outside rail",new double[]{1.58,.28,1.23},
                    new double[]{.12,.56,1.10},0,upright));
        }
        for (Object raw : objects) {
            var body = (JSONObject)raw;
            body.put("position",array(add(origin,rotate(v(body.getJSONArray("position")),rotation,false))))
                    .put("rotation",rotation);
        }
        return base;
    }

    private LoadingAisleIntent() {}
}
