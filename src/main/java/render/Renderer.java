package render;

import items.Equipment;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import ui.menu.*;
import ui.menu.player.EquipmentPanel;
import ui.menu.player.PlayerHud;
import ui.menu.player.PlayerStates;
import utils.ANSI;
import game.GameState;
import world.Entity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;

import static game.Constants.*;

public class Renderer {
    private PlayerHud hud;
    private PlayerStates states;
    private EquipmentPanel equip;
    private MapView mapView;
    private MessageLog msgLog;
    private ActionBar actionBar;
    private InspectView inspect;
    private final PCView pcOverlay = new PCView();
    private int inspectTop, inspectLeft, inspectW, inspectH;
    private final InventoryView invOverlay = new InventoryView();
    private final EquipmentView equipOverlay = new EquipmentView();
    private final StatsView statsOverlay = new StatsView();
    private Terminal term;
    private int lastCols = -1, lastRows = -1;
    private Integer pendingAnchorSX = null, pendingAnchorSY = null;
    private boolean wasWorldActionsOpen = false;
    private boolean wasOverlayOpen = false;
    private final HashMap<Long, Entity> overlay = new HashMap<>(256);
    private final ArrayList<String> inspectLines = new ArrayList<>(32);
    private String lastClockStr = "";
    private int lastClockSec = -1;
    private long lastSizeCheckNs = 0L;
    private static final long SIZE_CHECK_INTERVAL_NS = 200_000_000L;
    private final ArrayList<Entity> viewEntities = new ArrayList<>(256);
    private int lastViewCamX = Integer.MIN_VALUE, lastViewCamY = Integer.MIN_VALUE;
    private long lastViewBuildNs = 0L;
    private static final long VIEW_REBUILD_INTERVAL_NS = 120_000_000L;
    private int mapTop;

    public void init(GameState s, Terminal term) {
        this.term = term;

        ANSI.setEnabled(true);
        ANSI.useAltScreen(true);
        ANSI.setCursorVisible(false);
        ANSI.setWrap(false);

        hud = null;
        states = null;
        equip = null;
        inspect = new InspectView();
        msgLog = new MessageLog(1, 1, 40, 8);
        actionBar = new ActionBar(1, 1, 40);

        recomputeLayout(s);
        ANSI.clearScreenAndHome();
        renderAll(s);
    }

    public void onMapChanged(GameState s) {
        if (mapView == null) {
            recomputeLayout(s);
            return;
        }

        final int top = mapTop;
        final int left = mapView.getLeft();
        final int w = mapView.getViewW();
        final int h = mapView.getViewH();

        mapView = new MapView(top, left, w, h, 18, s.map, 2.0);
        mapView.setCenterSmallMaps(true);
        mapView.prefill();

        if (pendingAnchorSX != null && pendingAnchorSY != null) {
            int viewW = mapView.getViewW(), viewH = mapView.getViewH();
            boolean smallX = (s.map.w <= viewW);
            boolean smallY = (s.map.h <= viewH);

            int centerOx = smallX ? (viewW - s.map.w) / 2 : 0;
            int centerOy = smallY ? (viewH - s.map.h) / 2 : 0;

            int desiredSX = pendingAnchorSX;
            int desiredSY = pendingAnchorSY;

            int extraOx = smallX ? (desiredSX - (s.px + centerOx)) : 0;
            int extraOy = smallY ? (desiredSY - (s.py + centerOy)) : 0;

            int centerSX = mapView.getViewW() / 2;
            int centerSY = mapView.getViewH() / 2;
            int useSX = smallX ? desiredSX : centerSX;
            int useSY = smallY ? desiredSY : centerSY;

            mapView.anchorOnceKeepingPlayerAt(useSX, useSY, s.px, s.py, s.map);
            mapView.setExtraOffset(extraOx, extraOy);

            pendingAnchorSX = null;
            pendingAnchorSY = null;
        } else {
            mapView.setExtraOffset(0, 0);
        }
        resetViewCache();
    }


