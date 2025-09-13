import core.Engine;
import ui.input.InputHandler;

public class MainGame {
    public static void main(String[] args) {
        runGame();
    }

    private static void runGame() {
        InputHandler input = null;
        Engine engine = null;
        try {
            input = new InputHandler();
            engine = new Engine(input);
            engine.run();
        } finally {
            if (engine != null) engine.shutdown();
            if (input != null) try {
                input.close();
            } catch (Exception ignored) {
            }
        }
    }
}

