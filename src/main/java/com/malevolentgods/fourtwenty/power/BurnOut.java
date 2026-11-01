package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/** Wire carrying too much goes up in smoke and leaves ash. Only wire breaks: anything else overloads, smoking green until the power's gone. */
public final class BurnOut {
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.35f, 1f, 0.2f), 1.2f);

    private BurnOut() {}

    public static void burn(Level level, BlockPos pos) {
        level.playSound(null, pos, ModPower.BURN_OUT.get(), SoundSource.BLOCKS, 1, 0.9f + level.random.nextFloat() * 0.2f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.02);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        Block.popResource(level, pos, new ItemStack(ModPower.ASH.get()));
    }

    /** One moment of an overloaded block: green sparks, smoke, a sizzle. */
    public static void overloadFx(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 1, z = pos.getZ() + 0.5;
        server.sendParticles(GREEN, x, y, z, 6, 0.3, 0.1, 0.3, 0);
        server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 2, 0.2, 0, 0.2, 0.02);
        level.playSound(null, pos, ModPower.OVERLOAD.get(), SoundSource.BLOCKS, 0.5f, 0.9f + level.random.nextFloat() * 0.2f);
    }
}
