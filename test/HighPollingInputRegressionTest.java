import net.minecraft.util.MouseHelper;
import org.lwjgl.input.Mouse;
import sun.misc.Unsafe;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import cn.sux1ng.client.input.MotionAccumulator;
import cn.sux1ng.client.input.MouseEventBuffers;
import cn.sux1ng.client.input.HighPollingInput;
import cn.sux1ng.client.config.configs.ClientConfig;

/** Replays relative reports against the real LWJGL Windows queue and Minecraft MouseHelper. */
public class HighPollingInputRegressionTest {
    private static final Unsafe UNSAFE = unsafe();
    private static int failures;

    public static void main(String[] args) throws Exception {
        set(Mouse.class, null, "created", true);
        set(Mouse.class, null, "isGrabbed", true);
        for (int hz : new int[]{1000, 2000, 4000, 8000, 16000, 32000}) {
            check(hz + " Hz preserves hardware motion when window coordinates clip", () -> {
                Object windows = windowsMouse();
                Method moved = method(windows.getClass(), "handleMouseMoved", int.class, int.class, long.class);
                int reports = hz / 10;
                for (int i = 1; i <= reports; i++) {
                    // Same physical path, split into different numbers of HID reports.
                    int rawX = 1024 * i / reports, rawY = 512 * i / reports;
                    moved.invoke(windows, Math.min(50, rawX), -Math.min(50, rawY), (long) i);
                }
                set(Mouse.class, null, "dx", field(windows, "accum_dx"));
                set(Mouse.class, null, "dy", field(windows, "accum_dy"));
                int[] hardware = {1024, 512};
                Supplier<int[]> source = () -> {
                    int[] result = hardware.clone(); hardware[0] = hardware[1] = 0; return result;
                };
                MouseHelper helper = helper(source);
                helper.mouseXYChange();
                require(helper.deltaX == 1024 && helper.deltaY == -512,
                        "expected (1024,-512), read (" + helper.deltaX + "," + helper.deltaY + ")");
                helper.mouseXYChange();
                require(helper.deltaX == 0 && helper.deltaY == 0, "motion was applied twice");
            });
            check(hz + " Hz movement does not discard mouse buttons", () -> {
                Object windows = windowsMouse();
                growIfAvailable(windows);
                Method moved = method(windows.getClass(), "handleMouseMoved", int.class, int.class, long.class);
                for (int i = 0; i < hz / 20; i++) moved.invoke(windows, i + 1, 0, (long) i);
                method(windows.getClass(), "handleMouseButton", byte.class, byte.class, long.class).invoke(windows, (byte) 0, (byte) 1, 100L);
                method(windows.getClass(), "handleMouseButton", byte.class, byte.class, long.class).invoke(windows, (byte) 0, (byte) 0, 101L);
                ByteBuffer data = ByteBuffer.allocate(8192 * Mouse.EVENT_SIZE);
                method(windows.getClass(), "read", ByteBuffer.class).invoke(windows, data);
                data.flip();
                int presses = 0, releases = 0;
                while (data.remaining() >= Mouse.EVENT_SIZE) {
                    int button = data.get(); boolean down = data.get() != 0;
                    data.position(data.position() + Mouse.EVENT_SIZE - 2);
                    if (button == 0) { if (down) presses++; else releases++; }
                }
                require(presses == 1 && releases == 1,
                        "press/release events after movement: " + presses + "/" + releases);
            });
        }
        check("unavailable raw input preserves vanilla deltas", () -> {
            set(Mouse.class, null, "dx", 17); set(Mouse.class, null, "dy", -9);
            MouseHelper helper = helper(() -> null); helper.mouseXYChange();
            require(helper.deltaX == 17 && helper.deltaY == -9, "fallback changed vanilla motion");
        });
        check("focus epochs discard previous camera motion", () -> {
            MotionAccumulator motion = new MotionAccumulator();
            motion.setCaptured(true); long old = motion.epoch(); motion.add(100, 50, old);
            motion.setCaptured(false); motion.add(1000, 1000, old);
            motion.setCaptured(true); motion.add(2000, 2000, old);
            motion.add(7, -3, motion.epoch()); int[] result = motion.consume();
            require(result[0] == 7 && result[1] == -3, "unfocused/late motion leaked into the new capture");
        });
        check("grabbed event filtering preserves button and wheel order", () -> {
            Object windows = windowsMouse(); growIfAvailable(windows);
            Method moved = method(windows.getClass(), "handleMouseMoved", int.class, int.class, long.class);
            for (int i = 0; i < 1600; i++) moved.invoke(windows, i + 1, 0, (long)i);
            method(windows.getClass(), "handleMouseButton", byte.class, byte.class, long.class).invoke(windows, (byte)0, (byte)1, 2000L);
            method(windows.getClass(), "handleMouseScrolled", int.class, long.class).invoke(windows, 120, 2001L);
            method(windows.getClass(), "handleMouseButton", byte.class, byte.class, long.class).invoke(windows, (byte)0, (byte)0, 2002L);
            ByteBuffer filtered = ByteBuffer.allocate(3 * Mouse.EVENT_SIZE);
            MouseEventBuffers.filterRead(filtered, ByteBuffer.allocate(64 * Mouse.EVENT_SIZE), buffer -> {
                try { method(windows.getClass(), "read", ByteBuffer.class).invoke(windows, buffer); }
                catch (Exception failure) { throw new RuntimeException(failure); }
            });
            require(filtered.position() == 3 * Mouse.EVENT_SIZE, "controls were blocked by motion events");
            require(filtered.get(0) == 0 && filtered.get(1) == 1 && filtered.getInt(Mouse.EVENT_SIZE + 10) == 120
                    && filtered.get(Mouse.EVENT_SIZE * 2) == 0 && filtered.get(Mouse.EVENT_SIZE * 2 + 1) == 0, "control order changed");
        });
        check("both axes are consumed atomically under concurrent input", () -> {
            MotionAccumulator motion = new MotionAccumulator(); motion.setCaptured(true);
            Thread writer = new Thread(() -> { for (int i = 0; i < 100000; i++) motion.add(1, 2, motion.epoch()); });
            writer.start(); long x = 0, y = 0;
            while (writer.isAlive()) { int[] v = motion.consume(); require(v[1] == v[0] * 2, "axes came from different batches"); x += v[0]; y += v[1]; }
            writer.join(); int[] last = motion.consume(); x += last[0]; y += last[1];
            require(x == 100000 && y == 200000, "reports were lost or consumed twice");
        });
        check("RawInput client setting survives saving", () -> {
            java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("meow-raw-input-");
            java.nio.file.Path file = directory.resolve("Client.json");
            try {
                HighPollingInput.setEnabled(false); new ClientConfig().saveTo(file);
                HighPollingInput.setEnabled(true); new ClientConfig().loadFrom(file);
                require(!HighPollingInput.isEnabled(), "raw input setting was lost");
            } finally { java.nio.file.Files.deleteIfExists(file); java.nio.file.Files.deleteIfExists(directory); HighPollingInput.setEnabled(true); }
        });
        if (Boolean.getBoolean("meow.test.nativeInput")) check("LWJGL installation forwards controls through the real Windows display interface", () -> {
            org.lwjgl.Sys.initialize();
            Object windows = windowsMouse();
            Class<?> displayType = Class.forName("org.lwjgl.opengl.WindowsDisplay");
            Object display = UNSAFE.allocateInstance(displayType);
            set(displayType, display, "mouse", windows);
            ByteBuffer empty = ByteBuffer.allocate(Mouse.EVENT_SIZE * 50); empty.limit(0);
            set(Mouse.class, null, "readBuffer", empty); set(Mouse.class, null, "implementation", display);
            MouseEventBuffers.install();
            Object wrapper = fieldStatic(Mouse.class, "implementation");
            require(wrapper != display, "Windows display input wrapper was not installed");
            method(windows.getClass(), "handleMouseMoved", int.class, int.class, long.class).invoke(windows, 20, 10, 0L);
            method(windows.getClass(), "handleMouseButton", byte.class, byte.class, long.class).invoke(windows, (byte)0, (byte)1, 1L);
            ByteBuffer output = ByteBuffer.allocate(100);
            ((org.lwjgl.opengl.InputImplementation)wrapper).readMouse(output);
            require(output.position() == Mouse.EVENT_SIZE && output.get(0) == 0 && output.get(1) == 1, "installed wrapper discarded the button");
            MouseEventBuffers.close();
            require(fieldStatic(Mouse.class, "implementation") == display, "original implementation was not restored");
        });
        check("native raw packet decoding preserves signed hardware axes", () -> {
            Class<?> type = cn.sux1ng.client.input.WindowsRawMouseInput.class;
            Object raw = UNSAFE.allocateInstance(type);
            MotionAccumulator motion = new MotionAccumulator(); motion.setCaptured(true);
            int header = 8 + com.sun.jna.Native.POINTER_SIZE * 2;
            set(type, raw, "motion", motion); set(type, raw, "header", header); set(type, raw, "captureReady", true);
            ByteBuffer packet = ByteBuffer.allocate(header + 24).order(java.nio.ByteOrder.nativeOrder());
            packet.putInt(4, packet.capacity()); packet.putInt(header + 12, -1024); packet.putInt(header + 16, 512);
            method(type, "accept", ByteBuffer.class, int.class, int.class, long.class).invoke(raw, packet, 0, packet.capacity(), motion.epoch());
            int[] value = motion.consume(); require(value[0] == -1024 && value[1] == 512, "raw packet sign/offset is incorrect");
        });
        set(Mouse.class, null, "created", false);
        if (failures != 0) throw new AssertionError(failures + " input checks failed");
        System.out.println("High polling input checks passed");
    }

