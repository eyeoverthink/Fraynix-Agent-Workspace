package fraymus.workspace;

import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/** The existing JSON representation, counted and hashed without allocating a whole report copy. */
final class JsonStream058 {
    static long characters(JSONObject value) {
        var counter = new Writer() {
            long count;
            public void write(char[] chars, int offset, int length) { count += length; }
            public void write(String text, int offset, int length) { count += length; }
            public void write(int c) { count++; }
            public void flush() {}
            public void close() {}
        };
        value.write(counter);
        return counter.count;
    }
    static void write(JSONObject value, OutputStream output) throws IOException {
        var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), 65536);
        value.write(writer);
        writer.flush();
    }
    static String sha256(JSONObject value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var output = new DigestOutputStream(OutputStream.nullOutputStream(), digest)) { write(value, output); }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
