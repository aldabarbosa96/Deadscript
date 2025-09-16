package ui.menu;

import utils.ANSI;
import world.GameMap;

import static game.Constants.FOV_OUTER_EXTRA;

public class MapView {
    private final int top, left, viewW, viewH;
    private final int fovRadius;
    private final boolean[][] visible;
    private final boolean[][] detected; // ← anillo periférico (no visible, pero detectado)
    private final double cellAspect;

    public MapView(int top, int left, int viewW, int viewH, int fovRadius, GameMap map, double cellAspect) {
        this.top = Math.max(1, top);
        this.left = Math.max(1, left);
        this.viewW = Math.max(10, viewW);
        this.viewH = Math.max(5, viewH);
        this.fovRadius = Math.max(1, fovRadius);
        this.cellAspect = cellAspect <= 0 ? 2.0 : cellAspect;
        this.visible = new boolean[map.h][map.w];
        this.detected = new boolean[map.h][map.w];
    }

    public void prefill() {
        drawTitle();
        int base = top + 2;
        for (int sy = 0; sy < viewH; sy++) {
            ANSI.gotoRC(base + sy, left);
            for (int sx = 0; sx < viewW; sx++) System.out.print(' ');
        }
    }

    public void render(GameMap map, int px, int py) {
        drawTitle();
        int base = top + 2;

        computeFovAndPeriphery(map, px, py);

        int camX = Math.max(0, Math.min(px - viewW / 2, map.w - viewW));
        int camY = Math.max(0, Math.min(py - viewH / 2, map.h - viewH));

        for (int sy = 0; sy < viewH; sy++) {
            ANSI.gotoRC(base + sy, left);
            int currentColor = -1;

            for (int sx = 0; sx < viewW; sx++) {
                int mx = camX + sx, my = camY + sy;

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
                boolean exp = map.explored[my][mx];
                boolean det = detected[my][mx];

                if (vis) {
                    map.explored[my][mx] = true;
                    int next = switch (tile) {
                        case '#' -> 92;
                        case '~' -> 100000 + 45;
                        case '^' -> 37;
                        case '.' -> (map.indoor[my][mx] ? 97 : 100000 + 58);
                        case '╔', '╗', '╚', '╝', '═', '║' -> 100000 + 94;
                        case '+' -> 93;
                        default -> 100000 + 58;
                    };
                    if (next != currentColor) {
                        applyColor(next);
                        currentColor = next;
                    }
                    System.out.print(tile);

                } else if (det) {
                    if (isInterestingTile(tile)) {
                        if (exp) {
                            int next = switch (tile) {
                                case '#' -> 100000 + 22;
                                case '~' -> 100000 + 24;
                                case '^' -> 90;
                                case '.' -> (map.indoor[my][mx] ? 90 : 100000 + 137);
                                case '╔', '╗', '╚', '╝', '═', '║' -> 100000 + 58;
                                case '+' -> 90;
                                default -> 100000 + 137;
                            };
                            if (next != currentColor) {
                                applyColor(next);
                                currentColor = next;
                            }
                            System.out.print(tile);
                        } else {
                            int next = 90;
                            if (next != currentColor) {
                                applyColor(next);
                                currentColor = next;
                            }
                            System.out.print('?');
                        }
                    } else {
                        int next = 100000 + 155;
                        if (next != currentColor) {
                            applyColor(next);
                            currentColor = next;
                        }
                        System.out.print('.');
                    }

                } else if (exp) {
                    int next = switch (tile) {
                        case '#' -> 100000 + 22;
                        case '~' -> 100000 + 24;
                        case '^' -> 90;
                        case '.' -> (map.indoor[my][mx] ? 90 : 100000 + 137);
                        case '╔', '╗', '╚', '╝', '═', '║' -> 100000 + 58;
                        case '+' -> 90;
                        default -> 100000 + 137;
                    };
                    if (next != currentColor) {
                        applyColor(next);
                        currentColor = next;
                    }
                    System.out.print(tile);

                } else {
                    if (currentColor != 0) {
                        ANSI.resetStyle();
                        currentColor = 0;
                    }
                    System.out.print(' ');
                }
            }
            ANSI.resetStyle();
        }

        int sxPlayer = px - camX, syPlayer = py - camY;
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
        ANSI.clearToLineEnd();
    }

    private void computeFovAndPeriphery(GameMap map, int px, int py) {
        for (int y = 0; y < map.h; y++) {
            java.util.Arrays.fill(visible[y], false);
            java.util.Arrays.fill(detected[y], false);
        }

        // FOV principal
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

        // Anillo periférico (R + extra), sólo marca detectado si NO es visible
        int extra = Math.max(1, FOV_OUTER_EXTRA);
        int rp = r + extra;
        int yp0 = Math.max(0, py - rp), yp1 = Math.min(map.h - 1, py + rp);
        int xp0 = Math.max(0, px - rp), xp1 = Math.min(map.w - 1, px + rp);
        double rp2 = rp * (double) rp;

        for (int y = yp0; y <= yp1; y++) {
            for (int x = xp0; x <= xp1; x++) {
                if (visible[y][x]) continue; // ya se ve de verdad
                int dx = x - px, dy = y - py;
                double dyAdj = dy * cellAspect;
                if (dx * dx + dyAdj * dyAdj <= rp2 && los(map, px, py, x, y)) {
                    detected[y][x] = true;
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

    private static boolean isInterestingTile(char t) {
        // Terreno/estructura que merece "?" al detectarse
        return switch (t) {
            case '#', '^', '~', '╔', '╗', '╚', '╝', '═', '║', '+' -> true;
            default -> false;
        };
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

    public boolean wasDetectedLastRender(int x, int y) {
        if (y < 0 || y >= detected.length) return false;
        if (x < 0 || x >= detected[0].length) return false;
        return detected[y][x];
    }

    private static void applyColor(int sentinel) {
        if (sentinel >= 100000) {
            int idx = sentinel - 100000;
            if (ANSI.isEnabled()) System.out.print("\u001B[38;5;" + idx + "m");
        } else if (sentinel == 0) {
            ANSI.resetStyle();
        } else {
            ANSI.setFg(sentinel);
        }
    }
}
