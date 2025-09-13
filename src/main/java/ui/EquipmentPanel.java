package ui;

import utils.ANSI;

public class EquipmentPanel {
    private final int top;
    private final int left;
    private final int width;
    private final int maxRows;

    public EquipmentPanel(int top, int left, int width, int maxRows) {
        this.top = Math.max(1, top);
        this.left = Math.max(1, left);
        this.width = Math.max(16, width);
        this.maxRows = Math.max(6, maxRows);
    }

    public void render(String arma, String offhand, String cabeza, String pecho, String manos, String piernas, String pies, String mochila, int municion, int municionMax, double peso, double capacidad) {

        int row = top;
        title(row++, " EQUIPO ");
        row = kv(row, "Cabeza", cabeza);
        row = kv(row, "Pecho", pecho);
        row = kv(row, "Manos", manos);
        row = kv(row, "Piernas", piernas);
        row = kv(row, "Pies", pies);
        row = kv(row, "Mochila", mochila);
        row = kv(row, "Arma", arma);
        row = kv(row, "Off", offhand);
        row = kv(row, "Munic.", municion + "/" + municionMax);
        row = bar(row, "Peso", peso, capacidad);

        while (row < top + maxRows) {
            ANSI.gotoRC(row++, left);
            ANSI.clearToLineEnd();
        }
    }

    private void title(int row, String t) {
        ANSI.gotoRC(row, left);
        String head = "─" + t + "─";
        String line = head + "─".repeat(Math.max(0, width - head.length()));
        System.out.print(line);
    }

    private int kv(int row, String k, String v) {
        if (row >= top + maxRows) return row;
        ANSI.gotoRC(row, left);
        String s = String.format("%-8s %s", (k + ":"), safe(v));
        if (s.length() > width) s = s.substring(0, width);
        System.out.print(s);
        ANSI.clearToLineEnd();
        return row + 1;
    }

    private int bar(int row, String name, double val, double max) {
        if (row >= top + maxRows) return row;

        int ival = (int) Math.round(val);
        int imax = (int) Math.round(Math.max(0.0001, max));

        String numStr = ival + "/" + imax;
        int digitsLen = numStr.length();

        int fixed = 10 + 2 + digitsLen; // label(8) + " ["(2)  + "] "(2) + digits
        int barW = Math.max(3, width - fixed);

        double pct = Math.max(0, Math.min(1, (double) ival / Math.max(1, imax)));
        int filled = (int) Math.round(barW * pct);

        ANSI.gotoRC(row, left);
        System.out.printf("%-8s [", name + ":");
        System.out.print("█".repeat(Math.max(0, filled)));
        System.out.print("-".repeat(Math.max(0, barW - filled)));
        System.out.print("] ");
        System.out.print(numStr);
        ANSI.clearToLineEnd();
        return row + 1;
    }

    private static String safe(String s) {
        return s == null ? "-" : s;
    }
}
