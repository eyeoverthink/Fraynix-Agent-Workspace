package agent;

import components.*;
import jade.GameObject;
import java.util.*;
import scenes.Scene;

/** Visible game components supply identity and geometry; goals time out instead of looping forever. */
public final class PowerupPlanner {
    public enum Kind { TRAVEL, BUMP_BLOCK, COLLECT }
    public record Goal(Kind kind,GameObject object,float x,float y,String reason) {}
    private final Set<UUID> skipped=new HashSet<>();
    private UUID target;
    private UUID lastBlock;
    private long since;
    private final java.util.function.Predicate<UUID> avoided;
    private final java.util.function.Consumer<UUID> failed;
    public PowerupPlanner() {this(id->false,id->{});}
    public PowerupPlanner(java.util.function.Predicate<UUID> avoided,java.util.function.Consumer<UUID> failed) {this.avoided=avoided;this.failed=failed;}
    private void skip(UUID id) {skipped.add(id);failed.accept(lastBlock!=null?lastBlock:id);target=null;}
    public void abandon(Goal goal) {if(goal.object()!=null)skip(goal.object().getPersistentId());}
    public Goal step(Scene scene,GameObject player,float destination,long tick) {
        float x=player.transform.position.x,y=player.transform.position.y;
        PlayerController controller=player.getComponent(PlayerController.class);
        GameObject choice=scene.getGameObjects().stream().filter(o->!o.isDead()&&!skipped.contains(o.getPersistentId())&&!avoided.test(o.getPersistentId()))
            .filter(o->o.getComponent(MushroomAI.class)!=null && controller.isSmall()
                || o.getComponent(Flower.class)!=null && !controller.isFire())
            .filter(o->Math.abs(o.transform.position.x-x)<4 && Math.abs(o.transform.position.y-y)<1.5f)
            .min(Comparator.comparingDouble(o->Math.abs(o.transform.position.x-x)+Math.abs(o.transform.position.y-y)*1.5)).orElse(null);
        Kind kind=Kind.COLLECT;
        if(choice==null && !controller.isFire()) {
            kind=Kind.BUMP_BLOCK;
            choice=scene.getGameObjects().stream().filter(o->!o.isDead()&&!skipped.contains(o.getPersistentId())&&!avoided.test(o.getPersistentId()))
                .filter(o->o.getComponent(QuestionBlock.class)!=null && o.getComponent(QuestionBlock.class).hasPowerup())
                .filter(o->Math.abs(o.transform.position.x-x)<2.5f && o.transform.position.y-y>.25f && o.transform.position.y-y<1.6f
                    && (o.transform.position.x-x)*Math.signum(destination-x)>-.6f)
                .min(Comparator.comparingDouble(o->Math.abs(o.transform.position.x-x))).orElse(null);
        }
        if(choice==null) {target=null;return new Goal(Kind.TRAVEL,null,destination,y,"Reach target");}
        if(kind==Kind.BUMP_BLOCK)lastBlock=choice.getPersistentId();
        if(!choice.getPersistentId().equals(target)) {target=choice.getPersistentId();since=tick;}
        if(tick-since>480) {skip(target);return new Goal(Kind.TRAVEL,null,destination,y,"Power-up unreachable within 8s; continue safely");}
        return new Goal(kind,choice,choice.transform.position.x,choice.transform.position.y,
            kind==Kind.COLLECT?"Collect "+(choice.getComponent(MushroomAI.class)!=null?"mushroom":"flower"):"Bump power-up block");
    }
}
