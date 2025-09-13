import ui.EquipmentPanel;
import ui.MapView;
import ui.PlayerHud;
import ui.PlayerStates;
import ui.input.InputHandler;
import utils.ANSI;
import world.GameMap;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainGame {
    private static PlayerHud hud;
    private static PlayerStates states;
    private static MapView mapView;
    private static GameMap gameMap;
    private static int px, py;

    private static InputHandler input;
    private static boolean running = true;
    private static boolean dirty = true;

    private static EquipmentPanel equip;

    private static final int HUD_LEFT = 1;
    private static final int GAP = 2;

    private static final int STATES_LEFT = 48;
    private static final int STATES_WIDTH = 30;

    private static final int STATS_WIDTH = STATES_LEFT - HUD_LEFT - GAP;

    private static final int EQUIP_LEFT = STATES_LEFT + STATES_WIDTH + GAP;
    private static final int EQUIP_ROWS = 12;

    private static String ubicacion = "Bosque";
    private static int temperaturaC = 18;

    private static int salud = 65;
    private static int maxSalud = 100;
    private static int energia = 82;
    private static int maxEnergia = 100;
    private static int hambre = 36;
    private static int maxHambre = 100;
    private static int sed = 10;
    private static int maxSed = 100;
    private static int sueno = 75;
    private static int maxSueno = 100;
    private static boolean sangrado = false;
    private static int infeccionPct = 0;
    private static boolean escondido = true;

    private static final int MAP_TOP = 16;
    private static final int MAP_LEFT = 1;
    private static final int VIEW_W = 119;
    private static final int VIEW_H = 38;

    private static int lastDx = 0;
    private static int lastDy = 0;

    public static void main(String[] args) {
        try {
            init();
            gameLoop();
        } finally {
            shutdown();
        }
    }

    private static void init() {
        ANSI.setEnabled(true);
        ANSI.useAltScreen(true);
        ANSI.setCursorVisible(false);
        ANSI.setWrap(false);

        input = new InputHandler();

        int equipWidth = Math.max(18, Math.min(VIEW_W, 140) - EQUIP_LEFT);
        int headerWidth = (EQUIP_LEFT + equipWidth) - HUD_LEFT;

        hud = new PlayerHud(1, HUD_LEFT, headerWidth, STATS_WIDTH);
        states = new PlayerStates(3, STATES_LEFT, STATES_WIDTH);
        equip = new EquipmentPanel(3, EQUIP_LEFT, equipWidth, EQUIP_ROWS);

        gameMap = GameMap.randomBalanced(240, 160);
        px = gameMap.w / 2;
        py = gameMap.h / 2;

        int viewW = Math.min(headerWidth, gameMap.w);
        int viewH = Math.min(VIEW_H, gameMap.h);
        mapView = new MapView(MAP_TOP, MAP_LEFT, viewW, viewH, 18, gameMap, 2.0);

        ANSI.clearScreenAndHome();
        ANSI.setScrollRegion(MAP_TOP, MAP_TOP + viewH - 1);
        mapView.prefill();
        renderAll();
    }

    private static void gameLoop() {
        long lastHudSec = -1;
        while (running) {
            InputHandler.Command cmd;
            while ((cmd = input.poll(0)) != InputHandler.Command.NONE) {
                switch (cmd) {
                    case UP -> dirty |= tryMove(0, -1);
                    case DOWN -> dirty |= tryMove(0, 1);
                    case LEFT -> dirty |= tryMove(-1, 0);
                    case RIGHT -> dirty |= tryMove(1, 0);
                    case REGENERATE -> {
                        regenerateMap();
                        dirty = true;
                    }
                    case QUIT -> running = false;
                    default -> {
                    }
                }
            }

            long nowSec = System.currentTimeMillis() / 1000L;
            if (dirty || nowSec != lastHudSec) {
                lastHudSec = nowSec;
                renderAll();
                dirty = false;
            }

            try {
                Thread.sleep(8);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static void renderAll() {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        hud.renderHud(1, hora, "Soleado", temperaturaC, ubicacion, salud, maxSalud, energia, maxEnergia, hambre, maxHambre, sed, maxSed, sueno, maxSueno, px, py, rumboTexto(lastDx, lastDy));
        states.renderStates(salud, maxSalud, energia, maxEnergia, hambre, maxHambre, sed, maxSed, sueno, maxSueno, sangrado, infeccionPct, escondido);
        equip.render("Navaja", "-", "Gorra", "-", "-", "-", "-", "Mochila tela", 0, 0, 5, 20.0);
        mapView.render(gameMap, px, py);

        ANSI.gotoRC(1, 1);
        ANSI.flush();
    }

    private static boolean tryMove(int dx, int dy) {
        int nx = px + dx, ny = py + dy;
        if (nx < 0 || ny < 0 || nx >= gameMap.w || ny >= gameMap.h) return false;
        if (!gameMap.walk[ny][nx]) return false;
        px = nx;
        py = ny;
        lastDx = dx;
        lastDy = dy;
        return true;
    }

    private static void regenerateMap() {
        gameMap = GameMap.randomBalanced(240, 160);
        px = gameMap.w / 2;
        py = gameMap.h / 2;

        int equipWidth = Math.max(18, Math.min(VIEW_W, 140) - EQUIP_LEFT);
        int headerWidth = (EQUIP_LEFT + equipWidth) - HUD_LEFT;
        mapView = new MapView(MAP_TOP, MAP_LEFT, Math.min(headerWidth, gameMap.w), Math.min(VIEW_H, gameMap.h), 18, gameMap, 2.0);
        mapView.prefill();
    }

    private static String rumboTexto(int dx, int dy) {
        if (dx == 0 && dy == 0) return "-";
        if (dy < 0 && dx == 0) return "N";
        if (dy < 0 && dx > 0) return "NE";
        if (dy == 0 && dx > 0) return "E";
        if (dy > 0 && dx > 0) return "SE";
        if (dy > 0 && dx == 0) return "S";
        if (dy > 0 && dx < 0) return "SO";
        if (dy == 0 && dx < 0) return "O";
        return "NO";
    }

    private static void shutdown() {
        ANSI.resetScrollRegion();
        ANSI.setWrap(true);
        ANSI.setCursorVisible(true);
        ANSI.useAltScreen(false);
        if (input != null) try {
            input.close();
        } catch (Exception ignored) {
        }
    }
}
