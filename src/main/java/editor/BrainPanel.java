package editor;

import imgui.ImGui;
import nervous.BrainRuntime;
import java.nio.file.*;

/** Views the same neural core receiving live gameplay observations. */
public final class BrainPanel {
    private String error="";
    public void imgui() {
        ImGui.textWrapped("Hrm nervous system / actual live sensory activity");
        ImGui.textWrapped(BrainRuntime.status());
        if(ImGui.button("Start nervous system")) BrainRuntime.start();
        if(ImGui.checkbox("Experimental plasticity",BrainRuntime.learning())) BrainRuntime.learning(!BrainRuntime.learning());
        if(ImGui.checkbox("Experimental neural jump output",agent.GameInput.neuralJump())) agent.GameInput.neuralJump(!agent.GameInput.neuralJump());
        ImGui.textWrapped(agent.GameInput.learningStatus());
        if(ImGui.collapsingHeader("Recent AI Play outcomes")) {
            var results=agent.GameInput.experience().recent;
            for(int i=results.size()-1;i>=Math.max(0,results.size()-5);i--) {
                var run=results.get(i);
                ImGui.textWrapped(String.format(java.util.Locale.ROOT,"Run %d: %s | %.2f units | %d pickups | %d damage | %d ticks",
                    run.episode,run.outcome,run.progress,run.pickups,run.damage,run.ticks));
            }
        }
        ImGui.textWrapped("AI Play learns local motor strategy from progress, power-ups, damage and failures. Its per-level experience is saved between runs. Neural plasticity below is a separate experiment; improved neural gameplay has not been evaluated.");
        var core=BrainRuntime.core();
        if(core!=null) {
            ImGui.text(core.neurons()+" neurons | "+core.synapses()+" synapses");
            ImGui.text("Simulation ticks: "+core.ticks()+" | spikes: "+core.spikeCount());
            ImGui.text(String.format(java.util.Locale.ROOT,"Cortex step %.2f ms | weight change %.5f",BrainRuntime.milliseconds(),core.plasticity()));
            ImGui.progressBar((float)core.spikeCount()/core.neurons(),-1,18,"Firing population");
            ImGui.separator(); ImGui.text("Sensory channels");
            String[] labels={"Goal direction","Horizontal velocity","Vertical velocity","Grounded","Floor ahead","Wall ahead","Enemy ahead"};
            float[] sensory=agent.GameInput.sensory();
            for(int i=0;i<sensory.length;i++) ImGui.text(String.format(java.util.Locale.ROOT,"%s: %.3f",labels[i],sensory[i]));
        }
        Path checkpoint=jade.Window.getFactorySession().workspace().resolve("brain/cortex-v1.json");
        ImGui.beginDisabled(core==null || BrainRuntime.busy());
        if(ImGui.button("Save brain")) { try { BrainRuntime.save(checkpoint); error=""; } catch(RuntimeException failure) {error=failure.getMessage();} }
        ImGui.endDisabled(); ImGui.sameLine();
        ImGui.beginDisabled(BrainRuntime.busy() || !Files.isRegularFile(checkpoint) || agent.GameInput.automatic());
        if(ImGui.button("Restore brain")) { try {BrainRuntime.load(checkpoint);error="";} catch(RuntimeException failure) {error=failure.getMessage();} }
        ImGui.endDisabled();
        ImGui.textWrapped("Brain checkpoint: "+checkpoint);
        if(!error.isEmpty()) ImGui.textWrapped(error);
    }
}
