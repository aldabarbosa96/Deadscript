package world.builders.map;

import world.GameMap;
import java.util.*;
import static java.lang.Math.*;

public final class RoadBuilder {
    public static final char ROAD_H = '-';
    public static final char ROAD_V = '¦';
    public static final double DOOR_SAMPLE_RATIO = 0.65;

    private record P(int x, int y) {
    }

    public void build(GameMap m, Random rng) {
        List<P> doorAnchors = findExteriorDoorAnchors(m);
        if (doorAnchors.size() < 2) return;

        List<P> sample = sampleDoors(doorAnchors, rng, DOOR_SAMPLE_RATIO);
        if (sample.size() < 2) return;

        List<int[]> edges = mstEdges(sample);
        for (int[] e : edges) {
            P a = sample.get(e[0]);
            P b = sample.get(e[1]);
            List<P> path = bfsPath(m, a, b);
            if (path == null || path.isEmpty()) continue;
            carveRoad1Wide(m, path);
        }
    }

    private List<P> findExteriorDoorAnchors(GameMap m) {
        int w = m.w, h = m.h;
        List<P> out = new ArrayList<>();
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (m.tiles[y][x] != '+') continue;

                boolean hasIndoor = false, hasOutdoor = false;
                int ox = x, oy = y;
                int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] d : dirs) {
                    int nx = x + d[0], ny = y + d[1];
                    if (!inBounds(m, nx, ny)) continue;
                    if (m.indoor[ny][nx]) hasIndoor = true;
                    else {
                        hasOutdoor = true;
                        if (canLayRoad(m, nx, ny)) {
                            ox = nx;
                            oy = ny;
                        }
                    }
                }
                if (hasIndoor && hasOutdoor && canLayRoad(m, ox, oy)) out.add(new P(ox, oy));
            }
        }
        return out;
    }

    private static boolean inBounds(GameMap m, int x, int y) {
        return x >= 0 && y >= 0 && x < m.w && y < m.h;
    }

    private List<P> sampleDoors(List<P> all, Random rng, double ratio) {
        List<P> copy = new ArrayList<>(all);
        Collections.shuffle(copy, rng);
        int keep = max(2, (int) round(copy.size() * clamp01(ratio)));
        return copy.subList(0, keep);
    }

    private double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private List<int[]> mstEdges(List<P> pts) {
        int n = pts.size();
        boolean[] used = new boolean[n];
        double[] best = new double[n];
        int[] parent = new int[n];
        Arrays.fill(best, Double.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);

        List<int[]> edges = new ArrayList<>();
        used[0] = true;
        for (int i = 1; i < n; i++) {
            best[i] = dist(pts.get(0), pts.get(i));
            parent[i] = 0;
        }
        for (int k = 1; k < n; k++) {
            int u = -1;
            double bd = Double.POSITIVE_INFINITY;
            for (int i = 0; i < n; i++)
                if (!used[i] && best[i] < bd) {
                    bd = best[i];
                    u = i;
                }
            if (u == -1) break;
            used[u] = true;
            edges.add(new int[]{u, parent[u]});
            for (int v = 0; v < n; v++)
                if (!used[v]) {
                    double d = dist(pts.get(u), pts.get(v));
                    if (d < best[v]) {
                        best[v] = d;
                        parent[v] = u;
                    }
                }
        }
        return edges;
    }

    private double dist(P a, P b) {
        int dx = a.x - b.x, dy = a.y - b.y;
        return sqrt(dx * dx + dy * dy);
    }

    private boolean canLayRoad(GameMap m, int x, int y) {
        if (!inBounds(m, x, y)) return false;
        if (m.indoor[y][x]) return false;
        if (m.road[y][x]) return true;
        char c = m.tiles[y][x];
        if (c == '~') return false;
        if (c == '╔' || c == '╗' || c == '╚' || c == '╝' || c == '═' || c == '║' || c == '│' || c == '─' || c == '┼' || c == '├' || c == '┤' || c == '┬' || c == '┴' || c == '┌' || c == '┐' || c == '└' || c == '┘')
            return false;
        return true;
    }

    private List<P> bfsPath(GameMap m, P start, P goal) {
        int w = m.w, h = m.h;
        int[][] prev = new int[h][w];
        for (int[] row : prev) Arrays.fill(row, -1);
        ArrayDeque<P> q = new ArrayDeque<>();
        q.add(start);
        prev[start.y][start.x] = start.y * w + start.x;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // 4-dir: sin diagonales
        while (!q.isEmpty()) {
            P p = q.poll();
            if (p.x == goal.x && p.y == goal.y) break;
            for (int[] d : dirs) {
                int nx = p.x + d[0], ny = p.y + d[1];
                if (!inBounds(m, nx, ny)) continue;
                if (prev[ny][nx] != -1) continue;
                if (!canLayRoad(m, nx, ny)) continue;
                prev[ny][nx] = p.y * w + p.x;
                q.add(new P(nx, ny));
            }
        }
        if (prev[goal.y][goal.x] == -1) return null;

        ArrayList<P> path = new ArrayList<>();
        int cur = goal.y * w + goal.x;
        while (true) {
            int cx = cur % w, cy = cur / w;
            path.add(new P(cx, cy));
            if (cur == prev[cy][cx]) break;
            cur = prev[cy][cx];
        }
        Collections.reverse(path);
        return path;
    }

    // === 1 de ancho; selecciona glyph por orientación del segmento ===
    private void carveRoad1Wide(GameMap m, List<P> path) {
        for (int i = 0; i < path.size(); i++) {
            P cur = path.get(i);
            P prev = (i > 0) ? path.get(i - 1) : null;
            P next = (i + 1 < path.size()) ? path.get(i + 1) : null;
            char glyph = chooseGlyph(prev, cur, next);
            stamp(m, cur.x, cur.y, glyph);
        }
    }

    private char chooseGlyph(P prev, P cur, P next) {
        boolean N = false, S = false, E = false, W = false;

        if (prev != null) {
            if (prev.x < cur.x) W = true; else if (prev.x > cur.x) E = true;
            if (prev.y < cur.y) N = true; else if (prev.y > cur.y) S = true;
        }
        if (next != null) {
            if (next.x < cur.x) W = true; else if (next.x > cur.x) E = true;
            if (next.y < cur.y) N = true; else if (next.y > cur.y) S = true;
        }

        if ((E || W) && (N || S)) {
            if (E && N) return '└';
            if (E && S) return '┌';
            if (W && N) return '┘';
            if (W && S) return '┐';
        }

        if (E || W) return ROAD_H;
        if (N || S) return ROAD_V;

        if (next != null) {
            if (next.y == cur.y) return ROAD_H;
            if (next.x == cur.x) return ROAD_V;
        }
        if (prev != null) {
            if (cur.y == prev.y) return ROAD_H;
            if (cur.x == prev.x) return ROAD_V;
        }
        return ROAD_H;
    }

    private void stamp(GameMap m, int x, int y, char glyph) {
        if (!inBounds(m, x, y)) return;
        if (m.indoor[y][x]) return;
        char c = m.tiles[y][x];
        if (c == '╔' || c == '╗' || c == '╚' || c == '╝' || c == '═' || c == '║' || c == '│' || c == '─' || c == '┼' || c == '├' || c == '┤' || c == '┬' || c == '┴' || c == '┌' || c == '┐' || c == '└' || c == '┘' || c == '~')
            return;

        m.tiles[y][x] = glyph;
        m.walk[y][x] = true;
        m.transp[y][x] = true;
        m.indoor[y][x] = false;
        m.road[y][x] = true;
    }
}
