package game;

import world.Entity;
import world.GameMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    public GameMap map = GameMap.randomBalanced(240, 160);
    public int px = map.w / 2, py = map.h / 2;
    public int lastDx = 0, lastDy = 0;
    public String ubicacion = "Goodsummer";
    public int temperaturaC = 18;
    public int salud = 65, maxSalud = 100;
    public int energia = 82, maxEnergia = 100;
    public int hambre = 36, maxHambre = 100;
    public int sed = 10, maxSed = 100;
    public int sueno = 75, maxSueno = 100;
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


    public void resetMap() {
        map = GameMap.randomBalanced(240, 160);
        px = map.w / 2;
        py = map.h / 2;
        lastDx = lastDy = 0;
        entities.clear();
        spawnTimer = 0.0;
    }
}
