package ui.input;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class InputHandler implements AutoCloseable {
    public enum Command {UP, DOWN, LEFT, RIGHT, REGENERATE, QUIT, NONE}

    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private final Thread readerThread;
    private volatile boolean running = true;

    private final Terminal terminal;
    private final BindingReader reader;
    private final KeyMap<Command> keyMap;
    private final Attributes prevAttrs;

    public InputHandler() {
        try {
            terminal = TerminalBuilder.builder().system(true).jna(true).jansi(true).build();

            prevAttrs = terminal.enterRawMode(); // sin eco y no canónico
            reader = new BindingReader(terminal.reader());
            keyMap = buildKeyMap();

            readerThread = new Thread(this::loop, "InputReader");
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar InputHandler/JLine", e);
        }
    }

    public Command poll(long timeoutMs) {
        try {
            Command c = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            return c == null ? Command.NONE : c;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Command.NONE;
        }
    }

    private void loop() {
        while (running) {
            Command c = reader.readBinding(keyMap); // bloquea hasta tecla válida
            if (!running) break;
            if (c != null) queue.offer(c);
        }
    }

    private KeyMap<Command> buildKeyMap() {
        KeyMap<Command> km = new KeyMap<>();
        String ESC = "\u001B";

        // Flechas (dos variantes comunes)
        km.bind(Command.UP, ESC + "[A", ESC + "OA");
        km.bind(Command.DOWN, ESC + "[B", ESC + "OB");
        km.bind(Command.RIGHT, ESC + "[C", ESC + "OC");
        km.bind(Command.LEFT, ESC + "[D", ESC + "OD");

        // Atajos útiles
        km.bind(Command.REGENERATE, "r", "R");
        km.bind(Command.QUIT, "q", "Q");
        return km;
    }

    @Override
    public void close() {
        running = false;
        if (readerThread != null) readerThread.interrupt();
        try {
            if (terminal != null && prevAttrs != null) terminal.setAttributes(prevAttrs);
        } catch (Exception ignored) {
        }
        try {
            if (terminal != null) terminal.close();
        } catch (Exception ignored) {
        }
    }
}
