package factory;

import com.google.gson.*;
import fraymus.factory.*;
import jade.GameObject;
import scenes.Scene;
import physics2d.components.*;
import java.time.Instant;
import java.util.*;

/** Immutable observations captured on the scene thread, safe to query during local inference. */
public record SceneKnowledge(WorldSnapshot world, String fingerprint, String json, SpatialIndex2D spatial) {
    public static boolean authored(EntityRecord entity) { return entity.components().stream().anyMatch(c -> c.type().equals("editor.object")); }
    public static WorldSnapshot authoredWorld(WorldSnapshot world) { return new WorldSnapshot(world.revision(), world.entities().stream().filter(SceneKnowledge::authored).toList()); }
    public static String fingerprint(WorldSnapshot world) { return SceneJson.hash(authoredWorld(world).entities()); }
    public static SceneKnowledge capture(Scene scene, WorldSnapshot world, UUID selected, boolean playing) {
        return capture(scene, world, selected, playing, 0);
    }
    public static SceneKnowledge capture(Scene scene, WorldSnapshot world, UUID selected, boolean playing, int spriteCount) {
        var document = new JsonObject();
        String fingerprint = fingerprint(world);
        document.addProperty("observationId", fingerprint.substring(0, 16)); document.addProperty("sceneHash", fingerprint);
        document.addProperty("observedAt", Instant.now().toString()); document.addProperty("mode", playing ? "simulation" : "editing");
        document.addProperty("selectedEntityId", selected == null ? "" : selected.toString());
        document.addProperty("coordinateConvention", "World units; +x right, +y up; rotations in degrees; transform scale is sprite size. Box2DCollider.halfSize is the full box size in this legacy editor.");
        document.addProperty("componentVisibility", "All serialized authored components are included. Runtime physics values are separate. Editor helpers and renderer internals are not authored entities.");
        var grid = new JsonObject(); grid.addProperty("width", util.Settings.GRID_WIDTH); grid.addProperty("height", util.Settings.GRID_HEIGHT);
        grid.addProperty("snapEnabled", util.Settings.SNAP_TO_GRID); grid.addProperty("visible", util.Settings.SHOW_GRID); document.add("grid", grid);
        if (scene.camera() != null) {
            var camera = scene.camera(); var view = new JsonObject();
            view.addProperty("x", camera.position.x); view.addProperty("y", camera.position.y); view.addProperty("zoom", camera.getZoom());
            view.addProperty("width", camera.getProjectionSize().x * camera.getZoom()); view.addProperty("height", camera.getProjectionSize().y * camera.getZoom()); document.add("camera", view);
        }
        var entities = new JsonArray(); var items = new ArrayList<SpatialIndex2D.Item>();
        for (var entity : world.entities()) if (authored(entity)) {
            JsonObject data = describe(entity); var bounds = bounds(entity); data.add("bounds", SceneJson.GSON.toJsonTree(bounds));
            items.add(new SpatialIndex2D.Item(entity.id(), bounds));
            scene.getGameObjects().stream().filter(o -> o.getPersistentId().equals(entity.id())).findFirst().ifPresent(object -> {
                var rb = object.getComponent(Rigidbody2D.class);
                if (rb != null && rb.getRawBody() != null) {
                    var body = rb.getRawBody(); var runtime = new JsonObject();
                    runtime.addProperty("x", body.getPosition().x); runtime.addProperty("y", body.getPosition().y);
                    runtime.addProperty("velocityX", body.getLinearVelocity().x); runtime.addProperty("velocityY", body.getLinearVelocity().y);
                    runtime.addProperty("angleDegrees", Math.toDegrees(body.getAngle())); runtime.addProperty("mass", body.getMass());
                    runtime.addProperty("awake", body.isAwake()); runtime.addProperty("type", body.getType().name()); data.add("runtimePhysics", runtime);
                }
            });
            entities.add(data);
        }
        document.add("entities", entities); document.addProperty("entityCount", entities.size()); document.addProperty("physicsBodies", scene.physics().bodyCount());
        document.add("physicsSettings", SceneJson.GSON.toJsonTree(scene.physics().settings()));
        document.addProperty("boundsMeaning", "Axis-aligned broad-phase bounds, including rotated collider origins and offsets. An AABB overlap is not an exact collision result.");
        document.add("dataEntities", SceneJson.GSON.toJsonTree(world.entities().stream().filter(e -> !authored(e)).toList()));
        var assets = new JsonObject(); assets.addProperty("sheet", "decorationsAndBlocks"); assets.addProperty("spriteCount", spriteCount); assets.addProperty("defaultSize", .25);
        document.add("assets", assets);
        return new SceneKnowledge(world, fingerprint, document.toString(), new SpatialIndex2D(items));
    }
    public static JsonObject describe(EntityRecord entity) {
        var result = new JsonObject(); result.addProperty("entityId", entity.id().toString()); result.addProperty("name", entity.name()); var components = new JsonArray();
        for (var component : entity.components()) {
            if (component.type().equals("editor.object")) continue;
            var data = new JsonObject(); data.addProperty("componentId", component.id().toString()); data.addProperty("type", component.type().substring("editor.".length()));
            data.add("properties", JsonParser.parseString(component.values().get("json"))); components.add(data);
        }
        result.add("components", components); return result;
    }
    public static ComponentRecord component(EntityRecord entity, String type) {
        return entity.components().stream().filter(c -> c.type().equals("editor." + type)).findFirst().orElseThrow(() -> new IllegalArgumentException("Missing " + type + " on " + entity.id()));
    }
    public static SpatialIndex2D.Bounds bounds(EntityRecord entity) {
        var transform = component(entity, "jade.Transform").values();
        double x = Double.parseDouble(transform.get("x")), y = Double.parseDouble(transform.get("y")), angle = Math.toRadians(Double.parseDouble(transform.get("rotation")));
        double width = Math.abs(Double.parseDouble(transform.get("scaleX"))), height = Math.abs(Double.parseDouble(transform.get("scaleY"))), localX = 0, localY = 0;
        var collider = entity.components().stream().filter(c -> c.type().endsWith(".Box2DCollider") || c.type().endsWith(".CircleCollider")).findFirst();
        boolean circle = false;
        if (collider.isPresent()) {
            var data = JsonParser.parseString(collider.get().values().get("json")).getAsJsonObject();
            if (data.has("offset")) { localX = data.getAsJsonObject("offset").get("x").getAsDouble(); localY = data.getAsJsonObject("offset").get("y").getAsDouble(); }
            if (collider.get().type().endsWith(".Box2DCollider")) {
                width = data.getAsJsonObject("halfSize").get("x").getAsDouble(); height = data.getAsJsonObject("halfSize").get("y").getAsDouble();
                localX += data.getAsJsonObject("origin").get("x").getAsDouble(); localY += data.getAsJsonObject("origin").get("y").getAsDouble();
            } else { circle = true; width = height = 2 * data.get("radius").getAsDouble(); }
        }
        double cos = Math.cos(angle), sin = Math.sin(angle), cx = x + localX * cos - localY * sin, cy = y + localX * sin + localY * cos;
        double extentX = circle ? width / 2 : (Math.abs(cos) * width + Math.abs(sin) * height) / 2;
        double extentY = circle ? height / 2 : (Math.abs(sin) * width + Math.abs(cos) * height) / 2;
        return new SpatialIndex2D.Bounds(cx - extentX, cy - extentY, cx + extentX, cy + extentY);
    }
    /** Small, valid JSON view for a model; the complete frozen world remains queryable. */
    public String forModel(int maxCharacters) {
        int budget=Math.max(2048,maxCharacters);JsonObject full=SceneJson.parse(json),summary=new JsonObject();
        for(String key:List.of("observationId","sceneHash","mode","selectedEntityId","coordinateConvention","grid","camera"))
            if(full.has(key)) summary.add(key,full.get(key));
        var authored=world.entities().stream().filter(SceneKnowledge::authored).toList();
        summary.addProperty("totalEntities",authored.size());
        summary.addProperty("detailPolicy","Bounded summaries. Use nearest (limit <= 8) or inspect with optional componentType/property to read frozen details.");
        String selected=full.has("selectedEntityId")?full.get("selectedEntityId").getAsString():"";
        double centerX=0,centerY=0;
        if(full.has("camera")) {var camera=full.getAsJsonObject("camera");centerX=camera.get("x").getAsDouble()+camera.get("width").getAsDouble()/2;centerY=camera.get("y").getAsDouble()+camera.get("height").getAsDouble()/2;}
        final double cx=centerX,cy=centerY;
        var ordered=authored.stream().sorted(Comparator.<EntityRecord>comparingInt(e->e.id().toString().equals(selected)?0:1)
            .thenComparingDouble(e->{var b=bounds(e);return Math.hypot((b.minX()+b.maxX())/2-cx,(b.minY()+b.maxY())/2-cy);})).toList();
        JsonArray entities=new JsonArray();summary.add("entities",entities);
        for(var entity:ordered) {
            JsonObject item=compact(entity);entities.add(item);
            if(summary.toString().length()>budget-160) {entities.remove(entities.size()-1);break;}
        }
        summary.addProperty("returnedEntities",entities.size());summary.addProperty("partial",entities.size()!=authored.size());return summary.toString();
    }
    private static JsonObject compact(EntityRecord entity) {
        var item=new JsonObject();item.addProperty("entityId",entity.id().toString());item.addProperty("name",entity.name());
        item.add("transform",SceneJson.GSON.toJsonTree(component(entity,"jade.Transform").values().entrySet().stream().filter(e->!e.getKey().equals("json")).collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))));
        item.add("componentTypes",SceneJson.GSON.toJsonTree(entity.components().stream().filter(c->!c.type().equals("editor.object")).map(c->c.type().substring(7)).toList()));return item;
    }
    public String queryForModel(String source,int maxCharacters) {
        String result=query(source);if(result.length()<=maxCharacters) return result;
        JsonObject data=SceneJson.parse(result);JsonObject limited=new JsonObject();limited.addProperty("op",data.get("op").getAsString());
        limited.addProperty("observationId",fingerprint.substring(0,16));limited.addProperty("partial",true);
        limited.addProperty("hint","Result exceeds model budget. Query a specific entity, componentType and property, or a smaller nearest limit.");
        for(String key:List.of("entities","hits")) if(data.has(key)) {
            var sourceArray=data.getAsJsonArray(key);var returned=new JsonArray();limited.addProperty("totalMatches",sourceArray.size());limited.add(key,returned);
            for(var item:sourceArray) {
                JsonElement candidate=item;
                if(item.isJsonObject()&&item.getAsJsonObject().has("entityId")) candidate=compact(require(item.getAsJsonObject().get("entityId").getAsString()));
                returned.add(candidate);if(limited.toString().length()>maxCharacters-100) {returned.remove(returned.size()-1);break;}
            }
        }
        if(data.has("entity")) limited.add("entity",compact(require(data.getAsJsonObject("entity").get("entityId").getAsString())));
        return limited.toString();
    }
    public String query(String source) {
        var request = SceneJson.parse(source); String op = SceneJson.text(request, "op"); var result = new JsonObject(); result.addProperty("op", op);
        switch (op) {
            case "inspect" -> {
                SceneJson.keys(request, "op", "entityId","componentType","property");
                JsonObject entity=describe(require(SceneJson.text(request,"entityId")));
                if(request.has("componentType")) {
                    JsonArray matching=new JsonArray();
                    for(var item:entity.getAsJsonArray("components")) if(item.getAsJsonObject().get("type").getAsString().equals(SceneJson.text(request,"componentType"))) {
                        var component=item.getAsJsonObject();
                        if(request.has("property")) {
                            String field=SceneJson.text(request,"property");var properties=component.getAsJsonObject("properties");
                            if(!properties.has(field)) throw new IllegalArgumentException("Unknown component property: "+field);
                            var chosen=new JsonObject();chosen.add(field,properties.get(field));component.add("properties",chosen);
                        }
                        matching.add(component);
                    }
                    if(matching.size()==0) throw new IllegalArgumentException("Component is not present on this entity");entity.add("components",matching);
                } else if(request.has("property")) throw new IllegalArgumentException("Select componentType before property");
                result.add("entity",entity);
            }
            case "components" -> {
                SceneJson.keys(request, "op", "type"); String type = SceneJson.text(request, "type"); var array = new JsonArray();
                world.entities().stream().filter(SceneKnowledge::authored).filter(e -> e.components().stream().anyMatch(c -> c.type().equals("editor." + type))).forEach(e -> array.add(describe(e)));
                result.add("entities", array);
            }
            case "nearest" -> {
                SceneJson.keys(request, "op", "x", "y", "radius", "limit");
                double requested = request.has("limit") ? SceneJson.number(request, "limit") : Math.max(1, spatial.items().size());
                if (requested != Math.rint(requested) || requested > Integer.MAX_VALUE) throw new IllegalArgumentException("Limit must be a positive integer");
                result.add("hits", SceneJson.GSON.toJsonTree(spatial.nearest(SceneJson.number(request, "x"), SceneJson.number(request, "y"), SceneJson.number(request, "radius"), (int) requested)));
            }
            case "overlap" -> {
                SceneJson.keys(request, "op", "minX", "minY", "maxX", "maxY");
                result.add("hits", SceneJson.GSON.toJsonTree(spatial.overlap(new SpatialIndex2D.Bounds(SceneJson.number(request, "minX"), SceneJson.number(request, "minY"), SceneJson.number(request, "maxX"), SceneJson.number(request, "maxY")))));
            }
            case "relative" -> {
                SceneJson.keys(request, "op", "entityId", "otherId"); var a = bounds(require(SceneJson.text(request, "entityId"))); var b = bounds(require(SceneJson.text(request, "otherId")));
                double dx = (b.minX() + b.maxX() - a.minX() - a.maxX()) / 2, dy = (b.minY() + b.maxY() - a.minY() - a.maxY()) / 2;
                result.addProperty("deltaX", dx); result.addProperty("deltaY", dy); result.addProperty("centerDistance", Math.hypot(dx, dy));
                result.addProperty("aabbOverlap", a.intersects(b)); result.addProperty("entirelyLeft", a.maxX() < b.minX()); result.addProperty("entirelyBelow", a.maxY() < b.minY());
            }
            default -> throw new IllegalArgumentException("Unknown spatial query: " + op);
        }
        result.addProperty("observationId", fingerprint.substring(0, 16)); return result.toString();
    }
    private EntityRecord require(String id) { return world.entities().stream().filter(SceneKnowledge::authored).filter(e -> e.id().equals(UUID.fromString(id))).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown scene entity: " + id)); }
}
