package agent;

import components.*;
import jade.*;
import scenes.Scene;
import org.junit.Test;
import static org.junit.Assert.*;

public class PowerupPlannerTest {
    @Test public void identifiesMushroomAndTimesOutUnreachablePickup() {
        var scene=new Scene(null);var player=scene.createGameObject("Mario");player.addComponent(new PlayerController());scene.addGameObjectToScene(player);
        var mushroom=scene.createGameObject("Mushroom");mushroom.addComponent(new MushroomAI());mushroom.transform.position.set(1,.25f);scene.addGameObjectToScene(mushroom);
        var planner=new PowerupPlanner();
        assertEquals(PowerupPlanner.Kind.COLLECT,planner.step(scene,player,20,1).kind());
        assertEquals(PowerupPlanner.Kind.TRAVEL,planner.step(scene,player,20,482).kind());
        assertEquals(PowerupPlanner.Kind.TRAVEL,planner.step(scene,player,20,483).kind());
    }
    @Test public void recognizesActivePowerupBlockAndStopsTargetingAfterUse() {
        var scene=new Scene(null);var player=scene.createGameObject("Mario");player.addComponent(new PlayerController());scene.addGameObjectToScene(player);
        var block=scene.createGameObject("Powerup block");var question=new com.google.gson.Gson().fromJson("{\"blockType\":\"Powerup\"}",QuestionBlock.class);
        block.addComponent(question);block.transform.position.set(1,.8f);scene.addGameObjectToScene(block);
        var planner=new PowerupPlanner();assertEquals(PowerupPlanner.Kind.BUMP_BLOCK,planner.step(scene,player,20,1).kind());
        question.setInactive();assertEquals(PowerupPlanner.Kind.TRAVEL,planner.step(scene,player,20,2).kind());
    }
    @Test public void laterRunAvoidsPreviouslyUnreachableOptionalBlock() {
        var scene=new Scene(null);var player=scene.createGameObject("Mario");player.addComponent(new PlayerController());scene.addGameObjectToScene(player);
        var block=scene.createGameObject("Powerup block");block.addComponent(new com.google.gson.Gson().fromJson("{\"blockType\":\"Powerup\"}",QuestionBlock.class));
        block.transform.position.set(1,.8f);scene.addGameObjectToScene(block);
        java.util.Set<java.util.UUID> failed=new java.util.HashSet<>();
        var first=new PowerupPlanner(failed::contains,failed::add);first.abandon(first.step(scene,player,20,1));
        var next=new PowerupPlanner(failed::contains,failed::add);
        assertEquals(PowerupPlanner.Kind.TRAVEL,next.step(scene,player,20,1).kind());
    }
}
