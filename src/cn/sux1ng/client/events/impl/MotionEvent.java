package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;

/**
 * PRE runs before movement packets are assembled; changes to position, rotation and
 * onGround are copied into EventUpdate. POST is an observation after packet submission.
 */
public class MotionEvent extends Event {
    public final Type type;
    public double x, y, z;
    public float yaw, pitch;
    public boolean onGround;

    public MotionEvent(Type type, double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public enum Type { PRE, POST }
}
