package world.builders.map;

import world.GameMap;
import java.util.Random;

public final class RiverBuilder {

    public void build(GameMap m, Random rng, int cx, int cy, int safeRadius) {
        if (m.w < 40 || m.h < 30) return;

        boolean horizontal = rng.nextBoolean();
        double amp   = (horizontal ? m.h : m.w) * (0.08 + rng.nextDouble() * 0.06);
        double freq  = (0.015 + rng.nextDouble() * 0.015);
        double phase = rng.nextDouble() * Math.PI * 2.0;
        int base     = horizontal ? (int) (m.h * (0.25 + rng.nextDouble() * 0.5))
                : (int) (m.w * (0.25 + rng.nextDouble() * 0.5));
        int halfW = 2 + rng.nextInt(3); // 2..4

        if (horizontal) {
            for (int x = 1; x < m.w - 1; x++) {
                int yC = base + (int) Math.round(Math.sin(x * freq + phase) * amp);
                for (int dy = -halfW; dy <= halfW; dy++) {
                    int y = yC + dy;
                    if (y <= 0 || y >= m.h - 1) continue;
                    paintWater(m, x, y);
                }
            }
        } else {
            for (int y = 1; y < m.h - 1; y++) {
                int xC = base + (int) Math.round(Math.sin(y * freq + phase) * amp);
                for (int dx = -halfW; dx <= halfW; dx++) {
                    int x = xC + dx;
                    if (x <= 0 || x >= m.w - 1) continue;
                    paintWater(m, x, y);
                }
            }
        }

        // Ribera: si hay agua, limpia árboles adyacentes (# → suelo)
        for (int y = 1; y < m.h - 1; y++) {
            for (int x = 1; x < m.w - 1; x++) {
                if (m.tiles[y][x] == '~') {
                    for (int yy = y - 1; yy <= y + 1; yy++) {
                        for (int xx = x - 1; xx <= x + 1; xx++) {
                            if (m.tiles[yy][xx] == '#') paintFloor(m, xx, yy);
                        }
                    }
                }
            }
        }

        // Reabre el área segura del centro
        carveDiskFloor(m, cx, cy, safeRadius);
    }

    private void paintWater(GameMap m, int x, int y) {
        m.tiles[y][x]  = '~';
        m.walk[y][x]   = false;
        m.transp[y][x] = true;
        // indoor no se toca
    }

    private void paintFloor(GameMap m, int x, int y) {
        m.tiles[y][x]  = '▓';
        m.walk[y][x]   = true;
        m.transp[y][x] = true;
        // indoor no se toca
    }

    private void carveDiskFloor(GameMap m, int cx, int cy, int r) {
        int r2 = r * r;
        int x0 = Math.max(1, cx - r), x1 = Math.min(m.w - 2, cx + r);
        int y0 = Math.max(1, cy - r), y1 = Math.min(m.h - 2, cy + r);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy <= r2) paintFloor(m, x, y);
            }
        }
    }
}
