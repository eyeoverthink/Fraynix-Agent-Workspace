package fraymus.neural;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Closed set of original courses understood by the host and native runtime. */
public enum RelayCourse {
    GARDEN("relay-garden-v1", "", "Relay Garden"),
    CIRCUIT("relay-circuit-v1", "circuit", "Relay Circuit"),
    TERRACES("relay-terraces-v1", "terraces", "Relay Terraces");

    public static final String ASSETS = "assets/original/relay-garden/";
    public static final String OBJECTS = ASSETS + "courses/signals.png";
    private final String id;
    private final String directory;
    private final String title;

    RelayCourse(String id, String directory, String title) {
        this.id = id;
        this.directory = directory;
        this.title = title;
    }

    public String id() { return id; }
    public String title() { return title; }
    public boolean hasSignals() { return this != GARDEN; }
    public String manifest() { return root() + "manifest.json"; }
    public String level() { return root() + "level.json"; }

    private String root() {
        return hasSignals() ? ASSETS + "courses/" + directory + "/" : ASSETS;
    }

    public static RelayCourse find(String id) {
        return Arrays.stream(values()).filter(course -> course.id.equals(id)).findFirst().orElse(null);
    }

    public static final List<String> CUES = List.of("main-theme-overworld", "flagpole", "break_block", "bump",
            "coin", "gameover", "jump-small", "mario_die", "pipe", "powerup", "powerup_appears",
            "stage_clear", "stomp", "kick", "invincible");

    public Set<String> files() {
        var paths = new LinkedHashSet<>(List.of(ASSETS + "atlas.png", ASSETS + "sky.png", level()));
        for (String cue : CUES) paths.add(ASSETS + cue + ".wav");
        if (hasSignals()) paths.add(OBJECTS);
        return paths;
    }
}
