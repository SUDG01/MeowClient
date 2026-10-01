package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;
import net.minecraft.entity.player.EntityPlayer;

/** Local jump observation with the takeoff position, independent of render frames. */
public final class JumpEvent extends Event {
    private final EntityPlayer player;
    private final double x, y, z;

    public JumpEvent(EntityPlayer player, double x, double y, double z) {
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public EntityPlayer getPlayer() { return player; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
}
