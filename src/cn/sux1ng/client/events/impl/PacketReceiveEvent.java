package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;
import net.minecraft.network.Packet;
import java.util.Objects;

/**
 * 收包事件 — NetworkManager.channelRead0() 中触发
 * Runs synchronously on Netty's receive thread before packet processing. Listeners must
 * schedule world or GUI work onto Minecraft's main thread. Cancellation drops the packet.
 */
public class PacketReceiveEvent extends Event {
    private Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = Objects.requireNonNull(packet, "packet");
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = Objects.requireNonNull(packet, "packet");
    }
}
