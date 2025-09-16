package core;

import game.GameState;
import items.EquipmentSlot;
import items.Item;
import render.Renderer;
import systems.PlayerSystem;
import systems.ZombieSystem;
import ui.input.InputHandler;
import utils.AudioLoop;

public class Engine {
    private final InputHandler input;
    private final GameState state = new GameState();
    private final Renderer renderer = new Renderer();
    private final Clock clock = new Clock();
    private AudioLoop ambient;
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

        try {
            ambient = new AudioLoop("/audio/forestAmbient1.wav");
            ambient.setGainDb(-14f);
            ambient.start();
        } catch (RuntimeException ex) {
            System.err.println("No se pudo iniciar audio ambiente: " + ex.getMessage());
        }

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

        int vx = dx, vy = dy;

        if (isStickyActive(now)) {
            boolean changed = false;
            int ndx = stickDx, ndy = stickDy;
            if (dx != 0) {
                ndx = dx;
                changed = true;
            }
            if (dy != 0) {
                ndy = dy;
                changed = true;
            }
            if (changed || belongsToSticky(c)) renewSticky(now);
            vx = ndx;
            vy = ndy;

            state.lastDx = vx;
            state.lastDy = vy;
            return;
        }

        if (dy != 0) {
            int recentHx = recentHorizontalDir(now);
            if (recentHx != 0) {
                vx = recentHx;
                vy = dy;
                setSticky(vx, vy, now);
                state.lastDx = vx;
                state.lastDy = vy;
                return;
            }
        } else if (dx != 0) {
            int recentVy = recentVerticalDir(now);
            if (recentVy != 0) {
                vx = dx;
                vy = recentVy;
                setSticky(vx, vy, now);
                state.lastDx = vx;
                state.lastDy = vy;
                return;
            }
        }

        state.lastDx = vx;
        state.lastDy = vy;

