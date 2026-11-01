package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** An overloaded part goes up in a cloud of smoke and leaves ash. Nothing explodes, and nothing else is hurt. */
public final class BurnOut {
    private BurnOut() {}

    public static void burn(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 0.8f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.02);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        Block.popResource(level, pos, new ItemStack(ModPower.ASH.get()));
    }
}
