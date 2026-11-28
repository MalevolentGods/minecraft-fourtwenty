package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.Alignment;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingGroup;
import com.ldtteam.blockui.views.View;
import com.ldtteam.blockui.views.ZoomDragView;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBoardModule;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeNetwork;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.domain.college.StudyBoard;
import com.malevolentgods.fourtwenty.domain.college.StudyRules;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Locale;

/**
 * One branch of the Weed College's board (specs/017-weed-college US2, T023 and T025): the tree to drag and zoom on the left, the selected
 * study's details on the right (what it gives, how long, what it costs now, and why it's locked), and a button to queue it or take it off
 * the queue. Colour studies say what they're for (FR-026b) and show their colours only to a player who has discovered them (R10).
 */
public final class CollegeTreeWindow extends AbstractWindowSkeleton {
    private static final int DETAILS_W = 112;
    private final CollegeBoardView view;
    private final Study.Branch branch;
    private String selected;
    private int seen = -1;

    public CollegeTreeWindow(CollegeBoardView view, Study.Branch branch) {
        super(ResourceLocation.parse("fourtwenty:gui/college_tree.xml"));
        this.view = view;
        this.branch = branch;
        findPaneOfTypeByID("title", Text.class).setText(Component.translatable("college.fourtwenty.board.tree_title", CollegeClient.branch(branch)));
        ZoomDragView tree = findPaneOfTypeByID("tree", ZoomDragView.class);
        tree.setMinScale(0.4);
        tree.setMaxScale(1.5);
        registerButton("cancel", () -> new CollegeBoardWindow(view).open());
        registerButton("act", this::act);
    }

    @Override public void onOpened() { super.onOpened(); redraw(); }

    @Override public void onUpdate() {
        super.onUpdate();
        if (seen != view.version) redraw();
    }

    @Override public void onButtonClicked(Button button) {
        String id = button.getID();
        if (id != null && id.startsWith(StudyTreePane.PREFIX)) {
            selected = id.substring(StudyTreePane.PREFIX.length());
            redraw();
            return;
        }
        super.onButtonClicked(button);
    }

    private void redraw() {
        seen = view.version;
        var board = view.board;
        if (selected != null && board.entry(selected) == null) selected = null;
        StudyTreePane.build(findPaneOfTypeByID("tree", ZoomDragView.class), board, branch, selected);
        details(board, selected == null ? null : board.entry(selected));
    }

    private void act() {
        var e = selected == null ? null : view.board.entry(selected);
        if (e == null) return;
        if (e.state() == StudyBoard.State.AVAILABLE) CollegeClient.send(view, CollegeNetwork.Action.QUEUE, e.id());
        else if (removable(view.board, e)) CollegeClient.send(view, CollegeNetwork.Action.REMOVE, e.id());
    }

    private static boolean removable(CollegeBoardModule.Board board, CollegeBoardModule.Entry e) {
        return e.state() == StudyBoard.State.QUEUED || e.state() == StudyBoard.State.IN_PROGRESS && !board.headUnderWay();
    }

    // ---- the details pane

    private void details(CollegeBoardModule.Board board, CollegeBoardModule.Entry e) {
        ScrollingGroup group = findPaneOfTypeByID("details", ScrollingGroup.class);
        View box = group.getContainer();
        double scroll = group.getScrollY();
        StudyTreePane.clear(box);
        Button act = findPaneOfTypeByID("act", Button.class);
        int y = 0;
        if (e == null) {
            y = text(box, y, Component.translatable("college.fourtwenty.board.pick"), 0x49352b);
            act.setVisible(false);
        } else {
            y = text(box, y, CollegeClient.name(e).copy().withStyle(ChatFormatting.BOLD), 0x000000);
            y = text(box, y, Component.translatable("college.fourtwenty.board.tier_line", CollegeClient.branch(e.branch()), e.tier(),
                    length(CollegeBoardWindow.units(e), board.unitTicks())), 0x49352b) + 3;
            y = text(box, y, stateLine(board, e), stateColour(e.state())) + 3;
            y = text(box, y, Component.translatable(CollegeClient.nameKey(e.id()) + ".desc"), 0x000000) + 3;
            if (e.kind() == Study.Kind.COLOURS) {
                y = text(box, y, Component.translatable("college.fourtwenty.board.for_tie_dye").withStyle(ChatFormatting.ITALIC), 0x49352b);
                y = swatches(box, y + 1, e) + 3;
            }
            if (!e.cost().isEmpty()) {
                y = text(box, y, Component.translatable(e.state() == StudyBoard.State.IN_PROGRESS && board.headStarted()
                        ? "college.fourtwenty.board.supplies_fixed" : "college.fourtwenty.board.supplies"), 0x000000);
                for (Study.Supply s : e.cost()) y = supply(box, y, s);
            }
            if (e.state() == StudyBoard.State.AVAILABLE) {
                act.setText(Component.translatable("college.fourtwenty.board.queue_it"));
                act.setVisible(true);
            } else if (removable(board, e)) {
                act.setText(Component.translatable("college.fourtwenty.board.remove_it"));
                act.setVisible(true);
            } else {
                act.setVisible(false);
            }
        }
        group.getContainer().computeContentHeight();
        group.setScrollY(e != null && e.id().equals(shownBefore) ? scroll : 0);
        shownBefore = e == null ? null : e.id();
    }

    private String shownBefore;

