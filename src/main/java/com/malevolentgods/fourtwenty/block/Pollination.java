package com.malevolentgods.fourtwenty.block;

import com.malevolentgods.fourtwenty.domain.Climate;
import com.malevolentgods.fourtwenty.domain.Crosses;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.power.GrowOpBlockEntity;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Double Crosses and up are made by pollinating, not in a Pheno Chamber. Grown side by side, Double Cross parents sometimes cross on their own, which shows at
 * harvest. A Buzz Bot does it for certain, and Ultras and Golden God only happen that way. Either way the hybrid needs a plot good enough to grow it. Landrace
 * has no partners, but sometimes picks up the climate it grew in instead.
 */
public final class Pollination {
    /** How far up and down a Buzz Bot looks: a farm is flat, and its area is wide. */
    private static final int HEIGHT = 6;

    private Pollination() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Pollination::onDrops);
    }

    private static boolean ripe(BlockState state) {
        return state.getBlock() instanceof WeedCropBlock crop && crop.isMaxAge(state);
    }

    /** What pollen from {@code from} would make at {@code to}, if {@code route} is how that hybrid gets made and the plot at {@code to} can grow it. */
    private static Optional<Strain> child(ServerLevel level, BlockPos from, BlockPos to) {
        BlockState a = level.getBlockState(from), b = level.getBlockState(to);
        if (!ripe(a) || !ripe(b)) return Optional.empty();
        return Crosses.hybrid(a.getValue(WeedCropBlock.STRAIN), b.getValue(WeedCropBlock.STRAIN))
                .filter(strain -> Crosses.route(strain) != Crosses.Route.CHAMBER && WeedCropBlock.plotted(level, to, strain));
    }

    /** Whether a Buzz Bot can carry pollen from one crop to the other. */
    public static boolean canPollinate(ServerLevel level, BlockPos from, BlockPos to) {
        return level.getBlockEntity(to) instanceof WeedCropBlockEntity crop && crop.pollen() == null && child(level, from, to).isPresent();
    }

    /** A Buzz Bot's work: the next harvest of {@code to} gives the hybrid. */
    public static boolean pollinate(ServerLevel level, BlockPos from, BlockPos to) {
        if (!canPollinate(level, from, to)) return false;
        Strain strain = child(level, from, to).orElseThrow();
        var crop = (WeedCropBlockEntity) level.getBlockEntity(to);
        crop.pollinate(new WeedCropBlockEntity.Pollen(strain, Traits.cross(WeedCropBlockEntity.at(level, from), crop.traits(), level.random::nextDouble)));
        level.sendParticles(ParticleTypes.FALLING_NECTAR, to.getX() + 0.5, to.getY() + 0.7, to.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0);
        return true;
    }

    private static ItemStack seed(Strain strain, Traits traits) {
        return ModDataComponents.withTraits(ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get()), strain), traits);
    }

    /** What pollinating adds to the harvest of the crop at {@code pos}: the Bot's hybrid if it left one, or sometimes a natural cross with a neighbour. */
    public static @Nullable ItemStack harvest(BlockState state, ServerLevel level, BlockPos pos, @Nullable WeedCropBlockEntity crop) {
        if (!ripe(state)) return null;
        if (crop != null && crop.pollen() != null) {
            ItemStack out = seed(crop.pollen().strain(), crop.pollen().traits());
            crop.pollinate(null);
            return out;
        }
        if (state.getValue(WeedCropBlock.STRAIN) == Strain.LANDRACE) return touched(level, pos, crop);
        Traits traits = crop == null ? Traits.DEFAULT : crop.traits();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = pos.relative(side);
            Optional<Strain> child = child(level, beside, pos).filter(strain -> Crosses.route(strain) == Crosses.Route.POLLEN);
            if (child.isPresent() && level.random.nextDouble() < ProcessingConfig.pollenChance()) {
                return seed(child.get(), Traits.cross(WeedCropBlockEntity.at(level, beside), traits, level.random::nextDouble));
            }
        }
        return null;
    }

    /** The climate a crop grows in: its Cosmic Kush Pod's copy, or the biome's. */
    public static @Nullable Climate climate(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos.below()) instanceof GrowOpBlockEntity plot && plot.climate() != null) return plot.climate();
        var biome = level.getBiome(pos);
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.IS_BADLANDS)) return Climate.DESERT;
        if (biome.is(Tags.Biomes.IS_SNOWY)) return Climate.SNOW;
        if (biome.is(Tags.Biomes.IS_SWAMP)) return Climate.SWAMP;
        return biome.is(BiomeTags.IS_JUNGLE) ? Climate.JUNGLE : null;
    }

    /** Landrace sometimes picks up the climate it grew in, half as often from a Pod's copy. */
    private static @Nullable ItemStack touched(ServerLevel level, BlockPos pos, @Nullable WeedCropBlockEntity crop) {
        Climate climate = climate(level, pos);
        if (climate == null) return null;
        boolean copied = level.getBlockEntity(pos.below()) instanceof GrowOpBlockEntity plot && plot.climate() != null;
        if (level.random.nextDouble() >= ProcessingConfig.climateChance() * (copied ? Climate.COPIED : 1)) return null;
        ItemStack seed = seed(Strain.LANDRACE, crop == null ? Traits.DEFAULT : crop.traits());
        seed.set(ModDataComponents.CLIMATE.get(), climate);
        return seed;
    }

    private static void onDrops(BlockDropsEvent event) {
        BlockPos pos = event.getPos();
        ItemStack seed = harvest(event.getState(), event.getLevel(), pos, event.getBlockEntity() instanceof WeedCropBlockEntity crop ? crop : null);
        if (seed != null) event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, seed));
    }

    /** Every weed plant in reach of {@code around}, ripe or not. */
    public static List<BlockPos> plants(ServerLevel level, BlockPos around, int reach) {
        List<BlockPos> plants = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-reach, -HEIGHT, -reach), around.offset(reach, HEIGHT, reach))) {
            if (level.getBlockState(pos).getBlock() instanceof WeedCropBlock && pos.closerThan(around, reach)) plants.add(pos.immutable());
        }
        return plants;
    }

    /** Two ripe crops out of {@code plants} that can cross: the source, then the target. */
    public static @Nullable BlockPos[] findPair(ServerLevel level, List<BlockPos> plants) {
        List<BlockPos> ripe = new ArrayList<>();
        for (BlockPos pos : plants) if (ripe(level.getBlockState(pos))) ripe.add(pos);
        for (BlockPos from : ripe) {
            for (BlockPos to : ripe) {
                if (!from.equals(to) && canPollinate(level, from, to)) return new BlockPos[]{from, to};
            }
        }
        return null;
    }
}
