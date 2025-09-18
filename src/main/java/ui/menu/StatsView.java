package ui.menu;

import game.GameState;
import utils.ANSI;

import java.util.ArrayList;
import java.util.List;

import static utils.UI.*;

public class StatsView {

    public void render(int top, int left, int width, int height, GameState s) {
        if (width < 40 || height < 12) return;

        final int innerW = Math.max(0, width - 2);
        final int innerH = Math.max(1, height - 2);

        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print(centerLabel(" ESTADÍSTICAS ", innerW, '─'));
            System.out.print('┐');
        } else System.out.print(repeat('─', width));

        for (int i = 0; i < innerH; i++) {
            ANSI.gotoRC(top + 1 + i, left);
            if (width >= 2) {
                System.out.print('│');
                System.out.print(repeat(' ', innerW));
                System.out.print('│');
            } else System.out.print(repeat(' ', width));
        }

        ANSI.gotoRC(top + height - 1, left);
        if (width >= 2) {
            System.out.print('└');
            System.out.print(repeat('─', innerW));
            System.out.print('┘');
        } else System.out.print(repeat('─', width));

        final int baseTop = top + 1, baseLeft = left + 1;

        // --- ancho de columnas: L más ancho, C y R compactas
        int colGap = 2;
        int availCols = Math.max(18, innerW - 2 * colGap);

        int minL = 34, minC = 22, minR = 26;
        int wL, wC, wR;
        if (availCols < (minL + minC + minR)) {
            int cw = Math.max(18, availCols / 3);
            wL = wC = wR = cw;
        } else {
            int extra = availCols - (minL + minC + minR);
            wL = minL + (int) Math.round(extra * 0.33);
            wC = minC + (int) Math.round(extra * 0.20);
            wR = availCols - wL - wC;
        }

        int colL = baseLeft;
        int colC = colL + wL + colGap;
        int colR = colC + wC + colGap;

        int contentTop = baseTop + 1;
        int contentRows = innerH - 2;

        // --- izquierda (habilidades)
        int padL = 3;
        int xL = colL + padL;
        int wLcol = Math.max(0, wL - padL);

        int barW = Math.min(12, Math.max(10, wLcol / 3));
        int pctW = 5;
        int gap = 1;
        int labelW = Math.max(18, Math.min(36, wLcol - (gap + barW + pctW)));

        record Row(String text, boolean header) {
        }
        List<Row> rowsSkills = new ArrayList<>();
        for (var g : GameState.SkillGroup.values()) {
            rowsSkills.add(new Row(fit("** " + nombreGrupo(g) + " **", wLcol), true));
            var list = s.skills.get(g);
            if (list != null) {
                for (var sk : list) {
                    rowsSkills.add(new Row(skillLine(sk.nombre, sk.nivel, sk.xp, labelW, barW, pctW, wLcol, gap), false));
                }
            }
            rowsSkills.add(new Row("", true));
        }
        int totalSkillEntries = 0;
        for (Row rw : rowsSkills) if (!rw.header()) totalSkillEntries++;
        int selSkill = Math.max(0, Math.min(s.statsSelSkill, Math.max(0, totalSkillEntries - 1)));

        int rL = contentTop, idx = 0;
        for (Row rw : rowsSkills) {
            if (rL >= contentTop + contentRows) break;
            boolean selected = (!rw.header()) && (s.statsCol == 0 && idx == selSkill);
            if (!rw.header()) idx++;
            putSel(xL, rL++, rw.text(), selected);
        }

        // --- centro (stickman)
        final int STICK_H = 15;
        int centered = contentTop + Math.max(0, (contentRows - STICK_H) / 2) - 2;
        int stickTop = Math.max(contentTop, centered);
        int cx = colC + wC / 2;
        drawStickman(stickTop, cx, s);