    public void renderAll(GameState s) {
        boolean justClosedWorldActions = wasWorldActionsOpen && !s.worldActionsOpen;
        wasWorldActionsOpen = s.worldActionsOpen;
        boolean overlayOpen = s.inventoryOpen || s.equipmentOpen || s.statsOpen || s.computerOpen;
        boolean justClosedOverlay = wasOverlayOpen && !overlayOpen;
        wasOverlayOpen = overlayOpen;

        if (mapView != null && (justClosedWorldActions || justClosedOverlay)) {
            mapView.requestFullRepaint();
        }

        LocalTime now = LocalTime.now();
        int sec = now.getSecond();
        if (sec != lastClockSec) {
            lastClockSec = sec;
            lastClockStr = now.format(TS_FMT);
        }
        String hora = lastClockStr;

        hud.renderHud(1, hora, "Soleado", s.temperaturaC, s.ubicacion, s.salud, s.maxSalud, s.energia, s.maxEnergia, s.hambre, s.maxHambre, s.sed, s.maxSed, s.sueno, s.maxSueno, s.px, s.py, rumboTexto(s.lastDx, s.lastDy));

        states.renderStates(s.salud, s.maxSalud, s.energia, s.maxEnergia, s.hambre, s.maxHambre, s.sed, s.maxSed, s.sueno, s.maxSueno, s.sangrado, s.infeccionPct, s.escondido);

        Equipment eq = s.equipment;
        String arma = eq.nombreOGuion(eq.getMainHand());
        String off = eq.nombreOGuion(eq.getOffHand());
        String cabeza = eq.nombreOGuion(eq.getHead());
        String pecho = eq.nombreOGuion(eq.getChest());
        String manos = eq.nombreOGuion(eq.getHands());
        String piernas = eq.nombreOGuion(eq.getLegs());
        String pies = eq.nombreOGuion(eq.getFeet());
        String mochila = eq.nombreOGuion(eq.getBackpack());
        double peso = eq.pesoTotalKg(s.inventory);
        double capacidad = eq.capacidadKg();

        equip.render(arma, off, cabeza, pecho, manos, piernas, pies, mochila, 0, 0, peso, capacidad);

        if (!s.inventoryOpen && !s.equipmentOpen && !s.statsOpen && !s.computerOpen) {
            overlay.clear();

            final int camX = cameraX(s), camY = cameraY(s);
            final int vw = mapView.getViewW(), vh = mapView.getViewH();
            final int xMax = camX + vw, yMax = camY + vh;

            rebuildViewEntitiesIfNeeded(s);

            for (world.Entity e : viewEntities) {
                if (e.x >= camX && e.x < xMax && e.y >= camY && e.y < yMax) {
                    long k = (((long) e.x) << 32) ^ (e.y & 0xffffffffL);
                    overlay.put(k, e);
                }
            }
            mapView.render(s.map, s.px, s.py, overlay);
        }

        if (s.inventoryOpen) {
            int top = mapTop;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH() + 2;
            invOverlay.render(top, left, w, h, s.inventory, s.invSel);
            if (s.invActionsOpen && s.invActions != null && !s.invActions.isEmpty()) {
                invOverlay.renderActionMenu(mapTop + 2, left, mapView.getViewW(), mapView.getViewH(), s.invActions, s.invActionSel);
            }
        }

        if (s.equipmentOpen) {
            int top = mapTop;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH() + 2;
            equipOverlay.render(top, left, w, h, s.equipment, s.inventory, s.eqSel);
            if (s.eqActionsOpen && s.eqActions != null && !s.eqActions.isEmpty()) {
                equipOverlay.renderActionMenu(mapTop + 2, left, mapView.getViewW(), mapView.getViewH(), s.eqActions, s.eqActionSel);
            }
            if (s.eqSelectOpen) {
                equipOverlay.renderSelectMenu(mapTop + 2, left, mapView.getViewW(), mapView.getViewH(), s.eqSelectItems, s.eqSelectSel, "EQUIPAR", s.eqSelectItems == null || s.eqSelectItems.isEmpty());
            }
        }

        if (s.worldActionsOpen && s.worldActions != null && !s.worldActions.isEmpty()) {
            int top = mapTop + 2;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH();
            invOverlay.renderActionMenu(top, left, w, h, s.worldActions, s.worldActionSel);
        }

        if (s.statsOpen) {
            int top = mapTop;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH() + 2;
            statsOverlay.render(top, left, w, h, s);
        }

        if (s.computerOpen) {
            int top = mapTop;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH() + 2;
            pcOverlay.render(top, left, w, h, s);
        }

        msgLog.render();
        renderInspectPanel(s);
        actionBar.render();

        ANSI.gotoRC(1, 1);
        ANSI.flush();
    }


