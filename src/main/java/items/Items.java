package items;

import java.util.*;
import java.util.function.Supplier;


public final class Items {

    public static final class Def {
        public final String id;
        public final int weight;
        public final int maxWorld;
        private final Supplier<Item> factory;

        private Def(String id, int weight, int maxWorld, Supplier<Item> factory) {
            this.id = id;
            this.weight = Math.max(0, weight);
            this.maxWorld = Math.max(0, maxWorld);
            this.factory = Objects.requireNonNull(factory);
        }

        public Item create() {
            return factory.get();
        }
    }

    private static final Map<String, Def> REG = new LinkedHashMap<>();

    private static void reg(String id, int weight, int maxWorld, Supplier<Item> f) {
        if (REG.containsKey(id)) throw new IllegalArgumentException("Duplicated item id: " + id);
        REG.put(id, new Def(id, weight, maxWorld, f));
    }

    public static Item create(String id) {
        Def d = REG.get(id);
        if (d == null) throw new IllegalArgumentException("Item id desconocido: " + id);
        return d.create();
    }

    public static Def def(String id) {
        return REG.get(id);
    }

    public static boolean anyLootRemaining(Map<String, Integer> worldCounts) {
        for (Def d : REG.values()) {
            if (d.weight > 0 && count(worldCounts, d.id) < d.maxWorld) return true;
        }
        return false;
    }

    public static String pickRandomLootId(Random rng, Map<String, Integer> worldCounts) {
        int total = 0;
        for (Def d : REG.values()) {
            if (d.weight <= 0) continue;
            int used = count(worldCounts, d.id);
            int remain = Math.max(0, d.maxWorld - used);
            if (remain <= 0) continue;
            total += d.weight;
        }
        if (total <= 0) return null;

        int roll = rng.nextInt(total);
        for (Def d : REG.values()) {
            if (d.weight <= 0) continue;
            int used = count(worldCounts, d.id);
            int remain = Math.max(0, d.maxWorld - used);
            if (remain <= 0) continue;

            roll -= d.weight;
            if (roll < 0) return d.id;
        }
        return null;
    }

    private static int count(Map<String, Integer> m, String id) {
        return (m == null) ? 0 : m.getOrDefault(id, 0);
    }

