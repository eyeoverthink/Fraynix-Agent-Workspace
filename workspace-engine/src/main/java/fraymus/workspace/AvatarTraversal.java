package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;

final class AvatarTraversal {
    static final int BANK_Y=288, CELL=24, PLAYER_WIDTH=24, PLAYER_HEIGHT=32;
    static final int SPAWN_X=48, GOAL_X=672;
    private AvatarTraversal() {}

    static boolean requested(JSONObject brief) {
        return brief!=null && brief.optInt("gaps")+brief.optInt("hazards")>0;
    }

    static JSONObject design(String title,JSONObject player,JSONObject brief,String coin,String ground,String rail) {
        int gapCount=brief.getInt("gaps"), hazardCount=brief.getInt("hazards"), hurdles=brief.getInt("hurdles");
        int total=gapCount+hazardCount+hurdles;
        boolean staggered=brief.getString("spacing").equals("staggered");
        int[] slots=switch(total) {
            case 1 -> new int[]{staggered?240:288};
            case 2 -> new int[]{168,staggered?432:456};
            default -> staggered||gapCount>1&&brief.getBoolean("wideGap") ?
                    new int[]{144,384,528}:new int[]{144,360,528};
        };
        var gaps=new JSONArray();
        var objects=new JSONArray();
        for(int i=0;i<gapCount;i++){
            int width=i==0&&brief.getBoolean("wideGap")?216:72;
            if(i+1<gapCount && slots[i]+width>=slots[i+1])
                throw new IllegalArgumentException("Leave a bank between the wide gap and the next gap.");
            gaps.put(new JSONObject().put("id","gap-"+(i+1)).put("x",slots[i]).put("width",width));
        }
        for(int x=0;x<720;x+=CELL) if(!insideGap(x,gaps)) objects.put(bank(ground,x));
        for(int i=0;i<hazardCount;i++){
            int size=i==0&&brief.getBoolean("tallHazard")?120:24;
            objects.put(AvatarLevelMission.item("hazard-"+(i+1),rail,"foreground",slots[gapCount+i],BANK_Y-size,size)
                    .put("effect","hazard"));
        }
        for(int i=0;i<hurdles;i++){
            int x=slots[gapCount+hazardCount+i];
            int size=i==0&&brief.getBoolean("oversized")?120:36;
            objects.put(AvatarLevelMission.item("hurdle-"+(i+1),rail,"terrain",x,BANK_Y-size,size)
                    .put("effect","brick").put("collider",new JSONObject().put("x",0).put("y",0).put("w",size).put("h",size)));
        }
        int[] pockets={54,77,100,605,627,648};
        int pickups=brief.getInt("pickups");
        for(int i=0;i<pickups;i++){
            int slot=pickups==1?0:(int)Math.round((double)i*(pockets.length-1)/(pickups-1));
            int y=brief.getBoolean("elevated")&&i==pickups/2?54:BANK_Y-CELL-1;
            objects.put(AvatarLevelMission.item("coin-"+(i+1),coin,"pickup",pockets[slot],y,CELL).put("effect","coin"));
        }
        var palette=new JSONObject().put("sky","#142a46").put("water","#28495a").put("ground","#468f84")
                .put("rock","#315b83").put("accent","#ffd77b");
        var stage=new JSONObject().put("sourceSha256",WorldFrames.hash(brief)).put("palette",palette)
                .put("spawn",new JSONObject().put("x",SPAWN_X).put("y",BANK_Y-PLAYER_HEIGHT))
                .put("goal",new JSONObject().put("x",GOAL_X).put("y",BANK_Y-CELL));
        return new JSONObject().put("title",title).put("player",player).put("course",AvatarLevelMission.JUMP)
                .put("stage",stage).put("objects",objects).put("traversal",new JSONObject().put("gaps",gaps));
    }

