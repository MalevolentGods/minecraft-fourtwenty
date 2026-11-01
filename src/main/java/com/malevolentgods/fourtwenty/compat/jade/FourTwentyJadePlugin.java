package com.malevolentgods.fourtwenty.compat.jade;

import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.domain.Strain;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** A planted crop names its strain, in the strain's colour. Jade finds this itself; nothing else touches this class, so the mod runs without Jade. */
@WailaPlugin
public class FourTwentyJadePlugin implements IWailaPlugin {
    private static final ResourceLocation CROP_STRAIN = ResourceLocation.fromNamespaceAndPath("fourtwenty", "crop_strain");

    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CropStrain.INSTANCE, WeedCropBlock.class);
    }

    private enum CropStrain implements IBlockComponentProvider {
        INSTANCE;

        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Strain strain = accessor.getBlockState().getValue(WeedCropBlock.STRAIN);
            tooltip.add(Component.literal(strain.displayName()).withColor(strain.color()));
        }

        @Override public ResourceLocation getUid() { return CROP_STRAIN; }
    }
}
