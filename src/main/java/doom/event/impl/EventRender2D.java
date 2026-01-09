package doom.event.impl;

import doom.event.Event;

public class EventRender2D extends Event {
    public float partialTicks;

    public EventRender2D(float partialTicks) {
        this.partialTicks = partialTicks;
    }
}