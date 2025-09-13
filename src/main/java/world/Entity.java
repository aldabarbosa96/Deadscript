package world;

public class Entity {
    public int x, y;
    public char glyph;
    public double speedTilesPerSec;
    public double moveRemainder;
    public double attackCooldown;
    public int groupId;
    public boolean leader;
    public int offX, offY;

    public Entity(int x, int y, char glyph, double speed) {
        this.x = x;
        this.y = y;
        this.glyph = glyph;
        this.speedTilesPerSec = speed;
    }
}
