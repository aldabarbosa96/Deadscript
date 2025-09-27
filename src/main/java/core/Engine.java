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
    private boolean running = true;
    private boolean dirty = true;
    private final StickyMove sticky = new StickyMove();
    private final InGameController inGame = new InGameController();
    private final InventoryController invCtrl = new InventoryController();
    private final EquipmentController eqCtrl = new EquipmentController();
    private final WorldActionController worldCtrl = new WorldActionController();
    private final StatsController statsCtrl = new StatsController();
    private final PCController pcCtrl = new PCController();
    private boolean wasComputerOpen = false;
    private boolean dropFirstCommandAfterPc = false;
    private static final long PC_BOOT_NS = 4_500_000_000L;

    public Engine(InputHandler input) {
        this.input = input;
        renderer.init(state, input.getTerminal());
        input.setTypingEnabledSupplier(() -> state.computerOpen && state.computerBootDone);

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

            if (state.computerOpen && !state.computerBootDone) {
                if (System.nanoTime() - state.computerBootStartNs >= PC_BOOT_NS) {
                    state.computerBootDone = true;
                    state.computerBootJustEnded = true;
                    input.flushQueues();
                    dirty = true;
                }
            }
            // 1) INPUT
            InputHandler.Command c;
            while ((c = input.poll(0)) != InputHandler.Command.NONE) {
                if (dropFirstCommandAfterPc) {
                    dropFirstCommandAfterPc = false;
                    if (c == InputHandler.Command.OPTIONS) continue;
                }
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

                } else if (state.statsOpen) {
                    Effect e = statsCtrl.handle(c, state, renderer);
                    if (e == Effect.QUIT) {
                        running = false;
                        break;
                    }
                    if (e == Effect.CHANGED) dirty = true;

                } else if (state.computerOpen) {
                    Effect e = pcCtrl.handle(c, state, renderer);
                    if (e == Effect.QUIT) {
                        running = false;
                        break;
                    }
                    if (e == Effect.CHANGED) dirty = true;
                    continue;
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

            if (state.computerOpen && state.computerBootDone) {
                int ch;
                while ((ch = input.pollChar()) != -1) {
                    Effect e = pcCtrl.onChar(ch, state, renderer);
                    if (e == Effect.CHANGED) dirty = true;
                    if (e == Effect.QUIT) {
                        running = false;
                        break;
                    }
                }
                if (!running) break;
            }

            if (wasComputerOpen && !state.computerOpen) {
                input.flushQueues();
                dropFirstCommandAfterPc = true;
            }
            wasComputerOpen = state.computerOpen;

            boolean uiOpen = state.inventoryOpen || state.equipmentOpen || state.worldActionsOpen || state.statsOpen || state.computerOpen;

            if (renderer.ensureLayoutUpToDate(state)) {
                dirty = true;
            }
            if (state.computerOpen && state.computerBootJustEnded) {
                dirty = true;
                state.computerBootJustEnded = false;
            }

            // 2) AUTO-MOVE (mientras haya sticky activo y no hay UI encima)
            if (!uiOpen) {
                StickyMove.Vec v = sticky.getAutoDir();
                if (v.dx != 0 || v.dy != 0) {
                    if (PlayerSystem.tryMoveThrottled(state, v.dx, v.dy, renderer, false)) dirty = true;
                }
            }
            if (state.computerOpen && !state.computerBootDone) {
                dirty = true;
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
                boolean animPc = state.computerOpen && !state.computerBootDone;
                if (clock.shouldRender(dirty || animPc)) {
                    renderer.renderAll(state);
                    clock.onRendered();
                    dirty = false;
                }
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static boolean isArrow(InputHandler.Command c) {
        return c == InputHandler.Command.UP || c == InputHandler.Command.DOWN || c == InputHandler.Command.LEFT || c == InputHandler.Command.RIGHT;
    }

    public void shutdown() {
        AudioManager.shutdown();
        renderer.shutdown();
    }
}
