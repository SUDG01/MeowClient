package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;

/**
 * 运动事件 — 每 tick 在 EntityPlayerSP.onLivingUpdate() 中触发
 * PRE:  在移动计算之前（可修改 yaw/pitch 实现 Silent Aim）
 * POST: 在移动计算之后（可读取最终位置）
 */
public class MotionEvent extends Event {
    public Type type;
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
