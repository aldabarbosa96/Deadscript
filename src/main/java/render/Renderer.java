package render;

import items.Equipment;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import ui.menu.*;
import ui.menu.player.EquipmentPanel;
import ui.menu.player.PlayerHud;
import ui.menu.player.PlayerStates;
import utils.ANSI;
import game.GameState;

import java.time.LocalTime;

import static game.Constants.*;
import static utils.EntityUtil.*;

public class Renderer {
    private PlayerHud hud;
    private PlayerStates states;
    private EquipmentPanel equip;
    private MapView mapView;
    private MessageLog msgLog;
    private ActionBar actionBar;
    private InspectView inspect;
    private int inspectTop, inspectLeft, inspectW, inspectH;
    private final InventoryView invOverlay = new InventoryView();
    private final EquipmentView equipOverlay = new EquipmentView();
    private Terminal term;
    private int lastCols = -1, lastRows = -1;

    public void init(GameState s, Terminal term) {
        this.term = term;

        ANSI.setEnabled(true);
        ANSI.useAltScreen(true);
        ANSI.setCursorVisible(false);
        ANSI.setWrap(false);

        hud = null;
        states = null;
        equip = null;
        inspect = new InspectView();
        msgLog = new MessageLog(1, 1, 40, 8);
        actionBar = new ActionBar(1, 1, 40);

        recomputeLayout(s, true);
        ANSI.clearScreenAndHome();
        renderAll(s);
    }

    public void onMapChanged(GameState s) {
        recomputeLayout(s, false);
    }

    public void renderAll(GameState s) {
        String hora = LocalTime.now().format(TS_FMT);
        hud.renderHud(1, hora, "Soleado", s.temperaturaC, s.ubicacion, s.salud, s.maxSalud, s.energia, s.maxEnergia, s.hambre, s.maxHambre, s.sed, s.maxSed, s.sueno, s.maxSueno, s.px, s.py, rumboTexto(s.lastDx, s.lastDy));

        states.renderStates(s.salud, s.maxSalud, s.energia, s.maxEnergia, s.hambre, s.maxHambre, s.sed, s.maxSed, s.sueno, s.maxSueno, s.sangrado, s.infeccionPct, s.escondido);

        Equipment eq = s.equipment;
        String arma = eq.nombreOGuion(eq.getMainHand());
        String off = eq.nombreOGuion(eq.getOffHand());
        String cabeza = eq.nombreOGuion(eq.getHead());
        String pecho = eq.nombreOGuion(eq.getChest());
        String manos = eq.nombreOGuion(eq.getHands());
        String piernas = eq.nombreOGuion(eq.getLegs());
        String pies = eq.nombreOGuion(eq.getFeet());
        String mochila = eq.nombreOGuion(eq.getBackpack());
        double peso = eq.pesoTotalKg(s.inventory);
        double capacidad = eq.capacidadKg();

        equip.render(arma, off, cabeza, pecho, manos, piernas, pies, mochila, 0, 0, peso, capacidad);

        if (!s.inventoryOpen && !s.equipmentOpen) {
            mapView.render(s.map, s.px, s.py);
            renderEntities(s);
        }

        if (s.inventoryOpen) {
            int top = MAP_TOP + 2;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH();
            invOverlay.render(top, left, w, h, s.inventory, s.invSel);
            if (s.invActionsOpen && s.invActions != null && !s.invActions.isEmpty()) {
                invOverlay.renderActionMenu(top, left, w, h, s.invActions, s.invActionSel);
            }
        }

        if (s.equipmentOpen) {
            int top = MAP_TOP + 2;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH();
            equipOverlay.render(top, left, w, h, s.equipment, s.inventory, s.eqSel);
            if (s.eqActionsOpen && s.eqActions != null && !s.eqActions.isEmpty()) {
                equipOverlay.renderActionMenu(top, left, w, h, s.eqActions, s.eqActionSel);
            }
            if (s.eqSelectOpen) {
                equipOverlay.renderSelectMenu(top, left, w, h, s.eqSelectItems, s.eqSelectSel, "EQUIPAR", s.eqSelectItems == null || s.eqSelectItems.isEmpty());
            }
        }

        if (s.worldActionsOpen && s.worldActions != null && !s.worldActions.isEmpty()) {
            int top = MAP_TOP + 2;
            int left = mapView.getLeft();
            int w = mapView.getViewW();
            int h = mapView.getViewH();
            invOverlay.renderActionMenu(top, left, w, h, s.worldActions, s.worldActionSel);
        }

        msgLog.render();
        renderInspectPanel(s);
        actionBar.render();

        ANSI.gotoRC(1, 1);
        ANSI.flush();
    }


