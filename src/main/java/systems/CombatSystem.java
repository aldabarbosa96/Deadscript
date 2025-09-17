package systems;

import game.GameState;
import items.Item;
import render.Renderer;
import world.Entity;

public final class CombatSystem {
    private CombatSystem() {
    }

    public static boolean anyVisibleHostileInMainFov(GameState s, Renderer r) {
        for (Entity e : s.entities) {
            if ((e.type == Entity.Type.ZOMBIE || e.glyph == 'Z') && r.wasVisibleLastRender(e.x, e.y)) return true;
        }
        return false;
    }

    public static boolean quickAttack(GameState s, Renderer r) {
        Entity target = pickAdjacentZombie(s);
        if (target == null) {
            drainEnergy(s, 3);
            r.log("Golpeas al aire. Te cansas.");
            return true;
        }

        Item weapon = chooseBestWeapon(s);
        double energyRatio = ratio(s.energia, s.maxEnergia);

        double baseCd = weapon != null ? Math.max(0.0, weapon.getWeapon().cooldownSec()) : 0.0;
        double cd = baseCd * (1.0 + 1.2 * (1.0 - energyRatio));
        long now = System.nanoTime();
        if (cd > 0) {
            long needNs = (long) (cd * 1_000_000_000L);
            if (now - s.lastPlayerAttackNs < needNs) {
                r.log("Necesitas un instante para volver a atacar.");
                return false;
            }
        }

        int dmg = computeDamage(s, weapon, energyRatio);
        drainEnergy(s, weapon != null ? 5 : 3);
        if (weapon != null) weapon.consumirDurabilidad(1 + s.rng.nextInt(3));
        s.lastPlayerAttackNs = now;

        target.hp = Math.max(0, target.hp - dmg);
        if (target.hp <= 0) {
            s.entities.remove(target);
            String wname = weapon != null ? weapon.getNombre() : "tus puños";
            r.log("Golpeas con " + wname + " y matas al zombie.");
            return true;
        } else {
            r.log("Impactas y causas " + dmg + " de daño.");
            return true;
        }
    }

    private static int computeDamage(GameState s, Item weapon, double energyRatio) {
        if (weapon == null || weapon.getWeapon() == null) {
            int base = 1 + s.rng.nextInt(3);
            double ef = 0.6 + 0.4 * energyRatio;
            return Math.max(1, (int) Math.round(base * ef));
        }
        double base = weapon.getWeapon().danho();
        double rand = 0.85 + 0.30 * s.rng.nextDouble();
        double dur = Math.max(0.0, Math.min(1.0, weapon.getDurabilidadPct() / 100.0));
        double q = 0.5 + 0.5 * dur;
        double ef = 0.6 + 0.4 * energyRatio;
        double out = base * rand * q * ef;
        return Math.max(1, (int) Math.round(out));
    }

    public static Entity pickAdjacentZombie(GameState s) {
        int[][] dirs = new int[][]{{s.lastDx, s.lastDy}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : dirs) {
            int dx = d[0], dy = d[1];
            if (dx == 0 && dy == 0) continue;
            int tx = s.px + dx, ty = s.py + dy;
            for (Entity e : s.entities) {
                if ((e.type == Entity.Type.ZOMBIE || e.glyph == 'Z') && e.x == tx && e.y == ty) return e;
            }
        }
        return null;
    }

    public static boolean anyAdjacentZombie(GameState s) {
        return pickAdjacentZombie(s) != null;
    }

    private static Item chooseBestWeapon(GameState s) {
        Item main = s.equipment.getMainHand();
        Item off = s.equipment.getOffHand();
        double best = -1;
        Item pick = null;
        if (main != null && main.getWeapon() != null) {
            double sc = main.getWeapon().danho() * Math.max(0, main.getDurabilidadPct());
            if (sc > best) {
                best = sc;
                pick = main;
            }
        }
        if (off != null && off.getWeapon() != null) {
            double sc = off.getWeapon().danho() * Math.max(0, off.getDurabilidadPct());
            if (sc > best) {
                best = sc;
                pick = off;
            }
        }
        return pick;
    }

    private static void drainEnergy(GameState s, double pts) {
        s.energiaAcc = clamp(s.energiaAcc - pts, 0, s.maxEnergia);
        s.energia = (int) Math.round(s.energiaAcc);
    }

    private static double ratio(int v, int max) {
        int m = Math.max(1, max);
        int val = Math.max(0, Math.min(v, m));
        return val / (double) m;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
