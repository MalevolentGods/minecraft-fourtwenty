package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.Programs;
import com.malevolentgods.fourtwenty.lights.LightsNetwork;
import com.malevolentgods.fourtwenty.lights.LightsNetwork.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * The Weedweeno's editor: saved programs on the left, the code with line numbers in the middle (the line of an error marked), and Run,
 * Stop, the program buttons, the status and the console below. The server holds the programs; this screen sends edits and shows its state.
 */
public class WeedweenoScreen extends Screen {
    private static final int LIST_W = 92, ROW = 14, GUTTER = 22, BG = 0xE014161A, PANEL = 0xFF20232A, TEXT = 0xFFE3E6DF, DIM = 0xFF8A8F87,
            GREEN = 0xFF7BFF5A, WARN = 0xFFE8B04A, ERROR_LINE = 0x40FF4040;
    private static final String[] STATES = {"stopped", "running", "done", "error", "slow"};

    private LightsNetwork.State state;
    private CodeBox code;
    private EditBox name;
    private int scroll;
    private boolean edited;
    private String loadedCode;
    private int left, top, w, h;
    private final List<Button> listButtons = new ArrayList<>();

    public WeedweenoScreen(LightsNetwork.State state) {
        super(Component.translatable("block.fourtwenty.weedweeno"));
        this.state = state;
    }

    public BlockPos pos() { return state.pos(); }

    /** A code box that tells us how far it's scrolled, for the line numbers. */
    private static final class CodeBox extends MultiLineEditBox {
        CodeBox(net.minecraft.client.gui.Font font, int x, int y, int w, int h) {
            super(font, x, y, w, h, Component.translatable("gui.fourtwenty.weedweeno.placeholder"), Component.translatable("gui.fourtwenty.weedweeno.code"));
        }

        double scrolled() { return scrollAmount(); }
    }

