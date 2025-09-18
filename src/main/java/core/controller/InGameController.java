package core.controller;

import core.StickyMove;
import game.GameState;
import render.Renderer;
import systems.CombatSystem;
import systems.LootSystem;
import systems.WorldActionSystem;
import ui.input.InputHandler;
import utils.AudioManager;

public class InGameController {
    public Effect handle(InputHandler.Command c, GameState state, Renderer renderer, StickyMove move) {
        switch (c) {
            case INVENTORY -> {
                if (!state.inventoryOpen) {
                    state.inventoryOpen = true;
                    state.equipmentOpen = false;
                    move.reset();
                    renderer.log("Abres el inventario.");
                    AudioManager.playUi("/audio/backpackZip1.wav");
                    return Effect.CHANGED;
                }
            }
            case EQUIPMENT -> {
                if (!state.equipmentOpen) {
                    state.equipmentOpen = true;
                    state.inventoryOpen = false;
                    move.reset();
                    renderer.log("Abres el equipo.");
                    AudioManager.playUi("/audio/equipment.wav");
                    return Effect.CHANGED;
                }
            }
            case STATS -> {
                if (!state.statsOpen) {
                    state.inventoryOpen = false;
                    state.equipmentOpen = false;
                    state.worldActionsOpen = false;
                    state.statsOpen = true;
                    move.reset();
                    renderer.log("Abres el panel de estadísticas.");
                    AudioManager.playUi("/audio/statisticsMenuSound.wav");
                    return Effect.CHANGED;
                }
            }
            case ACTION -> {
                if (!state.worldActionsOpen) {
                    move.reset();

                    if (CombatSystem.anyAdjacentZombie(state)) {
                        CombatSystem.quickAttack(state, renderer);
                        return Effect.CHANGED;
                    }
                    if (WorldActionSystem.pickLootNearPlayer(state) != null) {
                        WorldActionSystem.openContextActions(state, renderer);
                        return Effect.CHANGED;
                    }
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
