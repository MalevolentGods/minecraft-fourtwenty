package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.ldtteam.blockui.PaneBuilders;
import com.ldtteam.blockui.controls.Text;
import com.malevolentgods.fourtwenty.compat.minecolonies.DispensaryIds;
import com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CannabisHappinessModifier;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand.Status;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.CitizenWindowUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Localize the authoritative snapshot, including pending demand which native neutral rows hide. */
@Mixin(value = CitizenWindowUtils.class, remap = false)
public abstract class CitizenHappinessWindowMixin {
    @Redirect(method = "updateHappiness", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/chat/Component;translatableEscape(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent fourtwenty$description(String key, Object[] args, ICitizenDataView citizen, AbstractWindowSkeleton window) {
        if (key.endsWith(DispensaryIds.SATISFACTION)
                && citizen.getHappinessHandler().getModifier(DispensaryIds.SATISFACTION) instanceof CannabisHappinessModifier modifier) {
            boolean description = key.contains(".desc.");
            key = "fourtwenty.dispensary.happiness." + modifier.status().name().toLowerCase(java.util.Locale.ROOT)
                    + (description ? ".description" : "");
            if (description) return fourtwenty$withProfile(Component.translatableEscape(key, args), modifier);
        }
        return Component.translatableEscape(key, args);
    }
    @Inject(method = "updateHappiness", at = @At("RETURN"))
    private static void fourtwenty$pending(ICitizenDataView citizen, AbstractWindowSkeleton window, CallbackInfo ci) {
        // Native rows hide neutral modifiers; always show the cannabis row so type and frequency are visible.
        if (!(citizen.getHappinessHandler().getModifier(DispensaryIds.SATISFACTION) instanceof CannabisHappinessModifier modifier)
                || modifier.getFactor(null) != 1) return;
        String status = "fourtwenty.dispensary.happiness." + modifier.status().name().toLowerCase(java.util.Locale.ROOT);
        int rows = (int)citizen.getHappinessHandler().getModifiers().stream()
                .filter(id -> citizen.getHappinessHandler().getModifier(id).getFactor(null) != 1).count();
        Text label = new Text();
        label.setSize(136, 11); label.setPosition(70, 62 + 12 * rows); label.setColors(0x000000);
        label.setText(Component.translatable(status));
        window.addChild(label);
        PaneBuilders.tooltipBuilder().hoverPane(label)
                .append(fourtwenty$withProfile(Component.translatable(status + ".description"), modifier)).build();
    }

    /** Appends "Smoker · daily" (preference and current frequency) once the citizen has been exposed. */
    private static MutableComponent fourtwenty$withProfile(MutableComponent description, CannabisHappinessModifier modifier) {
        if (modifier.status() == Status.UNEXPOSED) return description;
        return description.append(" ").append(Component.translatable("fourtwenty.dispensary.affinity.profile",
                Component.translatable("fourtwenty.dispensary.affinity.preference." + modifier.preference().name().toLowerCase(java.util.Locale.ROOT)),
                Component.translatable("fourtwenty.dispensary.affinity.step." + modifier.step())));
    }
}
