package fraymus.workspace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/** The original cells and the explicitly selected measurement column. */
record FrayTableInput(List<List<String>> rows, int measurementIndex) {
    static final String INVALID = "Use 1–8 unique headers, the named input column, and 1–24 valid rows.";

    static FrayTableInput read(String text, String column, String outputColumn) {
        if (text.startsWith("\uFEFF")) throw new IllegalArgumentException("CSV has a repeated byte-order mark");
        var rows = MeasurementStore.csv(text, 1);
        var headers = rows.getFirst();
        if (headers.size() > 8 || rows.size() > 25)
            throw new IllegalArgumentException(INVALID);
        if (headers.stream().anyMatch(FrayTableInput::badHeader) || new HashSet<>(headers).size() != headers.size())
            throw new IllegalArgumentException("CSV headers must be nonempty and unique, with no surrounding spaces");
        int index = headers.indexOf(column);
        if (index < 0) throw new IllegalArgumentException("The CSV is missing the selected column " + column);
        if (headers.contains(outputColumn)) throw new IllegalArgumentException("The CSV already has the output column " + outputColumn);
        if (rows.stream().anyMatch(row -> row.size() != headers.size()))
            throw new IllegalArgumentException("Every CSV row must have " + headers.size() + " cells");
        return new FrayTableInput(List.copyOf(rows), index);
    }

    private static boolean badHeader(String header) {
        return header.isEmpty() || header.startsWith(" ") || header.endsWith(" ")
                || header.startsWith("\t") || header.endsWith("\t") || header.indexOf('\n') >= 0;
    }

    String withResults(String outputColumn, List<String> converted) {
        var result = new ArrayList<String>();
        result.add(line(rows.getFirst(), outputColumn));
        for (int i = 0; i < converted.size(); i++)
            result.add(line(rows.get(i + 1), converted.get(i)));
        return String.join("\n", result);
    }

    private static String line(List<String> row, String extra) {
        return row.stream().map(FrayTableInput::quote).collect(Collectors.joining(",")) + "," + quote(extra);
    }

    private static String quote(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
