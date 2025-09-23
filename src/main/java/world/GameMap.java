package world;

import world.builders.ForestBuilder;
import world.builders.HouseBuilder;
import world.builders.RiverBuilder;
import world.builders.RocksBuilder;

import java.util.*;

// TODO --> refactorizar clase completa pronto
public class GameMap {
    public final int w, h;
    public final char[][] tiles;
    public final boolean[][] walk;
    public final boolean[][] transp;
    public final boolean[][] explored;
    public final boolean[][] indoor;

    public GameMap(int w, int h) {
        this.w = w;
        this.h = h;
        this.tiles = new char[h][w];
        this.walk = new boolean[h][w];
        this.transp = new boolean[h][w];
        this.explored = new boolean[h][w];
        this.indoor = new boolean[h][w];
    }

    public static GameMap randomBalanced(int w, int h) {
        long seed = System.nanoTime();
        double coverage = 0.14;
        int minClusterDist = 18;
        int minBlobSize = 5;
        int maxBlobSize = 55;
        int safeRadius = 6;
        return randomBalanced(w, h, seed, coverage, minClusterDist, minBlobSize, maxBlobSize, safeRadius);
    }

    public static GameMap randomBalanced(int w, int h, long seed, double coverageTarget, int minClusterDist, int minBlobSize, int maxBlobSize, int safeRadius) {
        if (w < 5 || h < 5) throw new IllegalArgumentException("Mapa demasiado pequeño");
        coverageTarget = Math.max(0.01, Math.min(0.45, coverageTarget));
        minBlobSize = Math.max(3, minBlobSize);
        maxBlobSize = Math.max(minBlobSize, maxBlobSize);

        Random rng = new Random(seed);
        GameMap m = new GameMap(w, h);

        // base suelo
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) setFloor(m, x, y);

        // borde con árboles
        for (int x = 0; x < w; x++) {
            setTree(m, x, 0);
            setTree(m, x, h - 1);
        }
        for (int y = 0; y < h; y++) {
            setTree(m, 0, y);
            setTree(m, w - 1, y);
        }

        int cx = w / 2, cy = h / 2;
        carveDisk(m, cx, cy, safeRadius);

        // elementos mapa
        int area = w * h;
        int groups = Math.max(12, area / 16000) + rng.nextInt(6);
        int singles = Math.max(22, area / 12000) + rng.nextInt(10);
        new ForestBuilder().build(m, rng, coverageTarget, minClusterDist, minBlobSize, maxBlobSize, safeRadius, 2);
        new RiverBuilder().build(m, rng, cx, cy, safeRadius);
        new HouseBuilder().build(m, rng, groups, singles, safeRadius);
        new RocksBuilder().build(m, rng, Math.max(8, (w * h) / 270), Math.max(12, (w * h) / 200), 1, 7, safeRadius);


        return m;
    }

    public static final class Stair {
        public final int x, y;
        public Link up;
        public Link down;

        public Stair(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public static final class Link {
            public final GameMap map; // mapa destino
            public final int x, y;    // posición destino

            public Link(GameMap map, int x, int y) {
                this.map = map;
                this.x = x;
                this.y = y;
            }
        }
    }

    // tiles
    private static void setTree(GameMap m, int x, int y) {
        m.tiles[y][x] = '#';
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }

    private static void setFloor(GameMap m, int x, int y) {
        m.tiles[y][x] = '▓';
        m.walk[y][x] = true;
        m.transp[y][x] = true;
    }

    private static int dist2(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private static void carveDisk(GameMap m, int cx, int cy, int r) {
        int r2 = r * r, x0 = Math.max(1, cx - r), x1 = Math.min(m.w - 2, cx + r), y0 = Math.max(1, cy - r), y1 = Math.min(m.h - 2, cy + r);
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) if (dist2(x, y, cx, cy) <= r2) setFloor(m, x, y);
    }

    private final java.util.Map<Long, Stair> stairs = new java.util.HashMap<>();

    private static long stairKey(int x, int y) {
        return (((long) y) << 32) ^ (x & 0xffffffffL);
    }

    public boolean hasStairAt(int x, int y) {
        return stairs.containsKey(stairKey(x, y));
    }

    public Stair getStairAt(int x, int y) {
        return stairs.get(stairKey(x, y));
    }

    public void placeStair(int x, int y) {
        tiles[y][x] = 'S';
        walk[y][x] = true;
        transp[y][x] = true;
        stairs.put(stairKey(x, y), new Stair(x, y));
    }

    public void linkStairUp(int x, int y, GameMap target, int tx, int ty) {
        Stair s = getStairAt(x, y);
        if (s != null) s.up = new Stair.Link(target, tx, ty);
    }

    public void linkStairDown(int x, int y, GameMap target, int tx, int ty) {
        Stair s = getStairAt(x, y);
        if (s != null) s.down = new Stair.Link(target, tx, ty);
    }
}
