package world.builders;

import world.GameMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class HouseBuilder {

    public void build(GameMap m, Random rng, int clusters, int singles, int safeRadius) {
        addCabinClusters(m, rng, clusters, safeRadius);
        addCabins(m, rng, singles, safeRadius);
        punchDoorsBetweenTouchingInteriors(m, rng);
    }

    public static void drawRectHouseShell(GameMap m, int x0, int y0, int x1, int y1) {
        // Interior
        for (int y = y0 + 1; y <= y1 - 1; y++) {
            for (int x = x0 + 1; x <= x1 - 1; x++) {
                paintFloor(m, x, y);
                m.indoor[y][x] = true;
            }
        }
        // Paredes
        paintWall(m, x0, y0, '╔');
        paintWall(m, x1, y0, '╗');
        paintWall(m, x0, y1, '╚');
        paintWall(m, x1, y1, '╝');
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            paintWall(m, x, y0, '═');
            paintWall(m, x, y1, '═');
        }
        for (int y = y0 + 1; y <= y1 - 1; y++) {
            paintWall(m, x0, y, '║');
            paintWall(m, x1, y, '║');
        }
    }

    private void addCabins(GameMap m, Random rng, int count, int safeRadius) {
        int target = Math.max(1, count);
        int tries = target * 60;
        int placed = 0;

        while (placed < target && tries-- > 0) {
            int wCab = 8 + rng.nextInt(11);
            int hCab = 6 + rng.nextInt(9);
            int x0 = 2 + rng.nextInt(Math.max(1, m.w - 2 - wCab - 2));
            int y0 = 2 + rng.nextInt(Math.max(1, m.h - 2 - hCab - 2));
            int x1 = x0 + wCab - 1, y1 = y0 + hCab - 1;

            int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) continue;

            // Requisito: sin agua en un halo 1-tile alrededor
            if (!areaClearOfWater(m, x0 - 1, y0 - 1, x1 + 1, y1 + 1)) continue;

            buildCabin(m, x0, y0, x1, y1);
            // Escaleras (subsuelo/altillo) con la API pública del GameMap
            maybePlaceStairsInHouse(m, rng, x0, y0, x1, y1);
            placed++;
        }

        // Fallback determinista: si no se colocó ninguna, forzamos 1
        if (placed == 0) {
            outer:
            for (int hCab = 7; hCab <= 14; hCab++) {
                for (int wCab = 10; wCab <= 18; wCab++) {
                    for (int y0 = 2; y0 <= m.h - 2 - hCab; y0++) {
                        for (int x0 = 2; x0 <= m.w - 2 - wCab; x0++) {
                            int x1 = x0 + wCab - 1, y1 = y0 + hCab - 1;
                            int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
                            if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) continue;
                            if (!areaClearOfWater(m, x0 - 1, y0 - 1, x1 + 1, y1 + 1)) continue;

                            buildCabin(m, x0, y0, x1, y1);
                            maybePlaceStairsInHouse(m, rng, x0, y0, x1, y1);
                            break outer;
                        }
                    }
                }
            }
        }
    }

    private void addCabinClusters(GameMap m, Random rng, int groups, int safeRadius) {
        int placed = 0;
        int attempts = groups * 40;

        while (placed < groups && attempts-- > 0) {
            int modules = 2 + rng.nextInt(5);

            int wCab = 8 + rng.nextInt(8);
            int hCab = 6 + rng.nextInt(7);
            int x0 = 2 + rng.nextInt(Math.max(1, m.w - 2 - wCab - 2));
            int y0 = 2 + rng.nextInt(Math.max(1, m.h - 2 - hCab - 2));
            int x1 = x0 + wCab - 1, y1 = y0 + hCab - 1;

            int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            if (dist2(cx, cy, m.w / 2, m.h / 2) <= (safeRadius + 3) * (safeRadius + 3)) continue;
            if (!areaBuildableForCabin(m, x0 - 1, y0 - 1, x1 + 1, y1 + 1)) continue;

            ArrayList<RectI> rooms = new ArrayList<>();
            drawCabinShell(m, x0, y0, x1, y1);
            RectI first = new RectI(x0, y0, x1, y1);
            rooms.add(first);

            for (int k = 1; k < modules; k++) {
                if (!tryAttachModule(m, rng, rooms, safeRadius)) break;
            }

            addExteriorDoorsOnBounding(m, rooms, rng);
            RectI bb = boundsOf(rooms);
            maybePlaceStairsInHouse(m, rng, bb.x0, bb.y0, bb.x1, bb.y1);
            placed++;
        }
    }

    private static void drawCabinShell(GameMap m, int x0, int y0, int x1, int y1) {
        for (int y = y0 + 1; y <= y1 - 1; y++) {
            for (int x = x0 + 1; x <= x1 - 1; x++) {
                paintFloor(m, x, y);
                m.indoor[y][x] = true;
            }
        }
        paintWall(m, x0, y0, '╔');
        paintWall(m, x1, y0, '╗');
        paintWall(m, x0, y1, '╚');
        paintWall(m, x1, y1, '╝');
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            paintWall(m, x, y0, '═');
            paintWall(m, x, y1, '═');
        }
        for (int y = y0 + 1; y <= y1 - 1; y++) {
            paintWall(m, x0, y, '║');
            paintWall(m, x1, y, '║');
        }
    }

    private boolean tryAttachModule(GameMap m, Random rng, List<RectI> rooms, int safeRadius) {
        RectI base = rooms.get(rng.nextInt(rooms.size()));

        int wCab = 10 + rng.nextInt(11);  // 8..16
        int hCab = 7 + rng.nextInt(10);  // 5..12

        int side = rng.nextInt(4);
        int minOverlap = 3;

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

            drawCabinShell(m, x0, y0, x1, y1);
            RectI neo = new RectI(x0, y0, x1, y1);
            rooms.add(neo);
            openSharedDoor(m, base, neo, rng);
            return true;
        }
        return false;
    }

    private void openSharedDoor(GameMap m, RectI a, RectI b, Random rng) {
        // vertical compartida
        if (a.x1 == b.x0 || b.x1 == a.x0) {
            int x = (a.x1 == b.x0) ? a.x1 : b.x1;
            int yStart = Math.max(a.y0 + 1, b.y0 + 1);
            int yEnd = Math.min(a.y1 - 1, b.y1 - 1);
            if (yStart <= yEnd) {
                int y = yStart + rng.nextInt(Math.max(1, yEnd - yStart + 1));
                paintDoor(m, x, y);
                if (inBounds(m, x - 1, y)) {
                    paintFloor(m, x - 1, y);
                    m.indoor[y][x - 1] = true;
                }
                if (inBounds(m, x + 1, y)) {
                    paintFloor(m, x + 1, y);
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
                paintDoor(m, x, y);
                if (inBounds(m, x, y - 1)) {
                    paintFloor(m, x, y - 1);
                    m.indoor[y - 1][x] = true;
                }
                if (inBounds(m, x, y + 1)) {
                    paintFloor(m, x, y + 1);
                    m.indoor[y + 1][x] = true;
                }
            }
        }
    }

    private void addExteriorDoorsOnBounding(GameMap m, List<RectI> rooms, Random rng) {
        RectI bb = boundsOf(rooms);
        int doors = 1 + rng.nextInt(2);
        for (int d = 0; d < doors; d++) {
            int side = rng.nextInt(4); // 0 top,1 right,2 bottom,3 left
            switch (side) {
                case 0 -> {
                    if (bb.w() >= 4) {
                        int x = bb.x0 + 2 + rng.nextInt(Math.max(1, bb.w() - 3));
                        int y = bb.y0;
                        paintDoor(m, x, y);
                        if (inBounds(m, x, y + 1)) {
                            paintFloor(m, x, y + 1);
                            m.indoor[y + 1][x] = true;
                        }
                    }
                }
                case 1 -> {
                    if (bb.h() >= 4) {
                        int x = bb.x1;
                        int y = bb.y0 + 2 + rng.nextInt(Math.max(1, bb.h() - 3));
                        paintDoor(m, x, y);
                        if (inBounds(m, x - 1, y)) {
                            paintFloor(m, x - 1, y);
                            m.indoor[y][x - 1] = true;
                        }
                    }
                }
                case 2 -> {
                    if (bb.w() >= 4) {
                        int x = bb.x0 + 2 + rng.nextInt(Math.max(1, bb.w() - 3));
                        int y = bb.y1;
                        paintDoor(m, x, y);
                        if (inBounds(m, x, y - 1)) {
                            paintFloor(m, x, y - 1);
                            m.indoor[y - 1][x] = true;
                        }
                    }
                }
                default -> {
                    if (bb.h() >= 4) {
                        int x = bb.x0;
                        int y = bb.y0 + 2 + rng.nextInt(Math.max(1, bb.h() - 3));
                        paintDoor(m, x, y);
                        if (inBounds(m, x + 1, y)) {
                            paintFloor(m, x + 1, y);
                            m.indoor[y][x + 1] = true;
                        }
                    }
                }
            }
        }
    }

    private void buildCabin(GameMap m, int x0, int y0, int x1, int y1) {
        drawCabinShell(m, x0, y0, x1, y1);
        if ((x1 - x0 + 1) >= (y1 - y0 + 1)) paintDoor(m, (x0 + x1) / 2, y1);
        else paintDoor(m, x1, (y0 + y1) / 2);
    }

    private void punchDoorsBetweenTouchingInteriors(GameMap m, Random rng) {
        // Segmentos verticales (pared '║'): interiores a izquierda y derecha
        for (int x = 1; x < m.w - 1; x++) {
            int y = 1;
            while (y < m.h - 1) {
                if (m.tiles[y][x] == '║' && m.indoor[y][x - 1] && m.indoor[y][x + 1]) {
                    int y0 = y;
                    while (y < m.h - 1 && m.tiles[y][x] == '║' && m.indoor[y][x - 1] && m.indoor[y][x + 1]) y++;
                    int y1 = y - 1;
                    int yy = y0 + rng.nextInt(y1 - y0 + 1);
                    paintDoor(m, x, yy);
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
                    paintDoor(m, xx, y);
                } else {
                    x++;
                }
            }
        }
    }

    private void maybePlaceStairsInHouse(GameMap m, Random rng, int x0, int y0, int x1, int y1) {
        if (rng == null) return;
        if (rng.nextDouble() > 0.35) return;

        // Elegir par de celdas en una esquina del INTERIOR
        int[] pair = pickCornerDoubleStair(m, x0, y0, x1, y1, rng);
        if (pair == null) return;

        int ax = pair[0], ay = pair[1];
        int bx = pair[2], by = pair[3];

        // Coloca las DOS celdas de escalera en la planta actual
        m.placeStair(ax, ay);
        m.placeStair(bx, by);

        boolean makeUp = rng.nextBoolean();          // ~50% segunda planta
        boolean makeDown = rng.nextDouble() < 0.45;  // ~45% sótano
        if (!makeUp && !makeDown) {                  // garantiza al menos una
            makeDown = rng.nextBoolean();
            makeUp = !makeDown;
        }

        int interiorW = x1 - x0 - 1;
        int interiorH = y1 - y0 - 1;

        // offsets relativos al interior [0..interiorW-1/0..interiorH-1]
        int relX1 = ax - (x0 + 1), relY1 = ay - (y0 + 1);
        int relX2 = bx - (x0 + 1), relY2 = by - (y0 + 1);

        if (makeUp) {
            GameMap up = makeUpperFloorVariant(interiorW, interiorH, rng);

            // Escoge una esquina interior válida del piso superior para la escalera doble
            int[] upPair = pickCornerDoubleStair(up, 0, 0, up.w - 1, up.h - 1, rng);

            // Fallback ultra-seguro (no debería ocurrir con tamaños actuales):
            int uax, uay, ubx, uby;
            if (upPair != null) {
                uax = upPair[0];
                uay = upPair[1];
                ubx = upPair[2];
                uby = upPair[3];
            } else {
                uax = 1;
                uay = 1;          // (1,1)
                ubx = Math.min(up.w - 2, 2);
                uby = 1;  // (2,1)
            }

            // Coloca y enlaza
            up.placeStair(uax, uay);
            up.linkStairDown(uax, uay, m, ax, ay);
            m.linkStairUp(ax, ay, up, uax, uay);

            up.placeStair(ubx, uby);
            up.linkStairDown(ubx, uby, m, bx, by);
            m.linkStairUp(bx, by, up, ubx, uby);
        }

        if (makeDown) {
            GameMap down = makeBasement(interiorW, interiorH, rng);

            int dx1 = 1 + clamp(relX1, 0, down.w - 3);
            int dy1 = 1 + clamp(relY1, 0, down.h - 3);
            int dx2 = 1 + clamp(relX2, 0, down.w - 3);
            int dy2 = 1 + clamp(relY2, 0, down.h - 3);

            // Evita solapado si el sótano es más pequeño
            if (dx2 == dx1 && dy2 == dy1) {
                if (dx2 + 1 <= down.w - 2) dx2++;
                else if (dx2 - 1 >= 1) dx2--;
                else if (dy2 + 1 <= down.h - 2) dy2++;
                else if (dy2 - 1 >= 1) dy2--;
                else {
                    dx2 = -1;
                    dy2 = -1;
                }
            }

            down.placeStair(dx1, dy1);
            down.linkStairUp(dx1, dy1, m, ax, ay);
            m.linkStairDown(ax, ay, down, dx1, dy1);

            if (dx2 >= 0) {
                down.placeStair(dx2, dy2);
                down.linkStairUp(dx2, dy2, m, bx, by);
                m.linkStairDown(bx, by, down, dx2, dy2);
            }
        }
    }

    private int[] pickCornerDoubleStair(GameMap m, int x0, int y0, int x1, int y1, Random rng) {
        // c: {cx, cy, dx_h, dy_h, dx_v, dy_v}
        // dx_h/dy_h: dirección horizontal preferida hacia el interior desde la esquina
        // dx_v/dy_v: dirección vertical alternativa hacia el interior
        int[][] corners = new int[][]{{x0 + 1, y0 + 1, +1, 0, 0, +1},  // top-left: → , luego ↓
                {x1 - 1, y0 + 1, -1, 0, 0, +1},  // top-right: ← , luego ↓
                {x1 - 1, y1 - 1, -1, 0, 0, -1},  // bottom-right: ← , luego ↑
                {x0 + 1, y1 - 1, +1, 0, 0, -1}   // bottom-left: → , luego ↑
        };
        // Mezcla el orden de prueba de esquinas
        for (int i = 0; i < corners.length; i++) {
            int j = i + rng.nextInt(corners.length - i);
            int[] t = corners[i];
            corners[i] = corners[j];
            corners[j] = t;
        }

        for (int[] c : corners) {
            int cx = c[0], cy = c[1];
            int dxh = c[2], dyh = c[3];
            int dxv = c[4], dyv = c[5];

            // Candidato horizontal (preferido)
            int ax = cx, ay = cy, bx = cx + dxh, by = cy + dyh;
            if (validStairCell(m, ax, ay) && validStairCell(m, bx, by)) return new int[]{ax, ay, bx, by};

            // Candidato vertical (alternativo)
            bx = cx + dxv;
            by = cy + dyv;
            if (validStairCell(m, ax, ay) && validStairCell(m, bx, by)) return new int[]{ax, ay, bx, by};
        }
        return null;
    }

    private boolean validStairCell(GameMap m, int x, int y) {
        return inBounds(m, x, y) && m.indoor[y][x] && m.tiles[y][x] == '▓';
    }

    private int clamp(int v, int lo, int hi) {
        return (v < lo) ? lo : (v > hi ? hi : v);
    }

    private static GameMap makeUpperFloorVariant(int baseInteriorW, int baseInteriorH, Random rng) {
        int shrinkW = Math.min(3, Math.max(0, baseInteriorW - 3));
        int shrinkH = Math.min(3, Math.max(0, baseInteriorH - 3));
        int w = Math.max(3, baseInteriorW - (shrinkW == 0 ? 1 : 1 + rng.nextInt(shrinkW)));
        int h = Math.max(3, baseInteriorH - (shrinkH == 0 ? 1 : 1 + rng.nextInt(shrinkH)));

        GameMap m = new GameMap(w + 2, h + 2);

        // Base suelo
        for (int y = 0; y < m.h; y++)
            for (int x = 0; x < m.w; x++)
                paintFloor(m, x, y);

        // Envolvente + interior SIN puerta exterior
        drawCabinShell(m, 0, 0, m.w - 1, m.h - 1);

        // Tabique vertical con hueco (sin puerta)
        if (w >= 6) {
            int vx = 1 + w / 2 + (rng.nextBoolean() ? -1 : 1) * (rng.nextInt(Math.max(1, w / 4)));
            vx = Math.max(2, Math.min(m.w - 3, vx));
            for (int y = 1; y <= h; y++) paintWall(m, vx, y, '║');
            int dy = 1 + rng.nextInt(Math.max(1, h));
            paintFloor(m, vx, dy);
            m.indoor[dy][vx] = true;
        }

        // Tabique horizontal opcional con hueco (sin puerta)
        if (h >= 5 && rng.nextBoolean()) {
            int hy = 1 + h / 2 + (rng.nextBoolean() ? -1 : 1) * (rng.nextInt(Math.max(1, h / 4)));
            hy = Math.max(2, Math.min(m.h - 3, hy));
            for (int x = 1; x <= w; x++) paintWall(m, x, hy, '═');
            int dx = 1 + rng.nextInt(Math.max(1, w));
            paintFloor(m, dx, hy);
            m.indoor[hy][dx] = true;
        }

        return m;
    }

    private static GameMap makeBasement(int baseInteriorW, int baseInteriorH, Random rng) {
        int addW = 3 + rng.nextInt(4);
        int addH = 2 + rng.nextInt(3);
        int w = Math.max(4, baseInteriorW + addW);
        int h = Math.max(4, baseInteriorH + addH);

        GameMap m = new GameMap(w + 2, h + 2);
        for (int y = 0; y < m.h; y++)
            for (int x = 0; x < m.w; x++)
                paintFloor(m, x, y);

        // Caja grande con puerta exterior
        drawRectHouseShell(m, 0, 0, m.w - 1, m.h - 1);
        return m;
    }

    private record RectI(int x0, int y0, int x1, int y1) {
        int w() {
            return x1 - x0 + 1;
        }

        int h() {
            return y1 - y0 + 1;
        }
    }

    private RectI boundsOf(List<RectI> rs) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (RectI r : rs) {
            x0 = Math.min(x0, r.x0);
            y0 = Math.min(y0, r.y0);
            x1 = Math.max(x1, r.x1);
            y1 = Math.max(y1, r.y1);
        }
        return new RectI(x0, y0, x1, y1);
    }

    private boolean areaBuildableForCabin(GameMap m, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!inBounds(m, x, y)) return false;
                char t = m.tiles[y][x];
                if (t == '~' || t == '█') return false; // agua/roca
                if (t == '╔' || t == '╗' || t == '╚' || t == '╝' || t == '═' || t == '║' || t == '+') return false;
            }
        }
        return true;
    }

    private boolean areaBuildableForAttachment(GameMap m, int x0, int y0, int x1, int y1, int side) {
        for (int y = y0 - 1; y <= y1 + 1; y++) {
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                if (!inBounds(m, x, y)) return false;

                char t = m.tiles[y][x];
                if (t == '~' || t == '█') return false; // nunca sobre agua/roca

                boolean inRect = (x >= x0 && x <= x1 && y >= y0 && y <= y1);

                boolean sharedHalo = (side == 0 && x == x0 - 1) ||   // derecha → halo a la izquierda del nuevo
                        (side == 1 && x == x1 + 1) ||   // izquierda → halo a la derecha del nuevo
                        (side == 2 && y == y0 - 1) ||   // abajo → halo arriba del nuevo
                        (side == 3 && y == y1 + 1);     // arriba → halo abajo del nuevo
                if (!inRect && sharedHalo) continue;

                boolean wallish = (t == '╔' || t == '╗' || t == '╚' || t == '╝' || t == '═' || t == '║' || t == '+');

                if (inRect && wallish) return false;                       // dentro, no pisar paredes/puertas
                if (!inRect && (wallish || m.indoor[y][x]))
                    return false;  // en halo no compartido, no tocar interiores/paredes
            }
        }
        return true;
    }

    private boolean areaClearOfWater(GameMap m, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!inBounds(m, x, y) || m.tiles[y][x] == '~') return false;
            }
        }
        return true;
    }

    private static void paintFloor(GameMap m, int x, int y) {
        m.tiles[y][x] = '▓';
        m.walk[y][x] = true;
        m.transp[y][x] = true;
    }

    private static void paintDoor(GameMap m, int x, int y) {
        m.tiles[y][x] = '+';
        m.walk[y][x] = true;
        m.transp[y][x] = true;
    }

    private static void paintWall(GameMap m, int x, int y, char ch) {
        m.tiles[y][x] = ch;
        m.walk[y][x] = false;
        m.transp[y][x] = false;
    }

    private boolean inBounds(GameMap m, int x, int y) {
        return x >= 0 && y >= 0 && x < m.w && y < m.h;
    }

    private int dist2(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2, dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}
