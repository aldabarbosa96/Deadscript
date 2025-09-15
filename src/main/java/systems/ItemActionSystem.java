package systems;

import game.GameState;
import items.EquipmentSlot;
import items.Item;
import render.Renderer;

import java.util.ArrayList;
import java.util.List;

public final class ItemActionSystem {
    private ItemActionSystem() {}

    /** Devuelve las acciones lógicas disponibles para el ítem. */
    public static List<String> actionsFor(GameState s, Item it) {
        ArrayList<String> out = new ArrayList<>();
        if (it == null) return out;

        if (it.getConsumable() != null) out.add("Consumir");
        if (it.getHealing() != null) out.add("Usar");
        if (isEquippable(it)) out.add("Equipar");

        // (futuro) out.add("Soltar"), "Inspeccionar", "Asignar acceso rápido", etc.
        out.add("Cancelar");
        return out;
    }

    /** Aplica una acción. Devuelve true si cambia el estado/inventario/equipo. */
    public static boolean apply(GameState s, Item it, String action, Renderer r) {
        if (it == null || action == null) return false;
        action = action.trim().toLowerCase();

        switch (action) {
            case "consumir" -> {
                if (it.getConsumable() == null) {
                    r.log("No puedes consumir eso.");
                    return false;
                }
                var c = it.getConsumable();
                int addH = pctOf(s.maxHambre, c.hambrePct());
                int addS = pctOf(s.maxSed, c.sedPct());

                s.hambreAcc = clamp(s.hambreAcc + addH, 0, s.maxHambre);
                s.sedAcc    = clamp(s.sedAcc    + addS, 0, s.maxSed);
                s.hambre = (int)Math.round(s.hambreAcc);
                s.sed    = (int)Math.round(s.sedAcc);

                // consumir 1 unidad
                if (s.inventory.remove(it)) {
                    r.log("Consumes " + it.getNombre() + ". (+" + Math.max(0, c.hambrePct()) + "% hambre, +" + Math.max(0, c.sedPct()) + "% sed)");
                    return true;
                }
                return false;
            }

            case "usar" -> {
                if (it.getHealing() == null) {
                    r.log("Eso no parece útil como curación.");
                    return false;
                }
                var h = it.getHealing();
                int heal = pctOf(s.maxSalud, h.saludPct());
                int before = s.salud;
                s.salud = clampInt(s.salud + heal, 0, s.maxSalud);
                if (s.inventory.remove(it)) {
                    int gained = s.salud - before;
                    r.log("Usas " + it.getNombre() + " y recuperas " + gained + " de salud (" + h.saludPct() + "%).");
                    return true;
                }
                return false;
            }

            case "equipar" -> {
                if (!isEquippable(it)) {
                    r.log("No puedes equipar eso.");
                    return false;
                }
                boolean changed = equip(s, it, r);
                return changed;
            }

            default -> {
                // "Cancelar" u otras
                return false;
            }
        }
    }

    private static boolean isEquippable(Item it) {
        // Armaduras/ropa/contenedores usan wearableSlot/armor/container.
        if (it.getWearableSlot() != null) return true;
        if (it.getArmor() != null) return true;
        if (it.getContainer() != null) return true;
        // Armas no llevan wearableSlot, se equipan a mano principal/secundaria:
        return it.getWeapon() != null;
    }

    private static boolean equip(GameState s, Item it, Renderer r) {
        boolean changed = false;
        Item swapped = null;

        // 1) Ropa/armadura/contenedor por slot
        EquipmentSlot slot = it.getWearableSlot();
        if (slot != null || it.getArmor() != null || it.getContainer() != null) {
            switch (slot) {
                case HEAD -> { swapped = s.equipment.getHead(); s.equipment.setHead(it); }
                case TORSO -> { swapped = s.equipment.getChest(); s.equipment.setChest(it); }
                case HANDS -> { swapped = s.equipment.getHands(); s.equipment.setHands(it); }
                case LEGS -> { swapped = s.equipment.getLegs(); s.equipment.setLegs(it); }
                case FEET -> { swapped = s.equipment.getFeet(); s.equipment.setFeet(it); }
                case BACKPACK -> { swapped = s.equipment.getBackpack(); s.equipment.setBackpack(it); }
                default -> { /* MAIN/OFF no aplican aquí */ }
            }
            // quitar del inventario y devolver lo previamente equipado (si existe)
            if (s.inventory.remove(it)) {
                if (swapped != null) s.inventory.add(swapped);
                r.log("Te equipas: " + it.getNombre() + (swapped != null ? " (guardas " + swapped.getNombre() + " en la mochila)" : ""));
                changed = true;
            }
        }
        // 2) Armas a mano
        else if (it.getWeapon() != null) {
            Item prevMain = s.equipment.getMainHand();
            Item prevOff  = s.equipment.getOffHand();

            boolean twoHands = it.getWeapon().manos() == 2;

            if (twoHands) {
                if (s.inventory.remove(it)) {
                    if (prevMain != null) s.inventory.add(prevMain);
                    if (prevOff != null)  s.inventory.add(prevOff);
                    s.equipment.setMainHand(it);
                    s.equipment.setOffHand(null);
                    r.log("Empuñas (dos manos): " + it.getNombre() + ".");
                    changed = true;
                }
            } else {
                // mano principal por defecto; si está ocupada con arma a 1 mano y hay off libre, úsala
                boolean canUseOff = (prevMain != null && prevMain.getWeapon() != null && prevMain.getWeapon().manos() == 1 && prevOff == null);
                if (s.inventory.remove(it)) {
                    if (canUseOff) {
                        s.equipment.setOffHand(it);
                        r.log("Equipas en mano izq.: " + it.getNombre() + ".");
                    } else {
                        if (prevMain != null) s.inventory.add(prevMain);
                        s.equipment.setMainHand(it);
                        if (it.getWeapon().manos() == 2) s.equipment.setOffHand(null);
                        r.log("Empuñas: " + it.getNombre() + ".");
                    }
                    changed = true;
                }
            }
        }

        return changed;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static int clampInt(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static int pctOf(int max, int pct) {
        pct = Math.max(-100, Math.min(100, pct));
        return (int) Math.round(max * (pct / 100.0));
    }
}
