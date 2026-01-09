package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.event.impl.EventUpdate;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.RenderUtil;
import doom.util.RotationUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.awt.Color;

public class BedBreaker extends Module {

    // === SETTINGS ===
    private final NumberSetting range = new NumberSetting("Range", this, 4.5, 1.0, 6.0, 0.1);
    // Suwak szybkości niszczenia (1.0 = normalnie, 2.0 = 2x szybciej, 3.0 = 3x szybciej)
    private final NumberSetting breakSpeed = new NumberSetting("Break Speed", this, 1.5, 1.0, 3.0, 0.1);

    private final BooleanSetting rotations = new BooleanSetting("Rotations", this, true);
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", this, true);
    private final BooleanSetting esp = new BooleanSetting("ESP", this, true);
    private final BooleanSetting swing = new BooleanSetting("Swing", this, true);

    // === STATE ===
    private BlockPos currentTarget;
    private float[] myRotations;

    // Progres niszczenia (od 0.0 do 1.0)
    private float currentDamage = 0.0f;
    private boolean isBreaking = false;

    public BedBreaker() {
        super("BedBreaker", 0, Category.PLAYER);
        Client.INSTANCE.settingsManager.rSetting(range);
        Client.INSTANCE.settingsManager.rSetting(breakSpeed);
        Client.INSTANCE.settingsManager.rSetting(rotations);
        Client.INSTANCE.settingsManager.rSetting(throughWalls);
        Client.INSTANCE.settingsManager.rSetting(esp);
        Client.INSTANCE.settingsManager.rSetting(swing);
    }

    @Override
    public void onEnable() {
        resetState();
        if (mc.thePlayer != null) {
            myRotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        }
    }

    @Override
    public void onDisable() {
        resetState();
        RotationUtil.shouldUseCustomPitch = false;
    }

