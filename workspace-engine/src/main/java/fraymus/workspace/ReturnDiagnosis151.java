package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Retrospective, labelled crop probes. Never called by a tracker or admitted to appearance memory. */
final class ReturnDiagnosis151 {
    private final WorldObservationStore observations;
    private final AssociationAudit134 association;
    private final ReplayEvidence151 replay;
    ReturnDiagnosis151(WorldObservationStore observations,AssociationAudit134 association,ReplayEvidence151 replay){this.observations=observations;this.association=association;this.replay=replay;}
    JSONObject diagnose(String id)throws Exception {
        long start=System.nanoTime();var verified=replay.read(id,false);var before=replay.snapshot();
        var audit=verified.getJSONObject("audit");var run=observations.read(audit.getJSONObject("input").getString("runId"));
        if(!IdentityAudit149.uses(ObservationMethods129.policy(run)))throw new IllegalArgumentException("Choose a completed identity 149 run with original hypotheses");
        var dataset=association.verifiedDataset(run.getString("datasetId"));
        if(!WorldFrames.hash(dataset).equals(audit.getString("datasetSha256"))||!WorldFrames.hash(run).equals(audit.getString("runSha256")))throw new IllegalStateException("Diagnostic source differs from verified audit");
        var first=WorldObservationStore.decode(observations.image(dataset.getString("id"),0));
        String object=association.read(audit.getJSONObject("input").getString("labelId")).getJSONObject("input").getString("objectKey");
        JSONObject sourceObject=null;for(Object raw:dataset.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("key").equals(object))sourceObject=o;}
        if(sourceObject==null)throw new IllegalArgumentException("Missing diagnostic object");
        var seed=PixelTracker.Box.from(sourceObject.getJSONArray("box"));
        var references=new LinkedHashMap<String,double[]>();references.put("initial-seed",values(StructureSearch149.descriptor(first,seed).getJSONArray("pixels")));
        for(Object raw:run.getJSONObject("referenceMemory137").getJSONArray("examples")) {var e=(JSONObject)raw;references.put(e.getString("imageSha256")+":"+e.getInt("ordinal"),values(e.getJSONObject("structure149").getJSONArray("pixels")));}
        String body=null;for(Object raw:run.getJSONArray("bindings")){var binding=(JSONObject)raw;if(binding.getString("objectKey").equals(object))body=binding.getString("bodyId");}
        if(body==null)throw new IllegalStateException("Missing original object binding");
        var rows=new JSONArray();int targetStrong=0,sweptStrong=0,otherStrong=0,searchNear=0,ambiguous=0,missing=0,returned=0,hidden=0;boolean afterHidden=false;
        for(Object raw:audit.getJSONArray("scores")) {
            var label=(JSONObject)raw;String visibility=label.getString("visibility");
            if(Set.of("occluded","out-of-view").contains(visibility)){afterHidden=true;hidden++;continue;}
            if(!visibility.equals("visible"))continue; // Partial crops cannot prove a full reference match.
            int ordinal=label.getInt("ordinal");var image=WorldObservationStore.decode(observations.image(dataset.getString("id"),ordinal));
            var target=PixelTracker.Box.from(label.getJSONArray("targetBox"));
            var exact=best(references,image,target,0,0,0);var sweep=exact;
            for(double angle=-20;angle<=20;angle+=2)for(int dx:new int[]{-2,0,2})for(int dy:new int[]{-2,0,2}) {
                // Evaluate labelled bounding geometry and original reference aspect separately.
                for(var box:List.of(target,new PixelTracker.Box(target.x()+target.w()/2-seed.w()/2,target.y()+target.h()/2-seed.h()/2,seed.w(),seed.h()))) {
                    var probe=best(references,image,box,angle,dx,dy);if(probe.getDouble("score")>sweep.getDouble("score"))sweep=probe;
                }
            }
            var others=new JSONArray();for(Object b:label.optJSONArray("otherBoxes")==null?new JSONArray():label.getJSONArray("otherBoxes")) {
                var other=best(references,image,PixelTracker.Box.from((JSONArray)b),0,0,0);others.put(other);if(other.getBoolean("strong"))otherStrong++;
            }
            JSONObject tracker=null;for(Object o:run.getJSONArray("trace").getJSONObject(ordinal-1).getJSONArray("objects")){var candidate=(JSONObject)o;if(candidate.getString("bodyId").equals(body))tracker=candidate.getJSONObject("tracker");}
            if(tracker==null)throw new IllegalStateException("Missing original tracker binding");
            var identity=tracker.getJSONObject("identity149");var hypotheses=identity.getJSONArray("hypotheses");double overlap=0;
            for(Object h:hypotheses)overlap=Math.max(overlap,PixelTracker.iou(target,PixelTracker.Box.from(((JSONObject)h).getJSONArray("box"))));
            if(afterHidden) {returned++;if(exact.getBoolean("strong"))targetStrong++;if(sweep.getBoolean("strong"))sweptStrong++;if(overlap>=.5)searchNear++;if(identity.getBoolean("ambiguityRetained"))ambiguous++;if(!label.getBoolean("accepted"))missing++;}
            rows.put(new JSONObject().put("ordinal",ordinal).put("sourceFrame",label.getInt("sourceFrame")).put("imageSha256",label.getString("imageSha256"))
                .put("afterOcclusion",afterHidden).put("targetBox",target.json()).put("accepted",label.getBoolean("accepted")).put("correct",label.getBoolean("correct"))
                .put("axisAlignedTarget",exact).put("bestLabelGuidedProbe",sweep).put("labelledOthers",others).put("searchTargetOverlap",overlap)
                .put("ambiguityRetained",identity.getBoolean("ambiguityRetained")).put("actualHypotheses",ParsedJson137.copy(new JSONObject().put("items",hypotheses)).getJSONArray("items")));
        }
        var after=replay.snapshot();if(!before.hash().equals(after.hash()))throw new IllegalStateException("Original diagnostic evidence changed; no result retained");
        return new JSONObject().put("schema","return-diagnosis-151-v1").put("auditId",id).put("auditSha256",WorldFrames.hash(audit)).put("evidenceFingerprint",after.hash())
            .put("trackerCodeSha256",audit.getString("trackerCodeSha256")).put("diagnosticCodeSha256",codeIdentity())
            .put("summary",new JSONObject().put("fullyVisibleReturns",returned).put("missingReturns",missing).put("hiddenLabels",hidden).put("strongAtLabelledTarget",targetStrong)
                .put("strongWithLabelGuidedSweep",sweptStrong).put("searchOverlapsTarget",searchNear).put("returnsWithAmbiguityVeto",ambiguous).put("strongOtherAxisCrops",otherStrong))
            .put("rows",rows).put("cost151",verified.getJSONObject("cost151")).put("totalMs",(System.nanoTime()-start)/1e6)
            .put("scope","Exposed, retrospective diagnostics only. Labels provide target locations; bounded ±20-degree / ±2-pixel probes are hypotheses, not measured orientation, identity, recovery or a deployed search. Original 149 thresholds and live defaults are unchanged. Other-object probes do not establish general lookalike rejection. No learned appearances, state writes or physical actions.");
    }
    static String codeIdentity()throws Exception{try(var in=ReturnDiagnosis151.class.getResourceAsStream("ReturnDiagnosis151.class")){return PerceptionStore.hash(in.readAllBytes());}}
    static double[] values(JSONArray array){var out=new double[array.length()];for(int i=0;i<out.length;i++)out[i]=array.getDouble(i);return out;}
    static JSONObject best(Map<String,double[]> refs,BufferedImage image,PixelTracker.Box box,double degrees,int dx,int dy) {
        double[] pixels=sample(image,box,degrees,dx,dy);JSONObject best=null;
        for(var ref:refs.entrySet()) {var scored=score(ref.getValue(),pixels).put("sourceKey",ref.getKey()).put("degrees",degrees).put("dxPixels",dx).put("dyPixels",dy).put("box",box.json());
            if(best==null||scored.getDouble("score")>best.getDouble("score"))best=scored;}
        return best;
    }
    static double[] sample(BufferedImage image,PixelTracker.Box b,double degrees,int dx,int dy) {
        if(degrees==0&&dx==0&&dy==0&&b.x()>=0&&b.y()>=0&&b.x()+b.w()<=1&&b.y()+b.h()<=1)return values(StructureSearch149.descriptor(image,b).getJSONArray("pixels"));
        var out=new double[768];double angle=Math.toRadians(degrees),c=Math.cos(angle),s=Math.sin(angle),cx=(b.x()+b.w()/2)*image.getWidth()+dx,cy=(b.y()+b.h()/2)*image.getHeight()+dy;int k=0;
        for(int y=0;y<16;y++)for(int x=0;x<16;x++) {
            double u=((x+.5)/16-.5)*b.w()*image.getWidth(),v=((y+.5)/16-.5)*b.h()*image.getHeight();
            double fx=cx+c*u-s*v,fy=cy+s*u+c*v;
            if(fx<0||fy<0||fx>=image.getWidth()||fy>=image.getHeight())return null;
            int px=(int)fx,py=(int)fy;
            int rgb=image.getRGB(px,py);for(int channel=0;channel<3;channel++)out[k++]=((rgb>>(16-channel*8))&255)/255.;
        }return out;
    }
    private static double gray(double[] a,int p){return .299*a[p*3]+.587*a[p*3+1]+.114*a[p*3+2];}
    static JSONObject score(double[] a,double[] b) {
        if(a.length!=768)throw new IllegalArgumentException("Full 16 by 16 reference required");
        if(b==null)return new JSONObject().put("score",-1).put("colorError",1).put("quadrantError",1).put("gradient",-1).put("strong",false).put("inFrame",false);
        if(b.length!=768)throw new IllegalArgumentException("Full 16 by 16 probe required");
        double sa=0,sb=0,aa=0,bb=0,ab=0,error=0,ga=0,gb=0,gab=0;var quadrants=new double[4];
        for(int y=0;y<16;y++)for(int x=0;x<16;x++) {
            int i=y*16+x;double u=gray(a,i),v=gray(b,i);sa+=u;sb+=v;aa+=u*u;bb+=v*v;ab+=u*v;
            double e=0;for(int ch=0;ch<3;ch++)e+=Math.abs(a[i*3+ch]-b[i*3+ch])/3;error+=e;quadrants[(y/8)*2+x/8]+=e;
            for(int offset:new int[]{x<15?1:0,y<15?16:0})if(offset!=0){double ag=gray(a,i+offset)-u,bg=gray(b,i+offset)-v;ga+=ag*ag;gb+=bg*bg;gab+=ag*bg;}
        }
        double va=aa-sa*sa/256,vb=bb-sb*sb/256,correlation=va/256<.0005||vb/256<.0005?-1:Math.max(-1,Math.min(1,(ab-sa*sb/256)/Math.sqrt(va*vb)));
        error/=256;double quadrant=Arrays.stream(quadrants).max().orElse(64)/64,gradient=ga<.001||gb<.001?-1:gab/Math.sqrt(ga*gb);
        return new JSONObject().put("score",correlation).put("colorError",error).put("quadrantError",quadrant).put("gradient",gradient)
            .put("strong",correlation>=.94&&error<=.10&&quadrant<=.14&&gradient>=.85).put("inFrame",true);
    }
}
