package world;

import java.util.*;

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

        // bosque (blobs)
        int interiorArea = (w - 2) * (h - 2);
        int targetTrees = (int) Math.round(coverageTarget * interiorArea);
        int placedTrees = 0;

        int minDist2 = minClusterDist * minClusterDist;
        List<int[]> seeds = new ArrayList<>();
        int attempts = 0, maxAttempts = interiorArea * 5;
        int margin = 2;

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

        // decoración
        addRiver(m, rng, cx, cy, safeRadius);
        addRockClusters(m, rng, 10, 28, safeRadius);
        addCabins(m, rng, 2 + rng.nextInt(3), safeRadius);

        return m;
    }

    // tiles
    private static void setTree(GameMap m, int x, int y) {
        m.tiles[y][x] = '#';
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }

    private static void setFloor(GameMap m, int x, int y) {
        m.tiles[y][x] = '.';
        m.walk[y][x] = true;
        m.transp[y][x] = true;
    }

    private static void setWater(GameMap m, int x, int y) {
        m.tiles[y][x] = '~';
        m.walk[y][x] = false;
        m.transp[y][x] = true;
    }

    private static void setRock(GameMap m, int x, int y) {
        m.tiles[y][x] = '^';
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }

    private static void setCabinWall(GameMap m, int x, int y, char ch) {
        m.tiles[y][x] = ch;
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }

    private static void setDoor(GameMap m, int x, int y) {
        m.tiles[y][x] = '+';
        m.walk[y][x] = true;
        m.transp[y][x] = true;
    }

    // util
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
        int r2 = r * r, x0 = Math.max(1, cx - r), x1 = Math.min(m.w - 2, cx + r), y0 = Math.max(1, cy - r), y1 = Math.min(m.h - 2, cy + r);
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) if (dist2(x, y, cx, cy) <= r2) setFloor(m, x, y);
    }

    // bosque
    private static int growTreeBlob(GameMap m, Random rng, int sx, int sy, int targetSize, int margin, int cx, int cy, int safeRadius, int budget) {
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
                setTree(m, x, y);
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

    private static int fillHolesRegionWithTrees(GameMap m, int x0, int y0, int x1, int y1, int budget) {
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
                setTree(m, x, y);
                if (++filled >= budget) return filled;
            }
        }
        return filled;
    }

    // río
    private static void addRiver(GameMap m, Random rng, int cx, int cy, int safeRadius) {
        if (m.w < 40 || m.h < 30) return;
        boolean horizontal = rng.nextBoolean();
        double amp = (horizontal ? m.h : m.w) * (0.08 + rng.nextDouble() * 0.06);
        double freq = (0.015 + rng.nextDouble() * 0.015);
        double phase = rng.nextDouble() * Math.PI * 2.0;
        int base = horizontal ? (int) (m.h * (0.25 + rng.nextDouble() * 0.5)) : (int) (m.w * (0.25 + rng.nextDouble() * 0.5));
        int halfW = 2 + rng.nextInt(3);

        if (horizontal) {
            for (int x = 1; x < m.w - 1; x++) {
                int yC = base + (int) Math.round(Math.sin(x * freq + phase) * amp);
                for (int dy = -halfW; dy <= halfW; dy++) {
                    int y = yC + dy;
                    if (y <= 0 || y >= m.h - 1) continue;
                    setWater(m, x, y);
                }
            }
        } else {
            for (int y = 1; y < m.h - 1; y++) {
                int xC = base + (int) Math.round(Math.sin(y * freq + phase) * amp);
                for (int dx = -halfW; dx <= halfW; dx++) {
                    int x = xC + dx;
                    if (x <= 0 || x >= m.w - 1) continue;
                    setWater(m, x, y);
                }
            }
        }
        // ribera
        for (int y = 1; y < m.h - 1; y++)
            for (int x = 1; x < m.w - 1; x++) {
                if (m.tiles[y][x] == '~') {
                    for (int yy = y - 1; yy <= y + 1; yy++)
                        for (int xx = x - 1; xx <= x + 1; xx++)
                            if (m.tiles[yy][xx] == '#') setFloor(m, xx, yy);
                }
            }
        carveDisk(m, cx, cy, safeRadius);
    }

    // rocas
    private static void addRockClusters(GameMap m, Random rng, int minC, int maxC, int safeRadius) {
        int want = minC + rng.nextInt(Math.max(1, maxC - minC + 1));

        int placedClusters = 0;
        int attempts = 0;
        int maxAttempts = want * 50; // margen generoso

        while (placedClusters < want && attempts++ < maxAttempts) {
            int rx = 2 + rng.nextInt(Math.max(1, m.w - 4));
            int ry = 2 + rng.nextInt(Math.max(1, m.h - 4));

            // Evitar centro seguro y agua en el seed
            if (dist2(rx, ry, m.w / 2, m.h / 2) <= (safeRadius + 2) * (safeRadius + 2)) continue;
            if (m.tiles[ry][rx] == '~') continue;

            int size = 6 + rng.nextInt(18);
            int rocksPlaced = 0;

            for (int k = 0; k < size; k++) {
                int x = rx + rng.nextInt(5) - 2;
                int y = ry + rng.nextInt(5) - 2;
                if (!inBounds(m, x, y)) continue;
                if (m.tiles[y][x] == '~') continue;
                if (dist2(x, y, m.w / 2, m.h / 2) <= (safeRadius + 2) * (safeRadius + 2)) continue;

                setRock(m, x, y);
                rocksPlaced++;
            }

            // Solo contamos clústeres efectivos
            if (rocksPlaced >= 3) placedClusters++;
        }

        // Fallback mínimo si no quedó ninguna roca (hiper raro, pero garantizamos)
        if (placedClusters == 0) {
            for (int y = 2; y < m.h - 2; y++) {
                for (int x = 2; x < m.w - 2; x++) {
                    if (m.tiles[y][x] != '~' && dist2(x, y, m.w / 2, m.h / 2) > (safeRadius + 2) * (safeRadius + 2)) {
                        setRock(m, x, y);
                        if (inBounds(m, x + 1, y) && m.tiles[y][x + 1] != '~') setRock(m, x + 1, y);
                        if (inBounds(m, x, y + 1) && m.tiles[y + 1][x] != '~') setRock(m, x, y + 1);
                        return;
                    }
                }
            }
        }
    }

    // cabañas
    private static void addCabins(GameMap m, Random rng, int count, int safeRadius) {
        // Intento aleatorio con más margen de reintentos
        int target = Math.max(1, count); // garantizamos al menos 1 en el diseño deseado
        int tries = target * 60;         // sube margen de reintentos
        int placed = 0;

        while (placed < target && tries-- > 0) {
            int wCab = 6 + rng.nextInt(6);  // 6..11
            int hCab = 4 + rng.nextInt(5);  // 4..8
            int x0 = 2 + rng.nextInt(Math.max(1, m.w - 2 - wCab - 2));
            int y0 = 2 + rng.nextInt(Math.max(1, m.h - 2 - hCab - 2));
            int x1 = x0 + wCab - 1, y1 = y0 + hCab - 1;

            int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) continue;

            // Requisito: sin agua en un halo 1-tile alrededor
            if (!areaClearOfWater(m, x0 - 1, y0 - 1, x1 + 1, y1 + 1)) continue;

            buildCabin(m, x0, y0, x1, y1);
            placed++;
        }

        // Fallback determinista: si por cualquier motivo no se colocó ninguna, forzamos 1
        if (placed == 0) {
            outer:
            for (int hCab = 4; hCab <= 8; hCab++) {
                for (int wCab = 6; wCab <= 11; wCab++) {
                    for (int y0 = 2; y0 <= m.h - 2 - hCab; y0++) {
                        for (int x0 = 2; x0 <= m.w - 2 - wCab; x0++) {
                            int x1 = x0 + wCab - 1, y1 = y0 + hCab - 1;
                            int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                            if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) continue;
                            if (!areaClearOfWater(m, x0 - 1, y0 - 1, x1 + 1, y1 + 1)) continue;

                            buildCabin(m, x0, y0, x1, y1);
                            break outer;
                        }
                    }
                }
            }
        }
    }

    // helpers
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
        double u = rng.nextDouble(), t = (u < 0.2) ? Math.pow(rng.nextDouble(), 2.4) : (u < 0.85) ? rng.nextDouble() : 1.0 - Math.pow(rng.nextDouble(), 2.0);
        int v = a + (int) Math.round(t * (b - a));
        return Math.min(b, Math.max(a, v));
    }
    private static boolean areaClearOfWater(GameMap m, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!inBounds(m, x, y) || m.tiles[y][x] == '~') return false;
            }
        }
        return true;
    }

    private static void buildCabin(GameMap m, int x0, int y0, int x1, int y1) {
        // Interior
        for (int y = y0 + 1; y <= y1 - 1; y++) {
            for (int x = x0 + 1; x <= x1 - 1; x++) {
                setFloor(m, x, y);
                m.indoor[y][x] = true;
            }
        }
        // Paredes
        setCabinWall(m, x0, y0, '╔');
        setCabinWall(m, x1, y0, '╗');
        setCabinWall(m, x0, y1, '╚');
        setCabinWall(m, x1, y1, '╝');
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            setCabinWall(m, x, y0, '═');
            setCabinWall(m, x, y1, '═');
        }
        for (int y = y0 + 1; y <= y1 - 1; y++) {
            setCabinWall(m, x0, y, '║');
            setCabinWall(m, x1, y, '║');
        }
        // Puerta
        if ((x1 - x0 + 1) >= (y1 - y0 + 1)) setDoor(m, (x0 + x1) / 2, y1);
        else setDoor(m, x1, (y0 + y1) / 2);
    }
}
