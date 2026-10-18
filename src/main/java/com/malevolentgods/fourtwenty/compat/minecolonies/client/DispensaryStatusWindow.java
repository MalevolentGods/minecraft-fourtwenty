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
        // Menu stock lines first (cookies, pipes, brownies), then the service conditions.
        findPaneOfTypeByID("conditions", ScrollingList.class).setDataProvider(
                () -> moduleView.status.menu().size() + Math.max(1, moduleView.status.conditions().size()), (index, row) -> {
            var menu = moduleView.status.menu();
            var conditions = moduleView.status.conditions();
            Component text;
            if (index < menu.size()) {
                var line = menu.get(index);
                text = Component.translatable("fourtwenty.dispensary.status.menu", name(line.itemId()), line.held(), line.target());
            } else if (conditions.isEmpty()) {
                text = Component.translatable("fourtwenty.dispensary.status.condition.ready");
            } else {
                String condition = conditions.get(index - menu.size());
                text = condition.startsWith("needs:")
                        ? Component.translatable("fourtwenty.dispensary.status.condition.needs", name(condition.substring(6)))
                        : Component.translatable("fourtwenty.dispensary.status.condition." + condition);
            }
            row.findPaneOfTypeByID("condition", Text.class).setText(text);
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
    private static Component name(String itemId) {
        return new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId))).getHoverName();
    }
    private void label(String id, Object... values) {
        findPaneOfTypeByID(id, Text.class).setText(Component.translatable("fourtwenty.dispensary.status." + id, values));
    }
}
