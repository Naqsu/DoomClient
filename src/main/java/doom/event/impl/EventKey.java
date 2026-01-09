package doom.event.impl;

import doom.event.Event;

public class EventKey extends Event {
    public int key;

    public EventKey(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}