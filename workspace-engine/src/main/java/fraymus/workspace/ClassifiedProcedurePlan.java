package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Routes checked classification rows to project-local, independently verified conversion lessons. */
final class ClassifiedProcedurePlan {
    private final Path directory;
    private final RequestWorkbench workbench;

    ClassifiedProcedurePlan(Path project, RequestWorkbench workbench) {
        this.directory = project.resolve("request-workbench").resolve("classification-routing");
        this.workbench = workbench;
    }

    synchronized JSONObject start(JSONObject input) throws Exception {
        SpatialScene.keys(input, "sourceId");
        var source = workbench.routingSource(input.getString("sourceId"));
        Files.createDirectories(directory);
        try (var files = Files.list(directory)) {
            if (files.filter(file -> file.getFileName().toString().endsWith(".json")).count() >= 64)
                throw new IllegalStateException("This project has 64 classification routing plans");
        }
        var plan = new JSONObject().put("id", UUID.randomUUID().toString()).put("source", source.binding())
                .put("results", new JSONArray());
        save(plan);
        return view(plan);
    }

    synchronized JSONObject attach(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "base64");
        var plan = load(input);
        if (plan.has("approved")) throw new IllegalStateException("A confirmed routing plan keeps its original mapping");
        String encoded = input.getString("base64");
        var decision = decide(source(plan), encoded);
        plan.put("mapping", encoded).put("decision", decision);
        save(plan);
        return view(plan);
    }

    synchronized JSONObject read(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        var plan = load(input);
        check(plan);
        return view(plan);
    }

    synchronized JSONObject export(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        var plan = load(input);
        var results = plan.getJSONArray("results");
        if (!plan.has("approved") || results.length() != stages(plan).length())
            throw new IllegalStateException("Complete and verify every routed procedure before exporting the CSV");
        var source = check(plan);
        var rows = plan.getJSONObject("decision").getJSONArray("rows");
        String csv = RoutedCsvReport.build(source.input().table().rows(), rows, stages(plan), results);
        var children = new JSONArray();
        for (int i = 0; i < results.length(); i++) {
            var child = results.getJSONObject(i);
            children.put(new JSONObject().put("reviewId", child.getString("childId")).put("artifact", child.getJSONObject("artifact")));
        }
        return new JSONObject().put("csv", csv).put("sha256", MeasurementStore.hash(csv))
                .put("planId", plan.getString("id")).put("decisionSha256", decisionHash(plan))
                .put("source", source.binding()).put("children", children).put("rows", rows.length());
    }

    synchronized JSONObject confirm(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "decisionSha256");
        var plan = load(input);
        check(plan);
        String hash = decisionHash(plan);
        if (!hash.equals(input.getString("decisionSha256")))
            throw new IllegalStateException("Review the current classification mapping before confirming");
        if (!plan.has("approved")) {
            plan.put("approved", hash);
            save(plan);
        }
        return view(plan);
    }

    synchronized JSONObject next(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        var plan = load(input);
        requireApproval(plan);
        check(plan);
        int index = plan.getJSONArray("results").length();
        var steps = stages(plan);
        var out = new JSONObject().put("plan", view(plan));
        if (index < steps.length()) {
            var step = steps.getJSONObject(index);
            out.put("child", workbench.routeChild(childId(plan, index), plan.getString("id"),
                    step.getJSONObject("lesson"), step.getString("csv")));
        }
        return out;
    }

    synchronized JSONObject accept(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "childId");
        var plan = load(input);
        requireApproval(plan);
        check(plan);
        var results = plan.getJSONArray("results");
        int index = results.length();
        String child = input.getString("childId");
        if (index > 0 && child.equals(childId(plan, index - 1))) return view(plan);
        if (index >= stages(plan).length() || !child.equals(childId(plan, index)))
            throw new IllegalStateException("Only the scheduled next procedure can be accepted");
        results.put(checkedResult(plan, index));
        save(plan);
        return view(plan);
    }

    private JSONObject checkedResult(JSONObject plan, int index) throws Exception {
        var step = stages(plan).getJSONObject(index);
        String child = childId(plan, index);
        return workbench.verifyRoutedChild(child, plan.getString("id"), step.getJSONObject("lesson"), step.getString("csv"))
                .put("childId", child);
    }

    private RequestWorkbench.RoutingSource check(JSONObject plan) throws Exception {
        var source = source(plan);
        if (!plan.has("decision")) return source;
        if (!decide(source, plan.getString("mapping")).similar(plan.getJSONObject("decision")))
            throw new IllegalStateException("The verified classification or routing decision changed");
        if (plan.has("approved")) requireApproval(plan);
        var results = plan.getJSONArray("results");
        for (int i = 0; i < results.length(); i++) {
            if (!checkedResult(plan, i).similar(results.getJSONObject(i)))
                throw new IllegalStateException("A completed routed result changed");
        }
        return source;
    }

    private RequestWorkbench.RoutingSource source(JSONObject plan) throws Exception {
        var ref = plan.getJSONObject("source");
        var source = workbench.routingSource(ref.getString("reviewId"));
        if (!source.binding().similar(ref)) throw new IllegalStateException("The verified classification source changed");
        return source;
    }

    private JSONObject decide(RequestWorkbench.RoutingSource source, String encoded) throws Exception {
        if (encoded.length() > 2732) throw new IllegalArgumentException("Choose a mapping CSV of up to 2 KiB");
        var bytes = Base64.getDecoder().decode(encoded);
        var csv = FrayDataTool.csvText(bytes);
        var mapping = MeasurementStore.csv(csv, 1);
        if (mapping.size() > 7 || !mapping.getFirst().equals(List.of("label", "lesson")))
            throw new IllegalArgumentException("Use a mapping CSV with label,lesson and one to six mappings");
        var labels = new HashSet<>(source.rules().ranges().stream().map(FrayRangeRules.Range::label).toList());
        var lessonNames = new LinkedHashMap<String, String>();
        var lessons = new LinkedHashMap<String, JSONObject>();
        for (var row : mapping.subList(1, mapping.size())) {
            if (row.size() != 2 || !labels.contains(row.getFirst()) || !row.getLast().matches(FrayDataTool.NAME)
                    || lessonNames.putIfAbsent(row.getFirst(), row.getLast()) != null)
                throw new IllegalArgumentException("Map each declared classification label to at most one named lesson");
            if (!lessons.containsKey(row.getLast())) lessons.put(row.getLast(), workbench.routingLesson(row.getLast()));
        }
        var table = source.input().table().rows();
        var headers = table.getFirst();
        var batches = new LinkedHashMap<String, StringBuilder>();
        var choices = new JSONArray();
        for (int i = 0; i < source.input().values().size(); i++) {
            String reading = source.input().values().get(i);
            var range = source.rules().match(reading);
            String lessonName = lessonNames.get(range.label());
            if (lessonName == null) throw new IllegalArgumentException("Missing a procedure mapping for " + range.label());
            var lesson = lessons.get(lessonName);
            String column = RequestWorkbench.routingHeader(lesson);
            int index = headers.indexOf(column);
            if (index < 0) throw new IllegalArgumentException("The lesson " + lessonName + " requires the source column " + column);
            String value = FrayDataTool.plainDecimal(table.get(i + 1).get(index));
            batches.computeIfAbsent(lessonName, unused -> new StringBuilder(column)).append('\n').append(value);
            choices.put(new JSONObject().put("row", i + 2).put("measurement", source.column()).put("reading", reading)
                    .put("min", FrayRangeRules.decimal(range.min())).put("max", FrayRangeRules.decimal(range.max()))
                    .put("label", range.label()).put("lesson", lessonName).put("inputColumn", column)
                    .put("unit", lesson.getString("column")).put("value", value));
        }
        var stages = new JSONArray();
        for (var batch : batches.entrySet()) {
            var lesson = lessons.get(batch.getKey());
            String data = batch.getValue().toString();
            RequestWorkbench.checkRoutingInput(lesson, data);
            stages.put(new JSONObject().put("name", batch.getKey()).put("lesson", lesson).put("csv", data));
        }
        return new JSONObject().put("mappingSha256", PerceptionStore.hash(bytes)).put("rows", choices).put("stages", stages);
    }

    private static void requireApproval(JSONObject plan) {
        if (!plan.has("approved") || !decisionHash(plan).equals(plan.getString("approved")))
            throw new IllegalStateException("Confirm the current mapping before any procedure can run");
    }

    private static String decisionHash(JSONObject plan) {
        if (!plan.has("decision")) throw new IllegalStateException("Attach a complete classification mapping first");
        return WorldFrames.hash(new JSONObject().put("id", plan.getString("id")).put("source", plan.getJSONObject("source"))
                .put("decision", plan.getJSONObject("decision")));
    }

    private static JSONArray stages(JSONObject plan) { return plan.getJSONObject("decision").getJSONArray("stages"); }

    private static String childId(JSONObject plan, int index) {
        return UUID.nameUUIDFromBytes(("classification-routing:" + plan.getString("id") + ":" + index)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static JSONObject view(JSONObject plan) {
        var results = plan.getJSONArray("results");
        var view = new JSONObject().put("id", plan.getString("id")).put("source", plan.getJSONObject("source"))
                .put("status", "awaiting-map");
        if (!plan.has("decision")) return view;
        var stages = stages(plan);
        String status = plan.has("approved") ? results.length() == stages.length() ? "verified" : "approved" : "review";
        var steps = new JSONArray();
        for (int i = 0; i < stages.length(); i++) {
            var step = new JSONObject().put("name", stages.getJSONObject(i).getString("name"));
            if (i < results.length()) step.put("result", results.getJSONObject(i));
            steps.put(step);
        }
        return view.put("status", status).put("decisionSha256", decisionHash(plan))
                .put("mappingSha256", plan.getJSONObject("decision").getString("mappingSha256"))
                .put("rows", plan.getJSONObject("decision").getJSONArray("rows")).put("steps", steps);
    }

    private JSONObject load(JSONObject input) throws Exception {
        String id = input.getString("id");
        Path file = path(id);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("Unknown classification routing plan");
        var data = AgentFiles042.loadChecked(file);
        if (!id.equals(data.getString("id"))) throw new IllegalStateException("Routing plan identity changed");
        return data;
    }

    private Path path(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Expected a routing plan ID");
        return directory.resolve(id + ".json");
    }

    private void save(JSONObject plan) throws Exception { AgentFiles042.saveChecked(path(plan.getString("id")), plan); }
}
