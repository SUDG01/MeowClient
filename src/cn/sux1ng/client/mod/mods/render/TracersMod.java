package cn.sux1ng.client.mod.mods.render;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.util.RenderState;
import cn.sux1ng.client.targeting.TargetRules;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.ColorValue;
import cn.sux1ng.client.value.NumberValue;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** Lines from the projected crosshair to selected entities, in any camera view. */
public class TracersMod extends Mod {
    public NumberValue range = new NumberValue("Range", 50, 10, 200, 5);
    public NumberValue lineWidth = new NumberValue("LineWidth", 2.5, 1, 6, 0.5);
    public BooleanValue players = new BooleanValue("Players", true).setVisibility(() -> !TargetRules.isUnified());
    public BooleanValue mobs = new BooleanValue("Mobs", false).setVisibility(() -> !TargetRules.isUnified());
    public BooleanValue animals = new BooleanValue("Animals", false).setVisibility(() -> !TargetRules.isUnified());
    public ColorValue lineColor = new ColorValue("Color", new Color(255, 183, 178));

    private final FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final FloatBuffer start = BufferUtils.createFloatBuffer(3);

    public TracersMod() {
        super("Tracers", Category.RENDER);
        addValues(range, lineWidth, players, mobs, animals, lineColor);
    }

    @Override
    public void render(float partialTicks) {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        // A point just in front of the near plane projects onto the crosshair. Starting
        // at the camera itself projects the entire line onto the target as one pixel.
        if (!GLU.gluUnProject(viewport.get(0) + viewport.get(2) / 2f,
                viewport.get(1) + viewport.get(3) / 2f, 0.1f, modelview, projection, viewport, start)) return;

        RenderManager rm = mc.getRenderManager();
        double rangeSq = range.getValue() * range.getValue();
        Color color = lineColor.getColor();
        try (RenderState state = RenderState.capture()) {
            RenderState.setupWorldEffect();
            GL11.glLineWidth(lineWidth.getValue().floatValue());
            WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
            wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity == mc.thePlayer || entity == mc.getRenderViewEntity() || entity.isDead
                        || !isValidTarget(entity) || entity.getDistanceSqToEntity(mc.thePlayer) > rangeSq) continue;
                double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.renderPosX;
                double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.renderPosY + entity.height / 2;
                double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.renderPosZ;
                vertex(wr, start.get(0), start.get(1), start.get(2), color, 0.95f);
                vertex(wr, x, y, z, color, 0.8f);
            }
            Tessellator.getInstance().draw();
        }
    }

    private void vertex(WorldRenderer wr, double x, double y, double z, Color color, float alpha) {
        wr.pos(x, y, z).color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
                alpha * color.getAlpha() / 255f).endVertex();
    }

    private boolean isValidTarget(Entity entity) {
        return TargetRules.canRender(entity, players.getValue(), mobs.getValue(), animals.getValue(), true);
    }
}
