package com.malevolentgods.fourtwenty.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "fourtwenty");

    @SuppressWarnings("null")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.malevolentgods.fourtwenty.processing.CuringRackBlockEntity>> CURING_RACK =
        BLOCK_ENTITIES.register("curing_rack", () -> BlockEntityType.Builder.of(com.malevolentgods.fourtwenty.processing.CuringRackBlockEntity::new, ModBlocks.CURING_RACK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeedBongBlockEntity>> WEED_BONG = 
        BLOCK_ENTITIES.register("weed_bong", () -> 
            BlockEntityType.Builder.of(WeedBongBlockEntity::new, ModBlocks.WEED_BONG.get(), ModPower.DOUBLE_HOSE.get(), ModPower.BIG_BERTHA.get())
                .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.malevolentgods.fourtwenty.blockentity.HotboxBlockEntity>> HOTBOX =
        BLOCK_ENTITIES.register("hotbox", () -> BlockEntityType.Builder.of(com.malevolentgods.fourtwenty.blockentity.HotboxBlockEntity::new,
                ModBlocks.HOTBOX.get()).build(null));

    /** Every lamp, inverted lamp and lamp trim (for the client-side halo). */
    @SuppressWarnings("null")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.malevolentgods.fourtwenty.blockentity.LampBlockEntity>> LAMP =
        BLOCK_ENTITIES.register("lamp", () -> BlockEntityType.Builder.of(com.malevolentgods.fourtwenty.blockentity.LampBlockEntity::new,
                java.util.stream.Stream.concat(java.util.stream.Stream.of(ModBlocks.LAMPS, ModBlocks.INVERTED_LAMPS, ModBlocks.LAMP_TRIMS)
                    .flatMap(map -> map.values().stream()).map(holder -> (net.minecraft.world.level.block.Block) holder.get()),
                    ModTerpenes.paletteLamps())
                    .toArray(net.minecraft.world.level.block.Block[]::new))
            .build(null));
}
