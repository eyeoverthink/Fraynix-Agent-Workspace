package fraymus.workspace;
import java.util.*;import java.util.regex.*;
/** Protocol choices are explicit; full remains the original contact122 default. */
final class GameStrategy123 {
 static final List<String> STRATEGIES=List.of("full","no-wait","no-return","direct");
 static String parse(String text){var m=Pattern.compile("(?i)\\bpickupStrategy\\s*(?:=|:)\\s*([^\\s,;]+)").matcher(text);long mentions=Pattern.compile("(?i)\\bpickupStrategy\\b").matcher(text).results().count();if(!m.find()){if(mentions>0)throw new IllegalArgumentException("pickupStrategy requires a value");return "full";}if(mentions!=1)throw new IllegalArgumentException("Choose one pickupStrategy");String v=m.group(1).toLowerCase(Locale.ROOT).replaceFirst("[.]$","");if(m.find()||!STRATEGIES.contains(v))throw new IllegalArgumentException("Choose one pickupStrategy: full, no-wait, no-return, direct");return v;}
 static String policy(String strategy){if(!STRATEGIES.contains(strategy))throw new IllegalArgumentException("Unknown pickup strategy");return strategy.equals("full")?"contact122":"contact123-"+strategy;}
 static boolean applies(String policy){return policy.equals("contact122")||Set.of("contact123-no-wait","contact123-no-return","contact123-direct").contains(policy);}
 static boolean waits(String policy){return !Set.of("contact123-no-wait","contact123-direct").contains(policy);}
 static boolean returns(String policy){return !Set.of("observe121","contact123-no-return","contact123-direct").contains(policy);}
}
