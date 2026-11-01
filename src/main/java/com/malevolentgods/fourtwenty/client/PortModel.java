package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.block.WeedBongBlock;
import com.malevolentgods.fourtwenty.power.BIBlock;
import com.malevolentgods.fourtwenty.power.HitBoxBlock;
import com.malevolentgods.fourtwenty.power.TubeBlock;
import com.malevolentgods.fourtwenty.power.WickBlock;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A block on the Wicks, with its own fitting on each side a Wick reaches. Fittings are ordinary models from tools/generate_ports.py, drawn for a block
 * facing north and turned with this one. Nothing is stored: the model looks at the neighbours each time the chunk builds, so no block needs a state for it.
 */
public final class PortModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Integer> MASK = new ModelProperty<>();
    private static final Set<String> BLOCKS = Set.of("weed_bong", "double_hose", "big_bertha", "grand_hookah", "stash_jar", "stash_box", "mother_stash", "the_vault",
            "snip_station", "bud_barber", "trim_reaper", "dry_box", "cure_closet", "the_humidor", "grow_tent", "hydro_bed", "aero_pod", "cosmic_kush_pod", "think_tank",
            "terpinator", "terpinator_lab", "inspiration_infuser", "inspiration_infuser_lab", "hit_box_toke", "hit_box_rip", "hit_box_blaze",
            "pheno_chamber", "pheno_lab", "pheno_cathedral");
    private static final Direction[] AROUND = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    /** The sides each Hookah has a hose on, for one facing north. A hose is its own piece, drawn unless a Wick has that side. */
    private static final Map<String, List<Direction>> HOSES = Map.of("weed_bong", List.of(Direction.EAST), "double_hose", List.of(Direction.EAST, Direction.WEST),
            "big_bertha", List.of(AROUND), "grand_hookah", List.of(AROUND));
    /** Blocks a Transport Tube plugs into. Each has its fittings again in tube parts, as <name>_tube_<side>: bits 6 to 11 of MASK. */
    private static final Set<String> TUBED = Set.of("weed_bong", "double_hose", "big_bertha", "grand_hookah", "snip_station", "bud_barber", "trim_reaper", "dry_box",
            "cure_closet", "the_humidor", "terpinator", "terpinator_lab", "pheno_chamber", "pheno_lab", "pheno_cathedral");
    private static final int TUBE_BITS = 6;
    private static final Map<String, List<BakedQuad>> TURNED = new ConcurrentHashMap<>();
    private static Map<ModelResourceLocation, BakedModel> parts = Map.of();

    private final String name;

    private PortModel(BakedModel model, String name) {
        super(model);
        this.name = name;
    }

    private static ModelResourceLocation part(String name, Direction side, boolean lit) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("fourtwenty", "block/port/" + name + "_" + side.getName() + (lit ? "_lit" : "")));
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        for (String name : BLOCKS) {
            for (Direction side : Direction.values()) {
                event.register(part(name, side, false));
                event.register(part(name, side, true));
            }
        }
        HOSES.forEach((name, sides) -> sides.forEach(side -> event.register(part(name + "_hose", side, false))));
        for (String name : TUBED) for (Direction side : Direction.values()) event.register(part(name + "_tube", side, false));
    }

    private static int turns(BlockState state) {
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return facing.get2DDataValue() == 2 ? 0 : facing.get2DDataValue() == 3 ? 1 : facing.get2DDataValue() == 0 ? 2 : 3;
    }

    /** A world side, as the side of the same block facing north. */
    private static Direction relative(Direction side, int turns) {
        if (side.getAxis().isVertical()) return side;
        for (int i = 0; i < 4; i++) if (AROUND[i] == side) return AROUND[(i - turns + 4) % 4];
        return side;
    }

    /** The world side of a side of a block facing north. */
    private static Direction world(Direction side, int turns) {
        for (int i = 0; i < 4; i++) if (AROUND[i] == side) return AROUND[(i + turns) % 4];
        return side;
    }

    private static boolean lit(BlockState state) {
        if (state.hasProperty(BIBlock.LIT)) return state.getValue(BIBlock.LIT);
        return state.hasProperty(WeedBongBlock.LIT) && state.getValue(WeedBongBlock.LIT);
    }

    @Override public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData data) {
        ModelData d = super.getModelData(level, pos, state, data);
        int mask = 0;
        for (Direction side : Direction.values()) {
            var neighbour = level.getBlockState(pos.relative(side)).getBlock();
            if (neighbour instanceof WickBlock) mask |= 1 << side.ordinal();
            else if (neighbour instanceof TubeBlock) mask |= 1 << TUBE_BITS + side.ordinal();
        }
        return d.derive().with(MASK, mask).build();
    }

    private List<BakedQuad> fitting(BlockState state, Direction relative, int turns, RandomSource rand) {
        return turned(name, relative, lit(state), turns, rand);
    }

    private static List<BakedQuad> turned(String name, Direction relative, boolean lit, int turns, RandomSource rand) {
        return TURNED.computeIfAbsent(name + relative + lit + turns, key -> {
            BakedModel model = parts.get(part(name, relative, lit));
            List<BakedQuad> out = new ArrayList<>();
            if (model == null) return out;
            var turn = new Transformation(null, Axis.YP.rotationDegrees(-90 * turns), null, null).blockCenterToCorner();
            var transform = QuadTransformers.applying(turn);
            out.addAll(transform.process(model.getQuads(null, null, rand, ModelData.EMPTY, null)));
            for (Direction face : Direction.values()) out.addAll(transform.process(model.getQuads(null, face, rand, ModelData.EMPTY, null)));
            return out;
        });
    }

    @Override public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                                       @NotNull ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (side != null || state == null) return quads;
        if (renderType != null && renderType != getRenderTypes(state, rand, data).iterator().next()) return quads;
        List<BakedQuad> out = new ArrayList<>(quads);
        int mask = data.has(MASK) ? data.get(MASK) : 0, turns = turns(state);
        for (Direction world : Direction.values()) {
            if ((mask & 1 << world.ordinal()) != 0) out.addAll(fitting(state, relative(world, turns), turns, rand));
            if (TUBED.contains(name) && (mask & 1 << TUBE_BITS + world.ordinal()) != 0) out.addAll(turned(name + "_tube", relative(world, turns), false, turns, rand));
        }
        for (Direction hose : HOSES.getOrDefault(name, List.of())) {
            int at = world(hose, turns).ordinal();
            if ((mask & (1 << at | 1 << TUBE_BITS + at)) == 0) out.addAll(turned(name + "_hose", hose, false, turns, rand));
        }
        return out;
    }

    /** Wraps every block a Wick can reach. Goes after anything else that wraps these models. */
    public static void wrap(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        parts = models;
        TURNED.clear();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("fourtwenty")) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                String name = id.getPath().equals("hit_box") ? "hit_box_" + state.getValue(HitBoxBlock.TIER).getSerializedName() : id.getPath();
                if (!BLOCKS.contains(name)) continue;
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(key);
                if (model != null) models.put(key, new PortModel(model, name));
            }
        }
    }
}
