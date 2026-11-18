package components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Native contact results for the current course attempt. Recreated on reset. */
public final class CourseProgress extends Component {
    public int totalPickups;
    private final transient List<ContactEvent> pickups = new ArrayList<>();
    private transient ContactEvent hazard;

    public record ContactEvent(String id, String name, long tick) {}

    void collect(ContactEvent event) {
        pickups.add(event);
    }

    void hitHazard(ContactEvent event) {
        hazard = event;
    }

    public Map<String, Object> observe() {
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("totalPickups", totalPickups);
        result.put("pickups", List.copyOf(pickups));
        if (hazard != null) result.put("hazard", hazard);
        return result;
    }
}
