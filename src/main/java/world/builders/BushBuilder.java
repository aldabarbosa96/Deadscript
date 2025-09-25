package world.builders;

import world.GameMap;

import java.util.*;

public final class BushBuilder {
    public static final char BUSH_CHAR = '░';

    public void build(GameMap m, Random rng) {
        build(m, rng, 1, 0.05, 0.08, 1, 4, 0.03, 0.6, true);
    }

    public void build(GameMap m, Random rng, int ringWidth, double seedBase, double seedBonus, int patchMin, int patchMax, double areaRatioCap, double perTreeRatio, boolean avoidRoad) {

        final int w = m.w, h = m.h;
        ringWidth = Math.max(1, ringWidth);
        patchMin = Math.max(1, patchMin);
        patchMax = Math.max(patchMin, patchMax);

        int treeCount = 0;
        for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) if (m.tiles[y][x] == '#') treeCount++;

        int interiorArea = Math.max(0, (w - 2) * (h - 2));
        int capByArea = (int) Math.round(interiorArea * Math.max(0.0, areaRatioCap));
        int capByTrees = (int) Math.round(treeCount * Math.max(0.0, perTreeRatio));
        int budget = Math.max(0, Math.min(capByArea, capByTrees));

        if (budget == 0) return;

        final class Cand {
            int x, y, k;

            Cand(int x, int y, int k) {
                this.x = x;
                this.y = y;
                this.k = k;
            }
        }
        ArrayList<Cand> cands = new ArrayList<>();

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (m.indoor[y][x]) continue;
                if (m.tiles[y][x] != '▓') continue;
                if (avoidRoad && m.road[y][x]) continue;

                boolean nearTree = false;
                int k = 0;
                for (int dy = -ringWidth; dy <= ringWidth; dy++) {
                    for (int dx = -ringWidth; dx <= ringWidth; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx, ny = y + dy;
                        if (nx <= 0 || ny <= 0 || nx >= w - 1 || ny >= h - 1) continue;
                        if (m.tiles[ny][nx] == '#') {
                            nearTree = true;
                            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) k++;
                        }
                    }
                }
                if (!nearTree) continue;
                cands.add(new Cand(x, y, k));
            }
        }
        if (cands.isEmpty()) return;

        Collections.shuffle(cands, rng);

        boolean[][] used = new boolean[h][w];

        int placed = 0;
        for (Cand c : cands) {
            if (placed >= budget) break;
            if (used[c.y][c.x]) continue;
            if (m.tiles[c.y][c.x] != '▓') continue; // pudo cambiar

            double p = clamp01(seedBase + seedBonus * c.k);
            if (rng.nextDouble() >= p) continue;

            int target = heavyBetween(rng, patchMin, patchMax);
            placed += growPatchFrom(m, rng, c.x, c.y, target, ringWidth, avoidRoad, used, budget - placed);
        }
    }

    private int growPatchFrom(GameMap m, Random rng, int sx, int sy, int target, int ringWidth, boolean avoidRoad, boolean[][] used, int budgetLeft) {

        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        used[sy][sx] = true;

        int grown = 0;
        while (!q.isEmpty() && grown < target && budgetLeft > 0) {
            int idx = rng.nextInt(q.size());
            int[] cur = pickAndRemove(q, idx);
            int x = cur[0], y = cur[1];

            if (canPaintBush(m, x, y, avoidRoad)) {
                paintBush(m, x, y);
                grown++;
                budgetLeft--;
            }

            int[][] dirs = rng.nextBoolean() ? new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}} : new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, -1}, {-1, -1}, {1, 1}, {-1, 1}};

            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                if (nx <= 0 || ny <= 0 || nx >= m.w - 1 || ny >= m.h - 1) continue;
                if (used[ny][nx]) continue;
                if (!candidateAroundTrees(m, nx, ny, ringWidth)) continue;
                if (m.indoor[ny][nx]) continue;
                if (m.tiles[ny][nx] != '▓') continue;
                if (avoidRoad && m.road[ny][nx]) continue;

                used[ny][nx] = true;
                q.add(new int[]{nx, ny});
            }
        }
        return grown;
    }

    private boolean candidateAroundTrees(GameMap m, int x, int y, int ringWidth) {
        for (int dy = -ringWidth; dy <= ringWidth; dy++) {
            for (int dx = -ringWidth; dx <= ringWidth; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx <= 0 || ny <= 0 || nx >= m.w - 1 || ny >= m.h - 1) continue;
                if (m.tiles[ny][nx] == '#') return true;
            }
        }
        return false;
    }

    private boolean canPaintBush(GameMap m, int x, int y, boolean avoidRoad) {
        if (m.indoor[y][x]) return false;
        if (m.tiles[y][x] != '▓') return false;
        if (avoidRoad && m.road[y][x]) return false;
        return true;
    }

    private void paintBush(GameMap m, int x, int y) {
        m.tiles[y][x] = BUSH_CHAR;
        m.walk[y][x] = true;
        m.transp[y][x] = true;
    }

    private static int[] pickAndRemove(ArrayDeque<int[]> dq, int index) {
        int size = dq.size();
        int[][] arr = new int[size][2];
        for (int i = 0; i < size; i++) arr[i] = dq.pollFirst();
        int[] pick = arr[index];
        for (int i = 0; i < size; i++) if (i != index) dq.addLast(arr[i]);
        return pick;
    }

    private static int heavyBetween(Random rng, int a, int b) {
        if (a >= b) return a;
        double u = rng.nextDouble();
        double t = (u < 0.7) ? Math.pow(rng.nextDouble(), 2.2) : rng.nextDouble();
        return a + (int) Math.round(t * (b - a));
    }

    private static double clamp01(double v) {
        return (v < 0) ? 0 : (v > 1 ? 1 : v);
    }
}
