package doom.module.impl.movement;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Category;
import doom.module.Module;
import doom.module.impl.combat.Killaura; // Import Killaura
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.MoveUtil;
import doom.util.TimerUtil;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import org.lwjgl.input.Keyboard;

public class Speed extends Module {

    public ModeSetting mode;
    public NumberSetting speedSettings;
    public BooleanSetting timerBoost;
    public BooleanSetting autoJump;

    // Zmienne do logiki
    private double moveSpeed;
    private double lastDist;
    private int stage;
    private int grimTicks = 0;

    public Speed() {
        super("Speed", Keyboard.KEY_X, Category.MOVEMENT);

        mode = new ModeSetting("Mode", this, "Grim", "Grim", "Strafe", "Verus", "Vulcan");
        speedSettings = new NumberSetting("Speed", this, 1.0, 0.2, 5.0, 0.1);
        timerBoost = new BooleanSetting("Use Timer", this, true);
        autoJump = new BooleanSetting("AutoJump", this, true);

        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(speedSettings);
        Client.INSTANCE.settingsManager.rSetting(timerBoost);
        Client.INSTANCE.settingsManager.rSetting(autoJump);

        this.setSuffix(mode.getMode());
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        moveSpeed = MoveUtil.getBaseMoveSpeed();
        lastDist = 0.0;
        stage = 2;
        grimTicks = 0;
        TimerUtil.setTimerSpeed(1.0f);
    }

    @Override
    public void onDisable() {
        TimerUtil.reset();
        // Przywracanie sprintu po wyłączeniu
        if (mc.thePlayer != null) {
            mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
        }
        this.setSuffix(mode.getMode());
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(mode.getMode());

        double xDist = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
        double zDist = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
        lastDist = Math.sqrt(xDist * xDist + zDist * zDist);

        if (!MoveUtil.isMoving()) {
            TimerUtil.reset();
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
            return;
        }

        switch (mode.getMode()) {
            case "Grim":
                doGrim();
                break;
            case "Strafe":
                doStrafe();
                break;
            case "Verus":
                doVerus();
                break;
            case "Vulcan":
                doVulcan(event);
                break;
        }
    }

    private void damageVerus() {
        if (mc.thePlayer.onGround) {
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY + 3.05,
                    mc.thePlayer.posZ,
                    false
            ));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    false
            ));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
        }
    }

    private void doGrim() {
        if (mc.thePlayer.onGround) {
            if (autoJump.isEnabled()) {
                mc.thePlayer.jump();
                if (timerBoost.isEnabled()) TimerUtil.setTimerSpeed(1.3f);
                grimTicks = 0;
            }
        } else {
            grimTicks++;
            if (timerBoost.isEnabled()) {
                if (grimTicks > 2) {
                    TimerUtil.setTimerSpeed(1.0f);
                }
            }
        }
    }

    private double getCurrentMotion() {
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    private void doStrafe() {
        if (timerBoost.isEnabled()) TimerUtil.setTimerSpeed(1.08f);

        if (mc.thePlayer.onGround && MoveUtil.isMoving()) {
            if (autoJump.isEnabled()) mc.thePlayer.motionY = 0.42f;
            double baseSpeed = MoveUtil.getBaseMoveSpeed();
            moveSpeed = baseSpeed * 2.14;
            stage = 1;
        } else if (stage == 1 && mc.thePlayer.motionY > 0) {
            double difference = 0.66 * (lastDist - MoveUtil.getBaseMoveSpeed());
            moveSpeed = lastDist - difference;
            stage = 2;
        } else {
            if ((mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0)).size() > 0 || mc.thePlayer.isCollidedVertically) && stage > 0) {
                stage = 0;
            }
            moveSpeed = lastDist - lastDist / 159.0;
        }

        moveSpeed = Math.max(moveSpeed, MoveUtil.getBaseMoveSpeed());
        MoveUtil.setSpeed(moveSpeed);
    }

    private void doVerus() {
        if (!MoveUtil.isMoving()) return;

        if (mc.thePlayer.onGround) {
            if (autoJump.isEnabled()) {
                damageVerus();
                mc.thePlayer.jump();
                mc.thePlayer.motionY = 0.33;
                moveSpeed = 0.75;
            }
        } else {
            moveSpeed = lastDist - lastDist / 159.0;
        }
        MoveUtil.setSpeed(moveSpeed);
    }

    private void doVulcan(EventUpdate event) {
        // 1. Domyślny stan: Timer 1.0 (Legit)
        TimerUtil.setTimerSpeed(1.0f);

        // Jeśli nie chodzimy, zerujemy ruch i wychodzimy
        if (!MoveUtil.isMoving()) {
            moveSpeed = 0.0;
            return;
        }

        // --- FIX BADPACKETS (ENTITY ACTION) ---
        // Sprawdzamy, czy Killaura bije cel.
        // Jeśli tak -> NIE wysyłamy pakietów sprintu, bo atakowanie samo w sobie resetuje sprint.
        // Unikamy spamu: StopSprint (Speed) -> Attack -> StopSprint (Killaura) -> StartSprint (Speed) w jednym ticku.

        Killaura aura = (Killaura) Client.INSTANCE.moduleManager.getModule(Killaura.class);
        boolean isAttacking = aura != null && aura.isToggled() && aura.target != null;

        if (!isAttacking) {
            // Cancel Sprint Packet (Tylko gdy nie walczymy)
            // Oszukuje serwer, że "idziemy", co zmienia limity detekcji prędkości.
            mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
            mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
        }

        // 3. LOGIKA GROUND PULSE
        if (mc.thePlayer.onGround) {
            if (autoJump.isEnabled()) {
                mc.thePlayer.jump();

                // "Pulse" Timer - tylko przy skoku
                if (timerBoost.isEnabled()) {
                    TimerUtil.setTimerSpeed(1.25f);
                }

                double base = MoveUtil.getBaseMoveSpeed();

                // --- FIX SPEED (PREDICTION) PRZY STRAFE'OWANIU ---
                // Sprawdzamy czy wciskasz A lub D (moveStrafe != 0)
                // Vulcan Prediction nienawidzi, gdy przy dużej prędkości zmieniasz wektor ruchu na boki.
                // Dlatego przy strafe'owaniu zmniejszamy boost.

                if (mc.thePlayer.movementInput.moveStrafe != 0.0f) {
                    // Strafe/Skręt - Mniejsza prędkość (bezpieczniej)
                    // 1.35x to wciąż szybciej niż vanilla, ale nie flaguje
                    moveSpeed = base * 1.35;
                } else {
                    // Tylko do przodu (W) - Pełna moc
                    moveSpeed = base * 1.6;
                }

                // Aplikujemy prędkość TYLKO na ziemi
                MoveUtil.setSpeed(moveSpeed);
            }
        } else {
            // 4. LOGIKA POWIETRZA - 100% VANILLA
            // Pozwalamy silnikowi Minecrafta (Vanilla) samemu obliczyć tarcie i lot.
            event.setYaw(mc.thePlayer.rotationYaw);
        }
    }
}