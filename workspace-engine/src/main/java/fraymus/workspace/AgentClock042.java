package fraymus.workspace;

import java.time.Instant;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.json.JSONObject;

/** Real wall time and per-boot elapsed time; simulation/source clocks retain their own domains. */
final class AgentClock042 {
    final String bootId=UUID.randomUUID().toString();
    private final LongSupplier monotonic;
    private final Supplier<Instant> wall;
    private final long origin;
    AgentClock042(){this(System::nanoTime,Instant::now);}
    AgentClock042(LongSupplier monotonic,Supplier<Instant> wall){this.monotonic=monotonic;this.wall=wall;origin=monotonic.getAsLong();}
    long elapsedMs(){return Math.max(0,(monotonic.getAsLong()-origin)/1_000_000);}
    Instant wall(){return wall.get();}
    JSONObject read(long ticks,long previousActiveMs){return new JSONObject().put("bootId",bootId).put("wallTime",wall()).put("elapsedThisBootMs",elapsedMs()).put("retainedActiveMs",previousActiveMs+elapsedMs()).put("logicalTicks",ticks).put("domains","Elapsed time is local to this boot. Source timestamps and native simulation ticks are separately qualified; downtime is not simulated activity.");}
}
