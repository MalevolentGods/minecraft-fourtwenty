package com.malevolentgods.fourtwenty.client.infuser;

import com.malevolentgods.fourtwenty.domain.Tint;
import com.malevolentgods.fourtwenty.infuser.Infuser;
import com.malevolentgods.fourtwenty.infuser.InfuserState;
import com.malevolentgods.fourtwenty.infuser.InspirationInfuserBlock;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * The client's copy of the player's Infuser state (sent by the server on login, when using the block, and after every
 * change), the full-screen colour wash when a hit inspires you, and opening the screen.
 */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InfuserClient {
    private InfuserClient() {}

    static InfuserState state = InfuserState.fresh(0);

    private static List<Integer> washColours = List.of();
    private static long washStart = -1;
    private static int washLevel;
    private static final String[] LINES = {"…something like this.", "…oh. That colour.", "…you can almost taste it.", "…closer now.", "…you see it perfectly."};

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        Infuser.clientSync = tag -> {
            state = InfuserState.load(tag, Tint.get().resins().size());
            com.malevolentgods.fourtwenty.terpene.TerpeneKnowledge.caught(state.caught.keySet());
            if (Minecraft.getInstance().screen instanceof InfuserScreen s) s.synced();
        };
        Infuser.clientInspired = msg -> {
            Tint tint = Tint.get();
            washColours = msg.ids().stream().map(id -> tint.target(id) == null ? 0xFFFFFF : tint.target(id).rgb()).toList();
            washLevel = Math.max(1, Math.min(5, msg.level()));
            washStart = System.currentTimeMillis();
        };
        InspirationInfuserBlock.clientOpen = () -> Minecraft.getInstance().setScreen(new InfuserScreen());
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath("fourtwenty", "inspiration_wash"), InfuserClient::wash);
    }

    /** The vision: the inspired colour floods the screen, then fades, with a whispered line. */
    private static void wash(GuiGraphics g, DeltaTracker delta) {
        if (washStart < 0 || washColours.isEmpty()) return;
        double t = (System.currentTimeMillis() - washStart) / 1900.0;
        if (t >= 1) { washStart = -1; return; }
        double a = (t < 0.25 ? t / 0.25 : 1 - (t - 0.25) / 0.75) * 0.6;
        int w = g.guiWidth(), h = g.guiHeight(), n = washColours.size();
        for (int i = 0; i < n; i++) {
            int x0 = w * i / n, x1 = w * (i + 1) / n;
            g.fill(x0, 0, x1, h, Draw.argb(washColours.get(i), a));
        }
        if (a < 0.08) return;   // text alpha below ~4/255 draws fully opaque
        var font = Minecraft.getInstance().font;
        Component line = Component.literal(LINES[washLevel - 1]).withStyle(s -> s.withItalic(true));
        g.pose().pushPose();
        g.pose().translate(w / 2f, h / 2f - 20, 0);
        g.pose().scale(2, 2, 1);
        g.drawCenteredString(font, line, 0, 0, Draw.argb(0xFFFFFF, Math.min(1, a / 0.6 * 1.2)));
        g.pose().popPose();
    }
}
