package core.controller;

import game.GameState;
import render.Renderer;
import ui.input.InputHandler;
import utils.AudioManager;

import java.util.*;
import java.util.stream.Collectors;

public class PCController {

    public Effect handle(InputHandler.Command c, GameState s, Renderer r) {
        switch (c) {
            case INVENTORY, EQUIPMENT, STATS, OPTIONS -> {
                if (s.computerBootDone) {
                    return Effect.NONE;
                } else {
                    r.log("Estás usando el ordenador: las letras se reservan para escribir. Menús bloqueados.");
                    try {
                        AudioManager.playUi("/audio/ui_blocked.wav");
                    } catch (Throwable ignored) {
                    }
                    return Effect.NONE;
                }
            }
            case UP, DOWN, LEFT, RIGHT, ACTION -> {
                return Effect.NONE;
            }
            case ENTER_KEY -> {
                return onEnter(s, r);
            }
            case BACKSPACE_KEY -> {
                return onBackspace(s, r);
            }
            case BACK, CANCEL -> {
                if (!s.computerBootDone) {
                    powerOff(s, r, true);
                    return Effect.CHANGED;
                } else {
                    r.log("Para apagar el ordenador, escribe: exit");
                    try {
                        AudioManager.playUi("/audio/ui_blocked.wav");
                    } catch (Throwable ignored) {
                    }
                    return Effect.NONE;
                }
            }
            case QUIT -> {
                return Effect.QUIT;
            }
            default -> {
                return Effect.NONE;
            }
        }
    }

    private static void powerOff(GameState s, Renderer r, boolean sound) {
        s.computerOpen = false;
        s.computerBootDone = false;
        s.computerBootJustEnded = false;

        r.log("Apagas el ordenador.");
        if (sound) {
            try {
                AudioManager.playUi("/audio/pcPowerDown.wav");
            } catch (Throwable ignored) {
            }
        }
    }

    public Effect onChar(int ch, GameState s, Renderer r) {
        if (!s.computerOpen || !s.computerBootDone) return Effect.NONE;
        if (ch >= 32 && ch < 127) {
            s.computerLine.append((char) ch);
            return Effect.CHANGED;
        }
        return Effect.NONE;
    }

    public Effect onBackspace(GameState s, Renderer r) {
        if (!s.computerOpen || !s.computerBootDone) return Effect.NONE;
        int len = s.computerLine.length();
        if (len > 0) {
            s.computerLine.setLength(len - 1);
            return Effect.CHANGED;
        }
        return Effect.NONE;
    }

