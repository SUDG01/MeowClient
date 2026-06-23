package cn.sux1ng.client.events;

import net.minecraft.network.Packet;

/**
 * 数据包事件（收包/发包时触发）
 * 可用于拦截、修改或监听网络数据包
 */
public class EventPacket extends Event {
    private final Packet<?> packet;

    public EventPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}
