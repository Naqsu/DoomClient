package doom.module.impl.combat;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventRender2D;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import doom.util.TimeHelper;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.play.client.*;
import net.minecraft.network.status.client.C00PacketServerQuery;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FakeLag extends Module {

    public ModeSetting mode = new ModeSetting("Mode", this, "Jump", "Jump", "Dynamic");
    public NumberSetting maxChoke = new NumberSetting("Max Choke", this, 2000, 100, 5000, 50);
    public BooleanSetting indicator = new BooleanSetting("Indicator", this, true);

    private final ConcurrentLinkedQueue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private final TimeHelper chokeTimer = new TimeHelper();
    private boolean isFlushing = false;

    public FakeLag() {
        super("FakeLag", Keyboard.KEY_NONE, Category.COMBAT);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(maxChoke);
        Client.INSTANCE.settingsManager.rSetting(indicator);
    }

    @Override
    public void onEnable() {
        packetQueue.clear();
        chokeTimer.reset();
        isFlushing = false;
    }

    @Override
    public void onDisable() {
        flush();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer == null) return;
        this.setSuffix(mode.getMode() + " [" + packetQueue.size() + "]");

        // --- DEFINICJA LOGIKI FLUSH (WYPUSZCZANIA PAKIETÓW) ---

        // Sprawdzamy, czy gracz chce skakać lub jest w powietrzu
        boolean isJumping = mc.gameSettings.keyBindJump.isKeyDown();
        boolean isInAir = !mc.thePlayer.onGround;

        // Warunek bycia "w akcji" (czyli kiedy mamy lagować)
        boolean active = isInAir || isJumping;

        if (mode.is("Jump")) {
            // Jeśli stoimy na ziemi I nie trzymamy spacji -> wypuść pakiety
            if (!active && !packetQueue.isEmpty()) {
                flush();
            }

            // Zabezpieczenie czasowe (Anti-Kick) - jeśli trzymamy pakiety za długo
            if (!packetQueue.isEmpty() && chokeTimer.hasReached((long) maxChoke.getValue())) {
                flush();
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.thePlayer == null || isFlushing) return;

        if (event.getDirection() == EventPacket.Direction.SEND) {
            Packet<?> packet = event.getPacket();

            // Pakiety systemowe - przepuszczamy natychmiast
            if (packet instanceof C00Handshake ||
                    packet instanceof C00PacketLoginStart ||
                    packet instanceof C00PacketServerQuery ||
                    packet instanceof C00PacketKeepAlive ||
                    packet instanceof C0FPacketConfirmTransaction) {
                return;
            }

            // Sprawdzamy stan gracza
            boolean isJumping = mc.gameSettings.keyBindJump.isKeyDown();
            boolean isInAir = !mc.thePlayer.onGround;
            boolean active = isInAir || isJumping;

            boolean shouldLag = false;

            if (mode.is("Jump")) {
                // Jeśli jesteśmy aktywni (powietrze lub spacja) -> lagujemy pakiety ruchu/walki
                if (active) {
                    if (shouldLagPacket(packet)) {
                        shouldLag = true;
                    }
                }
            } else if (mode.is("Dynamic")) {
                // Tryb Dynamic - laguje zawsze do limitu
                if (shouldLagPacket(packet)) {
                    shouldLag = true;
                    if (chokeTimer.hasReached((long) maxChoke.getValue())) {
                        flush();
                        shouldLag = false;
                    }
                }
            }

            if (shouldLag) {
                if (packetQueue.isEmpty()) {
                    chokeTimer.reset();
                }
                packetQueue.add(packet);
                event.setCancelled(true);
            }
        }
    }

    private boolean shouldLagPacket(Packet<?> packet) {
        // Lagujemy tylko to, co zdradza pozycję lub akcję
        return packet instanceof C03PacketPlayer ||
                packet instanceof C02PacketUseEntity ||
                packet instanceof C0APacketAnimation ||
                packet instanceof C0BPacketEntityAction;
    }

    private void flush() {
        if (packetQueue.isEmpty()) return;

        isFlushing = true;
        while (!packetQueue.isEmpty()) {
            Packet<?> packet = packetQueue.poll();
            if (packet != null) {
                try {
                    // Bezpośrednie wysyłanie, omija EventPacket
                    mc.getNetHandler().getNetworkManager().sendPacket(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        isFlushing = false;
        chokeTimer.reset();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!indicator.isEnabled()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float width = 100;
        float height = 16;
        float x = (sr.getScaledWidth() / 2.0f) - (width / 2.0f);
        float y = (sr.getScaledHeight() / 2.0f) + 40;

        RenderUtil.drawRoundedRect(x, y, width, height, 4, new Color(0, 0, 0, 180).getRGB());

        String text;
        int color;

        if (packetQueue.isEmpty()) {
            text = "Status: Ground";
            color = 0xFFAAAAAA;
        } else {
            text = "LAGGING [" + packetQueue.size() + "]";
            color = 0xFFFF5555;

            float maxTime = (float) maxChoke.getValue();
            float progress = Math.min(1.0f, (float)chokeTimer.getTime() / maxTime);

            int barColor = Color.HSBtoRGB(0.33f * (1.0f - progress), 1.0f, 1.0f);
            RenderUtil.drawRoundedRect(x + 2, y + height - 3, (width - 4) * progress, 1, 0.5f, barColor);
        }

        FontManager.r20.drawStringWithShadow(text, x + width / 2 - FontManager.r20.getStringWidth(text) / 2, y + 4, color);
    }
}