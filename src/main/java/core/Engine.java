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
    private static final long MS = 1_000_000L;
    private static final long COMBINE_WINDOW_MS = 180;
    private static final long STICKY_RENEW_MS = 260;
    private static final long COMBINE_WINDOW_NS = COMBINE_WINDOW_MS * MS;
    private static final long STICKY_RENEW_NS = STICKY_RENEW_MS * MS;

    private long lastUpNs = 0, lastDownNs = 0, lastLeftNs = 0, lastRightNs = 0;

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
            int reqDx = 0, reqDy = 0;
            InputHandler.Command c;
            while ((c = input.poll(0)) != InputHandler.Command.NONE) {
                if (state.inventoryOpen) {
                    if (handleInventoryInput(c)) {
                        running = false;
                        break;
                    }
                } else if (state.equipmentOpen) {
                    if (handleEquipmentInput(c)) {
                        running = false;
                        break;
                    }
                } else {
                    if (isArrow(c)) {
                        handleArrow(c);
                    } else {
                        if (handleNonMove(c)) {
                            running = false;
                            break;
                        }
                    }
                }
            }
            if (!running) break;

            long now = System.nanoTime();
            boolean uiOpen = state.inventoryOpen || state.equipmentOpen;

            if (!uiOpen && isStickyActive(now)) {
                reqDx = stickDx;
                reqDy = stickDy;
            }

            if (!uiOpen && (reqDx != 0 || reqDy != 0)) {
                dirty |= PlayerSystem.tryMoveThrottled(state, reqDx, reqDy, renderer);
            }

            clock.consumeFixedSteps(dt -> {
                if (!uiOpen) {
                    boolean changed = false;
                    changed |= ZombieSystem.update(state, renderer, dt);
                    PlayerSystem.drainNeeds(state, dt);
                    if (changed) dirty = true;
                }
            });

            if (!uiOpen) {
                if (clock.shouldRender(dirty)) {
                    renderer.renderAll(state);
                    clock.onRendered();
                    dirty = false;
                }
            } else {
                if (dirty) {
                    renderer.renderAll(state);
                    clock.onRendered();
                    dirty = false;
                }
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void handleArrow(InputHandler.Command c) {
        long now = System.nanoTime();

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

    private boolean handleNonMove(InputHandler.Command c) {
        switch (c) {
            case INVENTORY -> {
                if (!state.inventoryOpen) {
                    state.inventoryOpen = true;
                    state.equipmentOpen = false;
                    stickUntilNs = 0;
                    renderer.log("Abres el inventario.");
                } else {
                    state.inventoryOpen = false;
                    renderer.log("Cierras el inventario.");
                }
                dirty = true;
            }
            case EQUIPMENT -> {
                if (!state.equipmentOpen) {
                    state.equipmentOpen = true;
                    state.inventoryOpen = false;
                    stickUntilNs = 0;
                    renderer.log("Abres el equipo.");
                } else {
                    state.equipmentOpen = false;
                    renderer.log("Cierras el equipo.");
                }
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

    private boolean handleInventoryInput(InputHandler.Command c) {
        switch (c) {
            case INVENTORY -> {
                state.inventoryOpen = false;
                renderer.log("Cierras el inventario.");
                dirty = true;
            }
            case UP -> {
                if (!state.inventory.isEmpty()) {
                    state.invSel = Math.max(0, state.invSel - 1);
                    dirty = true;
                }
            }
            case DOWN -> {
                if (!state.inventory.isEmpty()) {
                    state.invSel = Math.min(state.inventory.size() - 1, state.invSel + 1);
                    dirty = true;
                }
            }
            case QUIT -> {
                return true;
            }
            default -> {
            }
        }
        return false;
    }

    private boolean handleEquipmentInput(InputHandler.Command c) {
        final int SLOTS = 7; // Cabeza, Mochila, Torso, Mano izq., Mano der., Piernas, Pies
        switch (c) {
            case EQUIPMENT -> {
                state.equipmentOpen = false;
                renderer.log("Cierras el equipo.");
                dirty = true;
            }
            case UP, LEFT -> {
                state.eqSel = (state.eqSel - 1 + SLOTS) % SLOTS;
                dirty = true;
            }
            case DOWN, RIGHT -> {
                state.eqSel = (state.eqSel + 1) % SLOTS;
                dirty = true;
            }
            case ACTION -> {
                renderer.log("Acciones sobre slot: " + slotNameByIndex(state.eqSel)); // todo --> definir opciones concretas para cada ítem
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

    private String slotNameByIndex(int idx) {
        return switch (Math.floorMod(idx, 7)) {
            case 0 -> "Cabeza";
            case 1 -> "Mochila";
            case 2 -> "Pecho";
            case 3 -> "Mano izq.";
            case 4 -> "Mano der.";
            case 5 -> "Piernas";
            case 6 -> "Pies";
            default -> "-";
        };
    }


    public void shutdown() {
        renderer.shutdown();
    }
}
