package com.malevolentgods.fourtwenty.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The joint in first person. Held, it sits in the hand like any item; smoking, it rises to your lips: the filter end at
 * the bottom middle of the view (your mouth), the lit end angled away, a slow bob as you draw on it, and back down
 * when you stop. The model's own first-person transforms are left at identity (weed_joint.json): all of it is here.
 */
public final class JointHand {
    private JointHand() {}

    /** Ticks to raise it to your mouth. */
    private static final float RAISE = 6f;

    /** {x, y, z, rotX, rotY, rotZ, scale}, for the right hand (mirrored for the left). */
    private static final float[] HELD = {0.63f, -0.32f, -0.65f, 0f, -90f, 25f, 0.68f};
    private static final float[] SMOKING = {0.22f, -0.2f, -0.62f, 0f, 20f, 8f, 0.85f};

    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
                                               float equipProcess, float swingProcess) {
            float lift = 0, bob = 0;
            if (player.isUsingItem() && player.getUseItem() == stack) {
                float used = stack.getUseDuration(player) - player.getUseItemRemainingTicks() + partialTick;
                float k = Math.min(1f, used / RAISE);
                lift = k * k * (3 - 2 * k);
                bob = (float) Math.sin(used * 0.25) * 0.012f * lift;
            }
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            float[] p = new float[7];
            for (int i = 0; i < 7; i++) p[i] = HELD[i] + (SMOKING[i] - HELD[i]) * lift;
            pose.translate(side * p[0], p[1] + bob - equipProcess * 0.6f, p[2]);
            pose.mulPose(Axis.XP.rotationDegrees(p[3]));
            pose.mulPose(Axis.YP.rotationDegrees(side * p[4]));
            pose.mulPose(Axis.ZP.rotationDegrees(side * p[5]));
            pose.scale(p[6], p[6], p[6]);
            return true;
        }
    };
}
