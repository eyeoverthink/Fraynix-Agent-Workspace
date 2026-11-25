package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static fraymus.workspace.ImageSceneRegistration.*;

/** Fits three-axis rotation using two registered views. A third view checks the frozen pose. */
final class ImageTiltRegistration {
    static final String POLICY = "registered-silhouettes-tilt-v3";
    static final String CALIBRATED_POLICY = "registered-silhouettes-tilt-v4";
    static final int TILT_LIMIT = 30;
    private static final double D = Math.sqrt(.5);
    private static final double FIT_AWAY = Math.sqrt(.75);
    private static final double FIT_UP = .5;
    private static final double ALTERNATE_ANGLE = 20;
    private static final double MIN_POSE_GAP = .022;
    private static final double MAX_WITNESS_ANGLE = 8;
    private static final double MIN_WITNESS_ROTATION_GAIN = .022;

    private record Camera(double heading, double elevation, double sine, double cosine,
                          double rise, double run, boolean legacy) {
        static Camera calibrated(double heading, double elevation) {
            double azimuth = Math.toRadians(heading), height = Math.toRadians(elevation);
            return new Camera(heading, elevation, Math.sin(azimuth), Math.cos(azimuth),
                    Math.sin(height), Math.cos(height), false);
        }

        double horizontal(double x, double z) {
            return legacy ? (heading < 0 ? D*(x+z) : D*(x-z)) : cosine*x-sine*z;
        }

        double vertical(double x, double y, double z) {
            if (legacy) return heading < 0 ? D*(z-x)*FIT_AWAY-y*FIT_UP : .5*(x+z)-D*y;
            return rise*(sine*x+cosine*z)-run*y;
        }

        double height(double x, double z, double viewY) {
            return (vertical(x,0,z)-viewY)/run;
        }

        double separation(Camera other) {
            double dot = rise*other.rise + run*other.run*(sine*other.sine + cosine*other.cosine);
            return Math.toDegrees(Math.acos(Math.clamp(dot,-1,1)));
        }
    }

    private record Cameras(Camera side, Camera witness, boolean calibrated) {
        static Cameras read(JSONObject angles) {
            if (angles == null) return new Cameras(
                    new Camera(-45,60,-D,D,FIT_AWAY,FIT_UP,true),
                    new Camera(45,45,D,D,D,D,true),false);
            SpatialScene.keys(angles,"sideHeadingDeg","sideElevationDeg","witnessHeadingDeg","witnessElevationDeg");
            var side = Camera.calibrated(SpatialScene.number(angles,"sideHeadingDeg",-180,180),
                    SpatialScene.number(angles,"sideElevationDeg",25,70));
            var witness = Camera.calibrated(SpatialScene.number(angles,"witnessHeadingDeg",-180,180),
                    SpatialScene.number(angles,"witnessElevationDeg",25,70));
            if (side.separation(witness)<25)
                throw new IllegalArgumentException("Fitting and held-out cameras must be at least 25° apart.");
            return new Cameras(side,witness,true);
        }
    }

    record Angles(int yaw, int pitch, int roll) {
        double[] quaternion() {
            double cy=Math.cos(Math.toRadians(yaw)/2), sy=Math.sin(Math.toRadians(yaw)/2);
            double cp=Math.cos(Math.toRadians(pitch)/2), sp=Math.sin(Math.toRadians(pitch)/2);
            double cr=Math.cos(Math.toRadians(roll)/2), sr=Math.sin(Math.toRadians(roll)/2);
            return new double[]{cy*sp*cr+sy*cp*sr,sy*cp*cr-cy*sp*sr,
                    cy*cp*sr-sy*sp*cr,cy*cp*cr+sy*sp*sr};
        }

        JSONArray values() { return new JSONArray(new int[]{yaw,pitch,roll}); }
    }

    record Pose(double x, double y, double z, Angles angles) {
        JSONArray position() { return new JSONArray(new double[]{x,y,z}); }
    }

    record Result(List<Pose> poses, JSONObject evidence) {}
    private record Candidate(Pose pose, Match top, Match guide) {
        double score() { return top.iou()+guide.iou(); }
    }

    private static final class JointAssignment {
        Candidate[] best;
        double score=Double.NEGATIVE_INFINITY, runner=Double.NEGATIVE_INFINITY;

