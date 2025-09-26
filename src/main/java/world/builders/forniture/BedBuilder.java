package world.builders.forniture;

import world.GameMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class BedBuilder {
    private BedBuilder() {
    }

    public static final char BED = 'b';

    public static boolean placeOneBedPreferCorners(GameMap m, Random rng, int x0, int y0, int x1, int y1, int minDistFromStairs) {
        if (rng == null) return false;
        List<int[]> stairs = collectStairs(m);
        List<int[][]> candidates = new ArrayList<>(32);

        for (int y = Math.max(y0 + 1, 1); y <= Math.min(y1 - 1, m.h - 2); y++) {
            for (int x = Math.max(x0 + 1, 1); x <= Math.min(x1 - 1, m.w - 2); x++) {
                if (!(m.indoor[y][x] && m.tiles[y][x] == '▓')) continue;

                boolean extL = isExteriorWallFacing(m, x - 1, y, x, y, x - 2, y);
                boolean extR = isExteriorWallFacing(m, x + 1, y, x, y, x + 2, y);
                boolean extU = isExteriorWallFacing(m, x, y - 1, x, y, x, y - 2);
                boolean extD = isExteriorWallFacing(m, x, y + 1, x, y, x, y + 2);

                if (extL && extU) {
                    pushIfValidH(m, candidates, x, y, x + 1, y, stairs, minDistFromStairs); // =>
                }
                if (extR && extU) {
                    pushIfValidH(m, candidates, x, y, x - 1, y, stairs, minDistFromStairs); // <=
                }
                if (extL && extD) {
                    pushIfValidH(m, candidates, x, y, x + 1, y, stairs, minDistFromStairs); // =>
                }
                if (extR && extD) {
                    pushIfValidH(m, candidates, x, y, x - 1, y, stairs, minDistFromStairs); // <=
                }
            }
        }

        if (candidates.isEmpty()) {
            for (int y = Math.max(y0 + 1, 1); y <= Math.min(y1 - 1, m.h - 2); y++) {
                for (int x = Math.max(x0 + 1, 1); x <= Math.min(x1 - 1, m.w - 2); x++) {
                    if (!(m.indoor[y][x] && m.tiles[y][x] == '▓')) continue;

                    // pared a la izquierda -> cama hacia la derecha
                    if (isExteriorWallFacing(m, x - 1, y, x, y, x - 2, y) && isFreeFloor(m, x + 1, y)) {
                        pushIfValidH(m, candidates, x, y, x + 1, y, stairs, minDistFromStairs);
                    }
                    // pared a la derecha -> cama hacia la izquierda
                    if (isExteriorWallFacing(m, x + 1, y, x, y, x + 2, y) && isFreeFloor(m, x - 1, y)) {
                        pushIfValidH(m, candidates, x, y, x - 1, y, stairs, minDistFromStairs);
                    }
                }
            }
        }

        if (candidates.isEmpty()) return false;

        candidates.sort((a, b) -> {
            int da = minDistToAny(stairs, a[0][0], a[0][1], a[1][0], a[1][1]);
            int db = minDistToAny(stairs, b[0][0], b[0][1], b[1][0], b[1][1]);
            return Integer.compare(db, da);
        });

        int pick = rng.nextInt(Math.max(1, Math.min(6, candidates.size())));
        int[][] bed = candidates.get(pick);

        paintBed(m, bed[0][0], bed[0][1]);
        paintBed(m, bed[1][0], bed[1][1]);
        return true;
    }

    private static void pushIfValidH(GameMap m, List<int[][]> out, int x1, int y1, int x2, int y2, List<int[]> stairs, int minDistFromStairs) {
        if (y2 != y1 || Math.abs(x2 - x1) != 1) return; // fuerza horizontal
        if (!inBounds(m, x2, y2)) return;
        if (!isFreeFloor(m, x1, y1) || !isFreeFloor(m, x2, y2)) return;
        if (nearDoor(m, x1, y1) || nearDoor(m, x2, y2)) return;

        int dist = minDistToAny(stairs, x1, y1, x2, y2);
        if (dist < minDistFromStairs) return;

        out.add(new int[][]{{x1, y1}, {x2, y2}});
    }

    private static boolean isFreeFloor(GameMap m, int x, int y) {
        if (!inBounds(m, x, y)) return false;
        return m.indoor[y][x] && m.tiles[y][x] == '▓' && m.walk[y][x];
    }

    private static boolean nearDoor(GameMap m, int x, int y) {
        return touchesTile(m, x, y, '+');
    }

    private static boolean touchesTile(GameMap m, int x, int y, char t) {
        int[][] d = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] v : d) {
            int nx = x + v[0], ny = y + v[1];
            if (inBounds(m, nx, ny) && m.tiles[ny][nx] == t) return true;
        }
        return false;
    }

    private static boolean isExteriorWallFacing(GameMap m, int wx, int wy, int inx, int iny, int outx, int outy) {
        if (!inBounds(m, wx, wy)) return false;
        char t = m.tiles[wy][wx];
        if (!(t == '╔' || t == '╗' || t == '╚' || t == '╝' || t == '═' || t == '║')) return false;
        if (!inBounds(m, inx, iny)) return false;

        boolean inIndoor = m.indoor[iny][inx];
        boolean outIndoor = inBounds(m, outx, outy) && m.indoor[outy][outx];
        return inIndoor && !outIndoor;
    }

    private static List<int[]> collectStairs(GameMap m) {
        ArrayList<int[]> out = new ArrayList<>();
        for (int y = 0; y < m.h; y++)
            for (int x = 0; x < m.w; x++)
                if (m.tiles[y][x] == 'S') out.add(new int[]{x, y});
        return out;
    }

    private static int minDistToAny(List<int[]> pts, int x1, int y1, int x2, int y2) {
        int best = Integer.MAX_VALUE;
        for (int[] p : pts) {
            int d1 = Math.abs(p[0] - x1) + Math.abs(p[1] - y1);
            int d2 = Math.abs(p[0] - x2) + Math.abs(p[1] - y2);
            int d = Math.min(d1, d2);
            if (d < best) best = d;
        }
        return (best == Integer.MAX_VALUE) ? 9999 : best;
    }

    private static void paintBed(GameMap m, int x, int y) {
        m.tiles[y][x] = BED;
        m.walk[y][x] = false;
        m.transp[y][x] = true;
    }

    private static boolean inBounds(GameMap m, int x, int y) {
        return x >= 0 && y >= 0 && x < m.w && y < m.h;
    }
}