    private static MouseHelper helper(Supplier<int[]> source) throws Exception {
        try {
            return (MouseHelper) Class.forName("cn.sux1ng.client.input.HighPollingMouseHelper")
                    .getConstructor(MouseHelper.class, Supplier.class).newInstance(new MouseHelper(), source);
        } catch (ClassNotFoundException originalVersion) { return new MouseHelper(); }
    }
    private static void growIfAvailable(Object windows) throws Exception {
        try { method(Class.forName("cn.sux1ng.client.input.MouseEventBuffers"), "growNativeQueue", Object.class).invoke(null, windows); }
        catch (ClassNotFoundException originalVersion) {}
    }
    private static Object windowsMouse() throws Exception {
        Class<?> type = Class.forName("org.lwjgl.opengl.WindowsMouse");
        Object mouse = UNSAFE.allocateInstance(type);
        Class<?> queueType = Class.forName("org.lwjgl.opengl.EventQueue");
        Constructor<?> ctor = queueType.getDeclaredConstructor(int.class); ctor.setAccessible(true);
        set(type, mouse, "event_queue", ctor.newInstance(Mouse.EVENT_SIZE));
        set(type, mouse, "mouse_event", ByteBuffer.allocate(Mouse.EVENT_SIZE));
        set(type, mouse, "button_states", new byte[5]);
        set(type, mouse, "mouse_grabbed", true);
        return mouse;
    }
    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters); method.setAccessible(true); return method;
    }
    private static Object field(Object instance, String name) throws Exception {
        Field field = instance.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(instance);
    }
    private static Object fieldStatic(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(null);
    }
    private static void set(Class<?> type, Object instance, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(instance, value);
    }
    private static Unsafe unsafe() {
        try { Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true); return (Unsafe) field.get(null); }
        catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private interface Checked { void run() throws Exception; }
    private static void check(String name, Checked test) {
        try { test.run(); System.out.println("PASS " + name); }
        catch (Throwable failure) { failures++; System.err.println("FAIL " + name + ": " + failure); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
