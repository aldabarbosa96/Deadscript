package ui.menu;

import utils.ANSI;

public class ActionBar {
    private int top;
    private int left;
    private int width;

    public ActionBar(int top, int left, int width) {
        updateGeometry(top, left, width);
    }

    public void updateGeometry(int top, int left, int width) {
        this.top = Math.max(1, top);
        this.left = Math.max(1, left);
        this.width = Math.max(20, width);
    }

    public void render() {
        ANSI.gotoRC(top, left);

        String full = " Inventario [I]   Equipo [E]   Estadísticas [S]   Mover: [Flechas]   Acción: [Espacio]   Opciones [O]   Salir [Q]";

        // Si no cabe, recortamos con "..."
        if (full.length() > width) {
            full = clipEndAscii(full, width);
        }

        System.out.print(full);
        if (full.length() < width) {
            System.out.print(" ".repeat(width - full.length()));
        }
        ANSI.clearToLineEnd();
    }

    private static String clipEndAscii(String s, int max) {
        if (s == null) return "";
        if (max <= 0) return "";
        if (s.length() <= max) return s;
        if (max <= 3) return ".".repeat(max);
        return s.substring(0, max - 3) + "...";
    }
}
