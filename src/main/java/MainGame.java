import ui.player.EquipmentPanel;
import ui.menu.MapView;
import ui.player.PlayerHud;
import ui.player.PlayerStates;
import ui.menu.MessageLog;
import ui.menu.ActionBar;
import ui.input.InputHandler;
import utils.ANSI;
import world.GameMap;
import world.Entity;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private static MessageLog msgLog;
    private static ActionBar actionBar;

    private static final int HUD_LEFT = 1;
    private static final int GAP = 2;

    private static final int STATES_LEFT = 48;
    private static final int STATES_WIDTH = 30;
    private static final int STATS_WIDTH = STATES_LEFT - HUD_LEFT - GAP;

    private static final int EQUIP_LEFT = STATES_LEFT + STATES_WIDTH + GAP;
    private static final int EQUIP_ROWS = 12;

    private static final int MAP_TOP = 16; // título mapa
    private static final int MAP_LEFT = 1;
    private static final int VIEW_W = 119;
    private static final int VIEW_H = 38;  // alto de celdas (no incluye título + blanco)

    private static final int LOG_ROWS = 8;

    private static String ubicacion = "Goodsummer";
    private static int temperaturaC = 18;

    private static int salud = 65, maxSalud = 100;
    private static int energia = 82, maxEnergia = 100;
    private static int hambre = 36, maxHambre = 100;
    private static int sed = 10, maxSed = 100;
    private static int sueno = 75, maxSueno = 100;
    private static boolean sangrado = false;
    private static int infeccionPct = 0;
    private static boolean escondido = true;

    private static int lastDx = 0, lastDy = 0;

    // Tiempo real
    private static final double FIXED_DT = 1.0 / 60.0;
    private static long prevTimeNs;
    private static double lagSec = 0.0;
    private static final long RENDER_MIN_INTERVAL_NS = 200_000_000L; // 200ms ~ 5 FPS
    private static long lastRenderNs = 0L;

    // Jugador: cap de velocidad
    private static final long PLAYER_MOVE_COOLDOWN_NS = 180_000_000L; // 180 ms ≈ 5.55 tiles/s
    private static long lastPlayerStepNs = 0L;

    // Entidades y spawn
    private static final List<Entity> entities = new ArrayList<>();
    private static final Random RNG = new Random();
    private static final int MAX_ZOMBIES = 80;

    // Spawns por "grupos" tipo mini-horda
    private static final double SPAWN_EVERY_SEC = 3.0;   // menos frecuente
    private static double spawnTimer = 0.0;
    private static final int SPAWN_RADIUS_MIN = 24;
    private static final int SPAWN_RADIUS_MAX = 44;
    private static int nextGroupId = 1;

    // Barras temporizadas
    private static double hambreAcc, sedAcc, suenoAcc, energiaAcc;

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

        int logTop = MAP_TOP + 2 + viewH + 1;
        msgLog = new MessageLog(logTop, MAP_LEFT, headerWidth, LOG_ROWS);
        String infoDia = String.format("%s, %d°C, Zona: %s", "Soleado", temperaturaC, (ubicacion + " (Bosque)"));
        msgLog.add(String.format("Día %d: %s", 1, infoDia));
        msgLog.add(String.format("Posición inicial: (%d,%d).", px, py));

        int menuTop = logTop + LOG_ROWS + 1;
        actionBar = new ActionBar(menuTop, MAP_LEFT, headerWidth);

        hambreAcc = hambre;
        sedAcc = sed;
        suenoAcc = sueno;
        energiaAcc = energia;

        ANSI.clearScreenAndHome();
        ANSI.setScrollRegion(MAP_TOP + 2, MAP_TOP + 2 + viewH - 1);
        mapView.prefill();
        renderAll();

        prevTimeNs = System.nanoTime();
        lastRenderNs = prevTimeNs;
        dirty = true;

    }

    private static void gameLoop() {
        long lastRenderSec = -1;
        while (running) {
            // INPUT con cap de velocidad para el jugador
            InputHandler.Command cmd;
            while ((cmd = input.poll(0)) != InputHandler.Command.NONE) {
                switch (cmd) {
                    case UP -> dirty |= tryMoveThrottled(0, -1);
                    case DOWN -> dirty |= tryMoveThrottled(0, 1);
                    case LEFT -> dirty |= tryMoveThrottled(-1, 0);
                    case RIGHT -> dirty |= tryMoveThrottled(1, 0);

                    case INVENTORY -> {
                        msgLog.add("Abres el inventario.");
                        dirty = true;
                    }
                    case EQUIPMENT -> {
                        msgLog.add("Abres el equipo.");
                        dirty = true;
                    }
                    case STATS -> {
                        msgLog.add("Abres el panel de estadísticas.");
                        dirty = true;
                    }
                    case ACTION -> {
                        msgLog.add("Acción principal.");
                        dirty = true;
                    }
                    case OPTIONS -> {
                        msgLog.add("Abres el menú de opciones.");
                        dirty = true;
                    }

                    case REGENERATE -> {
                        regenerateMap();
                        msgLog.add("Nuevo mapa generado.");
                        dirty = true;
                    }
                    case QUIT -> running = false;
                    default -> {
                    }
                }
            }

            // TIMESTEP FIJO
            long now = System.nanoTime();
            double dt = (now - prevTimeNs) / 1_000_000_000.0;
            if (dt < 0) dt = 0;
            if (dt > 0.25) dt = 0.25;
            prevTimeNs = now;

            lagSec += dt;
            while (lagSec >= FIXED_DT) {
                boolean worldChanged = update(FIXED_DT);
                if (worldChanged) dirty = true;
                lagSec -= FIXED_DT;
            }

            // RENDER con límite de frecuencia + tick del reloj por segundo
            long nowSec = System.currentTimeMillis() / 1000L;
            long nowNsForRender = System.nanoTime();

            boolean wantRender = false;
            if (dirty && (nowNsForRender - lastRenderNs) >= RENDER_MIN_INTERVAL_NS) {
                wantRender = true;
            }
            if (nowSec != lastRenderSec) {
                wantRender = true;
            }

            if (wantRender) {
                renderAll();
                dirty = false;
                lastRenderSec = nowSec;
                lastRenderNs = nowNsForRender;
            }


            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static boolean tryMoveThrottled(int dx, int dy) {
        long now = System.nanoTime();
        if (now - lastPlayerStepNs < PLAYER_MOVE_COOLDOWN_NS) return false;
        boolean ok = tryMove(dx, dy);
        if (ok) lastPlayerStepNs = now;
        return ok;
    }

    private static boolean update(double dt) {
        spawnTimer += dt;
        if (spawnTimer >= SPAWN_EVERY_SEC) {
            spawnTimer = 0.0;
            if (spawnZombieGroup()) dirty = true;
        }

        boolean moved = updateZombies(dt);
        drainNeeds(dt);
        return moved;
    }

    private static boolean updateZombies(double dt) {
        boolean touchedScreen = false;

        // Construye índice de líderes por grupo
        Map<Integer, Entity> leaders = new HashMap<>();
        for (Entity e : entities) if (e.leader) leaders.put(e.groupId, e);

        int camX = Math.max(0, Math.min(px - mapView.getViewW() / 2, gameMap.w - mapView.getViewW()));
        int camY = Math.max(0, Math.min(py - mapView.getViewH() / 2, gameMap.h - mapView.getViewH()));

        for (var e : entities) {
            int beforeX = e.x, beforeY = e.y;

            int targetX, targetY;
            if (e.leader) {
                targetX = px;
                targetY = py;
            } else {
                Entity lead = leaders.get(e.groupId);
                if (lead == null) { // si se quedó sin líder, que persiga al jugador
                    targetX = px;
                    targetY = py;
                } else {
                    targetX = lead.x + e.offX;
                    targetY = lead.y + e.offY;
                }
            }

            int stepX = Integer.compare(targetX - e.x, 0);
            int stepY = Integer.compare(targetY - e.y, 0);

            double tiles = e.speedTilesPerSec * dt + e.moveRemainder;
            while (tiles >= 1.0) {
                tiles -= 1.0;

                int nx = e.x, ny = e.y;
                if (stepX != 0 && stepY != 0) {
                    if (RNG.nextBoolean()) nx += stepX;
                    else ny += stepY;
                } else if (stepX != 0) {
                    nx += stepX;
                } else if (stepY != 0) {
                    ny += stepY;
                }

                if (nx >= 0 && ny >= 0 && nx < gameMap.w && ny < gameMap.h && gameMap.walk[ny][nx]) {
                    e.x = nx;
                    e.y = ny;
                } else {
                    break;
                }
            }
            e.moveRemainder = tiles;

            // Solo marcamos repintado si su visibilidad (FOV anterior) o su cercanía a cámara puede afectar a pantalla
            if (beforeX != e.x || beforeY != e.y) {
                boolean wasOnCam = isNearCamera(beforeX, beforeY, camX, camY);
                boolean nowOnCam = isNearCamera(e.x, e.y, camX, camY);
                boolean wasVis = mapView.wasVisibleLastRender(beforeX, beforeY);
                boolean nowVis = mapView.wasVisibleLastRender(e.x, e.y);
                if (wasOnCam || nowOnCam || wasVis || nowVis) touchedScreen = true;
            }

            if (e.attackCooldown > 0) e.attackCooldown -= dt;
            if (e.x == px && e.y == py && e.attackCooldown <= 0) {
                salud = Math.max(0, salud - 1);
                e.attackCooldown = 1.0;
                msgLog.add("¡Un zombi te ha golpeado!");
                touchedScreen = true;
            }
        }
        return touchedScreen;
    }

    // 1 grupo por “tick”: tamaño 1..5, con offsets cortos para mantener cohesión
    private static boolean spawnZombieGroup() {
        if (entities.size() >= MAX_ZOMBIES) return false;

        int camX = Math.max(0, Math.min(px - mapView.getViewW() / 2, gameMap.w - mapView.getViewW()));
        int camY = Math.max(0, Math.min(py - mapView.getViewH() / 2, gameMap.h - mapView.getViewH()));

        int size = 1 + RNG.nextInt(5); // 1..5
        double ang = RNG.nextDouble() * Math.PI * 2.0;
        int dist = SPAWN_RADIUS_MIN + RNG.nextInt(Math.max(1, SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN + 1));

        int anchorX = px + (int) Math.round(Math.cos(ang) * dist);
        int anchorY = py + (int) Math.round(Math.sin(ang) * dist);

        if (anchorX < 0 || anchorY < 0 || anchorX >= gameMap.w || anchorY >= gameMap.h) return false;

        int groupId = nextGroupId++;
        boolean anyNear = false;

        for (int i = 0; i < size && entities.size() < MAX_ZOMBIES; i++) {
            // esparce cerca del ancla (radio 2)
            int rx = anchorX + RNG.nextInt(5) - 2;
            int ry = anchorY + RNG.nextInt(5) - 2;

            if (rx < 0 || ry < 0 || rx >= gameMap.w || ry >= gameMap.h) continue;
            if (!gameMap.walk[ry][rx]) continue;

            // zombis más lentos (≈0.45–0.70 tiles/s)
            double speed = 0.45 + RNG.nextDouble() * 0.25;

            Entity z = new Entity(rx, ry, 'Z', speed);
            z.groupId = groupId;
            z.leader = (i == 0);
            if (!z.leader) {
                // offset de seguidor alrededor del líder
                z.offX = RNG.nextInt(5) - 2;
                z.offY = RNG.nextInt(5) - 2;
            }
            entities.add(z);

            if (isNearCamera(rx, ry, camX, camY)) anyNear = true;
        }
        return anyNear;
    }

    private static boolean isNearCamera(int x, int y, int camX, int camY) {
        return x >= camX && x < camX + mapView.getViewW() && y >= camY && y < camY + mapView.getViewH();
    }

    private static void drainNeeds(double dt) {
        hambreAcc = clamp(hambreAcc - dt * 0.02, 0, maxHambre);
        sedAcc = clamp(sedAcc - dt * 0.05, 0, maxSed);
        suenoAcc = clamp(suenoAcc - dt * 0.01, 0, maxSueno);
        energiaAcc = clamp(energiaAcc - dt * 0.03, 0, maxEnergia);

        hambre = (int) Math.round(hambreAcc);
        sed = (int) Math.round(sedAcc);
        sueno = (int) Math.round(suenoAcc);
        energia = (int) Math.round(energiaAcc);
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static void renderAll() {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        hud.renderHud(1, hora, "Soleado", temperaturaC, ubicacion, salud, maxSalud, energia, maxEnergia, hambre, maxHambre, sed, maxSed, sueno, maxSueno, px, py, rumboTexto(lastDx, lastDy));
        states.renderStates(salud, maxSalud, energia, maxEnergia, hambre, maxHambre, sed, maxSed, sueno, maxSueno, sangrado, infeccionPct, escondido);
        equip.render("Navaja", "-", "Gorra", "-", "-", "-", "-", "Mochila tela", 0, 0, 5, 20.0);

        mapView.render(gameMap, px, py); // recalcula FOV interno
        renderEntities();                // dibuja zombis SOLO si están en FOV

        msgLog.render();
        actionBar.render();
        ANSI.gotoRC(1, 1);
        ANSI.flush();
    }

    private static void renderEntities() {
        int camX = Math.max(0, Math.min(px - mapView.getViewW() / 2, gameMap.w - mapView.getViewW()));
        int camY = Math.max(0, Math.min(py - mapView.getViewH() / 2, gameMap.h - mapView.getViewH()));
        int baseTop = MAP_TOP + 2;

        for (var e : entities) {
            // No dibujar si no está en FOV del último render (evita parpadeos al borde)
            if (!mapView.wasVisibleLastRender(e.x, e.y)) continue;

            int sx = e.x - camX;
            int sy = e.y - camY;
            if (sx >= 0 && sy >= 0 && sx < mapView.getViewW() && sy < mapView.getViewH()) {
                ANSI.gotoRC(baseTop + sy, mapView.getLeft() + sx);
                ANSI.setFg(31);
                System.out.print(e.glyph);
                ANSI.resetStyle();
            }
        }
    }

    private static boolean tryMove(int dx, int dy) {
        int nx = px + dx, ny = py + dy;
        if (nx < 0 || ny < 0 || nx >= gameMap.w || ny >= gameMap.h) {
            msgLog.add("No puedes salir del mapa.");
            return false;
        }
        if (!gameMap.walk[ny][nx]) {
            msgLog.add("Hay un obstáculo bloqueando el paso.");
            return false;
        }
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
        entities.clear();

        int equipWidth = Math.max(18, Math.min(VIEW_W, 140) - EQUIP_LEFT);
        int headerWidth = (EQUIP_LEFT + equipWidth) - HUD_LEFT;

        int viewW = Math.min(headerWidth, gameMap.w);
        int viewH = Math.min(VIEW_H, gameMap.h);
        mapView = new MapView(MAP_TOP, MAP_LEFT, viewW, viewH, 18, gameMap, 2.0);
        mapView.prefill();

        int logTop = MAP_TOP + 2 + viewH + 1;
        msgLog.updateGeometry(logTop, MAP_LEFT, headerWidth, LOG_ROWS);

        int menuTop = logTop + LOG_ROWS + 1;
        actionBar.updateGeometry(menuTop, MAP_LEFT, headerWidth);

        ANSI.setScrollRegion(MAP_TOP + 2, MAP_TOP + 2 + viewH - 1);
        dirty = true;
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
