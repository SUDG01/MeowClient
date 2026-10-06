package cn.sux1ng.client.mod.mods.misc;

import cn.sux1ng.client.mod.Category;
import cn.sux1ng.client.mod.Mod;
import cn.sux1ng.client.value.BooleanValue;
import cn.sux1ng.client.value.NumberValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import java.util.*;
import java.util.function.LongSupplier;

/** Observes player identities without changing world entities or cancelling manual actions. */
public final class AntiBotMod extends Mod {
    public final BooleanValue tabCheck = new BooleanValue("TabCheck", true);
    public final BooleanValue profileCheck = new BooleanValue("ProfileCheck", true);
    public final BooleanValue duplicateCheck = new BooleanValue("DuplicateCheck", true);
    public final BooleanValue groundCheck = new BooleanValue("GroundCheck", false);
    public final BooleanValue hideBots = new BooleanValue("HideBots", true);
    public final NumberValue spawnDelay = new NumberValue("SpawnDelay", 500, 0, 2000, 50);
    public final NumberValue checkDelay = new NumberValue("CheckDelay", 1000, 250, 5000, 250);
    private final LongSupplier clock;
    private final IdentityHashMap<EntityPlayer, Observation> observations = new IdentityHashMap<>();
    private final Map<UUID, EntityPlayer> identities = new HashMap<>();
    private final Map<String, EntityPlayer> names = new HashMap<>();
    private World world;
    private EntityPlayerSP player;
    private NetHandlerPlayClient connection;
    private int tick = Integer.MIN_VALUE, size = -1, revision;

    public AntiBotMod() { this(() -> System.nanoTime() / 1_000_000L); }
    public AntiBotMod(LongSupplier clock) {
        super("AntiBot", Category.MISC); this.clock = Objects.requireNonNull(clock, "clock");
        addValues(tabCheck, profileCheck, duplicateCheck, groundCheck, hideBots, spawnDelay, checkDelay);
    }
    @Override public void enable() { reset(); }
    @Override public void disable() { reset(); }
    @Override public void update() { refresh(false); }
    private void reset() {
        observations.clear(); identities.clear(); names.clear(); world = null; player = null; connection = null;
        tick = Integer.MIN_VALUE; size = -1; revision = 0;
    }
    private boolean refresh(boolean force) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || mc.isSingleplayer() || mc.getNetHandler() == null) { reset(); return false; }
        if (world != mc.theWorld || player != mc.thePlayer || connection != mc.getNetHandler()) {
            reset(); world = mc.theWorld; player = mc.thePlayer; connection = mc.getNetHandler();
        }
        if (!force && tick == player.ticksExisted && size == world.loadedEntityList.size()) return true;
        tick = player.ticksExisted; size = world.loadedEntityList.size(); revision++;
        identities.clear(); names.clear(); rememberOwner(player);
        long now = clock.getAsLong();
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityPlayer) || entity == player || entity.isDead
                    || !Float.isFinite(((EntityPlayer)entity).getHealth()) || ((EntityPlayer)entity).getHealth() <= 0) continue;
            EntityPlayer candidate = (EntityPlayer)entity;
            Observation observation = observations.get(candidate);
            if (observation == null) {
                long age = Math.min(60_000L, Math.max(0L, (long)candidate.ticksExisted * 50));
                observation = new Observation(now - age); observations.put(candidate, observation);
            }
            observation.revision = revision; observation.seenGround |= candidate.onGround;
            UUID id = candidate.getUniqueID(); String name = name(candidate);
            if (id != null) identities.put(id, prefer(identities.get(id), candidate));
            if (name != null) names.put(name, prefer(names.get(name), candidate));
        }
        observations.entrySet().removeIf(entry -> entry.getValue().revision != revision);
        return true;
    }
    private void rememberOwner(EntityPlayer owner) {
        if (owner.getUniqueID() != null) identities.put(owner.getUniqueID(), owner);
        String name = name(owner); if (name != null) names.put(name, owner);
    }
    private EntityPlayer prefer(EntityPlayer previous, EntityPlayer candidate) {
        if (previous == null) return candidate;
        if (previous == player) return previous;
        boolean oldTab = validTab(previous), newTab = validTab(candidate);
        if (oldTab != newTab) return oldTab ? previous : candidate;
        long oldSeen = observations.get(previous).firstSeen, newSeen = observations.get(candidate).firstSeen;
        if (oldSeen != newSeen) return oldSeen < newSeen ? previous : candidate;
        return previous.getEntityId() <= candidate.getEntityId() ? previous : candidate;
    }
    private Observation observation(EntityPlayer candidate) {
        if (!refresh(false) || candidate.worldObj != world) return null;
        Observation observation = observations.get(candidate);
        if (observation == null) { refresh(true); observation = observations.get(candidate); }
        if (observation != null) observation.seenGround |= candidate.onGround;
        return observation;
    }
    public boolean allowsAutomatic(Entity entity) {
        if (!(entity instanceof EntityPlayer) || mc == null || entity == mc.thePlayer || !isEnable()) return true;
        EntityPlayer candidate = (EntityPlayer)entity; Observation observation = observation(candidate);
        if (observation == null) return world == null;
        if (clock.getAsLong() - observation.firstSeen < spawnDelay.getValue()) return false;
        return !suspicious(candidate, observation);
    }
    public boolean isBot(Entity entity) {
        if (!(entity instanceof EntityPlayer) || mc == null || entity == mc.thePlayer || !isEnable()) return false;
        EntityPlayer candidate = (EntityPlayer)entity; Observation observation = observation(candidate);
        return observation != null && clock.getAsLong() - observation.firstSeen >= checkDelay.getValue() && suspicious(candidate, observation);
    }
    private boolean suspicious(EntityPlayer candidate, Observation observation) {
        NetworkPlayerInfo info = connection.getPlayerInfo(candidate.getUniqueID());
        if (tabCheck.getValue() && info == null) return true;
        if (profileCheck.getValue() && (candidate.getUniqueID() == null || name(candidate) == null || info != null && !validTab(candidate))) return true;
        if (duplicateCheck.getValue()) {
            EntityPlayer identity = identities.get(candidate.getUniqueID()), named = names.get(name(candidate));
            if (identity != null && identity != candidate || named != null && named != candidate) return true;
        }
        boolean flying = candidate.capabilities != null && candidate.capabilities.allowFlying
                || info != null && (info.getGameType() == WorldSettings.GameType.CREATIVE || info.getGameType() == WorldSettings.GameType.SPECTATOR);
        return groundCheck.getValue() && !observation.seenGround && !flying;
    }
    private boolean validTab(EntityPlayer candidate) {
        NetworkPlayerInfo info = connection.getPlayerInfo(candidate.getUniqueID()); GameProfile profile = candidate.getGameProfile();
        return info != null && info.getGameType() != null && info.getGameType() != WorldSettings.GameType.NOT_SET
                && profile != null && profile.getId() != null && profile.getId().equals(candidate.getUniqueID())
                && profile.getId().equals(info.getGameProfile().getId()) && profile.getName() != null
                && profile.getName().equalsIgnoreCase(info.getGameProfile().getName());
    }
    private static String name(EntityPlayer entity) {
        GameProfile profile = entity.getGameProfile();
        return profile == null || profile.getName() == null || profile.getName().isEmpty() ? null : profile.getName().toLowerCase(Locale.ROOT);
    }
    private static final class Observation {
        final long firstSeen; int revision; boolean seenGround;
        Observation(long firstSeen) { this.firstSeen = firstSeen; }
    }
}
