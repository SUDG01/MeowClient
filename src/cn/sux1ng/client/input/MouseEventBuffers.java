package cn.sux1ng.client.input;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.InputImplementation;
import java.lang.reflect.*;
import java.nio.ByteBuffer;

/** Keeps button/wheel events from being starved by grabbed-cursor motion. */
public final class MouseEventBuffers {
    private static final int EVENTS = 8192;
    private static Object original, installed;
    private MouseEventBuffers() {}
    private static Field field(Class<?> type, String name) throws Exception {
        while (type != null) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
    public static void growNativeQueue(Object mouse) throws Exception {
        Object queue = field(mouse.getClass(), "event_queue").get(mouse);
        synchronized (queue) {
            Field storage = field(queue.getClass(), "queue");
            ByteBuffer before = (ByteBuffer) storage.get(queue);
            if (before.capacity() >= EVENTS * Mouse.EVENT_SIZE) return;
            ByteBuffer after = ByteBuffer.allocate(EVENTS * Mouse.EVENT_SIZE);
            ByteBuffer contents = before.duplicate(); contents.flip(); after.put(contents);
            storage.set(queue, after);
        }
    }
    public static void install() {
        if (!Mouse.isCreated() || installed != null) return;
        try {
            Field implementation = field(Mouse.class, "implementation");
            Object delegate = implementation.get(null);
            if (!delegate.getClass().getName().equals("org.lwjgl.opengl.WindowsDisplay")) return;
            Object windowsMouse = field(delegate.getClass(), "mouse").get(delegate);
            growNativeQueue(windowsMouse);
            Field read = field(Mouse.class, "readBuffer");
            ByteBuffer before = (ByteBuffer) read.get(null);
            ByteBuffer after = ByteBuffer.allocate(EVENTS * Mouse.EVENT_SIZE);
            after.put(before.duplicate()); after.flip(); read.set(null, after);
            ByteBuffer scratch = ByteBuffer.allocate(EVENTS * Mouse.EVENT_SIZE);
            original = delegate;
            installed = Proxy.newProxyInstance(InputImplementation.class.getClassLoader(), new Class<?>[]{InputImplementation.class},
                    (proxy, method, args) -> {
                        try {
                            if (!method.getName().equals("readMouse")) return method.invoke(delegate, args);
                            ByteBuffer destination = (ByteBuffer) args[0];
                            if (!Mouse.isGrabbed()) return method.invoke(delegate, args);
                            filterRead(destination, scratch, buffer -> {
                                try { method.invoke(delegate, new Object[]{buffer}); }
                                catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
                            });
                            return null;
                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
            implementation.set(null, installed);
        } catch (Exception failure) {
            installed = null; original = null;
            System.err.println("Mouse event buffer optimization unavailable: " + failure.getClass().getSimpleName());
        }
    }
    /** Accumulated DX/DY are independent of this event queue; only redundant motion is removed. */
    public static void filterRead(ByteBuffer destination, ByteBuffer scratch, java.util.function.Consumer<ByteBuffer> read) {
        while (destination.remaining() >= Mouse.EVENT_SIZE) {
            scratch.clear(); scratch.limit(Math.min(scratch.capacity(), destination.remaining()));
            read.accept(scratch); scratch.flip();
            if (!scratch.hasRemaining()) return;
            while (scratch.remaining() >= Mouse.EVENT_SIZE) {
                int start = scratch.position();
                boolean control = scratch.get(start) >= 0 || scratch.getInt(start + 10) != 0;
                if (control) {
                    ByteBuffer event = scratch.duplicate(); event.position(start); event.limit(start + Mouse.EVENT_SIZE);
                    destination.put(event);
                }
                scratch.position(start + Mouse.EVENT_SIZE);
            }
        }
    }
    public static void close() {
        try {
            Field implementation = field(Mouse.class, "implementation");
            if (installed != null && implementation.get(null) == installed) implementation.set(null, original);
        } catch (Exception ignored) {}
        installed = null; original = null;
    }
}
