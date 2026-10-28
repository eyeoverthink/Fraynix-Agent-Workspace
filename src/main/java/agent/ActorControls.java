package agent;

import java.util.UUID;

/** Tick leases keep late or replayed commands from controlling a different episode. */
public final class ActorControls {
    public record Input(float horizontal, float vertical, boolean jump, boolean fire) {
        public static final Input NONE = new Input(0,0,false,false);
        public Input {
            if (!Float.isFinite(horizontal) || !Float.isFinite(vertical) || Math.abs(horizontal)>1 || Math.abs(vertical)>1)
                throw new IllegalArgumentException("Axes must be finite and between -1 and 1");
        }
    }
    public record Lease(UUID episode, UUID actor, long sequence, long firstTick, long lastTick, Input input) {
        public Lease {
            java.util.Objects.requireNonNull(episode); java.util.Objects.requireNonNull(actor); java.util.Objects.requireNonNull(input);
            if (sequence<0 || firstTick<0 || lastTick<firstTick || lastTick-firstTick>60)
                throw new IllegalArgumentException("Invalid input lease bounds");
        }
    }
    private UUID episode, actor;
    private long sequence = -1;
    private Lease lease;
    public void reset(UUID episode, UUID actor) { this.episode=episode; this.actor=actor; sequence=-1; lease=null; }
    public boolean accept(Lease next, long now) {
        if (next == null || !next.episode().equals(episode) || !next.actor().equals(actor)
            || next.sequence()<=sequence || next.firstTick()>now || next.lastTick()<now
            || next.lastTick()-next.firstTick()>60 || next.lastTick()<next.firstTick() || next.input()==null) return false;
        sequence=next.sequence(); lease=next; return true;
    }
    public Input at(long tick) { return lease!=null && tick>=lease.firstTick() && tick<=lease.lastTick() ? lease.input() : Input.NONE; }
    public void release() { lease=null; }
}
