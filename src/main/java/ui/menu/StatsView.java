package ui.menu;

import game.GameState;
import utils.ANSI;

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

        int colGap = 2;
        int minL = 36, minC = 18, minR = 24;
        int wL = Math.max(minL, (int) Math.round(innerW * 0.46));
        int wR = Math.max(minR, (int) Math.round(innerW * 0.26));
        int wC = innerW - wL - wR - 2 * colGap;
        if (wC < minC) {
            int need = minC - wC;
            int reduceL = Math.min(need, Math.max(0, wL - minL));
            wL -= reduceL;
            need -= reduceL;
            int reduceR = Math.min(need, Math.max(0, wR - minR));
            wR -= reduceR;
            need -= reduceR;
            wC = innerW - wL - wR - 2 * colGap;
            if (wC < minC) wC = minC;
        }

        int colL = baseLeft;
        int colC = baseLeft + wL + colGap;
        int colR = baseLeft + wL + colGap + wC + colGap;

        int contentTop = baseTop + 1;
        int contentRows = innerH - 2;

        int marginLeft = 3;
        int xL = colL + marginLeft;
        int wLeftInner = Math.max(0, wL - marginLeft - 1);

        int barW = Math.min(14, Math.max(10, wLeftInner / 3));
        int lvlW = 6;
        int gap = 0;

        int maxName = 0;
        for (var g : GameState.SkillGroup.values()) {
            var list = s.skills.get(g);
            if (list != null) for (var sk : list) maxName = Math.max(maxName, sk.nombre.length());
        }
        int maxForLabel = Math.max(10, wLeftInner - (gap + barW + lvlW));
        int labelW = Math.max(10, Math.min(maxForLabel, maxName + 1));

        record Row(String text, boolean header) {
        }
        java.util.List<Row> rowsSkills = new java.util.ArrayList<>();
        for (var g : GameState.SkillGroup.values()) {
            rowsSkills.add(new Row(fit("** " + nombreGrupo(g) + " **", wLeftInner), true));
            var list = s.skills.get(g);
            if (list != null) {
                for (var sk : list) {
                    rowsSkills.add(new Row(skillLine(sk.nombre, sk.nivel, sk.xp, labelW, barW, lvlW, wLeftInner, gap), false));
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
            boolean selected = false;
            if (!rw.header()) {
                selected = (s.statsCol == 0 && idx == selSkill);
                idx++;
            }
            putSel(xL, rL++, rw.text(), selected);
        }

        final int STICK_H = 15;
        int cx = baseLeft + innerW / 2;
        int centered = contentTop + Math.max(0, (contentRows - STICK_H) / 2) - 2;
        int stickTop = Math.max(contentTop, centered);
        drawStickman(stickTop, cx, s);

        var parts = GameState.BodyPart.values();
        int selBody = Math.max(0, Math.min(s.statsBodySel, parts.length - 1));
        boolean bodyActive = (s.statsCol == 1);
        String bodyTitle = "* " + nombreParte(parts[selBody]) + " *";
        String bodyText = fit(bodyTitle, innerW);
        int labelY = stickTop + STICK_H + 2;
        int labelX = Math.max(baseLeft, Math.min(cx - bodyText.length() / 2, baseLeft + innerW - bodyText.length()));
        var ls = s.injuries.get(parts[selBody]);
        boolean injuredSel = ls != null && !ls.isEmpty();
        if (injuredSel) putColored(labelX, labelY, bodyText, 91, true);
        else if (bodyActive) putSel(labelX, labelY, bodyText, true);
        else put(labelX, labelY, bodyText);

        int rC = labelY + 1;
        if (ls == null || ls.isEmpty()) {
            if (rC < contentTop + contentRows) {
                String t = fit("Sin lesiones.", innerW);
                int x = Math.max(baseLeft, Math.min(cx - t.length() / 2, baseLeft + innerW - t.length()));
                put(x, rC++, t);
            }
        } else {
            for (var inj : ls) {
                if (rC >= contentTop + contentRows) break;
                String t = fit(String.format("- %s (%d%%)", inj.nombre, inj.severidad), innerW);
                int x = Math.max(baseLeft, Math.min(cx - t.length() / 2, baseLeft + innerW - t.length()));
                put(x, rC++, t);
            }
        }

        record Entry(String text, boolean selectable, int afterBlank) {
        }
        java.util.List<Entry> stats = new java.util.ArrayList<>();
        stats.add(new Entry("** Principales **", false, 1));
        stats.add(new Entry(String.format("Salud:       %3d%%", pct(s.salud, s.maxSalud)), true, 0));
        stats.add(new Entry(String.format("Energía:     %3d%%", pct(s.energia, s.maxEnergia)), true, 0));
        stats.add(new Entry(String.format("Hambre:      %3d%%", pct(s.hambre, s.maxHambre)), true, 0));
        stats.add(new Entry(String.format("Sed:         %3d%%", pct(s.sed, s.maxSed)), true, 0));
        stats.add(new Entry(String.format("Sueño:       %3d%%", pct(s.sueno, s.maxSueno)), true, 2));
        stats.add(new Entry("** Secundarias **", false, 1));
        stats.add(new Entry(String.format("Temp. corporal: %d°C", s.tempCorporalC), true, 0));
        stats.add(new Entry(String.format("Frío:          %3d%%", clampPct(s.frio)), true, 0));
        stats.add(new Entry(String.format("Miedo:         %3d%%", clampPct(s.miedo)), true, 0));
        stats.add(new Entry(String.format("Aburrim.:      %3d%%", clampPct(s.aburrimiento)), true, 0));
        stats.add(new Entry(String.format("Malestar:      %3d%%", clampPct(s.malestar)), true, 0));
        stats.add(new Entry(String.format("Dolor:         %3d%%", clampPct(s.dolor)), true, 0));
        stats.add(new Entry(String.format("Infección:     %3d%%", clampPct(s.infeccionPct)), true, 0));
        stats.add(new Entry(String.format("Radiación:     %3d%%", clampPct(s.radiacionPct)), true, 0));

        int padR = 1;
        int rightIndent = Math.max(2, wR / 8);
        int xR = colR + padR + rightIndent;
        int rR = contentTop;
        int selIdx = Math.max(0, s.statsSelBasic);
        int seen = 0;
        for (Entry e : stats) {
            if (rR >= contentTop + contentRows) break;
            boolean selected = false;
            if (e.selectable()) {
                selected = (s.statsCol == 2 && seen == selIdx);
                seen++;
            }
            putSel(xR, rR, fit(e.text(), Math.max(0, wR - rightIndent - padR)), selected);
            rR += 1 + Math.max(0, e.afterBlank());
        }

        int sepRows = 5;
        int infoY = rR + sepRows;
        int maxH = contentTop + contentRows - infoY;
        if (wR >= 10 && maxH >= 3) {
            int infoW = Math.max(22, Math.min(36, wR));
            int infoX = colR + Math.max(0, (wR - infoW) / 2);
            int boxH = Math.min(maxH, 10);
            int wrapPre = infoW - 2;

            java.util.List<String> info = new java.util.ArrayList<>();
            if (s.statsCol == 0) {
                int count = 0;
                Object selObj = null;
                for (var g : GameState.SkillGroup.values()) {
                    var list = s.skills.get(g);
                    if (list != null) for (var sk : list) {
                        if (count == selSkill) {
                            selObj = sk;
                            break;
                        }
                        count++;
                    }
                    if (selObj != null) break;
                }
                if (selObj != null) {
                    var sk = (game.GameState.Skill) selObj;
                    info.add("Habilidad: " + sk.nombre);
                    info.add("Nivel: " + sk.nivel);
                    info.add(String.format("Progreso: %d%%", (int) Math.round(sk.xp * 100)));
                    info.addAll(wrap("Desc: " + descHabilidad(sk.nombre), wrapPre));
                }
            } else if (s.statsCol == 1) {
                var p = parts[selBody];
                info.add("Parte: " + nombreParte(p));
                var injL = s.injuries.get(p);
                if (injL == null || injL.isEmpty()) {
                    info.add("Sin lesiones");
                } else {
                    int n = 0;
                    for (var inj : injL) {
                        info.add(String.format("%s (%d%%)", inj.nombre, inj.severidad));
                        if (++n >= 3) break;
                    }
                    var prim = injL.get(0);
                    info.addAll(wrap("Desc: " + descLesion(prim.nombre, prim.severidad), wrapPre));
                }
            } else {
                int k = 0;
                String txt = null;
                for (Entry e : stats)
                    if (e.selectable()) {
                        if (k == selIdx) {
                            txt = e.text();
                            break;
                        }
                        k++;
                    }
                if (txt != null) {
                    info.add("Atributo:");
                    info.add(txt.trim());
                    info.addAll(wrap("Desc: " + descStatFromText(txt), wrapPre));
                }
            }

            ANSI.gotoRC(infoY, infoX);
            System.out.print('┌');
            System.out.print(centerLabel(" INFO ", infoW - 2, '─'));
            System.out.print('┐');
            for (int i = 0; i < boxH - 2; i++) {
                ANSI.gotoRC(infoY + 1 + i, infoX);
                System.out.print('│');
                String line = (i < info.size()) ? fit(info.get(i), infoW - 2) : "";
                int pad = Math.max(0, infoW - 2 - line.length());
                System.out.print(line + repeat(' ', pad));
                System.out.print('│');
            }
            ANSI.gotoRC(infoY + boxH - 1, infoX);
            System.out.print('└');
            System.out.print(repeat('─', infoW - 2));
            System.out.print('┘');
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

    private static String skillLine(String nombre, int nivel, double xp, int labelW, int barW, int lvlW, int totalW, int gap) {
        String label = padRight(nombre, labelW);
        String bar = barraW(xp, barW);
        String lvl = String.format("%" + Math.max(3, lvlW) + "s", "Lv " + nivel);
        String out = label + (gap > 0 ? " " : "") + bar + lvl;
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

    private static void putColored(int x, int y, String txt, int fg, boolean bold) {
        ANSI.gotoRC(y, x);
        ANSI.setFg(fg);
        if (bold) ANSI.boldOn();
        System.out.print(txt);
        ANSI.resetStyle();
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
        boolean injHead = hasInjury(s, GameState.BodyPart.CABEZA);
        boolean injTorso = hasInjury(s, GameState.BodyPart.TORSO);
        boolean injArmL = hasInjury(s, GameState.BodyPart.BRAZO_IZQ);
        boolean injArmR = hasInjury(s, GameState.BodyPart.BRAZO_DER);
        boolean injHands = hasInjury(s, GameState.BodyPart.MANOS);
        boolean injLegL = hasInjury(s, GameState.BodyPart.PIERNA_IZQ);
        boolean injLegR = hasInjury(s, GameState.BodyPart.PIERNA_DER);
        boolean injFeet = hasInjury(s, GameState.BodyPart.PIES);

        if (injHead) {
            putColored(cx - 3, top + 0, "  ___  ", 91, false);
            putColored(cx - 3, top + 1, " /   \\", 91, false);
            putColored(cx - 3, top + 2, "| 0 0 |", 91, false);
            putColored(cx - 3, top + 3, " \\___/", 91, false);
        } else {
            put(cx - 3, top + 0, "  ___  ");
            put(cx - 3, top + 1, " /   \\");
            put(cx - 3, top + 2, "| 0 0 |");
            put(cx - 3, top + 3, " \\___/");
        }

        for (int i = 0; i < 3; i++) {
            if (injTorso) putColored(cx, top + 4 + i, "|", 91, false);
            else put(cx, top + 4 + i, "|");
        }

        if (injArmL) putColored(cx - 6, top + 6, "/-----", 91, false);
        else put(cx - 6, top + 6, "/-----");

        if (injTorso) putColored(cx, top + 6, "|", 91, false);
        else put(cx, top + 6, "|");

        if (injArmR) putColored(cx + 1, top + 6, "-----\\", 91, false);
        else put(cx + 1, top + 6, "-----\\");

        if (injHands) {
            putColored(cx - 6, top + 6, "/", 91, true);
            putColored(cx + 6, top + 6, "\\", 91, true);
        }

        for (int i = 0; i < 3; i++) {
            if (injTorso) putColored(cx, top + 7 + i, "|", 91, false);
            else put(cx, top + 7 + i, "|");
        }

        put(cx - 1, top + 10, "/ \\");
        put(cx - 2, top + 11, "/   \\");
        put(cx - 3, top + 12, "/     \\");
        put(cx - 4, top + 13, "/       \\");
        put(cx - 5, top + 14, "/         \\");

        if (injLegL) {
            putColored(cx - 1, top + 10, "/", 91, false);
            putColored(cx - 2, top + 11, "/", 91, false);
            putColored(cx - 3, top + 12, "/", 91, false);
            putColored(cx - 4, top + 13, "/", 91, false);
        }
        if (injLegR) {
            putColored(cx + 1, top + 10, "\\", 91, false);
            putColored(cx + 2, top + 11, "\\", 91, false);
            putColored(cx + 3, top + 12, "\\", 91, false);
            putColored(cx + 4, top + 13, "\\", 91, false);
        }
        if (injFeet) {
            putColored(cx - 5, top + 14, "/", 91, true);
            putColored(cx + 5, top + 14, "\\", 91, true);
        }

        int sel = Math.floorMod(s.statsBodySel, GameState.BodyPart.values().length);
        var bp = GameState.BodyPart.values()[sel];
        boolean injSel = hasInjury(s, bp);
        if (injSel) ANSI.setFg(91);
        else ANSI.resetStyle();
        switch (bp) {
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
        ANSI.resetStyle();
    }

    private boolean hasInjury(GameState s, GameState.BodyPart p) {
        var l = s.injuries.get(p);
        return l != null && !l.isEmpty();
    }

    private static java.util.List<String> wrap(String text, int width) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (text == null || width <= 0) return out;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if (line.length() == 0) {
                if (w.length() <= width) line.append(w);
                else out.add(w.substring(0, Math.min(w.length(), width)));
            } else {
                if (line.length() + 1 + w.length() <= width) {
                    line.append(' ').append(w);
                } else {
                    out.add(line.toString());
                    line.setLength(0);
                    if (w.length() <= width) line.append(w);
                    else out.add(w.substring(0, Math.min(w.length(), width)));
                }
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    private static String descHabilidad(String nombre) {
        String n = nombre == null ? "" : nombre.toLowerCase();
        if (n.contains("atlet")) return "Aumenta velocidad, resistencia y eficacia al correr o esquivar.";
        if (n.contains("fuerz")) return "Potencia el daño cuerpo a cuerpo y la capacidad de carga.";
        if (n.contains("agil")) return "Mejora acciones rápidas, trepar y maniobrar sin ser alcanzado.";
        if (n.contains("sigil")) return "Reduce el ruido y mejora la furtividad al evitar detección.";
        if (n.contains("punter") || n.contains("disparo")) return "Mejora precisión y control con armas a distancia.";
        if (n.contains("pistola")) return "Aumenta precisión y manejo de pistolas.";
        if (n.contains("escop")) return "Aumenta control y efectividad con escopetas.";
        if (n.contains("rifle")) return "Mejora precisión y estabilidad con rifles.";
        if (n.contains("arco")) return "Mejora tiro con arco y recuperación de flechas.";
        if (n.contains("cuchill") || n.contains("arma blanca"))
            return "Aumenta daño y control con armas ligeras cortas.";
        if (n.contains("hacha")) return "Aumenta daño y talado eficiente con hachas.";
        if (n.contains("lanza")) return "Mejora alcance y control con lanzas.";
        if (n.contains("medic") || n.contains("aux")) return "Mejora curación, vendajes y tratamiento de lesiones.";
        if (n.contains("constru") || n.contains("carp") || n.contains("madera"))
            return "Permite construir, reparar y mejorar estructuras y objetos.";
        if (n.contains("craf") || n.contains("fabric")) return "Desbloquea recetas y aumenta la calidad de lo creado.";
        if (n.contains("caza")) return "Mejora rastreo, sigilo y rendimiento al cazar.";
        if (n.contains("pesca")) return "Mejora probabilidad y tamaño de capturas.";
        if (n.contains("recolec") || n.contains("forraje"))
            return "Mejora identificación y aprovechamiento de recursos.";
        if (n.contains("agric") || n.contains("granja"))
            return "Mejora siembra, crecimiento y rendimiento de cultivos.";
        if (n.contains("cocina")) return "Mejora calidad y beneficios de los alimentos preparados.";
        if (n.contains("costur") || n.contains("sastre"))
            return "Mejora reparación y mejora de ropa y armaduras blandas.";
        if (n.contains("mecan")) return "Mejora reparación de herramientas, armas y vehículos.";
        if (n.contains("electric")) return "Permite fabricar y mantener aparatos eléctricos.";
        if (n.contains("forja") || n.contains("metal")) return "Permite trabajar metales y crear piezas resistentes.";
        return "Mejora eficacia en tareas relacionadas con \"" + nombre + "\".";
    }

    private static String descLesion(String nombre, int severidad) {
        String n = nombre == null ? "" : nombre.toLowerCase();
        if (n.contains("corte") || n.contains("lacer"))
            return "Corte que puede sangrar y limitar movimientos. Severidad " + severidad + "%.";
        if (n.contains("sangr")) return "Pérdida de sangre; requiere vendaje inmediato. Severidad " + severidad + "%.";
        if (n.contains("fract") || n.contains("rotur"))
            return "Hueso dañado; movilidad reducida, necesita inmovilización. Severidad " + severidad + "%.";
        if (n.contains("torced") || n.contains("esguin"))
            return "Daño en ligamentos; dolor y menor estabilidad. Severidad " + severidad + "%.";
        if (n.contains("contus") || n.contains("golpe"))
            return "Hematoma/contusión; reduce rendimiento temporal. Severidad " + severidad + "%.";
        if (n.contains("quemad")) return "Daño por calor; requiere curas y reposo. Severidad " + severidad + "%.";
        if (n.contains("mord") || n.contains("morded"))
            return "Herida por mordedura; riesgo de infección alto. Severidad " + severidad + "%.";
        if (n.contains("infecc")) return "Infección activa; requiere tratamiento. Severidad " + severidad + "%.";
        return "Lesión \"" + nombre + "\" que limita acciones. Severidad " + severidad + "%.";
    }

    private static String descStatFromText(String line) {
        String key = line;
        int i = line.indexOf(':');
        if (i >= 0) key = line.substring(0, i).trim();
        key = key.toLowerCase();
        if (key.startsWith("salud")) return "Vitalidad general. Si llega a 0, quedas incapacitado o mueres.";
        if (key.startsWith("energ"))
            return "Reserva de esfuerzo. Se consume al correr/pelear; descansa para recuperarla.";
        if (key.startsWith("hamb")) return "Necesidad de alimento. Aumenta con el tiempo; comer la reduce.";
        if (key.startsWith("sed")) return "Necesidad de agua. Hidrátate para evitar penalizaciones.";
        if (key.startsWith("sueñ") || key.startsWith("sueno"))
            return "Cansancio acumulado. Dormir lo reduce y mejora el rendimiento.";
        if (key.startsWith("temp")) return "Temperatura corporal. Valores bajos o altos causan penalizaciones.";
        if (key.startsWith("frío") || key.startsWith("frio"))
            return "Exposición al frío. Abrígate o busca calor para mitigarlo.";
        if (key.startsWith("miedo")) return "Estrés/temor. Reduce precisión y eficacia en combate.";
        if (key.startsWith("aburr")) return "Falta de estímulo. Afecta el ánimo y la recuperación.";
        if (key.startsWith("malestar")) return "Sensación general de enfermedad. Penaliza varias acciones.";
        if (key.startsWith("dolor")) return "Dolencia física. Reduce precisión y acciones exigentes.";
        if (key.startsWith("infecc")) return "Progreso de infección. Necesita tratamiento para no empeorar.";
        if (key.startsWith("radi")) return "Exposición a radiación. Provoca daño y efectos acumulativos.";
        return "Atributo del personaje que influye en el rendimiento general.";
    }
}
