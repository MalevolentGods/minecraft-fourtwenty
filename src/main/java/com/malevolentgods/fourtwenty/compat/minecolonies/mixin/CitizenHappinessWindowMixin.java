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
            key = "fourtwenty.dispensary.happiness." + modifier.status().name().toLowerCase(java.util.Locale.ROOT)
                    + (key.contains(".desc.") ? ".description" : "");
        }
        return Component.translatableEscape(key, args);
    }
    @Inject(method = "updateHappiness", at = @At("RETURN"))
    private static void fourtwenty$pending(ICitizenDataView citizen, AbstractWindowSkeleton window, CallbackInfo ci) {
        if (!(citizen.getHappinessHandler().getModifier(DispensaryIds.SATISFACTION) instanceof CannabisHappinessModifier modifier)
                || modifier.status() != Status.GRACE) return;
        int rows = (int)citizen.getHappinessHandler().getModifiers().stream()
                .filter(id -> citizen.getHappinessHandler().getModifier(id).getFactor(null) != 1).count();
        Text label = new Text();
        label.setSize(136, 11); label.setPosition(70, 62 + 12 * rows); label.setColors(0x000000);
        label.setText(Component.translatable("fourtwenty.dispensary.happiness.grace"));
        window.addChild(label);
        PaneBuilders.tooltipBuilder().hoverPane(label)
                .append(Component.translatable("fourtwenty.dispensary.happiness.grace.description")).build();
    }
}
