package cn.sux1ng.client.mod;

import cn.sux1ng.client.events.EventManager;
import cn.sux1ng.client.events.impl.MotionEvent;
import cn.sux1ng.client.events.impl.MoveEvent;
import cn.sux1ng.client.events.impl.PacketReceiveEvent;
import cn.sux1ng.client.events.impl.PacketSendEvent;
import cn.sux1ng.client.events.impl.StrafeEvent;
import cn.sux1ng.client.ui.notification.NotificationType;
import cn.sux1ng.client.value.Value;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class Mod {
    protected final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final Category category;

    private boolean enable;

    private int key;

    public Mod(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    // 动态后缀（显示在 ArrayList 中，如 "KillAura [Switch]")
    private String suffix;

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public List<Value> values = new ArrayList<>();

    public String getName() {
        return name;
    }

    public boolean isEnable() {
        return enable;
    }

    // ================== 核心修改在这里 ==================
    public void setEnable(boolean enable) {
        if (this.enable == enable) return;

        this.enable = enable;

        if (enable) {
            EventManager.register(this);
            enable();

            if (mc.thePlayer != null){
                cn.sux1ng.client.ui.notification.NotificationManager.show("Module",this.getName() + " Enabled!", NotificationType.SUCCESS);
            }
        } else {
            EventManager.unregister(this);
            disable();

            cn.sux1ng.client.ui.notification.NotificationManager.show("Module",this.getName() + " Disabled!", NotificationType.ERROR);
        }
    }
    // ==================================================

    public void addValues(Value... v) {
        for (Value value : v) {
            this.values.add(value);
        }
    }

    public List<Value> getValues() {
        return values;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Category getCategory() {
        return category;
    }

    public void draw(){

    }

    public void enable(){

    }

    public void disable(){

    }

    public void render(float partialTicks){

    }

    public void update(){

    }

    public void key(int key){

    }

    // ================== 事件回调（模块可覆写） ==================
    public void onPreMotion(MotionEvent e) {}
    public void onPostMotion(MotionEvent e) {}
    public void onPacketSend(PacketSendEvent e) {}
    public void onPacketReceive(PacketReceiveEvent e) {}
    public void onMove(MoveEvent e) {}
    public void onStrafe(StrafeEvent e) {}
}