    static JSONObject checked(JSONObject raw, JSONObject stage, JSONArray objects) {
        if (stage==null) throw new IllegalArgumentException("A gap needs real stage banks.");
        SpatialScene.keys(raw,"gaps");
        var gaps = new JSONArray();
        int last=0;
        var ids=new HashSet<String>();
        for(Object value:raw.getJSONArray("gaps")){
            var entry=(JSONObject)value;
            SpatialScene.keys(entry,"id","x","width");
            String id=entry.getString("id");
            int x=SpriteWorkshop.integer(entry,"x",96,576), width=SpriteWorkshop.integer(entry,"width",48,216);
            if(!id.matches("gap-[1-2]") || !ids.add(id) || x%CELL!=0 || width%CELL!=0 ||
                    x<=last || x+width>648 || gaps.length()>=2)
                throw new IllegalArgumentException("Use up to two separated gaps on the stage grid.");
            last=x+width;
            gaps.put(new JSONObject().put("id",id).put("x",x).put("width",width));
        }
        var banks=new HashSet<Integer>();
        for(Object rawItem:objects){
            var item=(JSONObject)rawItem;
            if(!"ground".equals(item.optString("effect")))continue;
            int x=item.getInt("x");
            if(item.getInt("y")!=BANK_Y || item.getInt("size")!=CELL || !item.getJSONObject("collider").similar(bankCollider())
                    || !item.getString("id").equals("bank-"+x) || !banks.add(x))
                throw new IllegalArgumentException("Invalid gap bank or collider.");
        }
        for(int x=0;x<720;x+=CELL) {
            if(banks.contains(x)==insideGap(x,gaps))
                throw new IllegalArgumentException("The declared gap differs from the measured bank geometry.");
        }
        return new JSONObject().put("gaps",gaps);
    }

    static JSONObject diagnoseReset(JSONObject layout,JSONArray rows) {
        var gaps=layout.getJSONObject("traversal").getJSONArray("gaps");
        int start=1,takeoff=0;
        for(int i=1;i<rows.length();i++){
            var rowAtTick=rows.getJSONArray(i);
            for(Object raw:rowAtTick.getJSONArray(17)){
                if("jump".equals(((JSONArray)raw).getString(0)))takeoff=i;
            }
            var reset=resetEvent(rowAtTick);
            if(reset==null)continue;
            var previous=rows.getJSONArray(i-1);
            String hit=reset.getString(1);
            if(!hit.isEmpty()){
                var hazard=AvatarCourseDesign.find(layout.getJSONArray("objects"),hit);
                if(hazard!=null && "hazard".equals(hazard.optString("effect")))
                    return new JSONObject().put("kind","hit-hazard").put("objectId",hit).put("resetTick",i).put("takeoffTick",takeoff)
                            .put("before",AvatarCourseDesign.geometry(hazard)).put("lastX",previous.getDouble(1));
            }else{
                for(int j=start;j<i;j++){
                    var row=rows.getJSONArray(j);
                    if(row.getDouble(2)+PLAYER_HEIGHT<=BANK_Y+.01||row.getDouble(4)<=0)continue;
                    for(Object raw:gaps){
                        var gap=(JSONObject)raw;
                        if(overlap(row.getDouble(1),gap.getInt("x"),gap.getInt("width")))
                            return new JSONObject().put("kind","fell-in-gap").put("objectId",gap.getString("id"))
                                    .put("resetTick",i).put("takeoffTick",takeoff).put("fallTick",j).put("lastX",previous.getDouble(1))
                                    .put("before",new JSONObject().put("x",gap.getInt("x")).put("width",gap.getInt("width")));
                    }
                }
            }
            start=i+1;
            takeoff=0;
        }
        return null;
    }

