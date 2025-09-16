package core.controller;

import game.GameState;
import render.Renderer;
import systems.WorldActionSystem;
import ui.input.InputHandler;

public class WorldActionController {
    public Effect handle(InputHandler.Command c, GameState state, Renderer renderer) {
        switch (c) {
            case UP -> {
                if (WorldActionSystem.handleArrow(state, true)) return Effect.CHANGED;
            }
            case DOWN -> {
                if (WorldActionSystem.handleArrow(state, false)) return Effect.CHANGED;
            }
            case ACTION -> {
                boolean changed = WorldActionSystem.executeSelected(state, renderer);
                return changed ? Effect.CHANGED : Effect.CHANGED;
            }
            case INVENTORY, EQUIPMENT, STATS, OPTIONS -> {
                state.worldActionsOpen = false;
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
