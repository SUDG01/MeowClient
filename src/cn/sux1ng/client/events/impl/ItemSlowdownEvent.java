package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;
import net.minecraft.client.entity.EntityPlayerSP;

/** The two vanilla item-use multipliers, applied once to fresh input. */
public final class ItemSlowdownEvent extends Event {
    public final EntityPlayerSP player;
    public float forward = 0.2f, strafe = 0.2f;
    public ItemSlowdownEvent(EntityPlayerSP player) { this.player = player; }
}
