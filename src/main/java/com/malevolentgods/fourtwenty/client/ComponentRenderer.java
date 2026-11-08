package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.electronics.Shown;
import com.malevolentgods.fourtwenty.electronics.ComponentBlock;
import com.malevolentgods.fourtwenty.electronics.ComponentBlockEntity;
import com.malevolentgods.fourtwenty.electronics.ComponentType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Set;

/**
 * What a component shows of its circuit (docs/electronics/style-pass.md), drawn over its still model: the plinth's light line while it's
 * active, and each part's own sign of life, a Resinator's rod warming with the power it burns, a Kief Cap filling with its charge, rings
 * of light round a Spliff Coil with its current, a hitter's bowl glowing while it conducts. Glows are fullbright. Coordinates are in
 * voxels for a model facing north, turned with the block.
 */
public class ComponentRenderer implements BlockEntityRenderer<ComponentBlockEntity> {
    /** The parts with the new look; the rest keep their plain models for now. */
    static final Set<ComponentType> LAB = EnumSet.allOf(ComponentType.class);
    /** Each switch's moving part, 0 open to 1 closed, as last drawn, and when: it eases toward the switch's state. */
    private static final java.util.Map<ComponentBlockEntity, float[]> MOTION = new java.util.WeakHashMap<>();
    private static final int LIME = 0xB8F06A, LIME_OFF = 0x34401F;

