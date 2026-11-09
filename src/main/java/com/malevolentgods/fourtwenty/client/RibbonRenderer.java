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
        var state = be.getBlockState();
        // a dyed Ribbon: its colour along both jacket edges, so runs of different colours (which don't join) read apart
        if (state.hasProperty(com.malevolentgods.fourtwenty.power.WickBlock.COLOUR)) {
            var dye = state.getValue(com.malevolentgods.fourtwenty.power.WickBlock.COLOUR).dye();
            if (dye != null) edges(pose, buffers, state, dye.getTextureDiffuseColor() & 0xFFFFFF, light);
        }
        int bits = Shown.bits(be.shown());
        // a twist's glow would run straight through its turn: it stays dark
        if (bits == 0 || state.getValue(RibbonBlock.TWIST)) return;
        boolean n = on(state, Direction.NORTH), s = on(state, Direction.SOUTH), e = on(state, Direction.EAST), w = on(state, Direction.WEST);
        boolean ns = n || s, ew = e || w, up = on(state, Direction.UP), down = on(state, Direction.DOWN);
        boolean zWide = state.getValue(RibbonBlock.VAXIS) == Direction.Axis.Z;
        int flat = (n ? 1 : 0) + (s ? 1 : 0) + (e ? 1 : 0) + (w ? 1 : 0);
        VertexConsumer add = buffers.getBuffer(RenderType.lightning());
        for (int k = 0; k < 9; k++) {
            if ((bits >> k & 1) == 0) continue;
            int c = GLOW[k];
            double a = 4 + k + 0.15, b = 5 + k - 0.15;
            if (n) ComponentRenderer.ring(pose, add, a, 6.95, 0, b, 9.05, 3, c, 0.5f);
            if (s) ComponentRenderer.ring(pose, add, a, 6.95, 14, b, 9.05, 16, c, 0.5f);
            if (e) ComponentRenderer.ring(pose, add, 14, 6.95, a, 16, 9.05, b, c, 0.5f);
            if (w) ComponentRenderer.ring(pose, add, 0, 6.95, a, 3, 9.05, b, c, 0.5f);
            // upright arms and core, turned as the run is (wire k along x, or along z)
            if (up) upright(pose, add, zWide, a, b, 9, 16, c);
            if (down) upright(pose, add, zWide, a, b, 0, 7, c);
            if (flat == 0 && (up || down)) {
                upright(pose, add, zWide, a, b, 7, 9, c);
                continue;
            }
            // a bend from flat to upright: only from its arm to the middle
            if (flat == 1 && (up || down)) {
                if (n) ComponentRenderer.ring(pose, add, a, 6.95, 3, b, 9.05, 9, c, 0.5f);
                if (s) ComponentRenderer.ring(pose, add, a, 6.95, 7, b, 9.05, 14, c, 0.5f);
                if (e) ComponentRenderer.ring(pose, add, 7, 6.95, a, 14, 9.05, b, c, 0.5f);
                if (w) ComponentRenderer.ring(pose, add, 3, 6.95, a, 9, 9.05, b, c, 0.5f);
                continue;
            }
            // the core: along whichever run goes straight through (flat corners fold, and keep their own look)
            if (n && s || ns && !ew || !ns && !ew) ComponentRenderer.ring(pose, add, a, 6.95, 3, b, 9.05, 14, c, 0.5f);
            else if (e && w || ew && !ns) ComponentRenderer.ring(pose, add, 3, 6.95, a, 14, 9.05, b, c, 0.5f);
        }
    }

    /** Thin coloured strips along the jacket's two edges (pixels 3 and 13), over every arm and through the core, top and bottom. */
    private static void edges(PoseStack pose, MultiBufferSource buffers, net.minecraft.world.level.block.state.BlockState state, int rgb, int light) {
        var sprite = net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("fourtwenty", "block/component/lab_glow"));
        boolean n = on(state, Direction.NORTH), s = on(state, Direction.SOUTH), e = on(state, Direction.EAST), w = on(state, Direction.WEST);
        boolean up = on(state, Direction.UP), down = on(state, Direction.DOWN);
        boolean zWide = state.getValue(RibbonBlock.VAXIS) == Direction.Axis.Z;
        java.util.function.Consumer<double[]> strip = q -> ComponentRenderer.box(pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)), sprite, q[0], q[1], q[2], q[3], q[4], q[5], rgb, light);
        // each set of strips a little proud of the jacket and of the others (north-south, east-west and upright each their own height), so
        // no two faces share a plane where they cross: coplanar faces flicker as they fight
        for (double[] edge : new double[][]{{2.88, 3.9}, {13.1, 14.12}}) {
            double a = edge[0], b = edge[1];
            // along z (north-south arms, and the core when they run through)
            double z0 = n ? 0.01 : 2.9, z1 = s ? 15.99 : 14.1;
            if (n || s || !e && !w && !up && !down) strip.accept(new double[]{a, 6.9, z0, b, 9.1, z1});
            double x0 = w ? 0.02 : 2.92, x1 = e ? 15.98 : 14.08;
            if (e || w) strip.accept(new double[]{x0, 6.86, a + 0.14, x1, 9.14, b - 0.14});
            // upright: the run stood on edge, its edges up its sides
            double y0 = down ? 0.03 : 6.8, y1 = up ? 15.97 : 9.2;
            if ((up || down) && !state.getValue(RibbonBlock.TWIST)) {
                if (zWide) strip.accept(new double[]{6.82, y0, a + 0.24, 9.18, y1, b - 0.24});
                else strip.accept(new double[]{a + 0.24, y0, 6.82, b - 0.24, y1, 9.18});
            }
        }
    }

    private static void upright(PoseStack pose, VertexConsumer add, boolean zWide, double a, double b, double y0, double y1, int c) {
        if (zWide) ComponentRenderer.ring(pose, add, 6.95, y0, a, 9.05, y1, b, c, 0.5f);
        else ComponentRenderer.ring(pose, add, a, y0, 6.95, b, y1, 9.05, c, 0.5f);
    }

    private static boolean on(net.minecraft.world.level.block.state.BlockState state, Direction d) {
        return state.getValue(RibbonBlock.SIDES.get(d));
    }
}
