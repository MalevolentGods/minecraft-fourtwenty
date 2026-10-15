package com.malevolentgods.fourtwenty.colonytest;

import com.ldtteam.blockui.controls.Text;
import com.malevolentgods.fourtwenty.compat.minecolonies.benefits.CannabisHappinessModifier;
import com.malevolentgods.fourtwenty.compat.minecolonies.domain.CitizenDemand.Status;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.CitizenWindowUtils;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Explicit test-only client probe; exercises real BlockUI construction and translated native happiness rows. */
@EventBusSubscriber(modid = "fourtwenty_colonytests", value = Dist.CLIENT)
public final class BenefitsClientProbe {
    private static boolean ran;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (ran || !Boolean.getBoolean("fourtwenty.benefitClientProbe") || mc.player == null || mc.getConnection() == null || mc.getOverlay() != null) return;
        ran = true;
        try {
            for (var status : Status.values()) {
                var handler = new CitizenHappinessHandler();
                handler.addModifier(new CannabisHappinessModifier(status, status == Status.SATISFIED || status == Status.UNMET ? .1 : 0));
                var citizen = (ICitizenDataView)java.lang.reflect.Proxy.newProxyInstance(BenefitsClientProbe.class.getClassLoader(),
                        new Class[]{ICitizenDataView.class}, (proxy, method, args) -> method.getName().equals("getHappinessHandler") ? handler : null);
                var window = new AbstractWindowSkeleton(ResourceLocation.parse("minecolonies:gui/citizen/happiness.xml")) {};
                CitizenWindowUtils.updateHappiness(citizen, window);
                var labels = window.getChildren().stream().filter(Text.class::isInstance).map(Text.class::cast).map(Text::getTextAsString).toList();
                String expected = switch (status) {
                    case SATISFIED -> "Cannabis: satisfied";
                    case UNMET -> "Cannabis: unmet demand";
                    case GRACE -> "Cannabis: pending";
                    default -> "";
                };
                if (!expected.isEmpty() && !labels.contains(expected)) throw new AssertionError("Missing translated native row: " + expected + " in " + labels);
                com.mojang.logging.LogUtils.getLogger().info("Cannabis native client row {} verified: {}", status, labels);
            }
            com.mojang.logging.LogUtils.getLogger().info("FOURTWENTY_BENEFITS_CLIENT_PROBE_PASSED");
        } catch (Throwable failure) {
            com.mojang.logging.LogUtils.getLogger().error("FOURTWENTY_BENEFITS_CLIENT_PROBE_FAILED", failure);
        }
        mc.stop();
    }
}
