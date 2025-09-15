package ui.menu;

import items.Item;
import utils.ANSI;

import java.util.ArrayList;
import java.util.List;

public class InventoryView {

    public void render(int top, int left, int width, int height, List<Item> items, int selectedIndex) {
        if (width < 10 || height < 5) return;

        final int inner = Math.max(0, width - 2);
        final int contentRows = Math.max(1, height - 3);

        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print(centerLabel(" INVENTARIO ", inner, '─'));
            System.out.print('┐');
        } else {
            System.out.print(repeat('─', width));
        }

        final int baseTop = top + 1;
        final int baseLeft = left + 1;

        final int blankTopRows = 1;

        final int n = items == null ? 0 : items.size();
        int sel = Math.max(0, Math.min(selectedIndex, Math.max(0, n - 1)));
        int listRows = Math.max(0, contentRows);
        int visibleRowsForItems = Math.max(0, listRows - blankTopRows);
        int start = Math.max(0, Math.min(sel - visibleRowsForItems / 2, Math.max(0, n - visibleRowsForItems)));

        int listW = Math.max(18, (int) Math.round(inner * 0.58));
        int gap = 1;
        int detailW = Math.max(0, inner - listW - gap);

        Item selected = (sel < n && sel >= 0) ? items.get(sel) : null;
        List<String> rightLines = buildRightPanel(selected, detailW);

        for (int i = 0; i < contentRows; i++) {
            ANSI.gotoRC(baseTop + i, left);
            if (width >= 2) System.out.print('│');

            String leftCell;
            boolean selectedRow = false;

            if (i < blankTopRows) {
                leftCell = repeat(' ', listW);  // limpia la fila en blanco
            } else {
                int idx = start + (i - blankTopRows);
                Item it = (idx < n) ? items.get(idx) : null;
                String line = it == null ? "" : it.getNombre() + "  [" + formatKg(it.getPesoKg()) + "]  " + it.getDurabilidadPct() + "%";
                selectedRow = (idx == sel);
                String prefix = selectedRow ? "» " : "  ";
                leftCell = clipAscii(prefix + line, listW);
                if (leftCell.length() < listW) leftCell = leftCell + repeat(' ', listW - leftCell.length());
            }

            if (selectedRow) {
                ANSI.boldOn();
                System.out.print(leftCell);
                ANSI.boldOff();
            } else {
                System.out.print(leftCell);
            }

            System.out.print(repeat(' ', gap));
            String rightCell = (i < rightLines.size()) ? clipAscii(rightLines.get(i), detailW) : "";
            if (rightCell.length() < detailW) rightCell = rightCell + repeat(' ', detailW - rightCell.length());
            System.out.print(rightCell);

            if (width >= 2) System.out.print('│');
        }

        String help = " [I] Cerrar    [Flechas] Navegar ";
        ANSI.gotoRC(top + 1 + contentRows, left);
        if (width >= 2) {
            System.out.print('│');
            String body = clipAscii(centerLabel(help, inner, ' '), inner);
            System.out.print(body);
            if (body.length() < inner) System.out.print(repeat(' ', inner - body.length()));
            System.out.print('│');
        } else {
            System.out.print(clipAscii(help, width));
        }