    public ComponentRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(@Nonnull ComponentBlockEntity be, float partialTick, @Nonnull PoseStack pose, @Nonnull MultiBufferSource buffers, int light, int overlay) {
        ComponentType type = be.type();
        if (type == null || !LAB.contains(type)) return;
        int shown = be.shown();
        float time = be.getLevel() == null ? 0 : (be.getLevel().getGameTime() + partialTick) / 20f;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // as ComponentModel turns the still model: to any of the 24 orientations
        pose.mulPose(com.malevolentgods.fourtwenty.electronics.Orientation.rotation(be.getBlockState().getValue(ComponentBlock.FACING),
                be.getBlockState().getValue(ComponentBlock.SPIN)));
        pose.translate(-0.5, -0.5, -0.5);
        // ask for each buffer where it's drawn: a batched source ends one buffer when another is asked for
        TextureAtlasSprite glow = sprite("lab_glow");
        boolean mounted = be.getBlockState().getValue(ComponentBlock.MOUNTED);
        if (mounted && type.part != ComponentType.Part.DISPLAY) {
            mounted(be, type, pose, buffers, light, glow, shown);
            pose.popPose();
            return;
        }
        // a lime ring round each gland while it's active (two-pin parts: front and back)
        if (Shown.active(shown) && type.layout == ComponentType.Layout.TWO_PIN) {
            VertexConsumer add = buffers.getBuffer(RenderType.lightning());
            ring(pose, add, 5.7, 5.7, 0.9, 10.3, 10.3, 1.4, LIME, 0.55f);
            ring(pose, add, 5.7, 5.7, 14.6, 10.3, 10.3, 15.1, LIME, 0.55f);
        }
        int level = Shown.level(shown);
        switch (type) {
            case RESINATOR -> {
                // the rod: dark resin when cold, amber as it warms, white past its rating
                float heat = level / 15f;
                int c = heat <= 0 ? 0x6A4A2A : heat < 2 / 3f ? lerp(0x8A5A26, 0xFFB54A, heat * 1.5f) : lerp(0xFFB54A, 0xFFFFFF, (heat - 2 / 3f) * 3);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 6.5, 6.5, 3.5, 9.5, 9.5, 12.5, c, heat > 0 ? LightTexture.FULL_BRIGHT : light);
            }
            case KIEF_CAP -> {
                if (level > 0) {
                    float f = level / 15f;
                    box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("lab_kief"), 4.8, 4.8, 3.2, 11.2, 4.8 + 6.4 * f, 12.8, lerp(0x8C7A50, 0xFFFFFF, 0.4f + 0.6f * f), LightTexture.FULL_BRIGHT);
                }
            }
            case SPLIFF_COIL -> {
                if (level > 0) {
                    // three rings of light drifting steadily along it, fading in at one end and out at the other, brighter with more current
                    VertexConsumer add = buffers.getBuffer(RenderType.lightning());
                    for (int r = 0; r < 3; r++) {
                        float phase = ((time * 0.5f + r / 3f) % 1f + 1f) % 1f;
                        float z = 3 + phase * 10.4f;
                        float a = (0.12f + 0.03f * level) * (float) Math.sin(Math.PI * phase);
                        ring(pose, add, 5.2, 5.2, z, 10.8, 10.8, z + 0.4, 0x9DE8FF, a);
                    }
                }
            }
            case ONE_HITTER, ZEN_HITTER -> {
                if (level > 0) {
                    float f = level / 15f;
                    int ember = type == ComponentType.ZEN_HITTER ? lerp(0x2E8A52, 0x9DFFC4, f) : lerp(0x8A2A10, 0xFFB060, f);
                    box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 6.45, 10.5, 2.65, 9.55, 10.62, 5.15, ember, LightTexture.FULL_BRIGHT);
                    VertexConsumer add = buffers.getBuffer(RenderType.lightning());
                    ring(pose, add, 7.3, 7.3, 6.7, 8.7, 8.7, 13.2, ember, 0.15f + 0.35f * f);
                }
            }
            case SLOW_BURNER -> {
                // the joint's cherry, at its front end, glows with the current it pushes, breathing slowly
                float f = level / 15f, breath = 0.85f + 0.15f * (float) Math.sin(time * 2.2);
                int cherry = level > 0 ? lerp(0x7A2A10, 0xFF8A3A, f * breath) : 0x3A2A20;
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 6.7, 6.7, 3.0, 9.3, 9.3, 3.6, cherry,
                        level > 0 ? LightTexture.FULL_BRIGHT : light);
            }
            case NUMB_NUG -> digit(be, pose, buffers, light, time);
            case SATIVA_FET, INDICA_FET -> {
                // the resin core glows with the channel current: green for N, purple for P
                float f = level / 15f;
                int on = type == ComponentType.SATIVA_FET ? 0xA8FF6A : 0xD08CFF, off = type == ComponentType.SATIVA_FET ? 0x1E3A18 : 0x2E1A3A;
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 6.6, 6.6, 6.6, 9.4, 9.4, 9.4,
                        level > 0 ? lerp(off, on, 0.35f + 0.65f * f) : off, level > 0 ? LightTexture.FULL_BRIGHT : light);
            }
            case HYPE_AMP -> {
                // a bar of light along the window, as high as its output stands in its supply
                if (level > 0) {
                    float f = level / 15f;
                    box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 4.6, 10.3, 4.6, 5.8, 10.8, 4.6 + 6.8 * f,
                            lerp(0xB8F06A, 0xFFB54A, f), LightTexture.FULL_BRIGHT);
                }
            }
            case TICKER -> {
                // a pendulum swinging on its period; its bob lights on the high half of each beat
                double period = Math.max(0.05, be.value());
                float amp = period < 0.3 ? 6 : 22;
                pose.pushPose();
                // it hangs from the pivot block under the cap (y 13.1 to 13.75), its rod running up into it
                pose.translate(8 / 16f, 13.4 / 16f, 8 / 16f);
                pose.mulPose(Axis.ZP.rotationDegrees(amp * (float) Math.sin(2 * Math.PI * time / period)));
                pose.translate(-8 / 16f, -13.4 / 16f, -8 / 16f);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("infuser/steel_trim"), 7.75, 11.4, 7.75, 8.25, 13.6, 8.25, 0xFFFFFF, light);
                boolean high = Shown.bit(shown, 0);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), high ? glow : sprite("component/lab_brass"),
                        7.1, 10.5, 7.1, 8.9, 11.6, 8.9, high ? LIME : 0xFFFFFF, high ? LightTexture.FULL_BRIGHT : light);
                pose.popPose();
            }
            case TOGGLE_JOINT -> {
                // the lever swings over the pivot along the circuit: toward the front closed, the back open
                float m = motion(be, 9f, 9f);
                pose.pushPose();
                pose.translate(8 / 16f, 11 / 16f, 8 / 16f);
                pose.mulPose(Axis.XP.rotationDegrees(-32 + 64 * (1 - m)));
                pose.translate(-8 / 16f, -11 / 16f, -8 / 16f);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("infuser/steel_trim"), 7.45, 10.2, 7.45, 8.55, 15, 8.55, 0xFFFFFF, light);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("component/lab_brass"), 7, 14.6, 7, 9, 16, 9, 0xFFFFFF, light);
                pose.popPose();
                boolean closed = be.getBlockState().getValue(ComponentBlock.ON);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 9.7, 10.6, 10.2, 10.7, 10.75, 11.2,
                        closed ? LIME : LIME_OFF, closed ? LightTexture.FULL_BRIGHT : light);
            }
            case PUFF_BUTTON -> {
                // the cap sinks fast and rises slower; it glows while held
                float m = motion(be, 22f, 6f);
                double drop = 1.1 * m;
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("infuser/steel_trim"), 7, 10 - drop, 7, 9, 11 - drop, 9, 0xFFFFFF, light);
                boolean held = be.getBlockState().getValue(ComponentBlock.ON);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("component/lab_brass"), 6, 11 - drop, 6, 10, 12.2 - drop, 10,
                        0xFFFFFF, light);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 6.6, 12.2 - drop, 6.6, 9.4, 12.35 - drop, 9.4,
                        held ? LIME : 0x6A7A40, held ? LightTexture.FULL_BRIGHT : light);
            }
            case KILL_SWITCH -> {
                // the armature, hinged at the back post, snaps down onto the contact while closed; the coil warms red
                float m = motion(be, 30f, 14f);
                boolean closed = be.getBlockState().getValue(ComponentBlock.ON);
                pose.pushPose();
                pose.translate(0, 8.25 / 16f, 5.4 / 16f);
                pose.mulPose(Axis.XP.rotationDegrees(-12 * (1 - m)));
                pose.translate(0, -8.25 / 16f, -5.4 / 16f);
                box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("component/lab_brass"), 8.8, 8.0, 5, 10.6, 8.5, 10.8, 0xFFFFFF, light);
                pose.popPose();
                if (closed) ring(pose, buffers.getBuffer(RenderType.lightning()), 5.9, 5.7, 4.9, 8.3, 9.1, 11.1, 0xFF5A3A, 0.25f);
            }
            default -> {
                if (type.chip()) {
                    // nine resin beads under the glass strip, one per output, lit for a 1
                    for (int k = 0; k < 9; k++) {
                        boolean on = Shown.bit(shown, k);
                        double z = 4.25 + k * 0.85;
                        box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 4.1, 10.25, z, 4.9, 10.75, z + 0.6,
                                on ? LIME : 0x2A2238, on ? LightTexture.FULL_BRIGHT : light);
                    }
                } else if (type.logic()) {
                    // the Nug's resin crystal under its dome: lime while its output is 1, turning slowly; dark green while 0
                    boolean high = Shown.bit(shown, 0);
                    pose.pushPose();
                    pose.translate(8 / 16f, 11.9 / 16f, 8 / 16f);
                    if (high) pose.mulPose(Axis.YP.rotationDegrees(time * 30 % 360));
                    float[][] shards = {{0, 0, 0, 2.2f, 0}, {0.9f, 0.6f, 0.3f, 1.3f, 35}, {-0.8f, 0.8f, -0.4f, 1.2f, 70}, {0.2f, 1.3f, 0.7f, 1.0f, 20}};
                    for (float[] sh : shards) {
                        pose.pushPose();
                        pose.translate(sh[0] / 16f, sh[1] / 16f, sh[2] / 16f);
                        pose.mulPose(Axis.YP.rotationDegrees(sh[4]));
                        pose.mulPose(Axis.XP.rotationDegrees(sh[4] * 0.6f));
                        float h = sh[3] / 2;
                        box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, -h, -h, -h, h, h, h,
                                high ? LIME : 0x1C3214, high ? LightTexture.FULL_BRIGHT : light);
                        pose.popPose();
                    }
                    pose.popPose();
                }
            }
        }
        pose.popPose();
    }

    /**
     * A switch mounted on a Rolling Tray, its plate against the tray (z 13 to 16): a Puff Button's cap stands out of its collar and pushes
     * in, glowing while held; a Toggle Joint's lever, on its boss, flips up for on and down for off, a lime lamp beside it while on.
     */
    private static void mounted(ComponentBlockEntity be, ComponentType type, PoseStack pose, MultiBufferSource buffers, int light, TextureAtlasSprite glow, int shown) {
        boolean closed = be.getBlockState().getValue(ComponentBlock.ON);
        // on a board a Dankoder reads, a key's number, one more than it, is in its bits
        int number = Shown.bits(shown) - 1;
        if (type == ComponentType.PUFF_BUTTON) {
            float m = motion(be, 22f, 6f);
            double in = 0.9 * m;
            box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("component/lab_brass"), 5.6, 5.6, 10.4 + in, 10.4, 10.4, 11.95,
                    0xFFFFFF, light);
            int lens = number >= 0 ? closed ? 0x3A5A20 : 0x141A10 : closed ? LIME : 0x6A7A40;
            box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 6.3, 6.3, 10.3 + in, 9.7, 9.7, 10.4 + in,
                    lens, closed || number >= 0 ? LightTexture.FULL_BRIGHT : light);
            if (number >= 0) glyph(pose, buffers, number, 6.6, 6.0, 9.4, 10.0, 10.2 + in, closed ? 0xE8FFB0 : LIME);
            return;
        }
        if (type == ComponentType.TOGGLE_JOINT) {
            float m = motion(be, 9f, 9f);
            pose.pushPose();
            pose.translate(8 / 16f, 8 / 16f, 11.6 / 16f);
            pose.mulPose(Axis.XP.rotationDegrees(-28 + 56 * m));
            pose.translate(-8 / 16f, -8 / 16f, -11.6 / 16f);
            box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("infuser/steel_trim"), 7.45, 7.45, 6.6, 8.55, 8.55, 11.6, 0xFFFFFF, light);
            box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("component/lab_brass"), 7, 7, 5.6, 9, 9, 6.6, 0xFFFFFF, light);
            pose.popPose();
            box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), glow, 11.6, 11.6, 12.85, 12.8, 12.8, 12.95,
                    closed ? LIME : LIME_OFF, closed ? LightTexture.FULL_BRIGHT : light);
            if (number >= 0) glyph(pose, buffers, number, 2.0, 2.0, 4.6, 5.6, 12.85, LIME);
        }
    }

    /** A hex digit in seven segments over the box (left, bottom, right, top) as you face it, at depth {@code z}: a key's label. */
    private static void glyph(PoseStack pose, MultiBufferSource buffers, int number, double x0, double y0, double x1, double y1, double z, int colour) {
        int glyph = GLYPHS[number & 15];
        double w = x1 - x0, h = y1 - y0;
        for (int k = 0; k < 7; k++) {
            if ((glyph >> k & 1) == 0) continue;
            double[] seg = SEGMENTS[k];
            // the digit's 7 by 9 box (4.5 to 11.5 across, 3.5 to 12.5 up), into this one; as you face it, its left is the model's east
            double l = (seg[0] - 4.5) / 7 * w, rt = (seg[2] - 4.5) / 7 * w, b = (seg[1] - 3.5) / 9 * h, t = (seg[3] - 3.5) / 9 * h;
            box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("component/lab_glow"), x1 - rt, y0 + b, z, x1 - l, y0 + t, z + 0.1,
                    colour, LightTexture.FULL_BRIGHT);
        }
    }

    /** Segments a to g for 0-9 and A-F (a top, b top right, c bottom right, d bottom, e bottom left, f top left, g middle). */
    private static final int[] GLYPHS = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F, 0x77, 0x7C, 0x39, 0x5E, 0x79, 0x71};
    /** Each segment as (left, bottom, right, top) in voxels as you face the display: a digit 7 wide and 9 high. */
    private static final double[][] SEGMENTS = {{5.1, 11.4, 10.9, 12.5}, {10.4, 8.3, 11.5, 11.6}, {10.4, 4.4, 11.5, 7.7}, {5.1, 3.5, 10.9, 4.6},
            {4.5, 4.4, 5.6, 7.7}, {4.5, 8.3, 5.6, 11.6}, {5.1, 7.45, 10.9, 8.55}, {12.3, 3.5, 13.2, 4.4}};

    /**
     * A Numb Nug's digit. Numb Nugs side by side, facing the same way, are one display: the first of them that's powered gives the
     * value (its byte) and the mode (its back toggle), the rest need nothing. The value is right-aligned, the most significant digit on
     * the left as you face it; a row too short for it shows its lowest digits and lights the leftmost point. Its back toggle shows the mode.
     */
    private static void digit(ComponentBlockEntity be, PoseStack pose, MultiBufferSource buffers, int light, float time) {
        var level = be.getLevel();
        var state = be.getBlockState();
        net.minecraft.core.Direction facing = state.getValue(ComponentBlock.FACING);
        int spin = state.getValue(ComponentBlock.SPIN);
        net.minecraft.core.Direction left = com.malevolentgods.fourtwenty.electronics.Orientation.toWorld(facing, spin, ComponentType.Face.LEFT);
        // the row, left to right as you face it
        java.util.List<ComponentBlockEntity> row = new java.util.ArrayList<>();
        net.minecraft.core.BlockPos start = be.getBlockPos();
        for (int i = 0; i < 8 && same(level, start.relative(left), state); i++) start = start.relative(left);
        for (int i = 0; i < 9; i++) {
            net.minecraft.core.BlockPos at = start.relative(left.getOpposite(), i);
            if (!same(level, at, state) || !(level.getBlockEntity(at) instanceof ComponentBlockEntity d)) break;
            row.add(d);
        }
        int place = row.indexOf(be), n = row.size();
        // a mounted digit is a plate on its tray: its screen 7 voxels further back, and no toggle on its back (it toggles from the front)
        boolean mounted = state.getValue(ComponentBlock.MOUNTED);
        double dz = mounted ? 7 : 0;
        ComponentBlockEntity source = null;
        // the source: the first that's powered with something plugged into its back (a Ribbon, or a bus face)
        for (ComponentBlockEntity d : row) if (Shown.active(d.shown()) && fed(level, d)) { source = d; break; }
        // the back toggle: up for hex, down for decimal
        boolean hex = be.value() > 0.5;
        if (!mounted) {
            box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("infuser/steel_trim"), 7.6, hex ? 12.2 : 10.9, 12.4, 8.4, hex ? 13.4 : 12.1, 13.4, 0xFFFFFF, light);
        }
        String text = "";
        boolean overflow = false;
        if (source != null) {
            int value = Shown.bits(source.shown()) & 0x1FF;   // the byte and the carry: a sum to 510
            text = source.value() > 0.5 ? Integer.toHexString(value).toUpperCase(java.util.Locale.ROOT) : Integer.toString(value);
            if (text.length() > n) {
                text = text.substring(text.length() - n);
                overflow = true;
            }
        }
        int at = place - (n - text.length());
        int glyph = at >= 0 && at < text.length() ? GLYPHS[Character.digit(text.charAt(at), 16)] : 0;
        boolean point = overflow && place == 0;
        for (int k = 0; k < 8; k++) {
            boolean on = k < 7 ? (glyph >> k & 1) != 0 : point;
            double[] s = SEGMENTS[k];
            // as you face it, its left is the model's east: x runs the other way
            box(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), sprite("component/lab_glow"), 16 - s[2], s[1], 4.7 + dz, 16 - s[0], s[3], 4.8 + dz,
                    on ? LIME : source != null ? 0x1E2A14 : 0x161A12, on ? LightTexture.FULL_BRIGHT : light);
        }
    }

    private static boolean fed(net.minecraft.world.level.Level level, ComponentBlockEntity d) {
        var s = d.getBlockState();
        net.minecraft.core.Direction back = com.malevolentgods.fourtwenty.electronics.Orientation.toWorld(s.getValue(ComponentBlock.FACING), s.getValue(ComponentBlock.SPIN), ComponentType.Face.BACK);
        // a mounted one is fed through its tray: a Ribbon on the tray's back
        net.minecraft.core.BlockPos at = d.getBlockPos().relative(back, s.getValue(ComponentBlock.MOUNTED) ? 2 : 1);
        var behind = level.getBlockState(at);
        return behind.getBlock() instanceof com.malevolentgods.fourtwenty.electronics.RibbonBlock || ComponentBlock.busAt(behind, back.getOpposite()) >= 0;
    }

    private static boolean same(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos at, net.minecraft.world.level.block.state.BlockState like) {
        var s = level.getBlockState(at);
        return s.getBlock() == like.getBlock() && s.getValue(ComponentBlock.FACING) == like.getValue(ComponentBlock.FACING)
                && s.getValue(ComponentBlock.SPIN) == like.getValue(ComponentBlock.SPIN) && s.getValue(ComponentBlock.MOUNTED) == like.getValue(ComponentBlock.MOUNTED);
    }

    /**
     * A switch's moving part, eased toward its state: 1 while the switch is closed (ON), 0 open; {@code down} and {@code up} are how
     * fast it closes and opens, per second.
     */
    private static float motion(ComponentBlockEntity be, float down, float up) {
        float target = be.getBlockState().getValue(ComponentBlock.ON) ? 1 : 0;
        float[] m = MOTION.computeIfAbsent(be, k -> new float[]{target, System.nanoTime()});
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - (long) m[1]) / 1e9f);
        m[1] = now;
        float rate = target > m[0] ? down : up;
        m[0] += (target - m[0]) * Math.min(1, rate * dt);
        return m[0];
    }

    /** A block-atlas sprite of ours: a bare name is under block/component, a path with a folder under block. */
    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(ResourceLocation.fromNamespaceAndPath("fourtwenty", "block/" + (name.contains("/") ? name : "component/" + name)));
    }

    static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    /** A textured box from (x0, y0, z0) to (x1, y1, z1) in voxels, in colour {@code rgb}, at light {@code light}. */
    static void box(PoseStack pose, VertexConsumer vc, TextureAtlasSprite sp, double x0, double y0, double z0, double x1, double y1, double z1, int rgb, int light) {
        float a = (float) x0 / 16, b = (float) y0 / 16, c = (float) z0 / 16, d = (float) x1 / 16, e = (float) y1 / 16, f = (float) z1 / 16;
        var p = pose.last();
        int argb = 0xFF000000 | rgb;
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        quad(p, vc, argb, light, u0, v0, u1, v1, 0, -1, 0, a, b, c, d, b, c, d, b, f, a, b, f);
        quad(p, vc, argb, light, u0, v0, u1, v1, 0, 1, 0, a, e, f, d, e, f, d, e, c, a, e, c);
        quad(p, vc, argb, light, u0, v0, u1, v1, 0, 0, -1, d, b, c, a, b, c, a, e, c, d, e, c);
        quad(p, vc, argb, light, u0, v0, u1, v1, 0, 0, 1, a, b, f, d, b, f, d, e, f, a, e, f);
        quad(p, vc, argb, light, u0, v0, u1, v1, -1, 0, 0, a, b, c, a, b, f, a, e, f, a, e, c);
        quad(p, vc, argb, light, u0, v0, u1, v1, 1, 0, 0, d, b, f, d, b, c, d, e, c, d, e, f);
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, int argb, int light, float u0, float v0, float u1, float v1, float nx, float ny, float nz,
                             float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        vc.addVertex(p, x1, y1, z1).setColor(argb).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x2, y2, z2).setColor(argb).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x3, y3, z3).setColor(argb).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, x4, y4, z4).setColor(argb).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    /** An additive glowing box, untextured (lightning's render type): a film of light. */
    static void ring(PoseStack pose, VertexConsumer vc, double x0, double y0, double z0, double x1, double y1, double z1, int rgb, float alpha) {
        float a = (float) x0 / 16, b = (float) y0 / 16, c = (float) z0 / 16, d = (float) x1 / 16, e = (float) y1 / 16, f = (float) z1 / 16;
        var m = pose.last();
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, bl = (rgb & 255) / 255f;
        float[][] faces = {
                {a, b, c, d, b, c, d, b, f, a, b, f}, {a, e, f, d, e, f, d, e, c, a, e, c},
                {d, b, c, a, b, c, a, e, c, d, e, c}, {a, b, f, d, b, f, d, e, f, a, e, f},
                {a, b, c, a, b, f, a, e, f, a, e, c}, {d, b, f, d, b, c, d, e, c, d, e, f}};
        for (float[] q : faces) {
            for (int k = 0; k < 4; k++) vc.addVertex(m, q[k * 3], q[k * 3 + 1], q[k * 3 + 2]).setColor(r, g, bl, alpha);
        }
    }
}
