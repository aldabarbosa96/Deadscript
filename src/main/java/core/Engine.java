package core;

import game.GameState;
import render.Renderer;
import systems.PlayerSystem;
import systems.ZombieSystem;
import ui.input.InputHandler;

public class Engine {
    private final InputHandler input;
    private final GameState state = new GameState();
    private final Renderer renderer = new Renderer();
    private final Clock clock = new Clock();

    private boolean running = true;
    private boolean dirty = true;

    // ---- Ventanas de tolerancia / sticky ----
    private static final long MS = 1_000_000L;
    private static final long COMBINE_WINDOW_MS = 180;           // para formar diagonal al pulsar 2 flechas “casi a la vez”
    private static final long STICKY_RENEW_MS = 260;           // cuánto se mantiene/renueva la diagonal
    private static final long COMBINE_WINDOW_NS = COMBINE_WINDOW_MS * MS;
    private static final long STICKY_RENEW_NS = STICKY_RENEW_MS * MS;

    // Último time de cada flecha (para detectar combinaciones sin exigir simultaneidad perfecta)
    private long lastUpNs = 0, lastDownNs = 0, lastLeftNs = 0, lastRightNs = 0;

    // Diagonal “sticky”
    private int stickDx = 0, stickDy = 0;
    private long stickUntilNs = 0L;

    public Engine(InputHandler input) {
        this.input = input;
        renderer.init(state);
        clock.start();
        clock.onRendered();
        dirty = false;
    }

    public void run() {
        while (running) {

            // --- INPUT ---
            int reqDx = 0, reqDy = 0;
            InputHandler.Command c;
            while ((c = input.poll(0)) != InputHandler.Command.NONE) {
                if (isArrow(c)) {
                    handleArrow(c);
                } else {
                    if (handleNonMove(c)) {
                        running = false;
                        break;
                    }
                }
            }
            if (!running) break;

            long now = System.nanoTime();

            // Si hay diagonal sticky activa, la usamos (aunque este frame no lleguen teclas)
            if (isStickyActive(now)) {
                reqDx = stickDx;
                reqDy = stickDy;
            }

            // Ejecutar movimiento si procede
            if (reqDx != 0 || reqDy != 0) {
                dirty |= PlayerSystem.tryMoveThrottled(state, reqDx, reqDy, renderer);
            }

            // --- UPDATE ---
            clock.consumeFixedSteps(dt -> {
                boolean changed = false;
                changed |= ZombieSystem.update(state, renderer, dt);
                PlayerSystem.drainNeeds(state, dt);
                if (changed) dirty = true;
            });

            // --- RENDER ---
            if (clock.shouldRender(dirty)) {
                renderer.renderAll(state);
                clock.onRendered();
                dirty = false;
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }
    }

    // ===== Movimiento: manejo de flechas =====
    private void handleArrow(InputHandler.Command c) {
        long now = System.nanoTime();

        // Registrar timestamp por eje
        switch (c) {
            case UP -> lastUpNs = now;
            case DOWN -> lastDownNs = now;
            case LEFT -> lastLeftNs = now;
            case RIGHT -> lastRightNs = now;
            default -> {
            }
        }

        int dx = axisDx(c);
        int dy = axisDy(c);

        // Si ya hay diagonal sticky, actualizar/renovar por eje
        if (isStickyActive(now)) {
            boolean changed = false;
            if (dx != 0) {
                stickDx = dx;
                changed = true;
            }
            if (dy != 0) {
                stickDy = dy;
                changed = true;
            }
            if (changed || belongsToSticky(c)) renewSticky(now);
            return;
        }

        // Intentar formar diagonal SIN exigir simultaneidad exacta:
        //   - Si llega vertical, combínala con horizontal “reciente”
        //   - Si llega horizontal, combínala con vertical “reciente”
        if (dy != 0) {
            int recentHx = recentHorizontalDir(now);
            if (recentHx != 0) {
                setSticky(recentHx, dy, now);
                return;
            }
        } else if (dx != 0) {
            int recentVy = recentVerticalDir(now);
            if (recentVy != 0) {
                setSticky(dx, recentVy, now);
                return;
            }
        }

        // Si no hay diagonal aún, damos un paso cardinal inmediato.
        // (Para el movimiento continuo cardinal confiamos en el autorepeat del SO)
        dirty |= PlayerSystem.tryMoveThrottled(state, dx, dy, renderer);
    }

    private static boolean isArrow(InputHandler.Command c) {
        return c == InputHandler.Command.UP || c == InputHandler.Command.DOWN || c == InputHandler.Command.LEFT || c == InputHandler.Command.RIGHT;
    }

    private static int axisDx(InputHandler.Command c) {
        return (c == InputHandler.Command.RIGHT) ? 1 : (c == InputHandler.Command.LEFT) ? -1 : 0;
    }

    private static int axisDy(InputHandler.Command c) {
        return (c == InputHandler.Command.DOWN) ? 1 : (c == InputHandler.Command.UP) ? -1 : 0;
    }

    private int recentHorizontalDir(long now) {
        if (now - lastRightNs <= COMBINE_WINDOW_NS) return 1;
        if (now - lastLeftNs <= COMBINE_WINDOW_NS) return -1;
        return 0;
    }

    private int recentVerticalDir(long now) {
        if (now - lastUpNs <= COMBINE_WINDOW_NS) return -1;
        if (now - lastDownNs <= COMBINE_WINDOW_NS) return 1;
        return 0;
    }

    private boolean isStickyActive(long now) {
        return now < stickUntilNs;
    }

    private void setSticky(int dx, int dy, long now) {
        stickDx = dx;
        stickDy = dy;
        stickUntilNs = now + STICKY_RENEW_NS;
    }

    private void renewSticky(long now) {
        stickUntilNs = now + STICKY_RENEW_NS;
    }

    private boolean belongsToSticky(InputHandler.Command c) {
        int dx = axisDx(c), dy = axisDy(c);
        if (dx != 0 && stickDx == dx) return true;
        if (dy != 0 && stickDy == dy) return true;
        return false;
    }

    // ===== Resto de comandos =====
    private boolean handleNonMove(InputHandler.Command c) {
        switch (c) {
            case INVENTORY -> {
                renderer.log("Abres el inventario.");
                dirty = true;
            }
            case EQUIPMENT -> {
                renderer.log("Abres el equipo.");
                dirty = true;
            }
            case STATS -> {
                renderer.log("Abres el panel de estadísticas.");
                dirty = true;
            }
            case ACTION -> {
                renderer.log("Acción principal.");
                dirty = true;
            }
            case OPTIONS -> {
                renderer.log("Abres el menú de opciones.");
                dirty = true;
            }
            case REGENERATE -> {
                state.resetMap();
                renderer.onMapChanged(state);
                renderer.log("Nuevo mapa generado.");
                dirty = true;
            }
            case QUIT -> {
                return true;
            }
            default -> {
            }
        }
        return false;
    }

    public void shutdown() {
        renderer.shutdown();
    }
}