        void search(Candidate[][] choices, int depth, int topTaken, int guideTaken, Candidate[] sequence, double total) {
            if (depth==choices.length) {
                if (total>score) {
                    runner=score;
                    score=total;
                    best=sequence.clone();
                } else runner=Math.max(runner,total);
                return;
            }
            for (int index=0;index<choices[depth].length;index++) {
                int top=1<<(index/choices.length), guide=1<<(index%choices.length);
                var candidate=choices[depth][index];
                if (candidate==null || (topTaken&top)!=0 || (guideTaken&guide)!=0) continue;
                sequence[depth]=candidate;
                search(choices,depth+1,topTaken|top,guideTaken|guide,sequence,total+candidate.score());
            }
        }
    }

    private static final class Fit {
        final int w,h,n;
        final double cell;
        final Prop prop;
        final List<Silhouette> overhead, guide;
        final Camera camera;
        final HashMap<Angles,Candidate[]> samples=new HashMap<>();

        Fit(int w, int h, double cell, Prop prop, List<Silhouette> overhead, List<Silhouette> guide, Camera camera) {
            this.w=w; this.h=h; this.cell=cell; this.prop=prop; this.overhead=overhead; this.guide=guide;
            this.camera=camera;
            n=overhead.size();
        }

        Candidate[] sample(Angles angles) {
            return samples.computeIfAbsent(angles,this::measure);
        }

        private Candidate[] measure(Angles angles) {
            var candidate=new Candidate[n*n];
            var origin=new Pose(0,0,0,angles);
            var top=project(w,h,cell,prop.mesh(),origin,null);
            if (top==null) return candidate;
            var fitted=new Match[n];
            boolean any=false;
            for (int i=0;i<n;i++) {
                var match=fit(top,overhead.get(i),w,h,1);
                if (match.iou()>=.55) {
                    fitted[i]=match;
                    any=true;
                }
            }
            if (!any) return candidate;
            var second=project(w,h,cell,prop.mesh(),origin,camera);
            if (second==null) return candidate;
            for (int i=0;i<n;i++) {
                var first=fitted[i];
                if (first==null) continue;
                double x=first.dx()*cell, z=first.dy()*cell;
                int dx=(int)Math.round(camera.horizontal(x,z)/cell);
                for (int j=0;j<n;j++) {
                    var observation=guide.get(j);
                    int center=(int)Math.round((observation.minY+observation.maxY-second.minY-second.maxY)/2.0);
                    Match best=null;
                    for (int dy=center-1;dy<=center+1;dy++) {
                        var match=compare(second,observation,w,h,dx,dy);
                        if (best==null || match.iou()>best.iou() ||
                                match.iou()==best.iou() && match.centerErrorPx()<best.centerErrorPx()) best=match;
                    }
                    double y=camera.height(x,z,best.dy()*cell);
                    if (best.iou()>=.55 && y>=0 && y<=15)
                        candidate[i*n+j]=new Candidate(new Pose(x,y,z,angles),first,best);
                }
            }
            return candidate;
        }

        Candidate[] solve() {
            var coarse=new ArrayList<Candidate[]>();
            for (int yaw=-180;yaw<180;yaw+=24)
                for (int pitch=-TILT_LIMIT;pitch<=TILT_LIMIT;pitch+=15)
                    for (int roll=-TILT_LIMIT;roll<=TILT_LIMIT;roll+=15)
                        coarse.add(sample(new Angles(yaw,pitch,roll)));
            var result=new Candidate[n*n];
            for (int pair=0;pair<result.length;pair++) {
                final int index=pair;
                var seeds=new ArrayList<Candidate>();
                for (var row:coarse) if (row[pair]!=null) seeds.add(row[pair]);
                seeds.sort((a,b)->Double.compare(b.score(),a.score()));
                var refined=new ArrayList<Candidate>();
                for (var seed:seeds) {
                    if (seed.score()<seeds.get(0).score()-.14 || refined.size()==3) break;
                    if (refined.stream().anyMatch(other->angularDistance(other.pose.angles,seed.pose.angles)<16)) continue;
                    var winner=new Candidate[]{seed};
                    for (int step:new int[]{8,4,2,2}) {
                        neighbors(winner[0].pose.angles,step, angle->{
                            var proposal=sample(angle)[index];
                            if (proposal!=null && proposal.score()>winner[0].score()) winner[0]=proposal;
                        });
                    }
                    refined.add(winner[0]);
                }
                for (var choice:refined)
                    if (result[pair]==null || choice.score()>result[pair].score()) result[pair]=choice;
            }
            return result;
        }

