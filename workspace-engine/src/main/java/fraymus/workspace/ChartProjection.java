package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Projects each original position onto one labeled time axis. No fitting or resampling. */
final class ChartProjection {
    private static final List<String> COLORS = List.of("#097c73", "#9a5121");
    private static final String TEMPLATE = template();

    record Trace(String label, JSONArray samples) {
        double firstTime() { return samples.getJSONObject(0).getDouble("timeS"); }
        double lastTime() { return samples.getJSONObject(samples.length() - 1).getDouble("timeS"); }
    }

    record Page(JSONObject files, JSONArray checks, JSONObject plot, JSONArray sources) {
        String html() { return files.getString("index.html"); }
        String hash() { return MeasurementStore.hash(html()); }
        JSONObject artifact(String title) { return new JSONObject().put("title", title).put("files", files).put("sources", sources); }
    }

    private ChartProjection() {}

    static Page render(String title, String quantity, JSONObject binding, List<Trace> traces) {
        double lowTime = traces.stream().mapToDouble(Trace::firstTime).min().orElseThrow();
        double highTime = traces.stream().mapToDouble(Trace::lastTime).max().orElseThrow();
        double low = 0;
        double high = 0;
        int samples = 0;
        for (var trace : traces) {
            for (Object sample : trace.samples()) {
                double distance = ((JSONObject) sample).getDouble("distanceM");
                low = Math.min(low, distance);
                high = Math.max(high, distance);
                samples++;
            }
        }
        if (high == low) {
            high = .001;
        }
        String origin = binding.getString("origin");
        String sourceHash = binding.getString("sourceSha256");
        String caption = samples + " original samples · " + quantity;
        var lines = new JSONArray();
        var series = new JSONArray();
        var svg = new StringBuilder();
        var legend = new StringBuilder();
        for (int i = 0; i < traces.size(); i++) {
            var trace = traces.get(i);
            var path = new StringBuilder();
            for (Object sample : trace.samples()) {
                var row = (JSONObject) sample;
                double x = 92 + 616 * ((row.getDouble("timeS") - lowTime) / (highTime - lowTime));
                double y = 265 - 224 * ((row.getDouble("distanceM") - low) / (high - low));
                if (!path.isEmpty()) path.append(' ');
                path.append(String.format(Locale.ROOT, "%.3f,%.3f", x, y));
            }
            String points = path.toString();
            lines.put(points);
            series.put(new JSONObject().put("label", trace.label()).put("samples", trace.samples()));
            svg.append("<polyline fill=\"none\" stroke=\"").append(COLORS.get(i))
                    .append("\" stroke-width=\"3\" vector-effect=\"non-scaling-stroke\" points=\"")
                    .append(points).append("\"/>");
            legend.append("<li><span style=\"background:").append(COLORS.get(i))
                    .append("\"></span>").append(SpatialReport.escape(trace.label())).append("</li>");
        }

        String lowerTime = axis(lowTime);
        String upperTime = axis(highTime);
        String lowerValue = axis(low);
        String upperValue = axis(high);
        String middleValue = axis((high + low) / 2);
        String html = TEMPLATE.formatted(SpatialReport.escape(title), SpatialReport.escape(origin), SpatialReport.escape(title),
                SpatialReport.escape(caption), svg, lowerTime, upperTime, lowerValue, middleValue, upperValue, legend,
                SpatialReport.escape(binding.getString("label")), sourceHash, binding.getString("evidenceSha256"));
        var checks = new JSONArray()
                .put(text("#chart-title", title)).put(text("#chart-origin", origin)).put(text("#chart-source-sha", sourceHash))
                .put(text("#chart-caption", caption))
                .put(count("svg[role=img] polyline", traces.size())).put(count("a[href='source.json']", 1));
        var plot = new JSONObject().put("lines", lines).put("timeStart", lowerTime).put("timeEnd", upperTime)
                .put("valueMin", lowerValue).put("valueMiddle", middleValue).put("valueMax", upperValue)
                .put("visible", true);
        var evidence = new JSONObject().put("schema", ReferenceChart.SCHEMA).put("binding", binding)
                .put("series", series).put("projection", plot);
        return new Page(new JSONObject().put("index.html", html).put("source.json",
                MapCheckpoint.canonical(evidence) + "\n"), checks, plot,
                new JSONArray().put("sha256:" + sourceHash).put("sha256:" + binding.getString("evidenceSha256")));
    }

    private static JSONObject text(String selector, String expected) {
        return new JSONObject().put("selector", selector).put("expectText", expected);
    }

    private static JSONObject count(String selector, int expected) {
        return new JSONObject().put("selector", selector).put("expectCount", expected);
    }

    private static String axis(double value) {
        return String.format(Locale.ROOT, "%.5g", value);
    }

    private static String template() {
        try (var in = ChartProjection.class.getResourceAsStream("/reference-chart.html")) {
            if (in == null) throw new IllegalStateException("Chart layout is missing");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Chart layout could not be read", failure);
        }
    }
}
