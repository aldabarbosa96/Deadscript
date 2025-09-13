package core;

import java.util.function.DoubleConsumer;

import static game.Constants.*;

public class Clock {
    private long prevNs;
    private double lag;
    private long lastRenderNs;
    private long lastRenderSec = -1;

    public void start() {
        prevNs = System.nanoTime();
        lastRenderNs = prevNs;
        lag = 0;
    }

    public void consumeFixedSteps(DoubleConsumer step) {
        long now = System.nanoTime();
        double dt = (now - prevNs) / 1_000_000_000.0;
        if (dt < 0) dt = 0;
        if (dt > 0.25) dt = 0.25;
        prevNs = now;

        lag += dt;
        while (lag >= FIXED_DT) {
            step.accept(FIXED_DT);
            lag -= FIXED_DT;
        }
    }

    public boolean shouldRender(boolean dirty) {
        long nowNs = System.nanoTime();
        long nowSec = System.currentTimeMillis() / 1000L;

        if (dirty && (nowNs - lastRenderNs) >= RENDER_MIN_INTERVAL_NS) return true;
        return nowSec != lastRenderSec; // tick del reloj en HUD
    }

    public void onRendered() {
        lastRenderNs = System.nanoTime();
        lastRenderSec = System.currentTimeMillis() / 1000L;
    }
}
