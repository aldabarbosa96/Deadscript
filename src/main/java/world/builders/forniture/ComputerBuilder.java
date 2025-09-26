package world.builders.forniture;

import world.GameMap;

import java.util.*;

public final class ComputerBuilder {
    private ComputerBuilder() {
    }

    public static final char COMPUTER = '©';
    private static final char BED = 'b';

    public static void placeComputersNearBeds(GameMap m, Random rng, int x0, int y0, int x1, int y1) {
        if (m == null || rng == null) return;

        int minX = Math.max(1, x0), minY = Math.max(1, y0);
        int maxX = Math.min(m.w - 2, x1), maxY = Math.min(m.h - 2, y1);

        boolean[][] used = new boolean[m.h][m.w];

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (m.tiles[y][x] != BED) continue;

                // Detectar origen de pareja
                int bx, by;
                if (inBounds(m, x + 1, y) && m.tiles[y][x + 1] == BED && !(inBounds(m, x - 1, y) && m.tiles[y][x - 1] == BED)) {
                    bx = x + 1;
                    by = y;
                } else if (inBounds(m, x, y + 1) && m.tiles[y + 1][x] == BED && !(inBounds(m, x, y - 1) && m.tiles[y - 1][x] == BED)) {
                    bx = x;
                    by = y + 1;
                } else continue;

                int[][] beds = new int[][]{{x, y}, {bx, by}};

                // Mejor candidato global para esta pareja
                int bestPCX = -1, bestPCY = -1, bestDist = Integer.MAX_VALUE;
                // Si encontramos dist==3 (ideal), colocamos y salimos
                boolean placed = false;

                for (int[] b : beds) {
                    // 4 direcciones (sin barajar: vamos a elegir mínimo)
                    int[][] dirs = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

                    for (int[] d : dirs) {
                        int dx = d[0], dy = d[1];

                        // 1) HUECO inmediato junto a la cama (para la mesita)
                        int gx = b[0] + dx, gy = b[1] + dy;
                        if (!inBounds(m, gx, gy) || !isFreeFloor(m, gx, gy)) continue;

                        // 2) Escanear hasta la pared exterior del casco
                        int t = 2; // distancia desde la cama al tile que inspeccionamos
                        while (true) {
                            int tx = b[0] + t * dx, ty = b[1] + t * dy;
                            if (!inBounds(m, tx, ty)) break;

                            char tt = m.tiles[ty][tx];

                            if (isShellWallChar(tt)) {
                                // Debe ser EXTERIOR
                                if (!isExteriorWallAt(m, tx, ty, dx, dy)) break;
                                int pcx = tx - dx, pcy = ty - dy;  // PC pegado a esa pared
                                int dist = t; // cama→pared

                                if (!inBounds(m, pcx, pcy) || !isFreeFloor(m, pcx, pcy)) break;
                                if (nearDoor(m, pcx, pcy) || isStair(m, pcx, pcy) || used[pcy][pcx]) break;

                                // Prioridad absoluta: pared a 3 (cama→hueco→PC→pared)
                                if (dist == 3) {
                                    paintComputer(m, pcx, pcy);
                                    used[pcy][pcx] = true;
                                    placed = true;
                                } else if (dist < bestDist) {
                                    bestDist = dist;
                                    bestPCX = pcx;
                                    bestPCY = pcy;
                                }
                                break; // pared encontrada en esta dirección
                            }

                            // Si antes de la pared hay algo que no es suelo interior libre → abortar dirección
                            if (!(tt == '▓' && m.indoor[ty][tx] && m.walk[ty][tx])) break;

                            t++;
                        }
                        if (placed) break;
                    }
                    if (placed) break;
                }

                // Si no hubo dist==3 pero sí había pared más cercana válida, usarla
                if (!placed && bestPCX >= 0) {
                    paintComputer(m, bestPCX, bestPCY);
                    used[bestPCY][bestPCX] = true;
                }
            }
        }
    }

    private static boolean isShellWallChar(char t) {
        return t == '╔' || t == '╗' || t == '╚' || t == '╝' || t == '═' || t == '║';
    }

    private static boolean isExteriorWallAt(GameMap m, int wx, int wy, int dx, int dy) {
        if (!inBounds(m, wx, wy) || !isShellWallChar(m.tiles[wy][wx])) return false;
        int ox = wx + dx, oy = wy + dy;
        if (!inBounds(m, ox, oy)) return true;
        return !m.indoor[oy][ox];
    }

    private static boolean isFreeFloor(GameMap m, int x, int y) {
        return inBounds(m, x, y) && m.indoor[y][x] && m.tiles[y][x] == '▓' && m.walk[y][x];
    }

    private static boolean nearDoor(GameMap m, int x, int y) {
        return touchesTile(m, x, y, '+');
    }

    private static boolean isStair(GameMap m, int x, int y) {
        return inBounds(m, x, y) && m.tiles[y][x] == 'S';
    }

    private static boolean touchesTile(GameMap m, int x, int y, char t) {
        int[][] d = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] v : d) {
            int nx = x + v[0], ny = y + v[1];
            if (inBounds(m, nx, ny) && m.tiles[ny][nx] == t) return true;
        }
        return false;
    }

    private static boolean inBounds(GameMap m, int x, int y) {
        return x >= 0 && y >= 0 && x < m.w && y < m.h;
    }

    private static void paintComputer(GameMap m, int x, int y) {
        m.tiles[y][x] = COMPUTER;
        m.walk[y][x] = false;
        m.transp[y][x] = true;
    }
}
