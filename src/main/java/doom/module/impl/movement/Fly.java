package doom.module.impl.movement;

import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class Fly extends Module {

    // Używamy nazwy i typu listy jak w Ethane
    private final List<Packet<INetHandlerPlayClient>> packets = new ArrayList<>();
    private long test;

    public Fly() {
        super("Fly", Keyboard.KEY_F, Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        // Logika z Ethane: czyszczenie i reset czasu
        packets.clear();
        test = System.currentTimeMillis();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        // Logika z Ethane: bezpieczne zwalnianie pakietów w głównym wątku (mc.execute)
        if (mc.getNetHandler() != null && !packets.isEmpty()) {
            final List<Packet<INetHandlerPlayClient>> packetsToRelease = new ArrayList<>(packets);
            packets.clear();

            // mc.execute z Ethane to w 1.8 addScheduledTask
            mc.addScheduledTask(() -> {
                if (mc.getNetHandler() != null) {
                    for (Packet<INetHandlerPlayClient> p : packetsToRelease) {
                        try {
                            // p.handle() w 1.8 to processPacket()
                            p.processPacket(mc.getNetHandler());
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            this.toggle(); // Odpowiednik module.setEnabled(false)
            return;
        }

        // Logika z Ethane: stawianie TNT pod graczem
        // W 1.8 używamy (posX, posY - 1, posZ)
        BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
        mc.theWorld.setBlockState(pos, Blocks.tnt.getDefaultState());
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        // Logika z Ethane: tylko pakiety przychodzące (ReceivePacket)
        if (event.getDirection() == EventPacket.Direction.RECEIVE) {

            // Timer z Ethane (750ms)
            if (System.currentTimeMillis() - test <= 750) {
                return;
            }

            // Dodawanie do listy i anulowanie
            // noinspection unchecked
            packets.add((Packet<INetHandlerPlayClient>) event.getPacket());
            event.setCancelled(true);
        }
    }
}