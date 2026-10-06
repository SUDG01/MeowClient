package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;

/** Fresh physical input, before item slowdown, sprint checks and the vanilla jump stage. */
public final class MovementInputEvent extends Event {
    public final EntityPlayerSP player;
    public final MovementInput input;
    public MovementInputEvent(EntityPlayerSP player, MovementInput input) { this.player = player; this.input = input; }
}
