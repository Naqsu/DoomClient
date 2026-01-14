package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;

public class NoFall extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Vulcan", "Vulcan", "Packet", "Ground", "Verus", "Watchdog", "Grim");
    private final BooleanSetting voidCheck = new BooleanSetting("Void Check", this, true);

    public NoFall() {
        super("NoFall", 0, Category.PLAYER);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(voidCheck);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(mode.getMode());

        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Jeśli spadamy do voida, wyłączamy NoFall
        // Antycheat cofnie nas na górę (Lagback), zamiast pozwolić zginąć od void dmg
        if (voidCheck.isEnabled() && isVoid()) {
            return;
        }

        if (mc.thePlayer.fallDistance > 2.5f) {
            switch (mode.getMode()) {
                case "Ground":
                    // Najprostszy tryb, działa na Vanilla/NCP
                    event.setOnGround(true);
                    break;

                case "Packet":
                    // Oszczędniejszy tryb, wysyła pakiet rzadziej
                    if (mc.thePlayer.fallDistance > 3.0f) {
                        event.setOnGround(true);
                        mc.thePlayer.fallDistance = 0;
                    }
                    break;

                case "Vulcan":
                    // Vulcan fix: "Miganie" statusem onGround
                    // Wysyłamy true tylko w parzystych tickach, żeby zmylić checki
                    if (mc.thePlayer.ticksExisted % 2 == 0) {
                        event.setOnGround(true);
                        mc.thePlayer.fallDistance = 0;
                    }
                    break;

                case "Verus":
                    // Verus: wymaga onGround=true + wyzerowania ruchu w dół (czasami)
                    if (mc.thePlayer.fallDistance > 2.0f) {
                        event.setOnGround(true);
                        mc.thePlayer.motionY = 0; // Lekki float
                        mc.thePlayer.fallDistance = 0;
                    }
                    break;

                case "Watchdog":
                    // Hypixel: Nie lubi onGround=true w powietrzu.
                    // Próbujemy zresetować fall distance tylko raz na jakiś czas.
                    if (mc.thePlayer.fallDistance > 3.5f) {
                        event.setOnGround(true);
                        mc.thePlayer.fallDistance = 0;
                    }
                    break;

                case "Grim":
                    // Grim jest symulacją 1:1. Trudny do obejścia pakietami.
                    // Próbujemy wysłać ghost packet C03 (bez pozycji, tylko status).
                    // Uwaga: To może powodować cofki.
                    if (mc.thePlayer.fallDistance > 3.0f) {
                        if (mc.thePlayer.ticksExisted % 3 == 0) {
                            mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                            mc.thePlayer.fallDistance = 0;
                        }
                    }
                    break;
            }
        }
    }

    private boolean isVoid() {
        if (mc.thePlayer.posY < 0) return true;
        for (int i = (int) mc.thePlayer.posY; i >= 0; i--) {
            BlockPos pos = new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ);
            if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }
}