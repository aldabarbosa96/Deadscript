package ui.log;

import utils.ANSI;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MessageLog {
    private int top;
    private int left;
    private int width;
    private int height; // altura total del bloque (incluye título + línea en blanco)

    private final Deque<String> lines = new ArrayDeque<>();

    public MessageLog(int top, int left, int width, int height) {
        this.top = Math.max(1, top);
        this.left = Math.max(1, left);
        this.width = Math.max(10, width);
        this.height = Math.max(3, height);
    }

    public void updateGeometry(int top, int left, int width, int height) {
        this.top = Math.max(1, top);
        this.left = Math.max(1, left);
        this.width = Math.max(10, width);
        this.height = Math.max(3, height);
    }

    public void add(String msg) {
        if (msg == null) return;
        for (String l : wrap(msg, width)) {
            lines.addLast(l);
            // Evita crecer sin límite (buffer razonable)
            if (lines.size() > 1000) lines.removeFirst();
        }
    }

    public void render() {
        // Título centrado
        ANSI.gotoRC(top, left);
        String label = " LOG ";
        if (label.length() >= width) {
            System.out.print(label.substring(0, Math.max(0, width)));
        } else {
            int leftDash = (width - label.length()) / 2;
            int rightDash = width - label.length() - leftDash;
            System.out.print("─".repeat(leftDash));
            System.out.print(label);
            System.out.print("─".repeat(rightDash));
        }

        // Una línea en blanco bajo el título
        ANSI.gotoRC(top + 1, left);
        ANSI.clearToLineEnd();

        // Zona de contenido
        int contentRows = height - 2;
        int startRow = top + 2;

        // Selecciona las últimas 'contentRows' líneas del buffer
        List<String> view = lastN(lines, contentRows);

        // Pinta desde la línea superior del área
        int row = startRow;
        for (String s : view) {
            ANSI.gotoRC(row++, left);
            String out = s.length() > width ? s.substring(0, width) : s;
            System.out.print(out);
            if (out.length() < width) {
                System.out.print(" ".repeat(width - out.length()));
            }
        }

        // Limpia posibles filas sobrantes si el buffer es corto
        for (; row < startRow + contentRows; row++) {
            ANSI.gotoRC(row, left);
            ANSI.clearToLineEnd();
        }
    }

    private static List<String> lastN(Deque<String> dq, int n) {
        ArrayList<String> all = new ArrayList<>(dq);
        int from = Math.max(0, all.size() - n);
        return all.subList(from, all.size());
    }

    // Envoltura simple con bullet en la primera línea (“» ”) y sangría en continuaciones (“  ”).
    private static List<String> wrap(String msg, int width) {
        List<String> out = new ArrayList<>();
        if (width <= 2) {
            out.add(msg);
            return out;
        }

        String[] words = msg.trim().split("\\s+");
        String firstPrefix = "» ";
        String contPrefix = "  ";
        int firstAvail = Math.max(0, width - firstPrefix.length());
        int contAvail = Math.max(0, width - contPrefix.length());

        StringBuilder line = new StringBuilder();
        boolean firstLine = true;
        int avail = firstAvail;

        for (String w : words) {
            if (w.length() > avail && !line.isEmpty()) {
                // cierra línea actual
                out.add((firstLine ? firstPrefix : contPrefix) + line);
                firstLine = false;
                line.setLength(0);
                avail = contAvail;
            }
            if (!line.isEmpty()) {
                if (line.length() + 1 + w.length() > avail) {
                    out.add((firstLine ? firstPrefix : contPrefix) + line);
                    firstLine = false;
                    line.setLength(0);
                    avail = contAvail;
                } else {
                    line.append(' ');
                }
            }
            if (w.length() > avail) {
                // palabra más larga que la línea -> trocea
                int idx = 0;
                while (idx < w.length()) {
                    int take = Math.min(avail, w.length() - idx);
                    String chunk = w.substring(idx, idx + take);
                    if (line.isEmpty()) line.append(chunk);
                    else line.append(' ').append(chunk);
                    out.add((firstLine ? firstPrefix : contPrefix) + line);
                    firstLine = false;
                    line.setLength(0);
                    idx += take;
                    avail = contAvail;
                }
            } else {
                line.append(w);
            }
        }
        if (!line.isEmpty()) {
            out.add((firstLine ? firstPrefix : contPrefix) + line);
        }
        if (out.isEmpty()) out.add(firstPrefix);
        return out;
    }
}