        int infoTop = stickTop + STICK_H + 2;
        int rC = infoTop;
        var parts = GameState.BodyPart.values();
        int selBody = Math.max(0, Math.min(s.statsBodySel, parts.length - 1));
        boolean bodyActive = (s.statsCol == 1);
        putCenteredSel(colC, wC, rC++, "* " + nombreParte(parts[selBody]) + " *", bodyActive);
        var ls = s.injuries.get(parts[selBody]);
        if (ls == null || ls.isEmpty()) {
            putCentered(colC, wC, rC++, "Sin lesiones.");
        } else {
            for (var inj : ls) {
                if (rC >= contentTop + contentRows) break;
                putCentered(colC, wC, rC++, String.format("- %s (%d%%)", inj.nombre, inj.severidad));
            }
        }

        // --- derecha (stats)
        int padR = 1;
        int rightIndent = Math.max(2, wR / 10);
        int xR = colR + padR + rightIndent;
        int wRcol = Math.max(0, wR - padR - rightIndent);

        record Entry(String text, boolean selectable, int afterBlank) {
        }
        List<Entry> stats = new ArrayList<>();
        stats.add(new Entry("** Principales **", false, 1));
        stats.add(new Entry(String.format("Salud:       %3d%%", pct(s.salud, s.maxSalud)), true, 0));
        stats.add(new Entry(String.format("Energía:     %3d%%", pct(s.energia, s.maxEnergia)), true, 0));
        stats.add(new Entry(String.format("Hambre:      %3d%%", pct(s.hambre, s.maxHambre)), true, 0));
        stats.add(new Entry(String.format("Sed:         %3d%%", pct(s.sed, s.maxSed)), true, 0));
        stats.add(new Entry(String.format("Sueño:       %3d%%", pct(s.sueno, s.maxSueno)), true, 2));
        stats.add(new Entry("** Secundarias **", false, 1));
        stats.add(new Entry(String.format("Frío:          %3d%%", clampPct(s.frio)), true, 0));
        stats.add(new Entry(String.format("Temp. corporal: %d°C", s.tempCorporalC), true, 0));
        stats.add(new Entry(String.format("Miedo:         %3d%%", clampPct(s.miedo)), true, 0));
        stats.add(new Entry(String.format("Aburrim.:      %3d%%", clampPct(s.aburrimiento)), true, 0));
        stats.add(new Entry(String.format("Malestar:      %3d%%", clampPct(s.malestar)), true, 0));
        stats.add(new Entry(String.format("Dolor:         %3d%%", clampPct(s.dolor)), true, 0));
        stats.add(new Entry(String.format("Infección:     %3d%%", clampPct(s.infeccionPct)), true, 0));
        stats.add(new Entry(String.format("Radiación:     %3d%%", clampPct(s.radiacionPct)), true, 0));

        int rR = contentTop;
        int selIdx = Math.max(0, s.statsSelBasic);
        int seen = 0;
        for (Entry e : stats) {
            if (rR >= contentTop + contentRows) break;
            boolean selected = e.selectable() && (s.statsCol == 2 && seen == selIdx);
            if (e.selectable()) seen++;
            putSel(xR, rR, fit(e.text(), wRcol), selected);
            rR += 1 + Math.max(0, e.afterBlank());
        }