    public Effect onEnter(GameState s, Renderer r) {
        if (!s.computerOpen || !s.computerBootDone) return Effect.NONE;

        ensureVfs(s);

        String raw = s.computerLine.toString();
        s.computerLine.setLength(0);

        String prompt = s.computerCwd + ">";
        s.computerConsole.add(prompt + raw);

        String line = raw.trim();
        if (line.isEmpty()) return Effect.CHANGED;

        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase(java.util.Locale.ROOT);

        switch (cmd) {
            case "help" -> {
                s.computerConsole.add("Comandos disponibles:");
                s.computerConsole.add("  help                - Mostrar esta ayuda");
                s.computerConsole.add("  ls [dir] [-l]       - Listar archivos");
                s.computerConsole.add("  cd <dir>            - Cambiar directorio");
                s.computerConsole.add("  pwd                 - Mostrar directorio actual");
                s.computerConsole.add("  echo <txt> [>|>> f] - Imprimir o redirigir a archivo");
                s.computerConsole.add("  clear               - Limpiar pantalla");
                s.computerConsole.add("  cat <file>          - Mostrar archivo");
                s.computerConsole.add("  rm [-r] <path>      - Borrar archivo o directorio");
                s.computerConsole.add("  mv <src> <dst>      - Mover/renombrar");
                s.computerConsole.add("  cp [-r] <src> <dst> - Copiar");
                s.computerConsole.add("  exit                - Apagar ordenador");
            }
            case "pwd" -> s.computerConsole.add(s.computerCwd);

            case "echo" -> {
                handleEcho(raw, s);
            }

            case "clear" -> s.computerConsole.clear();

            case "ls" -> {
                boolean longFmt = false;
                String pathArg = null;
                for (int i = 1; i < parts.length; i++) {
                    if ("-l".equals(parts[i])) longFmt = true;
                    else pathArg = parts[i];
                }
                String target = normalizePath(s, pathArg == null ? "." : pathArg);
                if (!exists(s, target)) {
                    s.computerConsole.add("ls: no existe: " + pathArg);
                } else if (isFile(s, target)) {
                    String name = baseName(target);
                    if (longFmt) {
                        int size = s.computerFiles.get(target).length();
                        s.computerConsole.add(String.format("%8d %s", size, name));
                    } else {
                        s.computerConsole.add(name);
                    }
                } else {
                    List<String> names = listChildren(s, target);
                    if (longFmt) {
                        for (String n : names) {
                            String abs = join(target, n);
                            if (isDir(s, abs)) s.computerConsole.add(String.format("[DIR]   %s", n));
                            else {
                                int size = s.computerFiles.getOrDefault(abs, "").length();
                                s.computerConsole.add(String.format("%8d %s", size, n));
                            }
                        }
                    } else {
                        String lineOut = String.join("  ", names);
                        s.computerConsole.add(lineOut);
                    }
                }
            }

            case "cd" -> {
                if (parts.length < 2) {
                    s.computerConsole.add("cd: falta argumento");
                } else {
                    String target = normalizePath(s, parts[1]);
                    if (!exists(s, target)) {
                        s.computerConsole.add("cd: no existe: " + parts[1]);
                    } else if (!isDir(s, target)) {
                        s.computerConsole.add("cd: no es un directorio: " + parts[1]);
                    } else {
                        s.computerCwd = target;
                    }
                }
            }

            case "cat" -> {
                if (parts.length < 2) {
                    s.computerConsole.add("cat: falta archivo");
                } else {
                    String p = normalizePath(s, parts[1]);
                    if (!isFile(s, p)) {
                        s.computerConsole.add("cat: no existe archivo: " + parts[1]);
                    } else {
                        for (String ln : s.computerFiles.get(p).split("\\R", -1)) {
                            s.computerConsole.add(ln);
                        }
                    }
                }
            }

            case "rm" -> {
                boolean recursive = false;
                List<String> args = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    if ("-r".equals(parts[i]) || "-R".equals(parts[i])) recursive = true;
                    else args.add(parts[i]);
                }
                if (args.isEmpty()) {
                    s.computerConsole.add("rm: falta ruta");
                } else {
                    String p = normalizePath(s, args.get(0));
                    if (!exists(s, p)) {
                        s.computerConsole.add("rm: no existe: " + args.get(0));
                    } else if (isFile(s, p)) {
                        s.computerFiles.remove(p);
                    } else {
                        if (!recursive) {
                            if (!listChildren(s, p).isEmpty()) {
                                s.computerConsole.add("rm: directorio no vacío: usa -r");
                            } else {
                                s.computerDirs.remove(p);
                            }
                        } else {
                            removeDirRecursive(s, p);
                        }
                    }
                }
            }

            case "mv" -> {
                if (parts.length < 3) {
                    s.computerConsole.add("mv: uso: mv <src> <dst>");
                } else {
                    String src = normalizePath(s, parts[1]);
                    String dst = normalizePath(s, parts[2]);
                    if (!exists(s, src)) {
                        s.computerConsole.add("mv: no existe: " + parts[1]);
                    } else if (isDir(s, dst)) {
                        String finalDst = join(dst, baseName(src));
                        movePath(s, src, finalDst);
                    } else {
                        movePath(s, src, dst);
                    }
                }
            }

            case "cp" -> {
                boolean recursive = false;
                List<String> args = new ArrayList<>();
                for (int i = 1; i < parts.length; i++) {
                    if ("-r".equals(parts[i]) || "-R".equals(parts[i])) recursive = true;
                    else args.add(parts[i]);
                }
                if (args.size() < 2) {
                    s.computerConsole.add("cp: uso: cp [-r] <src> <dst>");
                } else {
                    String src = normalizePath(s, args.get(0));
                    String dst = normalizePath(s, args.get(1));
                    if (!exists(s, src)) {
                        s.computerConsole.add("cp: no existe: " + args.get(0));
                    } else if (isFile(s, src)) {
                        if (isDir(s, dst)) dst = join(dst, baseName(src));
                        makeParentDirs(s, parentOf(dst));
                        s.computerFiles.put(dst, s.computerFiles.get(src));
                    } else {
                        if (!recursive) {
                            s.computerConsole.add("cp: directorio: usa -r");
                        } else {
                            copyDirRecursive(s, src, dst);
                        }
                    }
                }
            }

            case "exit", "poweroff" -> {
                powerOff(s, r, true);
                return Effect.CHANGED;
            }

            default -> s.computerConsole.add("Comando no reconocido: " + cmd + " (escribe 'help')");
        }

