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

    // 按键绑定模式
    public enum BindMode { TOGGLE, HOLD, SMART }

    private final String name;
    private final Category category;
    private boolean enable;
    private int key;
    private BindMode bindMode = BindMode.TOGGLE;
    private boolean smartHolding = false;  // SMART 模式下的长按状态

    public Mod(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    // ================== 后缀 / Tag ==================

    private String suffix;
    private Value<?> tagSource;  // 自动 tag 来源

    public String getSuffix() {
        // tagBy 关联的值优先
        if (tagSource != null && tagSource.getValue() != null) {
            return tagSource.getValue().toString();
        }
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * 关联一个 Value 作为自动 tag
     * 比如 KillAura.tagBy(targetMode) → ArrayList 里显示 "KillAura [Switch]"
     */
    public void tagBy(Value<?> source) {
        this.tagSource = source;
    }

    // ================== 绑定模式 ==================

    public BindMode getBindMode() { return bindMode; }
    public void setBindMode(BindMode mode) { this.bindMode = mode; }
    public boolean isSmartHolding() { return smartHolding; }
    public void setSmartHolding(boolean h) { this.smartHolding = h; }

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