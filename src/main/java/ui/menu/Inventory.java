package ui.menu;

import utils.ANSI;

import java.util.List;

public class Inventory {

    public void render(int top, int left, int width, int height, List<String> items, int selectedIndex) {
        if (width < 10 || height < 5) return;

        final int inner = Math.max(0, width - 2);
        final int listRows = Math.max(1, height - 3); // top border + footer + bottom border
        final int n = items == null ? 0 : items.size();
        int sel = Math.max(0, Math.min(selectedIndex, Math.max(0, n - 1)));

        // centrar selección: que quede más o menos en medio
        int start = Math.max(0, Math.min(sel - listRows / 2, Math.max(0, n - listRows)));

        // ┌── INVENTARIO ──┐
        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print(centerLabel(" INVENTARIO ", inner, '─'));
            System.out.print('┐');
        } else {
            System.out.print(repeat('─', width));
        }

        // Lista
        for (int i = 0; i < listRows; i++) {
            int idx = start + i;
            String line = (idx < n) ? items.get(idx) : "";
            String view = clipAscii(line, inner);
            boolean selected = (idx == sel);

            ANSI.gotoRC(top + 1 + i, left);
            if (width >= 2) {
                System.out.print('│');
                if (selected) ANSI.boldOn();
                // prefijo selección
                String prefix = selected ? "» " : "  ";
                String body = clipAscii(prefix + view, inner);
                System.out.print(body);
                if (body.length() < inner) System.out.print(repeat(' ', inner - body.length()));
                if (selected) ANSI.boldOff();
                System.out.print('│');
            } else {
                String body = clipAscii((selected ? "» " : "  ") + view, width);
                System.out.print(body);
            }
        }

        // Footer de ayuda
        String help = " [I] Cerrar    [↑/↓] Navegar ";
        ANSI.gotoRC(top + 1 + listRows, left);
        if (width >= 2) {
            System.out.print('│');
            String body = clipAscii(centerLabel(help, inner, ' '), inner);
            System.out.print(body);
            if (body.length() < inner) System.out.print(repeat(' ', inner - body.length()));
            System.out.print('│');
        } else {
            System.out.print(clipAscii(help, width));
        }

        // └───────────────┘
        ANSI.gotoRC(top + 2 + listRows, left);
        if (width >= 2) {
            System.out.print('└');
            System.out.print(repeat('─', inner));
            System.out.print('┘');
        } else {
            System.out.print(repeat('─', width));
        }
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
}
