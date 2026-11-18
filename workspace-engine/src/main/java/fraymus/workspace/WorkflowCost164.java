package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.util.function.LongSupplier;

/** Diagnostics only: never stored in an accepted proof or consulted by a verifier. */
final class WorkflowCost164 {
    enum Kind { SOURCE, HISTORY, NATIVE_JOB, NATIVE_OUTCOME, NATIVE_ACCEPT, REPORT_WRITE, REPORT_SOURCE, REPORT_AUDIT }
    @FunctionalInterface interface Work<T> { T run() throws Exception; }
    private static final class Frame { long start,child; Frame(long start){this.start=start;} }
    private static final class Total { long count,failed,inclusive,exclusive,max; }
    private final boolean enabled;
    private final LongSupplier clock;
    private final String epoch=UUID.randomUUID().toString();
    private final ThreadLocal<ArrayDeque<Frame>> stack=ThreadLocal.withInitial(ArrayDeque::new);
    private final EnumMap<Kind,Total> totals=new EnumMap<>(Kind.class);
    private final ArrayDeque<JSONObject> recent=new ArrayDeque<>();
    private long sequence;
    WorkflowCost164(){this(!"off".equals(System.getenv("FRAYNIX_WORKSPACE_COSTS164")),System::nanoTime);}
    WorkflowCost164(boolean enabled,LongSupplier clock){this.enabled=enabled;this.clock=clock;}
    <T>T measure(Kind kind,Work<T> work)throws Exception{
        if(!enabled)return work.run();
        var frames=stack.get();var f=new Frame(clock.getAsLong());frames.push(f);boolean ok=false;
        try{T result=work.run();ok=true;return result;}
        finally{
            long inclusive=Math.max(0,clock.getAsLong()-f.start),exclusive=Math.max(0,inclusive-f.child);frames.pop();
            if(!frames.isEmpty())frames.peek().child+=inclusive;else stack.remove();
            synchronized(this){var t=totals.computeIfAbsent(kind,k->new Total());t.count++;if(!ok)t.failed++;t.inclusive+=inclusive;t.exclusive+=exclusive;t.max=Math.max(t.max,inclusive);
                recent.addLast(new JSONObject().put("sequence",++sequence).put("kind",kind.name()).put("passed",ok).put("inclusiveMs",inclusive/1e6).put("exclusiveMs",exclusive/1e6));if(recent.size()>128)recent.removeFirst();}
        }
    }
    synchronized JSONObject status(){var values=new JSONObject();for(var e:totals.entrySet()){var t=e.getValue();values.put(e.getKey().name(),new JSONObject().put("count",t.count).put("failed",t.failed).put("inclusiveMs",t.inclusive/1e6).put("exclusiveMs",t.exclusive/1e6).put("maxMs",t.max/1e6));}
        return new JSONObject().put("enabled",enabled).put("epoch",epoch).put("operations",sequence).put("dropped",Math.max(0,sequence-128)).put("totals",values).put("recent",new JSONArray(recent.toString())).put("scope","Diagnostic wall spans only. Inclusive nested spans overlap; exclusive time is within one Java thread. Native checks include their original report/source checks. Do not add Java and browser spans as independent time. Restart starts a new epoch; diagnostics never authorize outcomes.");
    }
}
