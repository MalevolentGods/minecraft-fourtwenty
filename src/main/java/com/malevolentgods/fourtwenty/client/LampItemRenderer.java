package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.LampBlock;
import com.malevolentgods.fourtwenty.block.LampTrimBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nonnull;

/**
 * Lit lamp items (inverted lamps and lamp trims) as ProjectRed draws its illumar lamps in the
 * inventory and hands: the block at full brightness with its halo around it.
 */
public class LampItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        private LampItemRenderer renderer;

        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) renderer = new LampItemRenderer();
            return renderer;
        }
    };

    private LampItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext context, @Nonnull PoseStack pose,
                             @Nonnull MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem item)) return;
        var block = item.getBlock();
        var state = block.defaultBlockState();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
        AABB box;
        int rgba;
        if (block instanceof LampBlock lamp && state.getValue(LampBlock.LIT)) {
            box = new AABB(0, 0, 0, 1, 1, 1);
            rgba = lamp.itemHalo();
        } else if (block instanceof LampTrimBlock trim) {
            box = new AABB(0, 0, 0, 1, 2 / 16.0, 1);   // the default (floor) plate
            rgba = trim.itemHalo();
        } else return;
        LampHaloRenderer.halo(pose, buffers, box, rgba);
    }
}
