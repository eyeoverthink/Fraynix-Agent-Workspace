import jade.Window;

public class Main {
    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--mario")) {
            System.setProperty("fraynix.level", "levels/mario-original.json");
            if (System.getProperty("fraynix.studio.workspace") == null)
                System.setProperty("fraynix.studio.workspace", java.nio.file.Path.of(System.getProperty("user.home"), ".fraynix-agent-workspace", "projects", "default", "game-lab").toString());
        }
        Window window = Window.get();
        if (java.util.Arrays.asList(args).contains("--factory-smoke")) window.configureFactorySmoke(false);
        if (java.util.Arrays.asList(args).contains("--factory-smoke-exit")) window.configureFactorySmoke(true);
        window.run();
    }
}
