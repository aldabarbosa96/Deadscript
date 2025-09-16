package render;

import items.Equipment;
import ui.menu.*;
import ui.player.EquipmentPanel;
import ui.player.PlayerHud;
import ui.player.PlayerStates;
import utils.ANSI;
import game.GameState;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static game.Constants.*;
import static utils.EntityUtil.*;

public class Renderer {
    private PlayerHud hud;
    private PlayerStates states;
    private EquipmentPanel equip;
    private MapView mapView;
    private MessageLog msgLog;
    private ActionBar actionBar;
    private InspectView inspect;
    private int inspectTop, inspectLeft, inspectW, inspectH;
    private final InventoryView invOverlay = new InventoryView();
    private final EquipmentView equipOverlay = new EquipmentView();

    public void init(GameState s) {
        ANSI.setEnabled(true);
        ANSI.useAltScreen(true);
        ANSI.setCursorVisible(false);
        ANSI.setWrap(false);

        int equipWidth = Math.max(18, Math.min(VIEW_W, 140) - EQUIP_LEFT);
        int headerWidth = (EQUIP_LEFT + equipWidth) - HUD_LEFT;

        hud = new PlayerHud(1, HUD_LEFT, headerWidth, STATS_WIDTH);
        states = new PlayerStates(3, STATES_LEFT, STATES_WIDTH);
        equip = new EquipmentPanel(3, EQUIP_LEFT, equipWidth, EQUIP_ROWS);

        int viewW = Math.min(headerWidth, s.map.w);
        int viewH = Math.min(VIEW_H, s.map.h);
        mapView = new MapView(MAP_TOP, MAP_LEFT, viewW, viewH, 18, s.map, 2.0);

        int logTop = MAP_TOP + 2 + viewH + 1;
        int inspectWMin = 24;
        int proposedInspect = Math.max(inspectWMin, headerWidth / 4);
        int logW = Math.max(32, headerWidth - proposedInspect - 1);
        int finalInspect = Math.max(inspectWMin, headerWidth - logW - 1);

        msgLog = new MessageLog(logTop, MAP_LEFT, logW, LOG_ROWS);
        msgLog.add(String.format("Día %d: %s, %d°C, Zona: %s", 1, "Soleado", s.temperaturaC, s.ubicacion + " (Bosque)"));
        msgLog.add(String.format("Posición inicial: (%d,%d).", s.px, s.py));

        inspect = new InspectView();
        inspectTop = logTop;
        inspectLeft = MAP_LEFT + logW + 1;
        inspectW = finalInspect;
        inspectH = LOG_ROWS;

        int menuTop = logTop + LOG_ROWS + 1;
        actionBar = new ActionBar(menuTop, MAP_LEFT, headerWidth);

        ANSI.clearScreenAndHome();
        ANSI.setScrollRegion(MAP_TOP + 2, MAP_TOP + 2 + viewH - 1);
        mapView.prefill();

        renderAll(s);
    }

    public void onMapChanged(GameState s) {
        int equipWidth = Math.max(18, Math.min(VIEW_W, 140) - EQUIP_LEFT);
        int headerWidth = (EQUIP_LEFT + equipWidth) - HUD_LEFT;

        int viewW = Math.min(headerWidth, s.map.w);
        int viewH = Math.min(VIEW_H, s.map.h);
        mapView = new MapView(MAP_TOP, MAP_LEFT, viewW, viewH, 18, s.map, 2.0);
        mapView.prefill();

        int logTop = MAP_TOP + 2 + viewH + 1;
        int inspectWMin = 24;
        int proposedInspect = Math.max(inspectWMin, headerWidth / 4);
        int logW = Math.max(32, headerWidth - proposedInspect - 1);
        int finalInspect = Math.max(inspectWMin, headerWidth - logW - 1);

        msgLog.updateGeometry(logTop, MAP_LEFT, logW, LOG_ROWS);

        inspectTop = logTop;
        inspectLeft = MAP_LEFT + logW + 1;
        inspectW = finalInspect;
        inspectH = LOG_ROWS;

        int menuTop = logTop + LOG_ROWS + 1;
        actionBar.updateGeometry(menuTop, MAP_LEFT, headerWidth);

        ANSI.setScrollRegion(MAP_TOP + 2, MAP_TOP + 2 + viewH - 1);
    }

