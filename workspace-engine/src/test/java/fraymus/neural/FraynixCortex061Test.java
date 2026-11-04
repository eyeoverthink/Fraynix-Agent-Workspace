package fraymus.neural;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.nio.*;
import java.io.*;

public class FraynixCortex061Test {
 @Test public void smallEncodersFireAndZeroStaysZero(){
  double[] input=new double[32];Arrays.fill(input,1);
  for(int n:new int[]{1,2,4,8,10,32,128}){
   var a=FraynixCortex061.fly(input,n);assertEquals(Math.max(1,n/10),Arrays.stream(a).filter(v->v>0).count());
   assertArrayEquals(a,FraynixCortex061.fly(input,n),0);
   assertEquals(0,Arrays.stream(FraynixCortex061.fly(new double[32],n)).sum(),0);
  }
 }
 @Test public void continuationIncludesLifecycleFossilsAndHistory()throws Exception{
  var a=new FraynixCortex061(4,8);for(int t=0;t<80;t++)a.step(input(t),16);
  var c=a.active.remove(a.active.size()-1);c.fossil=true;c.idle=60;c.flash=7;c.omega+=.25;a.fossils.put(c.id,c);
  a.active.get(0).highStress=2;a.active.get(0).idle=59;a.active.get(0).flash=7;
  var b=FraynixCortex061.restore(a.checkpoint());assertArrayEquals(a.checkpoint(),b.checkpoint());
  assertEquals(c.parent,b.fossils.get(c.id).parent);assertEquals(c.omega,b.fossils.get(c.id).omega,0);
  for(int t=80;t<155;t++){assertEquals(a.step(input(t),16),b.step(input(t),16));assertArrayEquals(a.checkpoint(),b.checkpoint());}
  assertEquals(64,a.snapshots.size());assertTrue(a.sleeps>0);assertTrue(a.mitosis>0);
 }
 static double[] input(int t){double[] a=new double[8];for(int i=0;i<a.length;i++)a[i]=.5+.4*Math.sin(t*.17+i);return a;}
 static byte[] rehash(byte[] b){System.arraycopy(FraynixCortex061.hash(Arrays.copyOf(b,b.length-32)),0,b,b.length-32,32);return b;}
 @Test public void unsupportedVersionAndMalformedInputsRefuseWithoutMutation()throws Exception{
  var a=new FraynixCortex061(3,8);a.step(input(1),16);byte[] original=a.checkpoint();
  byte[] version=original.clone();ByteBuffer.wrap(version).putInt(4,999);reject(rehash(version));
  byte[] shape=original.clone();ByteBuffer.wrap(shape).putInt(8,Integer.MAX_VALUE);reject(rehash(shape));
  byte[] nan=original.clone();ByteBuffer.wrap(nan).putDouble(32,Double.NaN);reject(rehash(nan));
  byte[] trailing=Arrays.copyOf(original,original.length+1);reject(rehash(trailing));
  reject(Arrays.copyOf(original,40));byte[] corrupt=original.clone();corrupt[70]^=1;reject(corrupt);
  assertArrayEquals(original,a.checkpoint());
  try{a.step(new double[]{1},16);fail();}catch(IllegalArgumentException expected){}
  try{FraynixCortex061.fly(new double[]{Double.NaN},4);fail();}catch(IllegalArgumentException expected){}
 }
 static void reject(byte[] b){try{FraynixCortex061.restore(b);fail("Accepted malformed checkpoint");}catch(IOException expected){}}
 @Test public void readoutLearnsMeasuredMappingAndRetainsPrediction(){
  double[][] x={{1,0},{1,1},{1,2},{1,3}};double[] y={2,5,8,11};double[] w=OutcomeReadout061.train(x,y);
  assertEquals(6.5,OutcomeReadout061.predict(w,new double[]{1,1.5}),.01);
 }
}
