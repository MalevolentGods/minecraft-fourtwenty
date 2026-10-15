package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.ScrollingList;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DispensaryStatusWindow extends AbstractModuleWindow<DispensaryStatusView> {
    public DispensaryStatusWindow(DispensaryStatusView view) { super(view, ResourceLocation.parse("fourtwenty:gui/dispensary_status.xml")); }
    @Override public void onOpened() {
        super.onOpened();
        findPaneOfTypeByID("conditions", ScrollingList.class).setDataProvider(() -> Math.max(1, moduleView.status.conditions().size()), (index, row) -> {
            var conditions = moduleView.status.conditions();
            row.findPaneOfTypeByID("condition", Text.class).setText(Component.translatable(
                    "fourtwenty.dispensary.status.condition." + (conditions.isEmpty() ? "ready" : conditions.get(index))));
        });
        updateLabels();
    }
    @Override public void onUpdate() { super.onUpdate(); updateLabels(); }
    private void updateLabels() {
        var s = moduleView.status;
        label("level", s.level());
        findPaneOfTypeByID("worker", Text.class).setText(s.worker().isEmpty()
                ? Component.translatable("fourtwenty.dispensary.status.no_worker")
                : Component.translatable("fourtwenty.dispensary.status.worker", s.worker()));
        label("ingredients", s.buds(), s.paper()); label("joints", s.joints(), s.target());
        label("available", s.available()); label("customers", s.customers());
    }
    private void label(String id, Object... values) {
        findPaneOfTypeByID(id, Text.class).setText(Component.translatable("fourtwenty.dispensary.status." + id, values));
    }
}
