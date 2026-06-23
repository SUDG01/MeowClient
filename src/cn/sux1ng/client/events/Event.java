package cn.sux1ng.client.events;

public abstract class Event {
    private boolean cancelled;

    // 只有部分事件(如发包)需要取消，Update事件通常不需要
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}