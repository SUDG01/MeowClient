package cn.sux1ng.client.movement;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;

/** Replayed packets still reach other listeners, while packet buffers avoid capturing them twice. */
public final class PacketReplay {
    private static final ThreadLocal<Boolean> REPLAY = new ThreadLocal<>();
    private PacketReplay() {}
    public static boolean active() { return Boolean.TRUE.equals(REPLAY.get()); }
    public static void send(NetHandlerPlayClient connection, Packet<?> packet) {
        Boolean previous = REPLAY.get(); REPLAY.set(true);
        try { connection.addToSendQueue(packet); }
        finally { if (previous == null) REPLAY.remove(); else REPLAY.set(previous); }
    }
}