        ANSI.gotoRC(top + 2 + contentRows, left);
        if (width >= 2) {
            System.out.print('└');
            System.out.print(repeat('─', inner));
            System.out.print('┘');
        } else {
            System.out.print(repeat('─', width));
        }
    }


    private static List<String> buildRightPanel(Item it, int w) {
        ArrayList<String> out = new ArrayList<>();
        if (w <= 0) return out;

        if (it == null) {
            out.add(center("— Sin selección —", w));
            return out;
        }

        out.add("");
        out.add(center("[" + it.getNombre() + "]", w));
        out.add("");

        // arte ASCII
        List<String> art = asciiArtFor(it);
        for (String line : art) out.add(center(line, w));

        // atributos
        out.add(pad("Peso: " + formatKg(it.getPesoKg()), w));
        out.add(pad("Condición: " + it.getDurabilidadPct() + "%", w));
        if (it.getWeapon() != null) {
            out.add(pad("Daño: " + it.getWeapon().danho + "   Manos: " + it.getWeapon().manos, w));
            out.add(pad(String.format("Cadencia: %.2fs", it.getWeapon().cooldownSec), w));
        }
        if (it.getArmor() != null) {
            out.add(pad("Protección: " + it.getArmor().proteccion + "   Abrigo: " + it.getArmor().abrigo, w));
        }
        if (it.getContainer() != null) {
            out.add(pad("Capacidad: " + formatKg(it.getContainer().capacidadKg), w));
        }
        if (it.getWearableSlot() != null) {
            out.add(pad("Slot: " + it.getWearableSlot().name(), w));
        }

        // descripción
        String desc = describe(it);
        for (String ln : wrap(desc, w)) out.add(ln);

        return out;
    }


    private static String describe(Item it) {
        if (it == null) return "";
        if (it.getWeapon() != null) {
            return "Arma cuerpo a cuerpo fiable. Útil para encuentros cercanos.";
        } else if (it.getContainer() != null) {
            return "Contenedor para transportar equipo y recursos.";
        } else if (it.getArmor() != null && it.getArmor().proteccion > 0) {
            return "Pieza de protección que reduce el daño recibido.";
        } else if (it.getArmor() != null) {
            return "Prenda que aporta abrigo frente al frío.";
        } else if (it.getCategoria() == items.ItemCategory.CONSUMABLE) {
            return "Objeto consumible. Restaura o satisface necesidades.";
        } else if (it.getCategoria() == items.ItemCategory.MISC) {
            return "Objeto misceláneo con posibles usos variados.";
        } else {
            return "Objeto utilitario.";
        }
    }

    private static List<String> asciiArtFor(Item it) {
        ArrayList<String> art = new ArrayList<>();
        if (it == null) return art;

        if (it.getWeapon() != null) {
            art.add("   |   ");
            art.add("   |   ");
            art.add("  ###  ");
            art.add("  ###  ");
            art.add("  ###  ");
        } else if (it.getContainer() != null) {
            art.add(" .----. ");
            art.add("/|____|\\");
            art.add("\\|____|/");
            art.add("  |__|  ");
        } else if (it.getArmor() != null && it.getWearableSlot() == items.EquipmentSlot.HEAD) {
            art.add("  =||=  ");
            art.add(" /=||=\\ ");
            art.add(" \\=||=/ ");
        } else if (it.getArmor() != null || it.getWearableSlot() == items.EquipmentSlot.TORSO) {
            art.add(" |====| ");
            art.add(" | || | ");
            art.add(" |____| ");
        } else if (it.getCategoria() == items.ItemCategory.CONSUMABLE) {
            art.add("  ____  ");
            art.add(" |____| ");
            art.add(" |____| ");
        } else {
            art.add("  ____  ");
            art.add(" / __ \\ ");
            art.add(" \\____/ ");
        }
        return art;
    }

    private static String centerLabel(String label, int width, char fill) {
        label = label == null ? "" : label;
        if (label.length() >= width) return label.substring(0, Math.max(0, width));
        int left = (width - label.length()) / 2;
        int right = width - label.length() - left;
        return repeat(fill, left) + label + repeat(fill, right);
    }

    private static String clipAscii(String s, int max) {
        if (s == null || max <= 0) return "";
        if (s.length() <= max) return s;
        if (max <= 3) return ".".repeat(max);
        return s.substring(0, max - 3) + "...";
    }

    private static String repeat(char c, int n) {
        return (n <= 0) ? "" : String.valueOf(c).repeat(n);
    }

    private static String formatKg(double kg) {
        return String.format("%.2f kg", kg);
    }

    private static String center(String s, int w) {
        s = s == null ? "" : s;
        if (w <= 0) return "";
        if (s.length() >= w) return s.substring(0, w);
        int l = (w - s.length()) / 2;
        int r = w - s.length() - l;
        return repeat(' ', l) + s + repeat(' ', r);
    }

    private static String pad(String s, int w) {
        s = s == null ? "" : s;
        if (w <= 0) return "";
        if (s.length() >= w) return s.substring(0, w);
        return s + repeat(' ', w - s.length());
    }

    private static List<String> wrap(String text, int w) {
        ArrayList<String> out = new ArrayList<>();
        if (w <= 0 || text == null || text.isEmpty()) return out;
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String wv : words) {
            if (line.isEmpty()) {
                if (wv.length() <= w) line.append(wv);
                else out.add(wv.substring(0, w));
            } else if (line.length() + 1 + wv.length() <= w) {
                line.append(' ').append(wv);
            } else {
                out.add(pad(line.toString(), w));
                line.setLength(0);
                if (wv.length() <= w) line.append(wv);
                else out.add(wv.substring(0, w));
            }
        }
        if (!line.isEmpty()) out.add(pad(line.toString(), w));
        return out;
    }
}
