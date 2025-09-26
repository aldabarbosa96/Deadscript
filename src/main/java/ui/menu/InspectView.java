package ui.menu;

import utils.ANSI;
import java.util.ArrayList;
import java.util.List;
import static utils.UI.*;

public class InspectView {

    public void render(int top, int left, int width, int height,
                       String title, char glyph, String kind, List<String> lines) {
        if (width < 18 || height < 5) return;

        final int inner = Math.max(0, width - 2);
        final int rows  = Math.max(1, height - 2);

        // ── Cabecera ────────────────────────────────────────────────────────────
        ANSI.gotoRC(top, left);
        if (width >= 2) {
            System.out.print('┌');
            System.out.print(centerLabel(" " + safe(title) + " ", inner, '─'));
            System.out.print('┐');
        } else {
            System.out.print(repeat('─', width));
        }

        // Contenido: 1) cabecera interna "[X] Tipo", 2) líneas envueltas.
        final String head = String.format("[%s] %s", glyph == 0 ? " " : String.valueOf(glyph), safe(kind));
        final ArrayList<String> wrapped = new ArrayList<>(rows + 8);

        // Envuelve la cabecera y luego cada línea de contenido
        wrapped.addAll(wrapLine(head, inner));
        if (lines != null) {
            for (String l : lines) {
                wrapped.addAll(wrapLine(safe(l), inner));
            }
        }

        // ── Cuerpo con ajuste de línea (sin puntos suspensivos) ────────────────
        final int baseTop = top + 1;
        for (int i = 0; i < rows; i++) {
            ANSI.gotoRC(baseTop + i, left);
            if (width >= 2) System.out.print('│');

            String body = (i < wrapped.size()) ? wrapped.get(i) : "";
            if (body.length() < inner) body += repeat(' ', inner - body.length());
            else if (body.length() > inner) body = body.substring(0, inner); // por seguridad

            System.out.print(body);

            if (width >= 2) System.out.print('│');
        }

        // ── Pie ────────────────────────────────────────────────────────────────
        ANSI.gotoRC(top + 1 + rows, left);
        if (width >= 2) {
            System.out.print('└');
            System.out.print(repeat('─', inner));
            System.out.print('┘');
        } else {
            System.out.print(repeat('─', width));
        }
    }

    // Ajuste de línea por palabras (greedy). Si una “palabra” supera el ancho,
    // se parte en trozos de tamaño 'width' (corte duro) para no perder contenido.
    private static List<String> wrapLine(String s, int width) {
        ArrayList<String> out = new ArrayList<>();
        if (s == null) { out.add(""); return out; }
        if (width <= 0) { out.add(""); return out; }
        s = s.replace("\t", " "); // normaliza tabs

        String[] words = s.split(" ");
        StringBuilder cur = new StringBuilder();

        for (String w : words) {
            if (w.isEmpty()) continue;

            // Si la palabra excede el ancho por sí sola, cortamos en trozos
            if (w.length() > width) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
                int i = 0;
                while (i < w.length()) {
                    int end = Math.min(i + width, w.length());
                    out.add(w.substring(i, end));
                    i = end;
                }
                continue;
            }

            // Si cabe en la línea actual con un espacio (si procede), añadimos
            if (cur.length() == 0) {
                cur.append(w);
            } else if (cur.length() + 1 + w.length() <= width) {
                cur.append(' ').append(w);
            } else {
                // No cabe: volcamos línea y empezamos una nueva
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(w);
            }
        }

        if (cur.length() > 0) out.add(cur.toString());
        if (out.isEmpty()) out.add("");
        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
