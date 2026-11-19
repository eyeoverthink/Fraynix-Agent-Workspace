package fraymus.workspace;

import org.json.JSONArray;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** An explicit table of non-overlapping intervals, inclusive below and exclusive above. */
record FrayRangeRules(List<FrayRangeRules.Range> ranges) {
    static final String HEADER = "min_inclusive,max_exclusive,label";
    private static final List<String> COLUMNS = List.of("min_inclusive", "max_exclusive", "label");
    private static final String LABEL = "[A-Za-z][A-Za-z0-9 _-]{0,31}";

    record Range(long min, long max, String label) {}

    static FrayRangeRules read(String text) {
        if (text.startsWith("\uFEFF")) throw new IllegalArgumentException("Rules CSV has a repeated byte-order mark");
        var rows = MeasurementStore.csv(text, 2);
        if (!rows.getFirst().equals(COLUMNS) || rows.size() > 7)
            throw new IllegalArgumentException("Use " + HEADER + " and two to six rules");
        var ranges = new ArrayList<Range>();
        var labels = new HashSet<String>();
        for (var row : rows.subList(1, rows.size())) {
            if (row.size() != 3 || !row.get(2).matches(LABEL) || row.get(2).endsWith(" ") || !labels.add(row.get(2)))
                throw new IllegalArgumentException("Each range needs two limits and a unique plain label");
            long min = scaled(FrayDataTool.plainDecimal(row.get(0)));
            long max = scaled(FrayDataTool.plainDecimal(row.get(1)));
            if (min >= max) throw new IllegalArgumentException("Each range minimum must be below its exclusive maximum");
            ranges.add(new Range(min, max, row.get(2)));
        }
        ranges.sort(Comparator.comparingLong(Range::min));
        for (int i = 1; i < ranges.size(); i++) {
            if (ranges.get(i).min() < ranges.get(i - 1).max())
                throw new IllegalArgumentException("The classification ranges overlap");
        }
        return new FrayRangeRules(List.copyOf(ranges));
    }

    String label(String value) {
        return match(value).label();
    }

    Range match(String value) {
        long number = scaled(value);
        for (var range : ranges) {
            if (number >= range.min() && number < range.max()) return range;
        }
        throw new IllegalArgumentException("No declared range covers " + value);
    }

    List<String> checkValues(Set<String> used) {
        var values = new LinkedHashSet<String>();
        for (var range : ranges) {
            values.add(decimal(range.min()));
            values.add(decimal(range.max() - 1));
            for (long candidate = range.min(); candidate < range.max() && candidate - range.min() <= used.size(); candidate++) {
                String value = decimal(candidate);
                if (!used.contains(value)) {
                    values.add(value);
                    break;
                }
            }
        }
        return List.copyOf(values);
    }

    String uncovered() { return decimal(ranges.getLast().max()); }

    String firstGap() {
        for (int i = 1; i < ranges.size(); i++) {
            long boundary = ranges.get(i - 1).max();
            if (boundary < ranges.get(i).min()) return decimal(boundary);
        }
        return null;
    }

    String csv() {
        var rows = new StringBuilder(HEADER);
        for (var range : ranges)
            rows.append('\n').append(decimal(range.min())).append(',').append(decimal(range.max())).append(',').append(range.label());
        return rows.toString();
    }

    JSONArray browserRules() {
        var rules = new JSONArray();
        for (var range : ranges)
            rules.put(new JSONArray().put(Long.toString(range.min())).put(Long.toString(range.max())).put(range.label()));
        return rules;
    }

    private static long scaled(String value) { return new BigDecimal(value).movePointRight(4).longValueExact(); }

    static String decimal(long value) { return BigDecimal.valueOf(value, 4).stripTrailingZeros().toPlainString(); }
}
