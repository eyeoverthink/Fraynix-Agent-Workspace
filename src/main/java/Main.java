import jade.Window;

public class Main {
    public static void main(String[] args) {
        Window window = Window.get();
        if (java.util.Arrays.asList(args).contains("--factory-smoke")) window.configureFactorySmoke(false);
        if (java.util.Arrays.asList(args).contains("--factory-smoke-exit")) window.configureFactorySmoke(true);
        window.run();
    }
}
