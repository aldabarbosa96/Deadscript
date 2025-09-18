package core.controller;

import core.StickyMove;
import game.GameState;
import items.Item;
import render.Renderer;
import systems.ItemActionSystem;
import ui.input.InputHandler;
import utils.AudioManager;

public class InventoryController {
    public Effect handle(InputHandler.Command c, GameState state, Renderer renderer, StickyMove move) {
        switch (c) {
            case INVENTORY -> {
                state.inventoryOpen = false;
                state.invActionsOpen = false;
                renderer.log("Cierras el inventario.");
                return Effect.CHANGED;
            }
            case EQUIPMENT -> {
                state.inventoryOpen = false;
                state.invActionsOpen = false;
                state.equipmentOpen = true;
                state.eqActionsOpen = false;
                state.eqSelectOpen = false;
                move.reset();
                renderer.log("Abres el equipo.");
                AudioManager.playUi("/audio/equipment.wav");
                return Effect.CHANGED;
            }
            case STATS -> {
                state.inventoryOpen = false;
                state.invActionsOpen = false;
                state.statsOpen = true;
                move.reset();
                renderer.log("Abres el panel de estadísticas.");
                AudioManager.playUi("/audio/statisticsMenuSound.wav");
                return Effect.CHANGED;
            }
            case UP -> {
                if (state.invActionsOpen) {
                    if (!state.invActions.isEmpty()) {
                        state.invActionSel = Math.max(0, state.invActionSel - 1);
                        return Effect.CHANGED;
                    }
                } else if (!state.inventory.isEmpty()) {
                    state.invSel = Math.max(0, state.invSel - 1);
                    state.invActionsOpen = false;
                    return Effect.CHANGED;
                }
            }
            case DOWN -> {
                if (state.invActionsOpen) {
                    if (!state.invActions.isEmpty()) {
                        state.invActionSel = Math.min(state.invActions.size() - 1, state.invActionSel + 1);
                        return Effect.CHANGED;
                    }
                } else if (!state.inventory.isEmpty()) {
                    state.invSel = Math.min(state.inventory.size() - 1, state.invSel + 1);
                    state.invActionsOpen = false;
                    return Effect.CHANGED;
                }
            }
            case ACTION -> {
                if (state.inventory.isEmpty()) break;

                Item selected = state.inventory.get(Math.max(0, Math.min(state.invSel, state.inventory.size() - 1)));
                if (!state.invActionsOpen) {
                    state.invActions = ItemActionSystem.actionsFor(state, selected);
                    state.invActionSel = 0;
                    state.invActionsOpen = !state.invActions.isEmpty();
                    if (state.invActionsOpen)
                        renderer.log("Acciones para " + selected.getNombre() + ": " + String.join(", ", state.invActions) + ".");
                    return Effect.CHANGED;
                } else {
                    if (state.invActionSel >= 0 && state.invActionSel < state.invActions.size()) {
                        String action = state.invActions.get(state.invActionSel);
                        boolean changed = ItemActionSystem.apply(state, selected, action, renderer);
                        state.invActionsOpen = false;
                        if (changed) {
                            if (state.invSel >= state.inventory.size())
                                state.invSel = Math.max(0, state.inventory.size() - 1);
                        }
                        return Effect.CHANGED;
                    } else {
                        state.invActionsOpen = false;
                        return Effect.CHANGED;
                    }
                }
            }
            case QUIT -> {
                return Effect.QUIT;
            }
            default -> {
            }
        }
        return Effect.NONE;
    }
}
