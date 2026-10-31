package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.processing.CuringRackBlock;
import com.malevolentgods.fourtwenty.processing.CuringRackBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/** The buds on a Curing Rack: two rails of four, hanging under the twine, each turned a little so a full rack doesn't look stamped. */
public class CuringRackRenderer implements BlockEntityRenderer<CuringRackBlockEntity> {
    private static final float[] X = {0.22f, 0.41f, 0.59f, 0.78f};
    private static final float[] Y = {0.73f, 0.27f};
    private static final float SIZE = 0.3f;

    public CuringRackRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(@Nonnull CuringRackBlockEntity rack, float partial, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        float turn = rack.getBlockState().getValue(CuringRackBlock.FACING).toYRot();
        for (int i = 0; i < CuringRackBlockEntity.SLOTS; i++) {
            ItemStack bud = rack.buds().get(i);
            if (bud.isEmpty()) continue;
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-turn));
            pose.translate(X[i % 4] - 0.5, Y[i / 4], 0);
            pose.mulPose(Axis.YP.rotationDegrees(i % 2 == 0 ? 12 : -12));
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.scale(SIZE, SIZE, SIZE);
            Minecraft.getInstance().getItemRenderer().renderStatic(bud, ItemDisplayContext.FIXED, light, overlay, pose, buffers, rack.getLevel(), i);
            pose.popPose();
        }
    }
}
