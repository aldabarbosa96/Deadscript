package world;

public class Entity {
    public enum Type { ZOMBIE, LOOT, OTHER }
    public int x, y;
    public char glyph;
    public double speedTilesPerSec;
    public double moveRemainder;
    public double attackCooldown;
    public int groupId;
    public boolean leader;
    public int offX, offY;
    public boolean revealed;
    public Type type = Type.OTHER;
    public int hp;
    public int maxHp;

    public Entity(int x, int y, char glyph, double speed) {
        this.x = x;
        this.y = y;
        this.glyph = glyph;
        this.speedTilesPerSec = speed;
    }
}