    private void renderInspectPanel(GameState s) {
        if (inspect == null || inspectW < 18) return;
        inspectLines.clear();

        String title = "OBJETIVO";
        char glyph = ' ';
        String kind = "—";

        int tx, ty;
        world.Entity found = null;

        if (s.worldActionsOpen) {
            tx = s.worldTx;
            ty = s.worldTy;
            found = s.worldTarget;
        } else {
            world.Entity nearLoot = findNearbyLoot(s);
            int[] nearStair = (nearLoot == null) ? findNearbyStair(s) : null;

            if (nearLoot != null) {
                tx = nearLoot.x;
                ty = nearLoot.y;
                found = nearLoot;
            } else if (nearStair != null) {
                tx = nearStair[0];
                ty = nearStair[1];
                found = null;
            } else {
                int dx = s.lastDx, dy = s.lastDy;
                boolean hasDir = !(dx == 0 && dy == 0);
                tx = s.px + (hasDir ? dx : 0);
                ty = s.py + (hasDir ? dy : 0);

                if (tx < 0 || ty < 0 || tx >= s.map.w || ty >= s.map.h) {
                    inspectLines.add("Fuera del mapa.");
                    inspect.render(inspectTop, inspectLeft, inspectW, inspectH, title, glyph, kind, inspectLines);
                    return;
                }

                found = utils.EntityUtil.findTopEntityAt(s, tx, ty);
                if (found == null) {
                    world.Entity under = utils.EntityUtil.findTopEntityAt(s, s.px, s.py);
                    if (under != null) {
                        found = under;
                        tx = s.px;
                        ty = s.py;
                    }
                }
            }
        }

        // Bordes por seguridad
        if (tx < 0 || ty < 0 || tx >= s.map.w || ty >= s.map.h) {
            inspectLines.add("Fuera del mapa.");
            inspect.render(inspectTop, inspectLeft, inspectW, inspectH, title, glyph, kind, inspectLines);
            return;
        }

        boolean vis = wasVisibleLastRender(tx, ty);
        boolean det = wasDetectedLastRender(tx, ty);

        if (found != null) {
            glyph = found.glyph;
            kind = switch (found.type) {
                case ZOMBIE -> "Enemigo";
                case LOOT -> "Botín";
                default -> "Entidad";
            };

            // Nombre amigable; si es LOOT, usa el nombre del ítem
            String name = (found.type == world.Entity.Type.LOOT && found.item != null) ? found.item.getNombre() : utils.EntityUtil.entityName(found);

            title = "OBJETIVO: " + name;

            inspectLines.add(String.format("Pos: (%d,%d)", tx, ty));
            inspectLines.add("Tipo: " + kind);
            inspectLines.add("Visible: " + (vis ? "Sí" : det ? "Detectado" : "No"));
            inspectLines.add("Terreno: " + utils.EntityUtil.tileName(s.map.tiles[ty][tx], s.map.indoor[ty][tx]));
            inspectLines.add("Transitable: " + (s.map.walk[ty][tx] ? "Sí" : "No"));

            // Detalle extra cuando sea LOOT
            if (found.type == world.Entity.Type.LOOT && found.item != null) {
                var it = found.item;
                inspectLines.add("Objeto: " + it.getNombre());
                inspectLines.add(String.format("Peso: %.3f kg  Condición: %d%%", it.getPesoKg(), it.getDurabilidadPct()));
                if (it.getWeapon() != null) {
                    inspectLines.add(String.format("Arma • Daño:%d  Manos:%d  Cadencia:%.2fs", it.getWeapon().danho(), it.getWeapon().manos(), it.getWeapon().cooldownSec()));
                }
                if (it.getArmor() != null) {
                    inspectLines.add(String.format("Armadura • Prot:%d  Abrigo:%d", it.getArmor().proteccion(), it.getArmor().abrigo()));
                }
                if (it.getContainer() != null) {
                    inspectLines.add(String.format("Contenedor • Capacidad: %.2f kg", it.getContainer().capacidadKg()));
                }
                if (it.getWearableSlot() != null) {
                    inspectLines.add("Slot: " + it.getWearableSlot().name());
                }
                String desc = it.getDescripcion();
                if (desc != null && !desc.isBlank()) inspectLines.add(desc);
            }
        } else {
            // Terreno
            char t = s.map.tiles[ty][tx];
            glyph = t;
            kind = "Terreno";
            title = "OBJETIVO: " + utils.EntityUtil.tileName(t, s.map.indoor[ty][tx]);

            inspectLines.add(String.format("Pos: (%d,%d)", tx, ty));
            inspectLines.add("Visible: " + (vis ? "Sí" : det ? "Detectado" : "No"));
            inspectLines.add("Tipo: " + kind);
            inspectLines.add("Transitable: " + (s.map.walk[ty][tx] ? "Sí" : "No"));

            String extra = utils.EntityUtil.tileHint(t);
            if (!extra.isEmpty()) inspectLines.add(extra);
        }

        inspect.render(inspectTop, inspectLeft, inspectW, inspectH, title, glyph, kind, inspectLines);
    }

