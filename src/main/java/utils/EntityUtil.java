package utils;

import game.GameState;
import world.Entity;

public final class EntityUtil {
    private EntityUtil() {
    }

    public static Entity findTopEntityAt(GameState s, int x, int y) {
        Entity best = null;
        for (Entity e : s.entities) {
            if (e.x == x && e.y == y) {
                if (best == null) best = e;
                boolean eIsZ = (e.type == Entity.Type.ZOMBIE) || (e.glyph == 'Z');
                boolean bIsZ = best != null && ((best.type == Entity.Type.ZOMBIE) || (best.glyph == 'Z'));
                boolean eIsLoot = (e.type == Entity.Type.LOOT) || (e.glyph == '*');
                boolean bIsLoot = best != null && ((best.type == Entity.Type.LOOT) || (best.glyph == '*'));
                if (eIsZ && !bIsZ) return e;
                if (!bIsZ && eIsLoot && !bIsLoot) best = e;
            }
        }
        return best;
    }

    public static String entityName(Entity e) {
        if (e == null) return "-";
        if (e.type == Entity.Type.ZOMBIE || e.glyph == 'Z') return "Zombi";
        if (e.type == Entity.Type.LOOT || e.glyph == '*') return "Botín";
        return "Entidad";
    }

    public static String tileName(char t, boolean indoor) {
        return switch (t) {
            case '#' -> "Árbol";
            case '~' -> "Agua";
            case '█' -> "Roca";
            case '+' -> "Puerta";
            case 'S' -> "Escalera";
            case '╔', '╗', '╚', '╝', '═', '║' -> indoor ? "Pared interior" : "Pared";
            case '▓' -> indoor ? "Suelo (interior)" : "Suelo";
            default -> "Terreno";
        };
    }

    public static String tileHint(char t) {
        return switch (t) {
            case 'S' -> "Pulsa [Espacio] para subir o bajar";
            case '+' -> "Acción futura: abrir/cerrar.";
            case '~' -> "No transitable. Posible fuente de agua.";
            case '#' -> "Obstáculo. Cubre visión y paso.";
            case '█' -> "Cobertura dura. No transitable.";
            default -> "";
        };
    }

    public static boolean isInterestingTile(char t) {
        return switch (t) {
            case '#', '█', '~', '╔', '╗', '╚', '╝', '═', '║', '+', 'S' -> true;
            default -> false;
        };
    }
}
