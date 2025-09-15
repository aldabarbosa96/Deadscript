package ui.menu;

import items.Equipment;
import items.Item;
import utils.ANSI;

import java.util.List;

public class EquipmentView {

    public void render(int top, int left, int width, int height, Equipment eq, List<Item> inventory) {
        if (width < 28 || height < 16) {
            renderFallback(top, left, width, height, eq, inventory);
            return;
        }

        final int inner = Math.max(0, width - 2);
        final int rows = Math.max(1, height - 3);

        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print(centerLabel(" EQUIPO ", inner, '─'));
            System.out.print('┐');
        } else {
            System.out.print(repeat('─', width));
        }

        for (int i = 0; i < rows; i++) {
            ANSI.gotoRC(top + 1 + i, left);
            if (width >= 2) {
                System.out.print('│');
                System.out.print(repeat(' ', inner));
                System.out.print('│');
            } else {
                System.out.print(repeat(' ', width));
            }
        }

        ANSI.gotoRC(top + 1 + rows, left);
        if (width >= 2) {
            System.out.print('└');
            System.out.print(repeat('─', inner));
            System.out.print('┘');
        } else {
            System.out.print(repeat('─', width));
        }

        final int baseTop = top + 1;
        final int baseLeft = left + 1;
        final int drawH = rows;
        final int drawW = inner;

        final int figH = 14;
        final int gapSide = 10;

        final int cx = baseLeft + drawW / 2;
        final int figTop = baseTop + Math.max(1, (drawH - figH) / 2);
        final int figBottom = figTop + figH - 1;

        int leftAvail = (cx - gapSide) - baseLeft;
        int rightAvail = (baseLeft + drawW) - (cx + gapSide);
        if (leftAvail < 10 || rightAvail < 10 || drawH < figH + 3) {
            renderFallback(top, left, width, height, eq, inventory);
            return;
        }

        drawStickman(figTop, cx);

        String sHead = name(eq.getHead());
        String sChest = name(eq.getChest());
        String sLegs = name(eq.getLegs());
        String sFeet = name(eq.getFeet());
        String sMain = name(eq.getMainHand());
        String sOff = name(eq.getOffHand());
        String sPack = name(eq.getBackpack());

        int colLend = cx - gapSide;
        int colRstart = cx + gapSide;

        int headRow = Math.max(baseTop, figTop - 3);
        printCentered(headRow, baseLeft, drawW, "Cabeza: " + sHead);

        int chestRow = figTop + 4;
        int packRow = figTop + 4;
        printRight(chestRow, baseLeft, colLend, "Pecho: " + sChest);
        printLeft(packRow, colRstart, baseLeft + drawW, "Mochila: " + sPack);

        int handsRow = figTop + 6;
        printRight(handsRow, baseLeft, colLend, "Mano izq.: " + sOff);
        printLeft(handsRow, colRstart, baseLeft + drawW, "Mano der.: " + sMain);

        int legsRow = figTop + 10;
        printLeft(legsRow, colRstart, baseLeft + drawW, "Piernas: " + sLegs);

        int feetRow = Math.min(figBottom + 2, baseTop + drawH - 1);
        printCentered(feetRow, baseLeft, drawW, "Pies: " + sFeet);

        double peso = eq.pesoTotalKg(inventory);
        double capa = Math.max(0.0001, eq.capacidadKg());
        int prot = eq.proteccionTotal();
        int abrigo = eq.abrigoTotal();
        String resumen = String.format(" Protección: %d   Abrigo: %d   Carga: %.2f/%.2f kg", prot, abrigo, peso, capa);

