package world;

public class Entity {
    public int x, y;
    public char glyph;
    public double speedTilesPerSec;
    public double moveRemainder;
    public double attackCooldown;

    // Cohesión de grupo
    public int groupId;
    public boolean leader;
    public int offX, offY; // offset deseado respecto al líder

    public Entity(int x, int y, char glyph, double speed) {
        this.x = x;
        this.y = y;
        this.glyph = glyph;
        this.speedTilesPerSec = speed;
    }
}
