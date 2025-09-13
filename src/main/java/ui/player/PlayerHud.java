package ui.player;

import utils.ANSI;

public class PlayerHud {
    private final int filaSuperior;
    private final int colIzquierda;
    private final int anchoHeader;
    private final int anchoStats;
    private final int anchoBarra = 20;

    public PlayerHud(int filaSuperior, int colIzquierda, int anchoHeader, int anchoStats) {
        this.filaSuperior = Math.max(1, filaSuperior);
        this.colIzquierda = Math.max(1, colIzquierda);
        this.anchoHeader = Math.max(10, anchoHeader);
        this.anchoStats = Math.max(10, anchoStats);
    }

    public void renderHud(int dia, String hora, String clima, int temperatura, String ubicacion, int salud, int maxSalud, int energia, int maxEnergia, int hambre, int maxHambre, int sed, int maxSed, int sueno, int maxSueno, int px, int py, String rumbo) {

        ANSI.gotoRC(filaSuperior, colIzquierda);
        ANSI.boldOn();
        String encabezado = String.format(" Día %d   Hora: %s   Clima: %s   Temp: %d°C   Zona: %s   Coordenadas: (%d,%d)   Dirección: %s", dia, safe(hora), safe(clima), temperatura, safe(ubicacion), px, py, safe(rumbo));
        System.out.print(recortar(encabezado, anchoHeader));
        ANSI.resetStyle();
        ANSI.clearToLineEnd();

        int titleRow = filaSuperior + 2;
        titleStats(titleRow, "ESTADÍSTICAS");

        int row = titleRow + 2;

        ANSI.gotoRC(row, colIzquierda);
        imprimirBarraColoreada(recortar(formatearBarra("- Salud  ", salud, maxSalud), anchoStats));
        ANSI.clearToLineEnd();
        row += 2;

        ANSI.gotoRC(row, colIzquierda);
        imprimirBarraColoreada(recortar(formatearBarra("- Energía", energia, maxEnergia), anchoStats));
        ANSI.clearToLineEnd();
        row += 2;

        ANSI.gotoRC(row, colIzquierda);
        imprimirBarraColoreada(recortar(formatearBarra("- Hambre ", hambre, maxHambre), anchoStats));
        ANSI.clearToLineEnd();
        row += 2;

        ANSI.gotoRC(row, colIzquierda);
        imprimirBarraColoreada(recortar(formatearBarra("- Sed    ", sed, maxSed), anchoStats));
        ANSI.clearToLineEnd();
        row += 2;

        ANSI.gotoRC(row, colIzquierda);
        imprimirBarraColoreada(recortar(formatearBarra("- Sueño  ", sueno, maxSueno), anchoStats));
        ANSI.clearToLineEnd();
    }

    private void titleStats(int row, String t) {
        ANSI.gotoRC(row, colIzquierda);
        String label = " " + (t == null ? "" : t.trim()) + " ";
        if (label.length() >= anchoStats) {
            System.out.print(label.substring(0, Math.max(0, anchoStats)));
            return;
        }
        int leftDash = (anchoStats - label.length()) / 2;
        int rightDash = anchoStats - label.length() - leftDash;
        System.out.print("─".repeat(leftDash));
        System.out.print(label);
        System.out.print("─".repeat(rightDash));
    }

    private String formatearBarra(String etiqueta, int valor, int max) {
        int v = Math.max(0, Math.min(valor, Math.max(1, max)));
        int m = Math.max(1, max);
        int filled = (int) Math.round((v / (double) m) * anchoBarra);
        int empty = Math.max(0, anchoBarra - filled);
        String barra = "[" + "█".repeat(Math.max(0, filled)) + "-".repeat(empty) + "]";
        return String.format("%s %s %d/%d", etiqueta, barra, v, m);
    }

    private void imprimirBarraColoreada(String barraConEtiqueta) {
        int open = barraConEtiqueta.indexOf('[');
        int close = barraConEtiqueta.indexOf(']', open + 1);
        if (open >= 0 && close > open) {
            String etiqueta = barraConEtiqueta.substring(0, open);
            String barra = barraConEtiqueta.substring(open, close + 1);
            String resto = barraConEtiqueta.substring(close + 1);
            System.out.print(etiqueta);
            ANSI.setFg(getColor(barraConEtiqueta));
            System.out.print(barra);
            ANSI.resetStyle();
            System.out.print(resto);
        } else {
            System.out.print(barraConEtiqueta);
        }
    }

    private static int getColor(String barraConEtiqueta) {
        int idx = barraConEtiqueta.lastIndexOf(' ');
        String stats = (idx >= 0) ? barraConEtiqueta.substring(idx + 1) : "";
        int slash = stats.indexOf('/');
        double pct = 1.0;
        if (slash > 0) {
            try {
                int val = Integer.parseInt(stats.substring(0, slash));
                int max = Integer.parseInt(stats.substring(slash + 1));
                max = Math.max(1, max);
                pct = val / (double) max;
            } catch (Exception ignore) {
            }
        }
        if (pct >= 0.66) return 32;
        else if (pct >= 0.33) return 33;
        else return 31;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static String recortar(String s, int ancho) {
        if (s == null) return "";
        if (s.length() <= ancho) return s;
        return s.substring(0, Math.max(0, ancho));
    }
}
