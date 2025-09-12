import ui.MapView;
import ui.PlayerHud;
import ui.PlayerStates;
import utils.ANSI;
import world.GameMap;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class MainGame {
    private static PlayerHud hud;
    private static PlayerStates states;
    private static Scanner sc;

    private static MapView mapView;
    private static GameMap gameMap;
    private static int px, py;

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

    // Dimensiones fijas “seguras”
    private static final int MAP_TOP = 13;
    private static final int MAP_LEFT = 1;
    private static final int VIEW_W = 119; // evita última columna
    private static final int VIEW_H = 35;

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

        sc = new Scanner(System.in);
        hud = new PlayerHud(1, 1, 100);
        states = new PlayerStates(3, 48, 30);

        gameMap = GameMap.demo(240, 160);
        px = gameMap.w / 2;
        py = gameMap.h / 2;

        int viewW = Math.min(VIEW_W, gameMap.w);
        int viewH = Math.min(VIEW_H, gameMap.h);
        mapView = new MapView(MAP_TOP, MAP_LEFT, viewW, viewH, 18, gameMap, 2.0);

        ANSI.clearScreenAndHome();

        // Fijar región de scroll solo al bloque del mapa (evita que la terminal “empuje” el HUD)
        ANSI.setScrollRegion(MAP_TOP, MAP_TOP + viewH - 1);

        // Materializar el área del mapa y primer frame
        mapView.prefill();

        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        hud.renderHud(1, hora, "Soleado", temperaturaC, ubicacion, salud, maxSalud, energia, maxEnergia, hambre, maxHambre, sed, maxSed, sueno, maxSueno);
        states.renderStates(salud, maxSalud, energia, maxEnergia, hambre, maxHambre, sed, maxSed, sueno, maxSueno, sangrado, infeccionPct, escondido);
        mapView.render(gameMap, px, py);
        ANSI.flush();
    }

    private static void gameLoop() {
        while (true) { // Ctrl+C para salir
            String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            hud.renderHud(1, hora, "Soleado", temperaturaC, ubicacion, salud, maxSalud, energia, maxEnergia, hambre, maxHambre, sed, maxSed, sueno, maxSueno);
            states.renderStates(salud, maxSalud, energia, maxEnergia, hambre, maxHambre, sed, maxSed, sueno, maxSueno, sangrado, infeccionPct, escondido);

            // El mapa se dibuja dentro de la región de scroll fijada
            mapView.render(gameMap, px, py);
            ANSI.flush();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static void shutdown() {
        ANSI.resetScrollRegion();
        ANSI.setWrap(true);
        ANSI.setCursorVisible(true);
        ANSI.useAltScreen(false);
        if (sc != null) sc.close();
    }
}
