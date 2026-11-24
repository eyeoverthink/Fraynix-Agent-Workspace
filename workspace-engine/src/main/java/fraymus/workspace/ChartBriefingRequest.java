package fraymus.workspace;

import java.util.regex.Pattern;

/** Recognizes briefings about an already verified named comparison chart. */
final class ChartBriefingRequest {
    static final String CAUSE_MESSAGE =
            "Comparison rows show how readings changed. To explain why, attach separate evidence about the cause.";
    private static final String POLITE = "(?:(?:can|could|would) you )?(?:please )?";
    private static final String END = "[.!?]?";
    private static final String BRIEF = "(?:brief me on|write (?:me )?a briefing (?:from|on)|make (?:me )?a briefing (?:from|on)) ";
    private static final Pattern THIS = wording(BRIEF + "that chart");
    private static final Pattern LAST = wording(BRIEF + "the last comparison chart");
    private static final Pattern REOPEN = wording("(?:open|resume|recheck) (?:my |the )?last chart briefing");
    private static final Pattern CAUSE = wording("(?:what caused (?:the |those )?changes (?:in|on) that chart|"
            + BRIEF + "that chart and (?:explain|tell me) why (?:it|the readings) changed)");

    enum Intent { DISPLAYED, LAST_CHART, RESUME, CAUSE }

    private ChartBriefingRequest() {}

    static Intent detect(String text) {
        String request = text.strip();
        if (request.length() > 1200) return null;
        if (CAUSE.matcher(request).matches()) return Intent.CAUSE;
        if (THIS.matcher(request).matches()) return Intent.DISPLAYED;
        if (LAST.matcher(request).matches()) return Intent.LAST_CHART;
        if (REOPEN.matcher(request).matches()) return Intent.RESUME;
        return null;
    }

    private static Pattern wording(String text) {
        return Pattern.compile(POLITE + text + END, Pattern.CASE_INSENSITIVE);
    }
}