    private world.Entity findNearbyLoot(GameState s) {
        int px = s.px, py = s.py;
        int dx = s.lastDx, dy = s.lastDy;

        int[][] candidates = new int[][]{{px, py}, {px + dx, py + dy}, {px + 1, py}, {px - 1, py}, {px, py + 1}, {px, py - 1}, {px + 1, py + 1}, {px + 1, py - 1}, {px - 1, py + 1}, {px - 1, py - 1}};

        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        for (int[] c : candidates) {
            int x = c[0], y = c[1];
            if (x < 0 || y < 0 || x >= s.map.w || y >= s.map.h) continue;
            long key = (((long) x) << 32) ^ (y & 0xffffffffL);
            if (!seen.add(key)) continue;

            world.Entity e = utils.EntityUtil.findTopEntityAt(s, x, y);
            if (e != null && e.type == world.Entity.Type.LOOT) return e;
        }
        return null;
    }

    private int[] findNearbyStair(GameState s) {
        int px = s.px, py = s.py;
        int dx = s.lastDx, dy = s.lastDy;

        int[][] candidates = new int[][]{{px, py}, {px + dx, py + dy}, {px + 1, py}, {px - 1, py}, {px, py + 1}, {px, py - 1}, {px + 1, py + 1}, {px + 1, py - 1}, {px - 1, py + 1}, {px - 1, py - 1}};

        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        for (int[] c : candidates) {
            int x = c[0], y = c[1];
            if (x < 0 || y < 0 || x >= s.map.w || y >= s.map.h) continue;
            long key = (((long) x) << 32) ^ (y & 0xffffffffL);
            if (!seen.add(key)) continue;
            if (s.map.tiles[y][x] == 'S' && s.map.hasStairAt(x, y)) return new int[]{x, y};
        }
        return null;
    }


