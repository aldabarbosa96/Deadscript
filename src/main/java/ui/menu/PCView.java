package ui.menu;

import game.GameState;
import utils.ANSI;

import static utils.UI.*;

public final class PCView {
    private static final long BOOT_NS = 4_500_000_000L;

    public void render(int top, int left, int width, int height, GameState s) {
        if (width < 30 || height < 10) return;

        final int innerW = Math.max(0, width - 2);
        final int innerH = Math.max(1, height - 2);

        ANSI.gotoRC(top, left);
        System.out.print('┌');
        System.out.print(centerLabel(" ORDENADOR ", innerW, '─'));
        System.out.print('┐');

        for (int i = 0; i < innerH; i++) {
            ANSI.gotoRC(top + 1 + i, left);
            System.out.print('│');
            System.out.print(repeat(' ', innerW));
            System.out.print('│');
        }

        ANSI.gotoRC(top + height - 1, left);
        System.out.print('└');
        System.out.print(repeat('─', innerW));
        System.out.print('┘');

        final int baseTop = top + 2;
        final int baseLeft = left + 2;
        final int usableW = Math.max(0, innerW - 2);
        final int usableH = Math.max(0, innerH - 2);

        if (!s.computerBootDone) {
            renderBoot(baseTop, baseLeft, usableW, usableH, s);
        } else {
            renderShell(baseTop, baseLeft, usableW, usableH, s);
        }

        String help = !s.computerBootDone ? "Apagar ordenador [Z]" : "Para apagar escribe \"exit\"   Para ayuda escribe \"help\"";
        renderPcHelpLine(top, left, innerW, height, help);
    }

    private void renderBoot(int y, int x, int w, int h, GameState s) {
        long elapsedNs = Math.max(0, System.nanoTime() - s.computerBootStartNs);
        double sec = elapsedNs / 1_000_000_000.0;

        char[] spin = new char[]{'|', '/', '-', '\\'};
        char sp = spin[(int) ((elapsedNs / 80_000_000L) % spin.length)];

        int row = y;

        if (sec >= 0.0) put(x, row++, fit("Award Modular BIOS v4.51PG, An Energy Star Ally", w));
        if (sec >= 0.1) put(x, row++, fit("Copyright (C) 1984-1999 Award Software, Inc.", w));
        if (sec >= 0.2) put(x, row++, fit("BIOS Revision: 4.51PG  ID: 2A69KM4C", w));
        if (sec >= 0.3) row++; // línea en blanco

        if (sec >= 0.6) {
            int baseK = 640;
            int extK = 65536;
            int baseNow = (int) lerp(0, baseK, clamp01((sec - 0.6) / 0.6));
            int extNow = (int) lerp(0, extK, clamp01((sec - 1.0) / 0.9));

            put(x, row++, fit(String.format("Memory Testing : %5dK OK", baseNow), w));
            if (sec >= 1.0) {
                put(x, row++, fit(String.format("Extended Memory Testing : %6dK OK", extNow), w));
            }
            if (sec >= 1.6) row++;
        }

        if (sec >= 2.2) {
            put(x, row++, fit("Detecting Primary Master ... " + sp, w));
            if (sec >= 2.5) put(x, row++, fit(" Primary Master : WDC AC36400L  6.4GB", w));
            if (sec >= 2.8) put(x, row++, fit(" Primary Slave  : None", w));
            if (sec >= 3.0) put(x, row++, fit(" Secondary Master: None", w));
            if (sec >= 3.1) put(x, row++, fit(" Secondary Slave : None", w));
            row++;
        }

        if (sec >= 3.2) {
            put(x, row++, fit("Updating ESCD ... Success", w));
            put(x, row++, fit("Verifying DMI Pool Data ........", w));
            row++;
        }

        if (sec >= 4.0) {
            put(x, row++, fit("Boot from C:", w));
            put(x, row++, fit("Starting MS-DOS...", w));
        }
    }

    private void renderShell(int y, int x, int w, int h, GameState s) {
        int row = y;
        put(x, row++, fit("Microsoft(R) MS-DOS(R) Version 6.22", w));
        row++; // línea en blanco

        // Zona de consola (historial + 1 línea final para el prompt vivo)
        int consoleTop = row;
        int consoleH   = Math.max(1, h - (row - y));

        java.util.List<String> lines = s.computerConsole;

        // Dejamos 1 línea para el prompt vivo
        int histCap = Math.max(0, consoleH - 1);
        int start   = Math.max(0, lines.size() - histCap);

        // Pintar historial que cabe
        int painted = 0;
        for (int i = 0; i < histCap && (start + i) < lines.size(); i++) {
            put(x, consoleTop + i, fit(lines.get(start + i), w));
            painted++;
        }

        // Prompt vivo inmediatamente después del historial visible
        int promptY = consoleTop + painted;
        if (promptY >= consoleTop + consoleH) {
            promptY = consoleTop + consoleH - 1;
        }

        String prompt = s.computerCwd + ">";
        String entry  = s.computerLine.toString();
        boolean blink = ((System.nanoTime() / 400_000_000L) % 2 == 0);
        String line   = prompt + entry + (blink ? "_" : " ");
        put(x, promptY, fit(line, w));
    }

    private static void renderPcHelpLine(int top, int left, int innerW, int height, String help) {
        int y = top + height - 2;
        int x = left;

        ANSI.gotoRC(y, x);
        System.out.print('│');
        String body = clipAscii(centerLabel(help, innerW, ' '), innerW);
        System.out.print(body);
        if (body.length() < innerW) System.out.print(repeat(' ', innerW - body.length()));
        System.out.print('│');
    }

    private static void put(int x, int y, String txt) {
        ANSI.gotoRC(y, x);
        System.out.print(txt);
    }

    private static String fit(String s, int w) {
        if (s == null) return "";
        return (w <= 0 || s.length() <= w) ? s : s.substring(0, Math.max(0, w));
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp01(t);
    }
}
