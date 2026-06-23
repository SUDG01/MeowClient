package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;
import net.minecraft.network.Packet;

/**
 * 发包事件 — NetworkManager.sendPacket() 中触发
 * 设置 cancelled = true 可阻止发送
 */
public class PacketSendEvent extends Event {
    public Packet<?> packet;

    public PacketSendEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
