package systems;

import game.GameState;
import items.EquipmentSlot;
import items.Item;
import render.Renderer;

import java.util.ArrayList;
import java.util.List;

public final class ItemActionSystem {
    private ItemActionSystem() {
    }

    public static List<String> actionsFor(GameState s, Item it) {
        ArrayList<String> out = new ArrayList<>();
        if (it == null) return out;

        if (it.getConsumable() != null) out.add("Consumir");
        if (it.getHealing() != null) out.add("Usar");
        if (isEquippable(it)) out.add("Equipar");

        // todo--> out.add("Soltar"), "Inspeccionar", "Asignar acceso rápido", crafteo etc.
        out.add("Cancelar");
        return out;
    }

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
                s.sedAcc = clamp(s.sedAcc + addS, 0, s.maxSed);
                s.hambre = (int) Math.round(s.hambreAcc);
                s.sed = (int) Math.round(s.sedAcc);

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

    public static List<Item> equippablesForSlot(GameState s, EquipmentSlot slot) {
        ArrayList<Item> out = new ArrayList<>();
        if (s == null || slot == null) return out;

        switch (slot) {
            case HEAD, TORSO, HANDS, LEGS, FEET, BACKPACK -> {
                for (Item it : s.inventory) {
                    if (it == null) continue;
                    if (it.getWearableSlot() == slot) out.add(it);
                }
            }
            case MAIN_HAND -> {
                for (Item it : s.inventory) {
                    if (it != null && it.getWeapon() != null) out.add(it);
                }
            }
            case OFF_HAND -> {
                Item main = s.equipment.getMainHand();
                if (main != null && main.getWeapon() != null && main.getWeapon().manos() == 2) {
                    return out; // no permitido si el arma principal es a 2 manos
                }
                for (Item it : s.inventory) {
                    if (it != null && it.getWeapon() != null && it.getWeapon().manos() == 1) out.add(it);
                }
            }
        }
        return out;
    }

    public static boolean unequipSlot(GameState s, EquipmentSlot slot, Renderer r) {
        if (s == null || slot == null) return false;

        Item removed = null;
        switch (slot) {
            case HEAD -> {
                removed = s.equipment.getHead();
                s.equipment.setHead(null);
            }
            case TORSO -> {
                removed = s.equipment.getChest();
                s.equipment.setChest(null);
            }
            case HANDS -> {
                removed = s.equipment.getHands();
                s.equipment.setHands(null);
            }
            case LEGS -> {
                removed = s.equipment.getLegs();
                s.equipment.setLegs(null);
            }
            case FEET -> {
                removed = s.equipment.getFeet();
                s.equipment.setFeet(null);
            }
            case BACKPACK -> {
                removed = s.equipment.getBackpack();
                s.equipment.setBackpack(null);
            }
            case MAIN_HAND -> {
                removed = s.equipment.getMainHand();
                s.equipment.setMainHand(null);
                // libera la off-hand por si el arma previa era a 2 manos
                s.equipment.setOffHand(null);
            }
            case OFF_HAND -> {
                removed = s.equipment.getOffHand();
                s.equipment.setOffHand(null);
            }
        }

        if (removed != null) {
            s.inventory.add(removed);
            if (r != null) r.log("Desequipas: " + removed.getNombre() + " (guardado en la mochila).");
            return true;
        } else {
            if (r != null) r.log("No hay nada equipado en ese slot.");
            return false;
        }
    }

    public static boolean equipToSlot(GameState s, Item it, EquipmentSlot slot, Renderer r) {
        if (s == null || it == null || slot == null) return false;

        // Slots de ropa/armadura/mochila
        if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.TORSO || slot == EquipmentSlot.HANDS || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET || slot == EquipmentSlot.BACKPACK) {
            if (it.getWearableSlot() != slot) {
                if (r != null) r.log("Ese objeto no encaja en ese slot.");
                return false;
            }
            Item swapped = null;
            switch (slot) {
                case HEAD -> {
                    swapped = s.equipment.getHead();
                    s.equipment.setHead(it);
                }
                case TORSO -> {
                    swapped = s.equipment.getChest();
                    s.equipment.setChest(it);
                }
                case HANDS -> {
                    swapped = s.equipment.getHands();
                    s.equipment.setHands(it);
                }
                case LEGS -> {
                    swapped = s.equipment.getLegs();
                    s.equipment.setLegs(it);
                }
                case FEET -> {
                    swapped = s.equipment.getFeet();
                    s.equipment.setFeet(it);
                }
                case BACKPACK -> {
                    swapped = s.equipment.getBackpack();
                    s.equipment.setBackpack(it);
                }
                default -> {
                }
            }
            if (s.inventory.remove(it)) {
                if (swapped != null) s.inventory.add(swapped);
                if (r != null)
                    r.log("Te equipas: " + it.getNombre() + (swapped != null ? " (guardas " + swapped.getNombre() + " en la mochila)" : ""));
                return true;
            }
            return false;
        }

