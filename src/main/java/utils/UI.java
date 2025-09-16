package utils;

public final class UI {
    private UI() {
    }

    public static String repeat(char c, int n) {
        return (n <= 0) ? "" : String.valueOf(c).repeat(n);
    }

    public static String clip(String s, int max) {
        if (s == null || max <= 0) return "";
        if (s.length() <= max) return s;
        return (max <= 3) ? ".".repeat(max) : s.substring(0, max - 3) + "...";
    }

    public static String clipAscii(String s, int max) {
        return clip(s, max);
    }

    public static String centerLabel(String label, int width, char fill) {
        label = (label == null) ? "" : label;
        if (label.length() >= width) return label.substring(0, Math.max(0, width));
        int left = (width - label.length()) / 2;
        int right = width - label.length() - left;
        return repeat(fill, left) + label + repeat(fill, right);
    }

    public static String center(String s, int w) {
        s = (s == null) ? "" : s;
        if (w <= 0) return "";
        if (s.length() >= w) return s.substring(0, w);
        int l = (w - s.length()) / 2;
        int r = w - s.length() - l;
        return repeat(' ', l) + s + repeat(' ', r);
    }

    public static String pad(String s, int w) {
        s = (s == null) ? "" : s;
        if (w <= 0) return "";
        if (s.length() >= w) return s.substring(0, w);
        return s + repeat(' ', w - s.length());
    }

    public static String formatKg(double kg) {
        return String.format("%.2f kg", kg);
    }
}
