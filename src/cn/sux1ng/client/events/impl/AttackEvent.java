package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/** Local attack observation, fired once by PlayerControllerMP before damage is applied. */
public final class AttackEvent extends Event {
    private final EntityPlayer player;
    private final Entity target;

    public AttackEvent(EntityPlayer player, Entity target) {
        this.player = player;
        this.target = target;
    }

    public EntityPlayer getPlayer() { return player; }
    public Entity getTarget() { return target; }
}