    private void recomputeLayout(GameState s) {
        int cols = 120, rows = 40;
        if (term != null) {
            Size sz = term.getSize();
            cols = Math.max(60, sz.getColumns());
            rows = Math.max(24, sz.getRows());
        }
        lastCols = cols;
        lastRows = rows;

        int headerWidth = Math.max(40, cols - HUD_LEFT);

        final int gap = GAP;
        final int minStats = 36;
        final int minStates = 24;
        final int minEquip = 18;
        final int statsMax = 56;

        int targetStates = Math.max(minStates, Math.min(40, headerWidth / 4));
        int sideMin = Math.max(minStats, minEquip);
        int wStatesMax = Math.max(minStates, headerWidth - 2 * gap - 2 * sideMin);
        int wStates = Math.min(targetStates, wStatesMax);
        int totalSide = headerWidth - 2 * gap - wStates;
        if (totalSide < 0) {
            wStates = Math.max(minStates, headerWidth - 2 * gap);
            totalSide = Math.max(0, headerWidth - 2 * gap - wStates);
        }

        int sideWidth = totalSide / 2;
        if (sideWidth < sideMin) {
            sideWidth = sideMin;
            wStates = Math.max(minStates, headerWidth - 2 * gap - 2 * sideWidth);
        }
        if (sideWidth > statsMax) {
            sideWidth = statsMax;
            wStates = Math.max(minStates, headerWidth - 2 * gap - 2 * sideWidth);
        }

        int wStats = sideWidth;
        int wEquip = sideWidth;

        int statsLeft = HUD_LEFT;
        int statesLeft = statsLeft + wStats + gap;
        int equipLeft = statesLeft + wStates + gap;

        hud = new PlayerHud(1, statsLeft, headerWidth, wStats);
        states = new PlayerStates(3, statesLeft, wStates);
        int equipRows = Math.min(EQUIP_ROWS, Math.max(6, MAP_TOP));
        equip = new EquipmentPanel(3, equipLeft, wEquip, equipRows);

        int topBlockBottom = 3 + equipRows - 1;
        mapTop = topBlockBottom + 1;

        final int mapFrame = 2;
        final int gapMapLog = 0;
        final int actionBarH = 3;
        final int minMapH = 8;
        final int minLogH = 5;
        int desiredLogH = Math.min(LOG_ROWS, Math.max(minLogH, rows / 6));
        desiredLogH += 1;
        int viewH = rows - (mapTop + mapFrame + gapMapLog + desiredLogH + actionBarH);
        if (viewH < minMapH) {
            desiredLogH = minLogH;
            viewH = rows - (mapTop + mapFrame + gapMapLog + desiredLogH + actionBarH);
            if (viewH < minMapH) viewH = minMapH;
        }

        int viewW = headerWidth;
        int mapViewH = viewH;
        mapView = new MapView(mapTop, MAP_LEFT, viewW, mapViewH, 18, s.map, 2.0);
        mapView.prefill();
        mapView.setCenterSmallMaps(true);

        int logTop = mapTop + mapFrame + viewH + gapMapLog;
        int inspectWMin = 24;
        int proposedInspect = Math.max(inspectWMin, headerWidth / 4);
        int logW = Math.max(32, headerWidth - proposedInspect - 1);
        int finalInspect = Math.max(inspectWMin, headerWidth - logW - 1);

        if (msgLog == null) {
            msgLog = new MessageLog(logTop, MAP_LEFT, logW, desiredLogH);
        } else {
            msgLog.updateGeometry(logTop, MAP_LEFT, logW, desiredLogH);
        }

        inspectTop = logTop;
        inspectLeft = MAP_LEFT + logW + 1;
        inspectW = finalInspect;
        inspectH = desiredLogH;

        int menuTop = logTop + desiredLogH;
        if (actionBar == null) actionBar = new ActionBar(menuTop, MAP_LEFT, headerWidth);
        else actionBar.updateGeometry(menuTop, MAP_LEFT, headerWidth);

        ANSI.setScrollRegion(mapTop + 2, mapTop + 2 + viewH - 1);
        resetViewCache();
    }