    /** A wrapped line of text; returns the y below it. */
    private static int text(View box, int y, Component text, int colour) {
        int lines = Math.max(1, Minecraft.getInstance().font.split(text, DETAILS_W).size());
        Text t = new Text();
        t.setSize(DETAILS_W, lines * 10);
        t.setPosition(0, y);
        t.setColors(colour);
        t.setTextWrap(true);
        t.setText(text);
        box.addChild(t);
        return y + lines * 10;
    }

    /** The colours a colour study teaches: each one's hemp, named in its tooltip, or a grey "?" to a player who hasn't discovered it. */
    private static int swatches(View box, int y, CollegeBoardModule.Entry e) {
        int x = 0;
        for (String code : e.colours()) {
            if (x + 16 > DETAILS_W) { x = 0; y += 18; }
            var hemp = ModTerpenes.HEMP.get(code);
            if (hemp != null && TerpeneKnowledge.knows(code)) {
                ItemIcon icon = new ItemIcon();
                icon.setSize(16, 16);
                icon.setPosition(x, y);
                icon.setItem(new ItemStack(hemp.items().getFirst().get()));
                box.addChild(icon);
            } else {
                StudyTreePane.Rects swatch = new StudyTreePane.Rects();
                swatch.setSize(16, 16);
                swatch.setPosition(x, y);
                swatch.add(0, 0, 16, 16, 0xFF8E8A84);
                swatch.outline(0, 0, 16, 16, 0xFF5A554F);
                box.addChild(swatch);
                Text mark = new Text();
                mark.setSize(16, 16);
                mark.setPosition(x, y);
                mark.setTextAlignment(Alignment.MIDDLE);
                mark.setColors(0xFFFFFF);
                mark.setText(Component.literal("?"));
                box.addChild(mark);
                PaneBuilders.tooltipBuilder().hoverPane(mark).append(Component.translatable("college.fourtwenty.board.colour_unknown")).build();
            }
            x += 18;
        }
        return y + 18;
    }

    /** One supply line: the item's icon (with its strain, if the line names one) and how many. */
    private static int supply(View box, int y, Study.Supply s) {
        var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(s.item()));
        ItemStack stack = item.map(ItemStack::new).orElse(ItemStack.EMPTY);
        if (s.strain() != null && !stack.isEmpty()) stack = ModDataComponents.withStrain(stack, Strain.byId(s.strain()));
        ItemIcon icon = new ItemIcon();
        icon.setSize(16, 16);
        icon.setPosition(0, y);
        icon.setItem(stack);
        box.addChild(icon);
        Component name = stack.isEmpty() ? Component.literal(s.item()) : stack.getHoverName();
        Text t = new Text();
        t.setSize(DETAILS_W - 19, 16);
        t.setPosition(19, y);
        t.setColors(0x000000);
        t.setTextAlignment(Alignment.MIDDLE_LEFT);
        MutableComponent line = Component.translatable("college.fourtwenty.board.supply_line", s.count(), name);
        int width = Minecraft.getInstance().font.width(line);
        if (width > DETAILS_W - 19) t.setTextScale((double) (DETAILS_W - 19) / width);
        t.setText(line);
        box.addChild(t);
        return y + 18;
    }

    /** "About 2.4 hours", from the study's units and the server's unit length. */
    private static Component length(int units, int unitTicks) {
        double minutes = units * (double) unitTicks / 1200.0;
        if (minutes < 60) return Component.translatable("college.fourtwenty.board.minutes", Math.max(1, Math.round(minutes)));
        double hours = minutes / 60.0;
        return Component.translatable("college.fourtwenty.board.hours", hours < 10 ? String.format(Locale.ROOT, "%.1f", hours)
                : String.valueOf(Math.round(hours)));
    }

    /** Where a study stands, in a line: done, under way, queued, ready, or the reason it's locked. */
    static Component stateLine(CollegeBoardModule.Board board, CollegeBoardModule.Entry e) {
        return switch (e.state()) {
            case DONE -> Component.translatable("college.fourtwenty.board.state.done");
            case IN_PROGRESS -> Component.translatable("college.fourtwenty.board.state.in_progress", board.headUnits(), CollegeBoardWindow.units(e));
            case QUEUED -> Component.translatable("college.fourtwenty.board.state.queued", board.queue().indexOf(e.id()) + 1);
            case AVAILABLE -> Component.translatable("college.fourtwenty.board.state.available");
            case LOCKED -> lockLine(board, e);
        };
    }

    private static Component lockLine(CollegeBoardModule.Board board, CollegeBoardModule.Entry e) {
        String arg = e.lockArg() == null ? "" : e.lockArg();
        return switch (e.lock()) {
            case FEATURE -> Component.translatable("college.fourtwenty.lock.feature", Component.translatable("college.fourtwenty.feature." + arg));
            case PARENT -> Component.translatable("college.fourtwenty.lock.parent", CollegeClient.name(board, arg));
            case LEVEL -> Component.translatable("college.fourtwenty.lock.level", arg);
            case BRANCH -> Component.translatable("college.fourtwenty.lock.branch", Component.translatable("college.fourtwenty.branch." + arg));
            case NONE -> Component.translatable("college.fourtwenty.board.state.locked");
        };
    }

    private static int stateColour(StudyBoard.State state) {
        return switch (state) {
            case DONE -> 0x2e6b1f;
            case IN_PROGRESS, QUEUED -> 0x23466e;
            case AVAILABLE -> 0x49352b;
            case LOCKED -> 0x8a2b1c;
        };
    }
}
