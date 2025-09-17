package systems;

import game.GameState;
import items.EquipmentSlot;
import items.Item;
import render.Renderer;
import world.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Generación y dispersión de botín ('?') por el mapa.
 */
public final class LootSystem {
    private LootSystem() {
    }

    public static void scatterInitialLoot(GameState s, Renderer r) {
        // Limpiar posibles restos de loot (por si regeneramos)
        s.entities.removeIf(e -> e.type == Entity.Type.LOOT);

        // Catálogo de ítems “generoso”
        List<Item> pool = buildLootPool();

        // Cantidad objetivo según tamaño del mapa (denso pero sin saturar)
        int area = Math.max(1, s.map.w * s.map.h);
        int target = Math.min(200, Math.max(35, area / 500)); // ~0.2% del mapa, cap 200

        // Posiciones ya ocupadas por entidades para evitar solapamientos
        HashSet<Long> used = new HashSet<>();
        for (var e : s.entities) used.add(key(e.x, e.y));
        used.add(key(s.px, s.py)); // no spawnear bajo el jugador

        Random rng = s.rng;
        int placed = 0, tries = 0, maxTries = target * 20;

        while (placed < target && tries++ < maxTries && !pool.isEmpty()) {
            int x = rng.nextInt(s.map.w);
            int y = rng.nextInt(s.map.h);

            // Sólo en casilla transitable y lejos del jugador
            if (!s.map.walk[y][x]) continue;
            int dx = x - s.px, dy = y - s.py;
            if (dx * dx + dy * dy < 100) continue; // ≥ 10 tiles

            long k = key(x, y);
            if (used.contains(k)) continue;

            // Elegimos un ítem al azar de la pool
            Item it = pool.get(rng.nextInt(pool.size()));
            // Clon sencillo (mismo ítem “instancia” está bien por ahora)
            Entity loot = Entity.loot(x, y, it);
            s.entities.add(loot);

            used.add(k);
            placed++;
        }

        if (r != null) r.log("Se han dispersado " + placed + " objetos por la zona.");
    }

    private static long key(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }

    private static List<Item> buildLootPool() {
        ArrayList<Item> list = new ArrayList<>();

        // Bebida / comida
        list.add(Item.consumible("water_05", "Botella de agua (0.5 L)", 0.50, 0, 50, "Agua potable."));
        list.add(Item.consumible("water_05b", "Botella de agua (0.5 L)", 0.50, 0, 50, "Agua potable."));
        list.add(Item.consumible("soda_01", "Refresco azucarado", 0.33, 10, 20, "Demasiado dulce, pero ayuda."));
        list.add(Item.consumible("beans_02", "Lata de alubias", 0.35, 30, 0, "Alimento denso y calórico."));
        list.add(Item.consumible("bar_choco", "Barrita energética (chocolate)", 0.008, 15, 0, "Subidón de azúcar."));
        list.add(Item.consumible("bar_nuts", "Barrita energética (frutos)", 0.008, 20, 0, "Frutos secos y miel."));

        // Curación
        list.add(Item.curacion("bandage_clean", "Venda limpia", 0.03, 30, "Detiene sangrados."));
        list.add(Item.curacion("disinfect", "Antiséptico", 0.12, 15, "Desinfecta heridas superficiales."));

        // Herramientas / miscelánea
        list.add(Item.misc("lighter_red", "Mechero rojo", 0.005, "Fuente de fuego portátil."));
        list.add(Item.misc("battery_aa_2", "Pilas AA x2", 0.03, "Cargadas."));
        list.add(Item.misc("rope_02", "Cuerda (3 m)", 0.60, "Útil para atar/asegurar."));
        list.add(Item.misc("tape_01", "Cinta americana", 0.20, "Sirve para todo."));

        // Armas básicas
        list.add(Item.arma("knife_fold", "Navaja plegable", 0.12, 4, 0.9, 1, "Común, discreta."));
        list.add(Item.arma("kitchen_knife", "Cuchillo de cocina", 0.13, 5, 1.0, 1, "Corta que da gusto."));
        list.add(Item.arma("machete", "Machete", 0.55, 8, 1.2, 1, "Pesado pero eficaz."));

        // Prendas básicas
        list.add(Item.ropa("cap_black", "Gorra negra", 0.20, EquipmentSlot.HEAD, 1, 1, "Cubre algo del clima."));
        list.add(Item.armadura("gloves_work", "Guantes trabajo", 0.25, EquipmentSlot.HANDS, 1, 0, "Protegen cortes leves."));
        list.add(Item.ropa("boots_worn", "Botas gastadas", 0.90, EquipmentSlot.FEET, 1, 1, "Algo pesadas."));

        // Contenedores
        list.add(Item.mochila("bag_small", "Mochila pequeña", 0.55, 12.0, "Espacio limitado."));
        list.add(Item.mochila("bag_daypack", "Daypack", 0.80, 18.0, "Para un día largo."));

        // Repetimos algunos para que haya abundancia razonable
        ArrayList<Item> dense = new ArrayList<>(list);
        dense.addAll(list);
        dense.addAll(list);
        // deja el stack generoso
        return dense;
    }
}
