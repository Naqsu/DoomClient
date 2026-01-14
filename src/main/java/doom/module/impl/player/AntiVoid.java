package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AntiVoid extends Module {

    // --- USTAWIENIA ---
    public static ModeSetting mode = new ModeSetting("Mode", null, "SmartBlink", "SmartBlink", "SlowMotion", "Packet", "GhostBlock");
    public static NumberSetting fallDist = new NumberSetting("Fall Dist", null, 3.0, 1.0, 5.0, 0.5);
    public static NumberSetting voidHeight = new NumberSetting("Void Y", null, 0.0, -64.0, 256.0, 1.0);
    public static NumberSetting delay = new NumberSetting("Delay", null, 2, 0, 10, 1);
    public static NumberSetting motionY = new NumberSetting("Motion Y", null, 0.3, 0.0, 1.0, 0.05);

    // --- ZMIENNE SYSTEMOWE ---
    private final List<Packet> packetBuffer = new ArrayList<>();
    private Vec3 rescuePosition;
    private boolean isFallingIntoVoid;
    private boolean dispatching = false;
    private BlockPos ghostBlockPos = null;
    private int fallTicks = 0;
    private int rescueTicks = 0;
    private boolean hasRescued = false;
    private final Random random = new Random();
    private double lastMotionY = 0;

    public AntiVoid() {
        super("AntiVoid", Keyboard.KEY_NONE, Category.PLAYER);

        mode.parent = this;
        fallDist.parent = this;
        voidHeight.parent = this;
        delay.parent = this;
        motionY.parent = this;

        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(fallDist);
        Client.INSTANCE.settingsManager.rSetting(voidHeight);
        Client.INSTANCE.settingsManager.rSetting(delay);
        Client.INSTANCE.settingsManager.rSetting(motionY);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mode.is("GhostBlock")) removeGhostBlock();
        if (mode.is("SmartBlink") && !packetBuffer.isEmpty()) {
            sendBufferGradually();
        }
        reset();
    }

    private void reset() {
        packetBuffer.clear();
        rescuePosition = null;
        isFallingIntoVoid = false;
        dispatching = false;
        ghostBlockPos = null;
        fallTicks = 0;
        rescueTicks = 0;
        hasRescued = false;
        lastMotionY = 0;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Aktualizacja bezpiecznej pozycji
        if (!isFallingIntoVoid && mc.thePlayer.onGround && mc.thePlayer.fallDistance < 2) {
            rescuePosition = new Vec3(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ
            );
            hasRescued = false;
        }

        // Sprawdzenie czy spadamy w void
        isFallingIntoVoid = isFallingIntoVoid();

        // Resetowanie jeśli jesteśmy bezpieczni
        if (!isFallingIntoVoid && hasRescued) {
            resetState();
        }

        // Obsługa trybów
        if (isFallingIntoVoid && rescuePosition != null && !hasRescued) {
            fallTicks++;

            // Sprawdzenie czy czas na ratunek (z opóźnieniem)
            if (fallTicks >= delay.getValue() && mc.thePlayer.fallDistance >= fallDist.getValue()) {
                switch (mode.getMode()) {
                    case "SmartBlink":
                        handleSmartBlink();
                        break;
                    case "SlowMotion":
                        handleSlowMotion();
                        break;
                    case "Packet":
                        handlePacket();
                        break;
                    case "GhostBlock":
                        handleGhostBlock();
                        break;
                }
                hasRescued = true;
                rescueTicks = 0;
            }
        }

        // Kontynuacja ratunku
        if (hasRescued) {
            rescueTicks++;
            if (rescueTicks > 20) { // Max 1 sekunda ratunku
                resetState();
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.thePlayer == null || dispatching) return;

        Packet packet = event.getPacket();

        if (mode.is("SmartBlink") && packet instanceof C03PacketPlayer) {
            // Buforowanie tylko jeśli jesteśmy w voidzie
            if (isFallingIntoVoid && rescuePosition != null && !hasRescued) {
                event.setCancelled(true);
                packetBuffer.add(packet);
            }
        }
    }

    // ==========================================================
    //                   TRYB 1: SMART BLINK
    // ==========================================================
    private void handleSmartBlink() {
        if (rescuePosition == null) return;

        // 1. Najpierw stopniowe wysłanie buforowanych pakietów
        if (!packetBuffer.isEmpty()) {
            sendBufferGradually();
        }

        // 2. Płynny powrót z małymi offsetami (mniej podejrzane)
        double offsetX = (random.nextDouble() - 0.5) * 0.1;
        double offsetZ = (random.nextDouble() - 0.5) * 0.1;

        // 3. Wysłanie pakietów pozycji z interpolacją
        for (int i = 0; i < 3; i++) {
            double progress = i / 2.0;
            double x = mc.thePlayer.posX + (rescuePosition.xCoord - mc.thePlayer.posX) * progress + offsetX;
            double y = rescuePosition.yCoord + 0.5 + (i * 0.1);
            double z = mc.thePlayer.posZ + (rescuePosition.zCoord - mc.thePlayer.posZ) * progress + offsetZ;

            mc.getNetHandler().addToSendQueue(new C04PacketPlayerPosition(
                    x, y, z, mc.thePlayer.onGround
            ));
        }

        // 4. Ustawienie pozycji klienta
        mc.thePlayer.setPosition(
                rescuePosition.xCoord + offsetX,
                rescuePosition.yCoord + 0.2,
                rescuePosition.zCoord + offsetZ
        );

        // 5. Reset fizyki
        resetPhysics();
    }

    private void sendBufferGradually() {
        if (packetBuffer.isEmpty()) return;

        dispatching = true;
        try {
            // Wysyłanie co drugi pakiet dla naturalniejszego ruchu
            for (int i = 0; i < packetBuffer.size(); i++) {
                if (i % 2 == 0) {
                    mc.getNetHandler().addToSendQueue(packetBuffer.get(i));
                }
            }
        } finally {
            packetBuffer.clear();
            dispatching = false;
        }
    }

    // ==========================================================
    //                   TRYB 2: SLOW MOTION
    // ==========================================================
    private void handleSlowMotion() {
        // Zmniejszenie prędkości spadania zamiast teleportu
        mc.thePlayer.motionY = Math.min(mc.thePlayer.motionY, -0.1);
        mc.thePlayer.fallDistance = 0;

        // Delikatne przesunięcie w kierunku ratunkowej pozycji
        if (rescuePosition != null) {
            double dx = rescuePosition.xCoord - mc.thePlayer.posX;
            double dz = rescuePosition.zCoord - mc.thePlayer.posZ;
            double distance = Math.sqrt(dx * dx + dz * dz);

            if (distance > 0.1) {
                mc.thePlayer.motionX = dx * 0.1;
                mc.thePlayer.motionZ = dz * 0.1;
            }
        }

        // Wysłanie pakietu pozycji
        mc.getNetHandler().addToSendQueue(new C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY,
                mc.thePlayer.posZ,
                mc.thePlayer.onGround
        ));
    }

    // ==========================================================
    //                   TRYB 3: PACKET
    // ==========================================================
    private void handlePacket() {
        if (rescuePosition == null) return;

        // Wysłanie serii pakietów z małymi zmianami wysokości
        for (int i = 0; i < 5; i++) {
            double y = rescuePosition.yCoord + 0.5 + (i * 0.15);

            mc.getNetHandler().addToSendQueue(new C04PacketPlayerPosition(
                    rescuePosition.xCoord,
                    y,
                    rescuePosition.zCoord,
                    true
            ));

            // Małe opóźnienie między pakietami
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Ustawienie pozycji klienta
        mc.thePlayer.setPosition(
                rescuePosition.xCoord,
                rescuePosition.yCoord + 0.2,
                rescuePosition.zCoord
        );

        resetPhysics();
    }

    // ==========================================================
    //                   TRYB 4: GHOST BLOCK
    // ==========================================================
    private void handleGhostBlock() {
        BlockPos posUnder = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);

        // Sprawdź czy to bezpieczne miejsce na ghost blocka
        if (mc.theWorld.isAirBlock(posUnder) && isSafeForGhostBlock(posUnder)) {
            // Tylko klient-side ghost block
            mc.theWorld.setBlockState(posUnder, Blocks.barrier.getDefaultState());
            ghostBlockPos = posUnder;

            // Symulacja lądowania
            mc.thePlayer.motionY = 0;
            mc.thePlayer.fallDistance = 0;

            // Usuń ghost blocka po chwili
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    removeGhostBlock();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private boolean isSafeForGhostBlock(BlockPos pos) {
        // Sprawdź czy w promieniu 2 bloków są inne bloki (mniej podejrzane)
        int nearbyBlocks = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (!mc.theWorld.isAirBlock(pos.add(x, y, z))) {
                        nearbyBlocks++;
                    }
                }
            }
        }
        return nearbyBlocks > 2;
    }

    private void removeGhostBlock() {
        if (ghostBlockPos != null && mc.theWorld != null) {
            mc.theWorld.setBlockToAir(ghostBlockPos);
            ghostBlockPos = null;
        }
    }

    // ==========================================================
    //                      POMOCNICZE METODY
    // ==========================================================

    private boolean isFallingIntoVoid() {
        if (mc.thePlayer.posY < voidHeight.getValue()) return true;

        // Sprawdź czy spadamy z dużą prędkością
        if (mc.thePlayer.motionY < -0.5 && !mc.thePlayer.onGround) {
            // Sprawdź czy pod nami jest void
            return !isBlockUnderWithin(15);
        }

        return false;
    }

    private boolean isBlockUnderWithin(int range) {
        for (int i = 0; i < range; i++) {
            AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox()
                    .offset(0, -i, 0)
                    .contract(0.1, 0, 0.1);

            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void resetPhysics() {
        mc.thePlayer.fallDistance = 0;
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionY = motionY.getValue(); // Mały bounce
        mc.thePlayer.motionZ = 0;
        mc.thePlayer.jumpMovementFactor = 0.02f;
    }

    private void resetState() {
        packetBuffer.clear();
        isFallingIntoVoid = false;
        fallTicks = 0;
        rescueTicks = 0;
        hasRescued = false;
        if (mode.is("GhostBlock")) {
            removeGhostBlock();
        }
    }
}