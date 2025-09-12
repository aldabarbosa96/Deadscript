package utils;

public final class ANSI {
    private static boolean enabled = false;

    private ANSI() {
    }

    // control ANSI
    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    // control pantalla y cursor
    public static void clearScreenAndHome() {
        if (!enabled) return;
        System.out.print("\u001B[2J\u001B[H");
    }

    public static void useAltScreen(boolean on) {
        if (!enabled) return;
        if (on) {
            System.out.print("\u001B[?1049h");
        } else {
            System.out.print("\u001B[?1049l");
        }
    }

    public static void setCursorVisible(boolean on) {
        if (!enabled) return;
        if (on) {
            System.out.print("\u001B[?25h");
        } else {
            System.out.print("\u001B[?25l");
        }
    }

    // movimiento cursor
    public static void gotoRC(int row, int col) {
        if (!enabled) return;
        if (row < 1 || col < 1) {
            throw new IllegalArgumentException("Fila y columna deben ser >= 1");
        }
        System.out.print("\u001B[" + row + ";" + col + "H");
    }

    // limpieza líneas
    public static void clearLine() {
        if (!enabled) return;
        System.out.print("\u001B[2K");
    }

    public static void clearToLineEnd() {
        if (!enabled) return;
        System.out.print("\u001B[K");
    }

    // colores y estilos
    public static void resetStyle() {
        if (!enabled) return;
        System.out.print("\u001B[0m");
    }

    public static void boldOn() {
        if (!enabled) return;
        System.out.print("\u001B[1m");
    }

    public static void boldOff() {
        if (!enabled) return;
        System.out.print("\u001B[22m");
    }

    public static void setFg(int colorCode) {
        if (!enabled) return;
        System.out.print("\u001B[" + colorCode + "m");
    }

    public static void setBg(int colorCode) {
        if (!enabled) return;
        System.out.print("\u001B[" + colorCode + "m");
    }

    public static void setWrap(boolean on) {
        if (!enabled) return;
        System.out.print(on ? "\u001B[?7h" : "\u001B[?7l");
    }

    // NUEVO: región de scroll (DECSTBM). top/bottom inclusivos (1-based).
    public static void setScrollRegion(int top, int bottom) {
        if (!enabled) return;
        System.out.print("\u001B[" + top + ";" + bottom + "r");
    }

    public static void resetScrollRegion() {
        if (!enabled) return;
        System.out.print("\u001B[r");
    }

    // NUEVO: flush explícito
    public static void flush() {
        System.out.flush();
    }
}
