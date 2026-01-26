package doom.module.impl.movement;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.MoveUtil;
import doom.util.TimerUtil;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

public class Speed extends Module {

    // --- USTAWIENIA ---
    public ModeSetting mode = new ModeSetting("Mode", this, "Watchdog", "Watchdog", "Vulcan", "Verus", "NCP", "BlocksMC", "Grim");

    // Opcje dodatkowe
    public BooleanSetting timerBoost = new BooleanSetting("Timer Boost", this, true);
    public NumberSetting timerSpeed = new NumberSetting("Timer Speed", this, 1.0, 1.0, 2.0, 0.05);
    public BooleanSetting autoJump = new BooleanSetting("Auto Jump", this, true);

    // --- ZMIENNE MATEMATYCZNE ---
    private double moveSpeed;
    private double lastDist;
    private int stage;
    private int airTicks; // Licznik ticków w powietrzu

    public Speed() {
        super("Speed", Keyboard.KEY_X, Category.MOVEMENT);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(timerBoost);
        Client.INSTANCE.settingsManager.rSetting(timerSpeed);
        Client.INSTANCE.settingsManager.rSetting(autoJump);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        reset();
    }

    @Override
    public void onDisable() {
        TimerUtil.reset();
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;
    }

    private void reset() {
        moveSpeed = MoveUtil.getBaseMoveSpeed();
        lastDist = 0.0;
        stage = 2;
        airTicks = 0;
        TimerUtil.reset();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(mode.getMode());

        // 1. Obliczanie lastDist (dystans przebyty w poprzednim ticku)
        // To jest kluczowe dla "Friction Math" (NCP/Watchdog)
        double xDist = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
        double zDist = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;
        lastDist = Math.sqrt(xDist * xDist + zDist * zDist);

        if (!MoveUtil.isMoving()) {
            TimerUtil.reset();
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
            return;
        }

        // Licznik ticków w powietrzu
        if (mc.thePlayer.onGround) {
            airTicks = 0;
        } else {
            airTicks++;
        }

        // Wybór trybu
        switch (mode.getMode()) {
            case "Watchdog":
                handleWatchdog(event);
                break;
            case "Vulcan":
                handleVulcan(event);
                break;
            case "Verus":
                handleVerus(event);
                break;
            case "NCP":
                handleNCP(event);
                break;
            case "BlocksMC":
                handleBlocksMC(event);
                break;
            case "Grim":
                handleGrim(event);
                break;
        }
    }

    /**
     * WATCHDOG (Hypixel)
     * Logika: Low Strafe + Friction Override + Diagonal Math
     */
    private void handleWatchdog(EventUpdate event) {
        if (mc.thePlayer.onGround && MoveUtil.isMoving()) {
            // Skok z boostem
            mc.thePlayer.motionY = 0.42f;
            moveSpeed = MoveUtil.getBaseMoveSpeed() * 2.15; // Eksplozja prędkości
            stage = 1;
            if (timerBoost.isEnabled()) TimerUtil.setTimerSpeed(1.0f);
        } else if (stage == 1) {
            // Tick 1 w powietrzu: Modyfikacja Y dla LowHop
            mc.thePlayer.motionY += 0.057f; // Rise magic value

            double difference = 0.66 * (lastDist - MoveUtil.getBaseMoveSpeed());
            moveSpeed = lastDist - difference;
            stage = 2;
        } else {
            // Tick 2+: Opadanie i tarcie

            // LowHop physics modification
            if (airTicks == 1) mc.thePlayer.motionY -= 0.1309f;
            if (airTicks == 2) mc.thePlayer.motionY -= 0.2; // Szybkie opadanie

            // Friction Override (Oszukiwanie predykcji)
            double friction = lastDist / 159.0;
            moveSpeed = lastDist - friction;

            // Partial Strafe (jeśli prędkość za mała, wymuś strafe)
            if (Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ) < 0.0125) {
                // Wymuszenie ruchu jeśli antycheat myśli że stoimy
                stage = 1;
            }
        }

        // Zabezpieczenie przed cofnięciem
        moveSpeed = Math.max(moveSpeed, MoveUtil.getBaseMoveSpeed());

