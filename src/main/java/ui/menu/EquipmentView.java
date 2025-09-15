package ui.menu;

import items.Equipment;
import items.Item;
import utils.ANSI;

import java.util.List;

public class EquipmentView {

    public void render(int top, int left, int width, int height, Equipment eq, List<Item> inventory, int selectedIndex) {
        if (width < 28 || height < 10) {
            renderFallback(top, left, width, height, eq, inventory, selectedIndex);
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

        double peso = eq.pesoTotalKg(inventory);
        double capa = Math.max(0.0001, eq.capacidadKg());
        int prot = eq.proteccionTotal();
        int abrigo = eq.abrigoTotal();
        String resumen = String.format(" Protección: %d   Abrigo: %d   Carga: %.2f/%.2f kg", prot, abrigo, peso, capa);
        ANSI.gotoRC(baseTop, baseLeft);
        System.out.print(clip(resumen, drawW));

        final int actionsRow = baseTop + drawH - 1;
        if (actionsRow <= baseTop + 1 || drawW < 30) {
            renderFallback(top, left, width, height, eq, inventory, selectedIndex);
            return;
        }

        final int figAreaTop = baseTop + 1;
        final int figAreaH = (actionsRow - 1) - figAreaTop + 1;
        final int figH = 14;
        final int gapSide = 10;

        if (figAreaH < Math.min(10, figH - 2)) {
            renderFallback(top, left, width, height, eq, inventory, selectedIndex);
            return;
        }

        final int cx = baseLeft + drawW / 2;
        final int figTop = figAreaTop + Math.max(0, (figAreaH - figH) / 2);
        final int colLend = cx - gapSide;
        final int colRstart = cx + gapSide;

        drawStickman(figTop, cx);

        String sHead = name(eq.getHead());
        String sChest = name(eq.getChest());
        String sLegs = name(eq.getLegs());
        String sFeet = name(eq.getFeet());
        String sMain = name(eq.getMainHand());
        String sOff = name(eq.getOffHand());
        String sPack = name(eq.getBackpack());

        int idxHead = 0, idxPack = 1, idxChest = 2, idxOff = 3, idxMain = 4, idxLegs = 5, idxFeet = 6;

        int headRow = Math.max(baseTop + 1, figTop - 3);
        printCenteredLabeled(headRow, baseLeft, drawW, "Cabeza: ", sHead, selectedIndex == idxHead);

        int chestRow = figTop + 4;
        int packRow = figTop + 4;
        printRightLabeled(chestRow, baseLeft, colLend, "Pecho: ", sChest, selectedIndex == idxChest);
        printLeftLabeled(packRow, colRstart, baseLeft + drawW, "Mochila: ", sPack, selectedIndex == idxPack);

        int armsRow = figTop + 8;
        printRightLabeled(armsRow, baseLeft, colLend, "Mano izq.: ", sOff, selectedIndex == idxOff);
        printLeftLabeled(armsRow, colRstart, baseLeft + drawW, "Mano der.: ", sMain, selectedIndex == idxMain);

        int legsRow = figTop + 10;
        printLeftLabeled(legsRow, colRstart, baseLeft + drawW, "Piernas: ", sLegs, selectedIndex == idxLegs);

        int feetRow = Math.min(actionsRow - 1, figTop + 16);
        printCenteredLabeled(feetRow, baseLeft, drawW, "Pies: ", sFeet, selectedIndex == idxFeet);

        String help = " [E] Cerrar    [Flechas] Seleccionar    [Espacio] Acciones ";
        ANSI.gotoRC(actionsRow, baseLeft);
        System.out.print(clip(centerLabel(help, drawW, ' '), drawW));
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

    private void printCenteredLabeled(int row, int areaLeft, int areaWidth, String key, String val, boolean selected) {
        String prefix = selected ? ">> " : "  ";
        String text = prefix + key + val;
        String s = clip(text, areaWidth);
        int start = areaLeft + Math.max(0, (areaWidth - s.length()) / 2);
        putSelected(row, start, s, selected);
    }

    private void printRightLabeled(int row, int leftBound, int rightBound, String key, String val, boolean selected) {
        int span = Math.max(0, rightBound - leftBound);
        if (span <= 0) return;
        String prefix = selected ? ">> " : "  ";
        String s = clip(prefix + key + val, span);
        int start = rightBound - s.length();
        putSelected(row, start, s, selected);
    }

    private void printLeftLabeled(int row, int leftBound, int rightBound, String key, String val, boolean selected) {
        int span = Math.max(0, rightBound - leftBound);
        if (span <= 0) return;
        String prefix = selected ? ">> " : "  ";
        String s = clip(prefix + key + val, span);
        putSelected(row, leftBound, s, selected);
    }

    private void putSelected(int row, int col, String s, boolean selected) {
        if (s == null || s.isEmpty()) return;
        ANSI.gotoRC(row, col);
        if (selected) ANSI.boldOn();
        System.out.print(s);
        if (selected) ANSI.boldOff();
    }

    private void renderFallback(int top, int left, int width, int height, Equipment eq, List<Item> inv, int selectedIndex) {
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

        double peso = eq.pesoTotalKg(inv);
        double capa = Math.max(0.0001, eq.capacidadKg());
        int prot = eq.proteccionTotal();
        int abrigo = eq.abrigoTotal();
        String resumen = clip(String.format("Prot:%d  Abr:%d  Carga: %.2f/%.2f kg", prot, abrigo, peso, capa), inner);
        ANSI.gotoRC(baseTop, baseLeft);
        System.out.print(resumen);

        String[] lines = new String[]{"Cabeza: " + name(eq.getHead()), "Mochila: " + name(eq.getBackpack()), "Pecho: " + name(eq.getChest()), "Mano izq.: " + name(eq.getOffHand()), "Mano der.: " + name(eq.getMainHand()), "Piernas: " + name(eq.getLegs()), "Pies: " + name(eq.getFeet())};

        int listStart = baseTop + 1;
        int listRows = Math.max(0, rows - 2);
        int show = Math.min(listRows, lines.length);
        for (int i = 0; i < show; i++) {
            ANSI.gotoRC(listStart + i, baseLeft);
            boolean selected = (i == Math.floorMod(selectedIndex, 7));
            String prefix = selected ? ">> " : "  ";
            String body = clip(prefix + lines[i], inner);
            if (selected) ANSI.boldOn();
            System.out.print(body);
            if (selected) ANSI.boldOff();
        }

        String help = " [E] Cerrar    [Flechas] Seleccionar    [Espacio] Acciones ";
        ANSI.gotoRC(baseTop + rows - 1, baseLeft);
        System.out.print(clip(centerLabel(help, inner, ' '), inner));
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


    public void renderActionMenu(int top, int left, int width, int height, java.util.List<String> options, int selectedIndex) {
        if (options == null || options.isEmpty()) return;

        int inner = Math.max(0, width - 2);
        int rows = Math.max(1, height - 3);
        int baseTop = top + 1;
        int baseLeft = left + 1;

        int maxLen = 0;
        for (String s : options) maxLen = Math.max(maxLen, s == null ? 0 : s.length());
        int boxH = Math.min(options.size() + 2, Math.max(3, rows / 2));
        int boxW = Math.min(Math.max(14, maxLen + 6), Math.max(16, inner / 2));

        // Encima del placeholder "[Espacio] Acciones"
        int actionsRow = baseTop + rows - 1;
        int anchorTop = actionsRow - boxH - 2;

        // Panel derecho (igual que inventario)
        int drawW = inner;
        int cx = baseLeft + drawW / 2;
        int gapSide = 10;
        int rightStart = cx + gapSide;
        int anchorLeft = rightStart + 1;
        int detailW = baseLeft + drawW - rightStart;
        boxW = Math.min(boxW, Math.max(10, detailW - 2));

        // Marco
        ANSI.gotoRC(anchorTop, anchorLeft);
        System.out.print('┌');
        System.out.print(repeat('─', boxW - 2));
        System.out.print('┐');
        for (int i = 0; i < boxH - 2; i++) {
            ANSI.gotoRC(anchorTop + 1 + i, anchorLeft);
            System.out.print('│');
            System.out.print(repeat(' ', boxW - 2));
            System.out.print('│');
        }
        ANSI.gotoRC(anchorTop + boxH - 1, anchorLeft);
        System.out.print('└');
        System.out.print(repeat('─', boxW - 2));
        System.out.print('┘');

        // Título
        String title = " ACCIONES ";
        ANSI.gotoRC(anchorTop, anchorLeft + Math.max(1, (boxW - title.length()) / 2));
        System.out.print(clip(title, Math.max(0, boxW - 2)));

        // Opciones (scroll sencillo si no caben)
        int maxOpts = boxH - 2;
        int start = 0;
        if (selectedIndex >= maxOpts) start = selectedIndex - (maxOpts - 1);

        for (int i = 0; i < maxOpts; i++) {
            int idx = start + i;
            String opt = (idx < options.size()) ? options.get(idx) : "";
            boolean sel = (idx == selectedIndex);
            String prefix = sel ? "» " : "  ";
            String line = clip(prefix + opt, boxW - 2);
            ANSI.gotoRC(anchorTop + 1 + i, anchorLeft + 1);
            if (sel) ANSI.boldOn();
            System.out.print(line);
            if (line.length() < boxW - 2) System.out.print(repeat(' ', (boxW - 2) - line.length()));
            if (sel) ANSI.boldOff();
        }
    }

    public void renderSelectMenu(int top, int left, int width, int height, java.util.List<Item> items, int selectedIndex, String title, boolean disabled) {
        if (items == null) items = java.util.Collections.emptyList();

        int inner = Math.max(0, width - 2);
        int rows = Math.max(1, height - 3);
        int baseTop = top + 1;
        int baseLeft = left + 1;

        int maxLen = 0;
        for (Item it : items) {
            String n = (it == null || it.getNombre() == null) ? "" : it.getNombre();
            if (n.length() > maxLen) maxLen = n.length();
        }

        int boxH = Math.min(Math.max(3, items.size() + 2), Math.max(3, rows / 2));
        int drawW = inner;
        int cx = baseLeft + drawW / 2;
        int gapSide = 8;
        int rightStart = cx + gapSide;
        int detailW = baseLeft + drawW - rightStart;
        if (detailW < 8) return;

        int minInner = Math.max(16, Math.max(16, maxLen + 6));
        int availInner = Math.max(6, detailW - 2);
        int boxW = Math.min(minInner + 2, availInner + 2);

        int actionsRow = baseTop + rows - 1;
        int anchorTop = Math.max(baseTop, actionsRow - boxH - 2);
        int anchorLeft = rightStart + 1;

        ANSI.gotoRC(anchorTop, anchorLeft);
        System.out.print('┌');
        System.out.print(repeat('─', boxW - 2));
        System.out.print('┐');

        for (int i = 0; i < boxH - 2; i++) {
            ANSI.gotoRC(anchorTop + 1 + i, anchorLeft);
            System.out.print('│');
            System.out.print(repeat(' ', boxW - 2));
            System.out.print('│');
        }
        ANSI.gotoRC(anchorTop + boxH - 1, anchorLeft);
        System.out.print('└');
        System.out.print(repeat('─', boxW - 2));
        System.out.print('┘');

        String titleText = " " + ((title == null) ? "SELECCIONAR" : title.trim()) + " ";
        ANSI.gotoRC(anchorTop, anchorLeft + Math.max(1, (boxW - titleText.length()) / 2));
        System.out.print(clip(titleText, Math.max(0, boxW - 2)));

        int maxOpts = boxH - 2;
        if (items.isEmpty()) {
            String placeholder = "— Sin opciones —";
            ANSI.gotoRC(anchorTop + 1, anchorLeft + 1);
            System.out.print(clip(placeholder, boxW - 2));
            return;
        }

        int sel = Math.max(0, Math.min(selectedIndex, items.size() - 1));
        int start = 0;
        if (sel >= maxOpts) start = sel - (maxOpts - 1);

        for (int i = 0; i < maxOpts; i++) {
            int idx = start + i;
            String opt = (idx < items.size() && items.get(idx) != null) ? items.get(idx).getNombre() : "";
            boolean isSel = (idx == sel);
            String prefix = isSel ? "» " : "  ";
            String line = clip(prefix + opt, boxW - 2);
            ANSI.gotoRC(anchorTop + 1 + i, anchorLeft + 1);
            if (isSel) ANSI.boldOn();
            System.out.print(line);
            if (line.length() < boxW - 2) System.out.print(repeat(' ', (boxW - 2) - line.length()));
            if (isSel) ANSI.boldOff();
        }
    }
}
