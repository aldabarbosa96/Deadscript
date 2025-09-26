package world.builders.map;

import world.GameMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WindowBuilder {
    private WindowBuilder() {
    }

    // Glifo de ventana
    public static final char WINDOW = '"';

    // Pinta ventana: no caminable, transparente a la visión
    public static void paintWindow(GameMap m, int x, int y) {
        if (!inBounds(m, x, y)) return;
        m.tiles[y][x] = WINDOW;
        m.walk[y][x] = false;
        m.transp[y][x] = true;
    }

    public static void placeWindowsOnExteriorWallsInArea(GameMap m, Random rng, int x0, int y0, int x1, int y1, int min, int max) {
        if (rng == null) return;
        if (x0 > x1 || y0 > y1) return;

        List<int[]> cands = new ArrayList<>();

        for (int y = Math.max(0, y0); y <= Math.min(m.h - 1, y1); y++) {
            for (int x = Math.max(0, x0); x <= Math.min(m.w - 1, x1); x++) {
                char t = m.tiles[y][x];
                if (!isWallGlyph(t)) continue;                    // sólo sobre pared
                if (isCornerGlyph(t)) continue;                   // fuera esquinas
                if (nearDoorOrWindow(m, x, y)) continue;          // evita puerta/ventana contiguas

                boolean ok = false;
                // Arriba/abajo
                if (inBounds(m, x, y - 1) && inBounds(m, x, y + 1)) {
                    boolean inUp = m.indoor[y - 1][x];
                    boolean inDn = m.indoor[y + 1][x];
                    if (inUp ^ inDn) {
                        // interior por un lado, no-indoor por el otro
                        ok = true;
                    }
                }
                // Izq/der
                if (!ok && inBounds(m, x - 1, y) && inBounds(m, x + 1, y)) {
                    boolean inLf = m.indoor[y][x - 1];
                    boolean inRt = m.indoor[y][x + 1];
                    if (inLf ^ inRt) {
                        ok = true;
                    }
                }
                if (!ok) continue;

                cands.add(new int[]{x, y});
            }
        }

        if (cands.isEmpty()) return;

        // Separación mínima Manhattan >= 2 y muestreo aleatorio
        int want = Math.max(min, Math.min(max, cands.size()));
        List<int[]> placed = new ArrayList<>(want);

        int guard = 0;
        while (placed.size() < want && guard++ < cands.size() * 4) {
            int[] p = cands.get(rng.nextInt(cands.size()));
            boolean ok = true;
            for (int[] q : placed) {
                int d = Math.abs(p[0] - q[0]) + Math.abs(p[1] - q[1]);
                if (d < 2) {
                    ok = false;
                    break;
                }
            }
            if (ok) placed.add(p);
        }

        for (int[] p : placed) paintWindow(m, p[0], p[1]);
    }

    private static boolean nearDoorOrWindow(GameMap m, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (!inBounds(m, nx, ny)) continue;
                char t = m.tiles[ny][nx];
                if (t == '+' || t == WINDOW) return true;
                if (isCornerGlyph(t)) return true;
            }
        }
        return false;
    }

    private static boolean isWallGlyph(char t) {
        return t == '═' || t == '║' || t == '╔' || t == '╗' || t == '╚' || t == '╝';
    }

    private static boolean isCornerGlyph(char t) {
        return t == '╔' || t == '╗' || t == '╚' || t == '╝';
    }

    private static boolean inBounds(GameMap m, int x, int y) {
        return x >= 0 && y >= 0 && x < m.w && y < m.h;
    }
}