        String help = " [S] Cerrar   [Flechas] Cambiar bloque/Mover selección   [I] Inventario   [E] Equipo ";
        ANSI.gotoRC(top + height - 2, baseLeft);
        System.out.print(fit(centerLabel(help, innerW, ' '), innerW));
    }

    private static int pct(int v, int max) {
        if (max <= 0) return 0;
        int p = (int) Math.round((v * 100.0) / max);
        return Math.max(0, Math.min(100, p));
    }

    private static int clampPct(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String skillLine(String nombre, int nivel, double xp, int labelW, int barW, int pctW, int totalW, int gap) {
        String label = padRight(nombre + " Lv " + nivel, labelW);
        String bar = barraW(xp, barW);
        String pct = String.format(" %3d%%", (int) Math.round(xp * 100));
        String out = label + (gap > 0 ? " " : "") + bar + pct;
        return fit(out, totalW);
    }

    private static String padRight(String s, int width) {
        if (width <= 0) return "";
        if (s.length() >= width) return s.substring(0, width);
        return s + repeat(' ', width - s.length());
    }

    private static String fit(String s, int w) {
        if (w <= 0 || s == null) return "";
        return (s.length() <= w) ? s : s.substring(0, w);
    }

    private static void put(int x, int y, String txt) {
        if (txt == null) return;
        ANSI.gotoRC(y, x);
        System.out.print(txt);
    }

    private static void putSel(int x, int y, String txt, boolean selected) {
        ANSI.gotoRC(y, x);
        if (selected) {
            ANSI.setFg(92);
            ANSI.boldOn();
            System.out.print(txt);
            ANSI.resetStyle();
        } else System.out.print(txt);
    }

    private static void putCentered(int colLeft, int colW, int y, String txt) {
        String t = fit(txt, colW);
        int x = colLeft + Math.max(0, (colW - t.length()) / 2);
        put(x, y, t);
    }

    private static void putCenteredSel(int colLeft, int colW, int y, String txt, boolean sel) {
        String t = fit(txt, colW);
        int x = colLeft + Math.max(0, (colW - t.length()) / 2);
        putSel(x, y, t, sel);
    }

    private static String barraW(double t, int n) {
        t = Math.max(0, Math.min(1, t));
        int filled = (int) Math.round(t * n);
        return "[" + repeat('■', filled) + repeat('·', Math.max(0, n - filled)) + "]";
    }

    private static String nombreGrupo(GameState.SkillGroup g) {
        return switch (g) {
            case FISICO -> "Físico";
            case COMBATE -> "Combate";
            case CRAFTEO -> "Crafteo";
            case SUPERVIVENCIA -> "Supervivencia";
        };
    }

    private static String nombreParte(GameState.BodyPart p) {
        return switch (p) {
            case CABEZA -> "Cabeza";
            case TORSO -> "Torso";
            case BRAZO_IZQ -> "Brazo izq.";
            case BRAZO_DER -> "Brazo der.";
            case MANOS -> "Manos";
            case PIERNA_IZQ -> "Pierna izq.";
            case PIERNA_DER -> "Pierna der.";
            case PIES -> "Pies";
        };
    }

    private void drawStickman(int top, int cx, GameState s) {
        put(cx - 3, top + 0, "  ___  ");
        put(cx - 3, top + 1, " /   \\");
        put(cx - 3, top + 2, "| 0 0 |");
        put(cx - 3, top + 3, " \\___/");
        put(cx, top + 4, "|");
        put(cx, top + 5, "|");
        put(cx - 6, top + 6, "/-----|-----\\");
        put(cx, top + 7, "|");
        put(cx, top + 8, "|");
        put(cx, top + 9, "|");
        put(cx - 1, top + 10, "/ \\");
        put(cx - 2, top + 11, "/   \\");
        put(cx - 3, top + 12, "/     \\");
        put(cx - 4, top + 13, "/       \\");
        put(cx - 5, top + 14, "/         \\");

        int sel = Math.floorMod(s.statsBodySel, GameState.BodyPart.values().length);
        switch (GameState.BodyPart.values()[sel]) {
            case CABEZA -> put(cx - 6, top + 0, "«");
            case TORSO -> put(cx + 2, top + 7, "»");
            case BRAZO_IZQ -> put(cx - 8, top + 6, "«");
            case BRAZO_DER -> put(cx + 8, top + 6, "»");
            case MANOS -> {
                put(cx - 8, top + 6, "«");
                put(cx + 8, top + 6, "»");
            }
            case PIERNA_IZQ -> put(cx - 3, top + 12, "«");
            case PIERNA_DER -> put(cx + 4, top + 12, "»");
            case PIES -> {
                put(cx - 6, top + 14, "«");
                put(cx + 6, top + 14, "»");
            }
        }
    }
}
