package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/** Stores exact renderer tick packets and verifies their revision binding on every read. */
final class TilePlayRecords {
    static final int LIMIT = 7200;
    private static final List<String> FIELDS = List.of("tick", "x", "y", "vx", "vy", "support", "wall",
            "collectedHex", "won", "direction", "facing", "animationTimeS", "resets", "bounces", "spring",
            "score", "jumpPressed", "events", "inputDirection");
    private final Path root;

    TilePlayRecords(Path project) {
        root = project.resolve("media").resolve("tile-playtests");
    }

    synchronized JSONObject save(JSONObject input, JSONObject layout, boolean publish) throws IOException {
        SpatialScene.keys(input, "levelId", "reason", "rows", "rendererSha256");
        String levelId = input.getString("levelId");
        String renderer = input.getString("rendererSha256");
        if (!renderer.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("The record needs its tile renderer hash.");
        String reason = input.getString("reason");
        if (!List.of("won", "stopped", "limit").contains(reason)) throw new IllegalArgumentException("Invalid playtest ending.");
        JSONArray rows = input.getJSONArray("rows");
        if (rows.isEmpty() || rows.length() > LIMIT + 1 || reason.equals("limit") && rows.length() != LIMIT + 1)
            throw new IllegalArgumentException("The playtest exceeded its tick limit.");
        var entityIds = new HashSet<String>();
        var rules = layout.optJSONObject("scoring", new JSONObject());
        int pickupPoints = rules.optInt("pickup", 100);
        int goalPoints = rules.optInt("goal", 500);
        int resetPenalty = rules.optInt("resetPenalty", 25);
        var pickups = new HashMap<String, Integer>();
        var springs = new HashSet<String>();
        var hazards = new HashSet<String>();
        for (Object item : layout.getJSONArray("objects")) {
            var object = (JSONObject) item;
            String id = object.getString("id");
            entityIds.add(id);
            if (object.getString("layer").equals("pickup")) pickups.put(id, object.optInt("points", pickupPoints));
            if (object.optString("effect").equals("spring")) springs.add(id);
            if (object.optString("effect").equals("hazard")) hazards.add(id);
        }
        var awarded = new HashSet<String>();
        int maskLength = (pickups.size() + 3) / 4;
        int previousScore = 0, goalEvents = 0;
        for (int tick = 0; tick < rows.length(); tick++) {
            var row = rows.getJSONArray(tick);
            if (row.length() != FIELDS.size() || whole(row, 0, 0, LIMIT) != tick)
                throw new IllegalArgumentException("The playtest tick sequence has a missing or duplicate row.");
            for (int field : new int[]{1, 2, 3, 4, 11}) finite(row, field);
            for (int field : new int[]{5, 6, 14}) {
                String reference = row.getString(field);
                if (!reference.isEmpty() && !reference.equals("floor") && !entityIds.contains(reference))
                    throw new IllegalArgumentException("The tick refers to an unknown level object.");
            }
            String mask = row.getString(7);
            if (mask.length() != maskLength || !mask.matches("[0-9a-f]*"))
                throw new IllegalArgumentException("The tick pickup mask differs from the level.");
            if (!(row.get(8) instanceof Boolean) || !(row.get(16) instanceof Boolean))
                throw new IllegalArgumentException("The tick flags must be booleans.");
            if (row.getBoolean(8) && (tick != rows.length() - 1 || !reason.equals("won")))
                throw new IllegalArgumentException("A completed playtest must end at the winning tick.");
            whole(row, 9, -1, 1);
            whole(row, 18, -1, 1);
            if (whole(row, 10, -1, 1) == 0) throw new IllegalArgumentException("Invalid player facing.");
            whole(row, 12, 0, LIMIT);
            whole(row, 13, 0, LIMIT);
            int score = whole(row, 15, 0, 10_000_000);
            var events = row.getJSONArray(17);
            if (events.length() > 400) throw new IllegalArgumentException("Too many events at one tick.");
            int fromEvents = previousScore;
            for (Object entry : events) {
                var event = (JSONArray) entry;
                String type = event.getString(0), object = event.getString(1);
                if (event.length() != 3 || !List.of("jump", "spring", "pickup", "reset", "goal").contains(type)
                        || !object.isEmpty() && !entityIds.contains(object))
                    throw new IllegalArgumentException("The tick contains an invalid event.");
                int expected = switch (type) {
                    case "pickup" -> {
                        if (!pickups.containsKey(object)) throw new IllegalArgumentException("A pickup event names a different tile role.");
                        yield awarded.add(object) ? pickups.get(object) : 0;
                    }
                    case "reset" -> {
                        if (!object.isEmpty() && !hazards.contains(object)) throw new IllegalArgumentException("A reset names a different tile role.");
                        yield -Math.min(fromEvents, resetPenalty);
                    }
                    case "spring" -> {
                        if (!springs.contains(object)) throw new IllegalArgumentException("A boost names a different tile role.");
                        yield 0;
                    }
                    case "goal" -> {
                        if (!row.getBoolean(8) || !object.isEmpty()) throw new IllegalArgumentException("Goal points require the final winning tick.");
                        goalEvents++;
                        yield goalPoints;
                    }
                    default -> 0;
                };
                if (whole(event, 2, -10_000, 10_000) != expected)
                    throw new IllegalArgumentException("An event score differs from the level rules.");
                fromEvents += expected;
            }
            if (score != fromEvents) throw new IllegalArgumentException("The score differs from the recorded event deltas.");
            previousScore = score;
        }
        if (rows.getJSONArray(rows.length()-1).getBoolean(8) != reason.equals("won"))
            throw new IllegalArgumentException("The ending differs from the final tick.");
        if (goalEvents != (reason.equals("won") ? 1 : 0))
            throw new IllegalArgumentException("The playtest must record one goal event when it finishes.");
        var data = new JSONObject().put("format", "fraynix.tile-playtest-v2").put("levelId", levelId).put("reason", reason)
                .put("rendererSha256", renderer)
                .put("hz", 120).put("fields", new JSONArray(FIELDS)).put("rows", rows);
        String id = WorldFrames.hash(data);
        Path file = file(id);
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) checked(id);
        else AgentFiles042.saveChecked(file, data);
        if (publish) SpriteWorkshop.replace(root.resolve("latest"), id.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        return summary(id, data);
    }

    synchronized JSONObject read(JSONObject input, boolean all) throws IOException {
        SpatialScene.keys(input, "runId", "start", "limit");
        String id = input.optString("runId", "");
        if (id.isEmpty()) {
            Path latest = root.resolve("latest");
            if (!Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
            id = new String(SpriteWorkshop.read(latest, 64), java.nio.charset.StandardCharsets.US_ASCII);
        }
        var data = checked(id);
        var result = summary(id, data).put("fields", data.getJSONArray("fields"));
        var rows = data.getJSONArray("rows");
        if (all) return result.put("rows", rows);
        int start = input.has("start") ? SpriteWorkshop.integer(input, "start", 0, LIMIT) : 0;
        int limit = input.has("limit") ? SpriteWorkshop.integer(input, "limit", 1, 60) : 12;
        var page = new JSONArray();
        for (int i = start; i < Math.min(rows.length(), start + limit); i++) page.put(rows.getJSONArray(i));
        return result.put("rows", page).put("start", start).put("next", start + page.length() < rows.length()
                ? start + page.length() : JSONObject.NULL);
    }

    synchronized JSONObject compare(JSONObject input, boolean save) throws IOException {
        if (save) {
            SpatialScene.keys(input, "sourceId", "variantId", "jumpTick", "replacementTick");
            var receipt = comparison(input);
            String id = WorldFrames.hash(receipt);
            var target = root.resolve("comparison-" + id + ".json");
            AgentFiles042.saveChecked(target, receipt);
            SpriteWorkshop.replace(root.resolve("latest-comparison"), id.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return receipt.put("id", id);
        }
        SpatialScene.keys(input, "id");
        String id = input.optString("id", "");
        if (id.isEmpty()) {
            Path pointer = root.resolve("latest-comparison");
            if (!Files.exists(pointer, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
            id = new String(SpriteWorkshop.read(pointer, 64), java.nio.charset.StandardCharsets.US_ASCII);
        }
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid playtest comparison ID.");
        var receipt = AgentFiles042.loadChecked(root.resolve("comparison-" + id + ".json"));
        if (!id.equals(WorldFrames.hash(receipt)) || !comparison(receipt).similar(receipt))
            throw new IOException("The playtest comparison changed.");
        return receipt.put("id", id);
    }

    private JSONObject comparison(JSONObject input) throws IOException {
        String sourceId = input.getString("sourceId"), variantId = input.getString("variantId");
        var source = checked(sourceId);
        var variant = checked(variantId);
        String renderer = source.getString("rendererSha256");
        String levelId = source.getString("levelId");
        if (!source.getString("format").equals("fraynix.tile-playtest-v2") ||
                !variant.getString("format").equals("fraynix.tile-playtest-v2") ||
                !renderer.equals(variant.getString("rendererSha256")) || !levelId.equals(variant.getString("levelId")))
            throw new IllegalArgumentException("Use the same saved level and input-recording renderer for both runs.");
        var original = source.getJSONArray("rows");
        var changed = variant.getJSONArray("rows");
        int last = original.length() - 1;
        int jump = SpriteWorkshop.integer(input, "jumpTick", 1, last);
        int replacement = SpriteWorkshop.integer(input, "replacementTick", 0, last);
        if (!original.getJSONArray(jump).getBoolean(16) || replacement > 0 &&
                (Math.abs(jump - replacement) > 30 || original.getJSONArray(replacement).getBoolean(16)))
            throw new IllegalArgumentException("Choose a recorded jump and a free replacement tick within 30 steps.");
        if (changed.length() > original.length() || changed.length() < original.length() && !variant.getString("reason").equals("won"))
            throw new IllegalArgumentException("The variant must use the original input window or reach the goal sooner.");
        int divergence = -1;
        for (int tick = 0; tick < changed.length(); tick++) {
            var baseline = original.getJSONArray(tick);
            var test = changed.getJSONArray(tick);
            boolean expected = tick == replacement && replacement > 0 || tick != jump && baseline.getBoolean(16);
            if (baseline.getInt(18) != test.getInt(18) || expected != test.getBoolean(16))
                throw new IllegalArgumentException("The variant changed input beyond the reviewed jump.");
            if (divergence < 0 && !sameOutcome(baseline, test)) divergence = tick;
        }
        return new JSONObject().put("format", "fraynix.tile-replay-comparison-v1")
                .put("sourceId", sourceId).put("variantId", variantId).put("levelId", levelId).put("rendererSha256", renderer)
                .put("jumpTick", jump).put("replacementTick", replacement).put("firstOutcomeDifference", divergence)
                .put("source", outcome(sourceId, source)).put("variant", outcome(variantId, variant));
    }

    private static boolean sameOutcome(JSONArray source, JSONArray variant) {
        for (int i = 1; i <= 15; i++) if (!java.util.Objects.equals(source.get(i), variant.get(i))) return false;
        return source.getJSONArray(17).similar(variant.getJSONArray(17));
    }

    private static JSONObject outcome(String id, JSONObject data) {
        var result = summary(id, data);
        var finalRow = data.getJSONArray("rows").getJSONArray(result.getInt("lastTick"));
        return result.put("won", finalRow.getBoolean(8)).put("resets", finalRow.getInt(12))
                .put("bounces", finalRow.getInt(13)).put("x", finalRow.get(1)).put("y", finalRow.get(2));
    }

    private JSONObject checked(String id) throws IOException {
        var data = AgentFiles042.loadChecked(file(id));
        if (!id.equals(WorldFrames.hash(data)) || !List.of("fraynix.tile-playtest-v1", "fraynix.tile-playtest-v2").contains(data.getString("format")))
            throw new IOException("The retained playtest changed.");
        return data;
    }

    private Path file(String id) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid playtest ID.");
        return root.resolve(id + ".json");
    }

    private static JSONObject summary(String id, JSONObject data) {
        var rows = data.getJSONArray("rows");
        var jumps = new JSONArray();
        for (int i = 1; i < rows.length() && jumps.length() < 100; i++)
            if (rows.getJSONArray(i).getBoolean(16)) jumps.put(i);
        return new JSONObject().put("id", id).put("format", data.getString("format")).put("levelId", data.getString("levelId"))
                .put("jumpTicks", jumps)
                .put("reason", data.getString("reason")).put("hz", data.getInt("hz")).put("rendererSha256", data.getString("rendererSha256"))
                .put("tickCount", rows.length()).put("lastTick", rows.length() - 1)
                .put("score", rows.getJSONArray(rows.length() - 1).getInt(15));
    }

    private static int whole(JSONArray row, int field, int min, int max) {
        double value = finite(row, field);
        if (value != Math.rint(value) || value < min || value > max) throw new IllegalArgumentException("Invalid playtest integer.");
        return (int) value;
    }

    private static double finite(JSONArray row, int field) {
        if (!(row.get(field) instanceof Number number) || !Double.isFinite(number.doubleValue()))
            throw new IllegalArgumentException("The playtest contains an invalid numeric measurement.");
        return number.doubleValue();
    }
}
