package core;

import core.controller.*;
import game.GameState;
import render.Renderer;
import systems.PlayerSystem;
import systems.ZombieSystem;
import ui.input.InputHandler;
import utils.AudioManager;

public class Engine {
    private final InputHandler input;
    private final GameState state = new GameState();
    private final Renderer renderer = new Renderer();
    private final Clock clock = new Clock();
    private AudioManager ambient;
    private boolean running = true;
    private boolean dirty = true;
    private final StickyMove sticky = new StickyMove();
    private final InGameController inGame = new InGameController();
    private final InventoryController invCtrl = new InventoryController();
    private final EquipmentController eqCtrl = new EquipmentController();
    private final WorldActionController worldCtrl = new WorldActionController();

    public Engine(InputHandler input) {
        this.input = input;
        renderer.init(state, input.getTerminal());

        try {
            systems.LootSystem.scatterInitialLoot(state, renderer);
        } catch (Throwable t) {
            System.err.println("No se pudo generar loot inicial: " + t.getMessage());
        }

        try {
            AudioManager.startLoop("ambient", "/audio/forestAmbient1.wav", -7.5f);
        } catch (RuntimeException ex) {
            System.err.println("No se pudo iniciar audio ambiente: " + ex.getMessage());
        }


        clock.start();
        clock.onRendered();
        dirty = false;
    }

    public void run() {
        while (running) {
            // 1) INPUT
            InputHandler.Command c;
            while ((c = input.poll(0)) != InputHandler.Command.NONE) {
                if (state.inventoryOpen) {
                    Effect e = invCtrl.handle(c, state, renderer, sticky);
                    if (e == Effect.QUIT) {
                        running = false;
                        break;
                    }
                    if (e == Effect.CHANGED) dirty = true;
                } else if (state.equipmentOpen) {
                    Effect e = eqCtrl.handle(c, state, renderer, sticky);
                    if (e == Effect.QUIT) {
                        running = false;
                        break;
                    }
                    if (e == Effect.CHANGED) dirty = true;
                } else if (state.worldActionsOpen) {
                    Effect e = worldCtrl.handle(c, state, renderer);
                    if (e == Effect.QUIT) {
                        running = false;
                        break;
                    }
                    if (e == Effect.CHANGED) dirty = true;
                } else {
                    if (isArrow(c)) {
                        boolean moved = sticky.onArrow(c, state, renderer);
                        if (moved) dirty = true;
                    } else {
                        Effect e = inGame.handle(c, state, renderer, sticky);
                        if (e == Effect.QUIT) {
                            running = false;
                            break;
                        }
                        if (e == Effect.CHANGED) dirty = true;
                    }
                }
            }
            if (!running) break;

            boolean uiOpen = state.inventoryOpen || state.equipmentOpen || state.worldActionsOpen;

            // 2) AUTO-MOVE (mientras haya sticky activo y no hay UI encima)
            if (!uiOpen) {
                StickyMove.Vec v = sticky.getAutoDir();
                if (v.dx != 0 || v.dy != 0) {
                    if (PlayerSystem.tryMoveThrottled(state, v.dx, v.dy, renderer, true)) dirty = true;
                }
            }

            // 3) SIMULACIÓN (fixed steps)
            clock.consumeFixedSteps(dt -> {
                if (!uiOpen) {
                    boolean changed = ZombieSystem.update(state, renderer, dt);
                    PlayerSystem.drainNeeds(state, dt);
                    if (changed) dirty = true;
                }
            });

            // 4) RENDER
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
                Thread.sleep(3);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static boolean isArrow(InputHandler.Command c) {
        return c == InputHandler.Command.UP || c == InputHandler.Command.DOWN || c == InputHandler.Command.LEFT || c == InputHandler.Command.RIGHT;
    }

    public void shutdown() {
        if (ambient != null) AudioManager.shutdown();;
        renderer.shutdown();
    }
}
