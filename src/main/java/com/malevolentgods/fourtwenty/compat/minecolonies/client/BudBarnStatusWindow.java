package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.controls.Text;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** The Bud Barn's status tab: what the Trim Daddy is up to, the trays and racks, and what the hut has made. */
public final class BudBarnStatusWindow extends AbstractModuleWindow<BudBarnStatusView> {
    public BudBarnStatusWindow(BudBarnStatusView view) { super(view, ResourceLocation.parse("fourtwenty:gui/budbarn_status.xml")); }

    @Override public void onOpened() { super.onOpened(); updateLabels(); }
    @Override public void onUpdate() { super.onUpdate(); updateLabels(); }

    private void updateLabels() {
        var s = moduleView.status;
        label("level", s.level());
        findPaneOfTypeByID("worker", Text.class).setText(s.worker().isEmpty()
                ? Component.translatable("fourtwenty.budbarn.status.no_worker")
                : Component.translatable("fourtwenty.budbarn.status.worker", s.worker()));
        findPaneOfTypeByID("state", Text.class).setText(s.minutesToNext() >= 0 && s.state() == com.malevolentgods.fourtwenty.compat.minecolonies.domain.BudBarnRules.Status.CURING
                ? Component.translatable("fourtwenty.budbarn.status.state.curing_next", s.minutesToNext())
                : Component.translatable("fourtwenty.budbarn.status.state." + s.state().name().toLowerCase(Locale.ROOT)));
        label("benches", s.trays(), s.racks());
        label("hanging", s.hanging(), s.hanging() + s.free());
        label("stock", s.rough(), s.trimmed());
        label("made", s.curedMade(), s.seedsMade());
    }

    private void label(String id, Object... values) {
        findPaneOfTypeByID(id, Text.class).setText(Component.translatable("fourtwenty.budbarn.status." + id, values));
    }
}