        while (s.computerConsole.size() > 1000) s.computerConsole.remove(0);
        return Effect.CHANGED;
    }

    private static void ensureVfs(GameState s) {
        if (!s.computerDirs.isEmpty() || !s.computerFiles.isEmpty()) return;
        s.computerDirs.add("/");
        s.computerDirs.add("/home");
        s.computerDirs.add("/home/guest");
        s.computerDirs.add("/bin");
        s.computerFiles.put("/home/guest/readme.txt", "Bienvenido.\nComandos útiles: help, ls, cd, echo, cat, rm, mv, cp, clear, exit.");
        s.computerFiles.put("/bin/notes.txt", "No hay binarios reales. Es una demo del VFS.");
        s.computerCwd = "/home/guest";
    }

    private static void handleEcho(String raw, GameState s) {
        String after = raw.substring(raw.toLowerCase(Locale.ROOT).indexOf("echo") + 4).trim();
        if (after.isEmpty()) {
            s.computerConsole.add("");
            return;
        }

        int idx = after.indexOf(">>");
        boolean append = false;
        if (idx < 0) {
            idx = after.indexOf('>');
        } else {
            append = true;
        }

        if (idx >= 0) {
            String text = after.substring(0, idx).trim();
            String target = after.substring(idx + (append ? 2 : 1)).trim();
            if (target.isEmpty()) {
                s.computerConsole.add("echo: falta archivo tras >");
                return;
            }
            String abs = normalizePath(s, target);
            if (isDir(s, abs)) {
                s.computerConsole.add("echo: destino es un directorio");
                return;
            }
            makeParentDirs(s, parentOf(abs));
            String old = s.computerFiles.get(abs);
            if (append && old != null) s.computerFiles.put(abs, old + (old.endsWith("\n") ? "" : "\n") + text);
            else s.computerFiles.put(abs, text);
        } else {
            s.computerConsole.add(after);
        }
    }

    private static boolean exists(GameState s, String abs) {
        return isDir(s, abs) || isFile(s, abs);
    }

    private static boolean isDir(GameState s, String abs) {
        return s.computerDirs.contains(abs);
    }

    private static boolean isFile(GameState s, String abs) {
        return s.computerFiles.containsKey(abs);
    }

    private static String normalizePath(GameState s, String p) {
        if (p == null || p.isEmpty()) p = ".";
        if (p.equals("~")) p = "/home/guest";
        if (p.startsWith("~" + "/")) p = "/home/guest" + p.substring(1);
        String base = p.startsWith("/") ? "" : s.computerCwd;
        Deque<String> stack = new ArrayDeque<>();
        if (!base.isEmpty()) for (String seg : base.split("/")) if (!seg.isEmpty()) stack.addLast(seg);
        for (String seg : p.split("/")) {
            if (seg.isEmpty() || seg.equals(".")) continue;
            if (seg.equals("..")) {
                if (!stack.isEmpty()) stack.removeLast();
            } else stack.addLast(seg);
        }
        String out = "/" + stack.stream().collect(Collectors.joining("/"));
        return out.isEmpty() ? "/" : out;
    }

    private static String parentOf(String abs) {
        if ("/".equals(abs)) return "/";
        int i = abs.lastIndexOf('/');
        if (i <= 0) return "/";
        return abs.substring(0, i);
    }

    private static String baseName(String abs) {
        if ("/".equals(abs)) return "/";
        int i = abs.lastIndexOf('/');
        return (i < 0) ? abs : abs.substring(i + 1);
    }

    private static String join(String dir, String name) {
        if ("/".equals(dir)) return "/" + name;
        return dir.endsWith("/") ? dir + name : dir + "/" + name;
    }

    private static List<String> listChildren(GameState s, String dirAbs) {
        final String prefix = "/".equals(dirAbs) ? "/" : dirAbs + "/";
        Set<String> names = new TreeSet<>();
        for (String d : s.computerDirs) {
            if (d.startsWith(prefix)) {
                String rest = d.substring(prefix.length());
                if (!rest.isEmpty() && !rest.contains("/")) names.add(rest + "/");
            }
        }
        for (String f : s.computerFiles.keySet()) {
            if (f.startsWith(prefix)) {
                String rest = f.substring(prefix.length());
                if (!rest.isEmpty() && !rest.contains("/")) names.add(rest);
            }
        }
        return new ArrayList<>(names);
    }

    private static void makeParentDirs(GameState s, String dirAbs) {
        if (dirAbs == null || dirAbs.isEmpty()) return;
        List<String> segs = new ArrayList<>();
        for (String token : dirAbs.split("/")) if (!token.isEmpty()) segs.add(token);
        String cur = "/";
        s.computerDirs.add("/");
        for (int i = 0; i < segs.size(); i++) {
            cur = "/".equals(cur) ? "/" + segs.get(i) : cur + "/" + segs.get(i);
            s.computerDirs.add(cur);
        }
    }

    private static void removeDirRecursive(GameState s, String dirAbs) {
        List<String> toRemoveDirs = new ArrayList<>();
        List<String> toRemoveFiles = new ArrayList<>();
        String prefix = "/".equals(dirAbs) ? "/" : dirAbs + "/";
        for (String d : s.computerDirs) {
            if (d.equals(dirAbs) || d.startsWith(prefix)) toRemoveDirs.add(d);
        }
        for (String f : s.computerFiles.keySet()) {
            if (f.startsWith(prefix) || f.equals(dirAbs)) toRemoveFiles.add(f);
        }
        for (String f : toRemoveFiles) s.computerFiles.remove(f);
        for (String d : toRemoveDirs) if (!"/".equals(d)) s.computerDirs.remove(d);
    }

    private static void movePath(GameState s, String srcAbs, String dstAbs) {
        if (isFile(s, srcAbs)) {
            makeParentDirs(s, parentOf(dstAbs));
            String data = s.computerFiles.remove(srcAbs);
            s.computerFiles.put(dstAbs, data);
        } else if (isDir(s, srcAbs)) {
            if (dstAbs.startsWith(srcAbs + "/") || dstAbs.equals(srcAbs)) {
                s.computerConsole.add("mv: destino dentro del origen");
                return;
            }
            makeParentDirs(s, parentOf(dstAbs));
            Map<String, String> newFiles = new HashMap<>();
            Set<String> newDirs = new HashSet<>(s.computerDirs);
            String srcPrefix = "/".equals(srcAbs) ? "/" : srcAbs + "/";
            String dstPrefix = "/".equals(dstAbs) ? "/" : dstAbs + "/";
            newDirs.add(dstAbs);
            List<String> dirsToRemove = new ArrayList<>();
            for (String d : s.computerDirs) {
                if (d.equals(srcAbs)) {
                    dirsToRemove.add(d);
                    continue;
                }
                if (d.startsWith(srcPrefix)) {
                    String tail = d.substring(srcPrefix.length());
                    newDirs.add(dstPrefix + tail);
                    dirsToRemove.add(d);
                }
            }
            for (String d : dirsToRemove) if (!"/".equals(d)) newDirs.remove(d);
            for (Map.Entry<String, String> e : s.computerFiles.entrySet()) {
                String p = e.getKey();
                if (p.equals(srcAbs)) {
                    newFiles.put(dstAbs, e.getValue());
                } else if (p.startsWith(srcPrefix)) {
                    String tail = p.substring(srcPrefix.length());
                    newFiles.put(dstPrefix + tail, e.getValue());
                } else {
                    newFiles.put(p, e.getValue());
                }
            }
            s.computerDirs.clear();
            s.computerDirs.addAll(newDirs);
            s.computerFiles.clear();
            s.computerFiles.putAll(newFiles);
        }
    }

    private static void copyDirRecursive(GameState s, String srcAbs, String dstAbs) {
        if (!isDir(s, srcAbs)) return;
        makeParentDirs(s, parentOf(dstAbs));
        s.computerDirs.add(dstAbs);
        String srcPrefix = "/".equals(srcAbs) ? "/" : srcAbs + "/";
        String dstPrefix = "/".equals(dstAbs) ? "/" : dstAbs + "/";
        for (String d : s.computerDirs.toArray(new String[0])) {
            if (d.startsWith(srcPrefix)) {
                String tail = d.substring(srcPrefix.length());
                s.computerDirs.add(dstPrefix + tail);
            }
        }
        Map<String, String> addFiles = new HashMap<>();
        for (Map.Entry<String, String> e : s.computerFiles.entrySet()) {
            String p = e.getKey();
            if (p.startsWith(srcPrefix)) {
                String tail = p.substring(srcPrefix.length());
                addFiles.put(dstPrefix + tail, e.getValue());
            }
        }
        s.computerFiles.putAll(addFiles);
    }
}
