package cn.sux1ng.client.input;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import java.util.Arrays;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** One hidden message window receives mouse-only WM_INPUT, independently of render FPS. */
public final class WindowsRawMouseInput implements AutoCloseable {
    private static final int WM_INPUT = 0xFF, WM_CLOSE = 0x10, WM_DESTROY = 2, RESET_CAPTURE = 0x8001, RID_INPUT = 0x10000003;
    private final MotionAccumulator motion = new MotionAccumulator();
    private final Thread thread;
    private volatile boolean running = true, available, absolute, captureReady;
    private volatile WinDef.HWND window;
    private volatile User32 api;
    private Memory packet = new Memory(256), batch = new Memory(65536);
    private final ByteBuffer packetView = packet.getByteBuffer(0, packet.size()).order(ByteOrder.nativeOrder());
    private ByteBuffer batchView = batch.getByteBuffer(0, batch.size()).order(ByteOrder.nativeOrder());
    private final int header = 8 + Native.POINTER_SIZE * 2;
    private final WindowProc callback = this::windowProc;

    public WindowsRawMouseInput() {
        thread = new Thread(this::run, "MeowClient raw mouse"); thread.setDaemon(true); thread.start();
    }
    public boolean isAvailable() { return available && !absolute; }
    public synchronized void setCaptured(boolean captured) {
        if (motion.isCaptured() == captured) return;
        captureReady = false; absolute = false; motion.setCaptured(captured);
        if (captured) requestReset();
    }
    private void requestReset() {
        User32 current = api; WinDef.HWND hwnd = window;
        if (current != null && hwnd != null) current.PostMessageW(hwnd, RESET_CAPTURE, new WinDef.WPARAM(motion.epoch()), new WinDef.LPARAM(0));
    }
    public void reset() { captureReady = false; motion.clear(); if (motion.isCaptured()) requestReset(); }
    public int[] consume() { return isAvailable() ? (captureReady ? motion.consume() : new int[]{0, 0}) : null; }

