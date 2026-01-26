package doom.event.impl;

import doom.event.Event;

public class EventUpdate extends Event {

    private float yaw;
    private float pitch;
    private boolean onGround;
    private boolean isGroundSpoofed;
    private boolean cancelled;
    // Movement Input fields
    private float moveForward;
    private float moveStrafe;
    private boolean jump;
    private boolean sneak;
    private boolean sprint;

    // Nowe pole – określa fazę eventu
    private final boolean pre;   // <--- DODANE

    public EventUpdate(float yaw, float pitch, boolean onGround,
                       float moveForward, float moveStrafe,
                       boolean jump, boolean sneak, boolean sprint,
                       boolean pre) {                  // <--- DODANY parametr
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.isGroundSpoofed = false;
        this.moveForward = moveForward;
        this.moveStrafe = moveStrafe;
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = sprint;
        this.pre = pre;
    }

    // Getters
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public boolean isOnGround() { return onGround; }
    public boolean isGroundSpoofed() { return isGroundSpoofed; }
    public float getMoveForward() { return moveForward; }
    public float getMoveStrafe() { return moveStrafe; }
    public boolean isJumping() { return jump; }
    public boolean isSneaking() { return sneak; }
    public boolean isSprinting() { return sprint; }
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    public boolean isPre() { return pre; }          // <--- NOWA METODA

    // Setters
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
        this.isGroundSpoofed = true;
    }

    public void setMoveForward(float moveForward) { this.moveForward = moveForward; }
    public void setMoveStrafe(float moveStrafe) { this.moveStrafe = moveStrafe; }
    public void setSneaking(boolean sneak) { this.sneak = sneak; }
    public void setSprinting(boolean sprint) { this.sprint = sprint; }
}