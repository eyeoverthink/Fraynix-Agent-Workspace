package editor;

import imgui.ImGui;
import imgui.flag.*;
import imgui.type.ImInt;
import jade.Window;
import java.nio.file.*;

/** Versioned per-project layout; a default workspace never opens the library demo. */
public final class StudioLayout {
    private final Path ini;
    private boolean reset;
    public StudioLayout(Path workspace) {
        ini = workspace.resolve("studio-layout-v1.ini"); reset = !Files.isRegularFile(ini);
        try { Files.createDirectories(workspace); } catch (java.io.IOException error) { throw new IllegalStateException("Cannot create studio layout directory", error); }
    }
    public String iniPath() { return ini.toString(); }
    public void reset() { reset = true; }
    public void dock(int id) {
        if (!reset) return;
        reset = false;
        imgui.internal.ImGui.dockBuilderRemoveNode(id);
        imgui.internal.ImGui.dockBuilderAddNode(id, imgui.internal.flag.ImGuiDockNodeFlags.DockSpace);
        imgui.internal.ImGui.dockBuilderSetNodeSize(id, Window.getWidth(), Window.getHeight() - ImGui.getFrameHeight());
        ImInt left = new ImInt(), centerAndRight = new ImInt(), right = new ImInt(), center = new ImInt();
        imgui.internal.ImGui.dockBuilderSplitNode(id, ImGuiDir.Left, .21f, left, centerAndRight);
        imgui.internal.ImGui.dockBuilderSplitNode(centerAndRight.get(), ImGuiDir.Right, .40f, right, center);
        ImInt hierarchy = new ImInt(), properties = new ImInt(), assets = new ImInt(), viewport = new ImInt();
        imgui.internal.ImGui.dockBuilderSplitNode(left.get(), ImGuiDir.Up, .36f, hierarchy, properties);
        imgui.internal.ImGui.dockBuilderSplitNode(center.get(), ImGuiDir.Down, .25f, assets, viewport);
        imgui.internal.ImGui.dockBuilderDockWindow("Scene", hierarchy.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Properties", properties.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Fraynix", right.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Game Viewport", viewport.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Assets", assets.get());
        imgui.internal.ImGui.dockBuilderDockWindow("Editor Tools", assets.get());
        imgui.internal.ImGui.dockBuilderFinish(id);
    }
    public static void style() {
        var style = ImGui.getStyle();
        style.setWindowPadding(12, 10); style.setFrameRounding(3); style.setTabRounding(3); style.setWindowRounding(0);
        style.setColor(ImGuiCol.WindowBg, .043f, .043f, .059f, 1);
        style.setColor(ImGuiCol.ChildBg, .060f, .057f, .073f, 1);
        style.setColor(ImGuiCol.PopupBg, .083f, .075f, .094f, 1);
        style.setColor(ImGuiCol.Text, .91f, .90f, .92f, 1);
        style.setColor(ImGuiCol.TextDisabled, .56f, .54f, .60f, 1);
        style.setColor(ImGuiCol.Border, .18f, .17f, .20f, 1);
        style.setColor(ImGuiCol.TitleBg, .065f, .060f, .078f, 1);
        style.setColor(ImGuiCol.TitleBgActive, .11f, .09f, .13f, 1);
        style.setColor(ImGuiCol.FrameBg, .12f, .11f, .14f, 1);
        style.setColor(ImGuiCol.FrameBgHovered, .20f, .16f, .21f, 1);
        style.setColor(ImGuiCol.FrameBgActive, .27f, .18f, .24f, 1);
        style.setColor(ImGuiCol.Tab, .10f, .09f, .12f, 1);
        style.setColor(ImGuiCol.TabActive, .30f, .08f, .19f, 1);
        style.setColor(ImGuiCol.TabHovered, .43f, .10f, .23f, 1);
        style.setColor(ImGuiCol.TabUnfocused, .08f, .07f, .10f, 1);
        style.setColor(ImGuiCol.TabUnfocusedActive, .21f, .10f, .17f, 1);
        style.setColor(ImGuiCol.Button, .22f, .09f, .17f, 1);
        style.setColor(ImGuiCol.ButtonHovered, .40f, .09f, .21f, 1);
        style.setColor(ImGuiCol.ButtonActive, .55f, .09f, .25f, 1);
        style.setColor(ImGuiCol.Header, .24f, .09f, .18f, 1);
        style.setColor(ImGuiCol.HeaderHovered, .36f, .10f, .22f, 1);
        style.setColor(ImGuiCol.HeaderActive, .43f, .10f, .23f, 1);
        style.setColor(ImGuiCol.CheckMark, .85f, .17f, .41f, 1);
        style.setColor(ImGuiCol.SliderGrab, .76f, .09f, .36f, 1);
    }
}
