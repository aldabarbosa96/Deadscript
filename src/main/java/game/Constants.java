package game;

import java.time.format.DateTimeFormatter;

public final class Constants {
    private Constants() {
    }
    // Mundo
    public static final int WORLD_W = 1200;
    public static final int WORLD_H = 800;
    public static final long MS = 1_000_000L;
    public static final long COMBINE_WINDOW_MS = 180;
    public static final long STICKY_RENEW_MS = 260;
    public static final long COMBINE_WINDOW_NS = COMBINE_WINDOW_MS * MS;
    public static final long STICKY_RENEW_NS = STICKY_RENEW_MS * MS;

    // Player
    public static final long PLAYER_MOVE_COOLDOWN_NS = 180_000_000L; // ≈5.55 tiles/s
    public static final int FOV_OUTER_EXTRA = 2;
    public static final double SPRINT_SPEED_MULT = 2.0;
    public static final double WALK_STEP_ENERGY_COST = 0.075;
    public static final double SPRINT_ENERGY_MULT = 2.0;

    // Zombies
    public static final int MAX_ZOMBIES = 80;
    public static final double SPAWN_EVERY_SEC = 3.0;
    public static final int SPAWN_RADIUS_MIN = 24;
    public static final int SPAWN_RADIUS_MAX = 44;
    public static final double ZOMBIE_MIN_SPEED = 0.45;
    public static final double ZOMBIE_SPEED_RANGE = 0.25;

    // Combate
    public static final int ZOMBIE_HIT_DAMAGE = 1;
    public static final double ZOMBIE_ATTACK_COOLDOWN_SEC = 1.0;

    // Reloj
    public static final double FIXED_DT = 1.0 / 60.0;
    public static final long RENDER_MIN_INTERVAL_NS = 50_000_000L;
    public static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Renderizado
    public static final int HUD_LEFT = 1;
    public static final int GAP = 2;
    public static final int STATES_LEFT = 48;
    public static final int STATES_WIDTH = 30;
    public static final int STATS_WIDTH = STATES_LEFT - HUD_LEFT - GAP;
    public static final int EQUIP_LEFT = STATES_LEFT + STATES_WIDTH + GAP;
    public static final int EQUIP_ROWS = 12;
    public static final int MAP_TOP = 16;
    public static final int MAP_LEFT = 1;
    public static final int VIEW_W = 119;
    public static final int VIEW_H = 38;
    public static final int LOG_ROWS = 8;

}
