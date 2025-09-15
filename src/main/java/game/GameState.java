package game;

import items.Equipment;
import items.EquipmentSlot;
import items.Item;
import world.Entity;
import world.GameMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameState {
    public GameMap map = GameMap.randomBalanced(800, 600);
    public int px = map.w / 2, py = map.h / 2;
    public int lastDx = 0, lastDy = 0;
    public String ubicacion = "Goodsummer";
    public int temperaturaC = 18;
    public int salud = 100, maxSalud = 100;
    public int energia = 100, maxEnergia = 100;
    public int hambre = 100, maxHambre = 100;
    public int sed = 100, maxSed = 100;
    public int sueno = 100, maxSueno = 100;
    public boolean sangrado = false;
    public int infeccionPct = 0;
    public boolean escondido = true;
    public double hambreAcc = hambre;
    public double sedAcc = sed;
    public double suenoAcc = sueno;
    public double energiaAcc = energia;
    public long lastPlayerStepNs = 0L;
    public final List<Entity> entities = new ArrayList<>();
    public final Random rng = new Random();
    public double spawnTimer = 0.0;
    public int nextGroupId = 1;

    public boolean inventoryOpen = false;
    public boolean equipmentOpen = false;
    public int invSel = 0;
    public int eqSel = 0;

    public final List<Item> inventory = new ArrayList<>();
    public final Equipment equipment = new Equipment();

    public boolean invActionsOpen = false;
    public int invActionSel = 0;
    public List<String> invActions = Collections.emptyList();

    public boolean eqActionsOpen = false;
    public int eqActionSel = 0;
    public List<String> eqActions = Collections.emptyList();

    public boolean eqSelectOpen = false;
    public int eqSelectSel = 0;
    public List<Item> eqSelectItems = Collections.emptyList();



    public GameState() {
        // equipo
        equipment.setHead(Item.ropa("cap_01", "Gorra", 0.20, EquipmentSlot.HEAD, 1, "Gorra de tela descolorida; corta algo el frío y la llovizna."));
        equipment.setBackpack(Item.mochila("bag_01", "Mochila tela", 0.80, 20.0, "Mochila de lona sencilla con cremalleras gastadas; suficiente para lo básico."));
        equipment.setMainHand(Item.arma("knife_01", "Navaja", 0.125, 4, 0.7, 1, "Navaja plegable simple; útil para tareas y defensa cercana."));
        equipment.setFeet(Item.ropa("shoe_01", "Zapatillas", 0.25, EquipmentSlot.FEET, 1, "Zapatillas de estar por casa de la Hello Kitty."));

        // inventario
        inventory.add(Item.consumible("water_01", "Botella de agua (0.5 L)", 0.50, 0, 50, "Botella de plástico de medio litro. *Agua potable*."));
        inventory.add(Item.consumible("beans_01", "Lata de judías", 0.35, 30, 0, "Lata de alubias en salsa. Se necesita abrelatas o similar para poder abrirse."));
        inventory.add(Item.curacion("bandage_01", "Venda improvisada", 0.10, 20, "Tira de tela limpia y rasgada; detiene sangrados. *No esterilizada*."));
        inventory.add(Item.arma("knife_02", "Cuchillo de cocina", 0.13, 5, 1, 1, "Cuchillo de cocina afilado; útil para cortar alimentos, en especial carne."));
        inventory.add(Item.misc("lighter_01", "Encendedor", 0.05, "Mechero de plástico; pequeña fuente de fuego. *No recargable*."));
        inventory.add(Item.misc("rope_01", "Cuerda (5 m)", 1.00, "Cuerda de nylon de cinco metros, resistencia media; útil para atar o asegurar."));
        inventory.add(Item.consumible("bar_01", "Barrita energética", 0.08, 15, 0, "Barrita de chocolate compacta y muy calórica; recupera +15% de hambre."));
        inventory.add(Item.consumible("bar_02", "Barrita energética", 0.08, 20, 0, "Barrita de frutos secos y miel; recupera +20% de hambre."));
        inventory.add(Item.misc("map_01", "Mapa arrugado", 0.02, "Mapa viejo de la zona con anotaciones a bolígrafo y bordes desgastados."));
        inventory.add(Item.misc("canteen_01", "Cantimplora vacía", 0.20, "Cantimplora térmica metálica ligera. *Vacía*."));
        inventory.add(Item.misc("battery_aa_4", "Pila AA x4", 0.10, "Paquete improvisado de cuatro pilas alcalinas; carga óptima."));
        inventory.add(Item.ropa("blanket_01", "Manta térmica", 0.40, EquipmentSlot.TORSO, 2, "Manta de emergencia aluminizada; retiene calor y hace ruido al moverse."));
        inventory.add(Item.armadura("gloves_01", "Guantes de trabajo", 0.25, EquipmentSlot.HANDS, 1, 1, "Guantes de cuero con refuerzos; amortiguan golpes y cortaduras leves."));
    }

    public void resetMap() {
        map = GameMap.randomBalanced(240, 160);
        px = map.w / 2;
        py = map.h / 2;
        lastDx = lastDy = 0;
        entities.clear();
        spawnTimer = 0.0;
    }
}
