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
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * The client's copy of the player's Infuser state (sent by the server on login, when using the block, and after every
 * change), the full-screen colour wash when a hit inspires you, and opening the screen.
 */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InfuserClient {
    private InfuserClient() {}

    static InfuserState state = InfuserState.fresh(0);

    /** Whether the local player has caught this colour (a target id: "white", "myr_pin_lim"). */
    public static boolean caught(String id) { return state.caught.containsKey(id); }

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
            if (washColours.isEmpty()) washColours = List.of(0x8CD05A);
            washLevel = Math.max(0, Math.min(5, msg.level()));
            washStart = System.currentTimeMillis();
        };
        InspirationInfuserBlock.clientOpen = () -> Minecraft.getInstance().setScreen(new InfuserScreen());
        NeoForge.EVENT_BUS.addListener(InfuserClient::camera);
        NeoForge.EVENT_BUS.addListener(InfuserClient::fov);
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath("fourtwenty", "inspiration_wash"), InfuserClient::wash);
    }

    /** The drift lasts this long: gentle, never a spin (it replaces vanilla nausea). */
    private static final double DRIFT_MS = 4800;

    /** How strong the drift is now, 0..1: eases in, holds, eases out; 0 when there's none. */
    private static double drift() {
        if (washStart < 0) return 0;
        double t = (System.currentTimeMillis() - washStart) / DRIFT_MS;
        if (t >= 1) return 0;
        double in = Math.min(1, t / 0.15), out = t < 0.55 ? 1 : 1 - (t - 0.55) / 0.45;
        return smooth(in) * smooth(out);
    }

    private static double smooth(double x) { return x * x * (3 - 2 * x); }

    /** A slow sway: the view tilts a couple of degrees one way and the other and drifts a touch, like a deep breath. */
    private static void camera(ViewportEvent.ComputeCameraAngles e) {
        double d = drift();
        if (d <= 0) return;
        double s = System.currentTimeMillis() / 1000.0;
        e.setRoll((float) (e.getRoll() + 2.4 * d * Math.sin(s * 2 * Math.PI / 2.6)));
        e.setYaw((float) (e.getYaw() + 0.9 * d * Math.sin(s * 2 * Math.PI / 3.7)));
        e.setPitch((float) (e.getPitch() + 0.6 * d * Math.sin(s * 2 * Math.PI / 3.1 + 1)));
    }

    /** ... and the world breathes in and out. */
    private static void fov(ViewportEvent.ComputeFov e) {
        double d = drift();
        if (d > 0) e.setFOV(e.getFOV() * (1 + 0.04 * d * Math.sin(System.currentTimeMillis() / 1000.0 * 2 * Math.PI / 2.4)));
    }

    /**
     * The vision: the colours you're inspired by ripple out from the middle of the screen in slow rings and glow in at the
     * corners while the view sways; on a new inspiration, a whispered line.
     */
    private static void wash(GuiGraphics g, DeltaTracker delta) {
        double d = drift();
        if (d <= 0) {
            washStart = -1;
            return;
        }
        double sec = (System.currentTimeMillis() - washStart) / 1000.0;
        int w = g.guiWidth(), h = g.guiHeight(), n = washColours.size();
        double maxR = Math.hypot(w, h) / 2;
        // corner glows, the colours taking turns round the screen
        double[][] corners = {{0, 0}, {w, 0}, {w, h}, {0, h}};
        for (int k = 0; k < 4; k++) {
            int c = washColours.get((k + (int) (sec / 1.2)) % n);
            Draw.glow(g, corners[k][0], corners[k][1], 0, maxR * 0.75, c, 0.22 * d);
        }
        // ripples: each colour sends out three slow rings, staggered
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < 3; k++) {
                double age = sec - i * 0.35 - k * 1.3;
                if (age <= 0) continue;
                double r = age / 2.6 * maxR;
                if (r >= maxR) continue;
                double fade = 1 - r / maxR, width = 6 + r * 0.08;
                Draw.ring(g, w / 2.0, h / 2.0, Math.max(0, r - width), r, Draw.argb(washColours.get(i), 0.28 * d * fade));
            }
        }
        if (washLevel <= 0 || d < 0.12) return;   // text alpha below ~4/255 draws fully opaque
        var font = Minecraft.getInstance().font;
        Component line = Component.literal(LINES[washLevel - 1]).withStyle(s -> s.withItalic(true));
        g.pose().pushPose();
        g.pose().translate(w / 2f, h / 2f - 20, 0);
        g.pose().scale(2, 2, 1);
        g.drawCenteredString(font, line, 0, 0, Draw.argb(0xFFFFFF, d));
        g.pose().popPose();
    }
}
