import jade.Window;

public class Main {
    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--mario")) {
            if (System.getProperty("fraynix.level") == null) System.setProperty("fraynix.level", "levels/mario-original.json");
            if (System.getProperty("fraynix.studio.workspace") == null)
                System.setProperty("fraynix.studio.workspace", java.nio.file.Path.of(System.getProperty("user.home"), ".fraynix-agent-workspace", "projects", "default", "game-lab").toString());
        }
        if (System.getProperty("fraynix.level.sha256") != null) {
            try {
                var schemas = new fraymus.factory.SchemaRegistry();
                factory.FactorySceneAdapter.registerSchemas(schemas);
                editor.LevelDocumentIO.read(java.nio.file.Path.of(System.getProperty("fraynix.level")), schemas);
            } catch (Exception e) { throw new IllegalStateException("Generated level validation failed", e); }
        }
        Window window = Window.get();
        if (java.util.Arrays.asList(args).contains("--factory-smoke")) window.configureFactorySmoke(false);
        if (java.util.Arrays.asList(args).contains("--factory-smoke-exit")) window.configureFactorySmoke(true);
        window.run();
    }
}
