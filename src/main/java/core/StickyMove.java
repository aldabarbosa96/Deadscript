package core;

import game.GameState;
import render.Renderer;
import systems.PlayerSystem;
import ui.input.InputHandler;

import static game.Constants.*;

public class StickyMove {
    private long lastUpNs = 0, lastDownNs = 0, lastLeftNs = 0, lastRightNs = 0;
    private int stickDx = 0, stickDy = 0;
    private long stickUntilNs = 0L;

    public static final class Vec {
        public static final Vec ZERO = new Vec(0, 0);
        public final int dx, dy;

        public Vec(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    public boolean isActive() {
        return System.nanoTime() < stickUntilNs;
    }

    public Vec getAutoDir() {
        return isActive() ? new Vec(stickDx, stickDy) : Vec.ZERO;
    }

    public void reset() {
        stickUntilNs = 0L;
    }

    public boolean onArrow(InputHandler.Command c, GameState state, Renderer renderer) {
        long now = System.nanoTime();
        switch (c) {
            case UP -> lastUpNs = now;
            case DOWN -> lastDownNs = now;
            case LEFT -> lastLeftNs = now;
            case RIGHT -> lastRightNs = now;
            default -> {
            }
        }

        int dx = axisDx(c), dy = axisDy(c);
        int vx = dx, vy = dy;

        if (isActive()) {
            boolean changed = false;
            int ndx = stickDx, ndy = stickDy;
            if (dx != 0) {
                ndx = dx;
                ndy = 0;
                changed = true;
            }
            if (dy != 0) {
                ndy = dy;
                ndx = 0;
                changed = true;
            }
            if (changed) setSticky(ndx, ndy, now);
            else if (belongsToSticky(c)) renewSticky(now);

            vx = stickDx;
            vy = stickDy;
            state.lastDx = vx;
            state.lastDy = vy;
            return false; // Engine hará el auto-move en el loop
        }

        if (dy != 0) {
            int recentHx = recentHorizontalDir(now);
            if (recentHx != 0) {
                vx = recentHx;
                vy = dy;
                setSticky(vx, vy, now);
                state.lastDx = vx;
                state.lastDy = vy;
                return false;
            }
        } else if (dx != 0) {
            int recentVy = recentVerticalDir(now);
            if (recentVy != 0) {
                vx = dx;
                vy = recentVy;
                setSticky(vx, vy, now);
                state.lastDx = vx;
                state.lastDy = vy;
                return false;
            }
        }

        // Paso único inmediato si no hay sticky
        state.lastDx = vx;
        state.lastDy = vy;
        return PlayerSystem.tryMoveThrottled(state, vx, vy, renderer);
    }

    private static int axisDx(InputHandler.Command c) {
        return (c == InputHandler.Command.RIGHT) ? 1 : (c == InputHandler.Command.LEFT) ? -1 : 0;
    }

    private static int axisDy(InputHandler.Command c) {
        return (c == InputHandler.Command.DOWN) ? 1 : (c == InputHandler.Command.UP) ? -1 : 0;
    }

    private int recentHorizontalDir(long now) {
        if (now - lastRightNs <= COMBINE_WINDOW_NS) return 1;
        if (now - lastLeftNs <= COMBINE_WINDOW_NS) return -1;
        return 0;
    }

    private int recentVerticalDir(long now) {
        if (now - lastUpNs <= COMBINE_WINDOW_NS) return -1;
        if (now - lastDownNs <= COMBINE_WINDOW_NS) return 1;
        return 0;
    }

    private boolean belongsToSticky(InputHandler.Command c) {
        int dx = axisDx(c), dy = axisDy(c);
        if (dx != 0 && stickDx == dx) return true;
        if (dy != 0 && stickDy == dy) return true;
        return false;
    }

    private void setSticky(int dx, int dy, long now) {
        stickDx = dx;
        stickDy = dy;
        stickUntilNs = now + STICKY_RENEW_NS;
    }

    private void renewSticky(long now) {
        stickUntilNs = now + STICKY_RENEW_NS;
    }
}
