package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import com.ldtteam.blockui.controls.Text;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBuilding;
import com.minecolonies.core.client.gui.AbstractModuleWindow;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** The Weed College's status tab: who works there, the study in progress and how far along it is, what it's waiting for. */
public final class CollegeStatusWindow extends AbstractModuleWindow<CollegeStatusView> {
    public CollegeStatusWindow(CollegeStatusView view) { super(view, ResourceLocation.parse("fourtwenty:gui/college_status.xml")); }

    @Override public void onOpened() { super.onOpened(); updateLabels(); }
    @Override public void onUpdate() { super.onUpdate(); updateLabels(); }

    private void updateLabels() {
        var s = moduleView.status;
        set("level", Component.translatable("college.fourtwenty.status.level", s.level()));
        set("worker", s.workers().isEmpty() ? Component.translatable("college.fourtwenty.status.no_worker")
                : Component.translatable("college.fourtwenty.status.workers", s.workers()));
        set("study", s.study().isEmpty() ? Component.translatable("college.fourtwenty.status.no_study")
                : Component.translatable("college.fourtwenty.status.study", Component.translatable(name(s.study()))));
        set("progress", s.study().isEmpty() ? Component.empty()
                : s.minutesLeft() >= 0 ? Component.translatable("college.fourtwenty.status.progress_eta", s.units(), s.total(), s.minutesLeft())
                : Component.translatable("college.fourtwenty.status.progress", s.units(), s.total()));
        MutableComponent state = s.reason().isEmpty()
                ? Component.translatable("college.fourtwenty.status.activity." + s.activity().name().toLowerCase(Locale.ROOT))
                : Component.translatable(s.reason(), argument(s.reason(), s.reasonArg()));
        if (s.activity() == CollegeBuilding.Activity.WAITING && !s.missing().isEmpty()) {
            for (var m : s.missing()) {
                state.append(Component.literal("\n  " + m.count() + " x ")).append(BuiltInRegistries.ITEM.get(ResourceLocation.parse(m.item())).getDescription());
            }
        }
        set("state", state);
        set("made", Component.translatable("college.fourtwenty.status.made", s.finished(), s.badBatches()));
    }

    /** A lock's argument as the player should read it: a study's name, or the raw value (a level, a feature). */
    private static Component argument(String reason, String arg) {
        if (arg.isEmpty()) return Component.empty();
        if (reason.endsWith(".parent")) return Component.translatable(name(arg));
        if (reason.endsWith(".feature")) return Component.translatable("college.fourtwenty.feature." + arg);
        if (reason.endsWith(".branch")) return Component.translatable("college.fourtwenty.branch." + arg);
        return Component.literal(arg);
    }

    private static String name(String id) { return "college.fourtwenty.study." + id.substring(id.indexOf(':') + 1); }

    private void set(String id, Component text) { findPaneOfTypeByID(id, Text.class).setText(text); }
}
