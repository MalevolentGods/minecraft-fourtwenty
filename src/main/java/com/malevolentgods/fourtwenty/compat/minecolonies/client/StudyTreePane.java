package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.Alignment;
import com.ldtteam.blockui.BOGuiGraphics;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ZoomDragView;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBoardModule;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.domain.college.StudyBoard;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One branch of the Weed College's tree (specs/017-weed-college T024): a column per tier, each study a button in the University's colours
 * (cream to queue, blue queued or under way, green done, grey locked), placed level with its parents where it can be, with lines from
 * each parent. Clicking a study selects it; the window shows its details.
 */
final class StudyTreePane {
    private StudyTreePane() {}

    static final String PREFIX = "study|";
    static final int BUTTON_W = 132, BUTTON_H = 22, GAP = 28, ROW = 27, PAD = 6, TOP = 16;
    private static final int LINE = 0xFFB89A74, LINE_SELECTED = 0xFF5C3A1E, FRAME_SELECTED = 0xFF3B2412;
    private static final String TEXTURES = "minecolonies:textures/gui/research/research_button_medium_";

    /** Rebuilds {@code tree} with {@code branch}'s studies, keeping where the player had scrolled and zoomed. */
    static void build(ZoomDragView tree, CollegeBoardModule.Board board, Study.Branch branch, String selected) {
        double scrollX = tree.getScrollX(), scrollY = tree.getScrollY(), scale = tree.getScale();
        clear(tree);

        // columns by tier
        TreeMap<Integer, List<CollegeBoardModule.Entry>> columns = new TreeMap<>();
        for (var e : board.entries()) if (e.branch() == branch) columns.computeIfAbsent(e.tier(), t -> new ArrayList<>()).add(e);
        Map<String, int[]> at = new HashMap<>();   // id -> {x, y}
        int col = 0, bottom = 0;
        for (var column : columns.entrySet()) {
            int x = PAD + col * (BUTTON_W + GAP);
            // each study wants the height of its parents' middle; then they're stacked in that order without overlapping
            List<double[]> wanted = new ArrayList<>();
            List<CollegeBoardModule.Entry> studies = column.getValue();
            for (int i = 0; i < studies.size(); i++) {
                double sum = 0;
                int n = 0;
                for (String p : studies.get(i).parents()) {
                    int[] pos = at.get(p);
                    if (pos != null) { sum += pos[1]; n++; }
                }
                wanted.add(new double[] { n == 0 ? TOP + i * ROW : sum / n, i });
            }
            wanted.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
            int y = TOP;
            for (double[] w : wanted) {
                y = Math.max(y, (int) Math.round(w[0]));
                at.put(studies.get((int) w[1]).id(), new int[] { x, y });
                bottom = Math.max(bottom, y + BUTTON_H);
                y += ROW;
            }
            Text heading = new Text();
            heading.setSize(BUTTON_W, 10);
            heading.setPosition(x, 3);
            heading.setColors(0x49352b);
            heading.setTextAlignment(Alignment.MIDDLE);
            heading.setText(Component.translatable("college.fourtwenty.board.tier", column.getKey()));
            heading.setEnabled(false);
            tree.addChild(heading);
            col++;
        }
        int width = PAD * 2 + Math.max(1, col) * (BUTTON_W + GAP) - GAP;

        // lines first, so the buttons sit on top of them
        Rects lines = new Rects();
        lines.setSize(width, bottom + PAD);
        lines.setPosition(0, 0);
        for (var e : board.entries()) {
            int[] child = at.get(e.id());
            if (child == null) continue;
            for (String p : e.parents()) {
                int[] parent = at.get(p);
                if (parent == null) continue;
                int colour = e.id().equals(selected) || p.equals(selected) ? LINE_SELECTED : LINE;
                int y1 = parent[1] + BUTTON_H / 2, y2 = child[1] + BUTTON_H / 2, mid = child[0] - GAP / 2;
                lines.add(parent[0] + BUTTON_W, y1, mid - parent[0] - BUTTON_W + 1, 1, colour);
                lines.add(mid, Math.min(y1, y2), 1, Math.abs(y2 - y1) + 1, colour);
                lines.add(mid, y2, child[0] - mid, 1, colour);
            }
        }
        lines.setEnabled(false);
        tree.addChild(lines);

        var font = Minecraft.getInstance().font;
        for (var e : board.entries()) {
            int[] pos = at.get(e.id());
            if (pos == null) continue;
            ButtonImage button = new ButtonImage();
            button.setID(PREFIX + e.id());
            button.setSize(BUTTON_W, BUTTON_H);
            button.setPosition(pos[0], pos[1]);
            button.setImage(ResourceLocation.parse(TEXTURES + switch (e.state()) {
                case AVAILABLE -> "plain";
                case QUEUED, IN_PROGRESS -> "blue";
                case DONE -> "light_green";
                case LOCKED -> "light_gray";
            } + ".png"));
            Component name = CollegeClient.name(e);
            int box = BUTTON_W - 25;
            button.setText(name);
            button.setTextAlignment(Alignment.MIDDLE_LEFT);
            button.setTextOffset(22, 0);
            button.setTextRenderBox(box, BUTTON_H);
            button.setColors(e.state() == StudyBoard.State.LOCKED ? 0x5a524a : 0x000000);
            int textWidth = font.width(name);
            if (textWidth > box) button.setTextScale((double) box / textWidth);
            tree.addChild(button);
            PaneBuilders.tooltipBuilder().hoverPane(button).append(name).appendNL(CollegeTreeWindow.stateLine(board, e)).build();

            ItemIcon icon = new ItemIcon();
            icon.setSize(16, 16);
            icon.setPosition(pos[0] + 3, pos[1] + 3);
            icon.setItem(CollegeClient.icon(e));
            icon.setEnabled(false);   // so the click reaches the button under it
            tree.addChild(icon);

            if (e.id().equals(selected)) {
                Rects frame = new Rects();
                frame.setSize(BUTTON_W + 2, BUTTON_H + 2);
                frame.setPosition(pos[0] - 1, pos[1] - 1);
                frame.outline(0, 0, BUTTON_W + 2, BUTTON_H + 2, FRAME_SELECTED);
                frame.setEnabled(false);
                tree.addChild(frame);
            }
        }
        tree.computeContentSize();
        tree.setScaleRaw(scale);
        tree.setScrollX(scrollX);
        tree.setScrollY(scrollY);
    }

