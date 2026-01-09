package doom.event.impl;

import doom.event.Event;
import net.minecraft.network.Packet;

public class EventPacket extends Event {
    private Packet<?> packet;
    private final Direction direction;

    public EventPacket(Packet<?> packet, Direction direction) {
        this.packet = packet;
        this.direction = direction;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public Direction getDirection() {
        return direction;
    }

    public enum Direction {
        SEND,
        RECEIVE
    }
}