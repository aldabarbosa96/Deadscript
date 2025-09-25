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
        if (!indoor && (t == '-' || t == '¦' || t == '┌' || t == '┐' || t == '└' || t == '┘')) return "Camino";
        return switch (t) {
            case '#' -> "Árbol";
            case '~' -> "Agua";
            case '█' -> "Roca";
            case '+' -> "Puerta";
            case 'S' -> "Escalera";
            case '-' -> "Camino";
            case '╔', '╗', '╚', '╝', '═', '║' -> indoor ? "Pared interior" : "Pared";
            case '│', '─', '┼', '├', '┤', '┬', '┴', '┌', '┐', '└', '┘' -> "Tabique interior";
            case '▓' -> indoor ? "Suelo (interior)" : "Suelo";
            case '░' -> "Césped/arbusto bajo";
            case 'Û' -> "Pozo.";
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
            case '-', '¦', '┌', '┐', '└', '┘' -> "Vía asfaltada.";
            case '░' -> "Terreno blando y transitable.";
            case 'Û' -> "Pozo de agua dulce. Posible fuente de agua.";
            default -> "";
        };
    }

    public static boolean isInterestingTile(char t) {
        return switch (t) {
            case '#', '█', '~', '╔', '╗', '╚', '╝', '═', '║', '│', '─', '┼', '├', '┤', '┬', '┴', '┌', '┐', '└', '┘',
                 '+', 'S', '-', '¦', 'Û', '░' -> true;
            default -> false;
        };
    }
}