    private void renderEntities(GameState s) {
        int camX = cameraX(s), camY = cameraY(s);
        int baseTop = MAP_TOP + 2;
        for (world.Entity e : s.entities) {
            int sx = e.x - camX, sy = e.y - camY;
            if (sx < 0 || sy < 0 || sx >= mapView.getViewW() || sy >= mapView.getViewH()) continue;

            boolean vis = mapView.wasVisibleLastRender(e.x, e.y);
            boolean det = mapView.wasDetectedLastRender(e.x, e.y);
            if (!vis && !det) continue;

            ANSI.gotoRC(baseTop + sy, mapView.getLeft() + sx);
            if (vis) {
                e.revealed = true;
                // Color por tipo: Zombi rojo (31), Loot lila (256c #171 aprox)
                if (e.type == world.Entity.Type.LOOT) {
                    // 256-color lilac/purple ~171
                    System.out.print("\u001B[38;5;171m");
                } else {
                    ANSI.setFg(31);
                }
                System.out.print(e.glyph);
            } else {
                // Detectado pero no visible
                if (e.revealed) {
                    ANSI.setFg(90);
                    System.out.print(e.glyph);
                } else {
                    ANSI.setFg(90);
                    System.out.print('?');
                }
            }
            ANSI.resetStyle();
        }
    }

    private void renderInspectPanel(GameState s) {
        if (inspect == null || inspectW < 18) return;

        String title = "OBJETIVO";
        char glyph = ' ';
        String kind = "—";
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();

        int tx, ty;
        world.Entity found = null;

        if (s.worldActionsOpen) {
            tx = s.worldTx;
            ty = s.worldTy;
            found = s.worldTarget;
        } else {
            world.Entity nearLoot = findNearbyLoot(s);
            if (nearLoot != null) {
                tx = nearLoot.x;
                ty = nearLoot.y;
                found = nearLoot;
            } else {
                int dx = s.lastDx, dy = s.lastDy;
                boolean hasDir = !(dx == 0 && dy == 0);
                tx = s.px + (hasDir ? dx : 0);
                ty = s.py + (hasDir ? dy : 0);

                if (tx < 0 || ty < 0 || tx >= s.map.w || ty >= s.map.h) {
                    lines.add("Fuera del mapa.");
                    inspect.render(inspectTop, inspectLeft, inspectW, inspectH, title, glyph, kind, lines);
                    return;
                }

                found = utils.EntityUtil.findTopEntityAt(s, tx, ty);
                if (found == null) {
                    world.Entity under = utils.EntityUtil.findTopEntityAt(s, s.px, s.py);
                    if (under != null) {
                        found = under;
                        tx = s.px;
                        ty = s.py;
                    }
                }
            }
        }

        // Bordes por seguridad
        if (tx < 0 || ty < 0 || tx >= s.map.w || ty >= s.map.h) {
            lines.add("Fuera del mapa.");
            inspect.render(inspectTop, inspectLeft, inspectW, inspectH, title, glyph, kind, lines);
            return;
        }

        boolean vis = wasVisibleLastRender(tx, ty);
        boolean det = wasDetectedLastRender(tx, ty);

        if (found != null) {
            glyph = found.glyph;
            kind = switch (found.type) {
                case ZOMBIE -> "Enemigo";
                case LOOT -> "Botín";
                default -> "Entidad";
            };

            // Nombre amigable; si es LOOT, usa el nombre del ítem
            String name = (found.type == world.Entity.Type.LOOT && found.item != null) ? found.item.getNombre() : utils.EntityUtil.entityName(found);

            title = "OBJETIVO: " + name;

            lines.add(String.format("Pos: (%d,%d)", tx, ty));
            lines.add("Tipo: " + kind);
            lines.add("Visible: " + (vis ? "Sí" : det ? "Detectado" : "No"));
            lines.add("Terreno: " + utils.EntityUtil.tileName(s.map.tiles[ty][tx], s.map.indoor[ty][tx]));
            lines.add("Transitable: " + (s.map.walk[ty][tx] ? "Sí" : "No"));

            // Detalle extra cuando sea LOOT
            if (found.type == world.Entity.Type.LOOT && found.item != null) {
                var it = found.item;
                lines.add("Objeto: " + it.getNombre());
                lines.add(String.format("Peso: %.3f kg  Condición: %d%%", it.getPesoKg(), it.getDurabilidadPct()));
                if (it.getWeapon() != null) {
                    lines.add(String.format("Arma • Daño:%d  Manos:%d  Cadencia:%.2fs", it.getWeapon().danho(), it.getWeapon().manos(), it.getWeapon().cooldownSec()));
                }
                if (it.getArmor() != null) {
                    lines.add(String.format("Armadura • Prot:%d  Abrigo:%d", it.getArmor().proteccion(), it.getArmor().abrigo()));
                }
                if (it.getContainer() != null) {
                    lines.add(String.format("Contenedor • Capacidad: %.2f kg", it.getContainer().capacidadKg()));
                }
                if (it.getWearableSlot() != null) {
                    lines.add("Slot: " + it.getWearableSlot().name());
                }
                String desc = it.getDescripcion();
                if (desc != null && !desc.isBlank()) lines.add(desc);
            }
        } else {
            // Terreno
            char t = s.map.tiles[ty][tx];
            glyph = t;
            kind = "Terreno";
            title = "OBJETIVO: " + utils.EntityUtil.tileName(t, s.map.indoor[ty][tx]);

            lines.add(String.format("Pos: (%d,%d)", tx, ty));
            lines.add("Visible: " + (vis ? "Sí" : det ? "Detectado" : "No"));
            lines.add("Tipo: " + kind);
            lines.add("Transitable: " + (s.map.walk[ty][tx] ? "Sí" : "No"));

            String extra = utils.EntityUtil.tileHint(t);
            if (!extra.isEmpty()) lines.add(extra);
        }

        inspect.render(inspectTop, inspectLeft, inspectW, inspectH, title, glyph, kind, lines);
    }