        double gap(Candidate winner, int pair) {
            if (!camera.legacy) {
                // Local Y half-turn preserves many rectangular prisms. Check the exact alternate
                // even when the coarse lattice falls between the two refined poses.
                var angles = winner.pose.angles;
                sample(new Angles(Math.floorMod(angles.yaw,360)-180,-angles.pitch,-angles.roll));
            }
            double alternate=0;
            for (var choices:samples.values()) {
                var other=choices[pair];
                if (other!=null && angularDistance(winner.pose.angles,other.pose.angles)>=ALTERNATE_ANGLE)
                    alternate=Math.max(alternate,other.score());
            }
            return winner.score()-alternate;
        }
    }

    private static void neighbors(Angles center, int step, Consumer<Angles> evaluate) {
        for (int dy=-step;dy<=step;dy+=step) for (int dp=-step;dp<=step;dp+=step)
            for (int dr=-step;dr<=step;dr+=step) {
                int p=center.pitch+dp, r=center.roll+dr;
                if (Math.abs(p)<=TILT_LIMIT && Math.abs(r)<=TILT_LIMIT)
                    evaluate.accept(new Angles(Math.floorMod(center.yaw+dy+180,360)-180,p,r));
            }
    }

    static Result locate(BufferedImage overhead, byte[] guideBytes, byte[] witnessBytes, double cell,
                         List<Prop> props, JSONObject cameraAngles) throws Exception {
        var cameras = Cameras.read(cameraAngles);
        int w=overhead.getWidth(), h=overhead.getHeight(), n=props.size();
        if (cell>.025) throw new IllegalArgumentException("Use at least 40 pixels per metre in the registered views.");
        var first=components(overhead,n,"Overhead");
        var second=readParts(guideBytes,w,h,n,"Fitting side");
        if (sameParts(first,second)) throw new IllegalArgumentException("The fitting side view repeats the overhead silhouettes.");
        var fit=new Fit[n];
        var choices=new Candidate[n][];
        for (int i=0;i<n;i++) {
            fit[i]=new Fit(w,h,cell,props.get(i),first,second,cameras.side);
            choices[i]=fit[i].solve();
        }
        var assignment=new JointAssignment();
        assignment.search(choices,0,0,0,new Candidate[n],0);
        if (assignment.best==null || assignment.score-assignment.runner<MIN_ASSIGNMENT_GAP)
            throw new IllegalArgumentException("The fitting views do not uniquely identify the selected props.");
        var poses=new ArrayList<Pose>();
        var gaps=new double[n];
        for (int i=0;i<n;i++) {
            var candidate=assignment.best[i];
            int pair=Arrays.asList(choices[i]).indexOf(candidate);
            if (candidate.top.iou()<MIN_OVERHEAD_IOU || candidate.guide.iou()<MIN_WITNESS_IOU ||
                    candidate.guide.centerErrorPx()>MAX_WITNESS_CENTER_PX)
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        "Fitting views refused %s (%.1f%% overhead, %.1f%% side, %.2f px side center error).",
                        props.get(i).label(),candidate.top.iou()*100,candidate.guide.iou()*100,candidate.guide.centerErrorPx()));
            gaps[i]=fit[i].gap(candidate,pair);
            if (gaps[i]<MIN_POSE_GAP)
                throw new IllegalArgumentException("The fitting views leave "+props.get(i).label()+"'s tilt or heading ambiguous.");
            poses.add(candidate.pose);
        }

        // The withheld camera cannot select, move or rotate any fitted prop.
        var witness=readParts(witnessBytes,w,h,n,"Held-out view");
        if (sameParts(first,witness) || sameParts(second,witness))
            throw new IllegalArgumentException("Choose a separate held-out camera image.");
        var scores=new Match[n][n];
        for (int i=0;i<n;i++) {
            var expected=project(w,h,cell,props.get(i).mesh(),poses.get(i),cameras.witness);
            if (expected==null) throw new IllegalArgumentException("Keep each prop inside the held-out camera frame.");
            for (int j=0;j<n;j++) scores[i][j]=compare(expected,witness.get(j),w,h,0,0);
        }
        var pairing=assign(scores);
        var proof=new JSONArray();
        for (int i=0;i<n;i++) {
            var target=props.get(i);
            var fixed=poses.get(i);
            var match=scores[i][pairing.best[i]];
            var observed=witness.get(pairing.best[i]);
            if (match.centerErrorPx()>MAX_WITNESS_CENTER_PX || match.iou()<MIN_WITNESS_IOU)
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        "Held-out view refused %s's fixed pose (%.1f%% overlap, %.2f px center error).",
                        target.label(),match.iou()*100,match.centerErrorPx()));
            Angles[] head={fixed.angles};
            double[] best={match.iou()};
            for (int step:new int[]{8,4,2}) {
                neighbors(head[0],step, angles->{
                    var alternate=project(w,h,cell,target.mesh(),new Pose(fixed.x,fixed.y,fixed.z,angles),cameras.witness);
                    if (alternate==null) return;
                    double score=compare(alternate,observed,w,h,0,0).iou();
                    if (score>best[0]) { best[0]=score; head[0]=angles; }
                });
            }
            double angularDifference=angularDistance(fixed.angles,head[0]), gain=best[0]-match.iou();
            if (angularDifference>MAX_WITNESS_ANGLE && gain>MIN_WITNESS_ROTATION_GAIN)
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        "Held-out view refused %s's orientation at its fixed position (%.1f° correction).",
                        target.label(),angularDifference));
            var measured=assignment.best[i];
            proof.put(new JSONObject().put("sourceId","image-prop-"+(i+1)).put("label",target.label())
                    .put("positionM",fixed.position()).put("anglesDeg",fixed.angles.values())
                    .put("overheadIou",measured.top.iou()).put("guideIou",measured.guide.iou())
                    .put("poseGap",gaps[i]).put("witnessIou",match.iou()).put("witnessCenterErrorPx",match.centerErrorPx())
                    .put("witnessEstimatedAnglesDeg",head[0].values()).put("witnessCorrectionDeg",angularDifference)
                    .put("witnessCorrectionGain",gain));
        }
        return new Result(poses,new JSONObject().put("policy",cameras.calibrated?CALIBRATED_POLICY:POLICY).put("guideSha256",MapDefinition.sha(guideBytes))
                .put("witnessSha256",MapDefinition.sha(witnessBytes)).put("tiltLimitDeg",TILT_LIMIT)
                .put("assignmentGap",Double.isFinite(assignment.runner)?assignment.score-assignment.runner:JSONObject.NULL)
                .put("minimumPoseGap",MIN_POSE_GAP)
                .put("guideHeadingDeg",cameras.side.heading).put("guideElevationDeg",cameras.side.elevation)
                .put("witnessHeadingDeg",cameras.witness.heading).put("witnessElevationDeg",cameras.witness.elevation)
                .put("minimumWitnessIou",MIN_WITNESS_IOU).put("maximumWitnessCenterErrorPx",MAX_WITNESS_CENTER_PX)
                .put("maximumWitnessCorrectionDeg",MAX_WITNESS_ANGLE).put("minimumWitnessRotationGain",MIN_WITNESS_ROTATION_GAIN)
                .put("props",proof));
    }

    private static List<Silhouette> readParts(byte[] bytes, int w, int h, int count, String label) throws Exception {
        var image=BrowserShapeTrace.decode(bytes);
        if (image.getWidth()!=w || image.getHeight()!=h)
            throw new IllegalArgumentException("All registered views need the same image dimensions and scale.");
        return components(image,count,label);
    }

    private static boolean sameParts(List<Silhouette> a, List<Silhouette> b) {
        for (int i=0;i<a.size();i++) if (!a.get(i).mask.equals(b.get(i).mask)) return false;
        return true;
    }

    static double angularDistance(Angles a, Angles b) {
        if (a.equals(b)) return 0;
        var first=a.quaternion();
        var second=b.quaternion();
        double dot=0;
        for (int i=0;i<4;i++) dot+=first[i]*second[i];
        return 2*Math.toDegrees(Math.acos(Math.min(1,Math.abs(dot))));
    }

    static double[][] rotation(Angles angles) {
        var q=angles.quaternion();
        double x=q[0], y=q[1], z=q[2], w=q[3];
        return new double[][]{
            {1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)},
            {2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)},
            {2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)}
        };
    }

    private static Silhouette project(int w, int h, double cell, SpatialVisualMesh mesh, Pose pose, Camera camera) {
        var r=rotation(pose.angles);
        var points=mesh.positions();
        double[][] projected=new double[mesh.vertexCount()][2];
        for (int i=0;i<projected.length;i++) {
            double x=pose.x+r[0][0]*points[3*i]+r[0][1]*points[3*i+1]+r[0][2]*points[3*i+2];
            double y=pose.y+r[1][0]*points[3*i]+r[1][1]*points[3*i+1]+r[1][2]*points[3*i+2];
            double z=pose.z+r[2][0]*points[3*i]+r[2][1]*points[3*i+1]+r[2][2]*points[3*i+2];
            double u=camera==null?x:camera.horizontal(x,z);
            double v=camera==null?z:camera.vertical(x,y,z);
            projected[i][0]=w/2.0+u/cell;
            projected[i][1]=h/2.0+v/cell;
        }
        return raster(w,h,mesh,projected,true);
    }
}
