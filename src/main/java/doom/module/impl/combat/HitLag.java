package doom.module.impl.combat;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventRender2D;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import doom.util.TimeHelper;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HitLag extends Module {

    private final NumberSetting maxChoke = new NumberSetting("Max Choke (ms)", this, 300, 50, 1000, 50);
    private final NumberSetting maxDist = new NumberSetting("Max Dist (Blocks)", this, 3.0, 1.0, 5.0, 0.5);
    private final BooleanSetting showGhost = new BooleanSetting("Show Ghost", this, true);
    private final BooleanSetting onlyWeapon = new BooleanSetting("Weapon Only", this, true);

    private final List<Packet<?>> packetBuffer = new ArrayList<>();
    private final TimeHelper timer = new TimeHelper();
    private EntityOtherPlayerMP ghostEntity;
    private boolean isFlushing = false;

    public HitLag() {
        super("HitLag", Keyboard.KEY_NONE, Category.COMBAT);
        Client.INSTANCE.settingsManager.rSetting(maxChoke);
        Client.INSTANCE.settingsManager.rSetting(maxDist);
        Client.INSTANCE.settingsManager.rSetting(showGhost);
        Client.INSTANCE.settingsManager.rSetting(onlyWeapon);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        packetBuffer.clear();
        timer.reset();
        spawnGhost();
    }

    @Override
    public void onDisable() {
        flush();
        despawnGhost();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer == null) return;

        this.setSuffix(String.valueOf(packetBuffer.size()));

        // 1. Zabezpieczenie czasowe
        if (timer.hasReached(maxChoke.getValue())) {
            flush();
            return;
        }

        // 2. Zabezpieczenie dystansowe (NAJWAŻNIEJSZE NA VULCANA)
        // Jeśli odejdziemy za daleko od ducha, serwer uzna flush za teleport/speed.
        if (ghostEntity != null) {
            double dist = mc.thePlayer.getDistanceToEntity(ghostEntity);
            if (dist > maxDist.getValue()) {
                flush();
                return;
            }
        }

        // 3. Logika Uniku (Smart)
        if (ghostEntity != null) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer || player.isDead || player.isInvisible()) continue;

                // Sprawdzamy graczy w zasięgu 6 kratek
                if (mc.thePlayer.getDistanceToEntity(player) < 6.0) {

                    // Czy gracz ma broń?
                    if (onlyWeapon.isEnabled() && !isHoldingWeapon(player)) continue;

                    // Czy patrzy na naszego ducha?
                    if (isLookingAtEntity(player, ghostEntity)) {
                        flush();
                        Client.addChatMessage("§aDodge! " + player.getName());
                        return;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.thePlayer == null || isFlushing) return;

        Packet<?> p = event.getPacket();

        if (event.getDirection() == EventPacket.Direction.SEND) {
            // Zatrzymujemy WSZYSTKO co ważne, żeby wyglądało na LAG
            if (p instanceof C03PacketPlayer ||             // Ruch
                    p instanceof C0BPacketEntityAction ||       // Sprint/Sneak
                    p instanceof C0APacketAnimation ||          // Machanie ręką
                    p instanceof C02PacketUseEntity ||          // Atak
                    p instanceof C0FPacketConfirmTransaction || // Ping
                    p instanceof C00PacketKeepAlive) {          // Ping

                event.setCancelled(true);
                packetBuffer.add(p);

                // Jeśli to pierwszy pakiet ruchu, tworzymy ducha w starej pozycji
                if (packetBuffer.size() == 1) {
                    // spawnGhost() jest wołane w flush(), więc tutaj jest ok
                }
            }
        }
    }

    private void flush() {
        if (packetBuffer.isEmpty()) return;

        isFlushing = true;
        try {
            for (Packet<?> p : packetBuffer) {
                mc.getNetHandler().getNetworkManager().sendPacket(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        packetBuffer.clear();
        isFlushing = false;

        timer.reset();

        // Resetujemy ducha do NOWEJ pozycji
        despawnGhost();
        spawnGhost();
    }

    private boolean isLookingAtEntity(EntityLivingBase source, Entity target) {
        Vec3 eyes = source.getPositionEyes(1.0f);
        Vec3 look = source.getLook(1.0f);
        Vec3 end = eyes.addVector(look.xCoord * 8, look.yCoord * 8, look.zCoord * 8);

        // Powiększamy hitbox ducha, żeby "łapać" celowanie wcześniej
        // 0.6 to szerokość gracza + margines błędu
        return target.getEntityBoundingBox().expand(0.6, 1.8, 0.6).calculateIntercept(eyes, end) != null;
    }

    private boolean isHoldingWeapon(EntityPlayer player) {
        ItemStack stack = player.getHeldItem();
        return stack != null && (stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemAxe);
    }

    private void spawnGhost() {
        if (showGhost.isEnabled() && ghostEntity == null && mc.thePlayer != null) {
            ghostEntity = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
            // Kopiujemy pozycję DOKŁADNIE tam gdzie serwer nas widzi (czyli tu gdzie stoimy przed lagiem)
            ghostEntity.setPositionAndRotation(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            ghostEntity.rotationYawHead = mc.thePlayer.rotationYawHead;
            ghostEntity.renderYawOffset = mc.thePlayer.renderYawOffset;
            ghostEntity.inventory = mc.thePlayer.inventory;
            ghostEntity.setSneaking(mc.thePlayer.isSneaking());

            mc.theWorld.addEntityToWorld(-6969, ghostEntity);
        }
    }

    private void despawnGhost() {
        if (ghostEntity != null) {
            mc.theWorld.removeEntityFromWorld(-6969);
            ghostEntity = null;
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!packetBuffer.isEmpty()) {
            ScaledResolution sr = new ScaledResolution(mc);

            float percentage = (float) Math.min(1.0, (float)timer.getTime() / maxChoke.getValue());
            float width = 60;
            float x = sr.getScaledWidth() / 2.0f - width/2.0f;
            float y = sr.getScaledHeight() / 2.0f + 35;

            // Kolor zmienia się od zielonego do czerwonego
            int color = Color.HSBtoRGB(0.33f * (1.0f - percentage), 1.0f, 1.0f);

            RenderUtil.drawRoundedRect(x, y, width, 4, 2, new Color(0,0,0,100).getRGB());
            RenderUtil.drawRoundedRect(x, y, width * percentage, 4, 2, color);
            FontManager.r18.drawCenteredString("HitLag", x + width/2, y - 5, -1);
        }
    }
}