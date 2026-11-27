package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

final class AvatarCourseDesign {
    static final int MAX_REPAIRS = 4;
    private static final int FLOOR = 312, PLAYER_HEIGHT = 32, PLAYER_WIDTH = 24, PICKUP_SIZE = 24;

    private AvatarCourseDesign() {}

    static JSONObject checked(JSONObject input, String course) {
        SpatialScene.keys(input,"hurdles","pickups","spacing","oversized","elevated","locked","pauseRepair","gaps","hazards","wideGap","tallHazard");
        int hurdles = SpriteWorkshop.integer(input,"hurdles",0,3);
        int pickups = SpriteWorkshop.integer(input,"pickups",1,6);
        String spacing = input.getString("spacing");
        if (!Set.of("even","staggered").contains(spacing))
            throw new IllegalArgumentException("Choose even or staggered spacing.");
        var brief = new JSONObject().put("hurdles",hurdles).put("pickups",pickups).put("spacing",spacing);
        for (String flag : new String[]{"oversized","elevated","locked","pauseRepair"}) {
            if (!(input.get(flag) instanceof Boolean)) throw new IllegalArgumentException("Invalid " + flag + " option.");
            brief.put(flag,input.getBoolean(flag));
        }
        int gaps=0,hazards=0;
        if (input.has("gaps") || input.has("hazards") || input.has("wideGap") || input.has("tallHazard")) {
            gaps=SpriteWorkshop.integer(input,"gaps",0,2);
            hazards=SpriteWorkshop.integer(input,"hazards",0,2);
            if (!(input.get("wideGap") instanceof Boolean) || !(input.get("tallHazard") instanceof Boolean))
                throw new IllegalArgumentException("Invalid gap or hazard option.");
            if (gaps+hazards==0 || gaps+hazards+hurdles>3)
                throw new IllegalArgumentException("Use up to three combined gaps, hazards and hurdles.");
            if (gaps==0 && input.getBoolean("wideGap") || hazards==0 && input.getBoolean("tallHazard"))
                throw new IllegalArgumentException("An oversized feature needs a gap or hazard of that type.");
            brief.put("gaps",gaps).put("hazards",hazards).put("wideGap",input.getBoolean("wideGap"))
                    .put("tallHazard",input.getBoolean("tallHazard"));
        }
        if (course.equals(AvatarLevelMission.JUMP) != (hurdles+gaps+hazards>0) || hurdles==0 && brief.getBoolean("oversized"))
            throw new IllegalArgumentException("An oversized hurdle needs a course with at least one hurdle.");
        return brief;
    }

    static JSONArray objects(JSONObject brief, String token, String rail) {
        int count=brief.getInt("hurdles"), pickups=brief.getInt("pickups");
        boolean staggered=brief.getString("spacing").equals("staggered");
        int[] hurdles=switch(count) {
            case 1 -> new int[]{staggered?275:320};
            case 2 -> staggered?new int[]{205,474}:new int[]{240,463};
            case 3 -> staggered?new int[]{175,340,522}:new int[]{185,355,520};
            default -> new int[]{};
        };
        int[] pockets=switch(count) {
            case 1 -> new int[]{91,128,164,521,562,609};
            case 2 -> new int[]{90,122,151,579,611,645};
            case 3 -> new int[]{65,94,123,606,630,654};
            default -> staggered?new int[]{95,151,272,411,465,606}:new int[]{96,198,300,402,504,606};
        };
        var objects=new JSONArray();
        for (int i=0;i<pickups;i++) {
            int slot=pickups==1?0:(int)Math.round((double)i*(pockets.length-1)/(pickups-1));
            int y=brief.getBoolean("elevated") && i==pickups/2 ? 54 : FLOOR-PICKUP_SIZE-1;
            objects.put(AvatarLevelMission.item("coin-"+(i+1),token,"pickup",pockets[slot],y,PICKUP_SIZE));
        }
        for (int i=0;i<hurdles.length;i++) {
            int size=i==0&&brief.getBoolean("oversized")?144:staggered&&i%2==1?36:48;
            objects.put(hurdle("hurdle-"+(i+1),rail,hurdles[i],size));
        }
        return objects;
    }

    static JSONObject hurdle(String id,String asset,int x,int size) {
        return AvatarLevelMission.item(id,asset,"terrain",x,FLOOR-size,size)
                .put("collider",new JSONObject().put("x",0).put("y",0).put("w",size).put("h",size));
    }

