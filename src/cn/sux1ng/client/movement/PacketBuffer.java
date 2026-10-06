package cn.sux1ng.client.movement;

import net.minecraft.network.Packet;
import java.util.*;

/** Bounded FIFO shared by Netty callbacks and the client tick. */
public final class PacketBuffer {
    private final int capacity;
    private final ArrayDeque<Packet<?>> packets = new ArrayDeque<>();
    public PacketBuffer(int capacity) { if (capacity < 1) throw new IllegalArgumentException("capacity"); this.capacity = capacity; }
    public synchronized boolean offer(Packet<?> packet) { if (packets.size() >= capacity) return false; packets.add(Objects.requireNonNull(packet)); return true; }
    public synchronized List<Packet<?>> takeAll() { List<Packet<?>> result = new ArrayList<>(packets); packets.clear(); return result; }
    public synchronized void clear() { packets.clear(); }
    public synchronized int size() { return packets.size(); }
}
