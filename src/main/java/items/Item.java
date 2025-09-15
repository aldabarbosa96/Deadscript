package items;

import java.util.Objects;

public class Item {
    private final String id;
    private final String nombre;
    private final ItemCategory categoria;
    private final double pesoKg;
    private final int maxDurabilidad;
    private int durabilidad;
    private final EquipmentSlot wearableSlot;
    private final ArmorAttr armor;
    private final WeaponAttr weapon;
    private final ContainerAttr container;
    private final String descripcion;

    public record ArmorAttr(int proteccion, int abrigo) {
            public ArmorAttr(int proteccion, int abrigo) {
                this.proteccion = Math.max(0, proteccion);
                this.abrigo = abrigo;
            }
        }

    public record WeaponAttr(int danho, double cooldownSec, int manos) {
            public WeaponAttr(int danho, double cooldownSec, int manos) {
                this.danho = Math.max(0, danho);
                this.cooldownSec = Math.max(0.01, cooldownSec);
                this.manos = Math.max(1, Math.min(2, manos));
            }
        }

    public record ContainerAttr(double capacidadKg) {
            public ContainerAttr(double capacidadKg) {
                this.capacidadKg = Math.max(0, capacidadKg);
            }
        }

    private Item(String id, String nombre, ItemCategory categoria, double pesoKg, int maxDurabilidad, int durabilidad, EquipmentSlot wearableSlot, ArmorAttr armor, WeaponAttr weapon, ContainerAttr container, String descripcion) {
        this.id = Objects.requireNonNull(id);
        this.nombre = Objects.requireNonNull(nombre);
        this.categoria = Objects.requireNonNull(categoria);
        this.pesoKg = Math.max(0, pesoKg);
        this.maxDurabilidad = Math.max(1, maxDurabilidad);
        this.durabilidad = Math.max(0, Math.min(this.maxDurabilidad, durabilidad));
        this.wearableSlot = wearableSlot;
        this.armor = armor;
        this.weapon = weapon;
        this.container = container;
        this.descripcion = (descripcion == null) ? "" : descripcion.trim();
    }

    public static Item misc(String id, String nombre, double pesoKg, String descripcion) {
        return new Item(id, nombre, ItemCategory.MISC, pesoKg, 100, 100, null, null, null, null, descripcion);
    }

    public static Item consumible(String id, String nombre, double pesoKg, String descripcion) {
        return new Item(id, nombre, ItemCategory.CONSUMABLE, pesoKg, 100, 100, null, null, null, null, descripcion);
    }

    public static Item arma(String id, String nombre, double pesoKg, int danho, double cooldownSec, int manos, String descripcion) {
        return new Item(id, nombre, ItemCategory.WEAPON, pesoKg, 100, 100, null, null, new WeaponAttr(danho, cooldownSec, manos), null, descripcion);
    }

    public static Item armadura(String id, String nombre, double pesoKg, EquipmentSlot slot, int proteccion, int abrigo, String descripcion) {
        return new Item(id, nombre, ItemCategory.ARMOR, pesoKg, 100, 100, slot, new ArmorAttr(proteccion, abrigo), null, null, descripcion);
    }

    public static Item ropa(String id, String nombre, double pesoKg, EquipmentSlot slot, int abrigo, String descripcion) {
        return new Item(id, nombre, ItemCategory.CLOTHING, pesoKg, 100, 100, slot, new ArmorAttr(0, abrigo), null, null, descripcion);
    }

    public static Item mochila(String id, String nombre, double pesoKg, double capacidadKg, String descripcion) {
        return new Item(id, nombre, ItemCategory.CONTAINER, pesoKg, 100, 100, EquipmentSlot.BACKPACK, null, null, new ContainerAttr(capacidadKg), descripcion);
    }
    public static Item curacion(String id, String nombre, double pesoKg, String descripcion) {
        return new Item(id, nombre, ItemCategory.HEALING, pesoKg, 100, 100, null, null, null, null, descripcion);
    }


    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public ItemCategory getCategoria() {
        return categoria;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public int getMaxDurabilidad() {
        return maxDurabilidad;
    }

    public int getDurabilidad() {
        return durabilidad;
    }

    public EquipmentSlot getWearableSlot() {
        return wearableSlot;
    }

    public ArmorAttr getArmor() {
        return armor;
    }

    public WeaponAttr getWeapon() {
        return weapon;
    }

    public ContainerAttr getContainer() {
        return container;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getDurabilidadPct() {
        return (int) Math.round(100.0 * durabilidad / Math.max(1, maxDurabilidad));
    }

    public void setDurabilidad(int nueva) {
        this.durabilidad = Math.max(0, Math.min(maxDurabilidad, nueva));
    }

    public void consumirDurabilidad(int puntos) {
        setDurabilidad(durabilidad - Math.max(0, puntos));
    }

    @Override
    public String toString() {
        return nombre;
    }
}
