package doom.event.impl;

import doom.event.Event;

public class EventChat extends Event {
    private String message;
    private boolean cancelled;

    public EventChat(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}