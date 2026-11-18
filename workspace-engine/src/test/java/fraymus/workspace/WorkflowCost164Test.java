package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicLong;

public class WorkflowCost164Test {
 @Test public void nestedDurationsAndActualExceptionsRemainSeparate()throws Exception{
  var clock=new AtomicLong();var c=new WorkflowCost164(true,clock::get);var value=new Object();
  assertSame(value,c.measure(WorkflowCost164.Kind.HISTORY,()->{clock.addAndGet(2_000_000);c.measure(WorkflowCost164.Kind.NATIVE_JOB,()->{clock.addAndGet(5_000_000);return 1;});clock.addAndGet(3_000_000);return value;}));
  var t=c.status().getJSONObject("totals");assertEquals(10,t.getJSONObject("HISTORY").getDouble("inclusiveMs"),0);assertEquals(5,t.getJSONObject("HISTORY").getDouble("exclusiveMs"),0);assertEquals(5,t.getJSONObject("NATIVE_JOB").getDouble("inclusiveMs"),0);
  var failure=new IllegalStateException("Original verifier refusal");assertSame(failure,assertThrows(IllegalStateException.class,()->c.measure(WorkflowCost164.Kind.REPORT_AUDIT,()->{throw failure;})));assertEquals(1,c.status().getJSONObject("totals").getJSONObject("REPORT_AUDIT").getInt("failed"));
 }
 @Test public void disabledDelegatesWithoutClockAndRetentionIsBounded()throws Exception{
  var off=new WorkflowCost164(false,()->{throw new AssertionError("Disabled clock");});var value=new Object();assertSame(value,off.measure(WorkflowCost164.Kind.SOURCE,()->value));assertEquals(0,off.status().getInt("operations"));
  var c=new WorkflowCost164(true,System::nanoTime);for(int i=0;i<140;i++)c.measure(WorkflowCost164.Kind.SOURCE,()->42);var s=c.status();assertEquals(128,s.getJSONArray("recent").length());assertEquals(12,s.getInt("dropped"));s.getJSONObject("totals").getJSONObject("SOURCE").put("count",0);assertEquals(140,c.status().getJSONObject("totals").getJSONObject("SOURCE").getInt("count"));
 }
 @Test public void threadLocalParentsDoNotCaptureAnotherThreadsWork()throws Exception{
  var c=new WorkflowCost164(true,System::nanoTime);c.measure(WorkflowCost164.Kind.HISTORY,()->{var t=new Thread(()->{try{c.measure(WorkflowCost164.Kind.NATIVE_JOB,()->null);}catch(Exception e){throw new AssertionError(e);}});t.start();t.join();return null;});var t=c.status().getJSONObject("totals");assertEquals(t.getJSONObject("HISTORY").getDouble("inclusiveMs"),t.getJSONObject("HISTORY").getDouble("exclusiveMs"),0);assertEquals(1,t.getJSONObject("NATIVE_JOB").getInt("count"));
 }
}
