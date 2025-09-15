package game;

import items.Equipment;
import items.EquipmentSlot;
import items.Item;
import world.Entity;
import world.GameMap;

import java.util.ArrayList;
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

    public final List<Item> inventory = new ArrayList<>();
    public final Equipment equipment = new Equipment();

    public GameState() {
        equipment.setHead(Item.ropa("cap_01", "Gorra", 0.20, EquipmentSlot.HEAD, 1));
        equipment.setBackpack(Item.mochila("bag_01", "Mochila tela", 0.80, 20.0));
        equipment.setMainHand(Item.arma("knife_01", "Navaja", 0.25, 4, 0.7, 1));

        inventory.add(Item.misc("water_01", "Botella de agua (0.5 L)", 0.50));
        inventory.add(Item.consumible("beans_01", "Lata de judías", 0.35));
        inventory.add(Item.misc("bandage_01", "Venda improvisada", 0.10));
        inventory.add(Item.misc("lighter_01", "Encendedor", 0.05));
        inventory.add(Item.misc("rope_01", "Cuerda (5 m)", 1.00));
        inventory.add(Item.consumible("bar_01", "Barrita energética", 0.08));
        inventory.add(Item.consumible("bar_02", "Barrita energética", 0.08));
        inventory.add(Item.misc("map_01", "Mapa arrugado", 0.02));
        inventory.add(Item.misc("canteen_01", "Cantimplora vacía", 0.20));
        inventory.add(Item.misc("battery_aa_4", "Pila AA x4", 0.10));
        inventory.add(Item.ropa("blanket_01", "Manta térmica", 0.40, EquipmentSlot.TORSO, 2));
        inventory.add(Item.armadura("gloves_01", "Guantes de trabajo", 0.25, EquipmentSlot.HANDS, 1, 1));
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
