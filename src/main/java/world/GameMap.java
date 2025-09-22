package world;

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
        int area = w * h;
        int groups = Math.max(12, area / 16000) + rng.nextInt(6);
        int singles = Math.max(22, area / 12000) + rng.nextInt(10);
        addRiver(m, rng, cx, cy, safeRadius);
        addCabinClusters(m, rng, groups, safeRadius);
        addCabins(m, rng, singles, safeRadius);
        punchDoorsBetweenTouchingInteriors(m, rng);
        addRocks(m, rng, Math.max(8, (w * h) / 270), Math.max(12, (w * h) / 200), 1, 7, safeRadius);

        return m;
    }

    public static final class Stair {
        public final int x, y;
        public Link up;   // opcional
        public Link down; // opcional

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

    private static void setWater(GameMap m, int x, int y) {
        m.tiles[y][x] = '~';
        m.walk[y][x] = false;
        m.transp[y][x] = true;
    }

    private static void setRock(GameMap m, int x, int y) {
        m.tiles[y][x] = '█';
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
    private static void addRocks(GameMap m, Random rng, int minGroups, int maxGroups, int sizeMin, int sizeMax, int safeRadius) {
        // saneo parámetros
        sizeMin = Math.max(1, sizeMin);
        sizeMax = Math.max(sizeMin, sizeMax);
        minGroups = Math.max(1, minGroups);
        maxGroups = Math.max(minGroups, maxGroups);

        int groups = minGroups + rng.nextInt(maxGroups - minGroups + 1);
        int placedGroups = 0;
        int attempts = groups * 30; // margen de intentos
        int margin = 1;
        int cx = m.w / 2, cy = m.h / 2;
        int safe2 = (safeRadius + 1) * (safeRadius + 1);

        while (placedGroups < groups && attempts-- > 0) {
            // semilla en interior, sobre suelo libre
            int x = margin + rng.nextInt(Math.max(1, m.w - 2 * margin));
            int y = margin + rng.nextInt(Math.max(1, m.h - 2 * margin));
            if (!inInterior(m, x, y, margin)) continue;
            if (m.tiles[y][x] != '▓') continue;       // no agua/árbol/pared
            if (m.indoor[y][x]) continue;             // no interiores
            if (dist2(x, y, cx, cy) <= safe2) continue;

            int target = sizeMin + rng.nextInt(sizeMax - sizeMin + 1);
            int got = sprinkleRockMicroBlob(m, rng, x, y, target, cx, cy, safe2);
            if (got > 0) placedGroups++;
        }

        // fallback mínimo por si acaso
        if (placedGroups == 0) {
            for (int y = 1; y < m.h - 1; y++)
                for (int x = 1; x < m.w - 1; x++) {
                    if (m.tiles[y][x] == '▓' && !m.indoor[y][x] && dist2(x, y, cx, cy) > safe2) {
                        setRock(m, x, y);
                        return;
                    }
                }
        }
    }

    private static int sprinkleRockMicroBlob(GameMap m, Random rng, int sx, int sy, int target, int cx, int cy, int safe2) {
        int placed = 0, steps = 0;
        ArrayDeque<int[]> q = new ArrayDeque<>();
        boolean[][] seen = new boolean[m.h][m.w];
        q.add(new int[]{sx, sy});
        seen[sy][sx] = true;

        // 8 direcciones para formar grupitos compactos
        int[][] d8 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

        while (!q.isEmpty() && placed < target && steps < target * 12) {
            steps++;
            int[] cur = q.pollFirst();
            int x = cur[0], y = cur[1];

            if (m.tiles[y][x] == '▓' && !m.indoor[y][x] && dist2(x, y, cx, cy) > safe2) {
                setRock(m, x, y);
                placed++;
            }

            // expansión moderada (≈40%) para mantener grupos pequeños y orgánicos
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


    // cabañas
    private static void addCabins(GameMap m, Random rng, int count, int safeRadius) {
        int target = Math.max(1, count);
        int tries = target * 60;
        int placed = 0;

        while (placed < target && tries-- > 0) {
            int wCab = 6 + rng.nextInt(9);
            int hCab = 4 + rng.nextInt(8);
            int x0 = 2 + rng.nextInt(Math.max(1, m.w - 2 - wCab - 2));
            int y0 = 2 + rng.nextInt(Math.max(1, m.h - 2 - hCab - 2));
            int x1 = x0 + wCab - 1, y1 = y0 + hCab - 1;

            int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) continue;

            // Requisito: sin agua en un halo 1-tile alrededor
            if (!areaClearOfWater(m, x0 - 1, y0 - 1, x1 + 1, y1 + 1)) continue;

            buildCabin(m, x0, y0, x1, y1);
            m.maybePlaceStairsInHouse(rng, new RectI(x0, y0, x1, y1));
            placed++;
        }

        // Fallback determinista: si por cualquier motivo no se colocó ninguna, forzamos 1
        if (placed == 0) {
            outer:
            for (int hCab = 5; hCab <= 12; hCab++) {
                for (int wCab = 8; wCab <= 16; wCab++) {
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

    private static void drawCabinShell(GameMap m, int x0, int y0, int x1, int y1) {
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
    }

    private static boolean areaBuildableForCabin(GameMap m, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!inBounds(m, x, y)) return false;
                char t = m.tiles[y][x];
                if (t == '~' || t == '█') return false; // agua/roca
                if (t == '╔' || t == '╗' || t == '╚' || t == '╝' || t == '═' || t == '║' || t == '+')
                    return false; // ya hay casa
            }
        }
        return true;
    }

    private static final class RectI {
        final int x0, y0, x1, y1;

        RectI(int x0, int y0, int x1, int y1) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }

        int w() {
            return x1 - x0 + 1;
        }

        int h() {
            return y1 - y0 + 1;
        }
    }

    private static boolean areaBuildableForAttachment(GameMap m, int x0, int y0, int x1, int y1, int side) {
        for (int y = y0 - 1; y <= y1 + 1; y++) {
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                if (!inBounds(m, x, y)) return false;

                char t = m.tiles[y][x];
                if (t == '~' || t == '█') return false; // nunca sobre agua/roca

                boolean inRect = (x >= x0 && x <= x1 && y >= y0 && y <= y1);

                // halo adyacente al lado COMPARTIDO (permitido)
                boolean sharedHalo = (side == 0 && x == x0 - 1) ||   // derecha → halo a la izquierda del nuevo
                        (side == 1 && x == x1 + 1) ||   // izquierda → halo a la derecha del nuevo
                        (side == 2 && y == y0 - 1) ||   // abajo → halo arriba del nuevo
                        (side == 3 && y == y1 + 1);     // arriba → halo abajo del nuevo
                if (!inRect && sharedHalo) continue;

                boolean wallish = (t == '╔' || t == '╗' || t == '╚' || t == '╝' || t == '═' || t == '║' || t == '+');

                // dentro del rectángulo no podemos pisar paredes/puertas existentes
                if (inRect && wallish) return false;

                // en el resto del halo (no compartido) no podemos tocar interiores/paredes
                if (!inRect && (wallish || m.indoor[y][x])) return false;
            }
        }
        return true;
    }


    private static RectI boundsOf(java.util.List<RectI> rs) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (RectI r : rs) {
            x0 = Math.min(x0, r.x0);
            y0 = Math.min(y0, r.y0);
            x1 = Math.max(x1, r.x1);
            y1 = Math.max(y1, r.y1);
        }
        return new RectI(x0, y0, x1, y1);
    }

    // --- NUEVO: abre una puerta entre dos módulos adyacentes que comparten pared ---
    private static void openSharedDoor(GameMap m, RectI a, RectI b, Random rng) {
        // vertical compartida
        if (a.x1 == b.x0 || b.x1 == a.x0) {
            int x = (a.x1 == b.x0) ? a.x1 : b.x1;
            int yStart = Math.max(a.y0 + 1, b.y0 + 1);
            int yEnd = Math.min(a.y1 - 1, b.y1 - 1);
            if (yStart <= yEnd) {
                int y = yStart + rng.nextInt(Math.max(1, yEnd - yStart + 1));
                setDoor(m, x, y);
                // asegura suelo a ambos lados
                if (inBounds(m, x - 1, y)) {
                    setFloor(m, x - 1, y);
                    m.indoor[y][x - 1] = true;
                }
                if (inBounds(m, x + 1, y)) {
                    setFloor(m, x + 1, y);
                    m.indoor[y][x + 1] = true;
                }
            }
        }
        // horizontal compartida
        if (a.y1 == b.y0 || b.y1 == a.y0) {
            int y = (a.y1 == b.y0) ? a.y1 : b.y1;
            int xStart = Math.max(a.x0 + 1, b.x0 + 1);
            int xEnd = Math.min(a.x1 - 1, b.x1 - 1);
            if (xStart <= xEnd) {
                int x = xStart + rng.nextInt(Math.max(1, xEnd - xStart + 1));
                setDoor(m, x, y);
                if (inBounds(m, x, y - 1)) {
                    setFloor(m, x, y - 1);
                    m.indoor[y - 1][x] = true;
                }
                if (inBounds(m, x, y + 1)) {
                    setFloor(m, x, y + 1);
                    m.indoor[y + 1][x] = true;
                }
            }
        }
    }

    // --- NUEVO: añade 1–2 puertas exteriores en la envolvente de la casa compuesta ---
    private static void addExteriorDoorsOnBounding(GameMap m, java.util.List<RectI> rooms, Random rng) {
        RectI bb = boundsOf(rooms);
        int doors = 1 + rng.nextInt(2);
        for (int d = 0; d < doors; d++) {
            int side = rng.nextInt(4); // 0 top,1 right,2 bottom,3 left
            switch (side) {
                case 0 -> {
                    if (bb.w() >= 4) {
                        int x = bb.x0 + 2 + rng.nextInt(Math.max(1, bb.w() - 3));
                        int y = bb.y0;
                        setDoor(m, x, y);
                        if (inBounds(m, x, y + 1)) {
                            setFloor(m, x, y + 1);
                            m.indoor[y + 1][x] = true;
                        }
                    }
                }
                case 1 -> {
                    if (bb.h() >= 4) {
                        int x = bb.x1;
                        int y = bb.y0 + 2 + rng.nextInt(Math.max(1, bb.h() - 3));
                        setDoor(m, x, y);
                        if (inBounds(m, x - 1, y)) {
                            setFloor(m, x - 1, y);
                            m.indoor[y][x - 1] = true;
                        }
                    }
                }
                case 2 -> {
                    if (bb.w() >= 4) {
                        int x = bb.x0 + 2 + rng.nextInt(Math.max(1, bb.w() - 3));
                        int y = bb.y1;
                        setDoor(m, x, y);
                        if (inBounds(m, x, y - 1)) {
                            setFloor(m, x, y - 1);
                            m.indoor[y - 1][x] = true;
                        }
                    }
                }
                default -> {
                    if (bb.h() >= 4) {
                        int x = bb.x0;
                        int y = bb.y0 + 2 + rng.nextInt(Math.max(1, bb.h() - 3));
                        setDoor(m, x, y);
                        if (inBounds(m, x + 1, y)) {
                            setFloor(m, x + 1, y);
                            m.indoor[y][x + 1] = true;
                        }
                    }
                }
            }
        }
    }

    private static boolean tryAttachModule(GameMap m, Random rng, java.util.List<RectI> rooms, int safeRadius) {
        RectI base = rooms.get(rng.nextInt(rooms.size()));

        // módulos más grandes
        int wCab = 8 + rng.nextInt(9);  // 8..16
        int hCab = 5 + rng.nextInt(8);  // 5..12

        int side = rng.nextInt(4);
        int minOverlap = 3;

        // más reintentos para encontrar hueco
        for (int tries = 0; tries < 12; tries++) {
            int x0, y0, x1, y1;

            if (side == 0) { // derecha (comparte x0 con base.x1)
                x0 = base.x1;
                x1 = x0 + wCab - 1;
                int yTop = Math.max(2, base.y0 - hCab + minOverlap);
                int yBot = Math.min(m.h - 3 - hCab, base.y1 - minOverlap + 1);
                if (yTop > yBot) {
                    side = (side + 1) % 4;
                    continue;
                }
                y0 = yTop + rng.nextInt(Math.max(1, yBot - yTop + 1));
                y1 = y0 + hCab - 1;

                int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) {
                    side = (side + 1) % 4;
                    continue;
                }
                if (!areaBuildableForAttachment(m, x0, y0, x1, y1, 0)) {
                    side = (side + 1) % 4;
                    continue;
                }

            } else if (side == 1) { // izquierda (comparte x1 con base.x0)
                x1 = base.x0;
                x0 = x1 - wCab + 1;
                int yTop = Math.max(2, base.y0 - hCab + minOverlap);
                int yBot = Math.min(m.h - 3 - hCab, base.y1 - minOverlap + 1);
                if (yTop > yBot) {
                    side = (side + 1) % 4;
                    continue;
                }
                y0 = yTop + rng.nextInt(Math.max(1, yBot - yTop + 1));
                y1 = y0 + hCab - 1;

                int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) {
                    side = (side + 1) % 4;
                    continue;
                }
                if (!areaBuildableForAttachment(m, x0, y0, x1, y1, 1)) {
                    side = (side + 1) % 4;
                    continue;
                }

            } else if (side == 2) { // abajo (comparte y0 con base.y1)
                y0 = base.y1;
                y1 = y0 + hCab - 1;
                int xLeft = Math.max(2, base.x0 - wCab + minOverlap);
                int xRight = Math.min(m.w - 3 - wCab, base.x1 - minOverlap + 1);
                if (xLeft > xRight) {
                    side = (side + 1) % 4;
                    continue;
                }
                x0 = xLeft + rng.nextInt(Math.max(1, xRight - xLeft + 1));
                x1 = x0 + wCab - 1;

                int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) {
                    side = (side + 1) % 4;
                    continue;
                }
                if (!areaBuildableForAttachment(m, x0, y0, x1, y1, 2)) {
                    side = (side + 1) % 4;
                    continue;
                }

            } else { // arriba (comparte y1 con base.y0)
                y1 = base.y0;
                y0 = y1 - hCab + 1;
                int xLeft = Math.max(2, base.x0 - wCab + minOverlap);
                int xRight = Math.min(m.w - 3 - wCab, base.x1 - minOverlap + 1);
                if (xLeft > xRight) {
                    side = (side + 1) % 4;
                    continue;
                }
                x0 = xLeft + rng.nextInt(Math.max(1, xRight - xLeft + 1));
                x1 = x0 + wCab - 1;

                int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) {
                    side = (side + 1) % 4;
                    continue;
                }
                if (!areaBuildableForAttachment(m, x0, y0, x1, y1, 3)) {
                    side = (side + 1) % 4;
                    continue;
                }
            }

            // Dibuja y conecta
            drawCabinShell(m, x0, y0, x1, y1);
            RectI neo = new RectI(x0, y0, x1, y1);
            rooms.add(neo);
            openSharedDoor(m, base, neo, rng);
            return true;
        }
        return false;
    }


    private static void addCabinClusters(GameMap m, Random rng, int groups, int safeRadius) {
        int placed = 0;
        int attempts = groups * 40;

        while (placed < groups && attempts-- > 0) {
            int modules = 2 + rng.nextInt(5);

            int wCab = 6 + rng.nextInt(6);
            int hCab = 4 + rng.nextInt(5);
            int x0 = 2 + rng.nextInt(Math.max(1, m.w - 2 - wCab - 2));
            int y0 = 2 + rng.nextInt(Math.max(1, m.h - 2 - hCab - 2));
            int x1 = x0 + wCab - 1, y1 = y0 + hCab - 1;

            int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) continue;
            if (!areaBuildableForCabin(m, x0 - 1, y0 - 1, x1 + 1, y1 + 1)) continue;

            java.util.ArrayList<RectI> rooms = new java.util.ArrayList<>();
            drawCabinShell(m, x0, y0, x1, y1);
            RectI first = new RectI(x0, y0, x1, y1);
            rooms.add(first);

            for (int k = 1; k < modules; k++) {
                if (!tryAttachModule(m, rng, rooms, safeRadius)) break;
            }

            addExteriorDoorsOnBounding(m, rooms, rng);
            RectI bb = boundsOf(rooms);
            m.maybePlaceStairsInHouse(rng, bb);
            placed++;
        }
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

    public static void drawRectHouseShell(GameMap m, int x0, int y0, int x1, int y1) {
        for (int y = y0 + 1; y <= y1 - 1; y++) {
            for (int x = x0 + 1; x <= x1 - 1; x++) {
                setFloor(m, x, y);
                m.indoor[y][x] = true;
            }
        }
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
        // Puerta exterior simple (opcional)
        setDoor(m, (x0 + x1) / 2, y1);
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

    private static void punchDoorsBetweenTouchingInteriors(GameMap m, Random rng) {
        // Segmentos verticales (pared '║'): interiores a izquierda y derecha
        for (int x = 1; x < m.w - 1; x++) {
            int y = 1;
            while (y < m.h - 1) {
                if (m.tiles[y][x] == '║' && m.indoor[y][x - 1] && m.indoor[y][x + 1]) {
                    int y0 = y;
                    while (y < m.h - 1 && m.tiles[y][x] == '║' && m.indoor[y][x - 1] && m.indoor[y][x + 1]) y++;
                    int y1 = y - 1;
                    int yy = y0 + rng.nextInt(y1 - y0 + 1);
                    setDoor(m, x, yy);
                } else {
                    y++;
                }
            }
        }
        // Segmentos horizontales (pared '═'): interiores arriba y abajo
        for (int y = 1; y < m.h - 1; y++) {
            int x = 1;
            while (x < m.w - 1) {
                if (m.tiles[y][x] == '═' && m.indoor[y - 1][x] && m.indoor[y + 1][x]) {
                    int x0 = x;
                    while (x < m.w - 1 && m.tiles[y][x] == '═' && m.indoor[y - 1][x] && m.indoor[y + 1][x]) x++;
                    int x1 = x - 1;
                    int xx = x0 + rng.nextInt(x1 - x0 + 1);
                    setDoor(m, xx, y);
                } else {
                    x++;
                }
            }
        }
    }

    private static void buildCabin(GameMap m, int x0, int y0, int x1, int y1) {
        drawCabinShell(m, x0, y0, x1, y1);
        if ((x1 - x0 + 1) >= (y1 - y0 + 1)) setDoor(m, (x0 + x1) / 2, y1);
        else setDoor(m, x1, (y0 + y1) / 2);
    }

    private void maybePlaceStairsInHouse(Random rng, RectI bb) {
        if (rng == null) return;
        // 35% de probabilidad de poner escaleras en esta casa
        if (rng.nextDouble() > 0.35) return;

        // Elige una casilla interior cercana al centro que sea suelo interior '.'
        int cx = (bb.x0 + bb.x1) / 2, cy = (bb.y0 + bb.y1) / 2;
        int bestX = -1, bestY = -1, bestD2 = Integer.MAX_VALUE;
        for (int y = bb.y0 + 1; y <= bb.y1 - 1; y++) {
            for (int x = bb.x0 + 1; x <= bb.x1 - 1; x++) {
                if (tiles[y][x] == '▓' && indoor[y][x]) {
                    int d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }
        if (bestX < 0) return; // no hay interior utilizable

        // Coloca escalera en la planta actual
        placeStair(bestX, bestY);

        boolean makeUp = rng.nextBoolean();          // ~50% segunda planta
        boolean makeDown = rng.nextDouble() < 0.45;    // ~45% sótano

        if (!makeUp && !makeDown) {
            // elige una de forma aleatoria (o fija sótano si prefieres)
            makeDown = rng.nextBoolean() ? true : false;
            makeUp = !makeDown;
        }

        int interiorW = bb.x1 - bb.x0 - 1;
        int interiorH = bb.y1 - bb.y0 - 1;
        int relX = bestX - (bb.x0 + 1);
        int relY = bestY - (bb.y0 + 1);

        if (makeUp) {
            GameMap up = makeUpperFloorVariant(interiorW, interiorH, rng);
            int tx = 1 + Math.max(0, Math.min(relX, up.w - 2));
            int ty = 1 + Math.max(0, Math.min(relY, up.h - 2));
            up.placeStair(tx, ty);
            up.linkStairDown(tx, ty, this, bestX, bestY); // desde arriba puedes bajar
            linkStairUp(bestX, bestY, up, tx, ty);        // desde aquí puedes subir
        }
        if (makeDown) {
            GameMap down = makeBasement(interiorW, interiorH, rng);
            int tx = 1 + Math.max(0, Math.min(relX, down.w - 2));
            int ty = 1 + Math.max(0, Math.min(relY, down.h - 2));
            down.placeStair(tx, ty);
            down.linkStairUp(tx, ty, this, bestX, bestY); // desde abajo puedes subir
            linkStairDown(bestX, bestY, down, tx, ty);    // desde aquí puedes bajar
        }
    }

    public static GameMap makeUpperFloorVariant(int baseInteriorW, int baseInteriorH, java.util.Random rng) {
        int shrinkW = Math.min(3, Math.max(0, baseInteriorW - 3));
        int shrinkH = Math.min(3, Math.max(0, baseInteriorH - 3));
        int w = Math.max(3, baseInteriorW - (shrinkW == 0 ? 1 : 1 + rng.nextInt(shrinkW)));
        int h = Math.max(3, baseInteriorH - (shrinkH == 0 ? 1 : 1 + rng.nextInt(shrinkH)));

        GameMap m = new GameMap(w + 2, h + 2);

        // Base suelo
        for (int y = 0; y < m.h; y++)
            for (int x = 0; x < m.w; x++)
                setFloor(m, x, y);

        // Envolvente + interior SIN puerta exterior
        drawCabinShell(m, 0, 0, m.w - 1, m.h - 1);

        // Tabique vertical con hueco (sin puerta)
        if (w >= 6) {
            int vx = 1 + w / 2 + (rng.nextBoolean() ? -1 : 1) * (rng.nextInt(Math.max(1, w / 4)));
            vx = Math.max(2, Math.min(m.w - 3, vx));
            for (int y = 1; y <= h; y++) setCabinWall(m, vx, y, '║');
            int dy = 1 + rng.nextInt(Math.max(1, h));
            setFloor(m, vx, dy);
            m.indoor[dy][vx] = true;
        }

        // Tabique horizontal opcional con hueco (sin puerta)
        if (h >= 5 && rng.nextBoolean()) {
            int hy = 1 + h / 2 + (rng.nextBoolean() ? -1 : 1) * (rng.nextInt(Math.max(1, h / 4)));
            hy = Math.max(2, Math.min(m.h - 3, hy));
            for (int x = 1; x <= w; x++) setCabinWall(m, x, hy, '═');
            int dx = 1 + rng.nextInt(Math.max(1, w));
            setFloor(m, dx, hy);
            m.indoor[hy][dx] = true;
        }

        return m;
    }

    public static GameMap makeBasement(int baseInteriorW, int baseInteriorH, java.util.Random rng) {
        // Sótano más grande tipo garaje
        int addW = 2 + rng.nextInt(4);  // +2..+5
        int addH = 1 + rng.nextInt(3);  // +1..+3
        int w = Math.max(4, baseInteriorW + addW);
        int h = Math.max(4, baseInteriorH + addH);

        GameMap m = new GameMap(w + 2, h + 2);
        for (int y = 0; y < m.h; y++)
            for (int x = 0; x < m.w; x++)
                setFloor(m, x, y);

        // Caja grande
        drawRectHouseShell(m, 0, 0, m.w - 1, m.h - 1);

        // “Pilares” para ambientar (rocas como columnas)
        int pillars = Math.max(2, (w * h) / 80);
        for (int i = 0; i < pillars; i++) {
            int x = 2 + rng.nextInt(Math.max(1, w - 2));
            int y = 2 + rng.nextInt(Math.max(1, h - 2));
            setRock(m, x, y);
        }

        return m;
    }
}
