package world;

import java.util.*;

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
        if (coverageTarget < 0.01) coverageTarget = 0.01;
        if (coverageTarget > 0.45) coverageTarget = 0.45;
        if (minBlobSize < 3) minBlobSize = 3;
        if (maxBlobSize < minBlobSize) maxBlobSize = minBlobSize;

        Random rng = new Random(seed);
        GameMap m = new GameMap(w, h);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) setFloor(m, x, y);

        for (int x = 0; x < w; x++) {
            setWall(m, x, 0);
            setWall(m, x, h - 1);
        }
        for (int y = 0; y < h; y++) {
            setWall(m, 0, y);
            setWall(m, w - 1, y);
        }

        int cx = w / 2, cy = h / 2;
        carveDisk(m, cx, cy, safeRadius);

        int interiorArea = (w - 2) * (h - 2);
        int targetWalls = (int) Math.round(coverageTarget * interiorArea);
        int placedWalls = 0;

        int minDist2 = minClusterDist * minClusterDist;
        List<int[]> seeds = new ArrayList<>();
        int attempts = 0, maxAttempts = interiorArea * 5;
        int margin = 2;

        while (placedWalls < targetWalls && attempts++ < maxAttempts) {
            int x = margin + 1 + rng.nextInt(Math.max(1, w - 2 * (margin + 1)));
            int y = margin + 1 + rng.nextInt(Math.max(1, h - 2 * (margin + 1)));
            if (dist2(x, y, cx, cy) <= (safeRadius + 1) * (safeRadius + 1)) continue;

            boolean farEnough = true;
            for (int[] s : seeds) {
                if (dist2(x, y, s[0], s[1]) < minDist2) {
                    farEnough = false;
                    break;
                }
            }
            if (!farEnough) continue;

            seeds.add(new int[]{x, y});

            int blobTarget = heavyBetween(rng, minBlobSize, maxBlobSize);
            int placed = growBlob(m, rng, x, y, blobTarget, margin, cx, cy, safeRadius, targetWalls - placedWalls);
            placedWalls += placed;
        }

        return m;
    }

    private static void setWall(GameMap m, int x, int y) {
        m.tiles[y][x] = '#';
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }

    private static void setFloor(GameMap m, int x, int y) {
        m.tiles[y][x] = '.';
        m.walk[y][x] = true;
        m.transp[y][x] = true;
    }

    private static boolean inBounds(GameMap m, int x, int y) {
        return x >= 0 && y >= 0 && x < m.w && y < m.h;
    }

    private static boolean inInterior(GameMap m, int x, int y, int margin) {
        return x >= (1 + margin) && y >= (1 + margin) && x < (m.w - 1 - margin) && y < (m.h - 1 - margin);
    }

    private static int dist2(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private static void carveDisk(GameMap m, int cx, int cy, int r) {
        int r2 = r * r;
        int x0 = Math.max(1, cx - r), x1 = Math.min(m.w - 2, cx + r);
        int y0 = Math.max(1, cy - r), y1 = Math.min(m.h - 2, cy + r);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (dist2(x, y, cx, cy) <= r2) setFloor(m, x, y);
            }
        }
    }

    private static int growBlob(GameMap m, Random rng, int sx, int sy, int targetSize, int margin, int cx, int cy, int safeRadius, int budget) {

        if (budget <= 0) return 0;
        if (!inInterior(m, sx, sy, margin)) return 0;
        if (!m.walk[sy][sx]) return 0;
        if (dist2(sx, sy, cx, cy) <= (safeRadius + 1) * (safeRadius + 1)) return 0;

        ArrayDeque<int[]> frontier = new ArrayDeque<>();
        boolean[][] inBlob = new boolean[m.h][m.w];

        frontier.add(new int[]{sx, sy});
        inBlob[sy][sx] = true;

        int placed = 0;
        int placedThisBlob = 0;
        int minX = sx, maxX = sx, minY = sy, maxY = sy;

        while (!frontier.isEmpty() && placedThisBlob < targetSize) {
            int idx = rng.nextInt(frontier.size());
            int[] cur = pickAndRemove(frontier, idx);

            int x = cur[0], y = cur[1];

            if (m.walk[y][x] && inInterior(m, x, y, margin)) {
                if (dist2(x, y, cx, cy) > (safeRadius + 1) * (safeRadius + 1)) {
                    setWall(m, x, y);
                    placed++;
                    placedThisBlob++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                    if (placed >= budget || placedThisBlob >= targetSize) break;
                }
            }

            double progress = Math.min(1.0, placedThisBlob / Math.max(1.0, (double) targetSize));
            double p = 0.82 - 0.55 * progress;

            int[][] dirs = rng.nextBoolean() ? new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}} : new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                if (!inBounds(m, nx, ny)) continue;
                if (!inInterior(m, nx, ny, margin)) continue;
                if (inBlob[ny][nx]) continue;
                if (!m.walk[ny][nx]) continue;
                if (rng.nextDouble() < p) {
                    inBlob[ny][nx] = true;
                    frontier.add(new int[]{nx, ny});
                }
            }
        }

        if (placedThisBlob > 0 && placed < budget) {
            int pad = 1;
            int x0 = Math.max(1, minX - pad);
            int y0 = Math.max(1, minY - pad);
            int x1 = Math.min(m.w - 2, maxX + pad);
            int y1 = Math.min(m.h - 2, maxY + pad);
            int filled = fillHolesRegion(m, x0, y0, x1, y1, budget - placed);
            placed += filled;
            placedThisBlob += filled;
        }

        return placed;
    }

    private static int[] pickAndRemove(ArrayDeque<int[]> deque, int index) {
        int size = deque.size();
        int[][] arr = new int[size][2];
        for (int i = 0; i < size; i++) arr[i] = deque.pollFirst();
        int[] picked = arr[index];
        for (int i = 0; i < size; i++) if (i != index) deque.addLast(arr[i]);
        return picked;
    }

    private static int heavyBetween(Random rng, int a, int b) {
        if (a >= b) return a;
        double u = rng.nextDouble();
        double t;
        if (u < 0.2) {
            t = Math.pow(rng.nextDouble(), 2.4);
        } else if (u < 0.85) {
            t = rng.nextDouble();
        } else {
            t = 1.0 - Math.pow(rng.nextDouble(), 2.0);
        }
        int v = a + (int) Math.round(t * (b - a));
        if (v < a) v = a;
        if (v > b) v = b;
        return v;
    }

    private static int fillHolesRegion(GameMap m, int x0, int y0, int x1, int y1, int budget) {
        if (budget <= 0) return 0;
        int rw = x1 - x0 + 1, rh = y1 - y0 + 1;
        if (rw <= 0 || rh <= 0) return 0;
        boolean[][] vis = new boolean[rh][rw];
        ArrayDeque<int[]> q = new ArrayDeque<>();

        for (int x = x0; x <= x1; x++) {
            if (m.walk[y0][x]) {
                vis[0][x - x0] = true;
                q.add(new int[]{x, y0});
            }
            if (m.walk[y1][x]) {
                vis[rh - 1][x - x0] = true;
                q.add(new int[]{x, y1});
            }
        }
        for (int y = y0; y <= y1; y++) {
            if (m.walk[y][x0]) {
                vis[y - y0][0] = true;
                q.add(new int[]{x0, y});
            }
            if (m.walk[y][x1]) {
                vis[y - y0][rw - 1] = true;
                q.add(new int[]{x1, y});
            }
        }

        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] cur = q.pollFirst();
            int x = cur[0], y = cur[1];
            for (int[] d : d4) {
                int nx = x + d[0], ny = y + d[1];
                if (nx < x0 || ny < y0 || nx > x1 || ny > y1) continue;
                int vx = nx - x0, vy = ny - y0;
                if (vis[vy][vx]) continue;
                if (!m.walk[ny][nx]) continue;
                vis[vy][vx] = true;
                q.addLast(new int[]{nx, ny});
            }
        }

        int filled = 0;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!m.walk[y][x] || vis[y - y0][x - x0]) continue;
                setWall(m, x, y);
                filled++;
                if (filled >= budget) return filled;
            }
        }
        return filled;
    }
}
