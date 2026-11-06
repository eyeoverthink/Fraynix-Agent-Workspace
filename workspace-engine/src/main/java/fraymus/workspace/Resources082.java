package fraymus.workspace;

import java.lang.management.ManagementFactory;
import org.json.*;

/** Read-only measurements. These counters are never evidence of a task outcome. */
final class Resources082 {
    static JSONObject snapshot() {
        var heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        var nonHeap=ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        var os=ManagementFactory.getOperatingSystemMXBean();
        long cpu=os instanceof com.sun.management.OperatingSystemMXBean bean?bean.getProcessCpuTime():-1;
        long configuredHeap=Long.parseLong(ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class).getVMOption("MaxHeapSize").getValue());
        var collectors=new JSONArray();
        for(var gc:ManagementFactory.getGarbageCollectorMXBeans())collectors.put(new JSONObject()
            .put("name",gc.getName()).put("count",gc.getCollectionCount()).put("millis",gc.getCollectionTime()));
        return new JSONObject().put("schema","java-resources-082-v1").put("pid",ProcessHandle.current().pid())
            .put("uptimeMillis",ManagementFactory.getRuntimeMXBean().getUptime()).put("profile",TaskCost080.profile())
            .put("processCpuNanos",cpu).put("heapUsedBytes",heap.getUsed()).put("heapCommittedBytes",heap.getCommitted())
            .put("heapMaxBytes",heap.getMax()).put("configuredMaxHeapBytes",configuredHeap).put("nonHeapUsedBytes",nonHeap.getUsed()).put("gc",collectors)
            .put("liveThreads",ManagementFactory.getThreadMXBean().getThreadCount())
            .put("scope","Read-only JVM counters; heap limit excludes native allocations, Electron and other processes. CPU -1 means unavailable. Not outcome authority.");
    }
}
