package ui.player;

import utils.ANSI;

import java.util.ArrayList;
import java.util.List;

public class PlayerStates {
    private final int topRow;
    private final int leftCol;
    private final int width;
    private final int maxLines;

    public PlayerStates(int topRow, int leftCol, int width) {
        this(topRow, leftCol, width, 6);
    }

    public PlayerStates(int topRow, int leftCol, int width, int maxLines) {
        this.topRow = Math.max(1, topRow);
        this.leftCol = Math.max(1, leftCol);
        this.width = Math.max(10, width);
        this.maxLines = Math.max(2, maxLines);
    }

    public void renderStates(int salud, int maxSalud, int energia, int maxEnergia, int hambre, int maxHambre, int sed, int maxSed, int sueno, int maxSueno, boolean sangrado, int infeccionPct, boolean escondido) {

        // 1) Limpiar completamente el panel (título + línea en blanco + cuerpo)
        clearPanel();

        // 2) Recalcular badges en base al estado actual
        List<Badge> activos = new ArrayList<>();

        double ps = ratio(salud, maxSalud);
        if (ps < 0.15) activos.add(new Badge("CRÍTICO", 31));
        else if (ps < 0.33) activos.add(new Badge("HERIDO GRAVE", 31));
        else if (ps < 0.66) activos.add(new Badge("HERIDO", 33));

        double pe = ratio(energia, maxEnergia);
        if (pe <= 0.15) activos.add(new Badge("EXHAUSTO", 31));
        else if (pe <= 0.30) activos.add(new Badge("FATIGADO", 33));

        double ph = ratio(hambre, maxHambre);
        if (ph <= 0.33) activos.add(new Badge("HAMBRIENTO CRÍTICO", 31));
        else if (ph <= 0.50) activos.add(new Badge("MUY HAMBRIENTO", 33));
        else if (ph <= 0.66) activos.add(new Badge("HAMBRIENTO", 33));

        double psed = ratio(sed, maxSed);
        if (psed <= 0.15) activos.add(new Badge("DESHIDRATADO", 31));
        else if (psed <= 0.50) activos.add(new Badge("MUY SEDIENTO", 33));
        else if (psed <= 0.66) activos.add(new Badge("SEDIENTO", 33));

        double psueno = ratio(sueno, maxSueno);
        if (psueno <= 0.15) activos.add(new Badge("EXTENUADO", 31));
        else if (psueno <= 0.50) activos.add(new Badge("MUY CANSADO", 33));
        else if (psueno <= 0.66) activos.add(new Badge("SOMNOLIENTO", 33));

        if (sangrado) activos.add(new Badge("SANGRADO", 31));

        int inf = Math.max(0, Math.min(100, infeccionPct));
        if (inf > 0) {
            int color = (inf >= 50) ? 35 : 33;
            activos.add(new Badge("INFECCIÓN " + inf + "%", color));
        }

        if (escondido) activos.add(new Badge("ESCONDIDO", 36));

        // 3) Título y contenido
        title(topRow, "ESTADOS");

        int row = topRow + 2; // una línea en blanco bajo el título
        int printedLines = 0;
        int i = 0;

        while (i < activos.size() && printedLines < maxLines) {
            ANSI.gotoRC(row, leftCol);

            int used = 0;
            boolean first = true;

            while (i < activos.size()) {
                String visible = "[" + activos.get(i).text + "]";
                int needed = visible.length() + (first ? 0 : 1);
                if (used + needed > width) break;

                if (!first) {
                    System.out.print(" ");
                    used += 1;
                }
                printBadge(visible, activos.get(i).color);
                used += visible.length();

                i++;
                first = false;
            }

            // Relleno para cubrir hasta el final de línea
            if (used < width) System.out.print(" ".repeat(width - used));

            ANSI.resetStyle();
            printedLines++;
            row++;
        }

        // 4) Borrar líneas sobrantes si esta vez hay menos badges
        for (int k = printedLines; k < maxLines; k++) {
            ANSI.gotoRC(topRow + 2 + k, leftCol);
            System.out.print(" ".repeat(width));
        }
    }

    private void clearPanel() {
        for (int r = 0; r < maxLines + 2; r++) {
            ANSI.gotoRC(topRow + r, leftCol);
            System.out.print(" ".repeat(width));
        }
    }


    private void title(int row, String t) {
        ANSI.gotoRC(row, leftCol);
        String label = " " + (t == null ? "" : t.trim()) + " ";
        if (label.length() >= width) {
            System.out.print(label.substring(0, Math.max(0, width)));
            return;
        }
        int leftDash = (width - label.length()) / 2;
        int rightDash = width - label.length() - leftDash;
        System.out.print("─".repeat(leftDash));
        System.out.print(label);
        System.out.print("─".repeat(rightDash));
    }

    private static double ratio(int value, int max) {
        int m = Math.max(1, max);
        int v = Math.max(0, Math.min(value, m));
        return v / (double) m;
    }

    private static void printBadge(String visibleWithBrackets, int fgColor) {
        ANSI.setFg(fgColor);
        System.out.print(visibleWithBrackets);
        ANSI.resetStyle();
    }

    private static class Badge {
        final String text;
        final int color;

        Badge(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }
}