        ANSI.gotoRC(top + rows, baseLeft);
        System.out.print(clip(resumen, inner));
    }

    private void drawStickman(int figTop, int cx) {
        put(figTop + 0, cx - 3, "  ___  ");
        put(figTop + 1, cx - 3, " /   \\");
        put(figTop + 2, cx - 3, "|     |");
        put(figTop + 3, cx - 3, " \\___/");
        put(figTop + 4, cx, "|");
        put(figTop + 5, cx, "|");
        put(figTop + 6, cx - 5, "/----|----\\");
        put(figTop + 7, cx, "|");
        put(figTop + 8, cx, "|");
        put(figTop + 9, cx, "|");
        put(figTop + 10, cx - 1, "/ \\");
        put(figTop + 11, cx - 2, "/   \\");
        put(figTop + 12, cx - 3, "/     \\");
        put(figTop + 13, cx - 4, "/       \\");
    }

    private void printCentered(int row, int areaLeft, int areaWidth, String text) {
        String s = clip(text, areaWidth);
        int start = areaLeft + Math.max(0, (areaWidth - s.length()) / 2);
        put(row, start, s);
    }

    private void printRight(int row, int leftBound, int rightBound, String text) {
        int span = Math.max(0, rightBound - leftBound);
        if (span <= 0) return;
        String s = clip(text, span);
        int start = rightBound - s.length();
        put(row, start, s);
    }

    private void printLeft(int row, int leftBound, int rightBound, String text) {
        int span = Math.max(0, rightBound - leftBound);
        if (span <= 0) return;
        String s = clip(text, span);
        put(row, leftBound, s);
    }

    private void renderFallback(int top, int left, int width, int height, Equipment eq, List<Item> inv) {
        final int inner = Math.max(0, width - 2);
        final int rows = Math.max(1, height - 3);

        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print(centerLabel(" EQUIPO ", inner, '─'));
            System.out.print('┐');
        } else {
            System.out.print(repeat('─', width));
        }

        String[] lines = new String[]{"Cabeza: " + name(eq.getHead()), "Pecho: " + name(eq.getChest()), "Piernas: " + name(eq.getLegs()), "Pies: " + name(eq.getFeet()), "Mochila: " + name(eq.getBackpack()), "Mano der.: " + name(eq.getMainHand()), "Mano izq.: " + name(eq.getOffHand())};

        int show = Math.min(rows - 1, lines.length);
        for (int i = 0; i < rows; i++) {
            ANSI.gotoRC(top + 1 + i, left);
            if (width >= 2) {
                System.out.print('│');
                String body = (i < show) ? clip(lines[i], inner) : repeat(' ', inner);
                if (body.length() < inner) body = body + repeat(' ', inner - body.length());
                System.out.print(body);
                System.out.print('│');
            } else {
                System.out.print(clip(i < show ? lines[i] : "", width));
            }
        }

        double peso = eq.pesoTotalKg(inv);
        double capa = Math.max(0.0001, eq.capacidadKg());
        int prot = eq.proteccionTotal();
        int abrigo = eq.abrigoTotal();
        String resumen = clip(String.format("Prot:%d  Abr:%d  Carga: %.2f/%.2f kg", prot, abrigo, peso, capa), inner);

        ANSI.gotoRC(top + rows, left);
        if (width >= 2) {
            System.out.print('└');
            System.out.print(centerLabel(" " + resumen + " ", inner, '─'));
            System.out.print('┘');
        } else {
            System.out.print(clip(resumen, width));
        }
    }

    private static void put(int row, int col, String s) {
        if (s == null || s.isEmpty()) return;
        ANSI.gotoRC(row, col);
        System.out.print(s);
    }

    private static String name(Item it) {
        return it == null ? "-" : it.getNombre();
    }

    private static String centerLabel(String label, int width, char fill) {
        label = label == null ? "" : label;
        if (label.length() >= width) return label.substring(0, Math.max(0, width));
        int left = (width - label.length()) / 2;
        int right = width - label.length() - left;
        return repeat(fill, left) + label + repeat(fill, right);
    }

    private static String clip(String s, int max) {
        if (s == null || max <= 0) return "";
        if (s.length() <= max) return s;
        if (max <= 3) return ".".repeat(max);
        return s.substring(0, max - 3) + "...";
    }

    private static String repeat(char c, int n) {
        return (n <= 0) ? "" : String.valueOf(c).repeat(n);
    }
}
