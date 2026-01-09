package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.RotationUtil;
import doom.util.TimeHelper;
import doom.util.TimerUtil;
import net.minecraft.block.Block;
import net.minecraft.client.settings.KeyBinding;
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

    // Settings
    public ModeSetting rotationMode = new ModeSetting("Rotation", this, "VulcanSprint", "VulcanSprint", "Grim", "Strict", "Normal");
    public BooleanSetting sprint = new BooleanSetting("Sprint", this, false);
    public BooleanSetting tower = new BooleanSetting("Tower", this, false);
    public BooleanSetting jump = new BooleanSetting("Jump", this, true);
    public BooleanSetting swing = new BooleanSetting("Swing", this, true);
    public BooleanSetting silent = new BooleanSetting("Silent", this, true);
    public BooleanSetting rayCast = new BooleanSetting("RayCast", this, true);
    // Zmniejszamy delay domyślny, bo przy szybkim skakaniu musi stawiać natychmiast
    public NumberSetting placeDelay = new NumberSetting("Delay", this, 0, 0, 5, 1);

    // Data
    private BlockData targetBlock;
    private float[] currentRotations = new float[]{0, 0};
    private float lastYaw, lastPitch;
    private int currentSlot = -1;
    private double keepY = -1;
    private final TimeHelper timer = new TimeHelper();

    // Movement Fix vars
    private float originalForward, originalStrafe;

    private final List<Block> invalidBlocks = Arrays.asList(
            Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.lava, Blocks.flowing_lava,
            Blocks.enchanting_table, Blocks.chest, Blocks.ender_chest, Blocks.trapped_chest,
            Blocks.crafting_table, Blocks.furnace, Blocks.lit_furnace, Blocks.dispenser, Blocks.dropper,
            Blocks.tnt, Blocks.noteblock, Blocks.jukebox, Blocks.anvil, Blocks.bed, Blocks.skull,
            Blocks.ladder, Blocks.vine, Blocks.reeds, Blocks.tallgrass, Blocks.deadbush, Blocks.snow_layer,
            Blocks.glass, Blocks.stained_glass, Blocks.torch, Blocks.web, Blocks.sapling, Blocks.double_plant
    );

    public Scaffold() {
        super("Scaffold", Keyboard.KEY_B, Category.MOVEMENT);
        Client.INSTANCE.settingsManager.rSetting(rotationMode);
        Client.INSTANCE.settingsManager.rSetting(sprint);
        Client.INSTANCE.settingsManager.rSetting(tower);
        Client.INSTANCE.settingsManager.rSetting(jump);
        Client.INSTANCE.settingsManager.rSetting(swing);
        Client.INSTANCE.settingsManager.rSetting(silent);
        Client.INSTANCE.settingsManager.rSetting(rayCast);
        Client.INSTANCE.settingsManager.rSetting(placeDelay);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        lastYaw = mc.thePlayer.rotationYaw - 180;
        lastPitch = 80.0f;
        currentRotations[0] = lastYaw;
        currentRotations[1] = lastPitch;

        currentSlot = mc.thePlayer.inventory.currentItem;
        keepY = Math.floor(mc.thePlayer.posY - 1.0);

        timer.reset();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        if (currentSlot != -1) mc.thePlayer.inventory.currentItem = currentSlot;
        TimerUtil.setTimerSpeed(1.0f);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // 1. Slot
        int slot = getBlockSlot();
        if (slot == -1) return;
        mc.thePlayer.inventory.currentItem = slot;

        // 2. Keep Y
        if (!jump.isEnabled() || mc.gameSettings.keyBindJump.isKeyDown()) {
            keepY = Math.floor(mc.thePlayer.posY - 1.0);
        } else {
            if (mc.thePlayer.posY - 1.0 < keepY && mc.thePlayer.onGround) {
                keepY = Math.floor(mc.thePlayer.posY - 1.0);
            }
        }

        // 3. Find Block
        BlockPos searchPos = new BlockPos(mc.thePlayer.posX, keepY, mc.thePlayer.posZ);
        BlockData newTarget = getBlockData(searchPos);
        if (newTarget != null) targetBlock = newTarget;

        // 4. Capture Input
        if (silent.isEnabled()) {
            originalForward = mc.thePlayer.movementInput.moveForward;
            originalStrafe = mc.thePlayer.movementInput.moveStrafe;
        }

        // 5. Rotation Logic
        float[] rots = new float[]{lastYaw, lastPitch};
        boolean forceSprintOff = false;
        boolean forceSprintOn = false;

        if (targetBlock != null) {
            float[] blockRots = getRotations(targetBlock);

            // Lekki Jitter
            blockRots[0] += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.5f;
            blockRots[1] += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.5f;

            // --- VULCAN SPRINT LOGIC FIX ---
            if (rotationMode.getMode().equalsIgnoreCase("VulcanSprint")) {
                // Wykrywamy fazę skoku (Wznoszenie)
                // Zmieniono próg motionY na 0.15 (wcześniej 0.05), żeby szybciej wracał do bloku pod koniec skoku
                boolean isJumpingUp = !mc.thePlayer.onGround && mc.thePlayer.motionY > 0.15 && !mc.gameSettings.keyBindJump.isKeyDown();

                if (isJumpingUp && isMoving()) {
                    // Faza 1: Lot w górę -> Patrz w przód
                    float movementYaw = getMovementYaw(mc.thePlayer.rotationYaw, originalForward, originalStrafe);

                    // Płynne przejście w przód (0.25f)
                    rots[0] = interpolateRotation(lastYaw, movementYaw, 0.25f);
                    rots[1] = interpolateRotation(lastPitch, 5.0f, 0.25f);

                    forceSprintOn = true;
                } else {
                    // Faza 2: Opadanie lub Ziemia -> SZYBKI powrót na blok
                    // Zwiększono prędkość interpolacji do 0.8f (prawie instant), żeby zdążyć przed upadkiem
                    rots[0] = interpolateRotation(lastYaw, blockRots[0], 0.6f);
                    rots[1] = interpolateRotation(lastPitch, blockRots[1], 0.6f);

                    forceSprintOff = true;
                }
            }
            else {
                // Inne tryby (Grim/Strict)
                if (rotationMode.getMode().equalsIgnoreCase("Grim")) {
                    rots = RotationUtil.applyGCD(new float[]{lastYaw, lastPitch}, blockRots);
                } else {
                    rots = blockRots;
                }
            }

            if (rotationMode.getMode().equalsIgnoreCase("VulcanSprint")) {
                rots = RotationUtil.applyGCD(new float[]{lastYaw, lastPitch}, rots);
            }

            lastYaw = rots[0];
            lastPitch = rots[1];
        }
        currentRotations = rots;

        // 6. Apply Rotations
        event.setYaw(currentRotations[0]);
        event.setPitch(currentRotations[1]);

        mc.thePlayer.rotationYawHead = currentRotations[0];
        mc.thePlayer.renderYawOffset = currentRotations[0];
        RotationUtil.renderPitch = currentRotations[1];
        RotationUtil.shouldUseCustomPitch = true;

        // 7. Movement & Sprint
        if (silent.isEnabled()) {
            updateMovement(currentRotations[0]);

            if (rotationMode.getMode().equalsIgnoreCase("VulcanSprint")) {
                if (forceSprintOn) mc.thePlayer.setSprinting(true);
                else if (forceSprintOff) mc.thePlayer.setSprinting(false);
            } else {
                // Standard Smart Sprint
                if (sprint.isEnabled() && isMoving()) {
                    float moveYaw = getMovementYaw(mc.thePlayer.rotationYaw, originalForward, originalStrafe);
                    float diff = Math.abs(MathHelper.wrapAngleTo180_float(moveYaw - currentRotations[0]));
                    if (diff > 60) mc.thePlayer.setSprinting(false);
                    else mc.thePlayer.setSprinting(true);
                } else {
                    mc.thePlayer.setSprinting(false);
                }
            }
        } else {
            mc.thePlayer.rotationYaw = currentRotations[0];
            mc.thePlayer.rotationPitch = currentRotations[1];
            if (!sprint.isEnabled()) mc.thePlayer.setSprinting(false);
        }

        // 8. Auto Jump
        if (jump.isEnabled() && isMoving() && mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.thePlayer.jump();
        }

        // 9. Placing Logic
        long randomDelay = (long) (placeDelay.getValue() * 50);
        if (randomDelay < 60) randomDelay = 60 + ThreadLocalRandom.current().nextInt(40);
        if (targetBlock != null && mc.thePlayer.getDistanceSq(targetBlock.pos) <= 25 && timer.hasReached(randomDelay)) {
            // RayCast Logic - tutaj jest zabezpieczenie przed stawianiem w powietrzu
            // Jeśli RayCast nie widzi bloku (bo patrzymy w przód), po prostu nie postawi.
            // Usunąłem sztuczną blokadę "pitch < 70", bo powodowała spadanie.
            if (rayCast.isEnabled()) {
                if (!isLookingAtBlock(event.getYaw(), event.getPitch(), targetBlock.pos)) {
                    return;
                }
            }

            if (swing.isEnabled()) mc.thePlayer.swingItem();
            else mc.getNetHandler().addToSendQueue(new C0APacketAnimation());

            if (swing.isEnabled()) mc.thePlayer.swingItem();
            else mc.getNetHandler().addToSendQueue(new C0APacketAnimation());

            boolean placed = placeBlock(targetBlock);

            if (placed) {
                if (tower.isEnabled() && mc.gameSettings.keyBindJump.isKeyDown() && !isMoving()) {
                    mc.thePlayer.motionY = 0.42f;
                    mc.thePlayer.motionX = 0;
                    mc.thePlayer.motionZ = 0;
                }
            }

            timer.reset();
        }
    }

    private float interpolateRotation(float current, float target, float speed) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        if (Math.abs(diff) < 2.0f) return target;
        return current + diff * speed;
    }

    private float getMovementYaw(float baseYaw, float forward, float strafe) {
        if (forward == 0 && strafe == 0) return baseYaw;

        float moveYaw = baseYaw;
        if (forward > 0) {
            if (strafe > 0) moveYaw -= 45;
            else if (strafe < 0) moveYaw += 45;
        } else if (forward < 0) {
            moveYaw += 180;
            if (strafe > 0) moveYaw += 45;
            else if (strafe < 0) moveYaw -= 45;
        } else {
            if (strafe > 0) moveYaw -= 90;
            else if (strafe < 0) moveYaw += 90;
        }
        return moveYaw;
    }

    private void updateMovement(float targetYaw) {
        float forward = originalForward;
        float strafe = originalStrafe;

        if (forward == 0 && strafe == 0) return;

        float yawDifference = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - targetYaw);
        double rad = Math.toRadians(yawDifference);

        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float fixedForward = forward * cos + strafe * sin;
        float fixedStrafe = strafe * cos - forward * sin;

        if (Math.abs(fixedForward) > 1.0f) fixedForward = fixedForward > 0 ? 1.0f : -1.0f;
        if (Math.abs(fixedStrafe) > 1.0f) fixedStrafe = fixedStrafe > 0 ? 1.0f : -1.0f;

        if (mc.thePlayer.isSneaking()) {
            fixedForward *= 0.3f;
            fixedStrafe *= 0.3f;
        }

        mc.thePlayer.movementInput.moveForward = fixedForward;
        mc.thePlayer.movementInput.moveStrafe = fixedStrafe;
    }

    private boolean isMoving() {
        return mc.thePlayer != null && (originalForward != 0 || originalStrafe != 0);
    }

    private boolean isLookingAtBlock(float yaw, float pitch, BlockPos targetPos) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 dir = RotationUtil.getVectorForRotation(pitch, yaw);
        Vec3 end = eyes.addVector(dir.xCoord * 4.5, dir.yCoord * 4.5, dir.zCoord * 4.5);
        MovingObjectPosition ray = mc.theWorld.rayTraceBlocks(eyes, end, false, false, true);

        // Poluzowany RayCast: pozwala na trafienie w "sąsiada" (sideHit), co pomaga przy lagu
        if (ray != null && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = ray.getBlockPos();
            if (hitPos.equals(targetPos)) return true;
            if (hitPos.offset(ray.sideHit).equals(targetPos)) return true;
        }
        return false;
    }

    private boolean placeBlock(BlockData data) {
        ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
        Vec3 hitVec = getVec3(data);
        return mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, data.pos, data.face, hitVec);
    }

    private float[] getRotations(BlockData data) {
        Vec3 hitVec = getVec3(data);
        return RotationUtil.getRotationsToVec(hitVec);
    }

    private Vec3 getVec3(BlockData data) {
        BlockPos pos = data.pos;
        EnumFacing face = data.face;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        x += face.getFrontOffsetX() * 0.5;
        y += face.getFrontOffsetY() * 0.5;
        z += face.getFrontOffsetZ() * 0.5;

        double jitter = 0.05;
        if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
            x += (ThreadLocalRandom.current().nextDouble() - 0.5) * jitter;
            z += (ThreadLocalRandom.current().nextDouble() - 0.5) * jitter;
        } else {
            y += (ThreadLocalRandom.current().nextDouble() - 0.5) * jitter;
        }
        return new Vec3(x, y, z);
    }

    private BlockData getBlockData(BlockPos pos) {
        if (isValidBlock(pos)) return null;

        EnumFacing[] facings = EnumFacing.values();
        for (EnumFacing facing : facings) {
            if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) continue;
            BlockPos neighbor = pos.offset(facing);
            if (isValidBlock(neighbor)) return new BlockData(neighbor, facing.getOpposite());
        }

        if (isValidBlock(pos.add(0, -1, 0))) return new BlockData(pos.add(0, -1, 0), EnumFacing.UP);
        if (isValidBlock(pos.add(0, 1, 0))) return new BlockData(pos.add(0, 1, 0), EnumFacing.DOWN);

        BlockPos[] diagonals = new BlockPos[]{pos.add(-1, 0, 0), pos.add(1, 0, 0), pos.add(0, 0, -1), pos.add(0, 0, 1)};
        for (BlockPos diagonal : diagonals) {
            if (isValidBlock(diagonal.down())) return new BlockData(diagonal.down(), EnumFacing.UP);
        }
        return null;
    }

    private boolean isValidBlock(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return !invalidBlocks.contains(block) && block.isFullCube();
    }

    private int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && !invalidBlocks.contains(((ItemBlock) stack.getItem()).getBlock())) {
                return i;
            }
        }
        return -1;
    }

    private static class BlockData {
        public final BlockPos pos;
        public final EnumFacing face;
        public BlockData(BlockPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
        }
    }
}