        // Manos
        if (slot == EquipmentSlot.MAIN_HAND) {
            if (it.getWeapon() == null) {
                if (r != null) r.log("Solo puedes empuñar armas.");
                return false;
            }
            Item prevMain = s.equipment.getMainHand();
            Item prevOff = s.equipment.getOffHand();
            boolean twoHands = it.getWeapon().manos() == 2;
            if (!s.inventory.remove(it)) return false;

            if (twoHands) {
                if (prevMain != null) s.inventory.add(prevMain);
                if (prevOff != null) s.inventory.add(prevOff);
                s.equipment.setMainHand(it);
                s.equipment.setOffHand(null);
                if (r != null) r.log("Empuñas (dos manos): " + it.getNombre() + ".");
            } else {
                if (prevMain != null) s.inventory.add(prevMain);
                s.equipment.setMainHand(it);
                if (r != null) r.log("Empuñas: " + it.getNombre() + ".");
            }
            return true;
        }

        if (slot == EquipmentSlot.OFF_HAND) {
            if (it.getWeapon() == null || it.getWeapon().manos() != 1) {
                if (r != null) r.log("En la mano izquierda solo puedes llevar armas a 1 mano.");
                return false;
            }
            Item main = s.equipment.getMainHand();
            if (main != null && main.getWeapon() != null && main.getWeapon().manos() == 2) {
                if (r != null) r.log("No puedes usar off-hand con un arma de dos manos equipada.");
                return false;
            }
            if (!s.inventory.remove(it)) return false;
            Item prevOff = s.equipment.getOffHand();
            if (prevOff != null) s.inventory.add(prevOff);
            s.equipment.setOffHand(it);
            if (r != null) r.log("Equipas en mano izq.: " + it.getNombre() + ".");
            return true;
        }
        return false;
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
                case HEAD -> {
                    swapped = s.equipment.getHead();
                    s.equipment.setHead(it);
                }
                case TORSO -> {
                    swapped = s.equipment.getChest();
                    s.equipment.setChest(it);
                }
                case HANDS -> {
                    swapped = s.equipment.getHands();
                    s.equipment.setHands(it);
                }
                case LEGS -> {
                    swapped = s.equipment.getLegs();
                    s.equipment.setLegs(it);
                }
                case FEET -> {
                    swapped = s.equipment.getFeet();
                    s.equipment.setFeet(it);
                }
                case BACKPACK -> {
                    swapped = s.equipment.getBackpack();
                    s.equipment.setBackpack(it);
                }
                default -> {
                }
            }
            if (s.inventory.remove(it)) {
                if (swapped != null) s.inventory.add(swapped);
                r.log("Te equipas: " + it.getNombre() + (swapped != null ? " (guardas " + swapped.getNombre() + " en la mochila)" : ""));
                changed = true;
            }
        }
        // 2) Armas a mano
        else if (it.getWeapon() != null) {
            Item prevMain = s.equipment.getMainHand();
            Item prevOff = s.equipment.getOffHand();

            boolean twoHands = it.getWeapon().manos() == 2;

            if (twoHands) {
                if (s.inventory.remove(it)) {
                    if (prevMain != null) s.inventory.add(prevMain);
                    if (prevOff != null) s.inventory.add(prevOff);
                    s.equipment.setMainHand(it);
                    s.equipment.setOffHand(null);
                    r.log("Empuñas (dos manos): " + it.getNombre() + ".");
                    changed = true;
                }
            } else {
                // mano principal por defecto; si está ocupada con arma a 1 mano y hay off libre, se usa
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
