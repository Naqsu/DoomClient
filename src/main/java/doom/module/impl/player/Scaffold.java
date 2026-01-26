package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.MoveUtil;
import doom.util.RotationUtil;
import doom.util.TimeHelper;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Scaffold extends Module {

    // === SETTINGS ===
    public ModeSetting rotMode = new ModeSetting("Rot Mode", this, "Grim", "Grim", "Smooth", "Snap");
    public ModeSetting towerMode = new ModeSetting("Tower", this, "Vanilla", "None", "Vanilla", "NCP");

    public BooleanSetting lockToBlock = new BooleanSetting("Lock to Block", this, true);
    public BooleanSetting sprint = new BooleanSetting("Sprint", this, false);
    public BooleanSetting swing = new BooleanSetting("Swing", this, true);
    public BooleanSetting safeWalk = new BooleanSetting("SafeWalk", this, true);
    public BooleanSetting strictRayCast = new BooleanSetting("Strict RayCast", this, false);
    public BooleanSetting lineAlign = new BooleanSetting("Line Align", this, true);

    public NumberSetting rotationSpeed = new NumberSetting("Rot Speed", this, 160.0, 10.0, 180.0, 5.0);
    public NumberSetting expand = new NumberSetting("Expand", this, 0.0, 0.0, 4.0, 0.1);

    // NOWE: Opóźnienie stawiania (dla Matrix/NCP)
    public NumberSetting placeDelay = new NumberSetting("Place Delay", this, 0.0, 0.0, 500.0, 10.0);

    // === STATE ===
    private BlockData currentData;
    private Vec3 currentHitVec;
    private int slot;

    private float[] targetRotation;
    private float[] lastServerRotations;

    private boolean rotationToggle = false;
    private final TimeHelper placeTimer = new TimeHelper(); // Timer do delaya

    private final List<Block> invalidBlocks = Arrays.asList(
            Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.lava, Blocks.flowing_lava,
            Blocks.enchanting_table, Blocks.chest, Blocks.ender_chest, Blocks.trapped_chest,
            Blocks.crafting_table, Blocks.furnace, Blocks.lit_furnace, Blocks.dispenser, Blocks.dropper,
            Blocks.tnt, Blocks.noteblock, Blocks.jukebox, Blocks.anvil, Blocks.bed, Blocks.skull,
            Blocks.ladder, Blocks.vine, Blocks.reeds, Blocks.tallgrass, Blocks.deadbush, Blocks.snow_layer,
            Blocks.glass, Blocks.stained_glass, Blocks.torch, Blocks.web, Blocks.sapling, Blocks.double_plant,
            Blocks.yellow_flower, Blocks.red_flower
    );

    public Scaffold() {
        super("Scaffold", Keyboard.KEY_B, Category.MOVEMENT);
        Client.INSTANCE.settingsManager.rSetting(rotMode);
        Client.INSTANCE.settingsManager.rSetting(towerMode);
        Client.INSTANCE.settingsManager.rSetting(lockToBlock);
        Client.INSTANCE.settingsManager.rSetting(sprint);
        Client.INSTANCE.settingsManager.rSetting(swing);
        Client.INSTANCE.settingsManager.rSetting(safeWalk);
        Client.INSTANCE.settingsManager.rSetting(strictRayCast);
        Client.INSTANCE.settingsManager.rSetting(lineAlign);
        Client.INSTANCE.settingsManager.rSetting(rotationSpeed);
        Client.INSTANCE.settingsManager.rSetting(expand);
        Client.INSTANCE.settingsManager.rSetting(placeDelay); // Dodano
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        lastServerRotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
        targetRotation = null;
        currentData = null;
        currentHitVec = null;
        slot = -1;
        rotationToggle = false;
        placeTimer.reset();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        if (mc.gameSettings.keyBindJump.isPressed()) {
            mc.gameSettings.keyBindJump.pressed = false;
        }
        mc.gameSettings.keyBindSneak.pressed = false;
        currentData = null;
        currentHitVec = null;
        targetRotation = null;
        RotationUtil.reset();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!event.isPre()) return;

        // 1. Slot check
        slot = getBlockSlot();
        if (slot == -1) return;
        mc.thePlayer.inventory.currentItem = slot;

        // 2. TARGET LOGIC
        if (lockToBlock.isEnabled() && currentData != null) {
            if (!isStillValid(currentData)) {
                currentData = null;
                currentHitVec = null;
            }
        } else if (!lockToBlock.isEnabled()) {
            currentData = null;
            currentHitVec = null;
        }

        if (currentData == null) {
            currentData = findBlockData();
            if (currentData != null) {
                currentHitVec = getHitVec(currentData);
            }
        }

        // 3. ROTATIONS
        if (currentData != null && currentHitVec != null) {
            if (rotMode.is("Grim")) {
                targetRotation = RotationUtil.getGrimRotations(currentData.pos, currentData.face);
            } else {
                targetRotation = RotationUtil.getRotationsToVec(currentHitVec);
            }
        }

        if (targetRotation != null) {
            float[] finalRots = targetRotation;

            // FIX DUPLICATE ROT PLACE
            if (lineAlign.isEnabled()) {
                float snapYaw = Math.round(finalRots[0] / 45.0f) * 45.0f;
                float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
                float gcd = sensitivity * sensitivity * sensitivity * 1.2F;
                rotationToggle = !rotationToggle;
                float jitter = rotationToggle ? gcd : -gcd;
                finalRots[0] = snapYaw + jitter;
            }

            if (rotMode.is("Smooth")) {
                float speed = (float) rotationSpeed.getValue();
                finalRots = RotationUtil.smooth(lastServerRotations, finalRots, speed);
            }

            finalRots = RotationUtil.applyGCD(lastServerRotations, finalRots);
            lastServerRotations = finalRots;

            event.setYaw(finalRots[0]);
            event.setPitch(finalRots[1]);

            RotationUtil.setRotation(finalRots[0], finalRots[1]);

            float yawDiff = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - finalRots[0]);
            if (Math.abs(yawDiff) > 60 && sprint.isEnabled()) {
                mc.thePlayer.setSprinting(false);
                event.setSprinting(false);
            } else if (sprint.isEnabled() && MoveUtil.isMoving() && !mc.thePlayer.isCollidedHorizontally && Math.abs(yawDiff) < 60) {
                mc.thePlayer.setSprinting(true);
            } else {
                mc.thePlayer.setSprinting(false);
            }
        }

        // 4. SafeWalk
        if (safeWalk.isEnabled() && mc.thePlayer.onGround) {
            BlockPos under = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
            if (mc.theWorld.getBlockState(under).getBlock() == Blocks.air) {
                mc.gameSettings.keyBindSneak.pressed = true;
            } else if (mc.gameSettings.keyBindSneak.isPressed()) {
                mc.gameSettings.keyBindSneak.pressed = false;
            }
        }

        // 5. Tower
        handleTower();

        // 6. Placing with Delay
        if (currentData != null && currentHitVec != null) {
            if (strictRayCast.isEnabled()) {
                if (!isRayCastHit(currentData, event.getYaw(), event.getPitch())) {
                    return;
                }
            }

            // --- FIX MATRIX PLACE TOO FAST ---
            // Sprawdzamy timer. Jeśli ustawiony na 0, działa jak dawniej.
            if (placeTimer.hasReached(placeDelay.getValue())) {
                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getStackInSlot(slot), currentData.pos, currentData.face, currentHitVec)) {
                    if (swing.isEnabled()) mc.thePlayer.swingItem();
                    else mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());

                    placeTimer.reset(); // Resetujemy timer po udanym postawieniu
                }
            }
        }
    }

    // === TARGETING LOGIC ===

    private boolean isStillValid(BlockData data) {
        if (mc.thePlayer.getDistanceSq(data.pos) > 5.0 * 5.0) return false;

        Block blockAgainst = mc.theWorld.getBlockState(data.pos).getBlock();
        if (invalidBlocks.contains(blockAgainst)) return false;

        BlockPos placePos = data.pos.offset(data.face);
        if (!mc.theWorld.getBlockState(placePos).getBlock().getMaterial().isReplaceable()) {
            return false;
        }
        return true;
    }

    private BlockData findBlockData() {
        EntityPlayerSP player = mc.thePlayer;
        BlockPos pos = new BlockPos(player.posX, player.posY - 1.0, player.posZ);

        if (expand.getValue() > 0 && MoveUtil.isMoving()) {
            double yaw = Math.toRadians(player.rotationYaw);
            pos = pos.add(-Math.sin(yaw) * expand.getValue(), 0, Math.cos(yaw) * expand.getValue());
        }

        if (!isValidBlock(pos)) return getBlockData(pos);
        return null;
    }

    private BlockData getBlockData(BlockPos pos) {
        BlockPos[] offsets = {
                pos.add(0, -1, 0), pos.add(0, 0, 1), pos.add(-1, 0, 0), pos.add(1, 0, 0), pos.add(0, 0, -1)
        };
        EnumFacing[] facings = {
                EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH
        };

        for (int i = 0; i < offsets.length; i++) {
            if (isValidBlock(offsets[i])) {
                if (facings[i] == EnumFacing.UP && MoveUtil.isMoving() && !mc.gameSettings.keyBindJump.isKeyDown()) continue;
                return new BlockData(offsets[i], facings[i]);
            }
        }
        return null;
    }

    // === UTILS ===

    private boolean isValidBlock(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return !invalidBlocks.contains(block) && block.isFullCube() && !block.getMaterial().isLiquid();
    }

    private Vec3 getHitVec(BlockData data) {
        BlockPos pos = data.pos;
        EnumFacing face = data.face;
        double x = pos.getX() + 0.5 + face.getFrontOffsetX() * 0.5;
        double y = pos.getY() + 0.5 + face.getFrontOffsetY() * 0.5;
        double z = pos.getZ() + 0.5 + face.getFrontOffsetZ() * 0.5;

        if (!lineAlign.isEnabled()) {
            double randomXY = ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
            double randomZ = ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
            if (face.getAxis() != EnumFacing.Axis.Y) y += randomXY; else { x += randomXY; z += randomZ; }
        }
        return new Vec3(x, y, z);
    }

    private boolean isRayCastHit(BlockData data, float yaw, float pitch) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 look = RotationUtil.getVectorForRotation(pitch, yaw);
        double range = 4.5;
        Vec3 vec = eyes.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        MovingObjectPosition obj = mc.theWorld.rayTraceBlocks(eyes, vec, false, false, true);

        return obj != null && obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && obj.getBlockPos().equals(data.pos) && obj.sideHit == data.face;
    }

    private void handleTower() {
        if (mc.gameSettings.keyBindJump.isKeyDown() && !MoveUtil.isMoving()) {
            if (towerMode.is("Vanilla")) mc.thePlayer.motionY = 0.5;
            else if (towerMode.is("NCP")) {
                if (mc.thePlayer.onGround) mc.thePlayer.motionY = 0.42;
                if (mc.thePlayer.motionY < 0.23 && !mc.thePlayer.onGround) mc.thePlayer.motionY = 0.5;
            }
        }
    }

    private int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && stack.stackSize > 0) {
                if (!invalidBlocks.contains(((ItemBlock) stack.getItem()).getBlock())) return i;
            }
        }
        return -1;
    }

    public static class BlockData {
        public final BlockPos pos;
        public final EnumFacing face;
        public BlockData(BlockPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
        }
    }
}