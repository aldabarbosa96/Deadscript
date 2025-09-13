package game;

public final class Config {
    private Config() {}

    // Player
    public static final long PLAYER_MOVE_COOLDOWN_NS = 180_000_000L; // ≈5.55 tiles/s

    // Zombies
    public static final int   MAX_ZOMBIES = 80;
    public static final double SPAWN_EVERY_SEC = 3.0;
    public static final int   SPAWN_RADIUS_MIN = 24;
    public static final int   SPAWN_RADIUS_MAX = 44;
    public static final double ZOMBIE_MIN_SPEED = 0.45;
    public static final double ZOMBIE_SPEED_RANGE = 0.25;

    // Combate simple
    public static final int   ZOMBIE_HIT_DAMAGE = 1;
    public static final double ZOMBIE_ATTACK_COOLDOWN_SEC = 1.0;
}
