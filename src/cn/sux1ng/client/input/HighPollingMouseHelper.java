package cn.sux1ng.client.input;

import net.minecraft.util.MouseHelper;
import java.util.Objects;
import java.util.function.Supplier;

/** Hardware counts replace window cursor deltas; vanilla sensitivity is applied by EntityRenderer. */
public final class HighPollingMouseHelper extends MouseHelper {
    private final MouseHelper previous;
    private final Supplier<int[]> raw;
    public HighPollingMouseHelper(MouseHelper previous, Supplier<int[]> raw) {
        this.previous = Objects.requireNonNull(previous, "previous");
        this.raw = Objects.requireNonNull(raw, "raw");
    }
    @Override public void mouseXYChange() {
        // Always drain the standard deltas, so disabling RawInput never replays old movement.
        previous.mouseXYChange();
        int[] motion = raw.get();
        if (motion == null) { deltaX = previous.deltaX; deltaY = previous.deltaY; }
        else { deltaX = motion[0]; deltaY = -motion[1]; } // Windows Y down -> Minecraft Y up.
    }
    @Override public void grabMouseCursor() {
        previous.grabMouseCursor(); deltaX = deltaY = 0;
        HighPollingInput.resetCapture();
    }
    @Override public void ungrabMouseCursor() {
        HighPollingInput.releaseCapture();
        previous.ungrabMouseCursor(); deltaX = deltaY = 0;
    }
}