    private void resetState() {
        currentTarget = null;
        currentDamage = 0.0f;
        isBreaking = false;
        // Ważne: resetujemy stan niszczenia w kontrolerze, żeby nie było glitchy
        mc.playerController.resetBlockRemoving();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // 1. Walidacja celu
        if (currentTarget != null) {
            if (!isValid(currentTarget)) {
                resetState(); // Cel zniknął lub jest za daleko
            }
        }

        // 2. Szukanie nowego celu (tylko jeśli nie kopiemy lub cel zniknął)
        if (currentTarget == null) {
            currentTarget = findBed();
        }

        // Jeśli nadal brak, wyjdź
        if (currentTarget == null) {
            RotationUtil.shouldUseCustomPitch = false;
            myRotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
            return;
        }

        // 3. ROTACJE (Płynne, żeby ominąć Grima)
        if (rotations.isEnabled()) {
            Vec3 targetVec = new Vec3(currentTarget.getX() + 0.5, currentTarget.getY() + 0.5, currentTarget.getZ() + 0.5);
            float[] destRots = RotationUtil.getRotationsToVec(targetVec);

            // Wygładzanie (Smooth)
            float speed = 60.0f; // Wolniej = bezpieczniej
            float yawDiff = net.minecraft.util.MathHelper.wrapAngleTo180_float(destRots[0] - myRotations[0]);
            float pitchDiff = destRots[1] - myRotations[1];

            if (yawDiff > speed) yawDiff = speed;
            if (yawDiff < -speed) yawDiff = -speed;
            if (pitchDiff > speed) pitchDiff = speed;
            if (pitchDiff < -speed) pitchDiff = -speed;

            myRotations[0] += yawDiff;
            myRotations[1] += pitchDiff;

            // Ustawiamy rotacje
            event.setYaw(myRotations[0]);
            event.setPitch(myRotations[1]);

            mc.thePlayer.rotationYawHead = myRotations[0];
            mc.thePlayer.renderYawOffset = myRotations[0];
            RotationUtil.renderPitch = myRotations[1];
            RotationUtil.shouldUseCustomPitch = true;
        }

        // 4. LOGIKA NISZCZENIA (PROGRESSIVE)

        // Czekamy aż celownik "najedzie" na blok (kąt < 20 stopni)
        if (rotations.isEnabled() && !isLookingAtBlock(currentTarget, myRotations, 20.0f)) {
            return;
        }

        // Rozpoczęcie kopania (wysłanie pakietu START)
        if (!isBreaking) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, currentTarget, EnumFacing.UP));
            isBreaking = true;
            currentDamage = 0.0f;

            if (swing.isEnabled()) mc.thePlayer.swingItem();
            else mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
        }
        else {
            // Kontynuacja kopania (naliczanie uszkodzeń)
            if (swing.isEnabled()) mc.thePlayer.swingItem();
            else mc.getNetHandler().addToSendQueue(new C0APacketAnimation()); // Spam swing packet aby serwer wiedział że kopiemy

            // Obliczamy ile % łóżka niszczymy w tym ticku
            // getPlayerRelativeBlockHardness zwraca np. 0.3 (30% na tick)
            float damagePerTick = mc.theWorld.getBlockState(currentTarget).getBlock().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, currentTarget);

            // APLIKUJEMY MNOŻNIK PRĘDKOŚCI
            currentDamage += (damagePerTick * breakSpeed.getValue());

            // Wizualny progres niszczenia w świecie (cracks)
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), currentTarget, (int)(currentDamage * 10) - 1);

            // Jeśli zniszczono (>= 100%)
            if (currentDamage >= 1.0f) {
                // Wyślij STOP (zniszcz)
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, currentTarget, EnumFacing.UP));

                // Resetujemy, żeby szukać następnego
                resetState();
            }
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (currentTarget != null && esp.isEnabled()) {
            double x = currentTarget.getX() - mc.getRenderManager().viewerPosX;
            double y = currentTarget.getY() - mc.getRenderManager().viewerPosY;
            double z = currentTarget.getZ() - mc.getRenderManager().viewerPosZ;

            // Kolory
            int outlineColor = new Color(255, 50, 50, 255).getRGB();
            // Wypełnienie zmienia się w zależności od progresu kopania!
            // Zaczyna od przezroczystego, staje się bardziej widoczne im bliżej zniszczenia
            int alpha = (int) (60 + (currentDamage * 100)); // 60 -> 160
            if (alpha > 255) alpha = 255;
            int fillColor = new Color(255, 50, 50, alpha).getRGB();

            // Wysokość wypełnienia rośnie wraz z niszczeniem
            // 0% = płaskie, 100% = całe łóżko
            double height = 0.5625 * (currentDamage > 0 ? currentDamage : 1.0);
            if (height > 0.5625) height = 0.5625;

            // Rysujemy obrys całego łóżka
            net.minecraft.util.AxisAlignedBB fullBB = new net.minecraft.util.AxisAlignedBB(x, y, z, x + 1, y + 0.5625, z + 1);
            RenderUtil.drawBoundingBox(fullBB, 2.0f, outlineColor);

            // Rysujemy wypełnienie (progres)
            net.minecraft.util.AxisAlignedBB progressBB = new net.minecraft.util.AxisAlignedBB(x, y, z, x + 1, y + height, z + 1);
            RenderUtil.drawFilledBox(progressBB, fillColor);

            // Tracer
            RenderUtil.drawTracerLine(currentTarget, new Color(255, 50, 50));
        }
    }

    private BlockPos findBed() {
        int r = (int) range.getValue();
        BlockPos bestBed = null;
        double closestDist = Double.MAX_VALUE;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();

                    if (block == Blocks.bed) {
                        double dist = mc.thePlayer.getDistanceSq(pos);
                        if (dist < range.getValue() * range.getValue()) {
                            // Through Walls logic
                            if (!throughWalls.isEnabled()) {
                                Vec3 eyes = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
                                Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                if (mc.theWorld.rayTraceBlocks(eyes, center, false, true, false) != null) continue;
                            }

                            if (dist < closestDist) {
                                closestDist = dist;
                                bestBed = pos;
                            }
                        }
                    }
                }
            }
        }
        return bestBed;
    }

    private boolean isValid(BlockPos pos) {
        if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.bed) return false;
        return mc.thePlayer.getDistanceSq(pos) <= range.getValue() * range.getValue();
    }

    private boolean isLookingAtBlock(BlockPos pos, float[] rotations, float tolerance) {
        float[] needed = RotationUtil.getRotationsToVec(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        float yawDiff = Math.abs(net.minecraft.util.MathHelper.wrapAngleTo180_float(needed[0] - rotations[0]));
        float pitchDiff = Math.abs(needed[1] - rotations[1]);
        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }
}