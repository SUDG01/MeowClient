package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.RenderState;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/** A fading ribbon sampled at game ticks rather than render frames. */
public class BreadcrumbsMod extends Mod {
    public NumberValue maxPoints = new NumberValue("Length", 100, 10, 400, 5);
    public NumberValue fadeTime = new NumberValue("FadeTime", 4000, 1000, 15000, 500);
    public NumberValue width = new NumberValue("Width", 0.3, 0.05, 0.8, 0.05);
    public BooleanValue rainbow = new BooleanValue("Rainbow", true);
    public ColorValue trailColor = new ColorValue("Color", new Color(255, 183, 178))
            .setVisibility(() -> !rainbow.getValue());

    private final LinkedList<TrailPoint> trail = new LinkedList<>();
    private final LongSupplier clock;
    private World world;
    private EntityPlayerSP player;
    private int lastTick = -1;

    public BreadcrumbsMod() { this(System::currentTimeMillis); }

    public BreadcrumbsMod(LongSupplier clock) {
        super("Breadcrumbs", Category.RENDER);
        this.clock = Objects.requireNonNull(clock, "clock");
        addValues(maxPoints, fadeTime, width, rainbow, trailColor);
    }

    @Override public void enable() { clear(); }
    @Override public void disable() { clear(); }

    private void clear() {
        trail.clear();
        world = null;
        player = null;
        lastTick = -1;
    }

    private boolean syncWorld() {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
            clear();
            return false;
        }
        if (world != mc.theWorld || player != mc.thePlayer) {
            clear(); world = mc.theWorld; player = mc.thePlayer;
        }
        return true;
    }

    @Override
    public void update() {
        if (!syncWorld()) return;
        long now = clock.getAsLong();
        prune(now);
        if (lastTick == player.ticksExisted) return;
        lastTick = player.ticksExisted;
        TrailPoint previous = trail.peekLast();
        if (previous != null) {
            double dx = player.posX - previous.x;
            double dy = player.posY - previous.y;
            double dz = player.posZ - previous.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq < 0.0004) return;
            // A respawn or teleport must not draw a ribbon through the map.
            if (distanceSq > 64) trail.clear();
        }
        trail.addLast(new TrailPoint(player.posX, player.posY, player.posZ, now));
        prune(now);
    }

    private void prune(long now) {
        long maxAge = fadeTime.getValue().longValue();
        while (!trail.isEmpty() && (trail.size() > maxPoints.getValue().intValue()
                || now - trail.peekFirst().startTime >= maxAge)) trail.removeFirst();
    }

    @Override
    public void render(float partialTicks) {
        if (!syncWorld()) return;
        long now = clock.getAsLong();
        prune(now);
        if (trail.size() < 2) return;
        List<TrailPoint> points = new ArrayList<>(trail);
        // Only the leading endpoint is interpolated; frames never consume trail length.
        points.set(points.size() - 1, new TrailPoint(
                player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks,
                player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks,
                player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks,
                points.get(points.size() - 1).startTime));
        RenderManager rm = mc.getRenderManager();
        try (RenderState state = RenderState.capture()) {
            RenderState.setupWorldEffect();
            drawRibbon(points, rm, now, width.getValue() * 0.8, 0.18f);
            drawRibbon(points, rm, now, width.getValue() * 0.5, 0.85f);
            GL11.glLineWidth(2.5f);
            WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
            wr.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < points.size(); i++) vertex(wr, points.get(i), rm, now, i, 0, 0, 1f);
            Tessellator.getInstance().draw();
        }
    }

    private void drawRibbon(List<TrailPoint> points, RenderManager rm, long now, double halfWidth, float opacity) {
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < points.size(); i++) {
            TrailPoint before = points.get(Math.max(0, i - 1));
            TrailPoint after = points.get(Math.min(points.size() - 1, i + 1));
            double dx = after.x - before.x, dz = after.z - before.z;
            double length = Math.sqrt(dx * dx + dz * dz);
            double ox = length < 0.001 ? halfWidth : -dz / length * halfWidth;
            double oz = length < 0.001 ? 0 : dx / length * halfWidth;
            vertex(wr, points.get(i), rm, now, i, ox, oz, opacity);
            vertex(wr, points.get(i), rm, now, i, -ox, -oz, opacity);
        }
        Tessellator.getInstance().draw();
    }

    private void vertex(WorldRenderer wr, TrailPoint point, RenderManager rm, long now,
                        int index, double ox, double oz, float opacity) {
        Color base = trailColor.getColor();
        Color color = rainbow.getValue()
                ? Color.getHSBColor(((now % 6000) / 6000f + index * 0.015f) % 1, 0.55f, 1f) : base;
        float fade = Math.max(0, 1f - (now - point.startTime) / fadeTime.getValue().floatValue());
        wr.pos(point.x + ox - rm.renderPosX, point.y + 0.06 - rm.renderPosY, point.z + oz - rm.renderPosZ)
                .color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
                        fade * opacity * base.getAlpha() / 255f).endVertex();
    }

    private static final class TrailPoint {
        final double x, y, z;
        final long startTime;
        TrailPoint(double x, double y, double z, long startTime) {
            this.x = x; this.y = y; this.z = z; this.startTime = startTime;
        }
    }
}
