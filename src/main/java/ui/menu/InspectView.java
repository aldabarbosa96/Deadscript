package ui.menu;

import utils.ANSI;
import java.util.List;
import static utils.UI.*;

public class InspectView {
    public void render(int top, int left, int width, int height, String title, char glyph, String kind, List<String> lines) {
        if (width < 18 || height < 5) return;

        final int inner = Math.max(0, width - 2);
        final int rows = Math.max(1, height - 2);

        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print(centerLabel(" " + safe(title) + " ", inner, '─'));
            System.out.print('┐');
        } else {
            System.out.print(repeat('─', width));
        }

        final int baseTop = top + 1;
        final String head = String.format("[%s] %s", glyph == 0 ? " " : String.valueOf(glyph), safe(kind));

        for (int i = 0; i < rows; i++) {
            ANSI.gotoRC(baseTop + i, left);
            if (width >= 2) System.out.print('│');

            String body;
            if (i == 0) body = clip(head, inner);
            else {
                int li = i - 1;
                body = (li < lines.size()) ? clip(lines.get(li), inner) : "";
            }
            if (body.length() < inner) body += repeat(' ', inner - body.length());
            System.out.print(body);

            if (width >= 2) System.out.print('│');
        }

        ANSI.gotoRC(top + 1 + rows, left);
        if (width >= 2) {
            System.out.print('└');
            System.out.print(repeat('─', inner));
            System.out.print('┘');
        } else {
            System.out.print(repeat('─', width));
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
