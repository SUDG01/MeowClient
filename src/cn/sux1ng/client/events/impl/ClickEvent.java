package cn.sux1ng.client.events.impl;

import cn.sux1ng.client.events.Event;

public class ClickEvent extends Event {
    private ClickType type;

    public void setType (ClickType type){
        this.type = type;
    }

    public ClickEvent(ClickType type){
        this.type = type;
    }

    public ClickType getType() {
        return this.type;
    }

    public enum ClickType {
        LEFT, RIGHT, MIDDLE;
    }
}