    public void renderAll(GameState s) {
        String hora = LocalTime.now().format(TS_FMT);
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

        if (!s.inventoryOpen && !s.equipmentOpen) {
            mapView.render(s.map, s.px, s.py);
            renderEntities(s);
        }

        if (s.inventoryOpen) {
            int top = MAP_TOP + 2;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH();
            invOverlay.render(top, left, w, h, s.inventory, s.invSel);
            if (s.invActionsOpen && s.invActions != null && !s.invActions.isEmpty()) {
                invOverlay.renderActionMenu(top, left, w, h, s.invActions, s.invActionSel);
            }
        }

        if (s.equipmentOpen) {
            int top = MAP_TOP + 2;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH();
            equipOverlay.render(top, left, w, h, s.equipment, s.inventory, s.eqSel);
            if (s.eqActionsOpen && s.eqActions != null && !s.eqActions.isEmpty()) {
                equipOverlay.renderActionMenu(top, left, w, h, s.eqActions, s.eqActionSel);
            }
            if (s.eqSelectOpen) {
                equipOverlay.renderSelectMenu(top, left, w, h, s.eqSelectItems, s.eqSelectSel, "EQUIPAR", s.eqSelectItems == null || s.eqSelectItems.isEmpty());
            }
        }

        if (s.worldActionsOpen && s.worldActions != null && !s.worldActions.isEmpty()) {
            int top = MAP_TOP + 2;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH();
            invOverlay.renderActionMenu(top, left, w, h, s.worldActions, s.worldActionSel);
        }

        msgLog.render();
        renderInspectPanel(s);
        actionBar.render();

        ANSI.gotoRC(1, 1);
        ANSI.flush();
    }

    private void renderEntities(GameState s) {
        int camX = cameraX(s), camY = cameraY(s);
        int baseTop = MAP_TOP + 2;
        for (world.Entity e : s.entities) {
            int sx = e.x - camX, sy = e.y - camY;
            if (sx < 0 || sy < 0 || sx >= mapView.getViewW() || sy >= mapView.getViewH()) continue;

            boolean vis = mapView.wasVisibleLastRender(e.x, e.y);
            boolean det = mapView.wasDetectedLastRender(e.x, e.y);
            if (!vis && !det) continue;

            ANSI.gotoRC(baseTop + sy, mapView.getLeft() + sx);
            if (vis) {
                e.revealed = true;
                ANSI.setFg(31);
                System.out.print(e.glyph);
            } else {
                if (e.revealed) {
                    ANSI.setFg(90);
                    System.out.print(e.glyph);
                } else {
                    ANSI.setFg(90);
                    System.out.print('?');
                }
            }
            ANSI.resetStyle();
        }
    }

    private void renderInspectPanel(GameState s) {
        if (inspect == null || inspectW < 18) return;

        int dx = s.lastDx, dy = s.lastDy;

        String title = "OBJETIVO";
        char glyph = ' ';
        String kind = "—";
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();

        // 1) Casilla objetivo = contigua según última dirección.
        //    Si no hay dirección, usamos la casilla actual SOLO para mostrar entidades bajo los pies.
        boolean hasDir = !(dx == 0 && dy == 0);
        int tx = s.px + (hasDir ? dx : 0);
        int ty = s.py + (hasDir ? dy : 0);

        if (tx < 0 || ty < 0 || tx >= s.map.w || ty >= s.map.h) {
            lines.add("Fuera del mapa.");
            inspect.render(inspectTop, inspectLeft, inspectW, inspectH, title, glyph, kind, lines);
            return;
        }

        // 2) Entidad en la casilla objetivo (prevalece sobre el terreno)
        world.Entity found = findTopEntityAt(s, tx, ty);

        // 3) Si NO hay dirección y NO hay entidad en objetivo
        if (found == null) {
            world.Entity under = findTopEntityAt(s, s.px, s.py);
            if (under != null) {
                found = under;
                tx = s.px;
                ty = s.py;
            }
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
            String name = entityName(found);
            title = "OBJETIVO: " + name;

            lines.add(String.format("Pos: (%d,%d)", tx, ty));
            lines.add("Tipo: " + kind);
            lines.add("Visible: " + (vis ? "Sí" : det ? "Detectado" : "No"));
            lines.add("Terreno: " + tileName(s.map.tiles[ty][tx], s.map.indoor[ty][tx]));
            lines.add("Transitable: " + (s.map.walk[ty][tx] ? "Sí" : "No"));
        } else {
            char t = s.map.tiles[ty][tx];
            glyph = t;
            kind = "Terreno";
            title = "OBJETIVO: " + tileName(t, s.map.indoor[ty][tx]);

            lines.add(String.format("Pos: (%d,%d)", tx, ty));
            lines.add("Visible: " + (vis ? "Sí" : det ? "Detectado" : "No"));
            lines.add("Tipo: " + kind);
            lines.add("Transitable: " + (s.map.walk[ty][tx] ? "Sí" : "No"));

            String extra = tileHint(t);
            if (!extra.isEmpty()) lines.add(extra);
        }

        inspect.render(inspectTop, inspectLeft, inspectW, inspectH, title, glyph, kind, lines);
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
}
