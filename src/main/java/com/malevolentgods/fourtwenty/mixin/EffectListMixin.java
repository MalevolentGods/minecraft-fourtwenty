package com.malevolentgods.fourtwenty.mixin;

import com.malevolentgods.fourtwenty.client.SmokeBadge;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

/** Keeps smoke effects out of the inventory's list: they're all in the one badge (client/SmokeBadge). If this ever fails to apply they just show in the list too. */
@Mixin(EffectRenderingInventoryScreen.class)
abstract class EffectListMixin {
    @Redirect(method = "renderEffects", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> fourtwenty$withoutSmoke(LocalPlayer player) {
        return SmokeBadge.others(player);
    }
}
