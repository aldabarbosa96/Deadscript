package world.builders;

import world.GameMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public final class WellBuilder {
    public static final char WELL_CHAR = 'Û';

    public void build(GameMap m, Random rng, int count) {
        if (count < 2) return; // siempre en parejas
        final int targetPairs = count / 2; // nº de parejas a colocar

        boolean[][] used = new boolean[m.h][m.w];
        int placedPairs = 0;

        // Pase A: exterior, suelo '▓', SIN carretera
        ArrayList<int[]> pairsA = collectHorizontalPairs(m, /*allowRoad*/ false);
        Collections.shuffle(pairsA, rng);
        placedPairs += placePairs(m, pairsA, used, targetPairs - placedPairs, /*clearRoad*/ false);

        // Pase B (fallback): exterior, suelo '▓', PERMITIENDO carretera
        if (placedPairs < targetPairs) {
            ArrayList<int[]> pairsB = collectHorizontalPairs(m, /*allowRoad*/ true);
            Collections.shuffle(pairsB, rng);
            placedPairs += placePairs(m, pairsB, used, targetPairs - placedPairs, /*clearRoad*/ true);
        }
        // Resultado: 2*placedPairs pozos colocados en parejas horizontales.
    }

    // Recolecta SOLO parejas horizontales (x,y)-(x+1,y) que cumplan las reglas
    private static ArrayList<int[]> collectHorizontalPairs(GameMap m, boolean allowRoad) {
        ArrayList<int[]> list = new ArrayList<>();
        for (int y = 1; y < m.h - 1; y++) {
            for (int x = 1; x < m.w - 2; x++) { // x+1 seguro dentro
                if (canCell(m, x, y, allowRoad) && canCell(m, x + 1, y, allowRoad)) {
                    list.add(new int[]{x, y, x + 1, y});
                }
            }
        }
        return list;
    }

    private static boolean canCell(GameMap m, int x, int y, boolean allowRoad) {
        if (m.indoor[y][x]) return false;
        if (m.tiles[y][x] != '▓') return false;
        if (!allowRoad && m.road[y][x]) return false;
        if (m.tiles[y][x] == WELL_CHAR) return false;
        return true;
    }

    private static int placePairs(GameMap m, ArrayList<int[]> pairs, boolean[][] used, int neededPairs, boolean clearRoad) {
        int placed = 0;
        for (int i = 0; i < pairs.size() && placed < neededPairs; i++) {
            int[] p = pairs.get(i);
            int x1 = p[0], y1 = p[1], x2 = p[2], y2 = p[3];

            // Evita solapamientos: ninguna celda usada ni ya con pozo
            if (used[y1][x1] || used[y2][x2]) continue;
            if (m.tiles[y1][x1] == WELL_CHAR || m.tiles[y2][x2] == WELL_CHAR) continue;

            if (clearRoad) { // en fallback, limpiamos marca de carretera si la hubiera
                m.road[y1][x1] = false;
                m.road[y2][x2] = false;
            }

            placeWell(m, x1, y1);
            placeWell(m, x2, y2);

            used[y1][x1] = true;
            used[y2][x2] = true;

            placed++;
        }
        return placed;
    }

    private static void placeWell(GameMap m, int x, int y) {
        m.tiles[y][x] = WELL_CHAR;
        m.walk[y][x] = false;
        m.transp[y][x] = true; // ponlo a false si quieres que bloquee visión
    }
}
