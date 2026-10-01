package cn.sux1ng.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import java.util.Locale;

/** Client setting and lifetime for raw camera motion; game buttons remain on LWJGL. */
public final class HighPollingInput {
    private static volatile boolean enabled = true;
    private static WindowsRawMouseInput raw;
    private static MouseHelper previous, installed;
    private HighPollingInput() {}
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled = value;
        if (raw != null) { raw.setCaptured(false); raw.reset(); }
    }
    public static boolean isAvailable() { return raw != null && raw.isAvailable(); }
    public static void start() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || previous != null) return;
        MouseEventBuffers.install();
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            try { raw = new WindowsRawMouseInput(); }
            catch (LinkageError | RuntimeException failure) { System.err.println("Raw mouse initialization unavailable: " + failure.getClass().getSimpleName()); }
        }
        previous = mc.mouseHelper;
        installed = new HighPollingMouseHelper(previous, () -> enabled && raw != null ? raw.consume() : null);
        mc.mouseHelper = installed;
    }
    public static void beginFrame(Minecraft mc) {
        if (!Display.isCreated()) return;
        // Fetch messages before the tick/render consumes input, instead of waiting for the end of the frame.
        Display.processMessages();
        if (Mouse.isCreated()) Mouse.poll();
        if (Keyboard.isCreated()) Keyboard.poll();
        if (raw != null) raw.setCaptured(enabled && Display.isActive() && mc.inGameHasFocus
                && mc.currentScreen == null && mc.thePlayer != null && Mouse.isCreated() && Mouse.isGrabbed());
    }
    public static void resetCapture() { if (raw != null) { raw.setCaptured(false); raw.reset(); } }
    public static void releaseCapture() { if (raw != null) raw.setCaptured(false); }
    public static void stop() {
        if (raw != null) { raw.close(); raw = null; }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.mouseHelper == installed) mc.mouseHelper = previous;
        previous = installed = null; MouseEventBuffers.close();
    }
}
