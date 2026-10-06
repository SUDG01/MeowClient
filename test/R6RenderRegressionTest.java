import cn.sux1ng.client.events.EventManager;
import cn.sux1ng.client.mod.mods.render.BreadcrumbsMod;
import cn.sux1ng.client.mod.mods.render.ESPMod;
import cn.sux1ng.client.mod.mods.render.HitParticlesMod;
import cn.sux1ng.client.mod.mods.render.JumpEffectMod;
import cn.sux1ng.client.mod.mods.render.TracersMod;
import cn.sux1ng.client.mod.mods.render.DamageParticlesMod;
import cn.sux1ng.client.mod.mods.render.FullBrightMod;
import cn.sux1ng.client.mod.mods.render.NameTagMod;
import cn.sux1ng.client.mod.mods.render.TrajectoriesMod;
import cn.sux1ng.client.mod.mods.render.TNTTimerMod;
import cn.sux1ng.client.mod.mods.render.DamageIndicatorMod;
import cn.sux1ng.client.gui.ClientSettingsScreen;
import cn.sux1ng.client.gui.HudEditorScreen;
import cn.sux1ng.client.gui.KeyBindingsScreen;
import cn.sux1ng.client.ui.HudLayout;
import cn.sux1ng.client.ui.ClientLanguage;
import cn.sux1ng.client.util.ProjectilePrediction;
import cn.sux1ng.client.MeowClient;
import cn.sux1ng.client.mod.ModManager;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.mod.mods.misc.TargetMod;
import cn.sux1ng.client.mod.mods.misc.BlatantMod;
import cn.sux1ng.client.mod.mods.combat.KillAuraMod;
import cn.sux1ng.client.mod.mods.render.TargetHUDMod;
import cn.sux1ng.client.events.impl.MotionEvent;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.Packet;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.potion.Potion;
import net.minecraft.stats.StatBase;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.lang.reflect.Method;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Exercises the real module render methods in an offscreen LWJGL context. */
public class R6RenderRegressionTest {
    private static final int SIZE = 640;
    private static final Unsafe UNSAFE = unsafe();
    private static FixtureMinecraft mc;
    private static FixturePlayer player;
    private static FixtureWorld world;
    private static RenderManager manager;
    private static RecordingUploader uploader;
    private static int failures;
    private static String screenshots;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) screenshots = args[0];
        Pbuffer buffer = new Pbuffer(SIZE, SIZE, new PixelFormat().withDepthBits(24), null, null);
        try {
            buffer.makeCurrent();
            setupFixture();
            System.out.println("OpenGL: " + GL11.glGetString(GL11.GL_RENDERER));
            check("trail retains a walking route at 240 FPS", () -> {
                AtomicLong clock = new AtomicLong(1000);
                BreadcrumbsMod mod = new BreadcrumbsMod(clock::get);
                mod.maxPoints.setValue(100.0);
                mod.fadeTime.setValue(10000.0);
                double minX = Double.POSITIVE_INFINITY;
                for (int tick = 0; tick < 40; tick++) {
                    player.lastTickPosX = tick * 0.2;
                    player.posX = (tick + 1) * 0.2;
                    player.ticksExisted = tick;
                    mod.update();
                    clock.addAndGet(50);
                    int frames = 12;
                    for (int frame = 0; frame < frames; frame++) {
                        camera(0, 4, -5, 0, 0, 0);
                        uploader.positions.clear();
                        mod.render(frame / (float) frames);
                    }
                }
                for (double[] position : uploader.positions) minX = Math.min(minX, position[0]);
                double retained = player.posX - minX;
                require(retained >= 6.0, "retained only " + retained + " blocks of an 8-block route");
            });
            check("jump ring is visible from above with world culling enabled", () -> {
                player.posX = player.lastTickPosX = 0;
                player.onGround = true;
                AtomicLong clock = new AtomicLong(1000);
                JumpEffectMod mod = new JumpEffectMod(clock::get);
                camera(0, 4, -5, 0, 0, 0);
                mod.render(1);
                EventManager.register(mod);
                try { player.jump(); } finally { EventManager.unregister(mod); }
                player.onGround = false;
                List<?> rings = (List<?>) field(mod, "rings");
                require(!rings.isEmpty(), "no jump rings created");
                clock.addAndGet(300);
                camera(0, 4, -5, 0, 0, 0);
                mod.render(1);
                int pixels = coloredPixels();
                require(pixels > 30, "jump effect drew " + pixels + " pixels");
                saveFrame("jump-rings");
            });
            check("real attack generates the configured hit particles once", () -> {
                HitParticlesMod mod = new HitParticlesMod();
                Entity target = target(0, 0, 3);
                player.swingProgress = 1f / 6f;
                mc.objectMouseOver = null;
                world.particles = 0;
                EventManager.register(mod);
                try {
                    mc.playerController.attackEntity(player, target);
                    for (int i = 0; i < 12; i++) mod.render(i / 12f);
                    require(world.particles == mod.count.getValue().intValue(),
                            "an attack generated " + world.particles + " particles");
                } finally {
                    EventManager.unregister(mod);
                }
            });
            check("tracer runs from the crosshair to a visible target", () -> {
                player.posX = player.lastTickPosX = 0;
                world.loadedEntityList.clear();
                world.loadedEntityList.add(target(3, 0, 10));
                camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1);
                TracersMod mod = new TracersMod();
                mod.render(1);
                int pixels = coloredPixels();
                require(pixels >= 40, "tracer drew " + pixels + " pixels");
                saveFrame("tracer");
            });
            check("2D ESP preserves the world projection for following modules", () -> {
                world.loadedEntityList.clear();
                world.loadedEntityList.add(target(2, 0, 10));
                camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1);
                float[] before = matrix(GL11.GL_PROJECTION_MATRIX);
                ESPMod mod = new ESPMod();
                mod.showArmor.setValue(false);
                mod.render(1);
                require(Arrays.equals(before, matrix(GL11.GL_PROJECTION_MATRIX)),
                        "2D ESP replaced the perspective projection with the GUI projection");
                require(coloredPixels() > 20, "2D ESP did not draw a visible box");
                saveFrame("esp");
            });
            check("damage numbers follow attacks and observed health", () -> {
                DamageParticlesMod mod = new DamageParticlesMod();
                EntityLivingBase target = (EntityLivingBase) target(2, 0, 3);
                FixtureFont font = (FixtureFont) mc.fontRendererObj;
                font.drawn.clear();
                mc.objectMouseOver = null;
                player.swingProgress = 1f / 6f;
                EventManager.register(mod);
                try {
                    mc.playerController.attackEntity(player, target);
                    mod.render(1);
                    require(font.drawn.size() == 1 && font.drawn.get(0).startsWith("~"),
                            "an attack did not display a labelled damage estimate: " + font.drawn);
                    target.getDataWatcher().updateObject(6, 18f);
                    mod.update();
                    font.drawn.clear();
                    mod.render(1);
                    require(font.drawn.equals(Arrays.asList("2.0")), "observed damage was not displayed: " + font.drawn);
                } finally { EventManager.unregister(mod); }
            });
            check("FullBright restores brightness after duplicate activation", () -> {
                mc.gameSettings.gammaSetting = 0.7f;
                FullBrightMod mod = new FullBrightMod();
                mod.setEnable(true);
                mod.setEnable(true);
                mod.setEnable(false);
                require(mc.gameSettings.gammaSetting == 0.7f, "original brightness was overwritten");
            });
            check("all hit particle modes honor Count with reduced vanilla particles", () -> {
                HitParticlesMod mod = new HitParticlesMod();
                mod.count.setValue(5.0);
                EnumParticleTypes[] expected = {EnumParticleTypes.HEART, EnumParticleTypes.FLAME,
                        EnumParticleTypes.CRIT, EnumParticleTypes.SLIME, EnumParticleTypes.PORTAL, EnumParticleTypes.SMOKE_LARGE};
                mc.gameSettings.particleSetting = 2;
                EventManager.register(mod);
                try {
                    for (int i = 0; i < mod.particleMode.getModes().length; i++) {
                        mod.particleMode.setValue(mod.particleMode.getModes()[i]);
                        world.particles = 0;
                        world.forced = false;
                        mc.playerController.attackEntity(player, target(0, 0, 3));
                        for (int frame = 0; frame < 12; frame++) mod.render(1);
                        require(world.particles == 5 && world.type == expected[i] && world.forced,
                                "mode " + mod.particleMode.getValue() + " spawned incorrectly");
                    }
                    setField(mc.playerController, "currentGameType", net.minecraft.world.WorldSettings.GameType.SPECTATOR);
                    world.particles = 0;
                    mc.playerController.attackEntity(player, target(0, 0, 3));
                    require(world.particles == 0, "spectator interaction was treated as an attack");
                } finally {
                    setField(mc.playerController, "currentGameType", net.minecraft.world.WorldSettings.GameType.SURVIVAL);
                    EventManager.unregister(mod);
                }
            });
            check("trail is wide, fades while stationary and clears on disable", () -> {
                AtomicLong clock = new AtomicLong(1000);
                BreadcrumbsMod mod = new BreadcrumbsMod(clock::get);
                for (int i = 0; i < 12; i++) {
                    player.posX = player.lastTickPosX = (i - 6) * 0.3;
                    player.posZ = player.lastTickPosZ = 0;
                    player.ticksExisted++;
                    mod.update();
                    clock.addAndGet(50);
                }
                camera(0, 4, -5, 0, 0, 0);
                mod.render(1);
                require(coloredPixels() > 200, "default trail is still a thin line");
                saveFrame("trail");
                int samples = ((List<?>) field(mod, "trail")).size();
                for (int i = 0; i < 100; i++) {
                    player.ticksExisted++;
                    mod.update();
                    mod.render(1);
                }
                require(((List<?>) field(mod, "trail")).size() == samples, "standing still consumed the trail");
                clock.addAndGet(15000);
                camera(0, 4, -5, 0, 0, 0);
                mod.render(1);
                require(coloredPixels() == 0, "expired trail is still visible");
                mod.disable();
                require(((List<?>) field(mod, "trail")).isEmpty(), "disabled trail retained old samples");
            });
            check("effects clear on a new world and falling does not create jump rings", () -> {
                AtomicLong clock = new AtomicLong(1000);
                JumpEffectMod jump = new JumpEffectMod(clock::get);
                EventManager.register(jump);
                try {
                    player.onGround = true;
                    jump.render(1);
                    player.onGround = false;
                    player.motionY = -0.2;
                    jump.render(1);
                    require(((List<?>) field(jump, "rings")).isEmpty(), "walking off an edge triggered a jump effect");
                    player.jump();
                    clock.addAndGet(300);
                    camera(0, 4, -5, 0, 0, 0);
                    jump.render(1);
                    require(coloredPixels() > 30, "jump effect was not visible before world change");
                    mc.theWorld = allocate(FixtureWorld.class);
                    camera(0, 4, -5, 0, 0, 0);
                    jump.render(1);
                    require(coloredPixels() == 0, "old jump effect leaked into a new world");
                } finally { mc.theWorld = world; EventManager.unregister(jump); }
            });
            check("ESP followed by tracers preserves GL states and the depth buffer", () -> {
                player.posX = player.lastTickPosX = player.posZ = player.lastTickPosZ = 0;
                world.loadedEntityList.clear();
                world.loadedEntityList.add(target(3, 0, 10));
                camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1);
                GlStateManager.clearDepth(0.37);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                GlStateManager.clearDepth(1);
                GlStateManager.enableLighting();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
                GlStateManager.color(0.2f, 0.4f, 0.6f, 0.8f);
                float[] projection = matrix(GL11.GL_PROJECTION_MATRIX);
                float[] modelview = matrix(GL11.GL_MODELVIEW_MATRIX);
                float depthBefore = depthPixel();
                ESPMod esp = new ESPMod();
                esp.render(1);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
                new TracersMod().render(1);
                require(coloredPixels() > 40, "tracer disappeared when 2D ESP was enabled");
                require(Arrays.equals(projection, matrix(GL11.GL_PROJECTION_MATRIX))
                        && Arrays.equals(modelview, matrix(GL11.GL_MODELVIEW_MATRIX)), "world matrices leaked");
                require(GL11.glIsEnabled(GL11.GL_DEPTH_TEST) && GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
                        && GL11.glIsEnabled(GL11.GL_CULL_FACE) && GL11.glIsEnabled(GL11.GL_TEXTURE_2D)
                        && GL11.glIsEnabled(GL11.GL_LIGHTING) && GL11.glIsEnabled(GL11.GL_BLEND)
                        && GL11.glIsEnabled(GL11.GL_ALPHA_TEST), "GL enable states leaked");
                require(GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB) == GL11.GL_ONE
                        && GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB) == GL11.GL_ONE,
                        "blend functions leaked");
                require(depthPixel() == depthBefore, "2D ESP cleared or overwrote the world's depth buffer");
            });
            check("tracers stay at the crosshair with third person and camera bobbing", () -> {
                world.loadedEntityList.clear();
                world.loadedEntityList.add(target(3, 0, 10));
                mc.gameSettings.thirdPersonView = 2;
                camera(0, 2.5, -5, 0, 1, 10);
                GlStateManager.rotate(8, 0, 0, 1);
                GlStateManager.translate(0.08, 0.03, 0);
                new TracersMod().render(1);
                require(coloredPixels() > 40 && centerHasColor(), "tracer missed the crosshair after camera transform");
                mc.gameSettings.thirdPersonView = 0;
            });
            check("NameTag preserves states while drawing labels", () -> {
                world.loadedEntityList.clear();
                world.loadedEntityList.add(target(2, 0, 6));
                camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1);
                ((FixtureFont) mc.fontRendererObj).drawn.clear();
                float[] before = matrix(GL11.GL_MODELVIEW_MATRIX);
                new NameTagMod().render(1);
                require(!((FixtureFont) mc.fontRendererObj).drawn.isEmpty(), "name tag did not render");
                require(Arrays.equals(before, matrix(GL11.GL_MODELVIEW_MATRIX))
                        && !GL11.glIsEnabled(GL11.GL_LIGHTING) && GL11.glIsEnabled(GL11.GL_CULL_FACE),
                        "name tag altered the following module's render state");
            });
            check("global friendly and hostile selection reaches ESP, NameTag and Tracers", () -> {
                ModManager modules = new ModManager(); MeowClient.modManager = modules; modules.load();
                TargetMod targets = modules.getByClass(TargetMod.class); targets.players.setValue(false); targets.friendlies.setValue(true);
                player.posX = player.posY = player.posZ = player.lastTickPosX = player.lastTickPosY = player.lastTickPosZ = 0;
                world.loadedEntityList.clear(); world.loadedEntityList.add(visualLiving(FixtureVillager.class));
                ESPMod esp = new ESPMod(); esp.mode.setValue("Box3D");
                camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1); esp.render(1);
                require(coloredPixels() > 40, "ESP did not draw a selected friendly entity");
                camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1); esp.mode.setValue("2D"); esp.render(1);
                require(coloredPixels() > 40, "2D ESP or its equipment path rejected a friendly entity");
                ((FixtureFont)mc.fontRendererObj).drawn.clear(); new NameTagMod().render(1);
                require(!((FixtureFont)mc.fontRendererObj).drawn.isEmpty(), "NameTag did not label a selected friendly entity");
                camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1); new TracersMod().render(1);
                require(coloredPixels() > 40, "Tracers kept its local animal setting instead of the shared selection");
                targets.friendlies.setValue(false); camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1); esp.render(1);
                ((FixtureFont)mc.fontRendererObj).drawn.clear(); new NameTagMod().render(1); new TracersMod().render(1);
                require(coloredPixels() == 0 && ((FixtureFont)mc.fontRendererObj).drawn.isEmpty(), "excluded friendly entity stayed visible");
                targets.mobs.setValue(true); world.loadedEntityList.clear(); world.loadedEntityList.add(visualLiving(FixtureSlime.class));
                camera(0, player.getEyeHeight(), 0, 0, player.getEyeHeight(), 1); esp.render(1);
                require(coloredPixels() > 40, "a hostile outside EntityMob was not rendered");
                for (Mod mod : modules.getEnableMods()) mod.setEnable(false);
            });
            check("TargetHUD draws an overhealed target acquired by KillAura", () -> {
                ModManager modules = new ModManager(); MeowClient.modManager = modules;
                BlatantMod blatant = new BlatantMod(); KillAuraMod aura = new KillAuraMod(); TargetHUDMod hud = new TargetHUDMod();
                modules.getMods().add(blatant); modules.getMods().add(aura); modules.getMods().add(hud);
                DataWatcher owner = new DataWatcher(player); owner.addObject(0, (byte)0); owner.addObject(6, 20f); setField(player, "dataWatcher", owner);
                player.posX = player.posY = player.posZ = player.lastTickPosX = player.lastTickPosY = player.lastTickPosZ = 0;
                player.rotationYaw = player.rotationPitch = 0; mc.inGameHasFocus = true; mc.currentScreen = null; player.clearItemInUse();
                world.loadedEntityList.clear(); EntityLivingBase enemy = (EntityLivingBase)target(0, 0, 3); enemy.setEntityId(100); enemy.getDataWatcher().updateObject(6, 40f); world.loadedEntityList.add(enemy);
                RenderManager original = mc.manager; AvatarRenderManager avatar = allocate(AvatarRenderManager.class); mc.manager = avatar;
                blatant.setEnable(true); aura.autoBlock.setValue(false); aura.setEnable(true); hud.setEnable(true);
                try {
                    EventManager.call(new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, 0, 0, true));
                    require(aura.getTarget() == enemy && hud.getDisplayTarget() == enemy, "KillAura target did not reach the real HUD draw path");
                    overlay(); HudLayout.render(hud);
                    require(coloredPixels() > 40 && avatar.draws == 1, "TargetHUD did not draw its card and avatar");
                    require((Double)field(hud, "hpWidth") == 90, "overheal exceeded the HUD bar width");
                    enemy.getDataWatcher().updateObject(6, Float.NaN); overlay(); HudLayout.render(hud);
                    require(Double.isFinite((Double)field(hud, "hpWidth")), "invalid health poisoned the smooth bar");
                    enemy.getDataWatcher().updateObject(6, 20f); enemy.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(0);
                    overlay(); HudLayout.render(hud); require(Double.isFinite((Double)field(hud, "hpWidth")), "zero max health produced an invalid bar");
                    enemy.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20); overlay(); HudLayout.render(hud);
                    require(Double.isFinite((Double)field(hud, "hpWidth")) && (Double)field(hud, "hpWidth") <= 90, "health recovery left an invalid HUD state");
                    world.loadedEntityList.clear(); EntityLivingBase next = (EntityLivingBase)target(0, 0, 3); next.setEntityId(101); next.getDataWatcher().updateObject(6, 5f); world.loadedEntityList.add(next);
                    player.ticksExisted++; EventManager.call(new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, 0, 0, true));
                    overlay(); HudLayout.render(hud); require(hud.getDisplayTarget() == next && (Double)field(hud, "hpWidth") == 22.5, "new target inherited the previous bar");
                    hud.style.setValue("Simple"); overlay(); HudLayout.render(hud); require(coloredPixels() > 40, "Simple HUD failed on the same target");
                    world.loadedEntityList.clear(); player.ticksExisted++; EventManager.call(new MotionEvent(MotionEvent.Type.PRE, 0, 0, 0, 0, 0, true));
                    overlay(); HudLayout.render(hud); require(hud.getDisplayTarget() == null && (Double)field(hud, "hpWidth") == 0, "losing the target retained health state");
                } finally { hud.setEnable(false); aura.setEnable(false); blatant.setEnable(false); mc.manager = original; }
            });
            check("NightVision changes the vanilla lightmap and restores Gamma mode", () -> {
                ModManager mods = new ModManager();
                MeowClient.modManager = mods;
                FullBrightMod mod = new FullBrightMod();
                mods.getMods().add(mod);
                mc.gameSettings.gammaSetting = 0.0f;
                setField(mc, "mcProfiler", new Profiler());
                setField(world, "provider", new WorldProviderSurface());
                for (int i = 0; i < 16; i++) world.provider.getLightBrightnessTable()[i] = i / 15f;
                DynamicTexture lightmap = new DynamicTexture(16, 16);
                setField(mc.entityRenderer, "lightmapTexture", lightmap);
                setField(mc.entityRenderer, "lightmapColors", lightmap.getTextureData());
                Method update = EntityRenderer.class.getDeclaredMethod("updateLightmap", float.class);
                update.setAccessible(true);
                setField(mc.entityRenderer, "lightmapUpdateNeeded", true);
                update.invoke(mc.entityRenderer, 1f);
                int normal = lightmap.getTextureData()[0] & 255;
                mod.mode.setValue("NightVision");
                mod.setEnable(true);
                setField(mc.entityRenderer, "lightmapUpdateNeeded", true);
                update.invoke(mc.entityRenderer, 1f);
                require((lightmap.getTextureData()[0] & 255) > normal + 100, "night vision did not brighten the lightmap");
                require(mc.gameSettings.gammaSetting == 0.0f, "night vision overrode the Gamma setting");
                mod.mode.setValue("Gamma");
                mod.update();
                require(mc.gameSettings.gammaSetting == mod.gamma.getValue().floatValue(), "live mode switch did not apply Gamma");
                mod.setEnable(false);
                require(mc.gameSettings.gammaSetting == 0.0f, "original Gamma did not return");
                lightmap.deleteGlTexture();
            });
            check("held projectiles draw their arcs and stop at blocks or entities", () -> {
                player.posX = player.posY = player.posZ = player.lastTickPosX = player.lastTickPosY = player.lastTickPosZ = 0;
                player.rotationYaw = 0; player.rotationPitch = -20;
                world.loadedEntityList.clear();
                TrajectoriesMod mod = new TrajectoriesMod();
                for (net.minecraft.item.Item item : new net.minecraft.item.Item[]{Items.bow, Items.ender_pearl, Items.snowball, Items.egg}) {
                    player.inventory.mainInventory[0] = new ItemStack(item);
                    ProjectilePrediction.Result result = mod.predict(1);
                    require(result != null && result.hit != null && result.hit.hitVec.yCoord == 0, "arc did not end at the floor");
                    camera(3, 3, -7, 0, 1, 10);
                    mod.render(1);
                    require(coloredPixels() > 40, "supported projectile did not draw an arc");
                }
                saveFrame("projectile-arc");
                player.inventory.mainInventory[0] = new ItemStack(Items.bow);
                player.rotationPitch = 0;
                Entity victim = target(0, 0, 6);
                world.loadedEntityList.add(victim);
                require(mod.predict(1).hit.entityHit == victim, "arc passed through a nearer entity");
                player.inventory.mainInventory[0] = null;
            });
            check("damage direction appears for known sources and not unknown damage", () -> {
                AtomicLong clock = new AtomicLong(1000);
                DamageIndicatorMod mod = new DamageIndicatorMod(clock::get);
                world.loadedEntityList.clear();
                player.hurtTime = 0; player.setLastAttacker(null); mod.update();
                player.setLastAttacker(target(0, 0, 4)); player.hurtTime = 10; mod.update();
                overlay(); mod.draw();
                require(coloredPixels() > 20, "known source did not draw a direction indicator");
                saveFrame("damage-direction");
                clock.addAndGet(2000); overlay(); mod.draw();
                require(coloredPixels() == 0, "expired damage indicator remained visible");
                mod.disable(); player.hurtTime = 0; player.setLastAttacker(null); mod.update();
                player.hurtTime = 10; mod.update(); overlay(); mod.draw();
                require(coloredPixels() == 0, "unknown environmental damage was assigned an arbitrary direction");
                EntityLivingBase attacker = (EntityLivingBase) target(2, 0, 0);
                attacker.rotationYaw = attacker.rotationYawHead = 90; attacker.isSwingInProgress = true;
                world.loadedEntityList.add(attacker);
                DamageIndicatorMod.Source inferred = DamageIndicatorMod.findSource(player, world.loadedEntityList, true);
                require(inferred != null && inferred.inferred && inferred.entity == attacker, "nearby facing melee swing was not inferred");
                require(DamageIndicatorMod.findSource(player, world.loadedEntityList, false) == null, "inference switch was ignored");
                require(DamageIndicatorMod.relativeYaw(0, 4, 0) == 0 && DamageIndicatorMod.relativeYaw(-4, 0, 90) == 0,
                        "indicator does not follow camera yaw");
            });
            check("ClientSetting brush opens HUD editing and key bindings work", () -> {
                prepareRealFont();
                MeowClient.modManager = new ModManager(); MeowClient.modManager.load();
                ClientLanguage.setChinese(true);
                TestSettings settings = new TestSettings();
                mc.displayGuiScreen(settings);
                overlay(); settings.drawScreen(0, 0, 1); saveFrame("client-settings");
                int uiSize = new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth();
                int panelWidth = Math.min(384, uiSize - 30), left = (uiSize - panelWidth) / 2, top = (uiSize - 226) / 2;
                boolean rawBefore = cn.sux1ng.client.input.HighPollingInput.isEnabled();
                settings.click(left + 32, top + 137);
                require(cn.sux1ng.client.input.HighPollingInput.isEnabled() != rawBefore, "raw input toggle did not change");
                settings.click(left + 32, top + 137);
                settings.click(left + panelWidth - 62, top + 195);
                require(mc.currentScreen instanceof HudEditorScreen, "paintbrush did not open the HUD editor");
                overlay(); mc.currentScreen.drawScreen(0, 0, 1); saveFrame("hud-editor");
                TestHud editor = new TestHud(settings); mc.displayGuiScreen(editor);
                editor.key(Keyboard.KEY_TAB);
                Mod logo = MeowClient.modManager.getByName("Logo");
                HudLayout.Bounds original = HudLayout.originalBounds(logo, uiSize);
                editor.click((int)original.x + 4, (int)original.y + 4, 0);
                overlay(); editor.drawScreen(150, 100, 1); editor.release(150, 100);
                HudLayout.Bounds moved = HudLayout.transform("Logo", original, uiSize, uiSize);
                require(Math.abs(moved.x - 146) < 0.1 && Math.abs(moved.y - 96) < 0.1, "HUD dragging did not move the widget");
                editor.key(Keyboard.KEY_DELETE);
                require(HudLayout.transform("Logo", original, uiSize, uiSize).x == original.x, "HUD reset did not restore the original placement");
                mc.displayGuiScreen(settings);
                settings.click(left + panelWidth - 28, top + 195);
                require(mc.currentScreen instanceof KeyBindingsScreen, "keyboard icon did not open binding management");
                TestBindings bindings = new TestBindings(settings); mc.displayGuiScreen(bindings);
                int bindingLeft = (uiSize - Math.min(520, uiSize - 24)) / 2;
                Mod first = MeowClient.modManager.getMods().get(0);
                bindings.click(bindingLeft + 20, 75, 0); bindings.key(Keyboard.KEY_F);
                require(first.getKey() == Keyboard.KEY_F, "captured key was not assigned");
                bindings.click(bindingLeft + 20, 75, 1);
                require(first.getKey() == 0, "right click did not clear the binding");
                overlay(); bindings.drawScreen(0, 0, 1); saveFrame("key-bindings");
                ClientLanguage.setChinese(false); HudLayout.resetAll(); mc.currentScreen = null;
            });
            check("TNT countdown renders through the real font pipeline", () -> {
                world.loadedEntityList.clear();
                EntityTNTPrimed tnt = allocate(EntityTNTPrimed.class);
                tnt.posZ = tnt.lastTickPosZ = 6; tnt.height = 0.98f; tnt.fuse = 40;
                world.loadedEntityList.add(tnt);
                camera(0, 1.6, 0, 0, 1.6, 1);
                new TNTTimerMod().render(0);
                require(coloredPixels() > 20, "TNT countdown was invisible");
                saveFrame("tnt-timer");
            });
            if (failures != 0) throw new AssertionError(failures + " R6 rendering checks failed");
            System.out.println("R6 rendering checks passed");
        } finally {
            buffer.destroy();
        }
    }

    private static void setupFixture() throws Exception {
        mc = allocate(FixtureMinecraft.class);
        setStaticField(Minecraft.class, "theMinecraft", mc);
        mc.gameSettings = new GameSettings();
        setField(mc, "timer", new Timer(20));
        Bootstrap.register();
        mc.displayWidth = mc.displayHeight = SIZE;
        manager = allocate(RenderManager.class);
        mc.manager = manager;
        player = allocate(FixturePlayer.class);
        player.height = 1.8f;
        player.width = 0.6f;
        player.setEntityBoundingBox(new AxisAlignedBB(-0.3, 0, -0.3, 0.3, 1.8, 0.3));
        player.inventory = new InventoryPlayer(player);
        player.getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(4);
        FixtureFont font = allocate(FixtureFont.class);
        font.drawn = new ArrayList<>();
        mc.fontRendererObj = font;
        mc.thePlayer = player;
        setField(mc, "renderViewEntity", player);
        world = allocate(FixtureWorld.class);
        setField(world, "loadedEntityList", new ArrayList<Entity>());
        mc.theWorld = world;
        player.worldObj = world;
        FixtureConnection connection = allocate(FixtureConnection.class);
        mc.playerController = new PlayerControllerMP(mc, connection);
        mc.entityRenderer = allocate(EntityRenderer.class);
        setField(mc.entityRenderer, "mc", mc);
        uploader = new RecordingUploader();
        setField(Tessellator.getInstance(), "vboUploader", uploader);
        OpenGlHelper.initializeTextures();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
    }

    private static Entity target(double x, double y, double z) throws Exception {
        FixtureTarget entity = allocate(FixtureTarget.class);
        entity.posX = entity.lastTickPosX = x;
        entity.posY = entity.lastTickPosY = y;
        entity.posZ = entity.lastTickPosZ = z;
        entity.width = 0.6f;
        entity.height = 1.8f;
        entity.worldObj = world;
        entity.setEntityBoundingBox(new AxisAlignedBB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3));
        entity.inventory = new InventoryPlayer(entity);
        entity.getAttributeMap().registerAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20);
        DataWatcher watcher = new DataWatcher(entity);
        watcher.addObject(0, (byte) 0);
        watcher.addObject(6, 20f);
        setField(entity, "dataWatcher", watcher);
        return entity;
    }

    private static <T extends EntityLivingBase> T visualLiving(Class<T> type) throws Exception {
        T entity = allocate(type); entity.worldObj = world; entity.posZ = entity.lastTickPosZ = 6;
        entity.height = 1.8f; entity.width = 0.6f; entity.setEntityBoundingBox(new AxisAlignedBB(-0.3, 0, 5.7, 0.3, 1.8, 6.3));
        DataWatcher watcher = new DataWatcher(entity); watcher.addObject(0, (byte)0); watcher.addObject(6, 20f); setField(entity, "dataWatcher", watcher);
        entity.getAttributeMap().registerAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20); return entity;
    }
    private static void camera(double x, double y, double z, double tx, double ty, double tz) {
        GlStateManager.viewport(0, 0, SIZE, SIZE);
        GlStateManager.clearColor(0, 0, 0, 1);
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GLU.gluPerspective(70, 1, 0.05f, 100);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GLU.gluLookAt((float)x, (float)y, (float)z, (float)tx, (float)ty, (float)tz, 0, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }

    private static int coloredPixels() {
        ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
        GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        int count = 0;
        for (int i = 0; i < pixels.capacity(); i += 4) {
            if ((pixels.get(i) & 255) + (pixels.get(i + 1) & 255) + (pixels.get(i + 2) & 255) > 30) count++;
        }
        return count;
    }

    private static void overlay() {
        GlStateManager.viewport(0, 0, SIZE, SIZE);
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.loadIdentity();
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        GlStateManager.ortho(0, sr.getScaledWidth(), sr.getScaledHeight(), 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.loadIdentity();
        GlStateManager.translate(0, 0, -2000);
        GlStateManager.disableDepth(); GlStateManager.depthMask(false); GlStateManager.disableCull();
        GlStateManager.enableTexture2D(); GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableLighting(); GlStateManager.color(1, 1, 1, 1);
    }

    private static void prepareRealFont() throws Exception {
        java.nio.file.Path resourceFile = Paths.get(R6RenderRegressionTest.class.getClassLoader().getResource("assets/minecraft/font/glyph_sizes.bin").toURI());
        java.io.File root = resourceFile.getParent().getParent().getParent().getParent().toFile();
        SimpleReloadableResourceManager resources = new SimpleReloadableResourceManager(new IMetadataSerializer());
        resources.reloadResourcePack(new FolderResourcePack(root));
        mc.resources = resources; mc.textures = new TextureManager(resources);
        mc.fontRendererObj = new FontRenderer(mc.gameSettings, new ResourceLocation("textures/font/ascii.png"), mc.textures, false);
        mc.fontRendererObj.onResourceManagerReload(resources);
    }

    private static float[] matrix(int type) {
        FloatBuffer values = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(type, values);
        float[] result = new float[16];
        values.get(result);
        return result;
    }

    private static float depthPixel() {
        FloatBuffer value = BufferUtils.createFloatBuffer(1);
        GL11.glReadPixels(SIZE / 2, SIZE / 2, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, value);
        return value.get(0);
    }

    private static boolean centerHasColor() {
        ByteBuffer pixel = BufferUtils.createByteBuffer(5 * 5 * 4);
        GL11.glReadPixels(SIZE / 2 - 2, SIZE / 2 - 2, 5, 5, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixel);
        for (int i = 0; i < pixel.capacity(); i += 4) if ((pixel.get(i) & 255) > 30) return true;
        return false;
    }

    private static void saveFrame(String name) throws Exception {
        if (screenshots == null) return;
        Files.createDirectories(Paths.get(screenshots));
        ByteBuffer pixels = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
        GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int i = (y * SIZE + x) * 4;
            image.setRGB(x, SIZE - y - 1, (pixels.get(i) & 255) << 16 | (pixels.get(i + 1) & 255) << 8 | pixels.get(i + 2) & 255);
        }
        ImageIO.write(image, "png", Paths.get(screenshots, name + ".png").toFile());
    }

    private static void check(String name, CheckedRunnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            failures++;
            System.err.println("FAIL " + name + ": " + failure);
            if (!(failure instanceof AssertionError)) failure.printStackTrace();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception failure) { throw new RuntimeException(failure); }
    }

    private static <T> T allocate(Class<T> type) throws InstantiationException {
        return type.cast(UNSAFE.allocateInstance(type));
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object field(Object target, String name) throws Exception {
        return findField(target.getClass(), name).get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        findField(target.getClass(), name).set(target, value);
    }

    private static void setStaticField(Class<?> type, String name, Object value) throws Exception {
        findField(type, name).set(null, value);
    }

    private interface CheckedRunnable { void run() throws Exception; }

    private static class RecordingUploader extends WorldVertexBufferUploader {
        final List<double[]> positions = new ArrayList<>();
        @Override
        public void func_181679_a(WorldRenderer renderer) {
            ByteBuffer bytes = renderer.getByteBuffer();
            int stride = renderer.getVertexFormat().getNextOffset();
            for (int i = 0; i < renderer.getVertexCount(); i++) {
                int offset = i * stride;
                positions.add(new double[]{bytes.getFloat(offset), bytes.getFloat(offset + 4), bytes.getFloat(offset + 8)});
            }
            super.func_181679_a(renderer);
        }
    }

    private static class FixtureMinecraft extends Minecraft {
        RenderManager manager;
        IResourceManager resources;
        TextureManager textures;
        FixtureMinecraft() { super(null); }
        @Override public RenderManager getRenderManager() { return manager; }
        @Override public boolean isUnicode() { return false; }
        @Override public IResourceManager getResourceManager() { return resources; }
        @Override public TextureManager getTextureManager() { return textures; }
        @Override public void displayGuiScreen(GuiScreen screen) {
            currentScreen = screen;
            if (screen != null) {
                net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(this);
                screen.setWorldAndResolution(this, sr.getScaledWidth(), sr.getScaledHeight());
            }
        }
    }

    private static class FixturePlayer extends EntityPlayerSP {
        FixturePlayer() { super(null, null, null, null); }
        @Override public void attackTargetEntityWithCurrentItem(Entity target) {}
        @Override public boolean isSprinting() { return false; }
        @Override public boolean isPotionActive(Potion potion) { return false; }
        @Override public void triggerAchievement(StatBase stat) {}
        @Override public void addExhaustion(float amount) {}
    }

    private static class FixtureFont extends FontRenderer {
        List<String> drawn;
        FixtureFont() { super(null, null, null, false); }
        @Override public int getStringWidth(String text) { return text.length() * 6; }
        @Override public int drawStringWithShadow(String text, float x, float y, int color) {
            drawn.add(text);
            return getStringWidth(text);
        }
        @Override public int drawString(String text, int x, int y, int color) {
            drawn.add(text); return getStringWidth(text);
        }
    }

    private static class FixtureTarget extends EntityOtherPlayerMP {
        FixtureTarget() { super(null, null); }
        @Override public boolean isSpectator() { return false; }
        @Override public IChatComponent getDisplayName() { return new ChatComponentText("Player"); }
        @Override public String getName() { return "Player"; }
    }
    private static class AvatarRenderManager extends RenderManager {
        int draws; AvatarRenderManager() { super(null, null); }
        @Override public boolean renderEntityWithPosYaw(Entity entity, double x, double y, double z, float yaw, float partialTicks) { draws++; return true; }
    }

    private static class FixtureVillager extends EntityVillager {
        FixtureVillager() { super(null); }
        @Override public ItemStack getEquipmentInSlot(int slot) { return null; }
        @Override public ItemStack getHeldItem() { return null; }
        @Override public IChatComponent getDisplayName() { return new ChatComponentText("Friendly"); }
    }
    private static class FixtureSlime extends EntitySlime {
        FixtureSlime() { super(null); }
        @Override public ItemStack getEquipmentInSlot(int slot) { return null; }
        @Override public ItemStack getHeldItem() { return null; }
        @Override public IChatComponent getDisplayName() { return new ChatComponentText("Slime"); }
    }
    private static class FixtureConnection extends NetHandlerPlayClient {
        FixtureConnection() { super(null, null, null, null); }
        @Override public void addToSendQueue(Packet packet) {}
    }

    private static class FixtureWorld extends WorldClient {
        int particles;
        boolean forced;
        EnumParticleTypes type;
        FixtureWorld() { super(null, null, 0, null, null); }
        @Override public void spawnParticle(EnumParticleTypes type, double x, double y, double z,
                                             double vx, double vy, double vz, int... args) { particles++; this.type = type; }
        @Override public void spawnParticle(EnumParticleTypes type, boolean forced, double x, double y, double z,
                                             double vx, double vy, double vz, int... args) {
            particles++; this.type = type; this.forced = forced;
        }
        @Override public float getSunBrightness(float partialTicks) { return 0; }
        @Override public boolean isBlockLoaded(BlockPos pos) { return Math.abs(pos.getX()) < 100 && Math.abs(pos.getZ()) < 100; }
        @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.air.getDefaultState(); }
        @Override public MovingObjectPosition rayTraceBlocks(Vec3 from, Vec3 to) { return rayTraceBlocks(from, to, false, true, false); }
        @Override public MovingObjectPosition rayTraceBlocks(Vec3 from, Vec3 to, boolean liquid, boolean noBox, boolean last) {
            if (from.yCoord >= 0 && to.yCoord < 0) {
                double t = -from.yCoord / (to.yCoord - from.yCoord);
                Vec3 hit = new Vec3(from.xCoord + (to.xCoord - from.xCoord) * t, 0, from.zCoord + (to.zCoord - from.zCoord) * t);
                return new MovingObjectPosition(hit, EnumFacing.UP, new BlockPos(hit));
            }
            return null;
        }
        @Override public List<Entity> getEntitiesWithinAABBExcludingEntity(Entity excluded, AxisAlignedBB area) {
            List<Entity> entities = new ArrayList<>();
            for (Entity entity : loadedEntityList) if (entity != excluded && entity.getEntityBoundingBox().intersectsWith(area)) entities.add(entity);
            return entities;
        }
    }

    private static class TestSettings extends ClientSettingsScreen {
        TestSettings() { super(null); }
        void click(int x, int y) throws Exception { mouseClicked(x, y, 0); }
    }
    private static class TestHud extends HudEditorScreen {
        TestHud(GuiScreen parent) { super(parent); }
        void click(int x, int y, int button) throws Exception { mouseClicked(x, y, button); }
        void release(int x, int y) { mouseReleased(x, y, 0); }
        void key(int key) throws Exception { keyTyped('\0', key); }
    }
    private static class TestBindings extends KeyBindingsScreen {
        TestBindings(GuiScreen parent) { super(parent); }
        void click(int x, int y, int button) throws Exception { mouseClicked(x, y, button); }
        void key(int key) throws Exception { keyTyped('\0', key); }
    }
}
