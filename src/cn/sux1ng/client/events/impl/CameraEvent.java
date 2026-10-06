package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;

/** Once per frame, after user mouse input and before the world camera is assembled. */
public final class CameraEvent extends Event {
    public final float partialTicks, mouseYaw, mousePitch;
    public final boolean focused;
    public CameraEvent(float partialTicks, float mouseYaw, float mousePitch, boolean focused) {
        this.partialTicks = partialTicks; this.mouseYaw = mouseYaw; this.mousePitch = mousePitch; this.focused = focused;
    }
}
