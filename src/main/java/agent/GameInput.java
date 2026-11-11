package agent;

import components.*;
import jade.GameObject;
import jade.KeyListener;
import org.joml.Vector2f;
import scenes.Scene;
import java.util.UUID;
import static org.lwjgl.glfw.GLFW.*;

/** Shared input boundary for the original player AND pipe mechanics. No teleport operations. */
public final class GameInput {
    private static final ActorControls controls = new ActorControls();
    private static UUID episode, actor;
    private static boolean automatic;
    private static boolean landingGuard = true;
    private static String policyName="safe117";
    private static boolean safeCollection, riskAware, contactAware;
    private static ContactRecovery119 contactRecovery=new ContactRecovery119();
    private static long contactChecks,contactNanos,contactOverrides;
    private static int pickupDeferrals;
    private static long pickupRiskChecks,pickupRiskNanos;
    private static int pickupSkips;
    private static final java.util.List<java.util.Map<String,Object>> pickupDecisions=new java.util.ArrayList<>();
    private static long fallingWaitTicks;
    private static Pursuit117.Choice pursuit;
    private static long landingChecks, landingOverrides, landingNanos;
    private static LandingGuard116.Decision landingDecision;
    private static long edgeUntil, edgeCrossings;
    private static float edgeDirection;
    private static boolean edgeLeftGround;
    private static long sequence, tick, jumpUntil, lastJump=-100;
    private static float startX, maxX, goalX;
    private static String status="Human control";
    private static long stalledTicks;
    private static MotorRecovery recovery=new MotorRecovery();
    private static PowerupPlanner planner=new PowerupPlanner();
    private static PowerupPlanner.Goal goal;
    private static PlayMemory memory=new PlayMemory(null);
    private static String levelKey="unspecified",context="",outcome="interrupted";
    private static long startedTick;
    private static int pickups,damage;
    private static final java.util.Map<String,PlayMemory.Strategy> choices=new java.util.LinkedHashMap<>();
    private static final java.util.Map<String,Double> rewards=new java.util.LinkedHashMap<>();
    public static void project(java.nio.file.Path project) {memory=new PlayMemory(project.resolve("brain/play-memory-v1.json"));}
    public static void prepareLevel(String authored) {levelKey=PlayMemory.fingerprint(authored);}
    public static PlayMemory.Level experience() {return memory.level(levelKey);}
    public static String lastOutcome(){return outcome;}
    public static String learningStatus() {
        var learned=experience();
        return "Experience: "+learned.episodes+" runs | best "+String.format(java.util.Locale.ROOT,"%.2f",learned.bestProgress)
            +" units | "+learned.pickups+" pickups | "+learned.deaths+" deaths | "+memory.ioStatus();
    }
    private static void reward(double value) {if(automatic && !context.isEmpty()) rewards.merge(context,value,Double::sum);}
    public static void powerup(GameObject player) {if(automatic && player.getPersistentId().equals(actor)) {pickups++;reward(6);}}
    public static void damage(GameObject player) {if(automatic && player.getPersistentId().equals(actor)) {damage++;reward(-5);}}
    private static boolean neuralJump;
    private static float[] lastSensory=new float[0];
    private static ActorControls.Input current=ActorControls.Input.NONE, previous=ActorControls.Input.NONE;
    private static ActorControls.Input externalInput;
    public static void external(float horizontal,boolean jump){externalInput=new ActorControls.Input(horizontal,0,jump,false);}
    public static void externalOff(){externalInput=null;}
    private GameInput() {}
    public static boolean automatic() { return automatic; }
    public static String status() { return status; }
    public static float progress() { return Math.max(0,Math.abs(goalX-startX)-Math.abs(goalX-maxX)); }
    public static boolean neuralJump() { return neuralJump; }
    public static void neuralJump(boolean value) { neuralJump=value; }
    public static float[] sensory() { return lastSensory.clone(); }
    public static void requestJump() { if (automatic) { jumpUntil=tick+27; lastJump=tick; } }
    public static void start(Scene scene, float targetX) { start(scene,targetX,"safe117"); }
    public static void start(Scene scene, float targetX, boolean guarded) {
        start(scene,targetX,guarded?"landing116":"legacy115");
    }
    public static void start(Scene scene, float targetX, String policy) {
        if(!java.util.Set.of("legacy115","landing116","safe117","risk118","recover119").contains(policy))throw new IllegalArgumentException("Unknown game policy");
        policyName=policy;contactAware=policy.equals("recover119");contactRecovery=new ContactRecovery119();contactChecks=contactNanos=contactOverrides=0;pickupDeferrals=0;riskAware=policy.equals("risk118")||contactAware;safeCollection=policy.equals("safe117")||riskAware;pickupRiskChecks=pickupRiskNanos=0;pickupSkips=0;pickupDecisions.clear();fallingWaitTicks=0;pursuit=null;
        boolean guarded=!policy.equals("legacy115");
        landingGuard=guarded;landingChecks=landingOverrides=landingNanos=0;landingDecision=null;edgeUntil=edgeCrossings=0;edgeLeftGround=false;
        GameObject player=scene.getGameObjectWith(PlayerController.class);
        if (player==null || !Float.isFinite(targetX)) throw new IllegalArgumentException("A live player and finite target are required");
        if(automatic) stop("Previous AI segment ended for a new target");
        episode=UUID.randomUUID(); actor=player.getPersistentId(); sequence=0;
        controls.reset(episode,actor); automatic=true; startX=maxX=player.transform.position.x; goalX=targetX;
        current=previous=ActorControls.Input.NONE; jumpUntil=0; lastJump=-100; stalledTicks=0;
        recovery=new MotorRecovery();
        planner=new PowerupPlanner(id->experience().failedGoals.getOrDefault(id.toString(),0)>0,
            id->experience().failedGoals.merge(id.toString(),1,Integer::sum));
        goal=null;choices.clear();rewards.clear();context="";outcome="interrupted";
        pickups=damage=0;startedTick=scene.tick();
        status="Playing toward x="+goalX+" using local geometry and normal controls";
        nervous.BrainRuntime.start();
    }
    public static void stop(String reason) {
        if(automatic) {
            for(var entry:choices.entrySet()) memory.credit(levelKey,entry.getKey(),entry.getValue(),rewards.getOrDefault(entry.getKey(),0d));
            var names=new java.util.LinkedHashMap<String,String>();choices.forEach((key,value)->names.put(key,value.name()));
            memory.record(levelKey,new PlayMemory.Result(experience().episodes+1,outcome,progress(),pickups,damage,
                Math.max(0,tick-startedTick),names));
        }
        automatic=false; controls.release(); current=previous=ActorControls.Input.NONE; status=reason;
    }
    public static void tick(Scene scene) {
        tick=scene.tick(); previous=current;
        if (!automatic) { current=ActorControls.Input.NONE; return; }
        GameObject player=scene.getGameObjectWith(PlayerController.class);
        if (player==null || !player.getPersistentId().equals(actor)) { stop("Player changed; control released"); return; }
        PlayerController controller=player.getComponent(PlayerController.class);
        if (controller.isDead()) { outcome="death";reward(-8);stop("Player died; outcome saved for the next run"); return; }
        if (controller.hasWon()) { outcome="win";reward(8);stop("Flag reached; winning outcome saved"); return; }
        float x=player.transform.position.x, y=player.transform.position.y;
        if (Math.abs(x-goalX)<.15f) { outcome="target";reward(4);stop("Target reached at x="+x+"; experience saved"); return; }
        if (Math.abs(goalX-x)<Math.abs(goalX-maxX)-.002f) {
            reward((Math.abs(goalX-maxX)-Math.abs(goalX-x))*2);maxX=x; stalledTicks=0;
        } else stalledTicks++;
        if (stalledTicks>1500) { outcome="stalled";reward(-5);stop("No progress for 25s; failure saved for next run"); return; }
        var nextGoal=planner.step(scene,player,goalX,tick);
        int assessed=0;boolean accepted=false;
        if(riskAware)for(;assessed<4&&nextGoal.kind()!=PowerupPlanner.Kind.TRAVEL;assessed++){
            long checked=System.nanoTime();var risk=PickupObserver118.assess(scene,player,nextGoal);pickupRiskChecks++;pickupRiskNanos+=System.nanoTime()-checked;
            if(risk.pursue()){accepted=true;break;}
            boolean deferred=contactAware&&risk.reason().equals("Enemy motion intersects the pickup approach")&&planner.defer(nextGoal,tick);
            if(deferred)pickupDeferrals++;else pickupSkips++;if(pickupDecisions.size()<16){var row=new java.util.LinkedHashMap<String,Object>();row.put("tick",tick);row.put("objectId",nextGoal.object().getPersistentId().toString());row.put("kind",nextGoal.kind().name());row.put("targetX",nextGoal.x());row.put("targetY",nextGoal.y());row.put("reason",risk.reason());if(contactAware)row.put("action",deferred?"deferred for fresh observation":"declined for run");if(Float.isFinite(risk.supportY()))row.put("supportY",risk.supportY());row.put("clearance",risk.clearance());pickupDecisions.add(row);}
            if(!deferred)planner.declineForRun(nextGoal);nextGoal=planner.step(scene,player,goalX,tick);
        }
        if(riskAware && !accepted && assessed==4 && nextGoal.kind()!=PowerupPlanner.Kind.TRAVEL) nextGoal=new PowerupPlanner.Goal(PowerupPlanner.Kind.TRAVEL,null,goalX,y,"Pickup assessment budget reached; continue toward destination");
        if(goal==null || goal.kind()!=nextGoal.kind() || goal.object()!=nextGoal.object()) recovery=new MotorRecovery();
        goal=nextGoal;
        context=((int)Math.floor(x/2))+":"+(controller.isSmall()?"small":controller.isFire()?"fire":"big")+":"+goal.kind();
        var strategy=choices.computeIfAbsent(context,key->memory.choose(levelKey,key));
        reward(-.001);recovery.jumpTicks(strategy.jumpTicks);
        float localTarget=goal.x();
        pursuit=null;
        if(safeCollection&&goal.kind()==PowerupPlanner.Kind.COLLECT&&goal.object()!=null){
            var item=goal.object().getComponent(physics2d.components.Rigidbody2D.class);
            if(item!=null){pursuit=Pursuit117.choose(goal.x(),goal.y(),item.getVelocity().x,item.getVelocity().y,goal.object().getComponent(MushroomAI.class)!=null);localTarget=pursuit.targetX();}
        }
        float direction=localTarget>=x?1:-1;
        var floor=scene.physics().raycastSolid(player,new Vector2f(x+direction*.32f,y+.04f),new Vector2f(x+direction*.32f,y-.5f));
        var wall=scene.physics().raycastSolid(player,new Vector2f(x,y-.04f),new Vector2f(x+direction*strategy.wallLookahead,y-.04f));
        var behind=scene.physics().raycastSolid(player,new Vector2f(x-direction*.3f,y),new Vector2f(x-direction*.3f,y-.55f));
        var head=scene.physics().raycastSolid(player,new Vector2f(x,y+.13f),new Vector2f(x,y+1.1f));
        var enemy=scene.getGameObjects().stream().filter(o -> !o.isDead()
            &&(o.getComponent(GoombaAI.class)!=null || o.getComponent(TurtleAI.class)!=null)
            &&o.getComponent(physics2d.components.Rigidbody2D.class)!=null && !o.getComponent(physics2d.components.Rigidbody2D.class).isSensor()
            &&Math.abs(o.transform.position.x-x)<strategy.enemyLookahead && y-o.transform.position.y>-.5f && y-o.transform.position.y<2)
            .min(java.util.Comparator.comparingDouble(o->Math.abs(o.transform.position.x-x))).orElse(null);
        boolean hazard=enemy!=null && (enemy.transform.position.x-x)*direction>-.1f && Math.abs(enemy.transform.position.y-y)<.6f;
        if(riskAware && hazard && controller.isSmall()){
            float closing=(player.getComponent(physics2d.components.Rigidbody2D.class).getVelocity().x-enemy.getComponent(physics2d.components.Rigidbody2D.class).getVelocity().x)*direction;
            hazard=EnemyApproach118.jumpNow(Math.abs(enemy.transform.position.x-x),closing);
        }
        boolean obstacle=wall.hit && Math.abs(wall.normal.x)>.5f;
        var body=player.getComponent(physics2d.components.Rigidbody2D.class);
        lastSensory=new float[]{Math.max(-1,Math.min(1,(goalX-x)/5)),body.getVelocity().x/3,body.getVelocity().y/3,
            controller.onGround?1:-1,floor.hit?1:-1,obstacle?1:-1,hazard?1:-1};
        nervous.BrainRuntime.observe(lastSensory);
        boolean ceiling=head.hit && (!landingGuard || head.hitObject==null || (head.hitObject.getComponent(MushroomAI.class)==null && head.hitObject.getComponent(Flower.class)==null));
        boolean wedged=stalledTicks>45 && Math.abs(body.getVelocity().y)<.05f;
        var decision=recovery.step(tick-startedTick,Math.abs(localTarget-x),direction,controller.onGround,obstacle,!floor.hit,hazard,ceiling,behind.hit,wedged);
        status="AI "+decision.phase()+" | "+goal.reason()+" | "+strategy+" | recovery "+decision.attempts()+"/3";
        ContactRecovery119.Decision contactDecision=null;
        if(contactAware){
            long began=System.nanoTime();var observed=ContactObserver119.read(player,direction);
            boolean rearThreat=enemy!=null&&(enemy.transform.position.x-x)*direction<0&&Math.abs(enemy.transform.position.x-x)<.9f;
            contactDecision=contactRecovery.step(tick,x,y,direction,observed.support(),observed.opposing(),controller.onGround,ContactObserver119.retreatSupported(player,direction),ceiling,rearThreat);contactChecks++;contactNanos+=System.nanoTime()-began;
        }
        if(decision.phase()==MotorRecovery.Phase.BLOCKED && (contactDecision==null||!contactDecision.override())) {
            reward(-5);
            if(goal.kind()!=PowerupPlanner.Kind.TRAVEL) {
                planner.abandon(goal);goal=null;recovery=new MotorRecovery();jumpUntil=0;
                current=ActorControls.Input.NONE;status="Optional power-up route blocked; replanning toward destination";return;
            }
            outcome="blocked";stop("Blocked; failed strategy saved. Human control released.");return;
        }
        if(neuralJump && controller.onGround && tick-lastJump>35 && nervous.BrainRuntime.core()!=null
            && nervous.BrainRuntime.core().output(0)>.1f) { jumpUntil=tick+12; lastJump=tick; }
        float horizontal=decision.horizontal();
        boolean objectiveJump=false;
        if(goal.kind()!=PowerupPlanner.Kind.TRAVEL && !hazard && !obstacle) {
            float error=localTarget-x-body.getVelocity().x*.18f;
            horizontal=Math.abs(error)<.04f?0:Math.signum(error);
            if(goal.kind()==PowerupPlanner.Kind.BUMP_BLOCK && Math.abs(localTarget-x)<.19f && controller.onGround
                && tick-lastJump>40) {jumpUntil=tick+22;lastJump=tick;}
            if(goal.kind()==PowerupPlanner.Kind.COLLECT && goal.y()>y+.25f && Math.abs(localTarget-x)<1.2f
                && controller.onGround && tick-lastJump>40 && !ceiling && (pursuit==null||pursuit.allowJump())) {jumpUntil=tick+strategy.jumpTicks;lastJump=tick;}
            if(pursuit!=null&&!pursuit.allowJump()){jumpUntil=0;fallingWaitTicks++;status+=" | "+pursuit.reason();}
            objectiveJump=tick<jumpUntil;
        }
        if(goal.kind()==PowerupPlanner.Kind.TRAVEL && enemy!=null && !controller.onGround && body.getVelocity().y<-.1f && y>enemy.transform.position.y+.25f
            && Math.abs(enemy.transform.position.x-x)<1.5f && (!contactAware||(enemy.transform.position.x-x)*direction>.1f)) {
            float height=y-.13f-(enemy.transform.position.y+.12f),fallSpeed=-body.getVelocity().y;
            float landingTime=Math.min(.5f,Math.max(.02f,((float)Math.sqrt(fallSpeed*fallSpeed+14*Math.max(0,height))-fallSpeed)/7));
            float target=enemy.transform.position.x+enemy.getComponent(physics2d.components.Rigidbody2D.class).getVelocity().x*landingTime;
            float error=target-(x+body.getVelocity().x*landingTime);
            horizontal=Math.abs(error)<.04f?0:Math.signum(error);status+=" | lining up landing";
        }
        if(landingGuard){
            if(edgeUntil>tick){
                if(!controller.onGround)edgeLeftGround=true;
                if(edgeLeftGround&&controller.onGround)edgeUntil=0;
            }
            if(controller.onGround&&edgeUntil<=tick&&!ceiling){
                float travel=Math.abs(body.getVelocity().x)>.15f?Math.signum(body.getVelocity().x):Math.signum(goalX-x);
                float stopping=body.getVelocity().x*body.getVelocity().x/(2*(controller.walkSpeed+60*controller.slowDownForce));
                float look=x+travel*Math.max(.4f,stopping+.15f);
                var support=scene.physics().raycastSolid(player,new Vector2f(look,y+.04f),new Vector2f(look,y-.55f));
                if(!support.hit){
                    edgeDirection=travel;edgeUntil=tick+90;edgeLeftGround=false;edgeCrossings++;
                    jumpUntil=tick+28;lastJump=tick;
                    if(goal.kind()!=PowerupPlanner.Kind.TRAVEL){planner.abandon(goal);goal=null;recovery=new MotorRecovery();}
                    status+=" | defer unsafe chase; cross observed edge";
                }
            }
            if(edgeUntil>tick)horizontal=edgeDirection;
        }
        boolean landingCorrection=false;
        if (landingGuard && !controller.onGround && body.getVelocity().y < -.05f) {
            long forecastStarted=System.nanoTime();
            landingDecision=LandingGuard116.choose(x,y,body.getVelocity().x,body.getVelocity().y,
                controller.isSmall()?.13f:.21f,controller.walkSpeed,scene.physics().getGravity().y*.7f,
                controller.terminalVelocity.x,controller.terminalVelocity.y,controller.slowDownForce,
                horizontal,goalX,(px,top,bottom)->{
                    var hit=scene.physics().raycastSolid(player,new Vector2f(px,top),new Vector2f(px,bottom));
                    return hit.hit&&hit.normal.y>.5f?hit.point.y:Float.NaN;
                });
            landingChecks++;landingNanos+=System.nanoTime()-forecastStarted;
            if(landingDecision.override()){landingCorrection=true;horizontal=landingDecision.horizontal();jumpUntil=0;objectiveJump=false;landingOverrides++;status+=" | landing correction";}
        }
        boolean jump=!landingCorrection&&(decision.jump()||objectiveJump||tick<jumpUntil);
        if(contactDecision!=null&&contactDecision.override()){horizontal=contactDecision.horizontal();jump=contactDecision.jump();jumpUntil=0;edgeUntil=0;recovery=new MotorRecovery();contactOverrides++;status="AI contact recovery | "+contactDecision.reason();}
        ActorControls.Input input=new ActorControls.Input(horizontal,0,jump,tick%30==0);
        controls.accept(new ActorControls.Lease(episode,actor,sequence++,tick,tick+1,input),tick);
        current=controls.at(tick);
    }
    public static java.util.Map<String,Object> observation116() {
        var result=new java.util.LinkedHashMap<String,Object>();result.put("policy",policyName);result.put("automatic",automatic);result.put("status",status);result.put("horizontal",current.horizontal());result.put("jump",current.jump());result.put("goal",goal==null?"none":goal.kind().name());result.put("pickups",pickups);result.put("damage",damage);result.put("edgeCrossings",edgeCrossings);result.put("landingChecks",landingChecks);result.put("landingOverrides",landingOverrides);result.put("landingNanos",landingNanos);result.put("fallingWaitTicks",fallingWaitTicks);result.put("pickupSkips",pickupSkips);result.put("pickupRiskChecks",pickupRiskChecks);result.put("pickupRiskNanos",pickupRiskNanos);result.put("pickupDecisions",new java.util.ArrayList<>(pickupDecisions));
        if(contactAware){result.put("contactRecovery119",contactRecovery.observation());result.put("contactChecks",contactChecks);result.put("contactNanos",contactNanos);result.put("contactOverrides",contactOverrides);result.put("pickupDeferrals",pickupDeferrals);}
        if(pursuit!=null){result.put("interceptX",pursuit.targetX());result.put("pursuitReason",pursuit.reason());}
        if(goal!=null){result.put("goalX",goal.x());result.put("goalY",goal.y());if(goal.object()!=null){var rb=goal.object().getComponent(physics2d.components.Rigidbody2D.class);if(rb!=null){result.put("goalVx",rb.getVelocity().x);result.put("goalVy",rb.getVelocity().y);}}}
        if(landingDecision!=null){result.put("landingReason",landingDecision.reason());result.put("predictedLandingX",landingDecision.planned().x());result.put("predictedLandingY",landingDecision.planned().y());result.put("predictedLandingTicks",landingDecision.planned().ticks());}
        return result;
    }
    public static boolean isKeyPressed(int key) {
        if (!automatic&&externalInput==null) return KeyListener.isKeyPressed(key);
        var selected=externalInput==null?current:externalInput;
        return switch(key) {
            case GLFW_KEY_RIGHT,GLFW_KEY_D -> selected.horizontal()>0;
            case GLFW_KEY_LEFT,GLFW_KEY_A -> selected.horizontal()<0;
            case GLFW_KEY_UP,GLFW_KEY_W -> selected.vertical()>0;
            case GLFW_KEY_DOWN,GLFW_KEY_S -> selected.vertical()<0;
            case GLFW_KEY_SPACE -> selected.jump();
            case GLFW_KEY_E -> selected.fire();
            default -> false;
        };
    }
    public static boolean keyBeginPress(int key) {
        if(externalInput!=null)return false;
        if (!automatic) return KeyListener.keyBeginPress(key);
        return key==GLFW_KEY_E && current.fire() && !previous.fire();
    }
}
