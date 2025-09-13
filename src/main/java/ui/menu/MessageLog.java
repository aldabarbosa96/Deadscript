package ui.menu;

import utils.ANSI;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MessageLog {
    private int top;
    private int left;
    private int width;
    private int height;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
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
        String time = LocalTime.now().format(TS_FMT);
        String firstPrefix = "[" + time + "] » ";
        String contPrefix = " ".repeat(firstPrefix.length());

        for (String l : wrapWithPrefixes(msg, width, firstPrefix, contPrefix)) {
            lines.addLast(l);
            if (lines.size() > 1000) lines.removeFirst();
        }
    }

    public void render() {
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

        ANSI.gotoRC(top + 1, left);
        ANSI.clearToLineEnd();

        int contentRows = height - 2;
        int startRow = top + 2;

        List<String> view = lastN(lines, contentRows);

        int row = startRow;
        for (String s : view) {
            ANSI.gotoRC(row++, left);
            String out = s.length() > width ? s.substring(0, width) : s;
            System.out.print(out);
            if (out.length() < width) System.out.print(" ".repeat(width - out.length()));
        }

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

    private static List<String> wrapWithPrefixes(String msg, int width, String firstPrefix, String contPrefix) {
        List<String> out = new ArrayList<>();
        if (msg == null) return out;

        int firstAvail = Math.max(0, width - firstPrefix.length());
        int contAvail = Math.max(0, width - contPrefix.length());

        if (firstAvail <= 0) {
            out.add(firstPrefix.substring(0, Math.min(width, firstPrefix.length())));
            return out;
        }

        String[] words = msg.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        boolean firstLine = true;
        int avail = firstAvail;

        for (String w : words) {
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
                int idx = 0;
                while (idx < w.length()) {
                    int take = Math.min(avail, w.length() - idx);
                    if (line.length() == 0) {
                        line.append(w, idx, idx + take);
                    } else if (line.length() + 1 + take <= avail) {
                        line.append(' ').append(w, idx, idx + take);
                    } else {
                        out.add((firstLine ? firstPrefix : contPrefix) + line);
                        firstLine = false;
                        line.setLength(0);
                        avail = contAvail;
                        continue;
                    }
                    if (take == avail) {
                        out.add((firstLine ? firstPrefix : contPrefix) + line);
                        firstLine = false;
                        line.setLength(0);
                        avail = contAvail;
                    }
                    idx += take;
                }
            } else {
                line.append(w);
            }
        }
        if (!line.isEmpty()) out.add((firstLine ? firstPrefix : contPrefix) + line);
        return out;
    }
}
