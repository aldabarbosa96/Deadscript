package systems;

import game.GameState;
import render.Renderer;
import utils.AudioManager;
import world.Entity;
import world.GameMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static game.Constants.UPPER_OVERLAY_ACTIVE;
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

        // --- NUEVO: no sobrescribir si estamos mirando a una escalera
        char baseTile = s.map.tiles[ty][tx];
        boolean baseIsStair = (baseTile == 'S' && s.map.hasStairAt(tx, ty));

        // Auto-selección de loot cercano sólo si no estamos sobre/escalera delante
        if (!baseIsStair && (targetEnt == null || targetEnt.type != Entity.Type.LOOT)) {
            Entity nearLoot = pickLootNearPlayer(s);
            if (nearLoot != null) {
                targetEnt = nearLoot;
                tx = nearLoot.x;
                ty = nearLoot.y;
            } else {
                // --- NUEVO: imán a ESCALERA cercana (8-neighborhood)
                int[] stair = pickStairNearPlayer(s);
                if (stair != null) {
                    tx = stair[0];
                    ty = stair[1];
                }
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
                case 'S' -> {
                    world.GameMap.Stair st = s.map.getStairAt(tx, ty);
                    if (st != null) {
                        if (UPPER_OVERLAY_ACTIVE) {
                            out.add("Bajar");
                        } else {
                            if (st.up != null) out.add("Subir");
                            if (st.down != null) out.add("Bajar");
                        }
                    }
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
                AudioManager.playUi("/audio/backpackZip1.wav");
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
            case "subir" -> {
                world.GameMap.Stair st = s.map.getStairAt(tx, ty);
                if (st == null || st.up == null) {
                    r.log("Estas escaleras no llevan a ninguna planta superior.");
                    return false;
                }

                if (isGroundLike(s.map) && !UPPER_OVERLAY_ACTIVE) {
                    // Planta 0 → planta 1 con overlay (mantener mundo visible)
                    r.requestAnchorAtPlayer(s);
                    activateUpperOverlay(s, r, st.up.map, tx, ty, st.up.x, st.up.y);
                    return true;
                } else {
                    // No estamos en exterior: usa el comportamiento clásico (cambio de mapa)
                    return goThroughStairs(s, r, st.up, /*goingUp=*/true);
                }
            }

            case "bajar" -> {
                // Si estamos en overlay de planta superior, “Bajar” revierte overlay
                if (UPPER_OVERLAY_ACTIVE) {
                    r.requestAnchorAtPlayer(s);
                    deactivateUpperOverlay(s, r);
                    return true;
                }

                world.GameMap.Stair st = s.map.getStairAt(tx, ty);
                if (st == null || st.down == null) {
                    r.log("Estas escaleras no llevan a ningún sótano.");
                    return false;
                }
                // Sótano: mantener comportamiento actual (mapa separado)
                return goThroughStairs(s, r, st.down, /*goingUp=*/false);
            }

            default -> {
                r.log("Acción no implementada: " + action);
                return false;
            }
        }
    }

    public static int[] pickStairNearPlayer(GameState s) {
        int px = s.px, py = s.py;
        int dx = s.lastDx, dy = s.lastDy;

        int[][] candidates = new int[][]{{px, py}, {px + dx, py + dy}, {px + 1, py}, {px - 1, py}, {px, py + 1}, {px, py - 1}, {px + 1, py + 1}, {px + 1, py - 1}, {px - 1, py + 1}, {px - 1, py - 1}};

        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        for (int[] c : candidates) {
            int x = c[0], y = c[1];
            if (x < 0 || y < 0 || x >= s.map.w || y >= s.map.h) continue;
            long key = (((long) x) << 32) ^ (y & 0xffffffffL);
            if (!seen.add(key)) continue;

            if (s.map.tiles[y][x] == 'S' && s.map.hasStairAt(x, y)) {
                return new int[]{x, y};
            }
        }
        return null;
    }


    private static boolean goThroughStairs(GameState s, Renderer r, GameMap.Stair.Link link, boolean goingUp) {
        if (link == null || link.map == null) {
            r.log("La escalera no está conectada a ninguna planta.");
            return false;
        }

        // 1) Captura la celda de pantalla del jugador ANTES del swap
        r.requestAnchorAtPlayer(s);

        // 2) Cambia de mapa y recoloca al jugador
        s.map = link.map;
        s.px = link.x;
        s.py = link.y;

        if (s.entities != null) s.entities.clear();
        s.lastDx = 0;
        s.lastDy = 0;

        // 3) Reconstruye layout y aplica anclaje en onMapChanged()
        r.onMapChanged(s);

        r.log(goingUp ? "Subes por las escaleras." : "Bajas por las escaleras.");
        try {
            AudioManager.playUi("/audio/footstepsStairs.wav");
        } catch (Throwable ignored) {
        }
        return true;
    }


    public static Entity pickLootNearPlayer(GameState s) {
        int px = s.px, py = s.py;
        int dx = s.lastDx, dy = s.lastDy;
        int[][] candidates = new int[][]{{px, py}, {px + dx, py + dy}, {px + 1, py}, {px - 1, py}, {px, py + 1}, {px, py - 1}, {px + 1, py + 1}, {px + 1, py - 1}, {px - 1, py + 1}, {px - 1, py - 1}};

        HashSet<Long> seen = new HashSet<>();
        for (int[] c : candidates) {
            int x = c[0], y = c[1];
            if (x < 0 || y < 0 || x >= s.map.w || y >= s.map.h) continue;
            long key = (((long) x) << 32) ^ (y & 0xffffffffL);
            if (!seen.add(key)) continue; // evita duplicados si (dx,dy) era (0,0)

            Entity e = findTopEntityAt(s, x, y);
            if (e != null && e.type == Entity.Type.LOOT) return e;
        }
        return null;
    }

    private static final class OverlaySnapshot {
        int x0, y0, w, h;
        char[][] tiles;
        boolean[][] walk, transp, indoor;
    }

    private static OverlaySnapshot SNAP = null;
    private static int OVER_OFF_X = 0, OVER_OFF_Y = 0;

    private static boolean isGroundLike(world.GameMap m) {
        for (int y = 0; y < m.h; y++) {
            for (int x = 0; x < m.w; x++) {
                char t = m.tiles[y][x];
                if (t == '#' || t == '~') return true;
            }
        }
        return false;
    }

    private static void activateUpperOverlay(game.GameState s, render.Renderer r, world.GameMap up, int stairGX, int stairGY, int upX, int upY) {
        int offX = stairGX - upX;
        int offY = stairGY - upY;

        int x0 = Math.max(0, offX);
        int y0 = Math.max(0, offY);
        int x1 = Math.min(s.map.w - 1, offX + up.w - 1);
        int y1 = Math.min(s.map.h - 1, offY + up.h - 1);
        if (x1 < x0 || y1 < y0) return;

        OverlaySnapshot snap = new OverlaySnapshot();
        snap.x0 = x0;
        snap.y0 = y0;
        snap.w = x1 - x0 + 1;
        snap.h = y1 - y0 + 1;
        snap.tiles = new char[snap.h][snap.w];
        snap.walk = new boolean[snap.h][snap.w];
        snap.transp = new boolean[snap.h][snap.w];
        snap.indoor = new boolean[snap.h][snap.w];

        for (int gy = y0; gy <= y1; gy++) {
            for (int gx = x0; gx <= x1; gx++) {
                int ly = gy - y0, lx = gx - x0;
                snap.tiles[ly][lx] = s.map.tiles[gy][gx];
                snap.walk[ly][lx] = s.map.walk[gy][gx];
                snap.transp[ly][lx] = s.map.transp[gy][gx];
                snap.indoor[ly][lx] = s.map.indoor[gy][gx];

                int ux = gx - offX, uy = gy - offY;
                char t = up.tiles[uy][ux];
                s.map.tiles[gy][gx] = t;
                s.map.walk[gy][gx] = up.walk[uy][ux];
                s.map.transp[gy][gx] = up.transp[uy][ux];
                s.map.indoor[gy][gx] = up.indoor[uy][ux];
            }
        }

        SNAP = snap;
        UPPER_OVERLAY_ACTIVE = true;
        OVER_OFF_X = offX;
        OVER_OFF_Y = offY;

        if (s.entities != null) s.entities.clear();
        r.log("Subes a la planta superior.");
    }

    private static void deactivateUpperOverlay(game.GameState s, render.Renderer r) {
        if (!UPPER_OVERLAY_ACTIVE || SNAP == null) return;
        int x0 = SNAP.x0, y0 = SNAP.y0;

        for (int gy = y0; gy < y0 + SNAP.h; gy++) {
            for (int gx = x0; gx < x0 + SNAP.w; gx++) {
                int ly = gy - y0, lx = gx - x0;
                s.map.tiles[gy][gx] = SNAP.tiles[ly][lx];
                s.map.walk[gy][gx] = SNAP.walk[ly][lx];
                s.map.transp[gy][gx] = SNAP.transp[ly][lx];
                s.map.indoor[gy][gx] = SNAP.indoor[ly][lx];
            }
        }

        SNAP = null;
        UPPER_OVERLAY_ACTIVE = false;
        r.log("Bajas a la planta 0.");
    }
}
