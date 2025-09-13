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

    public Engine(InputHandler input) {
        this.input = input;
        renderer.init(state);
        clock.start();
        clock.onRendered();
        dirty = false;
    }

    public void run() {
        while (running) {

            // 1) INPUT
            InputHandler.Command cmd;
            while ((cmd = input.poll(0)) != InputHandler.Command.NONE) {
                if (handle(cmd)) {
                    running = false;
                    break;
                }
            }
            if (!running) break;

            // 2) UPDATE con timestep fijo
            clock.consumeFixedSteps(dt -> {
                boolean changed = false;
                changed |= ZombieSystem.update(state, renderer, dt);
                PlayerSystem.drainNeeds(state, dt);
                if (changed) dirty = true;
            });

            // 3) RENDER con pacing estable (parpadeo constante)
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

    private boolean handle(InputHandler.Command c) {
        switch (c) {
            case UP -> dirty |= PlayerSystem.tryMoveThrottled(state, 0, -1, renderer);
            case DOWN -> dirty |= PlayerSystem.tryMoveThrottled(state, 0, 1, renderer);
            case LEFT -> dirty |= PlayerSystem.tryMoveThrottled(state, -1, 0, renderer);
            case RIGHT -> dirty |= PlayerSystem.tryMoveThrottled(state, 1, 0, renderer);

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