    @Override protected void init() {
        w = Math.min(width - 20, 420);
        h = Math.min(height - 20, 260);
        left = (width - w) / 2;
        top = (height - h) / 2;
        int codeX = left + LIST_W + 8 + GUTTER, codeW = w - LIST_W - 16 - GUTTER;
        name = new EditBox(font, left + LIST_W + 8, top + 18, codeW + GUTTER - 64, 16, Component.translatable("gui.fourtwenty.weedweeno.name"));
        name.setMaxLength(Programs.MAX_NAME);
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("gui.fourtwenty.weedweeno.rename"), b -> act(Action.RENAME))
                .bounds(left + w - 62, top + 17, 56, 18).build());
        code = new CodeBox(font, codeX, top + 40, codeW, h - 112);
        code.setCharacterLimit(Programs.MAX_CODE);
        code.setValueListener(s -> edited = !s.equals(loadedCode));
        addRenderableWidget(code);
        int bx = left + LIST_W + 8, by = top + h - 66, bw = (w - LIST_W - 16 - 14) / 8;
        String[] labels = {"run", "stop", "save", "new", "copy", "delete", "export", "import"};
        Action[] actions = {Action.RUN, Action.STOP, Action.SAVE, Action.NEW, Action.DUPLICATE, Action.DELETE, Action.EXPORT, Action.IMPORT};
        for (int i = 0; i < labels.length; i++) {
            Action a = actions[i];
            addRenderableWidget(Button.builder(Component.translatable("gui.fourtwenty.weedweeno." + labels[i]), b -> act(a))
                    .bounds(bx + (i % 8) * (bw + 2), by, bw, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.fourtwenty.weedweeno.presets"), b -> act(Action.PRESETS))
                .bounds(left + 6, top + h - 24, LIST_W - 4, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> { scroll = Math.max(0, scroll - 1); rebuildList(); }).bounds(left + 6, top + 18, LIST_W - 4, 12).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> { scroll++; rebuildList(); }).bounds(left + 6, top + h - 40, LIST_W - 4, 12).build());
        load(state, true);
    }

    private int visibleRows() { return Math.max(1, (h - 76) / ROW); }

    private void rebuildList() {
        listButtons.forEach(this::removeWidget);
        listButtons.clear();
        var names = state.names();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, names.size() - visibleRows())));
        for (int i = 0; i < visibleRows() && scroll + i < names.size(); i++) {
            int index = scroll + i;
            String label = (index == state.running() ? "▶ " : "") + names.get(index);
            var button = Button.builder(Component.literal(label), b -> pick(index)).bounds(left + 6, top + 32 + i * ROW, LIST_W - 4, ROW - 1).build();
            button.active = index != state.selected();
            listButtons.add(addRenderableWidget(button));
        }
    }

    private void load(LightsNetwork.State s, boolean force) {
        boolean switched = state == null || s.selected() != state.selected() || force;
        state = s;
        if (switched || !edited) {
            loadedCode = s.code();
            code.setValue(s.code());
            edited = false;
            if (!s.names().isEmpty()) name.setValue(s.names().get(Math.min(s.selected(), s.names().size() - 1)));
        }
        rebuildList();
    }

    public void update(LightsNetwork.State s) { load(s, false); }

    private void pick(int index) {
        if (edited) act(Action.SAVE);
        LightsNetwork.send(state.pos(), Action.SELECT, index, "", "");
    }

    private void act(Action action) {
        String text = code.getValue();
        if ((action == Action.SAVE || action == Action.RUN) && !Programs.fits(text)) {
            if (minecraft != null && minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("message.fourtwenty.weedweeno.too_long"), true);
            return;
        }
        LightsNetwork.send(state.pos(), action, state.selected(), name.getValue(), action == Action.SAVE || action == Action.RUN ? text : "");
        if (action == Action.SAVE || action == Action.RUN) {
            loadedCode = text;
            edited = false;
        }
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(left, top, left + w, top + h, BG);
        g.renderOutline(left, top, w, h, 0xFF7BFF5A);
        g.drawString(font, title, left + 6, top + 5, TEXT, false);
        g.fill(left + 4, top + 16, left + LIST_W + 2, top + h - 4, PANEL);
        super.render(g, mouseX, mouseY, partial);
        // line numbers, and the error line marked
        int gx = left + LIST_W + 8, gy = top + 40 + 4;
        g.fill(gx, top + 40, gx + GUTTER - 2, top + 40 + code.getHeight(), PANEL);
        int first = (int) (code.scrolled() / 9);
        g.enableScissor(gx, top + 40, gx + GUTTER + code.getWidth(), top + 40 + code.getHeight());
        for (int row = 0; row < code.getHeight() / 9 + 1; row++) {
            int n = first + row + 1;
            int y = gy + row * 9 - (int) (code.scrolled() % 9);
            boolean bad = state.state() >= 3 && n == state.line();
            if (bad) g.fill(gx, y - 1, gx + GUTTER + code.getWidth(), y + 8, ERROR_LINE);
            g.drawString(font, Integer.toString(n), gx + 2, y, bad ? 0xFFFF6060 : DIM, false);
        }
        g.disableScissor();
        // the status and the console
        int sy = top + h - 44;
        String key = STATES[Math.max(0, Math.min(state.state(), STATES.length - 1))];
        Component status = Component.translatable("gui.fourtwenty.weedweeno.state." + key);
        int colour = state.state() == 1 ? GREEN : state.state() >= 3 ? WARN : DIM;
        g.drawString(font, status, left + LIST_W + 8, sy, colour, false);
        if (!state.message().isEmpty()) g.drawString(font, font.plainSubstrByWidth(state.message(), w - LIST_W - 90), left + LIST_W + 70, sy, WARN, false);
        if (edited) g.drawString(font, Component.translatable("gui.fourtwenty.weedweeno.unsaved"), left + w - 60, top + 5, WARN, false);
        var lines = state.console();
        int cy = sy + 11;
        for (int i = Math.max(0, lines.size() - 2); i < lines.size(); i++) {
            g.drawString(font, font.plainSubstrByWidth("> " + lines.get(i), w - LIST_W - 16), left + LIST_W + 8, cy, DIM, false);
            cy += 9;
        }
    }

    @Override public void renderBackground(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {}

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        LightsNetwork.send(state.pos(), Action.CLOSE, 0, "", "");
        super.onClose();
    }
}
