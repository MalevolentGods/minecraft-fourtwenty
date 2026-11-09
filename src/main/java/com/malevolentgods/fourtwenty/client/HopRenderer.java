package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.electronics.HopBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;

import javax.annotation.Nonnull;

/**
 * Nah, Skip Me, Bro, drawn: its first wire straight through, in its own model, exactly as that wire looks; its second the same, raised a
 * third of a block off the first, with a riser at each end down to where its neighbours meet it: a little bridge.
 */
public class HopRenderer implements BlockEntityRenderer<HopBlockEntity> {
    private static final double RAISE = 5;

    public HopRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(@Nonnull HopBlockEntity be, float partialTick, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        var blocks = Minecraft.getInstance().getBlockRenderer();
        HopBlockEntity.Wire under = be.wire(0), over = be.wire(1);
        if (under == null) {
            under = over;
            over = null;
        }
        if (under == null) return;
        blocks.renderSingleBlock(shaped(be, under), pose, buffers, light, overlay);
        if (over == null) return;
        // the axis neither runs along: the way the second hops
        Direction.Axis lift = Direction.Axis.Y;
        for (Direction.Axis a : Direction.Axis.values()) if (a != under.axis() && a != over.axis()) lift = a;
        pose.pushPose();
        pose.translate(lift == Direction.Axis.X ? RAISE / 16 : 0, lift == Direction.Axis.Y ? RAISE / 16 : 0, lift == Direction.Axis.Z ? RAISE / 16 : 0);
        blocks.renderSingleBlock(shaped(be, over), pose, buffers, light, overlay);
        pose.popPose();
        // the risers: at each end, from the wire's own level up to the raised one, in its texture and colour
        var state = over.state();
        var sprite = blocks.getBlockModel(state).getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
        int colour = Minecraft.getInstance().getBlockColors().getColor(state, null, null, 0);
        int rgb = colour == -1 ? 0xFFFFFF : colour & 0xFFFFFF;
        boolean ribbon = over.ribbon();
        double lo = ribbon ? 7 : 6, hi = (ribbon ? 9 : 10) + RAISE, w0 = ribbon ? 3 : 6, w1 = ribbon ? 14 : 10;
        Direction.Axis across = Direction.Axis.Y;
        for (Direction.Axis a : Direction.Axis.values()) if (a != over.axis() && a != lift) across = a;
        for (double[] end : new double[][]{{0, 2}, {14, 16}}) {
            double[] min = new double[3], max = new double[3];
            min[over.axis().ordinal()] = end[0];
            max[over.axis().ordinal()] = end[1];
            min[lift.ordinal()] = lo;
            max[lift.ordinal()] = hi;
            min[across.ordinal()] = w0;
            max[across.ordinal()] = w1;
            ComponentRenderer.box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite, min[0], min[1], min[2], max[0], max[1], max[2],
                    rgb, light);
        }
    }

    /**
     * A wire as drawn: an upright Ribbon turned as the Ribbon above or below it is (RibbonBlock's VAXIS), so its stripes run on in the
     * same order instead of reversing.
     */
    private static net.minecraft.world.level.block.state.BlockState shaped(HopBlockEntity be, HopBlockEntity.Wire wire) {
        var state = wire.state();
        if (!wire.ribbon() || wire.axis() != Direction.Axis.Y || be.getLevel() == null) return state;
        for (Direction d : new Direction[]{Direction.DOWN, Direction.UP}) {
            var next = be.getLevel().getBlockState(be.getBlockPos().relative(d));
            if (next.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.RibbonBlock && next.hasProperty(com.malevolentgods.fourtwenty.electronics.RibbonBlock.VAXIS)) {
                return state.setValue(com.malevolentgods.fourtwenty.electronics.RibbonBlock.VAXIS, next.getValue(com.malevolentgods.fourtwenty.electronics.RibbonBlock.VAXIS));
            }
        }
        return state;
    }
}
