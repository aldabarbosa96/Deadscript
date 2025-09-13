import core.Engine;
import ui.input.InputHandler;

public class MainGame {
    public static void main(String[] args) {
        InputHandler input = null;
        Engine engine = null;
        try {
            input = new InputHandler();    // teclado (raw mode)
            engine = new Engine(input);    // crea estado, renderer y clock
            engine.run();                  // bucle principal
        } finally {
            if (engine != null) engine.shutdown();
            if (input != null) try { input.close(); } catch (Exception ignored) {}
        }
    }
}
