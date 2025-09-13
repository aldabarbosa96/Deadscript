package core;

import java.util.function.DoubleConsumer;

public class Clock {
    // 60 Hz de sim; render mínimo cada 200ms y repintado 1/seg para reloj
    private static final double FIXED_DT = 1.0 / 60.0;
    private static final long   RENDER_MIN_INTERVAL_NS = 200_000_000L;

    private long  prevNs;
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
        if (nowSec != lastRenderSec) return true; // tick del reloj en HUD
        return false;
    }

    public void onRendered() {
        lastRenderNs = System.nanoTime();
        lastRenderSec = System.currentTimeMillis() / 1000L;
    }
}