    private void run() {
        WString name = new WString("MeowClientRawMouse" + System.identityHashCode(this));
        Pointer module = null;
        boolean registeredClass = false;
        try {
            api = (User32) Native.loadLibrary("user32", User32.class);
            Kernel32 kernel = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
            module = kernel.GetModuleHandleW(null);
            WindowClass type = new WindowClass();
            type.cbSize = type.size(); type.lpfnWndProc = callback; type.hInstance = module; type.lpszClassName = name;
            if (api.RegisterClassExW(type) == 0) throw new IllegalStateException("RegisterClassEx: " + Native.getLastError());
            registeredClass = true;
            WinDef.HWND parent = new WinDef.HWND(); parent.setPointer(Pointer.createConstant(-3));
            window = api.CreateWindowExW(0, name, name, 0, 0, 0, 0, 0, parent, null, module, null);
            if (window == null) throw new IllegalStateException("CreateWindowEx: " + Native.getLastError());
            RawDevice device = new RawDevice(); device.hwndTarget = window; device.flags = 0x100 | 0x2000; device.write();
            if (!api.RegisterRawInputDevices(device.getPointer(), 1, device.size())) throw new IllegalStateException("RegisterRawInputDevices: " + Native.getLastError());
            available = true;
            if (motion.isCaptured()) requestReset();
            WinUser.MSG message = new WinUser.MSG();
            while (running) {
                int result = api.GetMessageW(message, null, 0, 0);
                if (result <= 0 || !running) break;
                api.TranslateMessage(message); api.DispatchMessageW(message);
            }
        } catch (Throwable failure) {
            available = false;
            System.err.println("Raw mouse input unavailable: " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        } finally {
            available = false;
            if (api != null && window != null) {
                RawDevice remove = new RawDevice(); remove.flags = 1; remove.write();
                api.RegisterRawInputDevices(remove.getPointer(), 1, remove.size());
                api.DestroyWindow(window);
            }
            if (registeredClass) api.UnregisterClassW(name, module);
            window = null; motion.setCaptured(false);
        }
    }
    private WinDef.LRESULT windowProc(WinDef.HWND hwnd, int message, WinDef.WPARAM wparam, WinDef.LPARAM lparam) {
        try {
            if (message == RESET_CAPTURE) {
                if (motion.isCaptured() && wparam.longValue() == motion.epoch()) { motion.clear(); captureReady = true; }
                return new WinDef.LRESULT(0);
            }
            if (message == WM_INPUT && running) {
                long token = motion.epoch();
                IntByReference size = new IntByReference((int) packet.size());
                int bytes = api.GetRawInputData(Pointer.createConstant(lparam.longValue()), RID_INPUT, packet, size, header);
                if (bytes == -1) throw new IllegalStateException("GetRawInputData: " + Native.getLastError());
                if (bytes >= header + 24) accept(packetView, 0, bytes, token);
                // 32-bit uses individual reads to avoid the documented WOW64 buffer layout difference.
                if (Native.POINTER_SIZE == 8) drainBuffered(token);
            }
            if (message == WM_CLOSE) { api.DestroyWindow(hwnd); return new WinDef.LRESULT(0); }
            if (message == WM_DESTROY) { api.PostQuitMessage(0); return new WinDef.LRESULT(0); }
        } catch (Throwable failure) {
            available = false; running = false;
            System.err.println("Raw mouse read failed: " + failure.getClass().getSimpleName());
            api.PostQuitMessage(0);
        }
        return api.DefWindowProcW(hwnd, message, wparam, lparam);
    }
    private void drainBuffered(long token) {
        for (;;) {
            IntByReference size = new IntByReference((int) batch.size());
            int count = api.GetRawInputBuffer(batch, size, header);
            if (count == 0) return;
            if (count == -1) {
                if (size.getValue() > batch.size() && size.getValue() <= 8 * 1024 * 1024) {
                    batch = new Memory(size.getValue()); batchView = batch.getByteBuffer(0, batch.size()).order(ByteOrder.nativeOrder()); continue;
                }
                throw new IllegalStateException("GetRawInputBuffer: " + Native.getLastError());
            }
            long offset = 0;
            for (int i = 0; i < count; i++) {
                if (offset + header > batch.size()) throw new IllegalStateException("Raw input header bounds");
                int bytes = batchView.getInt((int)offset + 4);
                if (bytes < header || offset + bytes > batch.size()) throw new IllegalStateException("Raw input packet bounds");
                accept(batchView, (int)offset, bytes, token);
                offset = (offset + bytes + Native.POINTER_SIZE - 1) & -((long) Native.POINTER_SIZE);
            }
        }
    }
    private void accept(ByteBuffer data, int offset, int bytes, long token) {
        if (data.getInt(offset) != 0 || bytes < header + 24 || !captureReady || !motion.isCaptured() || token != motion.epoch()) return;
        if ((data.getShort(offset + header) & 1) != 0) { absolute = true; motion.clear(); return; }
        if (absolute) { absolute = false; motion.clear(); return; }
        motion.add(data.getInt(offset + header + 12), data.getInt(offset + header + 16), token);
    }
    @Override public void close() {
        running = false; available = false; motion.setCaptured(false);
        User32 current = api; WinDef.HWND hwnd = window;
        if (current != null && hwnd != null) current.PostMessageW(hwnd, WM_CLOSE, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
        try { thread.join(1000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }

    public interface WindowProc extends StdCallLibrary.StdCallCallback {
        WinDef.LRESULT invoke(WinDef.HWND hwnd, int message, WinDef.WPARAM wparam, WinDef.LPARAM lparam);
    }
    public interface User32 extends StdCallLibrary {
        short RegisterClassExW(WindowClass type);
        boolean UnregisterClassW(WString name, Pointer instance);
        WinDef.HWND CreateWindowExW(int extended, WString type, WString title, int style, int x, int y, int width, int height, WinDef.HWND parent, Pointer menu, Pointer instance, Pointer parameter);
        boolean DestroyWindow(WinDef.HWND hwnd);
        int GetMessageW(WinUser.MSG message, WinDef.HWND hwnd, int min, int max);
        boolean TranslateMessage(WinUser.MSG message);
        WinDef.LRESULT DispatchMessageW(WinUser.MSG message);
        WinDef.LRESULT DefWindowProcW(WinDef.HWND hwnd, int message, WinDef.WPARAM wparam, WinDef.LPARAM lparam);
        boolean PostMessageW(WinDef.HWND hwnd, int message, WinDef.WPARAM wparam, WinDef.LPARAM lparam);
        void PostQuitMessage(int status);
        boolean RegisterRawInputDevices(Pointer devices, int count, int size);
        int GetRawInputData(Pointer input, int command, Pointer data, IntByReference size, int header);
        int GetRawInputBuffer(Pointer data, IntByReference size, int header);
    }
    public interface Kernel32 extends StdCallLibrary { Pointer GetModuleHandleW(WString name); }
    public static final class WindowClass extends Structure {
        public int cbSize, style;
        public WindowProc lpfnWndProc;
        public int cbClsExtra, cbWndExtra;
        public Pointer hInstance, hIcon, hCursor, hbrBackground;
        public WString lpszMenuName, lpszClassName;
        public Pointer hIconSm;
        @Override protected List<String> getFieldOrder() { return Arrays.asList("cbSize", "style", "lpfnWndProc", "cbClsExtra", "cbWndExtra", "hInstance", "hIcon", "hCursor", "hbrBackground", "lpszMenuName", "lpszClassName", "hIconSm"); }
    }
    public static final class RawDevice extends Structure {
        public short page = 1, usage = 2;
        public int flags;
        public WinDef.HWND hwndTarget;
        @Override protected List<String> getFieldOrder() { return Arrays.asList("page", "usage", "flags", "hwndTarget"); }
    }
}