        dirty |= PlayerSystem.tryMoveThrottled(state, vx, vy, renderer);
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
                state.invActionsOpen = false;
                renderer.log("Cierras el inventario.");
                dirty = true;
            }
            case EQUIPMENT -> {
                state.inventoryOpen = false;
                state.invActionsOpen = false;

                state.equipmentOpen = true;
                state.eqActionsOpen = false;
                state.eqSelectOpen = false;

                stickUntilNs = 0;
                renderer.log("Abres el equipo.");
                dirty = true;
            }
            case UP -> {
                if (state.invActionsOpen) {
                    if (!state.invActions.isEmpty()) {
                        state.invActionSel = Math.max(0, state.invActionSel - 1);
                        dirty = true;
                    }
                } else if (!state.inventory.isEmpty()) {
                    state.invSel = Math.max(0, state.invSel - 1);
                    state.invActionsOpen = false;
                    dirty = true;
                }
            }
            case DOWN -> {
                if (state.invActionsOpen) {
                    if (!state.invActions.isEmpty()) {
                        state.invActionSel = Math.min(state.invActions.size() - 1, state.invActionSel + 1);
                        dirty = true;
                    }
                } else if (!state.inventory.isEmpty()) {
                    state.invSel = Math.min(state.inventory.size() - 1, state.invSel + 1);
                    state.invActionsOpen = false;
                    dirty = true;
                }
            }
            case ACTION -> {
                if (state.inventory.isEmpty()) break;

                items.Item selected = state.inventory.get(Math.max(0, Math.min(state.invSel, state.inventory.size() - 1)));

                if (!state.invActionsOpen) {
                    state.invActions = systems.ItemActionSystem.actionsFor(state, selected);
                    state.invActionSel = 0;
                    state.invActionsOpen = !state.invActions.isEmpty();
                    if (state.invActionsOpen)
                        renderer.log("Acciones para " + selected.getNombre() + ": " + String.join(", ", state.invActions) + ".");
                    dirty = true;
                } else {
                    if (state.invActionSel >= 0 && state.invActionSel < state.invActions.size()) {
                        String action = state.invActions.get(state.invActionSel);
                        boolean changed = systems.ItemActionSystem.apply(state, selected, action, renderer);
                        state.invActionsOpen = false;
                        dirty = true;
                        if (changed) {
                            if (state.invSel >= state.inventory.size())
                                state.invSel = Math.max(0, state.inventory.size() - 1);
                        }
                    } else {
                        state.invActionsOpen = false;
                        dirty = true;
                    }
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
        final int SLOTS = 7; // Cabeza, Mochila, Pecho, Mano izq., Mano der., Piernas, Pies

        switch (c) {
            case EQUIPMENT -> {
                state.equipmentOpen = false;
                state.eqActionsOpen = false;
                state.eqSelectOpen = false;
                renderer.log("Cierras el equipo.");
                dirty = true;
            }
            case INVENTORY -> {
                state.equipmentOpen = false;
                state.eqActionsOpen = false;
                state.eqSelectOpen = false;

                state.inventoryOpen = true;
                state.invActionsOpen = false;

                stickUntilNs = 0;
                renderer.log("Abres el inventario.");
                dirty = true;
            }
            case UP, LEFT -> {
                if (state.eqSelectOpen) {
                    if (state.eqSelectItems != null && !state.eqSelectItems.isEmpty()) {
                        state.eqSelectSel = Math.max(0, state.eqSelectSel - 1);
                        dirty = true;
                    }
                } else if (state.eqActionsOpen) {
                    if (state.eqActions != null && !state.eqActions.isEmpty()) {
                        state.eqActionSel = Math.max(0, state.eqActionSel - 1);
                        dirty = true;
                    }
                } else {
                    state.eqSel = (state.eqSel - 1 + SLOTS) % SLOTS;
                    dirty = true;
                }
            }
            case DOWN, RIGHT -> {
                if (state.eqSelectOpen) {
                    if (state.eqSelectItems != null && !state.eqSelectItems.isEmpty()) {
                        state.eqSelectSel = Math.min(state.eqSelectItems.size() - 1, state.eqSelectSel + 1);
                        dirty = true;
                    }
                } else if (state.eqActionsOpen) {
                    if (state.eqActions != null && !state.eqActions.isEmpty()) {
                        state.eqActionSel = Math.min(state.eqActions.size() - 1, state.eqActionSel + 1);
                        dirty = true;
                    }
                } else {
                    state.eqSel = (state.eqSel + 1) % SLOTS;
                    dirty = true;
                }
            }
            case ACTION -> {
                EquipmentSlot slot = slotByIndex(state.eqSel);

                if (state.eqSelectOpen) {
                    if (state.eqSelectItems != null && !state.eqSelectItems.isEmpty()) {
                        int idx = Math.max(0, Math.min(state.eqSelectSel, state.eqSelectItems.size() - 1));
                        Item choice = state.eqSelectItems.get(idx);
                        systems.ItemActionSystem.equipToSlot(state, choice, slot, renderer);
                    }
                    state.eqSelectOpen = false;
                    state.eqActionsOpen = false;
                    dirty = true;
                    break;
                }

                if (state.eqActionsOpen) {
                    if (state.eqActionSel >= 0 && state.eqActionSel < state.eqActions.size()) {
                        String action = state.eqActions.get(state.eqActionSel).toLowerCase();
                        switch (action) {
                            case "equipar" -> {
                                state.eqSelectItems = systems.ItemActionSystem.equippablesForSlot(state, slot);
                                state.eqSelectSel = 0;
                                state.eqSelectOpen = true;
                                dirty = true;
                            }
                            case "desequipar" -> {
                                systems.ItemActionSystem.unequipSlot(state, slot, renderer);
                                state.eqActionsOpen = false;
                                dirty = true;
                            }
                            default -> {
                                state.eqActionsOpen = false;
                                dirty = true;
                            }
                        }
                    } else {
                        state.eqActionsOpen = false;
                        dirty = true;
                    }
                    break;
                }

                Item cur = itemInSlot(slot);
                java.util.ArrayList<String> actions = new java.util.ArrayList<>();
                if (cur == null) actions.add("Equipar");
                else actions.add("Desequipar");
                actions.add("Cancelar");

                state.eqActions = actions;
                state.eqActionSel = 0;
                state.eqActionsOpen = true;
                renderer.log("Acciones sobre " + slotNameByIndex(state.eqSel) + ": " + String.join(", ", actions) + ".");
                dirty = true;
            }
            case QUIT -> {
                return true;
            }
            default -> { /* nada */ }
        }
        return false;
    }


    private EquipmentSlot slotByIndex(int idx) {
        return switch (Math.floorMod(idx, 7)) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.BACKPACK;
            case 2 -> EquipmentSlot.TORSO;
            case 3 -> EquipmentSlot.OFF_HAND;
            case 4 -> EquipmentSlot.MAIN_HAND;
            case 5 -> EquipmentSlot.LEGS;
            case 6 -> EquipmentSlot.FEET;
            default -> EquipmentSlot.TORSO;
        };
    }

    private Item itemInSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> state.equipment.getHead();
            case TORSO -> state.equipment.getChest();
            case HANDS -> state.equipment.getHands();
            case LEGS -> state.equipment.getLegs();
            case FEET -> state.equipment.getFeet();
            case MAIN_HAND -> state.equipment.getMainHand();
            case OFF_HAND -> state.equipment.getOffHand();
            case BACKPACK -> state.equipment.getBackpack();
        };
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
        if (ambient != null) ambient.close();
        renderer.shutdown();
    }
}
