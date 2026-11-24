package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** A chart request keeps the choices it originally showed, the exact source, and the browser receipt. */
final class ReferenceChart {
    static final String SCHEMA = "verified-chart-followup-v1";
    static final String PREVIEW = "/reference-chart-preview/";
    private static final Pattern REQUEST = Pattern.compile(
            "(?:(?:can|could|would) you )?(?:please )?(?:(?:(?:make|draw|build|show)(?: me)?) (?:a |the )?(?:chart|plot) (?:of|from|for) |(?:chart|plot) )"
                    + "(?:that|this|it|the verified result)(?:,? please)?[.!?]?", Pattern.CASE_INSENSITIVE);

    private record Prepared(String id, String title, ChartProjection.Page page) {
        String path() { return PREVIEW + id + "/" + page.hash() + "/index.html"; }
    }

    private final Path checkpoint;
    private final WorkspaceStore store;
    private final MeasurementStore measurements;
    private final Agency043 agency;
    private Prepared preview;

    ReferenceChart(Path project, WorkspaceStore store, MeasurementStore measurements, Agency043 agency) {
        checkpoint = project.resolve("chart-followup").resolve("current.json");
        this.store = store;
        this.measurements = measurements;
        this.agency = agency;
    }

    static JSONObject candidate(JSONObject task) {
        if (!"completed".equals(task.optString("status")) || !"verified".equals(task.optString("outcome"))) return null;
        var completion = task.optJSONObject("completion044", new JSONObject());
        var workflow = completion.optJSONObject("javaWorkflow", new JSONObject());
        var ramp = completion.optJSONObject("rampMission", new JSONObject());
        String goal = task.getString("goal");
        if (goal.length() > 300) goal = goal.substring(0, 299) + "…";
        var result = new JSONObject().put("taskId", task.getString("id")).put("goal", goal)
                .put("finishedAt", task.optString("finishedAt", task.getString("createdAt")));
        if ("workflow".equals(task.optString("family044")) && DataSummary104.summaryKind(workflow.optString("profile101"))) {
            var source = workflow.getJSONObject("dataBinding");
            return result.put("kind", "motion").put("label", source.getString("title"))
                    .put("source", source).put("report", workflow.getJSONObject("report"))
                    .put("identity", "motion:" + source.getString("datasetId") + ":" + source.getInt("datasetVersion"));
        }
        if (RampMission.FAMILY.equals(task.optString("family044")) && RampProcedure.COMPARE.equals(ramp.optString("procedure"))) {
            var source = ramp.getJSONObject("source");
            return result.put("kind", "ramp").put("label", "Ramp comparison")
                    .put("source", source).put("report", ramp.getJSONObject("report"))
                    .put("identity", "ramp:" + source.getString("id") + ":" + source.getInt("version"));
        }
        return null;
    }

    synchronized JSONObject begin(JSONObject input) throws Exception {
        SpatialScene.keys(input, "request");
        String request = input.getString("request").strip();
        if (request.length() > 1200 || !REQUEST.matcher(request.replaceAll("[ \t]+", " ")).matches()) {
            return new JSONObject().put("matched", false);
        }
        var previous = current();
        if (previous != null && !previous.has("result")) return view(previous).put("matched", true);

        var seen = new HashSet<String>();
        var choices = new JSONArray();
        agency.chartCandidates().stream()
                .sorted(Comparator.comparing((JSONObject row) -> Instant.parse(row.getString("finishedAt"))).reversed())
                .filter(row -> seen.add(row.getString("identity"))).forEach(choices::put);
        if (choices.isEmpty()) return new JSONObject().put("matched", true).put("status", "missing");
        var next = new JSONObject().put("schema", SCHEMA).put("id", UUID.randomUUID().toString())
                .put("request", request).put("choices", choices);
        next.put("decisionSha256", WorldFrames.hash(next));
        if (choices.length() == 1) next.put("selected", choices.getJSONObject(0).getString("taskId"));
        save(next);
        return view(next).put("matched", true);
    }

    synchronized JSONObject state(JSONObject input) throws Exception {
        SpatialScene.keys(input);
        var state = current();
        return state == null ? new JSONObject().put("status", "none") : view(state);
    }

