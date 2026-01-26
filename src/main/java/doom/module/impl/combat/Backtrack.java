package doom.module.impl.combat;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.event.impl.EventRender3D;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.module.impl.combat.Killaura;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.RenderUtil;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Backtrack extends Module {

    // --- USTAWIENIA ---
    public NumberSetting delay = new NumberSetting("Delay (ms)", this, 400, 50, 1000, 50);
    public BooleanSetting esp = new BooleanSetting("Render ESP", this, true);
    public BooleanSetting onlyCombat = new BooleanSetting("Only Combat", this, true);

    // --- ZMIENNE ---
    private final ConcurrentLinkedQueue<DelayedPacket> packetBuffer = new ConcurrentLinkedQueue<>();
    private EntityLivingBase target = null;
    private EntityOtherPlayerMP realTargetGhost = null;
    private Vec3 realPos = null;

    public Backtrack() {
        super("Backtrack", Keyboard.KEY_NONE, Category.COMBAT);
        Client.INSTANCE.settingsManager.rSetting(delay);
        Client.INSTANCE.settingsManager.rSetting(esp);
        Client.INSTANCE.settingsManager.rSetting(onlyCombat);
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        while (!packetBuffer.isEmpty()) {
            DelayedPacket p = packetBuffer.poll();
            if (p != null) p.process();
        }

        if (realTargetGhost != null) {
            mc.theWorld.removeEntityFromWorld(realTargetGhost.getEntityId());
            realTargetGhost = null;
        }
        target = null;
        realPos = null;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(String.valueOf((int)delay.getValue()) + "ms");

        if (mc.thePlayer == null || mc.theWorld == null) {
            reset();
            return;
        }

        Killaura aura = Client.INSTANCE.moduleManager.getModule(Killaura.class);
        if (onlyCombat.isEnabled()) {
            if (aura == null || aura.target == null) {
                if (!packetBuffer.isEmpty()) {
                    reset();
                }
                return;
            }
            target = aura.target;
        } else {
            target = getClosestPlayer(6.0);
        }

        // --- PRZETWARZANIE KOLEJKI ---
        if (!packetBuffer.isEmpty()) {
            long maxDelay = (long) delay.getValue();
            boolean shouldRelease = false;

            if (target == null) shouldRelease = true;
            else if (mc.thePlayer.getDistanceToEntity(target) > 6.0) shouldRelease = true;

            while (!packetBuffer.isEmpty()) {
                DelayedPacket p = packetBuffer.peek();

                if (shouldRelease || System.currentTimeMillis() - p.time > maxDelay) {
                    DelayedPacket packetToProcess = packetBuffer.poll();
                    if (packetToProcess != null) packetToProcess.process();
                } else {
                    break;
                }
            }
        }

        // Render ESP
        if (esp.isEnabled() && target != null && realPos != null) {
            if (realTargetGhost == null) {
                realTargetGhost = new EntityOtherPlayerMP(mc.theWorld, ((EntityPlayer)target).getGameProfile());
                realTargetGhost.copyLocationAndAnglesFrom(target);
                realTargetGhost.rotationYawHead = target.getRotationYawHead();
                realTargetGhost.setInvisible(true);
                mc.theWorld.addEntityToWorld(-1337, realTargetGhost);
            }
            realTargetGhost.setPositionAndRotation(realPos.xCoord, realPos.yCoord, realPos.zCoord, target.rotationYaw, target.rotationPitch);
        } else if (realTargetGhost != null) {
            mc.theWorld.removeEntityFromWorld(-1337);
            realTargetGhost = null;
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getDirection() == EventPacket.Direction.RECEIVE) {
            Packet<?> packet = event.getPacket();

            if (target == null) return;

            if (packet instanceof S14PacketEntity || packet instanceof S18PacketEntityTeleport) {
                int entityId = -1;

                if (packet instanceof S14PacketEntity) entityId = ((S14PacketEntity) packet).getEntity(mc.theWorld).getEntityId();
                if (packet instanceof S18PacketEntityTeleport) entityId = ((S18PacketEntityTeleport) packet).getEntityId();

                if (entityId == target.getEntityId()) {
                    event.setCancelled(true);
                    packetBuffer.add(new DelayedPacket(packet));
                    updateRealPos(packet);
                }
            }
        }
    }

    private void updateRealPos(Packet<?> packet) {
        if (realPos == null) realPos = new Vec3(target.posX, target.posY, target.posZ);

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            realPos = realPos.addVector(p.func_149062_c() / 32.0D, p.func_149061_d() / 32.0D, p.func_149064_e() / 32.0D);
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            realPos = new Vec3(p.getX() / 32.0D, p.getY() / 32.0D, p.getZ() / 32.0D);
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (esp.isEnabled() && target != null && !packetBuffer.isEmpty() && realPos != null) {
            double x = realPos.xCoord - mc.getRenderManager().viewerPosX;
            double y = realPos.yCoord - mc.getRenderManager().viewerPosY;
            double z = realPos.zCoord - mc.getRenderManager().viewerPosZ;

            AxisAlignedBB bb = target.getEntityBoundingBox().offset(
                    realPos.xCoord - target.posX,
                    realPos.yCoord - target.posY,
                    realPos.zCoord - target.posZ
            ).offset(-mc.getRenderManager().viewerPosX, -mc.getRenderManager().viewerPosY, -mc.getRenderManager().viewerPosZ);

            RenderUtil.drawBoundingBox(bb, 1.5f, new Color(0, 255, 0, 150).getRGB());
            RenderUtil.drawFilledBox(bb, new Color(0, 255, 0, 40).getRGB());
            RenderUtil.drawTracerLine(new net.minecraft.util.BlockPos(realPos.xCoord, realPos.yCoord, realPos.zCoord), Color.GREEN);
        }
    }

    private class DelayedPacket {
        Packet<?> packet;
        long time;

        public DelayedPacket(Packet<?> packet) {
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        public void process() {
            try {
                // --- FIX: RZUTOWANIE NA Packet<INetHandlerPlayClient> ---
                // To naprawia błąd kompilacji. Mówimy kompilatorowi:
                // "Wiem, że ten pakiet obsługuje Play Client Handler, więc pozwól mi go tam wysłać".
                if (packet instanceof Packet) {
                    ((Packet<INetHandlerPlayClient>) packet).processPacket(mc.getNetHandler());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private EntityPlayer getClosestPlayer(double range) {
        EntityPlayer closest = null;
        double dist = range;
        for(EntityPlayer p : mc.theWorld.playerEntities) {
            if(p != mc.thePlayer && mc.thePlayer.getDistanceToEntity(p) < dist) {
                dist = mc.thePlayer.getDistanceToEntity(p);
                closest = p;
            }
        }
        return closest;
    }
}