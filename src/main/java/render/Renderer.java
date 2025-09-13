package render;

import ui.player.EquipmentPanel;
import ui.menu.MapView;
import ui.player.PlayerHud;
import ui.player.PlayerStates;
import ui.menu.MessageLog;
import ui.menu.ActionBar;
import utils.ANSI;
import game.GameState;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static game.Constants.*;

public class Renderer {
    private PlayerHud hud;
    private PlayerStates states;
    private EquipmentPanel equip;
    private MapView mapView;
    private MessageLog msgLog;
    private ActionBar actionBar;

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
        msgLog = new MessageLog(logTop, MAP_LEFT, headerWidth, LOG_ROWS);
        msgLog.add(String.format("Día %d: %s, %d°C, Zona: %s", 1, "Soleado", s.temperaturaC, s.ubicacion + " (Bosque)"));
        msgLog.add(String.format("Posición inicial: (%d,%d).", s.px, s.py));

        int menuTop = logTop + LOG_ROWS + 1;
        actionBar = new ActionBar(menuTop, MAP_LEFT, headerWidth);

        ANSI.clearScreenAndHome();
        ANSI.setScrollRegion(MAP_TOP + 2, MAP_TOP + 2 + viewH - 1);
        mapView.prefill();

        renderAll(s); // primer frame
    }

    public void onMapChanged(GameState s) {
        int equipWidth = Math.max(18, Math.min(VIEW_W, 140) - EQUIP_LEFT);
        int headerWidth = (EQUIP_LEFT + equipWidth) - HUD_LEFT;

        int viewW = Math.min(headerWidth, s.map.w);
        int viewH = Math.min(VIEW_H, s.map.h);
        mapView = new MapView(MAP_TOP, MAP_LEFT, viewW, viewH, 18, s.map, 2.0);
        mapView.prefill();

        int logTop = MAP_TOP + 2 + viewH + 1;
        msgLog.updateGeometry(logTop, MAP_LEFT, headerWidth, LOG_ROWS);

        int menuTop = logTop + LOG_ROWS + 1;
        actionBar.updateGeometry(menuTop, MAP_LEFT, headerWidth);

        ANSI.setScrollRegion(MAP_TOP + 2, MAP_TOP + 2 + viewH - 1);
    }

    public void renderAll(GameState s) {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        hud.renderHud(1, hora, "Soleado", s.temperaturaC, s.ubicacion, s.salud, s.maxSalud, s.energia, s.maxEnergia, s.hambre, s.maxHambre, s.sed, s.maxSed, s.sueno, s.maxSueno, s.px, s.py, rumboTexto(s.lastDx, s.lastDy));
        states.renderStates(s.salud, s.maxSalud, s.energia, s.maxEnergia, s.hambre, s.maxHambre, s.sed, s.maxSed, s.sueno, s.maxSueno, s.sangrado, s.infeccionPct, s.escondido);
        equip.render("Navaja", "-", "Gorra", "-", "-", "-", "-", "Mochila tela", 0, 0, 5, 20.0);
        mapView.render(s.map, s.px, s.py);
        renderEntities(s);
        msgLog.render();
        actionBar.render();

        ANSI.gotoRC(1, 1);
        ANSI.flush();
    }

    private void renderEntities(GameState s) {
        int camX = cameraX(s), camY = cameraY(s);
        int baseTop = MAP_TOP + 2;
        for (var e : s.entities) {
            if (!mapView.wasVisibleLastRender(e.x, e.y)) continue;
            int sx = e.x - camX, sy = e.y - camY;
            if (sx >= 0 && sy >= 0 && sx < mapView.getViewW() && sy < mapView.getViewH()) {
                ANSI.gotoRC(baseTop + sy, mapView.getLeft() + sx);
                ANSI.setFg(31);
                System.out.print(e.glyph);
                ANSI.resetStyle();
            }
        }
    }

    public boolean wasVisibleLastRender(int x, int y) {
        return mapView.wasVisibleLastRender(x, y);
    }

    public boolean isNearCamera(int x, int y, GameState s) {
        int camX = cameraX(s), camY = cameraY(s);
        return x >= camX && x < camX + mapView.getViewW() && y >= camY && y < mapView.getViewH();
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
