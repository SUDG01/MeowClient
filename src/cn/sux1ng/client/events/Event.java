package cn.sux1ng.client.events;

public abstract class Event {
    private boolean cancelled;

    // Cancellation only takes effect when the source hook checks this flag (for example, packet hooks).
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
