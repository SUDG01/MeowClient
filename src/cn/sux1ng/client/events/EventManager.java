package cn.sux1ng.client.events;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    // 存储所有注册的方法：Key是事件类型(如EventUpdate.class)，Value是监听这个事件的所有方法列表
    private static final Map<Class<? extends Event>, List<Handler>> REGISTRY_MAP = new HashMap<>();

    // 1. 注册 (Register)
    // 当你开启模块时，把模块里标了 @EventTarget 的方法存进来
    public static void register(Object o) {
        for (Method method : o.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventTarget.class)) {
                // 找到参数里的事件类型 (例如 EventUpdate)
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) continue;

                Class<? extends Event> eventClass = (Class<? extends Event>) parameterTypes[0];

                if (!REGISTRY_MAP.containsKey(eventClass)) {
                    REGISTRY_MAP.put(eventClass, new CopyOnWriteArrayList<>());
                }

                REGISTRY_MAP.get(eventClass).add(new Handler(method, o));
            }
        }
    }

    // 2. 注销 (Unregister)
    // 当你关闭模块时，把它的方法移除
    public static void unregister(Object o) {
        for (List<Handler> handlers : REGISTRY_MAP.values()) {
            handlers.removeIf(h -> h.parent == o);
        }
    }

    // 3. 调用 (Call)
    // 游戏每tick都会调用这个方法，把事件分发给所有模块
    public static void call(Event event) {
        List<Handler> handlers = REGISTRY_MAP.get(event.getClass());
        if (handlers != null) {
            for (Handler h : handlers) {
                try {
                    h.method.setAccessible(true);
                    h.method.invoke(h.parent, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 内部类，用来包装方法和对象
    private static class Handler {
        public Method method;
        public Object parent;

        public Handler(Method method, Object parent) {
            this.method = method;
            this.parent = parent;
        }
    }
}