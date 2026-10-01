package cn.sux1ng.client;

import cn.sux1ng.client.command.CommandManager;
import cn.sux1ng.client.config.ConfigManager;
import cn.sux1ng.client.events.EventRender2D;
import cn.sux1ng.client.events.EventTarget;
import cn.sux1ng.client.events.EventUpdate;
import cn.sux1ng.client.gui.clickgui.ClickGUI;
import cn.sux1ng.client.mod.ModManager;
import org.lwjgl.opengl.Display;

public class MeowClient {
    public static final String NAME = "MeowClient";
    public static final String VERSION = "R6";

    // 【关键修改 1】定义 instance！
    // 这行代码的意思是：创建一个 MeowClient 的对象，起名叫 instance
    public static MeowClient instance = new MeowClient();

    public static ModManager modManager;
    public static ConfigManager configManager;
    public static CommandManager commandManager;

    public static ClickGUI clickGUI;

    public static void start(){
        modManager = new ModManager();
        configManager = new ConfigManager();
        commandManager = new CommandManager();

        modManager.load();
        clickGUI = new ClickGUI();
        configManager.load();
        cn.sux1ng.client.input.HighPollingInput.start();
        commandManager.load();

        // 【关键修改 2】现在 instance 存在了，可以注册了！
        // 这样下面的 onRender2D 才能接收到消息
        cn.sux1ng.client.events.EventManager.register(instance);

        Display.setTitle(NAME + " | " + VERSION);
    }

    public static void stop(){
        cn.sux1ng.client.input.HighPollingInput.stop();
        if (configManager != null){
            configManager.save();
        }
        // 养成好习惯，关闭时取消注册
        cn.sux1ng.client.events.EventManager.unregister(instance);
    }

    // 这是一个实例方法，必须通过对象(instance)来调用
    @EventTarget
    public void onRender2D(EventRender2D event) {
        // 画 Notification
        cn.sux1ng.client.ui.notification.NotificationManager.render();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (event.isPre() && configManager != null) {
            configManager.activatePendingModules();
        }
    }
}
