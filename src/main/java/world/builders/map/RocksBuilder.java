package world.builders.map;

import world.GameMap;
import java.util.ArrayDeque;
import java.util.Random;

public final class RocksBuilder {
    public void build(GameMap m, Random rng, int minGroups, int maxGroups, int sizeMin, int sizeMax, int safeRadius) {

        sizeMin = Math.max(1, sizeMin);
        sizeMax = Math.max(sizeMin, sizeMax);
        minGroups = Math.max(1, minGroups);
        maxGroups = Math.max(minGroups, maxGroups);

        int groups = minGroups + rng.nextInt(maxGroups - minGroups + 1);
        int placedGroups = 0;
        int attempts = groups * 30;
        int margin = 1;
        int cx = m.w / 2, cy = m.h / 2;
        int safe2 = (safeRadius + 1) * (safeRadius + 1);

        while (placedGroups < groups && attempts-- > 0) {
            int x = margin + rng.nextInt(Math.max(1, m.w - 2 * margin));
            int y = margin + rng.nextInt(Math.max(1, m.h - 2 * margin));
            if (!inInterior(m, x, y, margin)) continue;
            if (m.tiles[y][x] != '▓') continue;
            if (m.indoor[y][x]) continue;
            if (dist2(x, y, cx, cy) <= safe2) continue;

            int target = sizeMin + rng.nextInt(sizeMax - sizeMin + 1);
            int got = sprinkleRockMicroBlob(m, rng, x, y, target, cx, cy, safe2);
            if (got > 0) placedGroups++;
        }

        // Fallback: si no se colocó ninguna roca
        if (placedGroups == 0) {
            for (int y = 1; y < m.h - 1; y++) {
                for (int x = 1; x < m.w - 1; x++) {
                    if (m.tiles[y][x] == '▓' && !m.indoor[y][x] && dist2(x, y, cx, cy) > safe2) {
                        paintRock(m, x, y);
                        return;
                    }
                }
            }
        }
    }

    private int sprinkleRockMicroBlob(GameMap m, Random rng, int sx, int sy, int target, int cx, int cy, int safe2) {
        int placed = 0, steps = 0;
        ArrayDeque<int[]> q = new ArrayDeque<>();
        boolean[][] seen = new boolean[m.h][m.w];
        q.add(new int[]{sx, sy});
        seen[sy][sx] = true;

        int[][] d8 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

        while (!q.isEmpty() && placed < target && steps < target * 12) {
            steps++;
            int[] cur = q.pollFirst();
            int x = cur[0], y = cur[1];

            if (m.tiles[y][x] == '▓' && !m.indoor[y][x] && dist2(x, y, cx, cy) > safe2) {
                paintRock(m, x, y);
                placed++;
            }

            for (int[] d : d8) {
                if (rng.nextDouble() > 0.25) continue;
                int nx = x + d[0], ny = y + d[1];
                if (!inBounds(m, nx, ny) || seen[ny][nx]) continue;
                if (m.tiles[ny][nx] != '▓' || m.indoor[ny][nx]) continue;
                seen[ny][nx] = true;
                q.addLast(new int[]{nx, ny});
            }
        }
        return placed;
    }

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

    private void paintRock(GameMap m, int x, int y) {
        m.tiles[y][x] = '█';
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }
}
