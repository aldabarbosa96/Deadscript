package utils;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class AudioLoop implements AutoCloseable {
    private Clip clip;
    private FloatControl gain;
    private Thread fadeThread;

    public AudioLoop(String resourcePath) {
        try {
            InputStream in = AudioLoop.class.getResourceAsStream(resourcePath);
            if (in == null) throw new IllegalArgumentException("Recurso no encontrado: " + resourcePath);

            try (BufferedInputStream bin = new BufferedInputStream(in); AudioInputStream ais = AudioSystem.getAudioInputStream(bin)) {

                clip = AudioSystem.getClip();
                clip.open(ais); // carga a memoria
            }

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando audio: " + resourcePath, e);
        }
    }


    public void setGainDb(float db) {
        if (gain == null) return;
        db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
        gain.setValue(db);
    }


    public void setGainLinear(float linear) {
        if (gain == null) return;
        linear = Math.max(0.0001f, Math.min(1f, linear));
        setGainDb((float) (20.0 * Math.log10(linear)));
    }

    public void start() {
        if (clip == null) return;
        clip.setLoopPoints(0, -1);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        if (!clip.isActive()) clip.start();
    }

    public void fadeToDb(float targetDb, long millis) {
        if (gain == null) return;
        stopFadeThread();
        fadeThread = new Thread(() -> {
            float start = gain.getValue();
            int steps = Math.max(1, (int) (millis / 20));
            for (int i = 1; i <= steps; i++) {
                float v = start + (targetDb - start) * (i / (float) steps);
                setGainDb(v);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "AudioFade");
        fadeThread.setDaemon(true);
        fadeThread.start();
    }

    private void stopFadeThread() {
        if (fadeThread != null) {
            fadeThread.interrupt();
            fadeThread = null;
        }
    }

    public void shutdownNow() {
        stopFadeThread();
        try {
            if (clip != null) {
                try {
                    clip.loop(0);
                } catch (Exception ignore) {
                }
                try {
                    clip.stop();
                } catch (Exception ignore) {
                }
                try {
                    clip.flush();
                } catch (Exception ignore) {
                }
                try {
                    clip.setFramePosition(0);
                } catch (Exception ignore) {
                }
                try {
                    clip.close();
                } catch (Exception ignore) {
                }
            }
        } finally {
            clip = null;
            gain = null;
        }
    }

    @Override
    public void close() {
        shutdownNow();
    }
}
