package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.electronics.Shown;
import com.malevolentgods.fourtwenty.electronics.RibbonBlock;
import com.malevolentgods.fourtwenty.electronics.RibbonBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import javax.annotation.Nonnull;

/**
 * An Eighth Ribbon's wires glowing while their bits are 1 (what it shows: RibbonBlockEntity): a film of light along each lit wire, in its
 * colour, on every arm and through a straight, T or cross core. Wire k lies at x (or z) 4 + k, as the Ribbon's stripes do.
 */
public class RibbonRenderer implements BlockEntityRenderer<RibbonBlockEntity> {
    /** The resistor colour code, lightened to glow; the carry grey. */
    private static final int[] GLOW = {0x9A9A9A, 0xC88A5A, 0xFF6A6A, 0xFFB46A, 0xFFF07A, 0x8AF08A, 0x7AA6FF, 0xC69AFF, 0xD8D8D8};

    public RibbonRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(@Nonnull RibbonBlockEntity be, float partialTick, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        int bits = Shown.bits(be.shown());
        if (bits == 0) return;
        var state = be.getBlockState();
        boolean n = on(state, Direction.NORTH), s = on(state, Direction.SOUTH), e = on(state, Direction.EAST), w = on(state, Direction.WEST);
        boolean ns = n || s, ew = e || w;
        VertexConsumer add = buffers.getBuffer(RenderType.lightning());
        for (int k = 0; k < 9; k++) {
            if ((bits >> k & 1) == 0) continue;
            int c = GLOW[k];
            double a = 4 + k + 0.15, b = 5 + k - 0.15;
            if (n) ComponentRenderer.ring(pose, add, a, 6.95, 0, b, 9.05, 3, c, 0.5f);
            if (s) ComponentRenderer.ring(pose, add, a, 6.95, 14, b, 9.05, 16, c, 0.5f);
            if (e) ComponentRenderer.ring(pose, add, 14, 6.95, a, 16, 9.05, b, c, 0.5f);
            if (w) ComponentRenderer.ring(pose, add, 0, 6.95, a, 3, 9.05, b, c, 0.5f);
            if (on(state, Direction.UP)) ComponentRenderer.ring(pose, add, a, 9, 6.95, b, 16, 9.05, c, 0.5f);
            if (on(state, Direction.DOWN)) ComponentRenderer.ring(pose, add, a, 0, 6.95, b, 7, 9.05, c, 0.5f);
            // the core: along whichever run goes straight through (corners fold, and keep their own look)
            if (n && s || ns && !ew || !ns && !ew) ComponentRenderer.ring(pose, add, a, 6.95, 3, b, 9.05, 14, c, 0.5f);
            else if (e && w || ew && !ns) ComponentRenderer.ring(pose, add, 3, 6.95, a, 14, 9.05, b, c, 0.5f);
        }
    }

    private static boolean on(net.minecraft.world.level.block.state.BlockState state, Direction d) {
        return state.getValue(RibbonBlock.SIDES.get(d));
    }
}
