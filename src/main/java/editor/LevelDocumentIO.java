package editor;

import factory.FactorySceneAdapter;
import fraymus.factory.*;
import scenes.Scene;
import java.nio.file.*;

/** Parses known component schemas before a live scene can be replaced. */
public final class LevelDocumentIO {
    private LevelDocumentIO() {}
    public static Scene read(Path path,SchemaRegistry schemas) throws java.io.IOException {
        if(!Files.isRegularFile(path) || Files.size(path)==0 || Files.size(path)>64L*1024*1024)
            throw new java.io.IOException("Choose a nonempty scene JSON or level.txt file under 64 MB");
        Scene scene=new Scene(new scenes.LevelEditorSceneInitializer());
        try {
            scene.load(path);
            if(scene.getGameObjects().size()>50000) throw new IllegalArgumentException("Level has too many objects");
            for(var object:scene.getGameObjects()) {
                physics2d.Physics2D.validate(object);
                var sprite=object.getComponent(components.SpriteRenderer.class);
                if(sprite!=null && sprite.getTexture()!=null && !Files.isRegularFile(Path.of(sprite.getTexture().getFilepath())))
                    throw new IllegalArgumentException("Missing image asset: "+sprite.getTexture().getFilepath());
            }
            new FactorySceneAdapter().verifyRoundTrip(scene,schemas); return scene;
        } catch(RuntimeException failure) { throw new java.io.IOException("Level import rejected: "+failure.getMessage(),failure); }
    }
    public static void write(Scene authoring,Path path) throws java.io.IOException {
        for(var object:authoring.getGameObjects()) physics2d.Physics2D.validate(object);
        authoring.save(path);
    }
}
