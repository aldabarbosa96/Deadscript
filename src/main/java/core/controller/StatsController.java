package core.controller;

import game.GameState;
import render.Renderer;
import ui.input.InputHandler;
import utils.AudioManager;

public class StatsController {
    public Effect handle(InputHandler.Command c, GameState s, Renderer r) {
        switch (c) {
            case STATS -> {
                s.statsOpen = false;
                r.log("Cierras el panel de estadísticas.");
                return Effect.CHANGED;
            }
            case INVENTORY -> {
                s.statsOpen = false;
                s.inventoryOpen = true;
                r.log("Abres el inventario.");
                AudioManager.playUi("/audio/backpackZip1.wav");
                return Effect.CHANGED;
            }
            case EQUIPMENT -> {
                s.statsOpen = false;
                s.equipmentOpen = true;
                r.log("Abres el equipo.");
                AudioManager.playUi("/audio/equipment.wav");
                return Effect.CHANGED;
            }

            case LEFT -> {
                s.statsCol = Math.max(0, s.statsCol - 1);
                return Effect.CHANGED;
            }
            case RIGHT -> {
                s.statsCol = Math.min(2, s.statsCol + 1);
                return Effect.CHANGED;
            }

            case UP -> {
                switch (s.statsCol) {
                    case 0 -> s.statsSelSkill = Math.max(0, s.statsSelSkill - 1);
                    case 1 ->
                            s.statsBodySel = Math.floorMod(s.statsBodySel - 1, GameState.BodyPart.values().length); // CENTRO: cuerpo
                    case 2 -> s.statsSelBasic = Math.max(0, s.statsSelBasic - 1);
                }
                return Effect.CHANGED;
            }
            case DOWN -> {
                switch (s.statsCol) {
                    case 0 -> s.statsSelSkill = Math.min(skillSelectableCount(s) - 1, s.statsSelSkill + 1);
                    case 1 -> s.statsBodySel = Math.floorMod(s.statsBodySel + 1, GameState.BodyPart.values().length);
                    case 2 -> s.statsSelBasic = Math.min(basicSelectableCount(s) - 1, s.statsSelBasic + 1);
                }
                return Effect.CHANGED;
            }

            case QUIT -> {
                return Effect.QUIT;
            }
            default -> {
                return Effect.NONE;
            }
        }
    }

    private int basicSelectableCount(GameState s) {
        return 5 + 8;
    }

    private int skillSelectableCount(GameState s) {
        int n = 0;
        for (var g : GameState.SkillGroup.values()) {
            var list = s.skills.get(g);
            if (list != null) n += list.size();
        }
        return Math.max(0, n);
    }
}
