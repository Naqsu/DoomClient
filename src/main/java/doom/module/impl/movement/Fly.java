package doom.module.impl.movement;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.util.MoveUtil;
import doom.util.TimerUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion; // Obsługa TNT/Fireballi
import org.lwjgl.input.Keyboard;

public class Fly extends Module {

    private boolean active = false;
    private int ticks = 0;
    private double targetSpeed = 0.0;

    public Fly() {
        super("Fly", Keyboard.KEY_F, Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        active = false;
        ticks = 0;
        targetSpeed = 0.0;
        Client.addChatMessage("§e[Vulcan] Waiting for ANY damage (Bow/TNT)...");
    }

    @Override
    public void onDisable() {
        TimerUtil.reset();
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        Packet<?> packet = event.getPacket();

        // Obsługa Velocity z bicia (S12)
        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity s12 = (S12PacketEntityVelocity) packet;
            if (s12.getEntityID() == mc.thePlayer.getEntityId()) {
                double vX = s12.getMotionX() / 8000.0;
                double vZ = s12.getMotionZ() / 8000.0;
                handleVelocity(vX, vZ, event);
            }
        }
        // Obsługa Velocity z wybuchów (S27 - TNT/Fireball) - WAŻNE NA SKYWARS
        else if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion s27 = (S27PacketExplosion) packet;
            double vX = s27.func_149149_c();
            double vZ = s27.func_149147_e();
            handleVelocity(vX, vZ, event);
        }
    }

    private void handleVelocity(double vX, double vZ, EventPacket event) {
        // Obliczamy wektor poziomy, który dał nam serwer
        double velocityDist = Math.sqrt(vX * vX + vZ * vZ);

        // Ignorujemy słabe popchnięcia (np. bicie ręką), szukamy łuku/tnt
        if (velocityDist > 0.2) {
            active = true;
            ticks = 0;
            event.setCancelled(true);

            // LOGIKA NAUKOWA:
            // Vulcan pozwala na: BaseSpeed + Velocity.
            // Ustawiamy naszą prędkość na dokładnie tyle, ile dał serwer + mały boost
            targetSpeed = velocityDist;

            Client.addChatMessage("§a[Vulcan] Catch! V-Speed: " + String.format("%.3f", targetSpeed));

            // Wymuszamy skok fizyczny, żeby zdjąć flagę 'OnGround'
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.42;
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!active) return;

        ticks++;

        // Vulcan velocity exemption trwa ok. 20 ticków.
        // My lecimy przez 15, żeby być bezpiecznym.
        if (ticks <= 15) {

            // Nadajemy prędkość równą sile uderzenia.
            // Jeśli dostałeś mocno (TNT) -> lecisz szybko.
            // Jeśli dostałeś słabo (Śnieżka) -> lecisz wolno (ale nie dostaniesz bana).
            MoveUtil.setSpeed(targetSpeed);

            // Symulacja tarcia powietrza (wymagane przez SpeedA)
            targetSpeed *= 0.98; // Zwalniamy co tick o 2%

            // Podtrzymanie lotu (lekki Glide, ale nie płaski Hover)
            // Pozwalamy na lekkie opadanie, żeby nie wkurzyć Flight(Prediction)
            if (mc.thePlayer.motionY < -0.1) {
                mc.thePlayer.motionY = -0.1;
            }

            // Timer dla efektu
            TimerUtil.setTimerSpeed(1.1f);

        } else {
            active = false;
            TimerUtil.reset();
            MoveUtil.setSpeed(0.0); // Stop w miejscu
            Client.addChatMessage("§c[Vulcan] Velocity ended.");
            this.toggle();
        }
    }
}