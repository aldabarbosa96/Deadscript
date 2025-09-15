package systems;

import game.Constants;
import game.GameState;
import items.Item;
import render.Renderer;
import world.Entity;

import java.util.HashMap;
import java.util.Map;

public final class ZombieSystem {
    private ZombieSystem() {
    }

    public static boolean update(GameState s, Renderer r, double dt) {
        boolean touched = false;

        // Spawns
        s.spawnTimer += dt;
        if (s.spawnTimer >= Constants.SPAWN_EVERY_SEC) {
            s.spawnTimer = 0.0;
            if (trySpawnGroup(s, r)) touched = true;
        }

        // Movimiento + golpes
        Map<Integer, Entity> leaders = new HashMap<>();
        for (Entity e : s.entities) if (e.leader) leaders.put(e.groupId, e);

        for (var e : s.entities) {
            int beforeX = e.x, beforeY = e.y;

            // objetivo
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

            // ataque
            if (e.attackCooldown > 0) e.attackCooldown -= dt;
            if (e.x == s.px && e.y == s.py && e.attackCooldown <= 0) {
                int prot = Math.max(0, s.equipment.proteccionTotal());
                int base = Constants.ZOMBIE_HIT_DAMAGE;
                int dmg = Math.max(1, base - prot);
                s.salud = Math.max(0, s.salud - dmg);
                e.attackCooldown = Constants.ZOMBIE_ATTACK_COOLDOWN_SEC;
                r.log("¡Un zombi te ha golpeado!");

                for (Item part : new Item[]{s.equipment.getHead(), s.equipment.getChest(), s.equipment.getHands(), s.equipment.getLegs(), s.equipment.getFeet()}) {
                    if (part != null && part.getArmor() != null) part.consumirDurabilidad(1 + s.rng.nextInt(2));
                }
                touched = true;
            }
        }
        return touched;
    }

    private static boolean trySpawnGroup(GameState s, Renderer r) {
        if (s.entities.size() >= Constants.MAX_ZOMBIES) return false;

        int size = 1 + s.rng.nextInt(5); // 1..5
        double ang = s.rng.nextDouble() * Math.PI * 2.0;
        int dist = Constants.SPAWN_RADIUS_MIN + s.rng.nextInt(Math.max(1, Constants.SPAWN_RADIUS_MAX - Constants.SPAWN_RADIUS_MIN + 1));

        int ax = s.px + (int) Math.round(Math.cos(ang) * dist);
        int ay = s.py + (int) Math.round(Math.sin(ang) * dist);
        if (ax < 0 || ay < 0 || ax >= s.map.w || ay >= s.map.h) return false;

        int groupId = s.nextGroupId++;
        boolean anyNear = false;

        for (int i = 0; i < size && s.entities.size() < Constants.MAX_ZOMBIES; i++) {
            int rx = ax + s.rng.nextInt(5) - 2;
            int ry = ay + s.rng.nextInt(5) - 2;
            if (rx < 0 || ry < 0 || rx >= s.map.w || ry >= s.map.h) continue;
            if (!s.map.walk[ry][rx]) continue;

            double speed = Constants.ZOMBIE_MIN_SPEED + s.rng.nextDouble() * Constants.ZOMBIE_SPEED_RANGE;

            Entity z = new Entity(rx, ry, 'Z', speed);
            z.groupId = groupId;
            z.leader = (i == 0);
            if (!z.leader) {
                z.offX = s.rng.nextInt(5) - 2;
                z.offY = s.rng.nextInt(5) - 2;
            }
            s.entities.add(z);

            if (r.isNearCamera(rx, ry, s)) anyNear = true;
        }
        return anyNear;
    }
}
