package fraymus.workspace;

import org.json.*;

/** Numerical evidence reconstruction; cached impulses and measured momentum stay distinct. */
final class ContactMath064 {
    static double finite(JSONObject j,String key,double bound){double x=j.getDouble(key);if(!Double.isFinite(x)||Math.abs(x)>bound)throw new IllegalStateException("Invalid contact value: "+key);return x;}
    static JSONObject project(JSONObject c,JSONObject s,JSONObject previous,double dt){
        var out=Stopping063.projected(c,s);var contacts=s.getJSONArray("contacts");var seps=new JSONArray();double[] impulse=new double[3];double normal=0,lateral=0;
        for(Object raw:contacts){var p=(JSONObject)raw;seps.put(finite(p,"separationM",100));if(p.getInt("lifetime")<0)throw new IllegalStateException("Invalid contact lifetime");
            double jn=finite(p,"normalImpulseNs",1000),j1=finite(p,"lateralImpulse1Ns",1000),j2=finite(p,"lateralImpulse2Ns",1000),sign=p.getBoolean("blockIsA")?1:-1;
            double[] n=SpatialScene.vector(p.getJSONArray("normal"),2),d1=SpatialScene.vector(p.getJSONArray("lateral1"),2),d2=SpatialScene.vector(p.getJSONArray("lateral2"),2);
            SpatialScene.vector(p.getJSONArray("positionA"),100);SpatialScene.vector(p.getJSONArray("positionB"),100);finite(p,"friction",10);
            for(int k=0;k<3;k++)impulse[k]+=sign*(n[k]*jn+d1[k]*j1+d2[k]*j2);normal+=jn;lateral+=Math.hypot(j1,j2);
        }
        if(!seps.similar(s.getJSONArray("contactSeparationsM")))throw new IllegalStateException("Contact separations differ from original points");
        double a=Math.toRadians(c.getDouble("angleDeg")),co=Math.cos(a),si=Math.sin(a);
        out.put("contactPoints",contacts.length()).put("normalImpulseNs",normal).put("lateralMagnitudeNs",lateral).put("cachedImpulseNs",new JSONArray(impulse)).put("cachedTangentNs",impulse[0]*co-impulse[1]*si);
        if(previous==null)return out.put("observedContactTangentNs",JSONObject.NULL).put("momentumResidualNs",JSONObject.NULL);
        double[] v=SpatialScene.vector(s.getJSONArray("velocity"),100),old=SpatialScene.vector(previous.getJSONArray("velocity"),100),observed=new double[3];double squared=0;
        for(int k=0;k<3;k++){observed[k]=c.getDouble("massKg")*(v[k]-old[k]+(k==1?c.getDouble("gravityMps2")*dt:0));double d=observed[k]-impulse[k];squared+=d*d;}
        return out.put("observedContactImpulseNs",new JSONArray(observed)).put("observedContactTangentNs",observed[0]*co-observed[1]*si).put("momentumResidualNs",Math.sqrt(squared));
    }
    static JSONArray series(JSONObject record){var rows=new JSONArray();JSONObject previous=null;for(Object raw:record.getJSONArray("samples")){var s=(JSONObject)raw;rows.put(project(record.getJSONObject("definition"),s,previous,record.getJSONObject("settings").getDouble("dt")));previous=s;}return rows;}
    static JSONObject summary(JSONObject record){
        var c=record.getJSONObject("definition");var rows=series(record);double maxRotation=0,maxResidual=0,sumNormal=0,sumLateral=0;Object firstTouch=JSONObject.NULL,firstImpulse=JSONObject.NULL;JSONObject endpoint=null;
        for(Object raw:rows){var s=(JSONObject)raw;double t=s.getDouble("timeS");if(Math.abs(t-.75)<1e-12)endpoint=s;
            if(firstTouch==JSONObject.NULL&&s.getInt("touchPoints")>0)firstTouch=t;if(firstImpulse==JSONObject.NULL&&(Math.abs(s.getDouble("normalImpulseNs"))+s.getDouble("lateralMagnitudeNs")>1e-12))firstImpulse=t;
            maxRotation=Math.max(maxRotation,s.getDouble("rotationDeg"));if(!s.isNull("momentumResidualNs"))maxResidual=Math.max(maxResidual,s.getDouble("momentumResidualNs"));if(s.getInt("tick")>0){sumNormal+=s.getDouble("normalImpulseNs");sumLateral+=s.getDouble("lateralMagnitudeNs");}
        }
        if(endpoint==null)throw new IllegalStateException("Missing .75-second observation");var end=rows.getJSONObject(rows.length()-1);
        return new JSONObject().put("firstTouchS",firstTouch).put("firstImpulseS",firstImpulse).put("endpointM",endpoint.getDouble("distanceM")).put("endpointErrorM",endpoint.getDouble("distanceM")-Stopping063.analytic(c,.75)).put("finalDistanceM",end.getDouble("distanceM")).put("finalErrorM",end.getDouble("distanceM")-Stopping063.analytic(c,3)).put("maximumRotationDeg",maxRotation).put("maximumMomentumResidualNs",maxResidual).put("summedNormalImpulseNs",sumNormal).put("summedLateralMagnitudeNs",sumLateral);
    }
    static JSONObject wilson(int hits,int n){if(n<=0||hits<0||hits>n)throw new IllegalArgumentException("Invalid coverage counts");double z=1.959963984540054,p=hits/(double)n,d=1+z*z/n,center=(p+z*z/(2*n))/d,half=z*Math.sqrt(p*(1-p)/n+z*z/(4.0*n*n))/d;return new JSONObject().put("lower",Math.max(0,center-half)).put("upper",Math.min(1,center+half)).put("confidence",.95).put("meaning","Descriptive Wilson sampling interval; stratified simulation proportions, not a physical predictive guarantee");}
}
