package systems;

import game.Constants;
import game.GameState;
import render.Renderer;
import utils.AudioManager;
import world.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ZombieSystem {
    private ZombieSystem() {
    }

    // --- Estado de rugidos por-zombi (se limpia solo al GC si mueren/desaparecen) ---
    private static final WeakHashMap<Entity, RoarState> ROARS = new WeakHashMap<>();

    private static final class RoarState {
        double timerSec; // cuenta atrás hasta el próximo rugido

        RoarState(double t) {
            this.timerSec = t;
        }
    }

    // Intervalo entre rugidos de un zombi (en segundos). Ajusta a gusto.
    private static double nextInterval(GameState s) {
        // 1.6s .. 4.4s (dispersión suficiente para que no suenen “a la vez”)
        return 1.6 + s.rng.nextDouble() * 2.8;
    }

    // Retardo inicial cuando un zombi pasa a ser visible (para desincronizar)
    private static double initialDelay(GameState s) {
        // 0.2s .. 1.4s
        return 0.2 + s.rng.nextDouble() * 1.2;
    }

    public static boolean update(GameState s, Renderer r, double dt) {
        boolean touched = false;

        // Spawns
        s.spawnTimer += dt;
        if (s.spawnTimer >= Constants.SPAWN_EVERY_SEC) {
            s.spawnTimer = 0.0;
            if (trySpawnGroup(s, r)) touched = true;
        }

        // Indexar líderes por grupo para offsets
        Map<Integer, Entity> leaders = new HashMap<>();
        for (Entity e : s.entities) {
            if (e.type == Entity.Type.ZOMBIE && e.leader) {
                leaders.put(e.groupId, e);
            }
        }

        // Actualizar zombis
        for (Entity e : s.entities) {
            if (e.type != Entity.Type.ZOMBIE) continue;

            int beforeX = e.x, beforeY = e.y;

            // Objetivo (jugador o líder)
            int tx, ty;
            if (e.leader) {
                tx = s.px;
                ty = s.py;
            } else {
                Entity lead = leaders.get(e.groupId);
                if (lead == null) {
                    tx = s.px;
                    ty = s.py;
                } else {
                    tx = lead.x + e.offX;
                    ty = lead.y + e.offY;
                }
            }

            int stepX = Integer.compare(tx - e.x, 0);
            int stepY = Integer.compare(ty - e.y, 0);

            double tiles = e.speedTilesPerSec * dt + e.moveRemainder;
            while (tiles >= 1.0) {
                tiles -= 1.0;
                int nx = e.x, ny = e.y;
                if (stepX != 0 && stepY != 0) {
                    if (s.rng.nextBoolean()) nx += stepX;
                    else ny += stepY;
                } else if (stepX != 0) nx += stepX;
                else if (stepY != 0) ny += stepY;

                if (nx >= 0 && ny >= 0 && nx < s.map.w && ny < s.map.h && s.map.walk[ny][nx]) {
                    e.x = nx;
                    e.y = ny;
                } else break;
            }
            e.moveRemainder = tiles;

            if (beforeX != e.x || beforeY != e.y) {
                boolean wasOnCam = r.isNearCamera(beforeX, beforeY, s);
                boolean nowOnCam = r.isNearCamera(e.x, e.y, s);
                boolean wasVis = r.wasVisibleLastRender(beforeX, beforeY);
                boolean nowVis = r.wasVisibleLastRender(e.x, e.y);
                if (wasOnCam || nowOnCam || wasVis || nowVis) touched = true;
            }

            // Ataque
            if (e.attackCooldown > 0) e.attackCooldown -= dt;
            if (e.x == s.px && e.y == s.py && e.attackCooldown <= 0) {
                int prot = Math.max(0, s.equipment.proteccionTotal());
                int base = Constants.ZOMBIE_HIT_DAMAGE;
                int dmg = Math.max(1, base - prot);
                s.salud = Math.max(0, s.salud - dmg);
                e.attackCooldown = Constants.ZOMBIE_ATTACK_COOLDOWN_SEC;
                r.log("¡Un zombi te ha golpeado!");

                for (items.Item part : new items.Item[]{s.equipment.getHead(), s.equipment.getChest(), s.equipment.getHands(), s.equipment.getLegs(), s.equipment.getFeet()}) {
                    if (part != null && part.getArmor() != null) part.consumirDurabilidad(1 + s.rng.nextInt(2));
                }
                touched = true;
            }

            // --- Audio por-zombi: bucle independiente mientras sea visible ---
            boolean visible = r.wasVisibleLastRender(e.x, e.y);
            if (visible) {
                RoarState st = ROARS.get(e);
                if (st == null) {
                    st = new RoarState(initialDelay(s));
                    ROARS.put(e, st);
                }
                st.timerSec -= dt;
                if (st.timerSec <= 0.0) {
                    // Elección puramente aleatoria: puede repetirse el mismo consecutivamente
                    String path = s.rng.nextBoolean() ? "/audio/zombieRoar1.wav" : "/audio/zombieRoar2.wav";
                    try {
                        // Usamos el canal de SFX/UI existente
                        AudioManager.playUi(path);
                    } catch (Throwable ignored) {
                    }
                    st.timerSec = nextInterval(s);
                }
            } else {
                // Al dejar de ser visible, paramos su “bucle lógico”
                ROARS.remove(e);
            }
        }

        return touched;
    }

    private static boolean trySpawnGroup(GameState s, Renderer r) {
        int curZ = countZombies(s);
        if (curZ >= Constants.MAX_ZOMBIES) return false;
        int capacity = Math.max(0, Constants.MAX_ZOMBIES - curZ);
        int size = Math.min(capacity, 1 + s.rng.nextInt(5));

        double ang = s.rng.nextDouble() * Math.PI * 2.0;
        int dist = Constants.SPAWN_RADIUS_MIN + s.rng.nextInt(Math.max(1, Constants.SPAWN_RADIUS_MAX - Constants.SPAWN_RADIUS_MIN + 1));

        int ax = s.px + (int) Math.round(Math.cos(ang) * dist);
        int ay = s.py + (int) Math.round(Math.sin(ang) * dist);
        if (ax < 0 || ay < 0 || ax >= s.map.w || ay >= s.map.h) return false;

        int groupId = s.nextGroupId++;
        boolean anyNear = false;

        for (int i = 0; i < size; i++) {
            int rx = ax + s.rng.nextInt(5) - 2;
            int ry = ay + s.rng.nextInt(5) - 2;
            if (rx < 0 || ry < 0 || rx >= s.map.w || ry >= s.map.h) continue;
            if (!s.map.walk[ry][rx]) continue;

            double speed = Constants.ZOMBIE_MIN_SPEED + s.rng.nextDouble() * Constants.ZOMBIE_SPEED_RANGE;

            Entity z = new Entity(rx, ry, 'Z', speed);
            z.type = Entity.Type.ZOMBIE;
            z.groupId = groupId;
            z.leader = (i == 0);
            if (!z.leader) {
                z.offX = s.rng.nextInt(5) - 2;
                z.offY = s.rng.nextInt(5) - 2;
            }
            int hp = 10 + s.rng.nextInt(6);
            z.maxHp = hp;
            z.hp = hp;

            s.entities.add(z);
            if (r.isNearCamera(rx, ry, s)) anyNear = true;
        }
        return anyNear;
    }

    private static int countZombies(GameState s) {
        int z = 0;
        for (Entity e : s.entities) if (e.type == Entity.Type.ZOMBIE) z++;
        return z;
    }
}