    synchronized JSONObject choose(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "decisionSha256", "taskId");
        var state = checked(input);
        String taskId = input.getString("taskId");
        if (state.has("selected") && !taskId.equals(state.getString("selected"))) {
            throw new IllegalStateException("This request is already bound to a different source.");
        }
        choice(state, taskId);
        if (!state.has("selected")) {
            state.put("selected", taskId);
            save(state);
        }
        return view(state);
    }

    synchronized JSONObject discard(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "decisionSha256");
        var state = checked(input);
        if (state.has("result")) throw new IllegalStateException("The verified chart is already saved.");
        Files.delete(checkpoint);
        preview = null;
        return new JSONObject().put("status", "none");
    }

    synchronized JSONObject prepare(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "decisionSha256");
        var state = checked(input);
        var document = rebuild(state);
        if (state.has("result")) return verify(state, document);
        preview = document;
        return new JSONObject().put("preview", document.path()).put("htmlSha256", document.page.hash())
                .put("checks", document.page.checks());
    }

    synchronized byte[] preview(String path) {
        if (preview == null || !preview.path().equals(path)) return null;
        return preview.page.html().getBytes(StandardCharsets.UTF_8);
    }

    synchronized JSONObject finish(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "decisionSha256", "htmlSha256", "checks", "plot");
        var state = checked(input);
        var document = rebuild(state);
        var page = document.page;
        if (!page.hash().equals(input.getString("htmlSha256")) || !page.plot().similar(input.getJSONObject("plot"))) {
            throw new IllegalStateException("The browser chart differs from the verified source projection.");
        }
        if (state.has("result")) return verify(state, document);
        if (preview == null || !preview.path().equals(document.path())) {
            throw new IllegalStateException("Open the current chart preview before saving browser evidence.");
        }
        var test = new JSONObject().put("id", artifactId(document.id())).put("version", 1)
                .put("steps", page.checks()).put("checks", input.getJSONArray("checks"));
        if (!RampMissionReport.verifiedReceipt(test, WorldFrames.copy(test).put("passed", true), page.checks())) {
            throw new IllegalStateException("The chart needs all of its browser checks before it can be saved.");
        }
        var artifact = store.writeOnce(key(document.id()), page.artifact(document.title));
        var ref = Creation.reference(artifact);
        if (ref.getInt("version") != 1) throw new IllegalStateException("The chart artifact revision changed.");
        store.recordTestOnce(test);
        state.put("result", new JSONObject().put("artifact", ref).put("receipt", test)
                .put("plotSha256", WorldFrames.hash(input.getJSONObject("plot"))));
        save(state);
        preview = null;
        return verify(state, document);
    }

    private static String key(String id) { return SCHEMA + ":" + id; }

    private static String artifactId(String id) {
        return UUID.nameUUIDFromBytes(key(id).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private JSONObject verify(JSONObject state, Prepared document) throws Exception {
        var result = state.getJSONObject("result");
        var ref = result.getJSONObject("artifact");
        var artifact = store.read(ref.getString("id"));
        var page = document.page;
        if (!ref.similar(Creation.reference(artifact)) || !page.files().similar(artifact.getJSONObject("contents"))
                || !page.sources().similar(artifact.getJSONArray("sources"))
                || !WorldFrames.hash(page.plot()).equals(result.getString("plotSha256"))) {
            throw new IllegalStateException("The saved chart no longer matches its original source and browser observation.");
        }
        var receipt = result.getJSONObject("receipt");
        if (!RampMissionReport.verifiedReceipt(ref, WorldFrames.copy(receipt).put("passed", true), page.checks())) {
            throw new IllegalStateException("The original chart checks changed.");
        }
        store.verifyRecordedTest(receipt);
        return view(state).put("artifact", ref).put("htmlSha256", page.hash()).put("browserChecks", page.checks().length());
    }

    private Prepared rebuild(JSONObject state) throws Exception {
        if (!state.has("selected")) throw new IllegalStateException("Choose which verified result to chart.");
        String id = state.getString("selected");
        var selected = choice(state, id);
        try {
            var verified = agency.chartEvidence(id);
            if (!selected.similar(verified.getJSONObject("source"))) {
                throw new IllegalStateException("The selected result's original binding changed.");
            }
            var evidence = verified.getJSONObject("evidence");
            var binding = WorldFrames.copy(selected).put("evidenceSha256", WorldFrames.hash(evidence))
                    .put("requestId", state.getString("id"));
            var source = selected.getJSONObject("source");
            if (selected.getString("kind").equals("motion")) {
                var data = measurements.read(source.getString("datasetId"), source.getInt("datasetVersion"));
                if (!source.similar(MotionData.binding(data))) throw new IllegalStateException("The original recording version changed.");
                String kind = switch (data.getString("sourceKind")) {
                    case "measured" -> "Recorded motion";
                    case "synthetic" -> "Synthetic motion";
                    default -> "Motion data";
                };
                binding.put("origin", kind).put("sourceSha256", source.getString("sourceSha256"));
                String title = kind + " chart";
                return new Prepared(state.getString("id"), title, ChartProjection.render(title, "Displacement (m)", binding,
                        List.of(new ChartProjection.Trace("Displacement", MotionData.originalRows(data)))));
            }
            var report = evidence.getJSONObject("ramp").getJSONObject("report");
            var artifact = store.read(report.getString("id"));
            if (artifact.getInt("version") != report.getInt("version")) throw new IllegalStateException("The original ramp chart revision changed.");
            var experiment = new JSONObject(artifact.getJSONObject("contents").getString("experiment.json"));
            var original = experiment.getJSONObject("original");
            var traces = List.of(new ChartProjection.Trace("First setup", original.getJSONObject("first").getJSONArray("samples")),
                    new ChartProjection.Trace("Second setup", original.getJSONObject("second").getJSONArray("samples")));
            binding.put("origin", "Simulated data").put("sourceSha256", source.optString("csvSha256", WorldFrames.hash(source)));
            String title = "Simulated ramp comparison chart";
            return new Prepared(state.getString("id"), title, ChartProjection.render(title, "Downhill distance (m)", binding, traces));
        } catch (Exception failure) {
            throw new IllegalStateException("The chosen source could not be reverified. " + failure.getMessage(), failure);
        }
    }

    private static JSONObject choice(JSONObject state, String id) {
        for (Object raw : state.getJSONArray("choices")) {
            var entry = (JSONObject) raw;
            if (id.equals(entry.getString("taskId"))) return entry;
        }
        throw new IllegalArgumentException("That result was not offered for this chart request.");
    }

    private JSONObject current() throws Exception {
        if (!Files.exists(checkpoint)) return null;
        var state = AgentFiles042.loadChecked(checkpoint);
        if (!SCHEMA.equals(state.getString("schema"))) throw new IllegalStateException("Unknown saved chart request format.");
        return state;
    }

    private JSONObject checked(JSONObject input) throws Exception {
        var state = current();
        if (state == null || !state.getString("id").equals(input.getString("id"))
                || !state.getString("decisionSha256").equals(input.getString("decisionSha256"))) {
            throw new IllegalStateException("This chart request changed. Open the current request again.");
        }
        return state;
    }

    private void save(JSONObject state) throws Exception {
        AgentFiles042.saveChecked(checkpoint, state);
    }

    private static JSONObject view(JSONObject state) {
        var rows = new JSONArray();
        for (Object raw : state.getJSONArray("choices")) {
            var entry = (JSONObject) raw;
            rows.put(new JSONObject().put("taskId", entry.getString("taskId")).put("kind", entry.getString("kind"))
                    .put("label", entry.getString("label")).put("goal", entry.getString("goal"))
                    .put("finishedAt", entry.getString("finishedAt")));
        }
        return new JSONObject().put("id", state.getString("id")).put("decisionSha256", state.getString("decisionSha256"))
                .put("request", state.getString("request")).put("choices", rows).put("selected", state.optString("selected"))
                .put("status", state.has("result") ? "verified" : state.has("selected") ? "ready" : "choose");
    }
}
