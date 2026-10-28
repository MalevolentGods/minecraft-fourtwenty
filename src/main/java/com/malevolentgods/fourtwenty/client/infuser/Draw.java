package com.malevolentgods.fourtwenty.client.infuser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Smooth shapes for the Infuser screen: discs, rings and arcs as triangle fans and strips, so they're crisp at any GUI scale. */
final class Draw {
    private Draw() {}

    interface Shade { int argb(double t); }

    private static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void v(BufferBuilder b, Matrix4f m, double x, double y, int argb) {
        b.addVertex(m, (float) x, (float) y, 0).setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
    }

    static int argb(int rgb, double alpha) {
        return ((int) Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    static void disc(GuiGraphics g, double cx, double cy, double r, int argb) {
        begin();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        v(b, m, cx, cy, argb);
        int n = Math.max(24, (int) (r * 1.5));
        for (int i = n; i >= 0; i--) { double a = 2 * Math.PI * i / n; v(b, m, cx + Math.cos(a) * r, cy + Math.sin(a) * r, argb); }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** A ring between r0 and r1 over the angle a0..a1, coloured along its length. */
    static void arc(GuiGraphics g, double cx, double cy, double r0, double r1, double a0, double a1, Shade shade) {
        begin();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int n = Math.max(12, (int) (Math.abs(a1 - a0) * r1));
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n, a = a0 + (a1 - a0) * t;
            int c = shade.argb(t);
            v(b, m, cx + Math.cos(a) * r1, cy + Math.sin(a) * r1, c);
            v(b, m, cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, c);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    static void ring(GuiGraphics g, double cx, double cy, double r0, double r1, int argb) {
        arc(g, cx, cy, r0, r1, 0, 2 * Math.PI, t -> argb);
    }

    /** A soft radial glow: solid at the centre, fading to nothing at r. */
    static void glow(GuiGraphics g, double cx, double cy, double r0, double r, int rgb, double alpha) {
        begin();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int n = 64;
        for (int i = 0; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            v(b, m, cx + Math.cos(a) * r, cy + Math.sin(a) * r, argb(rgb, 0));
            v(b, m, cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, argb(rgb, alpha));
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** A filled rectangle with rounded corners. */
    static void round(GuiGraphics g, double x0, double y0, double x1, double y1, double r, int argb) {
        r = Math.max(0, Math.min(r, Math.min(x1 - x0, y1 - y0) / 2));
        begin();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        v(b, m, (x0 + x1) / 2, (y0 + y1) / 2, argb);
        // corner centres and their arcs, going round anticlockwise on screen (y down): top-right, top-left, bottom-left, bottom-right
        double[][] corners = {{x1 - r, y0 + r, 0}, {x0 + r, y0 + r, -Math.PI / 2}, {x0 + r, y1 - r, -Math.PI}, {x1 - r, y1 - r, -3 * Math.PI / 2}};
        for (double[] c : corners) for (int i = 0; i <= 6; i++) {
            double a = c[2] - (Math.PI / 2) * i / 6;
            v(b, m, c[0] + Math.cos(a) * r, c[1] + Math.sin(a) * r, argb);
        }
        v(b, m, x1, y0 + r, argb);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    static void outline(GuiGraphics g, double x0, double y0, double x1, double y1, double r, double w, int argb) {
        round(g, x0, y0, x1, y1, r, argb);
    }

    static int hsv(double h, double s, double v) {
        return java.awt.Color.HSBtoRGB((float) (((h % 1) + 1) % 1), (float) s, (float) v) & 0xFFFFFF;
    }
}