    private world.Entity findNearbyLoot(GameState s) {
        int px = s.px, py = s.py;
        int dx = s.lastDx, dy = s.lastDy;

        // Prioridad: 1) misma casilla, 2) frente a la dirección,
        // 3) ortogonales, 4) diagonales. Evita duplicados si (dx,dy)==(0,0).
        int[][] candidates = new int[][]{{px, py}, {px + dx, py + dy}, {px + 1, py}, {px - 1, py}, {px, py + 1}, {px, py - 1}, {px + 1, py + 1}, {px + 1, py - 1}, {px - 1, py + 1}, {px - 1, py - 1}};

        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        for (int[] c : candidates) {
            int x = c[0], y = c[1];
            if (x < 0 || y < 0 || x >= s.map.w || y >= s.map.h) continue;
            long key = (((long) x) << 32) ^ (y & 0xffffffffL);
            if (!seen.add(key)) continue;

            world.Entity e = utils.EntityUtil.findTopEntityAt(s, x, y);
            if (e != null && e.type == world.Entity.Type.LOOT) return e;
        }
        return null;
    }

    private void recomputeLayout(GameState s, boolean firstTime) {
        int cols = 120, rows = 40;
        if (term != null) {
            Size sz = term.getSize();
            cols = Math.max(60, sz.getColumns());
            rows = Math.max(24, sz.getRows());
        }
        lastCols = cols;
        lastRows = rows;

        int headerWidth = Math.max(40, cols - HUD_LEFT);

        final int gap = GAP;
        final int minStats = 36;
        final int minStates = 24;
        final int minEquip = 18;
        int availTop = Math.max(0, headerWidth - 2 * gap);
        int wStats = (int) Math.round(availTop * 0.42);
        int wStates = (int) Math.round(availTop * 0.26);
        int wEquip = availTop - wStats - wStates;
        if (wStats < minStats || wStates < minStates || wEquip < minEquip) {
            wStats = Math.max(minStats, wStats);
            wStates = Math.max(minStates, wStates);
            wEquip = Math.max(minEquip, wEquip);
            int used = wStats + wStates + wEquip;
            if (used > availTop) {
                int overflow = used - availTop;
                while (overflow > 0) {
                    if (wStats >= wStates && wStats >= wEquip && wStats > minStats) {
                        wStats--;
                        overflow--;
                        continue;
                    }
                    if (wStates >= wStats && wStates >= wEquip && wStates > minStates) {
                        wStates--;
                        overflow--;
                        continue;
                    }
                    if (wEquip > minEquip) {
                        wEquip--;
                        overflow--;
                        continue;
                    }
                    break;
                }
            }
        }
        int statsLeft = HUD_LEFT;
        int statesLeft = statsLeft + wStats + gap;
        int equipLeft = statesLeft + wStates + gap;

        hud = new PlayerHud(1, statsLeft, headerWidth, wStats);
        states = new PlayerStates(3, statesLeft, wStates);
        int equipRows = Math.min(EQUIP_ROWS, Math.max(6, MAP_TOP - 4));
        equip = new EquipmentPanel(3, equipLeft, wEquip, equipRows);

        final int mapTop = MAP_TOP;
        final int mapFrame = 2;
        final int gapMapLog = 1;
        final int actionBarH = 3;
        final int minMapH = 8;
        final int minLogH = 5;
        int desiredLogH = Math.min(LOG_ROWS, Math.max(minLogH, rows / 6));
        int viewH = rows - (mapTop + mapFrame + gapMapLog + desiredLogH + actionBarH);
        if (viewH < minMapH) {
            desiredLogH = minLogH;
            viewH = rows - (mapTop + mapFrame + gapMapLog + desiredLogH + actionBarH);
            if (viewH < minMapH) viewH = minMapH;
        }

        int viewW = Math.min(headerWidth, s.map.w);
        mapView = new MapView(mapTop, MAP_LEFT, viewW, Math.min(viewH, s.map.h), 18, s.map, 2.0);
        mapView.prefill();

        int logTop = mapTop + mapFrame + viewH + gapMapLog;
        int inspectWMin = 24;
        int proposedInspect = Math.max(inspectWMin, headerWidth / 4);
        int logW = Math.max(32, headerWidth - proposedInspect - 1);
        int finalInspect = Math.max(inspectWMin, headerWidth - logW - 1);

        if (msgLog == null) {
            msgLog = new MessageLog(logTop, MAP_LEFT, logW, desiredLogH);
        } else {
            msgLog.updateGeometry(logTop, MAP_LEFT, logW, desiredLogH);
        }

        inspectTop = logTop;
        inspectLeft = MAP_LEFT + logW + 1;
        inspectW = finalInspect;
        inspectH = desiredLogH;

        int menuTop = logTop + desiredLogH + 1;
        if (actionBar == null) actionBar = new ActionBar(menuTop, MAP_LEFT, headerWidth);
        else actionBar.updateGeometry(menuTop, MAP_LEFT, headerWidth);

        ANSI.setScrollRegion(mapTop + 2, mapTop + 2 + viewH - 1);
    }

