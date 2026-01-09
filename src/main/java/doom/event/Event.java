package doom.event;

import java.util.ArrayList;

public class Event {

    private boolean cancelled;

    // Tę metodę wywołujemy, żeby odpalić event
    public Event call() {
        this.cancelled = false;
        EventManager.call(this);
        return this;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}