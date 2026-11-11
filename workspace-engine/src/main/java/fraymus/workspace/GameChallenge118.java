package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Declared source-component construction, verified again before every launch. */
final class GameChallenge118 {
    static final String RECIPE="mario-challenge118-v1";
    static final List<String> FIELDS=List.of("goombaTile","turtleTile","obstacleTile","obstacleHeight","pickupTile");
    static boolean active(JSONObject p){return FIELDS.stream().anyMatch(p::has);}
    static JSONObject parameters(JSONObject in){
        var base=WorldFrames.copy(in);for(String k:FIELDS)base.remove(k);
        var p=GameLevels114.parameters115(base);int length=p.getInt("lengthTiles");
        for(String k:FIELDS){int v=GameLevels114.integer(in,k,0,0,k.equals("obstacleHeight")?3:length-8);if(!k.equals("obstacleHeight")&&v>0&&v<6)throw new IllegalArgumentException(k+" must be zero or at least6");p.put(k,v);}
        int obstacle=p.getInt("obstacleTile"),height=p.getInt("obstacleHeight");
        if((obstacle==0)!=(height==0))throw new IllegalArgumentException("Choose both obstacleTile and obstacleHeight, or zero for both");
        int a=16+p.getInt("seed")%3,b=32+(p.getInt("seed")/3)%3;
        for(String k:List.of("goombaTile","turtleTile","obstacleTile")){
            int x=p.getInt(k);if(x>0&&(x>=a&&x<a+p.getInt("gapTiles")||x>=b&&x<b+p.getInt("secondGapTiles")))throw new IllegalArgumentException(k+" requires original floor support");
        }
        int goomba=p.getInt("goombaTile"),turtle=p.getInt("turtleTile");
        if(goomba>0&&(goomba==turtle||goomba==obstacle)||turtle>0&&turtle==obstacle)throw new IllegalArgumentException("Initial enemy/obstacle bodies may not overlap");
        return p;
    }
    static void append(JSONArray source,JSONArray objects,JSONArray lineage,String id,JSONObject p){
        if(!active(p))return;
        int block=p.getInt("pickupTile");if(block>0)GameLevels114.prop(GameLevels114.first(objects,"components.QuestionBlock"),"jade.Transform").getJSONObject("position").put("x",block*.25+.125);
        int obstacle=p.getInt("obstacleTile");for(int h=0;h<p.getInt("obstacleHeight");h++)GameLevels114.clone(objects,lineage,GameLevels114.first(source,"components.Ground"),id,"Obstacle "+h,obstacle*.25+.125,.625+h*.25);
        if(p.getInt("goombaTile")>0)GameLevels114.clone(objects,lineage,GameLevels114.first(source,"components.GoombaAI"),id,"Goomba",p.getInt("goombaTile")*.25+.125,.625);
        if(p.getInt("turtleTile")>0)GameLevels114.clone(objects,lineage,GameLevels114.first(source,"components.TurtleAI"),id,"Turtle",p.getInt("turtleTile")*.25+.125,.675);
    }
}
