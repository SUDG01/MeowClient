package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.impl.JumpEvent;
import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.RenderState;
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
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Expanding annuli at the takeoff position of actual player jumps. */
public class JumpEffectMod extends Mod {
    public NumberValue ringCount = new NumberValue("Rings", 3, 1, 8, 1);
    public NumberValue duration = new NumberValue("Duration", 1000, 200, 3000, 100);
    public ColorValue ringColor = new ColorValue("Color", new Color(255, 183, 178));
    private final List<JumpRing> rings = new ArrayList<>();
    private final LongSupplier clock;
    private World world;
    private EntityPlayerSP player;

    public JumpEffectMod() { this(System::currentTimeMillis); }

    public JumpEffectMod(LongSupplier clock) {
        super("JumpEffect", Category.RENDER);
        this.clock = Objects.requireNonNull(clock, "clock");
        addValues(ringCount, duration, ringColor);
    }

    @Override public void enable() { clear(); }
    @Override public void disable() { clear(); }

    private void clear() { rings.clear(); world = null; player = null; }

    private boolean syncWorld() {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) { clear(); return false; }
        if (world != mc.theWorld || player != mc.thePlayer) {
            clear(); world = mc.theWorld; player = mc.thePlayer;
        }
        return true;
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (!syncWorld() || event.getPlayer() != player) return;
        long now = clock.getAsLong();
        for (int i = 0; i < ringCount.getValue().intValue(); i++) {
            rings.add(new JumpRing(event.getX(), event.getY() + 0.035 + i * 0.008, event.getZ(), now + i * 80L));
        }
        while (rings.size() > 128) rings.remove(0);
    }

    @Override
    public void render(float partialTicks) {
        if (!syncWorld()) return;
        long now = clock.getAsLong();
        long maxAge = duration.getValue().longValue();
        rings.removeIf(ring -> now - ring.startTime >= maxAge);
        if (rings.isEmpty()) return;
        RenderManager rm = mc.getRenderManager();
        Color color = ringColor.getColor();
        try (RenderState state = RenderState.capture()) {
            RenderState.setupWorldEffect();
            for (JumpRing ring : rings) {
                if (now < ring.startTime) continue;
                float progress = (now - ring.startTime) / (float) maxAge;
                float radius = 0.2f + (1 - (1 - progress) * (1 - progress)) * 2.3f;
                float alpha = (1 - progress) * color.getAlpha() / 255f;
                drawRing(ring, rm, color, radius, 0.18f, alpha * 0.16f);
                drawRing(ring, rm, color, radius, 0.065f, alpha);
            }
        }
    }

    private void drawRing(JumpRing ring, RenderManager rm, Color color, float radius, float thickness, float alpha) {
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 64; i++) {
            double angle = Math.PI * 2 * i / 64;
            for (int edge = -1; edge <= 1; edge += 2) {
                double r = Math.max(0, radius + thickness * edge);
                wr.pos(ring.x - rm.renderPosX + Math.cos(angle) * r, ring.y - rm.renderPosY,
                                ring.z - rm.renderPosZ + Math.sin(angle) * r)
                        .color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha).endVertex();
            }
        }
        Tessellator.getInstance().draw();
    }

    private static final class JumpRing {
        final double x, y, z;
        final long startTime;
        JumpRing(double x, double y, double z, long startTime) {
            this.x = x; this.y = y; this.z = z; this.startTime = startTime;
        }
    }
}
