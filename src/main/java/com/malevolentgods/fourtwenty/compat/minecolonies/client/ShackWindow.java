package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.ldtteam.blockui.views.View;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.ShackBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.ShackModule;
import com.malevolentgods.fourtwenty.compat.minecolonies.tiedye.TieDyeNetwork;
import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.malevolentgods.fourtwenty.tiedye.ModTieDye;
import com.malevolentgods.fourtwenty.tiedye.TieDye;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Tie Dye Shack's Orders tab (specs/018-tie-dye-shack US1, US2; contracts/orders.md): what the Shack is doing, the 8 patterns and
 * 16 colourways as tie-dyed swatches (the ones the colony can't make yet greyed), the kind and count, an Order button and the queue.
 * Colourways are named; colours never are (research R6).
 */
public final class ShackWindow extends AbstractModuleWindow<ShackView> {
    private static final int[] COUNTS = {1, 4, 8, 16, 32, 64, 128, 256};
    private static final String PATTERN = "pattern|", COLOURWAY = "colourway|";
    private static final String FRAME = "minecolonies:textures/gui/builderhut/builder_button_medium_small.png";
    private int pattern, countIndex = 2, seen = -1;
    private String colourway = "ashtray";
    private TieDyeRules.Kind kind = TieDyeRules.Kind.HEMP;

    public ShackWindow(ShackView view) {
        super(view, ResourceLocation.parse("fourtwenty:gui/tiedye_orders.xml"));
        registerButton("kind", this::nextKind);
        registerButton("less", () -> { countIndex = Math.max(0, countIndex - 1); redraw(); });
        registerButton("more", () -> { countIndex = Math.min(COUNTS.length - 1, countIndex + 1); redraw(); });
        registerButton("order", this::order);
        ScrollingList queue = findPaneOfTypeByID("queue", ScrollingList.class);
        registerButton("cancel", (Button b) -> {
            int i = queue.getListElementIndexByPane(b);
            var q = moduleView.view.queue();
            if (i >= 0 && i < q.size()) send(TieDyeNetwork.Action.CANCEL, q.get(i).id());
        });
    }

    @Override public void onOpened() { super.onOpened(); redraw(); }

    @Override public void onUpdate() {
        super.onUpdate();
        if (seen != moduleView.version) redraw();
    }

    @Override public void onButtonClicked(Button button) {
        String id = button.getID();
        if (id != null && id.startsWith(PATTERN)) { pattern = Integer.parseInt(id.substring(PATTERN.length())); redraw(); return; }
        if (id != null && id.startsWith(COLOURWAY)) { colourway = id.substring(COLOURWAY.length()); redraw(); return; }
        super.onButtonClicked(button);
    }

    private void nextKind() {
        var kinds = moduleView.view.kinds();
        if (kinds.isEmpty()) return;
        kind = kinds.get((kinds.indexOf(kind) + 1) % kinds.size());
        redraw();
    }

    private void order() { send(TieDyeNetwork.Action.ADD, 0); }

    private void send(TieDyeNetwork.Action action, int order) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(TieDyeNetwork.OrderAction.TYPE)) return;
        PacketDistributor.sendToServer(new TieDyeNetwork.OrderAction(moduleView.colonyId(), moduleView.position(), action, pattern, colourway, kind,
                "", COUNTS[countIndex], order));
    }

    private void redraw() {
        seen = moduleView.version;
        var v = moduleView.view;
        if (!v.kinds().isEmpty() && !v.kinds().contains(kind)) kind = v.kinds().getFirst();
        set("status", status(v));
        boolean patternOpen = pattern < v.patterns().size() && v.patterns().get(pattern);
        ShackModule.ColourwayView chosen = v.colourways().stream().filter(c -> c.id().equals(colourway)).findFirst().orElse(null);
        set("patternLabel", Component.translatable("tiedye.fourtwenty.orders.pattern", patternName(pattern)));
        set("colourwayLabel", Component.translatable("tiedye.fourtwenty.orders.colourway", chosen == null ? Component.literal("?") : colourwayName(chosen)));

        View patterns = findPaneOfTypeByID("patterns", View.class);
        clear(patterns);
        for (int p = 0; p < TieDyeRules.PATTERNS.size(); p++) {
            boolean open = p < v.patterns().size() && v.patterns().get(p);
            Component tip = open ? patternName(p) : Component.translatable("tiedye.fourtwenty.orders.pattern_locked", patternName(p));
            swatch(patterns, PATTERN + p, p * 20 + 2, 0, new TieDye(p, colourway), p == pattern, !open, tip);
        }
        View colourways = findPaneOfTypeByID("colourways", View.class);
        clear(colourways);
        for (int i = 0; i < v.colourways().size(); i++) {
            var c = v.colourways().get(i);
            MutableComponent tip = colourwayName(c).copy();
            if (!c.orderable()) tip.append("\n").append(Component.translatable("tiedye.fourtwenty.orders.colourway_locked"));
            swatch(colourways, COLOURWAY + c.id(), (i % 8) * 20 + 2, (i / 8) * 20, new TieDye(pattern, c.id()), c.id().equals(colourway), !c.orderable(), tip);
        }

        findPaneOfTypeByID("kind", Button.class).setText(Component.translatable("tiedye.fourtwenty.kind." + kind.name().toLowerCase(Locale.ROOT)));
        set("count", Component.literal(String.valueOf(COUNTS[countIndex])));
        Button order = findPaneOfTypeByID("order", Button.class);
        boolean canOrder = patternOpen && chosen != null && chosen.orderable() && v.kinds().contains(kind);
        order.setText(Component.translatable(canOrder ? "tiedye.fourtwenty.orders.order" : "tiedye.fourtwenty.orders.cannot"));
        order.setEnabled(canOrder);

        findPaneByID("empty").setVisible(v.queue().isEmpty());
        ScrollingList queue = findPaneOfTypeByID("queue", ScrollingList.class);
        queue.setDataProvider(() -> v.queue().size(), (index, row) -> {
            var o = v.queue().get(index);
            var cw = v.colourways().stream().filter(c -> c.id().equals(o.colourway())).findFirst().orElse(null);
            Component name = cw == null ? Component.literal("?") : colourwayName(cw);
            row.findPaneOfTypeByID("line", Text.class).setText(Component.translatable("tiedye.fourtwenty.orders.line", patternName(o.pattern()),
                    Component.translatable("tiedye.fourtwenty.kind." + o.kind().name().toLowerCase(Locale.ROOT)), name, o.made(), o.count()));
            row.findPaneByID("cancel").setVisible(index > 0 || !v.headUnderWay());
        });
        queue.refreshElementPanes();
    }

    private static MutableComponent status(ShackModule.View v) {
        MutableComponent line = Component.translatable("tiedye.fourtwenty.status.level", v.level(), v.made());
        Component state = v.reason().isEmpty()
                ? Component.translatable("tiedye.fourtwenty.status.activity." + v.activity().name().toLowerCase(Locale.ROOT))
                : Component.translatable(v.reason());
        line.append(" ").append(state);
        if (v.activity() == ShackBuilding.Activity.WAITING) {
            List<String> parts = new ArrayList<>();
            for (var m : v.missing()) parts.add(m.count() + " " + BuiltInRegistries.ITEM.get(ResourceLocation.parse(m.item())).getDescription().getString());
            if (!parts.isEmpty()) line.append(" ").append(String.join(", ", parts));
        }
        return line;
    }

    /** A swatch button: a tie-dyed hemp icon in a look, framed when chosen, greyed when it can't be ordered. */
    private static void swatch(View into, String id, int x, int y, TieDye look, boolean chosen, boolean greyed, Component tip) {
        ButtonImage button = new ButtonImage();
        button.setID(id);
        button.setSize(19, 19);
        button.setPosition(x, y);
        button.setImage(ResourceLocation.parse(FRAME));
        into.addChild(button);
        // drawn by hand: an ItemIcon brings the item's own tooltip, which piles up on top of the swatch's
        Swatch icon = new Swatch(ModTieDye.stack(ModTieDye.HEMP_ITEM.get(), look, 1));
        icon.setSize(16, 16);
        icon.setPosition(x + 1, y + 1);
        icon.setEnabled(false);
        into.addChild(icon);
        if (greyed || chosen) {
            StudyTreePane.Rects over = new StudyTreePane.Rects();
            over.setSize(19, 19);
            over.setPosition(x, y);
            if (greyed) over.add(1, 1, 17, 17, 0xA0807870);
            if (chosen) over.outline(0, 0, 19, 19, 0xFF3B2412);
            over.setEnabled(false);
            into.addChild(over);
        }
        PaneBuilders.tooltipBuilder().hoverPane(button).append(tip).build();
    }

    static Component patternName(int pattern) { return Component.translatable("tiedye.fourtwenty.pattern." + pattern); }

    /** A colourway's name. It names a palette, never a colour, so it's shown to everyone (research R6, revised 2026-11-28). */
    static Component colourwayName(ShackModule.ColourwayView c) { return Component.translatable("tiedye.fourtwenty.colourway." + c.id()); }

    /** An item drawn as a picture only: no tooltip of its own. */
    static final class Swatch extends Pane {
        private final net.minecraft.world.item.ItemStack stack;

        Swatch(net.minecraft.world.item.ItemStack stack) { this.stack = stack; }

        @Override public void drawSelf(com.ldtteam.blockui.BOGuiGraphics target, double mx, double my) { target.renderItem(stack, x, y); }
    }

    private static void clear(View view) { for (Pane p : new ArrayList<>(view.getChildren())) view.removeChild(p); }

    private void set(String id, Component text) { findPaneOfTypeByID(id, Text.class).setText(text); }
}
