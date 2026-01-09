package doom.module.impl.movement;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventUpdate;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.MoveUtil;
import doom.util.TimerUtil; // Zakładam, że masz TimerUtil, jeśli nie - usuń linie z nim
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import org.lwjgl.input.Keyboard;

import java.util.LinkedList;
import java.util.Queue;

public class Fly extends Module {

    // === SETTINGS ===
    private final ModeSetting mode;
    private final NumberSetting speed;
    private final NumberSetting verticalSpeed; // Do sterowania wysokością
    private final NumberSetting timerSpeed;

    // Opcje dla efektu z filmiku
    private final BooleanSetting waitDamage;  // Czekaj na TNT/Łuk
    private final BooleanSetting blink;       // To powoduje ten "teleport" na końcu
    private final BooleanSetting stopOnGround; // Auto-disable jak dotkniesz ziemi
    private final BooleanSetting bobbing;     // Efekt wizualny machania ręką

    // === STATE ===
    private boolean isFlying = false;
    private boolean awaitingDamage = false;
    private int flyTicks = 0;
    private double startY;

    // Bufor pakietów dla Blinka (Desync)
    private final Queue<Packet<?>> packetBuffer = new LinkedList<>();

    public Fly() {
        super("Fly", Keyboard.KEY_F, Category.MOVEMENT);

        mode = new ModeSetting("Mode", this, "Damage", "Damage", "Vanilla", "Motion", "Grim");
        speed = new NumberSetting("Speed", this, 2.0, 0.1, 9.5, 0.1);
        verticalSpeed = new NumberSetting("Vertical", this, 0.0, -2.0, 2.0, 0.1);
        timerSpeed = new NumberSetting("Timer", this, 1.0, 0.1, 3.0, 0.1);

        waitDamage = new BooleanSetting("Wait Damage", this, true);
        blink = new BooleanSetting("Blink (Desync)", this, true);
        stopOnGround = new BooleanSetting("Stop on Ground", this, true);
        bobbing = new BooleanSetting("View Bobbing", this, true);

        // Rejestracja ustawień
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(speed);
        Client.INSTANCE.settingsManager.rSetting(verticalSpeed);
        Client.INSTANCE.settingsManager.rSetting(timerSpeed);
        Client.INSTANCE.settingsManager.rSetting(waitDamage);
        Client.INSTANCE.settingsManager.rSetting(blink);
        Client.INSTANCE.settingsManager.rSetting(stopOnGround);
        Client.INSTANCE.settingsManager.rSetting(bobbing);

        this.setSuffix(mode.getMode());
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        isFlying = false;
        flyTicks = 0;
        startY = mc.thePlayer.posY;
        packetBuffer.clear();

        // Jeśli tryb Damage jest aktywny, czekamy na uderzenie
        if (mode.is("Damage") && waitDamage.isEnabled()) {
            awaitingDamage = true;
            this.setSuffix("Waiting...");
        } else {
            startFlight();
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

        // Reset timer'a
        TimerUtil.setTimerSpeed(1.0f);

        // Reset fizyki
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;

        // WYPUSZCZENIE PAKIETÓW (To powoduje teleport/resync)
        if (!packetBuffer.isEmpty()) {
            flushBuffer();
        }

        isFlying = false;
        awaitingDamage = false;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // Auto-disable na ziemi (przydatne przy Damage Fly)
        if (isFlying && stopOnGround.isEnabled() && mc.thePlayer.onGround && flyTicks > 5) {
            this.toggle();
            return;
        }

        if (awaitingDamage) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
            // Można tu dodać zerowanie Y, żeby nie spadać czekając na TNT
            if (mc.thePlayer.onGround) {
                // mc.thePlayer.jump(); // Opcjonalnie podskocz
            }
            return;
        }

        if (isFlying) {
            flyTicks++;
            if (bobbing.isEnabled()) mc.thePlayer.cameraYaw = 0.1f; // Efekt ruchu
            TimerUtil.setTimerSpeed((float) timerSpeed.getValue());

            switch (mode.getMode()) {
                case "Damage":
                    handleDamageMode();
                    break;
                case "Vanilla":
                    handleVanillaMode();
                    break;
                case "Motion":
                    handleMotionMode();
                    break;
                case "Grim":
                    handleGrimMode();
                    break;
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.thePlayer == null) return;

        // --- DETEKCJA OBRAŻEŃ ---
        if (awaitingDamage) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    // Otrzymaliśmy knockback - START
                    startFlight();

                    // Opcjonalnie: Zwiększamy siłę startową z pakietu
                    // Ale zazwyczaj Damage Fly ignoruje pakiet i leci sam
                }
            }
            else if (event.getPacket() instanceof S27PacketExplosion) {
                // Wybuch TNT - START
                startFlight();
            }
        }

        // --- LOGIKA BLINK (DESYNC) ---
        // Zatrzymuje pakiety wysyłane do serwera, tworząc desynchronizację.
        if (isFlying && blink.isEnabled() && event.getDirection() == EventPacket.Direction.SEND) {
            if (event.getPacket() instanceof C03PacketPlayer) { // Tylko pakiety ruchu
                event.setCancelled(true);
                packetBuffer.add(event.getPacket());
            }
        }
    }

    // === MECHANIKI LOTU ===

    private void startFlight() {
        awaitingDamage = false;
        isFlying = true;
        flyTicks = 0;
        startY = mc.thePlayer.posY;
        this.setSuffix(mode.getMode());

        if (mode.is("Damage")) {
            // Wybicie przy starcie
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            } else {
                mc.thePlayer.motionY = 0.42;
            }
            //MoveUtil.setSpeed(speed.getValue());
        }
    }

    private void handleDamageMode() {
        // Utrzymywanie poziomu (Glide)
        mc.thePlayer.motionY = verticalSpeed.getValue();

        // Zresetuj prędkość, żeby się "ślizgać"
        // Używamy Twojego MoveUtil
        //MoveUtil.setSpeed(speed.getValue());
    }

    private void handleVanillaMode() {
        mc.thePlayer.capabilities.isFlying = true;
       // MoveUtil.setSpeed(speed.getValue());

        if (mc.gameSettings.keyBindJump.isKeyDown()) mc.thePlayer.motionY = speed.getValue();
        else if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.thePlayer.motionY = -speed.getValue();
        else mc.thePlayer.motionY = 0;
    }

    private void handleMotionMode() {
        mc.thePlayer.motionY = verticalSpeed.getValue();
        if (mc.gameSettings.keyBindJump.isKeyDown()) mc.thePlayer.motionY += 1.0;
        if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.thePlayer.motionY -= 1.0;
       // MoveUtil.setSpeed(speed.getValue());
    }

    private void handleGrimMode() {
        // Grim Explosion Fly - bazuje na tym, że Grim pozwala na ruch po wybuchu
        // Ale sprawdza grawitację.

        if (flyTicks <= 10) {
            // Symulujemy duży wyrzut
            //MoveUtil.setSpeed(speed.getValue());
        } else {
            // Po chwili musimy opaść, bo Grim zbanuje za Gravity Check
            // Ale możemy opaść wolniej
            if (mc.thePlayer.motionY < 0) {
                mc.thePlayer.motionY *= 0.95; // Wolny spadek
            }
            //MoveUtil.setSpeed(MoveUtil.getBaseMoveSpeed() * 1.5);
        }
    }

    // === UTILS ===

    private void flushBuffer() {
        // Wysyłamy wszystkie zatrzymane pakiety naraz
        // To powoduje, że serwer nagle "widzi" całą naszą trasę lub teleportuje nas do ostatniej pewnej pozycji
        while (!packetBuffer.isEmpty()) {
            Packet<?> packet = packetBuffer.poll();
            // Wysyłamy bezpośrednio, omijając event system, żeby nie wpaść w pętlę
            mc.getNetHandler().addToSendQueue(packet);
        }
    }
}