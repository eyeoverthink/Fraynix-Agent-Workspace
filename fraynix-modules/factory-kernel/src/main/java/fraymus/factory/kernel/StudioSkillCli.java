package fraymus.factory.kernel;

import brain_architecture.kernel.*;
import java.nio.file.*;
import java.util.*;

/** Explicit local operator commands. Capability grants are command-line options, not input fields. */
public final class StudioSkillCli {
    private StudioSkillCli() {}
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("skills|verify-journal <workspace>; skill <workspace> <id> --grant CAPABILITY key=value; recover-journal-tail <workspace> <count> <root>");
        Path workspace = Path.of(args[1]).toAbsolutePath().normalize();
        var configuration = KernelConfiguration.defaultForWorkspace(workspace);
        if (args[0].equals("recover-journal-tail")) {
            if (args.length != 4) throw new IllegalArgumentException("Supply the independently retained prefix count and Merkle root (empty root for count zero)");
            System.out.println("Original evidence: " + MerkleAppendOnlyJournal.recoverUnterminatedTail(configuration.getStateRoot(), Integer.parseInt(args[2]), args[3]));
            return;
        }
        try (var kernel = new OfflineKernel(configuration)) {
            if (args[0].equals("verify-journal")) { System.out.println(kernel.getJournal().verify()); return; }
            var dispatcher = new LocalSkillDispatcher(workspace, kernel);
            if (args[0].equals("skills")) { dispatcher.definitions().forEach(System.out::println); return; }
            if (args.length < 3) throw new IllegalArgumentException("Missing skill id");
            var grants = EnumSet.noneOf(LocalSkillDispatcher.Capability.class); var fields = new TreeMap<String, String>();
            for (int i = 3; i < args.length; i++) {
                if (args[i].equals("--grant")) {
                    if (++i == args.length) throw new IllegalArgumentException("Missing capability");
                    grants.add(LocalSkillDispatcher.Capability.valueOf(args[i]));
                } else {
                    int separator = args[i].indexOf('='); if (separator < 1 || fields.put(args[i].substring(0, separator), args[i].substring(separator + 1)) != null)
                        throw new IllegalArgumentException("Expected unique key=value inputs");
                }
            }
            // A headless CLI has no live editor world; it must not invent an empty scene observation.
            System.out.println(dispatcher.invoke(args[2], fields, grants, null));
        }
    }
}
