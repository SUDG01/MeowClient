package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;

/**
 * 移动事件 — Entity.moveEntity() 中触发
 * 可修改 x/y/z 来改变玩家实际移动量
 */
public class MoveEvent extends Event {
    public double x, y, z;

    public MoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
