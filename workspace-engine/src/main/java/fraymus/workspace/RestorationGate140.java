package fraymus.workspace;

import org.json.JSONObject;

/** Bounded consecutive confirmation, never a timer-based release or an appearance memory. */
final class RestorationGate140 {
    static final int LIMIT=12,CONFIRMATIONS=3;
    private boolean eligible;
    private int age,confirmed;
    private PixelTracker.Box pending;
    void begin(boolean continuityAllowed){eligible=continuityAllowed;age=confirmed=0;pending=null;}
    boolean observe(boolean continuous,boolean sourceSupported,PixelTracker.Box box){
        age++;
        if(!continuous||age>LIMIT)eligible=false;
        if(!eligible||!sourceSupported||box==null){confirmed=0;pending=null;return false;}
        confirmed=pending!=null&&PixelTracker.iou(pending,box)>=.55?confirmed+1:1;
        pending=box;return confirmed>=CONFIRMATIONS;
    }
    JSONObject evidence(){return new JSONObject().put("eligible",eligible).put("ageImages",age).put("limitImages",LIMIT)
        .put("consecutiveConfirmations",confirmed).put("requiredConfirmations",CONFIRMATIONS).put("automaticAdmissions",0);}
}
