package fraymus.workspace;

import java.util.List;
import java.util.regex.Pattern;

import static fraymus.workspace.LocalWorkflowSelection.Procedure;

/** Complete request forms that map to a single existing workflow. Anything else needs review. */
final class QuickWorkflowRouting {
    static final String POLICY = "conservative-v1";
    static final String REVIEW = "localWorkflowReview";
    static final String NO_MODEL = "Choose one workflow in your request, or select a local model for a suggestion. "
            + LocalWorkflowSelection.QUESTION;

    record Match(Procedure procedure, String rule) {}
    private record Rule(Match match, Pattern wording) {
        Rule(Procedure procedure, String id, String wording) {
            this(new Match(procedure, id), Pattern.compile(wording, Pattern.CASE_INSENSITIVE));
        }
    }

    private static final String SETUPS = "(?:saved |selected |prepared )?(?:ramp setups|ramp configurations|inclines)";
    private static final String DECIMAL = "(?:[0-9]{1,4}(?:\\.[0-9]{1,6})?|\\.[0-9]{1,6})";
    private static final List<Rule> RULES = List.of(
            new Rule(Procedure.RAMP, "compare-setups",
                    "(?:compare|contrast) (?:the )?(?:both|two) " + SETUPS +
                            "(?: on (?:the )?same clock| over (?:the )?same duration)?[.!]?"),
            new Rule(Procedure.RAMP, "farther-setup",
                    "which (?:saved |selected |prepared )?(?:ramp setup|ramp configuration|incline) " +
                            "(?:goes|travels) farther(?: on the same clock| in the same time)?[?]?"),
            new Rule(Procedure.ANALYSIS, "summarize-motion",
                    "(?:analy[sz]e|summari[sz]e) (?:the |my )?selected motion (?:csv|dataset|recording)" +
                            "(?: for (?:travel|total travel|motion quality))?" +
                            "(?:[.!] gap limit " + DECIMAL + " seconds and speed limit " + DECIMAL + " m/s)?[.!]?"),
            new Rule(Procedure.CREATION, "standalone-tally",
                    "(?:build|create|make) a standalone (?:offline|local) web app for a tally that adds [1-9][0-9]? " +
                            "and resets to 0(?: with controls #[a-z][a-z0-9-]*, #[a-z][a-z0-9-]* and #[a-z][a-z0-9-]*)?[.!]?"),
            new Rule(Procedure.CREATION, "standalone-switch",
                    "(?:build|create|make) a standalone (?:offline|local) web app for a two-state status switch[.!]?")
    );

    private QuickWorkflowRouting() {}

    static Match match(String briefing) {
        return RULES.stream().filter(rule -> rule.wording().matcher(briefing.strip()).matches())
                .map(Rule::match).findFirst().orElse(null);
    }
}
