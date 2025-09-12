package ui;

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
        List<Badge> activos = new ArrayList<>();

        // Salud
        double ps = ratio(salud, maxSalud);
        if (ps < 0.15) activos.add(new Badge("CRÍTICO", 31));
        else if (ps < 0.33) activos.add(new Badge("HERIDO GRAVE", 31));
        else if (ps < 0.66) activos.add(new Badge("HERIDO", 33));

        // Energía
        double pe = ratio(energia, maxEnergia);
        if (pe <= 0.15) activos.add(new Badge("EXHAUSTO", 31));
        else if (pe <= 0.30) activos.add(new Badge("FATIGADO", 33));

        // Hambre
        double ph = ratio(hambre, maxHambre);
        if (ph <= 0.33) activos.add(new Badge("HAMBRIENTO CRÍTICO", 31));
        else if (ph <= 0.50) activos.add(new Badge("MUY HAMBRIENTO", 33));
        else if (ph <= 0.66) activos.add(new Badge("HAMBRIENTO", 33));

        // Sed
        double psed = ratio(sed, maxSed);
        if (psed <= 0.15) activos.add(new Badge("DESHIDRATADO", 31));
        else if (psed <= 0.50) activos.add(new Badge("MUY SEDIENTO", 33));
        else if (psed <= 0.66) activos.add(new Badge("SEDIENTO", 33));

        // Sueño
        double psueno = ratio(sueno, maxSueno);
        if (psueno <= 0.15) activos.add(new Badge("EXTASIADO", 31));
        else if (psueno <= 0.50) activos.add(new Badge("MUY CANSADO", 33));
        else if (psueno <= 0.66) activos.add(new Badge("SOMNOLIENTO", 33));

        // Sangrado
        if (sangrado) activos.add(new Badge("SANGRADO", 31));

        // Infección
        int inf = Math.max(0, Math.min(100, infeccionPct));
        if (inf > 0) {
            int color = (inf >= 50) ? 35 : 33;
            activos.add(new Badge("INFECCIÓN " + inf + "%", color));
        }

        // Escondido
        if (escondido) activos.add(new Badge("ESCONDIDO", 36));

        // 4) Pintar badges en filas de hasta 3 por línea
        int row = topRow;
        int printedLines = 0;
        for (int i = 0; i < activos.size() && printedLines < maxLines; i += 3) {
            ANSI.gotoRC(row++, leftCol);
            for (int j = 0; j < 3 && i + j < activos.size(); j++) {
                Badge b = activos.get(i + j);
                printBadge(b.text, b.color);
                System.out.print(" ");
            }

            ANSI.resetStyle();
            ANSI.clearToLineEnd();
            printedLines++;
        }

        // 5) Limpiar líneas sobrantes
        for (int i = printedLines; i < maxLines; i++) {
            ANSI.gotoRC(topRow + i, leftCol);
            ANSI.clearToLineEnd();
        }
    }


    private static double ratio(int value, int max) {
        int m = Math.max(1, max);
        int v = Math.max(0, Math.min(value, m));
        return v / (double) m;
    }

    private static void printBadge(String text, int fgColor) {
        ANSI.setFg(fgColor);
        System.out.print("[" + text + "]");
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
