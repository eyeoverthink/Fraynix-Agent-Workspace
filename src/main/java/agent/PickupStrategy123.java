package agent;
import java.util.Set;
/** Explicit feature ablations of contact122; no safety or movement override. */
public final class PickupStrategy123 {
 public static final Set<String> POLICIES=Set.of("contact122","contact123-no-wait","contact123-no-return","contact123-direct");
 public static boolean waits(String policy){return !Set.of("contact123-no-wait","contact123-direct").contains(policy);}
 public static boolean returns(String policy){return !Set.of("observe121","contact123-no-return","contact123-direct").contains(policy);}
 private PickupStrategy123(){}
}
