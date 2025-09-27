package core.controller;

import game.GameState;
import render.Renderer;
import ui.input.InputHandler;
import utils.AudioManager;

public class PCController {

    public Effect handle(InputHandler.Command c, GameState s, Renderer r) {
        switch (c) {
            case INVENTORY, EQUIPMENT, STATS, OPTIONS -> {
                if (s.computerBootDone) {
                    // Ya estamos en la shell: ignora silenciosamente (evita el log en la 1ª tecla tras el boot)
                    return Effect.NONE;
                } else {
                    // Durante el boot sí bloquea y da feedback
                    r.log("Estás usando el ordenador: las letras se reservan para escribir. Menús bloqueados.");
                    try { AudioManager.playUi("/audio/ui_blocked.wav"); } catch (Throwable ignored) {}
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
                    // Durante el arranque: Z apaga
                    powerOff(s, r, true);
                    return Effect.CHANGED;
                } else {
                    // En la terminal: Z no apaga; hay que escribir "exit"
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

        String raw = s.computerLine.toString();
        s.computerLine.setLength(0);

        // Eco del comando ejecutado
        String prompt = s.computerCwd + ">";
        s.computerConsole.add(prompt + raw);

        String line = raw.trim();
        if (line.isEmpty()) return Effect.CHANGED;

        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase(java.util.Locale.ROOT);

        switch (cmd) {
            case "help" -> {
                s.computerConsole.add("Comandos disponibles:");
                s.computerConsole.add("  help    - Mostrar esta ayuda");
                s.computerConsole.add("  ls      - Listar archivos (WIP)");
                s.computerConsole.add("  cd      - Cambiar directorio (WIP)");
                s.computerConsole.add("  pwd     - Mostrar directorio actual");
                s.computerConsole.add("  echo    - Imprimir texto");
                s.computerConsole.add("  clear   - Limpiar pantalla");
                s.computerConsole.add("  rm      - Borrar archivo (WIP)");
                s.computerConsole.add("  mv      - Mover/renombrar (WIP)");
                s.computerConsole.add("  cp      - Copiar archivo (WIP)");
                s.computerConsole.add("  cat     - Mostrar archivo (WIP)");
                s.computerConsole.add("  exit    - Apagar ordenador");
            }
            case "pwd" -> s.computerConsole.add(s.computerCwd);
            case "echo" -> {
                String rest = (parts.length > 1) ? raw.substring(raw.indexOf(' ') + 1) : "";
                s.computerConsole.add(rest);
            }
            case "clear" -> s.computerConsole.clear();
            case "exit", "poweroff" -> {
                powerOff(s, r, true);
                return Effect.CHANGED;
            }
            default -> s.computerConsole.add("Comando no reconocido: " + cmd + " (escribe 'help')");
        }

        // Limitar historial (por si crece mucho)
        while (s.computerConsole.size() > 1000) s.computerConsole.remove(0);

        return Effect.CHANGED;
    }
}
