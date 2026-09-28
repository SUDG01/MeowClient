package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;
import net.minecraft.network.Packet;
import java.util.Objects;

/**
 * 发包事件 — NetworkManager.sendPacket() 中触发
 * Runs synchronously on the send caller's thread. A replacement packet must be valid for
 * the current connection phase; cancellation prevents dispatch and queueing.
 */
public class PacketSendEvent extends Event {
    private Packet<?> packet;

    public PacketSendEvent(Packet<?> packet) {
        this.packet = Objects.requireNonNull(packet, "packet");
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = Objects.requireNonNull(packet, "packet");
    }
}