        MoveUtil.setSpeed(moveSpeed);
    }


    /**
     * VULCAN 2.7.6 "Balance-Fix" Strategy
     * Naprawia: Timer (Balance) poprzez zwalnianie w powietrzu.
     * Naprawia: Speed (Prediction) poprzez zmniejszenie Speeda na rzecz stabilności.
     */
    private void handleVulcan(EventUpdate event) {
        // 1. Reset, jeśli stoimy
        if (!MoveUtil.isMoving()) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
            TimerUtil.reset();
            return;
        }

        // --- ON GROUND (Wybicie) ---
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();

            // ZMNIEJSZONO do 1.42.
            // To jest bezpieczna wartość. Poprzednie 1.46 było "na styk" i przy lagach wywalało Prediction.
            double speed = MoveUtil.getBaseMoveSpeed() * 1.42;

            if (mc.thePlayer.isPotionActive(1)) {
                // Z potką Speed lekko szybciej
                speed = MoveUtil.getBaseMoveSpeed() * 1.48;
            }

            MoveUtil.setSpeed(speed);

            // Timer normalny przy wybiciu, żeby serwer dobrze policzył predykcję startu
            if (timerBoost.isEnabled()) {
                TimerUtil.setTimerSpeed(1.0f);
            }

            airTicks = 0;
        }
        // --- IN AIR (Regeneracja) ---
        else {
            // KLUCZOWA POPRAWKA DLA TIMER (BALANCE):
            // Musimy ustawić timer PONIŻEJ 1.0.
            // 0.9f sprawia, że gra działa 10% wolniej w powietrzu.
            // To "spłaca" pakiety i zeruje licznik TimerD w Vulcanie.
            if (timerBoost.isEnabled()) {
                TimerUtil.setTimerSpeed(0.9f);
            }

            // Fix dla Speed (Prediction) - Strafe
            // Pozwalamy na lekkie sterowanie, ale nie narzucamy całej prędkości.
            // Tylko aktualizujemy kierunek (Yaw), zachowując obecną magnitudę (pęd).
            if (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0) {
                // Pobieramy obecną prędkość (która naturalnie maleje przez tarcie)
                double currentSpeed = Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
                // I aplikujemy ją w stronę, w którą patrzymy
                MoveUtil.setSpeed(currentSpeed);
            } else {
                // Jeśli nic nie klikamy, hamujemy naturalnie
                mc.thePlayer.motionX *= 0.91;
                mc.thePlayer.motionZ *= 0.91;
            }
        }
    }
    /**
     * VERUS
     * Logika: Y-Port (Packet Spam) + Low MotionY
     */
    private void handleVerus(EventUpdate event) {
        if (!MoveUtil.isMoving()) return;

        if (mc.thePlayer.onGround && autoJump.isEnabled()) {
            mc.thePlayer.jump();
            moveSpeed = 0.65; // Verus pozwala na szybki start
            stage = 0;
        } else {
            stage++;
            // Y-Port Logic
            if (stage == 1) {
                mc.thePlayer.motionY = 0.33; // Obniżamy skok
                moveSpeed *= 0.98;
            } else if (stage == 4) {
                // To jest ten "Air Walking" bypass
                // Wysyłamy pakiet interakcji pod nogami (Verus myśli że na czymś stoimy)
                // Używamy -255 jako Y, żeby nie postawić faktycznie bloku, ale wywołać logikę
                mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
                        new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ),
                        1,
                        mc.thePlayer.getCurrentEquippedItem(),
                        0, 0, 0
                ));

                mc.thePlayer.motionY = -0.078; // Pull down aggressive
            }

            moveSpeed = lastDist - lastDist / 159.0;
        }

        MoveUtil.setSpeed(moveSpeed);
    }

    /**
     * NCP (NoCheatPlus)
     * Logika: Bunny Slope (Friction Simulation) + Timer Pulse
     */
    private void handleNCP(EventUpdate event) {
        if (!MoveUtil.isMoving()) return;

        if (mc.thePlayer.onGround) {
            if (autoJump.isEnabled()) {
                mc.thePlayer.jump();
                // Timer Pulse: Szybko na starcie
                if (timerBoost.isEnabled()) TimerUtil.setTimerSpeed(1.08f);
                moveSpeed = MoveUtil.getBaseMoveSpeed() * 1.7; // Start boost
            }
        } else {
            // Timer Balance: Wolno w powietrzu
            if (timerBoost.isEnabled()) TimerUtil.setTimerSpeed(1.0f);

            // Bunny Slope Math
            // speed -= speed / 159 to idealna emulacja fizyki NCP
            moveSpeed = lastDist - lastDist / 159.0;
        }

        MoveUtil.setSpeed(Math.max(moveSpeed, MoveUtil.getBaseMoveSpeed()));
    }

    /**
     * BLOCKSMC
     * Logika: Aggressive NCP + Friction
     */
    private void handleBlocksMC(EventUpdate event) {
        if (mc.thePlayer.onGround && MoveUtil.isMoving()) {
            if (autoJump.isEnabled()) {
                mc.thePlayer.jump();
                moveSpeed = MoveUtil.getBaseMoveSpeed() * 2.149; // Agresywny start
            }
        } else {
            // Agresywne tarcie, żeby nie cofało (Rubberband fix)
            // speed -= 0.8 * (speed - base)
            double diff = lastDist - MoveUtil.getBaseMoveSpeed();
            moveSpeed = lastDist - (diff * 0.75); // Trochę łagodniej niż 0.8 dla stabilności
        }

        MoveUtil.setSpeed(Math.max(moveSpeed, MoveUtil.getBaseMoveSpeed()));
    }

    /**
     * GRIM
     * Logika: 1:1 Physics Simulation + Legit Strafe
     * Grim nie pozwala na modyfikację prędkości "z powietrza".
     * Jedyne co możemy zrobić, to idealnie odwzorować movement i dodać minimalny boost.
     */
    private void handleGrim(EventUpdate event) {
        if (mc.thePlayer.onGround && MoveUtil.isMoving()) {
            if (autoJump.isEnabled()) {
                mc.thePlayer.jump();

                // Grim pozwala na lekki boost przy skoku, jeśli kąt jest idealny
                // MoveUtil.getDirection() zapewnia, że motionX/Z zgadza się z Yaw
                float yaw = MoveUtil.getDirection();
                double boost = 0.05; // Malutki boost, większy flaguje

                mc.thePlayer.motionX -= MathHelper.sin(yaw) * boost;
                mc.thePlayer.motionZ += MathHelper.cos(yaw) * boost;
            }
        } else {
            // W powietrzu Grim sprawdza, czy nie przyspieszasz.
            // Nic nie robimy z motionX/Z w powietrzu (pozwalamy działać fizyce gry).
            // To jest bypass "Legit".
        }
    }
}