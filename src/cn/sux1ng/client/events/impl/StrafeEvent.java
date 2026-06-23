package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;

/**
 * 空中转向事件 — Entity.moveEntityWithHeading() 空中分支触发
 * 可修改 forward/strafe/friction 来实现自定义空中移动
 */
public class StrafeEvent extends Event {
    public float forward, strafe, friction;

    public StrafeEvent(float forward, float strafe, float friction) {
        this.forward = forward;
        this.strafe = strafe;
        this.friction = friction;
    }
}
