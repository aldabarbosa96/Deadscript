package systems;

import game.GameState;
import items.Items;
import items.Item;
import render.Renderer;
import world.Entity;

import java.util.HashSet;
import java.util.Random;

public final class LootSystem {
    private LootSystem() {
    }

    public static void scatterInitialLoot(GameState s, Renderer r) {
        s.entities.removeIf(e -> e.type == Entity.Type.LOOT);

        int area = Math.max(1, s.map.w * s.map.h);
        int target = Math.min(200, Math.max(35, area / 500));

        HashSet<Long> used = new HashSet<>();
        for (var e : s.entities) used.add(key(e.x, e.y));
        used.add(key(s.px, s.py)); // no spawneamos bajo el jugador

        Random rng = s.rng;
        int placed = 0, tries = 0, maxTries = target * 30;

        if (!Items.anyLootRemaining(s.worldSpawnedByItem)) {
            if (r != null) r.log("No queda loot disponible (cupos agotados).");
            return;
        }

        while (placed < target && tries++ < maxTries) {
            // 1) Posición
            int x = rng.nextInt(s.map.w);
            int y = rng.nextInt(s.map.h);
            if (!s.map.walk[y][x]) continue;
            int dx = x - s.px, dy = y - s.py;
            if (dx * dx + dy * dy < 100) continue;

            long k = key(x, y);
            if (used.contains(k)) continue;

            // 2) Sorteo de ítem con cupos restantes
            String id = Items.pickRandomLootId(rng, s.worldSpawnedByItem);
            if (id == null) break; // no queda nada que sortear

            Item it = Items.create(id);

            // 3) Colocar y contar
            Entity loot = Entity.loot(x, y, it);
            s.entities.add(loot);
            used.add(k);
            placed++;

            s.worldSpawnedByItem.merge(id, 1, Integer::sum);
        }

        if (r != null) r.log("Se han dispersado " + placed + " objetos por la zona.");
    }

    private static long key(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }
}
