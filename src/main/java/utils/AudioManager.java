package utils;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class AudioManager {
    private AudioManager() {
    }

    private static final long UI_COOLDOWN_NS = 120_000_000L; // 120 ms
    private static final float UI_GAIN_DB = -20.0f;

    private static final ConcurrentHashMap<String, Clip> SFX = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> SFXLAST = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Clip> LOOPS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FloatControl> LOOPGAIN = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Thread> LOOPFADES = new ConcurrentHashMap<>();

    public static void playUi(String resourcePath) {
        playSfx(resourcePath, true, UI_GAIN_DB, UI_COOLDOWN_NS);
    }

    public static void playSfx(String resourcePath) {
        playSfx(resourcePath, false, 0f, 0L);
    }

    public static void playSfx(String resourcePath, boolean noOverlap, float gainDb, long cooldownNs) {
        long now = System.nanoTime();
        if (cooldownNs > 0) {
            Long last = SFXLAST.get(resourcePath);
            if (last != null && now - last < cooldownNs) return;
            SFXLAST.put(resourcePath, now);
        }

        Clip clip = SFX.computeIfAbsent(resourcePath, AudioManager::loadClipPcm16);
        if (clip == null) return;

        synchronized (clip) {
            if (noOverlap && clip.isActive()) clip.stop();
            clip.setFramePosition(0);
            setGain(clip, gainDb);
            clip.start();
        }
    }

    public static void startLoop(String name, String resourcePath, float gainDb) {
        Clip prev = LOOPS.get(name);
        if (prev == null) {
            Clip c = loadClipPcm16(resourcePath);
            if (c == null) return;
            LOOPS.put(name, c);
            if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                LOOPGAIN.put(name, (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN));
                setLoopGainDb(name, gainDb);
            }
            c.setLoopPoints(0, -1);
            c.loop(Clip.LOOP_CONTINUOUSLY);
            if (!c.isActive()) c.start();
        } else {
            // si ya existe, solo ajusta gain y asegura activo
            setLoopGainDb(name, gainDb);
            if (!prev.isActive()) {
                prev.setLoopPoints(0, -1);
                prev.loop(Clip.LOOP_CONTINUOUSLY);
                prev.start();
            }
        }
    }

    public static void setLoopGainDb(String name, float db) {
        FloatControl g = LOOPGAIN.get(name);
        if (g == null) return;
        db = Math.max(g.getMinimum(), Math.min(g.getMaximum(), db));
        g.setValue(db);
    }

    public static void fadeLoopToDb(String name, float targetDb, long millis) {
        FloatControl g = LOOPGAIN.get(name);
        Clip c = LOOPS.get(name);
        if (g == null || c == null) return;

        Thread old = LOOPFADES.get(name);
        if (old != null) old.interrupt();

        Thread t = new Thread(() -> {
            float start = g.getValue();
            int steps = Math.max(1, (int) (millis / 20));
            for (int i = 1; i <= steps; i++) {
                float v = start + (targetDb - start) * (i / (float) steps);
                try {
                    v = Math.max(g.getMinimum(), Math.min(g.getMaximum(), v));
                    g.setValue(v);
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                    break;
                } catch (IllegalArgumentException ignored) {
                    break;
                }
            }
        }, "AudioFade-" + name);
        t.setDaemon(true);
        LOOPFADES.put(name, t);
        t.start();
    }

    public static void stopLoop(String name) {
        Thread f = LOOPFADES.remove(name);
        if (f != null) f.interrupt();
        Clip c = LOOPS.remove(name);
        LOOPGAIN.remove(name);
        if (c != null) {
            try {
                c.loop(0);
            } catch (Exception ignored) {
            }
            try {
                c.stop();
            } catch (Exception ignored) {
            }
            try {
                c.flush();
            } catch (Exception ignored) {
            }
            try {
                c.setFramePosition(0);
            } catch (Exception ignored) {
            }
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static Clip loadClipPcm16(String resourcePath) {
        try {
            InputStream raw = Objects.requireNonNull(AudioManager.class.getResourceAsStream(resourcePath), "Recurso no encontrado: " + resourcePath);
            try (BufferedInputStream bin = new BufferedInputStream(raw)) {
                AudioInputStream in = AudioSystem.getAudioInputStream(bin);
                AudioFormat src = in.getFormat();
                AudioFormat dst = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16, src.getChannels(), src.getChannels() * 2, src.getSampleRate(), false);
                AudioInputStream din = AudioSystem.getAudioInputStream(dst, in);
                Clip clip = AudioSystem.getClip();
                clip.open(din);
                din.close();
                in.close();
                return clip;
            }
        } catch (Exception e) {
            System.err.println("Audio load error: " + resourcePath + " -> " + e);
            return null;
        }
    }

    private static void setGain(Clip clip, float gainDb) {
        try {
            FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float v = Math.max(c.getMinimum(), Math.min(c.getMaximum(), gainDb));
            c.setValue(v);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void shutdown() {
        // loops
        for (String k : LOOPS.keySet().toArray(new String[0])) stopLoop(k);
        // sfx
        for (Clip c : SFX.values())
            try {
                c.close();
            } catch (Exception ignored) {
            }
        SFX.clear();
        SFXLAST.clear();
    }
}
