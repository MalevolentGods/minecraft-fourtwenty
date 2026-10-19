package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingGroup;
import com.ldtteam.blockui.views.ScrollingList;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryRegistry;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.AbstractWindowCitizen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The Budtender's guide to FourTwenty: a citizen-window tab with a topic list and scrolling pages. */
public final class BudtenderGuideWindow extends AbstractWindowCitizen {
    /** Topic ids, in reading order; each has fourtwenty.guide.<id>.title and .text in the lang file. */
    public static final List<String> TOPICS = List.of("welcome", "growing", "recipes", "smoking", "edibles", "bong",
            "specialty", "hookah", "dispensary", "budtender", "customers", "affinity", "benefits", "buying", "lamps",
            "sensi_heights", "faq");
    private static final int PAGE_WIDTH = 148;
    private static final String TAB = "fourtwentyGuideTab", ICON = "fourtwentyGuideIcon";

    public BudtenderGuideWindow(ICitizenDataView citizen) {
        super(citizen, ResourceLocation.parse("fourtwenty:gui/citizen/guide.xml"));
        ScrollingList topics = findPaneOfTypeByID("topics", ScrollingList.class);
        topics.setDataProvider(TOPICS::size, (index, row) ->
                row.findPaneOfTypeByID("topic", Button.class).setText(Component.translatable(key(TOPICS.get(index), "title"))));
        registerButton("topic", button -> showTopic(TOPICS.get(topics.getListElementIndexByPane(button))));
        registerButton("back", this::showIndex);
        showIndex();
    }

    /** Adds the guide tab to a citizen window when that citizen works at a Dispensary. */
    public static void addTab(AbstractWindowSkeleton window, ICitizenDataView citizen) {
        var colony = citizen.getColony();
        var work = citizen.getWorkBuilding();
        var building = colony == null || work == null ? null : colony.getClientBuildingManager().getBuilding(work);
        if (building == null || building.getBuildingType() != DispensaryRegistry.BUILDING.get()) return;
        // Below the native tabs: after the job tab, or after the debug tab when that one is showing.
        Pane debug = window.findPaneByID("debugTab");
        int y = debug != null && debug.isVisible() ? 222 : 196;
        ButtonImage tab = new ButtonImage();
        tab.setID(TAB); tab.setSize(32, 26); tab.setPosition(0, y);
        tab.setImage(ResourceLocation.parse("minecolonies:textures/gui/modules/tab_left_side4.png"));
        ButtonImage icon = new ButtonImage();
        icon.setID(ICON); icon.setSize(20, 20); icon.setPosition(5, y + 3);
        icon.setImage(ResourceLocation.parse("fourtwenty:textures/gui/dispensary_tab.png"));
        // Behind the paper, like the native tabs, so only the left edge shows.
        window.addChild(tab, 0);
        window.addChild(icon, 1);
        PaneBuilders.tooltipBuilder().hoverPane(icon).build().setText(Component.translatable("fourtwenty.guide.tab"));
        window.registerButton(TAB, () -> new BudtenderGuideWindow(citizen).open());
        window.registerButton(ICON, () -> new BudtenderGuideWindow(citizen).open());
    }

    private void showIndex() {
        findPaneOfTypeByID("title", Text.class).setText(Component.translatable("fourtwenty.guide.title"));
        findPaneByID("topics").setVisible(true);
        findPaneByID("page").setVisible(false);
        findPaneByID("back").setVisible(false);
    }

    private void showTopic(String topic) {
        findPaneOfTypeByID("title", Text.class).setText(Component.translatable(key(topic, "title")));
        findPaneByID("topics").setVisible(false);
        ScrollingGroup page = findPaneOfTypeByID("page", ScrollingGroup.class);
        var container = page.getContainer();
        new ArrayList<>(container.getChildren()).forEach(container::removeChild);
        Component text = Component.translatable(key(topic, "text"));
        // Size the text to its wrapped height so the group scrolls through all of it.
        int lines = Minecraft.getInstance().font.split(text, PAGE_WIDTH).size();
        Text body = new Text();
        body.setSize(PAGE_WIDTH, lines * 9 + 2);
        body.setPosition(0, 0);
        body.setColors(0x000000);
        body.setTextWrap(true);
        body.setText(text);
        container.addChild(body);
        container.computeContentHeight();
        page.setScrollY(0);
        page.setVisible(true);
        findPaneByID("back").setVisible(true);
    }

    private static String key(String topic, String part) { return "fourtwenty.guide." + topic + "." + part; }
}
