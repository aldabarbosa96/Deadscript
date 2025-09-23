package utils;

import world.GameMap;
import java.util.*;

public final class SpawnUtil {
    private SpawnUtil() {
    }

    public static int[] pickIndoorSpawn(GameMap m, Random rng) {
        int h = m.h, w = m.w;
        boolean[][] seen = new boolean[h][w];

        ArrayList<ArrayList<int[]>> comps = new ArrayList<>();
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (!seen[y][x] && isIndoorFloor(m, x, y)) {
                    ArrayList<int[]> tiles = new ArrayList<>(64);
                    flood(m, x, y, seen, tiles);
                    if (!tiles.isEmpty()) comps.add(tiles);
                }
            }
        }

        if (comps.isEmpty()) {
            // Centro seguro si existe
            int cx = w / 2, cy = h / 2;
            for (int r = 0; r <= Math.max(w, h); r++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dx = -r; dx <= r; dx++) {
                        int x = cx + dx, y = cy + dy;
                        if (inBounds(m, x, y) && m.walk[y][x]) return new int[]{x, y};
                    }
                }
            }
            return new int[]{Math.max(0, w / 2), Math.max(0, h / 2)};
        }

        ArrayList<int[]> comp = comps.get(rng.nextInt(comps.size()));
        ArrayList<int[]> best = new ArrayList<>();
        ArrayList<int[]> ok = new ArrayList<>();
        for (int[] t : comp) {
            int x = t[0], y = t[1];
            int nearDoors = countAdj(m, x, y, '+');
            int nearWalls = countAdjAnyOf(m, x, y, "╔╗╚╝═║");
            if (nearDoors == 0 && nearWalls == 0) best.add(t);
            else if (nearDoors == 0) ok.add(t);
        }

        int[] pick;
        if (!best.isEmpty()) pick = best.get(rng.nextInt(best.size()));
        else if (!ok.isEmpty()) pick = ok.get(rng.nextInt(ok.size()));
        else pick = comp.get(rng.nextInt(comp.size()));
        return pick;
    }

    private static void flood(GameMap m, int sx, int sy, boolean[][] seen, ArrayList<int[]> out) {
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        seen[sy][sx] = true;
        while (!q.isEmpty()) {
            int[] c = q.pollFirst();
            out.add(c);
            for (int[] d : d4) {
                int nx = c[0] + d[0], ny = c[1] + d[1];
                if (!inBounds(m, nx, ny) || seen[ny][nx]) continue;
                if (!isIndoorFloor(m, nx, ny)) continue;
                seen[ny][nx] = true;
                q.addLast(new int[]{nx, ny});
            }
        }
    }

    private static boolean isIndoorFloor(GameMap m, int x, int y) {
        return m.indoor[y][x] && m.tiles[y][x] == '▓' && m.walk[y][x];
    }

    private static boolean inBounds(GameMap m, int x, int y) {
        return x >= 0 && y >= 0 && x < m.w && y < m.h;
    }

    private static int countAdj(GameMap m, int x, int y, char target) {
        int c = 0;
        if (inBounds(m, x + 1, y) && m.tiles[y][x + 1] == target) c++;
        if (inBounds(m, x - 1, y) && m.tiles[y][x - 1] == target) c++;
        if (inBounds(m, x, y + 1) && m.tiles[y + 1][x] == target) c++;
        if (inBounds(m, x, y - 1) && m.tiles[y - 1][x] == target) c++;
        return c;
    }

    private static int countAdjAnyOf(GameMap m, int x, int y, String chars) {
        int c = 0;
        if (inBounds(m, x + 1, y) && chars.indexOf(m.tiles[y][x + 1]) >= 0) c++;
        if (inBounds(m, x - 1, y) && chars.indexOf(m.tiles[y][x - 1]) >= 0) c++;
        if (inBounds(m, x, y + 1) && chars.indexOf(m.tiles[y + 1][x]) >= 0) c++;
        if (inBounds(m, x, y - 1) && chars.indexOf(m.tiles[y - 1][x]) >= 0) c++;
        return c;
    }
}
