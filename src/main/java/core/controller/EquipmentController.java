package core.controller;

import core.StickyMove;
import game.GameState;
import items.EquipmentSlot;
import items.Item;
import render.Renderer;
import systems.ItemActionSystem;
import ui.input.InputHandler;

import java.util.ArrayList;

public class EquipmentController {

    public Effect handle(InputHandler.Command c, GameState state, Renderer renderer, StickyMove move) {
        final int SLOTS = 7;

        switch (c) {
            case EQUIPMENT -> {
                state.equipmentOpen = false;
                state.eqActionsOpen = false;
                state.eqSelectOpen = false;
                renderer.log("Cierras el equipo.");
                return Effect.CHANGED;
            }
            case INVENTORY -> {
                state.equipmentOpen = false;
                state.eqActionsOpen = false;
                state.eqSelectOpen = false;

                state.inventoryOpen = true;
                state.invActionsOpen = false;

                move.reset();
                renderer.log("Abres el inventario.");
                return Effect.CHANGED;
            }
            case UP, LEFT -> {
                if (state.eqSelectOpen) {
                    if (state.eqSelectItems != null && !state.eqSelectItems.isEmpty()) {
                        state.eqSelectSel = Math.max(0, state.eqSelectSel - 1);
                        return Effect.CHANGED;
                    }
                } else if (state.eqActionsOpen) {
                    if (state.eqActions != null && !state.eqActions.isEmpty()) {
                        state.eqActionSel = Math.max(0, state.eqActionSel - 1);
                        return Effect.CHANGED;
                    }
                } else {
                    state.eqSel = (state.eqSel - 1 + SLOTS) % SLOTS;
                    return Effect.CHANGED;
                }
            }
            case DOWN, RIGHT -> {
                if (state.eqSelectOpen) {
                    if (state.eqSelectItems != null && !state.eqSelectItems.isEmpty()) {
                        state.eqSelectSel = Math.min(state.eqSelectItems.size() - 1, state.eqSelectSel + 1);
                        return Effect.CHANGED;
                    }
                } else if (state.eqActionsOpen) {
                    if (state.eqActions != null && !state.eqActions.isEmpty()) {
                        state.eqActionSel = Math.min(state.eqActions.size() - 1, state.eqActionSel + 1);
                        return Effect.CHANGED;
                    }
                } else {
                    state.eqSel = (state.eqSel + 1) % SLOTS;
                    return Effect.CHANGED;
                }
            }
            case ACTION -> {
                EquipmentSlot slot = slotByIndex(state.eqSel);

                if (state.eqSelectOpen) {
                    if (state.eqSelectItems != null && !state.eqSelectItems.isEmpty()) {
                        int idx = Math.max(0, Math.min(state.eqSelectSel, state.eqSelectItems.size() - 1));
                        Item choice = state.eqSelectItems.get(idx);
                        ItemActionSystem.equipToSlot(state, choice, slot, renderer);
                    }
                    state.eqSelectOpen = false;
                    state.eqActionsOpen = false;
                    return Effect.CHANGED;
                }

                if (state.eqActionsOpen) {
                    if (state.eqActionSel >= 0 && state.eqActionSel < state.eqActions.size()) {
                        String action = state.eqActions.get(state.eqActionSel).toLowerCase();
                        switch (action) {
                            case "equipar" -> {
                                state.eqSelectItems = ItemActionSystem.equippablesForSlot(state, slot);
                                state.eqSelectSel = 0;
                                state.eqSelectOpen = true;
                                return Effect.CHANGED;
                            }
                            case "desequipar" -> {
                                ItemActionSystem.unequipSlot(state, slot, renderer);
                                state.eqActionsOpen = false;
                                return Effect.CHANGED;
                            }
                            default -> {
                                state.eqActionsOpen = false;
                                return Effect.CHANGED;
                            }
                        }
                    } else {
                        state.eqActionsOpen = false;
                        return Effect.CHANGED;
                    }
                }

                Item cur = itemInSlot(state, slot);
                ArrayList<String> actions = new ArrayList<>();
                if (cur == null) actions.add("Equipar");
                else actions.add("Desequipar");
                actions.add("Cancelar");

                state.eqActions = actions;
                state.eqActionSel = 0;
                state.eqActionsOpen = true;
                renderer.log("Acciones sobre " + slotNameByIndex(state.eqSel) + ": " + String.join(", ", actions) + ".");
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

    private Item itemInSlot(GameState s, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> s.equipment.getHead();
            case TORSO -> s.equipment.getChest();
            case HANDS -> s.equipment.getHands();
            case LEGS -> s.equipment.getLegs();
            case FEET -> s.equipment.getFeet();
            case MAIN_HAND -> s.equipment.getMainHand();
            case OFF_HAND -> s.equipment.getOffHand();
            case BACKPACK -> s.equipment.getBackpack();
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
}
