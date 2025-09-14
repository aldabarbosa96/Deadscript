package game;

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
    public int invSel = 0;
    public final List<String> inventory = new ArrayList<>(List.of("Botella de agua (0.5 kg)", "Lata de judías", "Venda improvisada", "Encendedor", "Cuchillo de bolsillo", "Cuerda (5 m)", "Barrita energética x2", "Mapa arrugado", "Cantimplora vacía", "Pila AA x4", "Manta térmica"));


    public void resetMap() {
        map = GameMap.randomBalanced(240, 160);
        px = map.w / 2;
        py = map.h / 2;
        lastDx = lastDy = 0;
        entities.clear();
        spawnTimer = 0.0;
    }
}