    static {
        // ---------- Equipo inicial / comunes  ----------
        reg("cap_01", 8, 35, () -> Item.ropa("cap_01", "Gorra", 0.22, EquipmentSlot.HEAD, 1, 1, "Gorra de tela descolorida; corta algo el frío y la llovizna."));
        reg("bag_01", 6, 35, () -> Item.mochila("bag_01", "Mochila de tela", 0.77, 20.0, "Mochila de lona sencilla con cremalleras gastadas; suficiente para lo básico."));
        reg("knife_01", 5, 20, () -> Item.arma("knife_01", "Navaja", 0.125, 4, 0.7, 1, "Navaja plegable simple; útil para tareas y defensa cercana."));
        reg("shoe_01", 7, 25, () -> Item.ropa("shoe_01", "Zapatillas", 0.25, EquipmentSlot.FEET, 2, 1, "Zapatillas de estar por casa de la Hello Kitty."));

        reg("water_01", 0, 0, () -> Item.consumible("water_01", "Botella de agua (0.5 L)", 0.50, 0, 50, "Botella de plástico de medio litro. *Agua potable*."));
        reg("beans_01", 0, 0, () -> Item.consumible("beans_01", "Lata de judías", 0.35, 30, 0, "Alubias en salsa. Se necesita abrelatas."));
        reg("bandage_01", 0, 0, () -> Item.curacion("bandage_01", "Venda improvisada", 0.02, 20, "Tira de tela limpia; detiene sangrados."));
        reg("knife_02", 0, 0, () -> Item.arma("knife_02", "Cuchillo de cocina", 0.13, 5, 1, 1, "Cuchillo de cocina afilado."));
        reg("lighter_01", 0, 0, () -> Item.misc("lighter_01", "Encendedor", 0.005, "Mechero de plástico; pequeña fuente de fuego."));
        reg("rope_01", 0, 0, () -> Item.misc("rope_01", "Cuerda (5 m)", 1.00, "Cuerda de nylon de cinco metros, resistencia media."));
        reg("bar_01", 0, 0, () -> Item.consumible("bar_01", "Barrita energética (chocolate)", 0.008, 15, 0, "Barrita de chocolate compacta y calórica."));
        reg("bar_02", 0, 0, () -> Item.consumible("bar_02", "Barrita energética (frutos)", 0.008, 20, 0, "Frutos secos y miel."));
        reg("map_01", 0, 0, () -> Item.misc("map_01", "Mapa arrugado", 0.02, "Mapa viejo de la zona con anotaciones."));
        reg("canteen_01", 0, 0, () -> Item.misc("canteen_01", "Cantimplora", 0.20, "Cantimplora térmica metálica ligera. *Vacía*."));
        reg("battery_aa_4", 0, 0, () -> Item.misc("battery_aa_4", "Pila AAA x4", 0.05, "Paquete de cuatro pilas alcalinas; carga óptima."));
        reg("blanket_01", 0, 0, () -> Item.ropa("blanket_01", "Manta térmica", 0.40, EquipmentSlot.TORSO, 1, 2, "Manta de emergencia aluminizada; retiene calor."));
        reg("gloves_01", 0, 0, () -> Item.armadura("gloves_01", "Guantes de trabajo", 0.25, EquipmentSlot.HANDS, 1, 1, "Guantes de cuero con refuerzos."));

        // ---------- Loot del mundo con pesos + topes ----------
        // Bebida / comida
        reg("water_05", 12, 50, () -> Item.consumible("water_05", "Botella de agua (0.5 L)", 0.50, 0, 50, "Agua potable."));
        reg("water_05b", 12, 50, () -> Item.consumible("water_05b", "Botella de agua (0.5 L)", 0.50, 0, 50, "Agua potable."));
        reg("soda_01", 6, 20, () -> Item.consumible("soda_01", "Refresco azucarado", 0.33, 10, 20, "Demasiado dulce, pero ayuda."));
        reg("beans_02", 8, 30, () -> Item.consumible("beans_02", "Lata de alubias", 0.35, 30, 0, "Alimento denso y calórico."));
        reg("bar_choco", 10, 40, () -> Item.consumible("bar_choco", "Barrita energética (chocolate)", 0.008, 15, 0, "Subidón de azúcar."));
        reg("bar_nuts", 10, 40, () -> Item.consumible("bar_nuts", "Barrita energética (frutos)", 0.008, 20, 0, "Frutos secos y miel."));

        // Curación
        reg("bandage_clean", 8, 25, () -> Item.curacion("bandage_clean", "Venda limpia", 0.03, 30, "Detiene sangrados."));
        reg("disinfect", 5, 20, () -> Item.curacion("disinfect", "Antiséptico", 0.12, 15, "Desinfecta heridas superficiales."));

        // Herramientas / miscelánea
        reg("lighter_red", 7, 30, () -> Item.misc("lighter_red", "Mechero rojo", 0.005, "Fuente de fuego portátil."));
        reg("battery_aa_2", 8, 40, () -> Item.misc("battery_aa_2", "Pilas AA x2", 0.03, "Cargadas."));
        reg("rope_02", 5, 20, () -> Item.misc("rope_02", "Cuerda (3 m)", 0.60, "Útil para atar/asegurar."));
        reg("tape_01", 6, 20, () -> Item.misc("tape_01", "Cinta americana", 0.20, "Sirve para todo."));

        // Armas básicas
        reg("knife_fold", 7, 25, () -> Item.arma("knife_fold", "Navaja plegable", 0.12, 4, 0.9, 1, "Común, discreta."));
        reg("kitchen_knife", 6, 20, () -> Item.arma("kitchen_knife", "Cuchillo de cocina", 0.13, 5, 1.0, 1, "Corta que da gusto."));
        reg("machete", 3, 10, () -> Item.arma("machete", "Machete", 0.55, 8, 1.2, 1, "Pesado pero eficaz."));

        // Prendas básicas
        reg("cap_black", 9, 35, () -> Item.ropa("cap_black", "Gorra negra", 0.20, EquipmentSlot.HEAD, 1, 1, "Cubre algo del clima."));
        reg("gloves_work", 7, 25, () -> Item.armadura("gloves_work", "Guantes trabajo", 0.25, EquipmentSlot.HANDS, 1, 0, "Protegen cortes leves."));
        reg("boots_worn", 8, 30, () -> Item.ropa("boots_worn", "Botas gastadas", 0.90, EquipmentSlot.FEET, 1, 1, "Algo pesadas."));

        // Contenedores
        reg("bag_small", 7, 35, () -> Item.mochila("bag_small", "Mochila pequeña", 0.55, 12.0, "Espacio limitado."));
        reg("bag_daypack", 5, 20, () -> Item.mochila("bag_daypack", "Daypack", 0.80, 18.0, "Para un día largo."));
    }

    public static Collection<Def> all() {
        return Collections.unmodifiableCollection(REG.values());
    }
}
