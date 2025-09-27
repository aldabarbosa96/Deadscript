package ui.input;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class InputHandler implements AutoCloseable {

    public enum Command {
        UP, DOWN, LEFT, RIGHT, REGENERATE, INVENTORY, EQUIPMENT, STATS, ACTION, OPTIONS, QUIT, NONE, BACK, CANCEL, ENTER_KEY, BACKSPACE_KEY
    }

    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private final Thread readerThread;
    private volatile boolean running = true;
    private final Terminal terminal;
    private final BindingReader reader;
    private final KeyMap<Command> keyMap;
    private final Attributes prevAttrs;
    private final BlockingQueue<Integer> charQueue = new LinkedBlockingQueue<>();
    private volatile BooleanSupplier typingEnabled = () -> false;


    public InputHandler() {
        try {
            terminal = TerminalBuilder.builder().system(true).jna(true).jansi(true).build();

            prevAttrs = terminal.enterRawMode();
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
        final int POLL_MS = 50; // pequeño timeout para re-chequear typingEnabled
        try {
            while (running) {
                boolean typing = typingEnabled.getAsBoolean();

                if (typing) {
                    // MODO TERMINAL (escritura libre)
                    int ch = reader.readCharacter(); // bloquea, pero aquí queremos escribir
                    if (!running) break;

                    if (ch == 13 || ch == 10) {
                        queue.offer(Command.ENTER_KEY);
                    } else if (ch == 127 || ch == 8) {
                        queue.offer(Command.BACKSPACE_KEY);
                    } else if (ch >= 32 && ch < 127) {
                        charQueue.offer(ch);
                    } else {
                        // ignora otros controles
                    }
                } else {
                    // MODO MENÚ (no escritura): no bloquees indefinidamente
                    int peek = reader.peekCharacter(POLL_MS);
                    if (!running) break;

                    // Si ha expirado el timeout, reevalúa modo y sigue
                    if (peek == NonBlockingReader.READ_EXPIRED) {
                        continue;
                    }

                    // Si durante la espera cambiamos a typing, vuelve al inicio
                    if (typingEnabled.getAsBoolean()) {
                        continue;
                    }

                    // Hay input disponible: ahora sí, parsea con el KeyMap
                    Command c = reader.readBinding(keyMap);
                    if (!running) break;
                    if (c != null) queue.offer(c);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private KeyMap<Command> buildKeyMap() {
        KeyMap<Command> km = new KeyMap<>();
        String ESC = "\u001B";

        // Flechas
        km.bind(Command.UP, ESC + "[A", ESC + "OA");
        km.bind(Command.DOWN, ESC + "[B", ESC + "OB");
        km.bind(Command.RIGHT, ESC + "[C", ESC + "OC");
        km.bind(Command.LEFT, ESC + "[D", ESC + "OD");

        // Atajos juego
        km.bind(Command.REGENERATE, "r", "R");
        km.bind(Command.QUIT, "q", "Q");

        // Menú acción/atajos
        km.bind(Command.ACTION, " ");
        km.bind(Command.INVENTORY, "i", "I");
        km.bind(Command.EQUIPMENT, "e", "E");
        km.bind(Command.STATS, "s", "S");
        km.bind(Command.OPTIONS, "o", "O");

        km.bind(Command.BACK, "z", "Z");

        km.bind(Command.ENTER_KEY, "\r", "\n");
        km.bind(Command.BACKSPACE_KEY, "\u007F", "\b");

        return km;
    }

    @Override
    public void close() {
        running = false;

        try {
            readerThread.interrupt();
        } catch (Exception ignore) {
        }

        try {
            if (terminal != null && prevAttrs != null) {
                terminal.setAttributes(prevAttrs);
            }
        } catch (Exception ignore) {
        }

        try {
            if (terminal != null) terminal.close();
        } catch (Exception ignore) {
        }

        try {
            if (readerThread != null) readerThread.join(1000);
        } catch (InterruptedException ignore) {
        }
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void setTypingEnabledSupplier(BooleanSupplier supplier) {
        this.typingEnabled = (supplier != null) ? supplier : () -> false;
    }

    public int pollChar() {
        Integer v = charQueue.poll();
        return v == null ? -1 : v;
    }

    public void flushQueues() {
        queue.clear();
        charQueue.clear();
    }
}
