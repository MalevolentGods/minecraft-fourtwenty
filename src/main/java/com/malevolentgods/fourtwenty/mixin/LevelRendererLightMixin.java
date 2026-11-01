package com.malevolentgods.fourtwenty.mixin;

import com.malevolentgods.fourtwenty.client.DynamicLights;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Raises the light blocks are drawn with near a moving light. If this ever fails to apply, moving lights just don't light anything. */
@Mixin(LevelRenderer.class)
abstract class LevelRendererLightMixin {
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static void fourtwenty$movingLight(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> out) {
        int packed = out.getReturnValueI();
        int moving = DynamicLights.at(pos);
        if (moving > LightTexture.block(packed)) out.setReturnValue(LightTexture.pack(moving, LightTexture.sky(packed)));
    }
}
