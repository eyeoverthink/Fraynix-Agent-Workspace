package editor;

import imgui.ImGui;
import observers.EventSystem;
import observers.events.Event;
import observers.events.EventType;

public class MenuBar {

    public void imgui() {
        ImGui.beginMainMenuBar();

        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Save", "Ctrl+S")) {
                EventSystem.notify(null, new Event(EventType.SaveLevel));
            }

            if (ImGui.menuItem("Load", "Ctrl+O")) {
                EventSystem.notify(null, new Event(EventType.LoadLevel));
            }

            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Workspace")) {
            if (ImGui.menuItem("Show Fraynix")) ImGui.setWindowFocus("Fraynix");
            if (ImGui.menuItem("Reset studio layout")) jade.Window.getImguiLayer().resetStudioLayout();
            ImGui.endMenu();
        }
        ImGui.endMainMenuBar();
    }
}