    public boolean wasVisibleLastRender(int x, int y) {
        return mapView.wasVisibleLastRender(x, y);
    }

    public boolean wasDetectedLastRender(int x, int y) {
        return mapView.wasDetectedLastRender(x, y);
    }

    public boolean isNearCamera(int x, int y, GameState s) {
        int camX = cameraX(s), camY = cameraY(s);
        return x >= camX && x < camX + mapView.getViewW() && y >= camY && y < camY + mapView.getViewH();
    }

    public int cameraX(GameState s) {
        return Math.max(0, Math.min(s.px - mapView.getViewW() / 2, s.map.w - mapView.getViewW()));
    }

    public int cameraY(GameState s) {
        return Math.max(0, Math.min(s.py - mapView.getViewH() / 2, s.map.h - mapView.getViewH()));
    }

    public void log(String m) {
        msgLog.add(m);
    }

    public boolean ensureLayoutUpToDate(GameState s) {
        if (term == null) return false;
        org.jline.terminal.Size sz = term.getSize();
        int cols = Math.max(1, sz.getColumns());
        int rows = Math.max(1, sz.getRows());
        if (cols != lastCols || rows != lastRows) {
            ANSI.resetScrollRegion();
            ANSI.clearScreenAndHome();
            recomputeLayout(s, false);
            return true;
        }
        return false;
    }

    public void shutdown() {
        ANSI.resetScrollRegion();
        ANSI.setWrap(true);
        ANSI.setCursorVisible(true);
        ANSI.useAltScreen(false);
    }

    private static String rumboTexto(int dx, int dy) {
        if (dx == 0 && dy == 0) return "-";
        if (dy < 0 && dx == 0) return "NORTE";
        if (dy < 0 && dx > 0) return "NE";
        if (dy == 0 && dx > 0) return "ESTE";
        if (dy > 0 && dx > 0) return "SE";
        if (dy > 0 && dx == 0) return "SUR";
        if (dy > 0) return "SO";
        if (dy == 0) return "OESTE";
        return "NO";
    }
}
