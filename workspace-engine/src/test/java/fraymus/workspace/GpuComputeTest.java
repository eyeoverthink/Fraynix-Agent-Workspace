package fraymus.workspace;
import fraymus.neural.*;
import fraymus.neural.gpu.GlNeuralCompute;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
public class GpuComputeTest {
    @Test public void actualGpuMatchesCpuIncludingTransfersAndNonWorkgroupSizes() throws Exception {
        var result=new JSONObject().put("contract","matvec-v1").put("gpuKernelLanguage","GLSL 430").put("scope","Dense neural readout primitive; not the whole spiking cortex, LLM training or guaranteed speedup");
        var rows=new JSONArray();try(var gpu=new GlNeuralCompute();var cpu=new CpuNeuralCompute()){
            result.put("device",gpu.device());var random=new Random(16016);
            for(int[] shape:new int[][]{{5,7},{65,33},{1024,128}}){
                int r=shape[0],c=shape[1];float[] w=new float[r*c],x=new float[c];for(int i=0;i<w.length;i++)w[i]=random.nextFloat()-.5f;for(int i=0;i<c;i++)x[i]=random.nextFloat()-.5f;
                var expected=cpu.matvec(w,x,r,c);gpu.matvec(w,x,r,c);long cpuNs=0,gpuNs=0;double max=0;
                for(int n=0;n<10;n++){long t=System.nanoTime();cpu.matvec(w,x,r,c);cpuNs+=System.nanoTime()-t;t=System.nanoTime();var actual=gpu.matvec(w,x,r,c);gpuNs+=System.nanoTime()-t;for(int i=0;i<r;i++)max=Math.max(max,Math.abs(actual[i]-expected[i]));}
                assertTrue("GPU difference "+max,max<=.0002);rows.put(new JSONObject().put("rows",r).put("columns",c).put("maxAbsDifference",max).put("tolerance",.0002).put("cpuMeanMs",cpuNs/10e6).put("gpuMeanIncludingTransfersMs",gpuNs/10e6));
            }
        }
        result.put("passed",true).put("cases",rows);Path output=Path.of("build/gpu-compute-evidence.json");Files.createDirectories(output.getParent());Files.writeString(output,result.toString(2));
    }
}
