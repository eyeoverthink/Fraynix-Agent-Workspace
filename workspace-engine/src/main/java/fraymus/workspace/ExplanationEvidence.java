package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import static fraymus.workspace.RoutedDashboardReport.plain;

/** An exact, bounded test of a declared numerical prediction against separate paired trials. */
final class ExplanationEvidence {
    static final int MAX_BYTES = 65_536;
    static final List<String> HEADER = List.of("run_id", "observed_at", "source_ref", "unit",
            "treatment_before", "treatment_after", "control_before", "control_after", "error_bound");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?[0-9]{1,12}(?:\\.[0-9]{1,12})?");

    record Interval(BigDecimal low, BigDecimal high) {
        JSONObject evidence() { return new JSONObject().put("low", plain(low)).put("high", plain(high)); }
        String label(String unit) { return plain(low) + " to " + plain(high) + " " + unit; }
    }

    record Trial(String id, Instant when, String source, BigDecimal treatmentBefore, BigDecimal treatmentAfter,
                 BigDecimal controlBefore, BigDecimal controlAfter, BigDecimal bound, Interval interval, String status) {
        BigDecimal treatment() { return treatmentAfter.subtract(treatmentBefore); }
        BigDecimal control() { return controlAfter.subtract(controlBefore); }
        BigDecimal effect() { return treatment().subtract(control()); }

        JSONObject evidence() {
            return new JSONObject().put("runId", id).put("observedAt", when.toString()).put("sourceRef", source)
                    .put("treatmentBefore", plain(treatmentBefore)).put("treatmentAfter", plain(treatmentAfter))
                    .put("controlBefore", plain(controlBefore)).put("controlAfter", plain(controlAfter))
                    .put("treatmentChange", plain(treatment())).put("controlChange", plain(control()))
                    .put("effect", plain(effect())).put("errorBound", plain(bound))
                    .put("interval", interval.evidence()).put("status", status);
        }
    }

    record Result(String csv, String sha256, String unit, BigDecimal observedDelta, BigDecimal margin,
                  Interval prediction, List<Trial> trials, String status, int consistent, int conflicting, int unresolved) {
        JSONObject evidence() {
            return new JSONObject().put("observationsSha256", sha256).put("unit", unit)
                    .put("comparisonDelta", plain(observedDelta)).put("margin", plain(margin))
                    .put("prediction", prediction.evidence()).put("status", status)
                    .put("consistent", consistent).put("conflicting", conflicting).put("unresolved", unresolved)
                    .put("trials", new JSONArray(trials.stream().map(Trial::evidence).toList()));
        }
    }

    private ExplanationEvidence() {}

    static BigDecimal decimal(String text, String label) {
        String value = text.strip();
        if (!DECIMAL.matcher(value).matches()) throw new IllegalArgumentException("Enter an exact decimal for " + label + ".");
        return new BigDecimal(value);
    }

    static BigDecimal margin(String text, JSONObject claim) {
        var margin = decimal(text, "the allowed difference");
        if (margin.signum() <= 0 || margin.compareTo(new BigDecimal(claim.getString("delta")).abs()) >= 0)
            throw new IllegalArgumentException("Use a positive allowed difference smaller than the selected change.");
        return margin;
    }

    static Result evaluate(String encoded, JSONObject claim, String allowance, String originalSha256) throws Exception {
        if (encoded.length() > (MAX_BYTES + 2) / 3 * 4) throw new IllegalArgumentException("Choose an observation CSV up to 64 KiB.");
        var bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Choose an observation CSV up to 64 KiB.");
        String hash = PerceptionStore.hash(bytes);
        if (hash.equals(originalSha256)) throw new IllegalArgumentException("Attach separate trial observations.");
        String csv = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        var rows = MeasurementStore.csv(csv, 1);
        if (!rows.getFirst().equals(HEADER) || rows.size() > 65) throw new IllegalArgumentException("Use the listed CSV columns and 1 to 64 trial rows.");
        String unit = claim.getString("unit");
        var observed = new BigDecimal(claim.getString("delta"));
        var margin = margin(allowance, claim);
        var expected = new Interval(observed.subtract(margin), observed.add(margin));

        var ids = new HashSet<String>();
        var references = new HashSet<String>();
        var trials = new ArrayList<Trial>();
        int consistent = 0, conflicting = 0, unresolved = 0;
        for (int index = 1; index < rows.size(); index++) {
            var row = rows.get(index);
            if (row.size() != HEADER.size()) throw new IllegalArgumentException("Observation row " + (index + 1) + " has the wrong number of fields.");
            String id = required(row.get(0), 48, "run ID");
            String reference = required(row.get(2), 160, "source reference");
            if (!ids.add(id) || !references.add(reference)) {
                throw new IllegalArgumentException("Give each trial its own run ID and source reference.");
            }
            if (!row.get(3).strip().equals(unit)) throw new IllegalArgumentException("Every observation must use " + unit + ".");
            Instant date;
            try {
                date = Instant.parse(row.get(1).strip());
            } catch (java.time.format.DateTimeParseException error) {
                throw new IllegalArgumentException("Use an ISO-8601 timestamp with a time zone for observation row " + (index + 1) + ".", error);
            }
            var a = decimal(row.get(4), "treatment before");
            var b = decimal(row.get(5), "treatment after");
            var c = decimal(row.get(6), "control before");
            var d = decimal(row.get(7), "control after");
            var error = decimal(row.get(8), "the error bound");
            if (error.signum() < 0) throw new IllegalArgumentException("Error bounds must be zero or positive.");
            var effect = b.subtract(a).subtract(d.subtract(c));
            var interval = new Interval(effect.subtract(error), effect.add(error));
            String status;
            if (interval.low().compareTo(expected.high()) > 0 || interval.high().compareTo(expected.low()) < 0) {
                status = "conflicting";
                conflicting++;
            } else if (interval.low().compareTo(expected.low()) >= 0 && interval.high().compareTo(expected.high()) <= 0) {
                status = "consistent";
                consistent++;
            } else {
                status = "unresolved";
                unresolved++;
            }
            trials.add(new Trial(id, date, reference, a, b, c, d, error, interval, status));
        }
        String status;
        if (conflicting > 0) status = consistent > 0 ? "mixed" : "conflicting";
        else if (consistent >= 2 && unresolved == 0) status = "supports-prediction";
        else status = "inconclusive";
        return new Result(csv, hash, unit, observed, margin, expected, List.copyOf(trials), status,
                consistent, conflicting, unresolved);
    }

    static String required(String text, int limit, String label) {
        String value = text.strip();
        if (value.isEmpty() || value.length() > limit || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Enter " + label + " in one line, up to " + limit + " characters.");
        }
        return value;
    }
}
