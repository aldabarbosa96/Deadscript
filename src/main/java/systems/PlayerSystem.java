package systems;

import game.GameState;
import game.Constants;
import render.Renderer;

public final class PlayerSystem {
    private PlayerSystem() {
    }

    public static boolean tryMoveThrottled(GameState s, int dx, int dy, Renderer r) {
        long now = System.nanoTime();
        if (now - s.lastPlayerStepNs < Constants.PLAYER_MOVE_COOLDOWN_NS) return false;
        boolean moved = tryMove(s, dx, dy, r);
        if (moved) s.lastPlayerStepNs = now;
        return moved;
    }

    private static boolean tryMove(GameState s, int dx, int dy, Renderer r) {
        int nx = s.px + dx, ny = s.py + dy;
        if (nx < 0 || ny < 0 || nx >= s.map.w || ny >= s.map.h) {
            r.log("No puedes salir del mapa.");
            return false;
        }

        if (dx != 0 && dy != 0) {
            boolean okX = (s.px + dx >= 0 && s.px + dx < s.map.w) && s.map.walk[s.py][s.px + dx];
            boolean okY = (s.py + dy >= 0 && s.py + dy < s.map.h) && s.map.walk[s.py + dy][s.px];
            if (!okX && !okY) {
                r.log("Hay un obstáculo bloqueando el paso.");
                return false;
            }
        }

        if (!s.map.walk[ny][nx]) {
            r.log("Hay un obstáculo bloqueando el paso.");
            return false;
        }

        s.px = nx;
        s.py = ny;
        s.lastDx = dx;
        s.lastDy = dy;
        return true;
    }


    public static void drainNeeds(GameState s, double dt) {
        double peso = s.equipment.pesoTotalKg(s.inventory);
        double cap = s.equipment.capacidadKg();
        double exceso = Math.max(0.0, peso - cap);
        double multFatiga = Math.min(3.0, 1.0 + 0.25 * exceso);

        s.hambreAcc = clamp(s.hambreAcc - dt * 0.02, 0, s.maxHambre);
        s.sedAcc = clamp(s.sedAcc - dt * 0.05, 0, s.maxSed);
        s.suenoAcc = clamp(s.suenoAcc - dt * 0.01, 0, s.maxSueno);
        s.energiaAcc = clamp(s.energiaAcc - dt * 0.03 * multFatiga, 0, s.maxEnergia);

        s.hambre = (int) Math.round(s.hambreAcc);
        s.sed = (int) Math.round(s.sedAcc);
        s.sueno = (int) Math.round(s.suenoAcc);
        s.energia = (int) Math.round(s.energiaAcc);
    }


    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