    /**
     * Removes every child of {@code view}, and their tooltips with them: BlockUI parents a tooltip to the window, not to its pane, so
     * one left behind by a removed button stays on screen if it was showing.
     */
    static void clear(com.ldtteam.blockui.views.View view) {
        for (Pane child : new ArrayList<>(view.getChildren())) {
            Pane tip = child.getHoverPane();
            if (tip != null && tip.getParent() != null) tip.getParent().removeChild(tip);
            view.removeChild(child);
        }
    }

    /** Flat coloured rectangles: the tree's lines, frames and rules. Draws nothing it is not told to; never takes a click. */
    static final class Rects extends Pane {
        private final List<int[]> rects = new ArrayList<>();

        void add(int x, int y, int w, int h, int argb) { if (w > 0 && h > 0) rects.add(new int[] { x, y, w, h, argb }); }

        void outline(int x, int y, int w, int h, int argb) {
            add(x, y, w, 1, argb);
            add(x, y + h - 1, w, 1, argb);
            add(x, y + 1, 1, h - 2, argb);
            add(x + w - 1, y + 1, 1, h - 2, argb);
        }

        @Override public void drawSelf(BOGuiGraphics target, double mx, double my) {
            for (int[] r : rects) fill(target.pose(), x + r[0], y + r[1], r[2], r[3], r[4]);
        }
    }
}
