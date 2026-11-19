package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Converts supported data representations and reads file bytes without running the file. */
final class FileRepresentation {
    private FileRepresentation() {}

    record Output(String name, String data, String note) {}
    record Table(List<String> columns, List<List<String>> rows) {
        Table {
            if (columns.isEmpty() || columns.size() > 32 || columns.stream().anyMatch(String::isBlank)
                    || new HashSet<>(columns).size() != columns.size() || rows.isEmpty() || rows.size() > 2048
                    || rows.stream().anyMatch(row -> row.size() != columns.size()))
                throw new IllegalArgumentException("Use up to 32 unique named columns and 1 to 2048 rows with matching widths");
        }

        String json() {
            return new JSONObject().put("columns", columns).put("rows", rows).toString(2) + "\n";
        }

        String csv() {
            var text = new StringBuilder(csvRow(columns));
            for (var row : rows) text.append(csvRow(row));
            return text.toString();
        }

        String describe() { return rows.size() + " rows × " + columns.size() + " columns · string cells verified by round trip"; }
    }

    static Output convert(RequestLanguage.Intent intent, byte[] bytes) throws Exception {
        if (intent == RequestLanguage.Intent.INSPECT) return inspect(bytes);
        String source = utf8(bytes);
        if (intent == RequestLanguage.Intent.TO_JSON) {
            var table = csv(source);
            String out = table.json();
            if (!table.equals(new TableJson(out).parse())) throw new IllegalStateException("JSON round trip differs");
            return new Output("converted.json", out, table.describe());
        }
        if (intent == RequestLanguage.Intent.TO_CSV) {
            var table = new TableJson(source).parse();
            String out = table.csv();
            if (!table.equals(csv(out))) throw new IllegalStateException("CSV round trip differs");
            return new Output("converted.csv", out, table.describe());
        }
        throw new IllegalArgumentException("Select a file action");
    }

    private static Table csv(String text) {
        var cells = MeasurementStore.csv(text, 0);
        return new Table(cells.getFirst(), cells.subList(1, cells.size()));
    }

    private static String csvRow(List<String> row) {
        return row.stream().map(cell -> "\"" + cell.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(",")) + "\r\n";
    }

    private static String utf8(byte[] bytes) {
        try {
            String value = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            return value.startsWith("\uFEFF") ? value.substring(1) : value;
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Conversion needs a valid UTF-8 CSV or JSON file", e);
        }
    }

    /** Small, strict reader for the supported JSON shapes. JSON strings are lexed before decoding. */
    private static final class TableJson {
        private static final Pattern STRING = Pattern.compile("\"(?:[^\"\\\\\\x00-\\x1F]++|\\\\(?:[\"\\\\/bfnrt]|u[0-9a-fA-F]{4}))*+\"");
        private final String text;
        private int pos;

        TableJson(String text) { this.text = text; }

        Table parse() {
            Table table = peek() == '{' ? namedTable() : records();
            if (peek() != -1) throw shape();
            return table;
        }

        private Table namedTable() {
            expect('{');
            List<String> columns = null;
            List<List<String>> rows = null;
            do {
                String name = string();
                expect(':');
                if (name.equals("columns") && columns == null) columns = strings();
                else if (name.equals("rows") && rows == null) {
                    var values = new ArrayList<List<String>>();
                    expect('[');
                    if (!take(']')) {
                        do {
                            values.add(strings());
                            if (values.size() > 2048) throw shape();
                        } while (take(','));
                        expect(']');
                    }
                    rows = values;
                } else throw shape();
            } while (take(','));
            expect('}');
            if (columns == null || rows == null) throw shape();
            return new Table(columns, rows);
        }

        private Table records() {
            expect('[');
            List<String> columns = null;
            var rows = new ArrayList<List<String>>();
            do {
                expect('{');
                var record = new TreeMap<String, String>();
                do {
                    String name = string();
                    expect(':');
                    if (record.putIfAbsent(name, string()) != null || record.size() > 32) throw shape();
                } while (take(','));
                expect('}');
                if (columns == null) columns = List.copyOf(record.keySet());
                if (!columns.equals(List.copyOf(record.keySet()))) throw shape();
                rows.add(List.copyOf(record.values()));
                if (rows.size() > 2048) throw shape();
            } while (take(','));
            expect(']');
            return new Table(columns, rows);
        }

        private List<String> strings() {
            expect('[');
            var cells = new ArrayList<String>();
            if (!take(']')) {
                do {
                    cells.add(string());
                    if (cells.size() > 32) throw shape();
                } while (take(','));
                expect(']');
            }
            return cells;
        }

        private String string() {
            var matcher = STRING.matcher(text).region(posAfterSpace(), text.length());
            if (!matcher.lookingAt()) throw shape();
            pos = matcher.end();
            String cell = new JSONTokener(matcher.group()).nextValue().toString();
            if (cell.length() > 4096 || cell.indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(cell)) throw shape();
            return cell;
        }

        private int posAfterSpace() {
            while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) pos++;
            return pos;
        }

        private int peek() { return posAfterSpace() < text.length() ? text.charAt(pos) : -1; }
        private boolean take(char ch) { if (peek() != ch) return false; pos++; return true; }
        private void expect(char ch) { if (!take(ch)) throw shape(); }
        private IllegalArgumentException shape() {
            return new IllegalArgumentException("JSON needs a columns/rows table or records with identical string-valued keys (near character "
                    + (pos + 1) + "). Numbers, booleans, null and nested values need an explicit type mapping.");
        }
    }

    private record Signature(String hex, String label) {}
    private static final List<Signature> SIGNATURES = List.of(
            new Signature("89504e470d0a1a0a", "PNG"),
            new Signature("504b0304", "ZIP"), new Signature("504b0506", "ZIP"),
            new Signature("255044462d", "PDF"), new Signature("7f454c46", "ELF"),
            new Signature("4d5a", "MZ executable family"), new Signature("cafebabe", "Java class or Mach-O universal"),
            new Signature("0061736d", "WebAssembly"), new Signature("47494638", "GIF"));

    private static Output inspect(byte[] bytes) throws Exception {
        int preview = Math.min(bytes.length, 64);
        var data = new JSONArray();
        var formatter = HexFormat.of();
        String prefix = formatter.formatHex(bytes, 0, Math.min(preview, 8));
        String signature = SIGNATURES.stream().filter(x -> prefix.startsWith(x.hex())).findFirst()
                .map(Signature::label).orElse("Unknown");
        for (int i = 0; i < preview; i++) {
            int value = Byte.toUnsignedInt(bytes[i]);
            data.put(new JSONObject().put("offset", i).put("hex", formatter.toHexDigits(bytes[i]))
                    .put("decimal", value).put("bits", String.format(Locale.ROOT, "%8s", Integer.toBinaryString(value)).replace(' ', '0')));
        }
        var result = new JSONObject().put("bytes", bytes.length).put("sha256", PerceptionStore.hash(bytes))
                .put("signatureHint", signature).put("previewBytes", preview).put("preview", data);
        return new Output("inspection.json", MapCheckpoint.canonical(result) + "\n",
                bytes.length + " bytes · " + signature + " signature hint · first " + preview + " byte values");
    }
}
