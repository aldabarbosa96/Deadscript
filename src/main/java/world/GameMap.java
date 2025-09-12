package world;

public class GameMap {
    public final int w, h;
    public final char[][] tiles;
    public final boolean[][] walk;
    public final boolean[][] transp;
    public final boolean[][] explored;

    public GameMap(int w, int h) {
        this.w = w;
        this.h = h;
        this.tiles = new char[h][w];
        this.walk = new boolean[h][w];
        this.transp = new boolean[h][w];
        this.explored = new boolean[h][w];
    }

    public static GameMap demo(int w, int h) {
        GameMap m = new GameMap(w, h);
        // suelo
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                m.tiles[y][x] = '.';
                m.walk[y][x] = true;
                m.transp[y][x] = true;
                m.explored[y][x] = false;
            }
        // bordes
        for (int x = 0; x < w; x++) {
            setWall(m, x, 0);
            setWall(m, x, h - 1);
        }
        for (int y = 0; y < h; y++) {
            setWall(m, 0, y);
            setWall(m, w - 1, y);
        }
        // columnas internas
        for (int y = 4; y < h - 4; y += 4)
            for (int x = 6; x < w - 6; x += 10) setWall(m, x, y);
        return m;
    }

    private static void setWall(GameMap m, int x, int y) {
        m.tiles[y][x] = '#';
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }
}
