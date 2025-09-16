package ui.menu;

import utils.ANSI;

import java.util.List;

public class InspectView {
    public void render(int top, int left, int width, int height, String title, char glyph, String kind, List<String> lines) {
        if (width < 18 || height < 5) return;

        final int inner = Math.max(0, width - 2);
        final int rows = Math.max(1, height - 2);

        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print(center(" " + safe(title) + " ", inner, '─'));
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

        ANSI.gotoRC(baseTop, baseLeft);
        String head = String.format("[%s] %s", glyph == 0 ? " " : String.valueOf(glyph), safe(kind));
        System.out.print(clip(head, inner));
        if (head.length() < inner) System.out.print(repeat(' ', inner - head.length()));

        int row = baseTop + 1;
        for (int i = 0; i < lines.size() && row <= top + rows - 1; i++, row++) {
            ANSI.gotoRC(row, baseLeft);
            String line = clip(lines.get(i), inner);
            System.out.print(line);
            if (line.length() < inner) System.out.print(repeat(' ', inner - line.length()));
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String repeat(char c, int n) {
        return n <= 0 ? "" : String.valueOf(c).repeat(n);
    }

    private static String clip(String s, int max) {
        if (s == null || max <= 0) return "";
        if (s.length() <= max) return s;
        return (max <= 3) ? ".".repeat(max) : s.substring(0, max - 3) + "...";
    }

    private static String center(String label, int width, char fill) {
        label = safe(label);
        if (label.length() >= width) return label.substring(0, Math.max(0, width));
        int left = (width - label.length()) / 2;
        int right = width - label.length() - left;
        return repeat(fill, left) + label + repeat(fill, right);
    }
}
