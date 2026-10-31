package fraymus.geometry;

import java.util.*;

/** Space-colonization idea: Runions, Lane and Prusinkiewicz(2007). Original implementation. */
public final class Colonization {
    public record Spec(int dimensions,long seed,int attractors,int iterations,double step,double influence,double kill) {
        public Spec { NDSpace.dimensions(dimensions);if(attractors<16||attractors>512||iterations<1||iterations>128||!Double.isFinite(step)||step<.015||step>.08||!Double.isFinite(influence)||influence<.12||influence>.6||!Double.isFinite(kill)||kill<.015||kill>influence||kill<step*.5)throw new IllegalArgumentException("Colony parameters exceed the evaluated budgets"); }
    }
    public record Node(int parent,double[] position,int generation) { public Node {position=NDSpace.vector(position);} @Override public double[] position(){return position.clone();} }
    public record Result(Spec spec,List<Node> nodes,List<double[]> attractors,List<Integer> consumedBy,List<Integer> growth,String stopReason) {}
    public static Result grow(Spec s) {
        var rng=new Random(s.seed());var attractions=new ArrayList<double[]>();
        for(int j=0;j<s.attractors();j++) {double[] p=new double[s.dimensions()];double norm=0;for(int k=0;k<p.length;k++){p[k]=rng.nextGaussian();norm+=p[k]*p[k];}double radius=.32*StrictMath.pow(rng.nextDouble(),1.0/p.length)/StrictMath.sqrt(norm);for(int k=0;k<p.length;k++)p[k]=(k==1?.65:.5)+radius*p[k];attractions.add(p);}
        double[] root=new double[s.dimensions()];Arrays.fill(root,.5);root[1]=.02;var nodes=new ArrayList<Node>();nodes.add(new Node(-1,root,0));int[] consumed=new int[s.attractors()];Arrays.fill(consumed,-1);var growth=new ArrayList<Integer>();growth.add(1);String stop="iteration budget";
        for(int iteration=1;iteration<=s.iterations();iteration++) {
            NDSpace.interrupted();double[][] directions=new double[nodes.size()][s.dimensions()];int[] counts=new int[nodes.size()];int remaining=0;double nearestDistance=Double.POSITIVE_INFINITY;int nearestNode=-1,nearestPoint=-1;
            for(int j=0;j<attractions.size();j++)if(consumed[j]<0) {
                remaining++;double best=Double.POSITIVE_INFINITY;int closest=-1;double[] target=attractions.get(j);
                for(int i=0;i<nodes.size();i++){double dist=NDSpace.distance(nodes.get(i).position,target);if(dist<best){best=dist;closest=i;}}
                if(best<=s.kill()){consumed[j]=closest;remaining--;continue;}
                if(best<nearestDistance){nearestDistance=best;nearestNode=closest;nearestPoint=j;}
                if(best<=s.influence()){for(int k=0;k<s.dimensions();k++)directions[closest][k]+=(target[k]-nodes.get(closest).position[k])/best;counts[closest]++;}
            }
            if(remaining==0){stop="all attractors reached";break;}int before=nodes.size();
            for(int i=0;i<before&&nodes.size()<1024;i++)if(counts[i]>0)append(nodes,i,directions[i],s.step(),iteration);
            if(nodes.size()==before&&nearestNode>=0){double[] delta=new double[s.dimensions()];for(int k=0;k<delta.length;k++)delta[k]=attractions.get(nearestPoint)[k]-nodes.get(nearestNode).position[k];append(nodes,nearestNode,delta,s.step(),iteration);}
            growth.add(nodes.size());if(nodes.size()>=1024){stop="node budget";break;}if(nodes.size()==before){stop="no new direction";break;}
        }
        // Account for attractors reached by the last permitted growth step too.
        for(int j=0;j<consumed.length;j++)if(consumed[j]<0)for(int i=0;i<nodes.size();i++)if(NDSpace.distance(attractions.get(j),nodes.get(i).position)<=s.kill()){consumed[j]=i;break;}
        var consumedList=Arrays.stream(consumed).boxed().toList();var result=new Result(s,List.copyOf(nodes),List.copyOf(attractions),consumedList,List.copyOf(growth),stop);verify(result);return result;
    }
    private static void append(List<Node> nodes,int parent,double[] direction,double step,int generation) {
        double norm=0;for(double x:direction)norm=StrictMath.hypot(norm,x);if(norm<1e-12)return;double[] p=nodes.get(parent).position.clone();for(int k=0;k<p.length;k++)p[k]+=step*direction[k]/norm;
        for(Node n:nodes)if(NDSpace.distance(n.position,p)<step*.15)return;nodes.add(new Node(parent,p,generation));
    }
    public static void verify(Result r) {
        if(r.nodes().isEmpty()||r.nodes().size()>1024||r.nodes().getFirst().parent()!=-1||r.attractors().size()!=r.spec().attractors()||r.consumedBy().size()!=r.attractors().size())throw new IllegalStateException("Colony accounting failed");
        for(int i=1;i<r.nodes().size();i++){Node n=r.nodes().get(i);if(n.parent()<0||n.parent()>=i||n.position.length!=r.spec().dimensions()||Math.abs(NDSpace.distance(n.position,r.nodes().get(n.parent()).position)-r.spec().step())>1e-10)throw new IllegalStateException("Branch parent/step invariant failed");}
        for(int j=0;j<r.consumedBy().size();j++){int node=r.consumedBy().get(j);if(node>=r.nodes().size()||node< -1||node>=0&&NDSpace.distance(r.attractors().get(j),r.nodes().get(node).position)>r.spec().kill()+1e-12)throw new IllegalStateException("Attractor removal invariant failed");}
    }
}
