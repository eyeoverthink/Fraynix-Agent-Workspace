package content;

import com.google.gson.JsonParser;
import fraymus.neural.RelayCourse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class RelayCourseGeneratorTest {
    @Test public void originalCoursesReproduceTheirFilesAndHaveSeparateIdentities() throws Exception {
        Path temp = Files.createTempDirectory("relay-courses").resolve("generated");
        RelayCourseGenerator.generate(temp);
        var ids = new HashSet<String>();
        for (RelayCourse course : RelayCourse.values()) {
            if (!course.hasSignals()) continue;
            for (String file : List.of(course.manifest(), course.level(), RelayCourse.OBJECTS)) {
                Path relative = Path.of(RelayCourse.ASSETS + "courses").relativize(Path.of(file));
                assertArrayEquals(file, Files.readAllBytes(Path.of(file)), Files.readAllBytes(temp.resolve(relative)));
            }
            var level = JsonParser.parseString(Files.readString(Path.of(course.level()))).getAsJsonArray();
            for (var object : level) {
                assertTrue(ids.add(object.getAsJsonObject().get("persistentId").getAsString()));
                for (var component : object.getAsJsonObject().getAsJsonArray("components")) {
                    var fields = component.getAsJsonObject().getAsJsonObject("properties");
                    assertTrue(ids.add(fields.get("persistentId").getAsString()));
                }
            }
        }
        assertThrows(IllegalArgumentException.class, () -> RelayCourseGenerator.generate(temp));
    }
}
