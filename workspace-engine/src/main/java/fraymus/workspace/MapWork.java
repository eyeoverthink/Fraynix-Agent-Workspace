package fraymus.workspace;

import org.json.*;

/** Independent reconstruction from actual per-tick force/displacement rows. */
final class MapWork {
    static final String CONVENTION="Discrete force dot measured step displacement, J; effort is force magnitude times dt, N·s. Contact/damping residual is unresolved work, not proof of energy conservation.";
    static JSONObject audit(JSONObject run) {
        int tick=0;double signed=0,positive=0,braking=0,drag=0,gravity=0,effort=0;
        double[] position=SpatialScene.vector(run.getJSONObject("initialState").getJSONArray("position"),30);
        for(Object raw:run.getJSONArray("samples")) {
            var sample=(JSONObject)raw;
            for(Object row:sample.getJSONArray("integration")) {
                var r=(JSONArray)row;
                if(r.length()!=8||r.getInt(0)!=++tick)throw new IllegalStateException("Missing/duplicate work integration tick");
                for(Object value:r)if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalStateException("Nonfinite work row");
                double fx=r.getDouble(1),fz=r.getDouble(2),dx=r.getDouble(3),dz=r.getDouble(4);
                if(Math.hypot(fx,fz)>24.0001)throw new IllegalStateException("Actuator force exceeds 24N");
                double work=fx*dx+fz*dz;signed+=work;positive+=Math.max(0,work);braking+=Math.max(0,-work);
                drag+=r.getDouble(5)*dx+r.getDouble(6)*dz;gravity+=-9.81*r.getDouble(7);
                effort+=Math.hypot(fx,fz)*BulletRamp.DT;
                position[0]+=dx;position[1]+=r.getDouble(7);position[2]+=dz;
            }
            var expected=new JSONObject().put("actuatorWorkJ",signed).put("positiveWorkJ",positive).put("brakingWorkJ",braking)
                .put("dragWorkJ",drag).put("gravityWorkJ",gravity).put("effortNs",effort);
            if(sample.getInt("tick")!=tick)throw new IllegalStateException("Work sample tick disagrees with integration rows");
            for(String key:expected.keySet())if(Math.abs(expected.getDouble(key)-sample.getDouble(key))>1e-7*Math.max(1,Math.abs(expected.getDouble(key))))
                throw new IllegalStateException("Work counter differs from original integration: "+key);
            if(ObserverGeometry.norm(ObserverGeometry.sub(position,SpatialScene.vector(sample.getJSONArray("position"),30)))>1e-6)
                throw new IllegalStateException("Integration displacement does not reconstruct native position");
            var velocity=SpatialScene.vector(sample.getJSONArray("velocity"),30);var angular=SpatialScene.vector(sample.getJSONArray("angularVelocity"),300);
            double energy=.5*ObserverGeometry.dot(velocity,velocity)+.5*.4*.18*.18*ObserverGeometry.dot(angular,angular);
            if(Math.abs(energy-sample.getDouble("kineticEnergyJ"))>2e-6*Math.max(1,energy))throw new IllegalStateException("Kinetic energy differs from actual velocities");
        }
        var last=run.getJSONObject("final");if(tick!=last.getInt("tick"))throw new IllegalStateException("Final native tick missing work rows");
        var finalRecorded=run.getJSONArray("samples").isEmpty()?run.getJSONObject("initialState"):run.getJSONArray("samples").getJSONObject(run.getJSONArray("samples").length()-1);
        if(!last.similar(finalRecorded))throw new IllegalStateException("Final body differs from recorded native boundary");
        double impulse=0;int impulses=0;for(Object raw:run.getJSONArray("events")){var e=(JSONObject)raw;if(e.getString("kind").equals("impulse")){
            if(e.getInt("tick")!=480||e.getDouble("impulseNs")!=run.getDouble("disturbanceNs"))throw new IllegalStateException("Disturbance differs from scheduled intent");
            var before=SpatialScene.vector(e.getJSONArray("beforeVelocity"),30);var after=SpatialScene.vector(e.getJSONArray("afterVelocity"),30);
            if(Math.abs(after[0]-before[0]-e.getDouble("impulseNs"))>1e-6||after[1]!=before[1]||after[2]!=before[2])throw new IllegalStateException("Actual native impulse velocity differs from intent");
            double actualWork=.5*(ObserverGeometry.dot(after,after)-ObserverGeometry.dot(before,before));
            if(Math.abs(actualWork-e.getDouble("workJ"))>2e-6)throw new IllegalStateException("Impulse work differs from measured velocities");
            impulses++;impulse+=e.getDouble("workJ");}}
        int expected=run.getBoolean("automatic")&&run.getDouble("disturbanceNs")!=0&&tick>480?1:0;
        if(impulses!=expected||run.getBoolean("impulseApplied")!=(expected==1))throw new IllegalStateException("Disturbance missing or applied more than once");
        double residual=last.getDouble("kineticEnergyJ")-run.getDouble("initialKineticEnergyJ")-signed-drag-gravity-impulse;
        return new JSONObject().put("passed",true).put("integrationTicks",tick).put("actuatorWorkJ",signed).put("positiveWorkJ",positive)
            .put("brakingWorkJ",braking).put("dragWorkJ",drag).put("gravityWorkJ",gravity).put("impulseWorkJ",impulse)
            .put("kineticEnergyJ",last.getDouble("kineticEnergyJ")).put("unresolvedContactDampingWorkJ",residual)
            .put("effortNs",effort).put("convention",CONVENTION);
    }
}
