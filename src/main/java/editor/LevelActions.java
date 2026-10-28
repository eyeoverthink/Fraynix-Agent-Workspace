package editor;

import factory.*;
import fraymus.factory.*;
import jade.Window;
import scenes.Scene;
import java.nio.file.*;
import java.util.concurrent.*;

/** File dialogs run on AWT; all world commits stay on the engine thread. */
public final class LevelActions implements AutoCloseable {
    private record Selected(String action,Path path) {}
    private CompletableFuture<Selected> choosing;
    private volatile javax.swing.JFileChooser activeDialog;
    private String status="Save stores the authoring level. Import accepts scene JSON and original level.txt.";
    public String status() { return status; }
    public boolean busy() { return choosing!=null; }
    public void save() {
        try {
            Scene authored=Window.authoringForSave();
            Path path=Window.getFactorySession().workspace().resolve("authored-scene.json");
            LevelDocumentIO.write(authored,path); Window.getFactorySession().save(authored);
            status="Saved level: "+path;
        } catch(Exception failure) {status="Save failed: "+failure.getMessage();}
    }
    public void choose(String action) {
        if(busy()) return;
        if(action.equals("Import") && Window.isPlaying()) {status="Stop Play before importing a level.";return;}
        choosing=new CompletableFuture<>();
        var future=choosing;
        javax.swing.SwingUtilities.invokeLater(()->{
            try {
                if(future.isCancelled()) return;
                var dialog=new javax.swing.JFileChooser(Window.getFactorySession().workspace().toFile());
                activeDialog=dialog;
                dialog.setDialogTitle(action+" level"); dialog.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Scene JSON or level.txt","json","txt"));
                if(action.equals("Export")) dialog.setSelectedFile(new java.io.File("level.json"));
                int result=action.equals("Import")?dialog.showOpenDialog(null):dialog.showSaveDialog(null);
                if(result==javax.swing.JFileChooser.APPROVE_OPTION && action.equals("Export") && dialog.getSelectedFile().exists()
                    && javax.swing.JOptionPane.showConfirmDialog(dialog,"Replace this exported level?","Export level",javax.swing.JOptionPane.YES_NO_OPTION)!=javax.swing.JOptionPane.YES_OPTION)
                    result=javax.swing.JFileChooser.CANCEL_OPTION;
                future.complete(new Selected(action,result==javax.swing.JFileChooser.APPROVE_OPTION?dialog.getSelectedFile().toPath():null));
                activeDialog=null;
            } catch(Exception failure) {future.completeExceptionally(failure);}
        });
    }
    public void tick() {
        if(choosing==null || !choosing.isDone()) return;
        try {
            Selected selected=choosing.join();
            if(selected.path()==null) {status=selected.action()+" canceled";return;}
            if(selected.action().equals("Export")) {
                LevelDocumentIO.write(Window.authoringForSave(),selected.path()); status="Exported level: "+selected.path();
            } else importPath(selected.path());
        } catch(Exception failure) {status="Level operation failed: "+failure.getMessage();}
        finally {choosing=null;}
    }
    public void importPath(Path path) throws java.io.IOException {
        if(Window.isPlaying()) throw new IllegalStateException("Stop Play before importing a level");
        var session=Window.getFactorySession(); Scene imported=LevelDocumentIO.read(path,session.schemas());
        var adapter=new FactorySceneAdapter(); var candidate=new FactoryWorld(session.schemas()); adapter.capture(imported,candidate);
        Scene current=Window.getScene();session.capture(current);
        String expected=SceneKnowledge.fingerprint(session.world().snapshot());
        Path backup=session.workspace().resolve("level-backups/before-import-"+java.util.UUID.randomUUID()+".json");
        LevelDocumentIO.write(current,backup);
        session.commitScene(current,candidate.snapshot(),expected,Window::installFactoryScene);
        status="Imported "+imported.getGameObjects().size()+" objects from "+path+". Previous level: "+backup;
    }
    @Override public void close() {
        if(choosing!=null) choosing.cancel(false);
        if(activeDialog!=null) javax.swing.SwingUtilities.invokeLater(()->{var dialog=activeDialog;if(dialog!=null) {var window=javax.swing.SwingUtilities.getWindowAncestor(dialog);if(window!=null) window.dispose();activeDialog=null;}});
    }
}
