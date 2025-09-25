package ui.menu;

import utils.ANSI;
import world.GameMap;

import static game.Constants.FOV_OUTER_EXTRA;
import static utils.EntityUtil.isInterestingTile;

public class MapView {
    private final int top, left, viewW, viewH;
    private final int fovRadius;
    private final double cellAspect;
    private static final char ROOF_CHAR = '#';
    private static final int ROOF_COLOR = 100000 + 16;
    private static final int ROAD_VIS = 100000 + 240;
    private static final int ROAD_DET = 100000 + 239;
    private static final int ROAD_EXP = 100000 + 238;

    private final boolean[][] roofSeen;
    private boolean centerSmallMaps = false;

    private int lastCamX = Integer.MIN_VALUE, lastCamY = Integer.MIN_VALUE;
    private boolean pendingAnchor = false;
    private int anchorCamX = 0, anchorCamY = 0;

    private final int[][] visStamp;
    private final int[][] detStamp;
    private int frameId = 0;

    private final char[][] prevGlyph;
    private final int[][] prevColor;
    private final char[][] curGlyph;
    private final int[][] curColor;

    private final world.Entity[][] ovCell;

    private final int[][] roomStamp;
    private int curRoomId = 1;

    private boolean forceFullRepaint = false;
    private int extraOffX = 0, extraOffY = 0;

    public MapView(int top, int left, int viewW, int viewH, int fovRadius, GameMap map, double cellAspect) {
        this.top = Math.max(1, top);
        this.left = Math.max(1, left);
        this.viewW = Math.max(10, viewW);
        this.viewH = Math.max(5, viewH);
        this.fovRadius = Math.max(1, fovRadius);
        this.cellAspect = cellAspect <= 0 ? 2.0 : cellAspect;

        this.visStamp = new int[map.h][map.w];
        this.detStamp = new int[map.h][map.w];
        this.roofSeen = new boolean[map.h][map.w];

        this.prevGlyph = new char[viewH][viewW];
        this.prevColor = new int[viewH][viewW];
        this.curGlyph = new char[viewH][viewW];
        this.curColor = new int[viewH][viewW];
        this.ovCell = new world.Entity[viewH][viewW];

        this.roomStamp = new int[map.h][map.w];

        for (int y = 0; y < viewH; y++) {
            for (int x = 0; x < viewW; x++) {
                prevGlyph[y][x] = '\0';
                prevColor[y][x] = Integer.MIN_VALUE;
            }
        }
    }

    public void prefill() {
        drawTitle();
        int base = top + 2;
        for (int sy = 0; sy < viewH; sy++) {
            ANSI.gotoRC(base + sy, left);
            for (int sx = 0; sx < viewW; sx++) System.out.print(' ');
        }
    }

