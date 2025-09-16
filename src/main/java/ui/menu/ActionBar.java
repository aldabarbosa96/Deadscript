package ui.menu;

import utils.ANSI;

import static utils.UI.clipAscii;

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
        int inner = Math.max(0, width - 2);

        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print("─".repeat(inner));
            System.out.print('┐');
        } else {
            System.out.print("─".repeat(width));
        }
        ANSI.clearToLineEnd();

        String full = "  Inventario [I]   Equipo [E]   Estadísticas [S]   Mover: [Flechas]   Acción: [Espacio]   Opciones [O]   Salir [Q]";
        full = clipAscii(full, inner);
        ANSI.gotoRC(top + 1, left);
        if (width >= 2) {
            System.out.print('│');
            System.out.print(full);
            if (full.length() < inner) System.out.print(" ".repeat(inner - full.length()));
            System.out.print('│');
        } else {
            System.out.print(clipAscii(full, width));
        }
        ANSI.clearToLineEnd();
        ANSI.gotoRC(top + 2, left);
        if (width >= 2) {
            System.out.print('└');
            System.out.print("─".repeat(inner));
            System.out.print('┘');
        } else {
            System.out.print("─".repeat(width));
        }
        ANSI.clearToLineEnd();
    }
}
