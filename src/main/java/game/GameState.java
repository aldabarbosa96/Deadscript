package game;

import items.Equipment;
import items.Item;
import items.Items;
import world.Entity;
import world.GameMap;

import java.util.*;

public class GameState {
    public GameMap map = GameMap.randomBalanced(800, 650);
    public int px = map.w / 2, py = map.h / 2;
    public int lastDx = 0, lastDy = 0;
    public String ubicacion = "Goodsummer";
    public int temperaturaC = 18;
    public int tempCorporalC = 37;
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

    public boolean statsOpen = false;
    public int frio = 12;
    public int miedo = 0;
    public int aburrimiento = 8;
    public int malestar = 0;
    public int dolor = 0;
    public int radiacionPct = 0;
    public int statsCol = 0;
    public int statsSelBasic = 0;
    public int statsSelSkill = 0;

    public enum SkillGroup {FISICO, COMBATE, CRAFTEO, SUPERVIVENCIA}

    public static final class Skill {
        public final String id, nombre;
        public final SkillGroup grupo;
        public int nivel;
        public double xp;

        public Skill(String id, String nombre, SkillGroup g, int nivel, double xp) {
            this.id = id;
            this.nombre = nombre;
            this.grupo = g;
            this.nivel = Math.max(0, Math.min(10, nivel));
            this.xp = Math.max(0, Math.min(1, xp));
        }
    }

    public final Map<SkillGroup, List<Skill>> skills = new EnumMap<>(SkillGroup.class);

    public enum BodyPart {CABEZA, TORSO, BRAZO_IZQ, BRAZO_DER, MANOS, PIERNA_IZQ, PIERNA_DER, PIES}

    public static final class Injury {
        public final String nombre;
        public final int severidad;

        public Injury(String n, int s) {
            nombre = n;
            severidad = Math.max(1, Math.min(100, s));
        }
    }

    public final Map<BodyPart, List<Injury>> injuries = new EnumMap<>(BodyPart.class);
    public int statsBodySel = 0;


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

        for (SkillGroup g : SkillGroup.values()) skills.put(g, new ArrayList<>());

        skills.get(SkillGroup.FISICO).add(new Skill("fitness", "Forma física", SkillGroup.FISICO, 2, 0.35));
        skills.get(SkillGroup.FISICO).add(new Skill("strength", "Fuerza", SkillGroup.FISICO, 1, 0.60));
        skills.get(SkillGroup.FISICO).add(new Skill("sprint", "Sprint", SkillGroup.FISICO, 1, 0.10));
        skills.get(SkillGroup.FISICO).add(new Skill("agility", "Agilidad", SkillGroup.FISICO, 0, 0.55));
        skills.get(SkillGroup.FISICO).add(new Skill("stealth", "Sigilo", SkillGroup.FISICO, 0, 0.15));

        skills.get(SkillGroup.COMBATE).add(new Skill("melee_s", "Arma corta (cont.)", SkillGroup.COMBATE, 2, 0.20));
        skills.get(SkillGroup.COMBATE).add(new Skill("melee_l", "Arma larga (cont.)", SkillGroup.COMBATE, 1, 0.70));
        skills.get(SkillGroup.COMBATE).add(new Skill("blade_s", "Arma corta (hoja)", SkillGroup.COMBATE, 2, 0.40));
        skills.get(SkillGroup.COMBATE).add(new Skill("blade_l", "Arma larga (hoja)", SkillGroup.COMBATE, 0, 0.05));
        skills.get(SkillGroup.COMBATE).add(new Skill("gun_s", "Arma de fuego (corta)", SkillGroup.COMBATE, 0, 0.00));
        skills.get(SkillGroup.COMBATE).add(new Skill("gun_l", "Arma de fuego (larga)", SkillGroup.COMBATE, 0, 0.00));
        skills.get(SkillGroup.COMBATE).add(new Skill("reload", "Recarga", SkillGroup.COMBATE, 0, 0.00));
        skills.get(SkillGroup.COMBATE).add(new Skill("maint", "Mantenimiento", SkillGroup.COMBATE, 1, 0.25));

        skills.get(SkillGroup.CRAFTEO).add(new Skill("carp", "Carpintería", SkillGroup.CRAFTEO, 0, 0.10));
        skills.get(SkillGroup.CRAFTEO).add(new Skill("elec", "Electricista", SkillGroup.CRAFTEO, 0, 0.00));
        skills.get(SkillGroup.CRAFTEO).add(new Skill("font", "Fontanería", SkillGroup.CRAFTEO, 0, 0.00));
        skills.get(SkillGroup.CRAFTEO).add(new Skill("mech", "Mecánico", SkillGroup.CRAFTEO, 0, 0.00));
        skills.get(SkillGroup.CRAFTEO).add(new Skill("cook", "Cocina", SkillGroup.CRAFTEO, 1, 0.30));
        skills.get(SkillGroup.CRAFTEO).add(new Skill("med", "Primeros auxilios", SkillGroup.CRAFTEO, 0, 0.00));
        skills.get(SkillGroup.CRAFTEO).add(new Skill("farm", "Granjero", SkillGroup.CRAFTEO, 0, 0.00));
        skills.get(SkillGroup.CRAFTEO).add(new Skill("agri", "Agricultor", SkillGroup.CRAFTEO, 0, 0.00));
        skills.get(SkillGroup.CRAFTEO).add(new Skill("tail", "Sastre", SkillGroup.CRAFTEO, 0, 0.00));

        skills.get(SkillGroup.SUPERVIVENCIA).add(new Skill("fish", "Pesca", SkillGroup.SUPERVIVENCIA, 0, 0.00));
        skills.get(SkillGroup.SUPERVIVENCIA).add(new Skill("hunt", "Caza", SkillGroup.SUPERVIVENCIA, 0, 0.00));
        skills.get(SkillGroup.SUPERVIVENCIA).add(new Skill("pros", "Prospección", SkillGroup.SUPERVIVENCIA, 0, 0.00));

        for (BodyPart p : BodyPart.values()) injuries.put(p, new ArrayList<>());
        // testing injuries todo--> borrar en producción
        injuries.get(BodyPart.BRAZO_IZQ).add(new Injury("Corte superficial", 18));

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
