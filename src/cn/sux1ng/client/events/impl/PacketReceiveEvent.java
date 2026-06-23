package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;
import net.minecraft.network.Packet;

/**
 * 收包事件 — NetworkManager.channelRead0() 中触发
 * 设置 cancelled = true 可丢弃该包
 */
public class PacketReceiveEvent extends Event {
    public Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