    static JSONObject diagnose(JSONObject layout, JSONObject run) {
        var rows=run.getJSONArray("rows");
        var last=rows.getJSONArray(rows.length()-1);
        if (layout.has("traversal")) {
            var reset=AvatarTraversal.diagnoseReset(layout,rows);
            if (reset!=null) return reset;
        }
        var picked=new HashSet<String>();
        for (Object raw : rows) {
            for (Object event : ((JSONArray) raw).getJSONArray(17)) {
                var action=(JSONArray)event;
                if ("pickup".equals(action.getString(0))) picked.add(action.getString(1));
            }
        }

        String wall=last.getString(6);
        var objects=layout.getJSONArray("objects");
        var blocker=find(objects,wall);
        if (blocker!=null && blocker.has("collider")) {
            int contacts=0;
            for (int tick=Math.max(1,rows.length()-90);tick<rows.length();tick++) {
                if (wall.equals(rows.getJSONArray(tick).getString(6))) contacts++;
            }
            if (contacts>=10 && Math.abs(last.getDouble(3))<.01)
                return new JSONObject().put("kind","blocked-hurdle").put("objectId",wall).put("contacts",contacts)
                        .put("lastX",last.getDouble(1)).put("before",geometry(blocker));
        }

        if (last.getDouble(1)+PLAYER_WIDTH>=671.99) {
            for (Object raw : objects) {
                var pickup=(JSONObject)raw;
                if ("pickup".equals(pickup.getString("layer"))&&!picked.contains(pickup.getString("id")))
                    return new JSONObject().put("kind","missed-pickup").put("objectId",pickup.getString("id"))
                            .put("lastX",last.getDouble(1)).put("before",geometry(pickup));
            }
        }
        return new JSONObject().put("kind","unresolved").put("lastX",last.getDouble(1))
                .put("collected",picked.size()).put("before",new JSONObject());
    }

    static JSONObject repair(JSONObject original, JSONObject diagnosis, JSONArray rows) {
        String kind=diagnosis.getString("kind"), id=diagnosis.getString("objectId");
        var layout=new JSONObject(original.toString());
        if (kind.equals("fell-in-gap") || kind.equals("hit-hazard")) return AvatarTraversal.adjust(layout,diagnosis);
        var objects=layout.getJSONArray("objects");
        var target=find(objects,id);
        if (target==null || !geometry(target).similar(diagnosis.getJSONObject("before")))
            throw new IllegalArgumentException("The failed level geometry changed.");
        if (kind.equals("blocked-hurdle")) {
            int size=target.getInt("size");
            int bank=layout.has("traversal")?AvatarTraversal.BANK_Y:FLOOR;
            if (size<=24) return null;
            int next=size>48?48:Math.max(24,size-16);
            var reduced=hurdle(id,target.getString("assetId"),target.getInt("x"),next);
            target.put("y",bank-next).put("size",next).put("collider",reduced.getJSONObject("collider"));
        } else if (kind.equals("missed-pickup")) {
            int bank=layout.has("traversal")?AvatarTraversal.BANK_Y:FLOOR;
            int x=groundedPickupX(target,objects,rows,bank);
            if (x<0) return null;
            target.put("x",x).put("y",bank-PICKUP_SIZE-1);
        } else throw new IllegalArgumentException("No measured repair is available.");
        return new JSONObject().put("layout",layout).put("after",geometry(target));
    }

    private static int groundedPickupX(JSONObject target,JSONArray objects,JSONArray rows,int bank) {
        int nearest=-1, distance=Integer.MAX_VALUE;
        for (Object raw : rows) {
            var row=(JSONArray)raw;
            if ((bank==FLOOR?!"floor".equals(row.getString(5)):!AvatarTraversal.supported(row))
                    || Math.abs(row.getDouble(2)+PLAYER_HEIGHT-bank)>.02)
                continue;
            int x=(int)Math.round(row.getDouble(1)), d=Math.abs(x-target.getInt("x"));
            if (x<55 || x>646 || d>=distance || !clear(objects,target,x)) continue;
            nearest=x;
            distance=d;
        }
        return nearest;
    }

    private static boolean clear(JSONArray objects,JSONObject target,int x) {
        for (Object raw : objects) {
            var item=(JSONObject)raw;
            if (item==target || "ground".equals(item.optString("effect"))) continue;
            int left=item.getInt("x"), right=left+item.getInt("size");
            if (x<right+2 && x+PICKUP_SIZE>left-2) return false;
        }
        return true;
    }

    static JSONObject find(JSONArray objects,String id) {
        for (Object raw : objects) {
            var item=(JSONObject)raw;
            if (item.getString("id").equals(id)) return item;
        }
        return null;
    }

    static JSONObject geometry(JSONObject item) {
        return new JSONObject().put("x",item.getInt("x")).put("y",item.getInt("y")).put("size",item.getInt("size"));
    }
}
