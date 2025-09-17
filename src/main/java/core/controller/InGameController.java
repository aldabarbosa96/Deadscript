package core.controller;

import core.StickyMove;
import game.GameState;
import render.Renderer;
import systems.CombatSystem;
import systems.LootSystem;
import systems.WorldActionSystem;
import ui.input.InputHandler;

public class InGameController {
    public Effect handle(InputHandler.Command c, GameState state, Renderer renderer, StickyMove move) {
        switch (c) {
            case INVENTORY -> {
                if (!state.inventoryOpen) {
                    state.inventoryOpen = true;
                    state.equipmentOpen = false;
                    move.reset();
                    renderer.log("Abres el inventario.");
                    return Effect.CHANGED;
                }
            }
            case EQUIPMENT -> {
                if (!state.equipmentOpen) {
                    state.equipmentOpen = true;
                    state.inventoryOpen = false;
                    move.reset();
                    renderer.log("Abres el equipo.");
                    return Effect.CHANGED;
                }
            }
            case STATS -> {
                renderer.log("Abres el panel de estadísticas.");
                return Effect.CHANGED;
            }
            case ACTION -> {
                if (!state.worldActionsOpen) {
                    move.reset();
                    if (CombatSystem.anyVisibleHostileInMainFov(state, renderer)) {
                        CombatSystem.quickAttack(state, renderer);
                        return Effect.CHANGED;
                    } else {
                        WorldActionSystem.openContextActions(state, renderer);
                        return Effect.CHANGED;
                    }
                }
            }
            case OPTIONS -> {
                renderer.log("Abres el menú de opciones.");
                return Effect.CHANGED;
            }
            case REGENERATE -> {
                state.resetMap();
                renderer.onMapChanged(state);
                LootSystem.scatterInitialLoot(state, renderer);
                renderer.log("Nuevo mapa generado.");
                return Effect.CHANGED;
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
