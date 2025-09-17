package game;

import items.Equipment;
import items.Item;
import items.Items;
import world.Entity;
import world.GameMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

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
    public boolean escondido = false;
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

    public boolean worldActionsOpen = false;
    public int worldActionSel = 0;
    public List<String> worldActions = Collections.emptyList();
    public int worldTx = 0, worldTy = 0;
    public world.Entity worldTarget = null;
    public int hidePrevX = -1, hidePrevY = -1;
    public long lastPlayerAttackNs = 0L;
    public final Map<String, Integer> worldSpawnedByItem = new HashMap<>();

    public GameState() {
        // Equipo inicial (no consume cupos de loot)
        equipment.setHead(Items.create("cap_01"));
        equipment.setBackpack(Items.create("bag_01"));
        equipment.setMainHand(Items.create("knife_01"));
        equipment.setFeet(Items.create("shoe_01"));

        // Inventario inicial (no consume cupos de loot)
        inventory.add(Items.create("water_01"));
        inventory.add(Items.create("beans_01"));
        inventory.add(Items.create("bandage_01"));
        inventory.add(Items.create("knife_02"));
        inventory.add(Items.create("lighter_01"));
        inventory.add(Items.create("rope_01"));
        inventory.add(Items.create("bar_01"));
        inventory.add(Items.create("bar_02"));
        inventory.add(Items.create("map_01"));
        inventory.add(Items.create("canteen_01"));
        inventory.add(Items.create("battery_aa_4"));
        inventory.add(Items.create("blanket_01"));
        inventory.add(Items.create("gloves_01"));
    }

    public void resetMap() {
        map = GameMap.randomBalanced(240, 160);
        px = map.w / 2;
        py = map.h / 2;
        lastDx = lastDy = 0;
        escondido = false;
        hidePrevX = -1;
        hidePrevY = -1;
        entities.clear();
        spawnTimer = 0.0;
        nextGroupId = 1;
        worldSpawnedByItem.clear();
    }
}
