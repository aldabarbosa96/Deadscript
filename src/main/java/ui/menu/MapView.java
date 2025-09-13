package ui.menu;

import utils.ANSI;
import world.GameMap;

import java.awt.*;

public class MapView {
    private final int top, left, viewW, viewH;
    private final int fovRadius;
    private final boolean[][] visible;
    private final double cellAspect;

    public MapView(int top, int left, int viewW, int viewH, int fovRadius, GameMap map, double cellAspect) {
        this.top = Math.max(1, top);
        this.left = Math.max(1, left);
        this.viewW = Math.max(10, viewW);
        this.viewH = Math.max(5, viewH);
        this.fovRadius = Math.max(1, fovRadius);
        this.cellAspect = cellAspect <= 0 ? 2.0 : cellAspect;
        this.visible = new boolean[map.h][map.w];
    }

    public static int suggestedWidth(int viewH, double cellAspect) {
        return (int) Math.round(viewH * (cellAspect <= 0 ? 2.0 : cellAspect));
    }

    public void prefill() {
        drawTitle(); // título + línea en blanco
        int base = top + 2;
        for (int sy = 0; sy < viewH; sy++) {
            ANSI.gotoRC(base + sy, left);
            for (int sx = 0; sx < viewW; sx++) System.out.print(' ');
        }
    }

    public void render(GameMap map, int px, int py) {
        // repinta encabezado y deja una línea en blanco
        drawTitle();
        int base = top + 2;

        // recalcula FOV para este frame
        computeFov(map, px, py);

        // cámara centrada en el jugador dentro de los límites del mapa
        int camX = Math.max(0, Math.min(px - viewW / 2, map.w - viewW));
        int camY = Math.max(0, Math.min(py - viewH / 2, map.h - viewH));

        for (int sy = 0; sy < viewH; sy++) {
            ANSI.gotoRC(base + sy, left);
            int currentColor = -1; // -1 = sin color, 0 = reset, ANSI básicos = su código, 100000+idx = 256-color

            for (int sx = 0; sx < viewW; sx++) {
                int mx = camX + sx, my = camY + sy;

                // fuera del mapa → espacio en blanco
                if (mx < 0 || my < 0 || mx >= map.w || my >= map.h) {
                    if (currentColor != 0) {
                        ANSI.resetStyle();
                        currentColor = 0;
                    }
                    System.out.print(' ');
                    continue;
                }

                char tile = map.tiles[my][mx];
                boolean vis = visible[my][mx];

                if (vis) {
                    // visible este frame
                    map.explored[my][mx] = true;

                    // color por tipo de tile (visible)
                    int color;
                    if (tile == '#') {
                        // árbol/obstáculo visible → verde brillante (sin cambios)
                        color = 92;
                    } else {
                        // suelo u otros → blanco/gris claro
                        color = 37;
                    }

                    if (color != currentColor) {
                        ANSI.setFg(color);
                        currentColor = color;
                    }
                    System.out.print(tile);

                } else if (map.explored[my][mx]) {
                    // ya explorado pero fuera de FOV
                    if (tile == '#') {
                        // árbol/obstáculo explorado → verde MUCHO más oscuro (xterm-256: 22 = #005f00)
                        int sentinel = 100000 + 22; // marca interna para no repetir escapes
                        if (currentColor != sentinel) {
                            if (utils.ANSI.isEnabled()) {
                                System.out.print("\u001B[38;5;22m");
                            }
                            currentColor = sentinel;
                        }
                        System.out.print(tile);
                    } else {
                        // suelo explorado → gris oscuro
                        int color = 90;
                        if (color != currentColor) {
                            ANSI.setFg(color);
                            currentColor = color;
                        }
                        System.out.print(tile);
                    }

                } else {
                    // no visto nunca → vacío
                    if (currentColor != 0) {
                        ANSI.resetStyle();
                        currentColor = 0;
                    }
                    System.out.print(' ');
                }
            }
            ANSI.resetStyle();
        }

        // dibuja al jugador encima
        int sxPlayer = px - camX;
        int syPlayer = py - camY;
        if (sxPlayer >= 0 && sxPlayer < viewW && syPlayer >= 0 && syPlayer < viewH) {
            ANSI.gotoRC(base + syPlayer, left + sxPlayer);
            ANSI.setFg(36);
            System.out.print('@');
            ANSI.resetStyle();
        }
    }


    private void drawTitle() {
        ANSI.gotoRC(top, left);
        String label = " MAPA ";
        if (label.length() >= viewW) {
            System.out.print(label.substring(0, Math.max(0, viewW)));
        } else {
            int leftDash = (viewW - label.length()) / 2;
            int rightDash = viewW - label.length() - leftDash;
            System.out.print("─".repeat(leftDash));
            System.out.print(label);
            System.out.print("─".repeat(rightDash));
        }
        ANSI.gotoRC(top + 1, left);
        ANSI.clearToLineEnd(); // línea en blanco bajo el título
    }

    private void computeFov(GameMap map, int px, int py) {
        for (int y = 0; y < map.h; y++) java.util.Arrays.fill(visible[y], false);

        int r = fovRadius;
        int y0 = Math.max(0, py - r), y1 = Math.min(map.h - 1, py + r);
        int x0 = Math.max(0, px - r), x1 = Math.min(map.w - 1, px + r);
        double r2 = r * (double) r;

        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int dx = x - px, dy = y - py;
                double dyAdj = dy * cellAspect;
                if (dx * dx + dyAdj * dyAdj <= r2 && los(map, px, py, x, y)) {
                    visible[y][x] = true;
                }
            }
        }
    }

    private boolean los(GameMap map, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy, e2, x = x0, y = y0;
        while (true) {
            if (x == x1 && y == y1) return true;
            if (!(x == x0 && y == y0) && !map.transp[y][x]) return false;

            e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y += sy;
            }

            if (x < 0 || y < 0 || x >= map.w || y >= map.h) return false;
        }
    }

    public int getTop() {
        return top;
    }

    public int getLeft() {
        return left;
    }

    public int getViewW() {
        return viewW;
    }

    public int getViewH() {
        return viewH;
    }

    public boolean wasVisibleLastRender(int x, int y) {
        if (y < 0 || y >= visible.length) return false;
        if (x < 0 || x >= visible[0].length) return false;
        return visible[y][x];
    }
}
