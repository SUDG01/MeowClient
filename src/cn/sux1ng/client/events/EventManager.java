package cn.sux1ng.client.events;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Synchronous, exact-event-class dispatch. Listener methods must declare one Event parameter
 * and carry @EventTarget. Packet receive listeners run on Netty's thread; Minecraft world and
 * GUI changes must be scheduled onto the main thread by the listener.
 */
public class EventManager {
    // 存储所有注册的方法：Key是事件类型(如EventUpdate.class)，Value是监听这个事件的所有方法列表
    private static final ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArrayList<Handler>> REGISTRY_MAP = new ConcurrentHashMap<>();

    // 1. 注册 (Register)
    // 当你开启模块时，把模块里标了 @EventTarget 的方法存进来
    public static synchronized void register(Object o) {
        for (Method method : o.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventTarget.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1 || !Event.class.isAssignableFrom(parameterTypes[0])) {
                    throw new IllegalArgumentException("@EventTarget needs one Event parameter: " + method);
                }
                method.setAccessible(true);
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) parameterTypes[0];
                CopyOnWriteArrayList<Handler> handlers = REGISTRY_MAP.computeIfAbsent(eventClass, key -> new CopyOnWriteArrayList<>());
                boolean registered = false;
                for (Handler handler : handlers) {
                    if (handler.parent == o && handler.method.equals(method)) {
                        registered = true;
                        break;
                    }
                }
                if (!registered) handlers.add(new Handler(method, o));
            }
        }
    }

    // 2. 注销 (Unregister)
    // 当你关闭模块时，把它的方法移除
    public static synchronized void unregister(Object o) {
        for (CopyOnWriteArrayList<Handler> handlers : REGISTRY_MAP.values()) {
            handlers.removeIf(h -> h.parent == o);
        }
    }

    // 3. 调用 (Call)
    // 游戏每tick都会调用这个方法，把事件分发给所有模块
    public static void call(Event event) {
        // Handlers run synchronously on the caller's thread. Packet receive callers use Netty's thread.
        CopyOnWriteArrayList<Handler> handlers = REGISTRY_MAP.get(event.getClass());
        if (handlers != null) {
            for (Handler h : handlers) {
                try {
                    h.method.invoke(h.parent, event);
                } catch (IllegalAccessException | InvocationTargetException failure) {
                    Throwable cause = failure instanceof InvocationTargetException
                            ? ((InvocationTargetException) failure).getTargetException() : failure;
                    cause.printStackTrace();
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
