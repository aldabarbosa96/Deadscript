package items;

import java.util.List;

public class Equipment {
    private Item head;
    private Item chest;
    private Item hands;
    private Item legs;
    private Item feet;
    private Item mainHand;
    private Item offHand;
    private Item backpack;

    public Item getHead() { return head; }
    public Item getChest() { return chest; }
    public Item getHands() { return hands; }
    public Item getLegs() { return legs; }
    public Item getFeet() { return feet; }
    public Item getMainHand() { return mainHand; }
    public Item getOffHand() { return offHand; }
    public Item getBackpack() { return backpack; }

    public void setHead(Item i) { head = i; }
    public void setChest(Item i) { chest = i; }
    public void setHands(Item i) { hands = i; }
    public void setLegs(Item i) { legs = i; }
    public void setFeet(Item i) { feet = i; }
    public void setMainHand(Item i) { mainHand = i; if (i != null && i.getWeapon() != null && i.getWeapon().manos == 2) offHand = null; }
    public void setOffHand(Item i) { if (mainHand == null || mainHand.getWeapon() == null || mainHand.getWeapon().manos == 1) offHand = i; }
    public void setBackpack(Item i) { backpack = i; }

    public double capacidadKg() {
        if (backpack != null && backpack.getContainer() != null) return backpack.getContainer().capacidadKg;
        return 20.0;
    }

    public double pesoEquipadoKg() {
        double p = 0;
        if (head != null) p += head.getPesoKg();
        if (chest != null) p += chest.getPesoKg();
        if (hands != null) p += hands.getPesoKg();
        if (legs != null) p += legs.getPesoKg();
        if (feet != null) p += feet.getPesoKg();
        if (mainHand != null) p += mainHand.getPesoKg();
        if (offHand != null) p += offHand.getPesoKg();
        if (backpack != null) p += backpack.getPesoKg();
        return p;
    }

    public double pesoTotalKg(List<Item> inventario) {
        double p = pesoEquipadoKg();
        if (inventario != null) for (Item it : inventario) p += it.getPesoKg();
        return p;
    }

    public String nombreOGuion(Item it) { return it == null ? "-" : it.getNombre(); }
}
