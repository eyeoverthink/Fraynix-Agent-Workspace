package fraymus.workspace;

import java.nio.file.*;

/** Explicit synthetic controls for end-to-end tests; never presented as recorded video. */
public final class TrackingFixtures {
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Choose a new fixture output directory");
        var target=Path.of(args[0]);Files.createDirectories(target);
        Files.writeString(target.resolve("development.ftrack.json"),TrackingLabTest.pack("Synthetic development","development",0,false).toString(),StandardOpenOption.CREATE_NEW);
        Files.writeString(target.resolve("aspect.ftrack.json"),TrackingLabTest.pack("Synthetic aspect control","evaluation",15,true).toString(),StandardOpenOption.CREATE_NEW);
    }
}
