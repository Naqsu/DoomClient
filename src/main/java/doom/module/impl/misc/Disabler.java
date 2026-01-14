package doom.module.impl.misc;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.TimeHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Disabler extends Module {

    public ModeSetting mode = new ModeSetting("Mode", this, "Vulcan", "Vulcan", "Verus", "Grim", "PingSpoof");

    public NumberSetting vulcanDelay = new NumberSetting("Vulcan Delay", this, 1000, 200, 5000, 100);
    public BooleanSetting vulcanC00 = new BooleanSetting("Cancel KeepAlive", this, true);

    public BooleanSetting grimPost = new BooleanSetting("Post Transaction", this, true);
    public NumberSetting pingDelay = new NumberSetting("Ping (ms)", this, 400, 50, 5000, 50);

    // Thread-Safe Queue
    private final ConcurrentLinkedQueue<Packet<?>> packetBuffer = new ConcurrentLinkedQueue<>();
    private final TimeHelper timer = new TimeHelper();

    // --- CRITICAL FIX: FLAGA BLOKUJĄCA PĘTLĘ ---
    private boolean isFlushing = false;

    public Disabler() {
        super("Disabler", 0, Category.MISC);

        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(vulcanDelay);
        Client.INSTANCE.settingsManager.rSetting(vulcanC00);
        Client.INSTANCE.settingsManager.rSetting(grimPost);
        Client.INSTANCE.settingsManager.rSetting(pingDelay);

        vulcanDelay.setDependency(() -> mode.is("Vulcan"));
        vulcanC00.setDependency(() -> mode.is("Vulcan"));
        grimPost.setDependency(() -> mode.is("Grim"));
        pingDelay.setDependency(() -> mode.is("PingSpoof"));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        flush();
    }

    private void reset() {
        packetBuffer.clear();
        timer.reset();
        isFlushing = false;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(mode.getMode());

        if (mc.thePlayer == null || mc.theWorld == null) {
            packetBuffer.clear();
            return;
        }

        switch (mode.getMode()) {
            case "Vulcan":
                if (timer.hasReached(vulcanDelay.getValue())) {
                    flush();
                    timer.reset();
                }
                break;

            case "PingSpoof":
                if (!packetBuffer.isEmpty() && timer.hasReached(pingDelay.getValue())) {
                    flush();
                    timer.reset();
                }
                break;

            case "Verus":
                if (mc.thePlayer.ticksExisted % 40 == 0) {
                    flush();
                }
                break;
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.thePlayer == null) return;

        // --- CRITICAL FIX: JEŚLI FLUSHUJEMY, IGNORUJ PAKIETY ---
        // To zapobiega nieskończonej pętli i crashom
        if (isFlushing) return;

        Packet<?> p = event.getPacket();

        // 1. VULCAN
        if (mode.is("Vulcan") && event.getDirection() == EventPacket.Direction.SEND) {
            if (p instanceof C0FPacketConfirmTransaction) {
                event.setCancelled(true);
                packetBuffer.add(p);
            }
            if (vulcanC00.isEnabled() && p instanceof C00PacketKeepAlive) {
                event.setCancelled(true);
                packetBuffer.add(p);
            }
        }

        // 2. VERUS
        if (mode.is("Verus") && event.getDirection() == EventPacket.Direction.SEND) {
            if (p instanceof C0FPacketConfirmTransaction) {
                event.setCancelled(true);
                packetBuffer.add(p);
            }
        }

        // 3. GRIM
        if (mode.is("Grim")) {
            if (event.getDirection() == EventPacket.Direction.SEND) {
                if (p instanceof C0FPacketConfirmTransaction) {
                    event.setCancelled(true);
                    packetBuffer.add(p);
                }
            }
            if (event.getDirection() == EventPacket.Direction.SEND && p instanceof C03PacketPlayer) {
                if (!packetBuffer.isEmpty() && grimPost.isEnabled()) {
                    flush();
                }
            }
        }

        // 4. PING SPOOF
        if (mode.is("PingSpoof") && event.getDirection() == EventPacket.Direction.SEND) {
            if (p instanceof C0FPacketConfirmTransaction || p instanceof C00PacketKeepAlive) {
                event.setCancelled(true);
                packetBuffer.add(p);
            }
        }

        // ANTI LAGBACK
        if (event.getDirection() == EventPacket.Direction.RECEIVE && p instanceof S08PacketPlayerPosLook) {
            flush();
        }
    }

    private void flush() {
        if (packetBuffer.isEmpty()) return;

        // Ustawiamy flagę, żeby onPacket ignorował te pakiety
        isFlushing = true;

        while (!packetBuffer.isEmpty()) {
            Packet<?> p = packetBuffer.poll();
            if (p != null) {
                try {
                    if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
                        mc.getNetHandler().getNetworkManager().sendPacket(p);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Zdejmujemy flagę
        isFlushing = false;
    }
}