package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.Units;
import com.malevolentgods.fourtwenty.power.BIBlock;
import com.malevolentgods.fourtwenty.power.SupplyBlock;
import com.malevolentgods.fourtwenty.power.SupplyBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import javax.annotation.Nonnull;

/** The Bench Rig's readouts: the voltage it's set to and its current limit, in glowing amber on the screen in its front panel. */
public class BenchRigRenderer implements BlockEntityRenderer<SupplyBlockEntity> {
    private static final int AMBER = 0xFFFFB54A, DIM = 0xFFB07A2A;
    private final Font font;

    public BenchRigRenderer(BlockEntityRendererProvider.Context context) { this.font = context.getFont(); }

    @Override public void render(@Nonnull SupplyBlockEntity be, float partialTick, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        if (!(be.getBlockState().getBlock() instanceof SupplyBlock block) || !block.rig()) return;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        // as the blockstate turns the model: north 0, east 90, south 180, west 270
        pose.mulPose(Axis.YP.rotationDegrees(-(be.getBlockState().getValue(BIBlock.FACING).toYRot() + 180)));
        pose.translate(-0.5, 0, -0.5);
        // the screen faces north (out of the model's front); text reads left to right as you face it
        pose.translate(13.1 / 16, 12.35 / 16, 1.25 / 16);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        float s = 1.25f / 16 / 9;
        pose.scale(s, -s, s);
        line(pose, buffers, Units.format(be.volts(), Units.VOLTS), 0, AMBER);
        line(pose, buffers, "max " + Units.format(be.amps(), Units.AMPS), 11, DIM);
        pose.popPose();
    }

    private void line(PoseStack pose, MultiBufferSource buffers, String text, float y, int colour) {
        font.drawInBatch(text, 0, y, colour, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
    }
}
