package com.malevolentgods.fourtwenty.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "fourtwenty")
public class DebugWeedCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("debugweed")
            .requires(source -> source.hasPermission(2))
            .executes(DebugWeedCommand::placeWeedPatch)
        );
        
        dispatcher.register(Commands.literal("testweed")
            .requires(source -> source.hasPermission(2))
            .executes(DebugWeedCommand::placeWeedBlock)
        );
    }
    
    private static int placeWeedPatch(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Command must be run in a server world"));
            return 0;
        }
        
        BlockPos pos = BlockPos.containing(source.getPosition());
        
        try {
            // First, let's try to place the block directly to test if it works
            var registry = level.registryAccess().registryOrThrow(Registries.BLOCK);
            var weedBlock = registry.get(ResourceLocation.fromNamespaceAndPath("fourtwenty", "wild_weed"));
            
            if (weedBlock == null) {
                source.sendFailure(Component.literal("Wild weed block not found in registry!"));
                return 0;
            }
            
            // Test placing the block directly
            BlockPos testPos = pos.below();
            var defaultState = weedBlock.defaultBlockState();
            if (defaultState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7)) {
                defaultState = defaultState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7, 7);
            }
            
            level.setBlock(testPos, defaultState, 3);
            source.sendSuccess(() -> Component.literal("Placed weed crop directly at " + testPos), true);
            
            // Now try the configured feature
            ResourceLocation featureId = ResourceLocation.fromNamespaceAndPath("fourtwenty", "wild_weed_patch");
            var featureRegistry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
            
            var featureOpt = featureRegistry.getOptional(featureId);
            if (featureOpt.isEmpty()) {
                source.sendFailure(Component.literal("Configured feature 'wild_weed_patch' not found in registry!"));
                return 0;
            }
            
            var feature = featureOpt.get();
            boolean success = feature.place(level, level.getChunkSource().getGenerator(), 
                level.getRandom(), pos.above());
            
            if (success) {
                source.sendSuccess(() -> Component.literal("Placed wild weed patch feature at " + pos.above()), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("Feature placement returned false - check placement conditions"));
                return 0;
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    private static int placeWeedBlock(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getLevel() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("Command must be run in a server world"));
            return 0;
        }
        
        BlockPos pos = BlockPos.containing(source.getPosition());
        
        try {
            var registry = level.registryAccess().registryOrThrow(Registries.BLOCK);
            var weedBlock = registry.get(ResourceLocation.fromNamespaceAndPath("fourtwenty", "wild_weed"));
            
            if (weedBlock == null) {
                source.sendFailure(Component.literal("Wild weed block not found in registry!"));
                return 0;
            }
            
            // Place weed at different ages around the player
            for (int i = 0; i < 8; i++) {
                BlockPos testPos = pos.offset(i % 3 - 1, 0, (i / 3) - 1);
                var blockState = weedBlock.defaultBlockState();
                
                // Set age if the block has an age property
                if (blockState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7)) {
                    blockState = blockState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7, i);
                }
                
                level.setBlock(testPos, blockState, 3);
            }
            
            source.sendSuccess(() -> Component.literal("Placed test weed crops around " + pos), true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
