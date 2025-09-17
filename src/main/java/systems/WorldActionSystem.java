package systems;

import game.GameState;
import items.Equipment;
import items.Item;
import render.Renderer;
import world.Entity;

import java.util.ArrayList;
import java.util.List;

import static utils.EntityUtil.findTopEntityAt;


public final class WorldActionSystem {
    private WorldActionSystem() {
    }

    public static void openContextActions(GameState s, Renderer r) {
        int dx = s.lastDx, dy = s.lastDy;
        boolean hasDir = !(dx == 0 && dy == 0);
        int tx = s.px + (hasDir ? dx : 0);
        int ty = s.py + (hasDir ? dy : 0);

        if (tx < 0 || ty < 0 || tx >= s.map.w || ty >= s.map.h) {
            r.log("No hay acciones fuera del mapa.");
            s.worldActionsOpen = false;
            return;
        }

        Entity targetEnt = findTopEntityAt(s, tx, ty);

        if (!hasDir && targetEnt == null) {
            Entity under = findTopEntityAt(s, s.px, s.py);
            if (under != null) {
                targetEnt = under;
                tx = s.px;
                ty = s.py;
            }
        }

        s.worldTx = tx;
        s.worldTy = ty;
        s.worldTarget = targetEnt;

        s.worldActions = buildActionsList(s, tx, ty, targetEnt);
        s.worldActionSel = 0;
        s.worldActionsOpen = !s.worldActions.isEmpty();

        if (s.worldActionsOpen) {
            r.log("Acciones disponibles: " + String.join(", ", s.worldActions) + ".");
        } else {
            r.log("No hay acciones disponibles aquí.");
        }
    }

    public static boolean executeSelected(GameState s, Renderer r) {
        if (!s.worldActionsOpen || s.worldActions == null || s.worldActions.isEmpty()) return false;
        int idx = Math.max(0, Math.min(s.worldActionSel, s.worldActions.size() - 1));
        String action = s.worldActions.get(idx);
        boolean changed = applyAction(s, action, r);

        s.worldActionsOpen = false;
        return changed;
    }

    public static boolean handleArrow(GameState s, boolean up) {
        if (!s.worldActionsOpen || s.worldActions == null || s.worldActions.isEmpty()) return false;
        int n = s.worldActions.size();
        if (n <= 0) return false;
        if (up) s.worldActionSel = Math.max(0, s.worldActionSel - 1);
        else s.worldActionSel = Math.min(n - 1, s.worldActionSel + 1);
        return true;
    }

    private static List<String> buildActionsList(GameState s, int tx, int ty, Entity ent) {
        ArrayList<String> out = new ArrayList<>();

        if (ent != null) {
            if (ent.type == Entity.Type.ZOMBIE) {
                out.add("Atacar");
            } else if (ent.type == Entity.Type.LOOT) {
                out.add("Lootear");
            } else {
                // todo --> gestionar más opciones de acción
            }
        } else {
            char t = s.map.tiles[ty][tx];
            switch (t) {
                case '.' -> out.add("Cavar");
                case '#' -> {
                    boolean onSameTree = (s.px == tx && s.py == ty) && s.escondido;
                    if (onSameTree) out.add("Salir");
                    else out.add("Esconderse");
                    out.add("Rebuscar");
                }
                default -> {
                }
            }
        }

        out.add("Cancelar");
        return out;
    }

    private static boolean applyAction(GameState s, String action, Renderer r) {
        if (action == null) return false;
        action = action.trim().toLowerCase();

        // Usamos snapshot de destino al abrir el menú
        int tx = s.worldTx;
        int ty = s.worldTy;
        Entity ent = s.worldTarget;
        char tile = s.map.tiles[ty][tx];

        switch (action) {
            case "cancelar" -> {
                return false;
            }
            case "lootear" -> {
                if (ent == null || ent.type != Entity.Type.LOOT || ent.item == null) {
                    r.log("No hay nada que lootear aquí.");
                    return false;
                }
                s.inventory.add(ent.item);
                s.entities.remove(ent);
                r.log("Recojes: " + ent.item.getNombre() + " (guardado en la mochila).");
                return true;
            }
            case "cavar" -> {
                r.log("Cavas el suelo con tus manos. (WIP)");
                return false;
            }
            case "rebuscar" -> {
                r.log("Rebuscas entre las ramas... no encuentras nada útil. (WIP)");
                return false;
            }
            case "esconderse" -> {
                if (tile != '#') {
                    r.log("No hay suficiente cobertura aquí.");
                    return false;
                }
                s.hidePrevX = s.px;
                s.hidePrevY = s.py;
                s.px = tx;
                s.py = ty;
                s.escondido = true;
                r.log("Te escondes entre el follaje.");
                return true;
            }
            case "salir" -> {
                if (!s.escondido) {
                    r.log("No estabas escondido.");
                    return false;
                }
                int ex = s.hidePrevX, ey = s.hidePrevY;
                boolean moved = false;
                if (ex >= 0 && ey >= 0 && ex < s.map.w && ey < s.map.h && s.map.walk[ey][ex]) {
                    s.px = ex;
                    s.py = ey;
                    moved = true;
                } else {
                    // Buscamos casilla transitable adyacente
                    int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                    for (int[] d : dirs) {
                        int nx = s.px + d[0], ny = s.py + d[1];
                        if (nx >= 0 && ny >= 0 && nx < s.map.w && ny < s.map.h && s.map.walk[ny][nx]) {
                            s.px = nx;
                            s.py = ny;
                            moved = true;
                            break;
                        }
                    }
                }
                s.escondido = false;
                s.hidePrevX = -1;
                s.hidePrevY = -1;
                if (moved) r.log("Sales de tu escondite.");
                else r.log("Intentas salir, pero alrededor no hay espacio libre.");
                return moved;
            }
            case "atacar" -> {
                return systems.CombatSystem.quickAttack(s, r);
            }
            default -> {
                r.log("Acción no implementada: " + action);
                return false;
            }
        }
    }

    private static String armaActualTexto(Equipment eq) {
        Item main = eq.getMainHand();
        Item off = eq.getOffHand();
        if (main != null) return main.getNombre();
        if (off != null) return off.getNombre();
        return "los puños";
    }
}
