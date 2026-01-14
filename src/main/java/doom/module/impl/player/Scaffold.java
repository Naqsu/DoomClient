package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
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
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Scaffold extends Module {

    // --- SETTINGS (Bez zmian) ---
    public ModeSetting rotationMode = new ModeSetting("Rotation", this, "VulcanSwitch", "VulcanSwitch", "Normal");
    public BooleanSetting sprint = new BooleanSetting("Sprint", this, true);
    public BooleanSetting tower = new BooleanSetting("Tower", this, true);
    public BooleanSetting towerMove = new BooleanSetting("Tower Move", this, true);
    public BooleanSetting jump = new BooleanSetting("Jump", this, false);
    public BooleanSetting swing = new BooleanSetting("Swing", this, true);
    public BooleanSetting silent = new BooleanSetting("Silent", this, true);
    public BooleanSetting rayCast = new BooleanSetting("RayCast", this, false);

    public BooleanSetting sneak = new BooleanSetting("Sneak", this, true);
    public NumberSetting sneakEvery = new NumberSetting("Sneak Every", this, 4, 1, 10, 1);

    public NumberSetting placeDelay = new NumberSetting("Delay", this, 0, 0, 5, 1);

    // --- DATA ---
    private BlockData targetBlock;
    private float[] currentRotations = new float[]{0, 0};
    private float lastYaw, lastPitch;
    private double keepY = -1;
    private final TimeHelper timer = new TimeHelper();

    private int blocksPlaced = 0;
    private boolean isSneaking = false;
    private boolean isJumpingUp = false;
    private int currentSlot = -1;

    // Cache dla wektora uderzenia, aby był spójny między rotacją a postawieniem
    private Vec3 currentHitVec = null;

    private float originalForward, originalStrafe;

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
        Client.INSTANCE.settingsManager.rSetting(rotationMode);
        Client.INSTANCE.settingsManager.rSetting(sprint);
        Client.INSTANCE.settingsManager.rSetting(tower);
        Client.INSTANCE.settingsManager.rSetting(towerMove);
        Client.INSTANCE.settingsManager.rSetting(jump);
        Client.INSTANCE.settingsManager.rSetting(swing);
        Client.INSTANCE.settingsManager.rSetting(silent);
        Client.INSTANCE.settingsManager.rSetting(rayCast);
        Client.INSTANCE.settingsManager.rSetting(sneak);
        Client.INSTANCE.settingsManager.rSetting(sneakEvery);
        Client.INSTANCE.settingsManager.rSetting(placeDelay);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        lastYaw = mc.thePlayer.rotationYaw;
        lastPitch = 80.0f;
        currentRotations[0] = lastYaw;
        currentRotations[1] = lastPitch;

        keepY = Math.floor(mc.thePlayer.posY - 1.0);
        blocksPlaced = 0;
        isSneaking = false;
        currentHitVec = null;
        timer.reset();
        currentSlot = mc.thePlayer.inventory.currentItem;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        TimerUtil.setTimerSpeed(1.0f);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);

        // Nie wyłączamy sprintu tutaj na siłę, bo gracz może chcieć biec dalej
        if (sprint.isEnabled() && !Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode())) {
            mc.thePlayer.setSprinting(false);
        }

        RotationUtil.reset();

        // Sync slotu przy wyłączaniu
        if (mc.thePlayer.inventory.currentItem != currentSlot && currentSlot != -1) {
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // --- INPUT UPDATE ---
        originalForward = mc.thePlayer.movementInput.moveForward;
        originalStrafe = mc.thePlayer.movementInput.moveStrafe;
        boolean moving = originalForward != 0 || originalStrafe != 0;

        // Warunek Towera
        boolean isTowering = tower.isEnabled() && mc.gameSettings.keyBindJump.isKeyDown() && (!moving || towerMove.isEnabled());

        // 1. Slot Logic
        int bestSlot = getBlockSlot();
        if (bestSlot == -1) return; // Brak bloków

        // Zmieniamy slot tylko jeśli to konieczne
        if (bestSlot != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.inventory.currentItem = bestSlot;
            currentSlot = bestSlot;
        }

        // KeepY Logic
        if (!jump.isEnabled() || mc.gameSettings.keyBindJump.isKeyDown() || isTowering) {
            keepY = Math.floor(mc.thePlayer.posY - 1.0);
        } else {
            // Jeśli tryb Jump jest włączony i nie trzymamy spacji, trzymaj Y
            if (mc.thePlayer.posY - 1.0 < keepY && mc.thePlayer.onGround) {
                keepY = Math.floor(mc.thePlayer.posY - 1.0);
            }
        }

        // 2. Find Block
        BlockPos searchPos = new BlockPos(mc.thePlayer.posX, keepY, mc.thePlayer.posZ);
        BlockData newTarget = getBlockData(searchPos);

        // Reset hitVec jeśli zmieniliśmy blok docelowy
        if (targetBlock == null || newTarget == null || !targetBlock.pos.equals(newTarget.pos) || targetBlock.face != newTarget.face) {
            currentHitVec = null;
        }

        if (newTarget != null) {
            targetBlock = newTarget;
            // Generujemy HitVec RAZ na blok, żeby rotacja i click były zgodne
            if (currentHitVec == null) {
                currentHitVec = getStrictVec3(targetBlock);
            }
        }

        // 3. Jump Logic (Detekcja skoku dla rotacji Switch)
        if (moving && !mc.gameSettings.keyBindJump.isKeyDown()) {
            if (mc.thePlayer.onGround && jump.isEnabled()) {
                mc.thePlayer.jump();
                isJumpingUp = true;
            } else if (mc.thePlayer.motionY > 0.1) {
                isJumpingUp = true;
            } else {
                isJumpingUp = false;
            }
        } else {
            isJumpingUp = false;
        }

        // 4. Rotations
        float[] rots = new float[]{lastYaw, lastPitch};

        if (rotationMode.is("VulcanSwitch")) {
            // Specyficzna logika dla Vulcana: patrzenie prosto podczas biegu i skoku
            if (isJumpingUp && moving && !isTowering) {
                float moveYaw = getMovementYaw(mc.thePlayer.rotationYaw, originalForward, originalStrafe);
                rots[0] = interpolate(lastYaw, moveYaw, 45.0f);
                rots[1] = 0.0f; // Patrz prosto
            } else {
                if (targetBlock != null && currentHitVec != null) {
                    float[] dest = RotationUtil.getRotationsToVec(currentHitVec);
                    rots = RotationUtil.applyGCD(new float[]{lastYaw, lastPitch}, dest);
                }
            }
        } else {
            // Normal mode
            if (targetBlock != null && currentHitVec != null) {
                float[] dest = RotationUtil.getRotationsToVec(currentHitVec);
                rots = RotationUtil.applyGCD(new float[]{lastYaw, lastPitch}, dest);
            }
        }

        lastYaw = rots[0];
        lastPitch = rots[1];
        currentRotations = rots;

        event.setYaw(currentRotations[0]);
        event.setPitch(currentRotations[1]);

        mc.thePlayer.rotationYawHead = currentRotations[0];
        mc.thePlayer.renderYawOffset = currentRotations[0];
        RotationUtil.renderPitch = currentRotations[1];
        RotationUtil.shouldUseCustomPitch = true;

        if (silent.isEnabled()) {
            updateMovement(currentRotations[0]);
        }

        // --- 5. TOWER LOGIC ---
        if (isTowering) {
            if (!moving) {
                // Stabilizacja środka bloku (płynniejsza niż w oryginale)
                double centerX = Math.floor(mc.thePlayer.posX) + 0.5;
                double centerZ = Math.floor(mc.thePlayer.posZ) + 0.5;
                if (mc.thePlayer.getDistance(centerX, mc.thePlayer.posY, centerZ) > 0.1) {
                    mc.thePlayer.motionX = (centerX - mc.thePlayer.posX) * 0.2;
                    mc.thePlayer.motionZ = (centerZ - mc.thePlayer.posZ) * 0.2;
                }
            }

            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.42f;
            }
            else if (mc.thePlayer.motionY > 0) {
                // Motion clip (Vulcan bypass)
                if (mc.thePlayer.motionY < 0.1) {
                    mc.thePlayer.motionY -= 0.015;
                }
            }
        }

        // 6. Placing
        boolean canPlace = isTowering || (!isJumpingUp || (!moving && mc.gameSettings.keyBindJump.isKeyDown()));

        // Dodatkowy check: nie stawiaj, jeśli rotacja Switch jeszcze nie wycelowała w dół (żeby nie stawiać w powietrze)
        if (rotationMode.is("VulcanSwitch") && isJumpingUp && lastPitch < 45) {
            canPlace = false;
        }

        if (canPlace && targetBlock != null && currentHitVec != null) {
            if (mc.thePlayer.getDistanceSq(targetBlock.pos) <= 25) {
                // Tower = brak delayu, normalnie = delay z ustawień
                long delay = isTowering ? 0 : (long)(placeDelay.getValue() * 50);

                if (timer.hasReached(delay)) {
                    // RayCast Check: Używamy tych samych rotacji co w evencie
                    if (rayCast.isEnabled()) {
                        if (!isLookingAtBlock(event.getYaw(), event.getPitch(), targetBlock.pos, targetBlock.face)) return;
                    }

                    // --- SPRINT FIX ---
                    // Vulcan/NCP wymaga wyłączenia sprintu pakietem przed interakcją
                    boolean wasSprinting = mc.thePlayer.isSprinting();
                    if (wasSprinting) {
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    }

                    if (placeBlock(targetBlock)) {
                        if (swing.isEnabled()) {
                            mc.thePlayer.swingItem();
                        } else {
                            mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                        }

                        timer.reset();

                        if (sneak.isEnabled()) handleSneak();

                        // Regeneracja HitVec po postawieniu (żeby kolejny click był w inne miejsce)
                        currentHitVec = getStrictVec3(targetBlock);
                    }

                    // Przywróć sprint
                    if (wasSprinting) {
                        mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    }
                }
            }
        }
    }

    private void handleSneak() {
        if (!sneak.isEnabled()) return;
        blocksPlaced++;

        if (blocksPlaced >= sneakEvery.getValue()) {
            if (!isSneaking) {
                isSneaking = true;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
            } else if (mc.thePlayer.onGround) {
                blocksPlaced = 0;
                isSneaking = false;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        } else {
            // Resetuj, jeśli gracz puścił shift manualnie
            if (!Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()) && isSneaking) {
                isSneaking = false;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        }
    }

    private float interpolate(float current, float target, float speed) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        if (diff > speed) diff = speed;
        if (diff < -speed) diff = -speed;
        return current + diff;
    }

    private float getMovementYaw(float yaw, float forward, float strafe) {
        if (forward == 0 && strafe == 0) return yaw;
        float moveYaw = yaw;
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

        float yaw = mc.thePlayer.rotationYaw;
        float fixedYaw = yaw - targetYaw;

        float rad = (float) Math.toRadians(fixedYaw);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        // Poprawiona matematyka wektora
        float fixedForward = forward * cos + strafe * sin;
        float fixedStrafe = strafe * cos - forward * sin;

        // Normalizacja, aby nie przekraczać wartości wejścia
        if (Math.abs(fixedForward) > 1.0f) fixedForward = fixedForward > 0 ? 1.0f : -1.0f;
        if (Math.abs(fixedStrafe) > 1.0f) fixedStrafe = fixedStrafe > 0 ? 1.0f : -1.0f;

        if (mc.gameSettings.keyBindSneak.isKeyDown() || isSneaking) {
            fixedForward *= 0.3f;
            fixedStrafe *= 0.3f;
        }

        mc.thePlayer.movementInput.moveForward = fixedForward;
        mc.thePlayer.movementInput.moveStrafe = fixedStrafe;
    }

    private boolean isLookingAtBlock(float yaw, float pitch, BlockPos targetPos, EnumFacing targetFace) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 dir = RotationUtil.getVectorForRotation(pitch, yaw);
        Vec3 end = eyes.addVector(dir.xCoord * 4.5, dir.yCoord * 4.5, dir.zCoord * 4.5);
        MovingObjectPosition ray = mc.theWorld.rayTraceBlocks(eyes, end, false, false, true);

        if (ray != null && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = ray.getBlockPos();
            // Zezwalamy na trafienie w blok docelowy LUB sąsiada (czasem raycast łapie krawędź)
            return hitPos.equals(targetPos);
        }
        return false;
    }

    private boolean placeBlock(BlockData data) {
        ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
        // Używamy zapamiętanego wektora, aby serwer widział kliknięcie tam, gdzie celowaliśmy
        Vec3 hitVec = (currentHitVec != null) ? currentHitVec : getStrictVec3(data);
        return mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack, data.pos, data.face, hitVec);
    }

    private Vec3 getStrictVec3(BlockData data) {
        BlockPos pos = data.pos;
        EnumFacing face = data.face;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        x += face.getFrontOffsetX() * 0.5;
        y += face.getFrontOffsetY() * 0.5;
        z += face.getFrontOffsetZ() * 0.5;

        double variance = 0.25; // Randomization range
        // Dodajemy losowość, aby ominąć wykrywanie "Clicking same pixel"
        if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
            x += (ThreadLocalRandom.current().nextDouble() - 0.5) * variance;
            z += (ThreadLocalRandom.current().nextDouble() - 0.5) * variance;
        } else {
            y += (ThreadLocalRandom.current().nextDouble() - 0.5) * variance;
        }
        return new Vec3(x, y, z);
    }

    private BlockData getBlockData(BlockPos pos) {
        if (isValidBlock(pos)) return null;

        EnumFacing[] facings = EnumFacing.values();

        // 1. Sprawdź bezpośrednich sąsiadów
        for (EnumFacing facing : facings) {
            if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) continue; // Optymalizacja: najpierw boki
            BlockPos neighbor = pos.offset(facing);
            if (isValidBlock(neighbor)) return new BlockData(neighbor, facing.getOpposite());
        }

        // 2. Sprawdź górę/dół
        if (isValidBlock(pos.add(0, -1, 0))) return new BlockData(pos.add(0, -1, 0), EnumFacing.UP);
        if (isValidBlock(pos.add(0, 1, 0))) return new BlockData(pos.add(0, 1, 0), EnumFacing.DOWN);

        // 3. Sprawdź przekątne (dla lepszego wykrywania podczas szybkiego biegu)
        BlockPos[] diagonals = new BlockPos[]{pos.add(-1, 0, 0), pos.add(1, 0, 0), pos.add(0, 0, -1), pos.add(0, 0, 1)};
        for (BlockPos diagonal : diagonals) {
            if (isValidBlock(diagonal.down())) return new BlockData(diagonal.down(), EnumFacing.UP);
        }
        return null;
    }

    private boolean isValidBlock(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return !invalidBlocks.contains(block) && block.isFullCube(); // Wymagamy pełnego bloku do postawienia na nim
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