    public boolean wasVisibleLastRender(int x, int y) {
        return mapView.wasVisibleLastRender(x, y);
    }

    public boolean wasDetectedLastRender(int x, int y) {
        return mapView.wasDetectedLastRender(x, y);
    }

    public boolean isNearCamera(int x, int y, GameState s) {
        int camX = cameraX(s), camY = cameraY(s);
        return x >= camX && x < camX + mapView.getViewW() && y >= camY && y < camY + mapView.getViewH();
    }

    public int cameraX(GameState s) {
        return Math.max(0, Math.min(s.px - mapView.getViewW() / 2, s.map.w - mapView.getViewW()));
    }

    public int cameraY(GameState s) {
        return Math.max(0, Math.min(s.py - mapView.getViewH() / 2, s.map.h - mapView.getViewH()));
    }

    public void log(String m) {
        msgLog.add(m);
    }

    public boolean ensureLayoutUpToDate(GameState s) {
        if (term == null) return false;

        long now = System.nanoTime();
        if (now - lastSizeCheckNs < SIZE_CHECK_INTERVAL_NS) {
            return false;
        }
        lastSizeCheckNs = now;

        org.jline.terminal.Size sz = term.getSize();
        int cols = Math.max(1, sz.getColumns());
        int rows = Math.max(1, sz.getRows());
        if (cols != lastCols || rows != lastRows) {
            ANSI.resetScrollRegion();
            ANSI.clearScreenAndHome();
            recomputeLayout(s);
            return true;
        }
        return false;
    }

    public void shutdown() {
        ANSI.resetScrollRegion();
        ANSI.setWrap(true);
        ANSI.setCursorVisible(true);
        ANSI.useAltScreen(false);
    }

    private static String rumboTexto(int dx, int dy) {
        if (dx == 0 && dy == 0) return "-";
        if (dy < 0 && dx == 0) return "NORTE";
        if (dy < 0 && dx > 0) return "NE";
        if (dy == 0 && dx > 0) return "ESTE";
        if (dy > 0 && dx > 0) return "SE";
        if (dy > 0 && dx == 0) return "SUR";
        if (dy > 0) return "SO";
        if (dy == 0) return "OESTE";
        return "NO";
    }

    public void requestAnchorAtPlayer(game.GameState s) {
        int camX = cameraX(s);
        int camY = cameraY(s);

        int sx = s.px - camX;
        int sy = s.py - camY;

        int ox = 0, oy = 0;
        if (mapView != null) {
            ox = Math.max(0, (mapView.getViewW() - s.map.w) / 2);
            oy = Math.max(0, (mapView.getViewH() - s.map.h) / 2);
        }

        pendingAnchorSX = sx + ox;
        pendingAnchorSY = sy + oy;
    }

    private void rebuildViewEntitiesIfNeeded(GameState s) {
        final int camX = cameraX(s), camY = cameraY(s);
        final long now = System.nanoTime();

        final boolean camChanged = (camX != lastViewCamX) || (camY != lastViewCamY);
        final boolean timeElapsed = (now - lastViewBuildNs) >= VIEW_REBUILD_INTERVAL_NS;

        if (camChanged || timeElapsed || viewEntities.isEmpty()) {
            viewEntities.clear();

            final int xMax = camX + mapView.getViewW();
            final int yMax = camY + mapView.getViewH();

            // Escaneo completo SOLO cuando toca (cámara nueva o timeout)
            for (world.Entity e : s.entities) {
                if (e.x >= camX && e.x < xMax && e.y >= camY && e.y < yMax) {
                    viewEntities.add(e);
                }
            }
            lastViewCamX = camX;
            lastViewCamY = camY;
            lastViewBuildNs = now;
        }
    }

    private void resetViewCache() {
        viewEntities.clear();
        lastViewCamX = Integer.MIN_VALUE;
        lastViewCamY = Integer.MIN_VALUE;
        lastViewBuildNs = 0L;
    }
}
