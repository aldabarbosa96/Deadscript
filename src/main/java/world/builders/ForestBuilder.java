package world.builders;

import world.GameMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ForestBuilder {

    public void build(GameMap m, Random rng, double coverageTarget, int minClusterDist, int minBlobSize, int maxBlobSize, int safeRadius, int margin) {

        int w = m.w, h = m.h;
        int cx = w / 2, cy = h / 2;

        coverageTarget = Math.max(0.01, Math.min(0.45, coverageTarget));
        minBlobSize = Math.max(3, minBlobSize);
        maxBlobSize = Math.max(minBlobSize, maxBlobSize);

        // objetivo de cobertura interior (como antes)
        int interiorArea = Math.max(0, (w - 2) * (h - 2));
        int targetTrees = (int) Math.round(coverageTarget * interiorArea);

        int placedTrees = 0;
        int minDist2 = minClusterDist * minClusterDist;
        List<int[]> seeds = new ArrayList<>();
        int attempts = 0, maxAttempts = Math.max(1, interiorArea * 5);

        while (placedTrees < targetTrees && attempts++ < maxAttempts) {
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
            int placed = growTreeBlob(m, rng, x, y, blobTarget, margin, cx, cy, safeRadius, targetTrees - placedTrees);
            placedTrees += placed;
        }
    }

    // --- Algoritmo de crecimiento del blob (idéntico en intención) ---
    private int growTreeBlob(GameMap m, Random rng, int sx, int sy, int targetSize, int margin, int cx, int cy, int safeRadius, int budget) {
        if (budget <= 0 || !inInterior(m, sx, sy, margin) || !m.walk[sy][sx]) return 0;
        if (dist2(sx, sy, cx, cy) <= (safeRadius + 1) * (safeRadius + 1)) return 0;

        ArrayDeque<int[]> frontier = new ArrayDeque<>();
        boolean[][] inBlob = new boolean[m.h][m.w];
        frontier.add(new int[]{sx, sy});
        inBlob[sy][sx] = true;

        int placed = 0, placedThis = 0, minX = sx, maxX = sx, minY = sy, maxY = sy;

        while (!frontier.isEmpty() && placedThis < targetSize) {
            int idx = rng.nextInt(frontier.size());
            int[] cur = pickAndRemove(frontier, idx);
            int x = cur[0], y = cur[1];

            if (m.walk[y][x] && inInterior(m, x, y, margin) && dist2(x, y, cx, cy) > (safeRadius + 1) * (safeRadius + 1)) {
                paintTree(m, x, y);
                placed++;
                placedThis++;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
                if (placed >= budget || placedThis >= targetSize) break;
            }

            double progress = Math.min(1.0, placedThis / Math.max(1.0, (double) targetSize));
            double p = 0.82 - 0.55 * progress;
            int[][] dirs = rng.nextBoolean() ? new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}} : new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                if (!inBounds(m, nx, ny) || !inInterior(m, nx, ny, margin) || inBlob[ny][nx] || !m.walk[ny][nx])
                    continue;
                if (rng.nextDouble() < p) {
                    inBlob[ny][nx] = true;
                    frontier.add(new int[]{nx, ny});
                }
            }
        }

        if (placedThis > 0 && placed < budget) {
            int pad = 1, x0 = Math.max(1, minX - pad), y0 = Math.max(1, minY - pad), x1 = Math.min(m.w - 2, maxX + pad), y1 = Math.min(m.h - 2, maxY + pad);
            placed += fillHolesRegionWithTrees(m, x0, y0, x1, y1, budget - placed);
        }
        return placed;
    }

    private int fillHolesRegionWithTrees(GameMap m, int x0, int y0, int x1, int y1, int budget) {
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
            int[] c = q.pollFirst();
            int x = c[0], y = c[1];
            for (int[] d : d4) {
                int nx = x + d[0], ny = y + d[1];
                if (nx < x0 || ny < y0 || nx > x1 || ny > y1) continue;
                int vx = nx - x0, vy = ny - y0;
                if (vis[vy][vx] || !m.walk[ny][nx]) continue;
                vis[vy][vx] = true;
                q.addLast(new int[]{nx, ny});
            }
        }

        int filled = 0;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!m.walk[y][x] || vis[y - y0][x - x0]) continue;
                paintTree(m, x, y);
                if (++filled >= budget) return filled;
            }
        }
        return filled;
    }

    // --- helpers locales (equivalentes a los privados de GameMap) ---
    private boolean inBounds(GameMap m, int x, int y) {
        return x >= 0 && y >= 0 && x < m.w && y < m.h;
    }

    private boolean inInterior(GameMap m, int x, int y, int margin) {
        return x >= (1 + margin) && y >= (1 + margin) && x < (m.w - 1 - margin) && y < (m.h - 1 - margin);
    }

    private int dist2(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private void paintTree(GameMap m, int x, int y) {
        m.tiles[y][x] = '#';
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }

    private int[] pickAndRemove(ArrayDeque<int[]> dq, int index) {
        int size = dq.size();
        int[][] arr = new int[size][2];
        for (int i = 0; i < size; i++) arr[i] = dq.pollFirst();
        int[] pick = arr[index];
        for (int i = 0; i < size; i++) if (i != index) dq.addLast(arr[i]);
        return pick;
    }

    private int heavyBetween(Random rng, int a, int b) {
        if (a >= b) return a;
        double u = rng.nextDouble();
        double t = (u < 0.2) ? Math.pow(rng.nextDouble(), 2.4) : (u < 0.85) ? rng.nextDouble() : 1.0 - Math.pow(rng.nextDouble(), 2.0);
        int v = a + (int) Math.round(t * (b - a));
        return Math.min(b, Math.max(a, v));
    }
}
