package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/** Restores source order from verified batches, with independent exact-decimal checks. */
final class RoutedCsvReport {
    static final List<String> FIELDS = List.of("source_row", "classification", "lesson", "input_column",
            "input_value", "input_unit", "output_value", "output_unit");

    private RoutedCsvReport() {}

    static String build(List<List<String>> original, JSONArray decisions, JSONArray stages, JSONArray verifiedResults) {
        if (original.size() != decisions.length() + 1 || stages.length() != verifiedResults.length())
            throw new IllegalStateException("Every source row and routed batch needs a verified result");
        var batches = new LinkedHashMap<String, Batch>();
        for (int i = 0; i < stages.length(); i++) {
            var stage = stages.getJSONObject(i);
            String lessonName = stage.getString("name");
            if (batches.putIfAbsent(lessonName, batch(stage, verifiedResults.getJSONObject(i))) != null)
                throw new IllegalStateException("A routed lesson appears more than once");
        }
        List<String> headers = original.getFirst();
        var output = new ArrayList<List<String>>();
        var reportHeaders = new ArrayList<>(headers);
        reportHeaders.addAll(reportFields(headers));
        output.add(reportHeaders);
        for (int i = 0; i < decisions.length(); i++) {
            var chosen = decisions.getJSONObject(i);
            var batch = batches.get(chosen.getString("lesson"));
            var row = original.get(i + 1);
            int sourceColumn = headers.indexOf(chosen.getString("inputColumn"));
            String value = chosen.getString("value");
            if (chosen.getInt("row") != i + 2 || batch == null || sourceColumn < 0
                    || !batch.header.equals(headers.get(sourceColumn))
                    || !batch.unit.key.equals(chosen.getString("unit"))
                    || !value.equals(FrayDataTool.plainDecimal(row.get(sourceColumn))))
                throw new IllegalStateException("The routed choice does not match its original row");
            String actual = batch.consume(value);
            var combined = new ArrayList<>(row);
            combined.addAll(List.of(Integer.toString(i + 2), chosen.getString("label"), chosen.getString("lesson"),
                    batch.header, value, batch.unit.input, actual, batch.unit.output));
            output.add(combined);
        }
        for (var batch : batches.values())
            if (batch.position != batch.inputs.size()) throw new IllegalStateException("A routed batch has unused output rows");
        String csv = output.stream().map(FrayTableInput::csvLine).collect(Collectors.joining("\n")) + "\n";
        if (!MeasurementStore.csv(csv, 1).equals(output))
            throw new IllegalStateException("The combined CSV did not preserve its checked rows");
        return csv;
    }

    private static List<String> reportFields(List<String> headers) {
        var existing = new HashSet<>(headers);
        for (int attempt = 1; attempt <= headers.size() + 1; attempt++) {
            String prefix = attempt == 1 ? "route_" : "route_" + attempt + "_";
            var proposal = FIELDS.stream().map(field -> prefix + field).toList();
            if (proposal.stream().noneMatch(existing::contains)) return proposal;
        }
        throw new IllegalStateException("Could not assign unique routing column names");
    }

    private static Batch batch(JSONObject stage, JSONObject verified) {
        var lesson = stage.getJSONObject("lesson");
        var unit = Unit.from(lesson.getString("column"));
        String header = RequestWorkbench.routingHeader(lesson);
        var source = MeasurementStore.csv(stage.getString("csv"), 1);
        if (!source.getFirst().equals(List.of(header)) || source.stream().anyMatch(row -> row.size() != 1))
            throw new IllegalStateException("The routed batch input changed its column");
        var inputs = source.subList(1, source.size()).stream().map(List::getFirst).toList();
        String attested = verified.getString("result");
        var values = new ArrayList<String>();
        if (lesson.has("tableColumn")) {
            var table = MeasurementStore.csv(attested, 1);
            if (!table.getFirst().equals(List.of(header, unit.outputColumn)) || table.size() != inputs.size() + 1)
                throw new IllegalStateException("The verified table changed its output column or row count");
            for (int i = 0; i < inputs.size(); i++) {
                var row = table.get(i + 1);
                if (row.size() != 2 || !row.getFirst().equals(inputs.get(i)))
                    throw new IllegalStateException("The verified table changed a routed input row");
                values.add(row.getLast());
            }
        } else {
            String suffix = " " + unit.output;
            for (String row : attested.split("\n", -1)) {
                if (!row.endsWith(suffix)) throw new IllegalStateException("The verified result changed its output unit");
                values.add(row.substring(0, row.length() - suffix.length()));
            }
        }
        if (inputs.size() != values.size()) throw new IllegalStateException("A routed batch has an incomplete result");
        return new Batch(header, unit, inputs, values);
    }

    private static final class Batch {
        final String header;
        final Unit unit;
        final List<String> inputs;
        final List<String> values;
        int position;

        Batch(String header, Unit unit, List<String> inputs, List<String> values) {
            this.header = header;
            this.unit = unit;
            this.inputs = inputs;
            this.values = values;
        }

        String consume(String value) {
            if (position >= inputs.size() || !value.equals(inputs.get(position)))
                throw new IllegalStateException("A verified batch was out of order");
            String observed = values.get(position++);
            var calculated = new BigDecimal(value).multiply(unit.factor).add(unit.offset).stripTrailingZeros();
            if (!observed.equals(calculated.toPlainString()))
                throw new IllegalStateException("The routed output did not match the independent decimal check");
            return observed;
        }
    }

    private enum Unit {
        TEMPERATURE("celsius", "°C", "°F", "fahrenheit", "1.8", "32"),
        LENGTH("inches", "in", "mm", "millimetres", "25.4", "0");

        final String key, input, output, outputColumn;
        final BigDecimal factor, offset;

        Unit(String key, String input, String output, String outputColumn, String factor, String offset) {
            this.key = key;
            this.input = input;
            this.output = output;
            this.outputColumn = outputColumn;
            this.factor = new BigDecimal(factor);
            this.offset = new BigDecimal(offset);
        }

        static Unit from(String key) {
            return switch (key) {
                case "celsius" -> TEMPERATURE;
                case "inches" -> LENGTH;
                default -> throw new IllegalStateException("The routing export requires a verified conversion unit");
            };
        }
    }
}
