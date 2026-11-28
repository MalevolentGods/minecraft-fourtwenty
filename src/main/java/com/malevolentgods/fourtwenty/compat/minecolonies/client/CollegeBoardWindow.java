package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBoardModule;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeNetwork;
import com.malevolentgods.fourtwenty.domain.college.Study;
import com.malevolentgods.fourtwenty.domain.college.StudyBoard;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The Weed College's Board tab (specs/017-weed-college US2, T023): the six branches, each opening its tree, and the queue with the
 * study under way first. Removing a study is a request; the server decides and the board redraws from what it sends back.
 */
public final class CollegeBoardWindow extends AbstractModuleWindow<CollegeBoardView> {
    private int seen = -1;

    public CollegeBoardWindow(CollegeBoardView view) {
        super(view, ResourceLocation.parse("fourtwenty:gui/college_board.xml"));
        for (Study.Branch branch : Study.Branch.values()) {
            registerButton("branch_" + branch.key(), () -> new CollegeTreeWindow(moduleView, branch).open());
        }
        ScrollingList queue = findPaneOfTypeByID("queue", ScrollingList.class);
        registerButton("remove", (Button button) -> {
            int index = queue.getListElementIndexByPane(button);
            var ids = moduleView.board.queue();
            if (index >= 0 && index < ids.size()) CollegeClient.send(moduleView, CollegeNetwork.Action.REMOVE, ids.get(index));
        });
    }

    @Override public void onOpened() { super.onOpened(); redraw(); }

    @Override public void onUpdate() {
        super.onUpdate();
        if (seen != moduleView.version) redraw();
    }

    private void redraw() {
        seen = moduleView.version;
        var board = moduleView.board;
        findPaneOfTypeByID("level", Text.class).setText(board.level() >= com.malevolentgods.fourtwenty.domain.college.StudyRules.MAX_LEVEL
                ? Component.translatable("college.fourtwenty.board.level_max", board.level())
                : Component.translatable("college.fourtwenty.board.level", board.level()));
        for (Study.Branch branch : Study.Branch.values()) {
            int done = 0, total = 0;
            for (var e : board.entries()) {
                if (e.branch() != branch) continue;
                total++;
                if (e.state() == StudyBoard.State.DONE) done++;
            }
            findPaneOfTypeByID("branch_" + branch.key(), Button.class).setText(
                    Component.translatable("college.fourtwenty.board.branch_button", CollegeClient.branch(branch), done, total));
        }
        findPaneByID("empty").setVisible(board.queue().isEmpty());
        ScrollingList queue = findPaneOfTypeByID("queue", ScrollingList.class);
        queue.setDataProvider(() -> board.queue().size(), (index, row) -> {
            String id = board.queue().get(index);
            CollegeBoardModule.Entry e = board.entry(id);
            Component name = CollegeClient.name(board, id);
            Component line = index == 0 && e != null
                    ? Component.translatable("college.fourtwenty.board.queue_head", name, board.headUnits(), units(e))
                    : Component.translatable("college.fourtwenty.board.queue_line", index + 1, name);
            row.findPaneOfTypeByID("name", Text.class).setText(line);
            // the head can come off only while nothing has gone into it; the server has the last word
            row.findPaneByID("remove").setVisible(index > 0 || !board.headUnderWay());
        });
        queue.refreshElementPanes();
    }

    static int units(CollegeBoardModule.Entry e) { return com.malevolentgods.fourtwenty.domain.college.StudyRules.units(e.tier()); }
}