    static JSONObject adjust(JSONObject layout,JSONObject diagnosis) {
        String id=diagnosis.getString("objectId");
        var objects=layout.getJSONArray("objects");
        if("hit-hazard".equals(diagnosis.getString("kind"))){
            var hazard=AvatarCourseDesign.find(objects,id);
            if(hazard==null||!AvatarCourseDesign.geometry(hazard).similar(diagnosis.getJSONObject("before")))
                throw new IllegalArgumentException("The failed hazard geometry changed.");
            int previous=hazard.getInt("size");
            if(previous<=24)return null;
            int size=previous>48?24:Math.max(24,previous-12);
            hazard.put("size",size).put("y",BANK_Y-size);
            return new JSONObject().put("layout",layout).put("after",AvatarCourseDesign.geometry(hazard));
        }
        var ordered=layout.getJSONObject("traversal").getJSONArray("gaps");
        var gap=AvatarCourseDesign.find(ordered,id);
        var before=diagnosis.getJSONObject("before");
        if(gap==null || gap.getInt("x")!=before.getInt("x") || gap.getInt("width")!=before.getInt("width"))
            throw new IllegalArgumentException("The failed gap geometry changed.");
        int width=gap.getInt("width");
        if(width<=48)return null;
        int x=gap.getInt("x"), next=width>96?96:width-CELL;
        String asset=AvatarCourseDesign.find(objects,"bank-0").getString("assetId");
        for(int bank=x+next;bank<x+width;bank+=CELL) objects.put(bank(asset,bank));
        gap.put("width",next);
        return new JSONObject().put("layout",layout).put("after",new JSONObject().put("x",x).put("width",next));
    }

    static boolean supported(JSONArray row) { return row.getString(5).startsWith("bank-"); }

    static JSONObject crossings(JSONObject layout,JSONArray rows){
        var clearedGaps=new JSONArray();
        for(Object raw:layout.getJSONObject("traversal").getJSONArray("gaps")){
            var gap=(JSONObject)raw;
            int x=gap.getInt("x"), right=x+gap.getInt("width"), over=-1, landing=-1;
            for(int i=1;i<rows.length();i++){
                var row=rows.getJSONArray(i);
                if(over<0 && row.getDouble(1)>x && row.getDouble(1)+PLAYER_WIDTH<right
                        && row.getDouble(2)+PLAYER_HEIGHT<BANK_Y-.5)over=i;
                if(over>=0 && row.getDouble(1)+PLAYER_WIDTH>right && supported(row)
                        && Integer.parseInt(row.getString(5).substring(5))>=right){landing=i;break;}
            }
            if(landing<0)throw new IllegalArgumentException("A gap lacked an airborne crossing and far-bank landing.");
            clearedGaps.put(new JSONObject().put("id",gap.getString("id")).put("overTick",over).put("landTick",landing));
        }
        var clearedHazards=new JSONArray();
        for(Object raw:layout.getJSONArray("objects")){
            var hazard=(JSONObject)raw;
            if(!"hazard".equals(hazard.optString("effect")))continue;
            int first=-1, last=-1;
            for(int i=1;i<rows.length();i++){
                var row=rows.getJSONArray(i);
                if(!overlap(row.getDouble(1),hazard.getInt("x"),hazard.getInt("size")))continue;
                if(row.getDouble(2)+PLAYER_HEIGHT>hazard.getInt("y")+.001)
                    throw new IllegalArgumentException("A hazard overlapped the player on its recorded crossing.");
                if(first<0)first=i;
                last=i;
            }
            if(last<0 || last+1>=rows.length() || rows.getJSONArray(last+1).getDouble(1)<hazard.getInt("x")+hazard.getInt("size"))
                throw new IllegalArgumentException("The playtest did not pass the hazard.");
            clearedHazards.put(new JSONObject().put("id",hazard.getString("id")).put("firstTick",first).put("lastTick",last));
        }
        return new JSONObject().put("gaps",clearedGaps).put("hazards",clearedHazards);
    }

    private static JSONArray resetEvent(JSONArray row){
        for(Object raw:row.getJSONArray(17)){
            var event=(JSONArray)raw;
            if("reset".equals(event.getString(0)))return event;
        }
        return null;
    }

    private static boolean overlap(double x,int at,int width){return x<at+width&&x+PLAYER_WIDTH>at;}

    private static boolean insideGap(int x,JSONArray gaps){
        for(Object raw:gaps){
            var gap=(JSONObject)raw;
            int left=gap.getInt("x");
            if(x>=left&&x<left+gap.getInt("width"))return true;
        }
        return false;
    }

    private static JSONObject bank(String asset,int x){
        return AvatarLevelMission.item("bank-"+x,asset,"terrain",x,BANK_Y,CELL)
                .put("kind","tile").put("effect","ground").put("collider",bankCollider());
    }

    private static JSONObject bankCollider(){
        return new JSONObject().put("x",0).put("y",0).put("w",CELL).put("h",CELL);
    }
}