    public void render(GameMap map, int px, int py, java.util.Map<Long, world.Entity> overlay) {
        drawTitle();
        int base = top + 2;

        computeFovAndPeriphery(map, px, py);

        int camX, camY;
        int cx = Math.max(0, Math.min(px - viewW / 2, map.w - viewW));
        int cy = Math.max(0, Math.min(py - viewH / 2, map.h - viewH));
        if (centerSmallMaps) {
            camX = (map.w <= viewW) ? 0 : cx;
            camY = (map.h <= viewH) ? 0 : cy;
        } else {
            camX = cx;
            camY = cy;
        }
        if (pendingAnchor) {
            camX = anchorCamX;
            camY = anchorCamY;
            pendingAnchor = false;
        }

        if (camX != lastCamX || camY != lastCamY) {
            for (int y = 0; y < viewH; y++) {
                for (int x = 0; x < viewW; x++) {
                    prevGlyph[y][x] = '\0';
                    prevColor[y][x] = Integer.MIN_VALUE;
                }
            }
            lastCamX = camX;
            lastCamY = camY;
        }

        if (forceFullRepaint) {
            for (int y = 0; y < viewH; y++) {
                for (int x = 0; x < viewW; x++) {
                    prevGlyph[y][x] = '\0';
                    prevColor[y][x] = Integer.MIN_VALUE;
                }
            }
            forceFullRepaint = false;
        }

        int ox = 0, oy = 0;
        if (centerSmallMaps) {
            if (map.w < viewW) ox = (viewW - map.w) / 2;
            if (map.h < viewH) oy = (viewH - map.h) / 2;
        }

        ox += extraOffX;
        oy += extraOffY;

        for (int sy = 0; sy < viewH; sy++) {
            for (int sx = 0; sx < viewW; sx++) {
                ovCell[sy][sx] = null;
            }
        }
        if (overlay != null && !overlay.isEmpty()) {
            for (world.Entity e : overlay.values()) {
                int sx = e.x - camX + ox;
                int sy = e.y - camY + oy;
                if (sx >= 0 && sy >= 0 && sx < viewW && sy < viewH) {
                    ovCell[sy][sx] = e;
                }
            }
        }

        boolean playerIndoor = (py >= 0 && py < map.h && px >= 0 && px < map.w) && map.indoor[py][px];
        if (playerIndoor && roomStamp[py][px] != curRoomId) {
            ++curRoomId;
            if (curRoomId == Integer.MAX_VALUE) {
                for (int y = 0; y < map.h; y++) java.util.Arrays.fill(roomStamp[y], 0);
                curRoomId = 1;
            }
            floodRoom(map, px, py, curRoomId);
        }

        for (int sy = 0; sy < viewH; sy++) {
            for (int sx = 0; sx < viewW; sx++) {
                int mx = (sx - ox) + camX;
                int my = (sy - oy) + camY;

                char ch = ' ';
                int nextColor = 0;

                if (mx >= 0 && my >= 0 && mx < map.w && my < map.h) {
                    boolean vis = visStamp[my][mx] == frameId;
                    boolean det = detStamp[my][mx] == frameId;
                    boolean exp = map.explored[my][mx];

                    if (mx == px && my == py) {
                        world.Entity eHere = ovCell[sy][sx];
                        if (eHere != null) eHere.revealed = true;
                        ch = '@';
                        nextColor = 36;
                    } else {
                        char tile = map.tiles[my][mx];
                        boolean indoor = map.indoor[my][mx];
                        boolean isIndoorFloor = (tile == '▓' && indoor);

                        if (isIndoorFloor && inDisc(mx, my, px, py)) {
                            roofSeen[my][mx] = true;
                        }

                        boolean exposed = isIndoorFloor && ((playerIndoor && roomStamp[my][mx] == curRoomId) || vis);
                        boolean roofNow = isIndoorFloor && !exposed && inDisc(mx, my, px, py);
                        boolean roofDim = isIndoorFloor && !exposed && !inDisc(mx, my, px, py) && roofSeen[my][mx];

                        world.Entity ent = ovCell[sy][sx];
                        boolean drewEntity = false;
                        if (ent != null) {
                            if (vis && !roofNow && !roofDim) {
                                ent.revealed = true;
                                ch = ent.glyph;
                                nextColor = (ent.type == world.Entity.Type.LOOT) ? 100000 + 171 : 31;
                                drewEntity = true;
                            } else if (det && !roofNow && !roofDim) {
                                if (ent.type == world.Entity.Type.LOOT) {
                                    if (ent.revealed) {
                                        ch = ent.glyph;
                                        nextColor = 100000 + 139;
                                    } else {
                                        ch = '?';
                                        nextColor = 90;
                                    }
                                } else {
                                    ch = ent.revealed ? ent.glyph : '?';
                                    nextColor = 90;
                                }
                                drewEntity = true;
                            } else if (!det && ent.type == world.Entity.Type.LOOT && ent.revealed && !roofNow && !roofDim) {
                                ch = ent.glyph;
                                nextColor = 100000 + 139;
                                drewEntity = true;
                            }
                        }

                        if (!drewEntity) {
                            boolean strictVis = vis;
                            if (tile == '█') {
                                strictVis = vis;
                            }

                            if (vis) {
                                map.explored[my][mx] = true;
                                exp = true;
                            }

                            boolean roadFlag = map.road[my][mx] && !map.indoor[my][mx];

                            boolean isRoadOnFloor = roadFlag && tile == '▓';

                            boolean isRoadGlyph = roadFlag && (tile == '-' || tile == '¦' || tile == '┌' || tile == '┐' || tile == '└' || tile == '┘');

                            if (roofNow || roofDim) {
                                ch = ROOF_CHAR;
                                nextColor = ROOF_COLOR;

                            } else if (strictVis) {
                                ch = tile;
                                if (isRoadOnFloor || isRoadGlyph) {
                                    nextColor = ROAD_VIS;
                                } else {
                                    nextColor = switch (tile) {
                                        case '#' -> 92;
                                        case '~' -> 100000 + 45;
                                        case '█' -> 97;
                                        case '▓' -> (indoor ? 97 : 100000 + 252);
                                        case '╔', '╗', '╚', '╝', '═', '║', '│', '─', '┼', '├', '┤', '┬', '┴', '┌', '┐',
                                             '└', '┘' -> 100000 + 94;
                                        case '+' -> 93;
                                        case 'Û' -> 100000 + 228;
                                        default -> 100000 + 58;
                                    };
                                }

                            } else if (det) {
                                if (isInterestingTile(tile)) {
                                    if (exp) {
                                        ch = tile;
                                        if (isRoadOnFloor || isRoadGlyph) {
                                            nextColor = ROAD_DET;
                                        } else {
                                            nextColor = switch (tile) {
                                                case '#' -> 100000 + 22;
                                                case '~' -> 100000 + 24;
                                                case '█' -> 97;
                                                case '▓' -> (indoor ? 90 : 100000 + 248);
                                                case '╔', '╗', '╚', '╝', '═', '║', '│', '─', '┼', '├', '┤', '┬', '┴',
                                                     '┌', '┐', '└', '┘' -> 100000 + 94;
                                                case '+' -> 90;
                                                case 'Û' -> 100000 + 228;
                                                default -> 100000 + 137;
                                            };
                                        }
                                    } else {
                                        ch = '?';
                                        nextColor = 90;
                                    }
                                } else {
                                    ch = '▓';
                                    nextColor = indoor ? 90 : 100000 + 248;
                                }

                            } else if (exp) {
                                ch = tile;
                                if (isRoadOnFloor || isRoadGlyph) {
                                    nextColor = ROAD_EXP;
                                } else {
                                    nextColor = switch (tile) {
                                        case '#' -> 100000 + 22;
                                        case '~' -> 100000 + 24;
                                        case '█' -> 100000 + 250;
                                        case '▓' -> (indoor ? 90 : 100000 + 246);
                                        case '╔', '╗', '╚', '╝', '═', '║', '│', '─', '┼', '├', '┤', '┬', '┴', '┌', '┐',
                                             '└', '┘' -> 100000 + 94;
                                        case '+' -> 90;
                                        case 'Û' -> 100000 + 19;
                                        default -> 100000 + 137;
                                    };
                                }

                            } else {
                                ch = ' ';
                                nextColor = 0;
                            }
                        }
                    }
                }

                curGlyph[sy][sx] = ch;
                curColor[sy][sx] = nextColor;
            }
        }

        for (int sy = 0; sy < viewH; sy++) {
            int x = 0;
            while (x < viewW) {
                while (x < viewW && curGlyph[sy][x] == prevGlyph[sy][x] && curColor[sy][x] == prevColor[sy][x]) x++;
                if (x >= viewW) break;
                int start = x;
                int col = curColor[sy][x];
                while (x < viewW && (curGlyph[sy][x] != prevGlyph[sy][x] || curColor[sy][x] != prevColor[sy][x]) && curColor[sy][x] == col) {
                    x++;
                }
                ANSI.gotoRC(base + sy, left + start);
                applyColor(col);
                System.out.print(new String(curGlyph[sy], start, x - start));
                System.arraycopy(curGlyph[sy], start, prevGlyph[sy], start, x - start);
                System.arraycopy(curColor[sy], start, prevColor[sy], start, x - start);
            }
        }
        ANSI.resetStyle();
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
        frameId++;
        if (frameId == Integer.MAX_VALUE) {
            frameId = 1;
            for (int y = 0; y < map.h; y++) {
                java.util.Arrays.fill(visStamp[y], 0);
                java.util.Arrays.fill(detStamp[y], 0);
            }
        }

        int r = fovRadius;
        int y0 = Math.max(0, py - r), y1 = Math.min(map.h - 1, py + r);
        int x0 = Math.max(0, px - r), x1 = Math.min(map.w - 1, px + r);
        double r2 = r * (double) r;

        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int dx = x - px, dy = y - py;
                double dyAdj = dy * cellAspect;
                if (dx * dx + dyAdj * dyAdj <= r2 && los(map, px, py, x, y)) {
                    visStamp[y][x] = frameId;
                }
            }
        }

        int extra = Math.max(1, FOV_OUTER_EXTRA);
        int rp = r + extra;
        int yp0 = Math.max(0, py - rp), yp1 = Math.min(map.h - 1, py + rp);
        int xp0 = Math.max(0, px - rp), xp1 = Math.min(map.w - 1, px + rp);
        double rp2 = rp * (double) rp;

        for (int y = yp0; y <= yp1; y++) {
            for (int x = xp0; x <= xp1; x++) {
                if (visStamp[y][x] == frameId) continue;
                int dx = x - px, dy = y - py;
                double dyAdj = dy * cellAspect;
                if (dx * dx + dyAdj * dyAdj <= rp2 && los(map, px, py, x, y)) {
                    detStamp[y][x] = frameId;
                }
            }
        }
    }

    private boolean inDisc(int x, int y, int px, int py) {
        int r = fovRadius;
        int dx = x - px, dy = y - py;
        double dyAdj = dy * cellAspect;
        return dx * dx + dyAdj * dyAdj <= r * (double) r;
    }

    private void floodRoom(GameMap m, int sx, int sy, int id) {
        int w = m.w, h = m.h;
        int max = w * h;
        int[] qx = new int[max];
        int[] qy = new int[max];
        int head = 0, tail = 0;
        if (!m.indoor[sy][sx]) return;
        roomStamp[sy][sx] = id;
        qx[tail] = sx;
        qy[tail] = sy;
        tail++;
        while (head != tail) {
            int x = qx[head], y = qy[head];
            head++;
            if (head == max) head = 0;

            if (x > 0 && m.indoor[y][x - 1] && roomStamp[y][x - 1] != id) {
                roomStamp[y][x - 1] = id;
                qx[tail] = x - 1;
                qy[tail] = y;
                tail++;
                if (tail == max) tail = 0;
            }
            if (x + 1 < w && m.indoor[y][x + 1] && roomStamp[y][x + 1] != id) {
                roomStamp[y][x + 1] = id;
                qx[tail] = x + 1;
                qy[tail] = y;
                tail++;
                if (tail == max) tail = 0;
            }
            if (y > 0 && m.indoor[y - 1][x] && roomStamp[y - 1][x] != id) {
                roomStamp[y - 1][x] = id;
                qx[tail] = x;
                qy[tail] = y - 1;
                tail++;
                if (tail == max) tail = 0;
            }
            if (y + 1 < h && m.indoor[y + 1][x] && roomStamp[y + 1][x] != id) {
                roomStamp[y + 1][x] = id;
                qx[tail] = x;
                qy[tail] = y + 1;
                tail++;
                if (tail == max) tail = 0;
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
        if (y < 0 || y >= visStamp.length) return false;
        if (x < 0 || x >= visStamp[0].length) return false;
        return visStamp[y][x] == frameId;
    }

    public boolean wasDetectedLastRender(int x, int y) {
        if (y < 0 || y >= detStamp.length) return false;
        if (x < 0 || x >= detStamp[0].length) return false;
        return detStamp[y][x] == frameId;
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

    public void anchorOnceKeepingPlayerAt(int sx, int sy, int px, int py, GameMap map) {
        int sxClamped = Math.max(0, Math.min(sx, viewW - 1));
        int syClamped = Math.max(0, Math.min(sy, viewH - 1));
        int camX = px - sxClamped;
        int camY = py - syClamped;
        camX = Math.max(0, Math.min(camX, Math.max(0, map.w - viewW)));
        camY = Math.max(0, Math.min(camY, Math.max(0, map.h - viewH)));
        this.anchorCamX = camX;
        this.anchorCamY = camY;
        this.pendingAnchor = true;
    }

    public void setCenterSmallMaps(boolean v) {
        this.centerSmallMaps = v;
    }

    public void requestFullRepaint() {
        this.forceFullRepaint = true;
    }

    public void setExtraOffset(int ex, int ey) {
        this.extraOffX = ex;
        this.extraOffY = ey;
    }
}
