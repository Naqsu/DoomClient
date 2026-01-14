package doom.module.impl.combat;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPostUpdate;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.module.impl.player.Scaffold;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.RotationUtil;
import doom.util.TimeHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Killaura extends Module {

    // --- USTAWIENIA ---
    public NumberSetting range = new NumberSetting("Range", this, 3.0, 3.0, 6.0, 0.1);
    public NumberSetting scanRange = new NumberSetting("Scan Range", this, 4.0, 3.0, 8.0, 0.1);

    public NumberSetting minAps = new NumberSetting("Min APS", this, 9.0, 1.0, 20.0, 0.5); // Zmniejszone dla bezpieczeństwa
    public NumberSetting maxAps = new NumberSetting("Max APS", this, 13.0, 1.0, 20.0, 0.5);

    // Vulcan lubi mniejszy Turn Speed (np. 60-90)
    public NumberSetting turnSpeed = new NumberSetting("Turn Speed", this, 80.0, 10.0, 180.0, 5.0);
    public NumberSetting fov = new NumberSetting("FOV", this, 360.0, 30.0, 360.0, 10.0);

    public BooleanSetting keepSprint = new BooleanSetting("KeepSprint", this, true);
    public BooleanSetting autoBlock = new BooleanSetting("AutoBlock", this, true);
    public BooleanSetting rayCast = new BooleanSetting("RayCast", this, true);

    // --- TARGETING ---
    public BooleanSetting targetsPlayers = new BooleanSetting("Players", this, true);
    public BooleanSetting targetsMobs = new BooleanSetting("Mobs", this, false);
    public BooleanSetting targetsAnimals = new BooleanSetting("Animals", this, false);
    public BooleanSetting targetsInvisibles = new BooleanSetting("Invisibles", this, false);
    public BooleanSetting ignoreTeams = new BooleanSetting("Ignore Teams", this, true);
    public BooleanSetting crackedMode = new BooleanSetting("Cracked/NoRules", this, false);

    public EntityLivingBase target;
    private boolean wasBlocking = false;

    private final TimeHelper attackTimer = new TimeHelper();
    private final Random random = new Random();

    private float serverYaw, serverPitch;
    private long nextAttackDelay = 0;

    // Usunąłem skomplikowany offset jitter, bo on flaguje Acceleration na Vulcanie
    private double randomOffsetZ = 0, randomOffsetY = 0, randomOffsetX = 0;

    public Killaura() {
        super("Killaura", Keyboard.KEY_R, Category.COMBAT);
        Client.INSTANCE.settingsManager.rSetting(range);
        Client.INSTANCE.settingsManager.rSetting(scanRange);
        Client.INSTANCE.settingsManager.rSetting(minAps);
        Client.INSTANCE.settingsManager.rSetting(maxAps);
        Client.INSTANCE.settingsManager.rSetting(turnSpeed);
        Client.INSTANCE.settingsManager.rSetting(fov);
        Client.INSTANCE.settingsManager.rSetting(keepSprint);
        Client.INSTANCE.settingsManager.rSetting(autoBlock);
        Client.INSTANCE.settingsManager.rSetting(rayCast);
        Client.INSTANCE.settingsManager.rSetting(targetsPlayers);
        Client.INSTANCE.settingsManager.rSetting(targetsMobs);
        Client.INSTANCE.settingsManager.rSetting(targetsAnimals);
        Client.INSTANCE.settingsManager.rSetting(targetsInvisibles);
        Client.INSTANCE.settingsManager.rSetting(ignoreTeams);
        Client.INSTANCE.settingsManager.rSetting(crackedMode);
    }

    @Override
    public void onEnable() {
        target = null;
        wasBlocking = false;
        if (mc.thePlayer != null) {
            serverYaw = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
        }
        randomizeAttackDelay();
    }

    @Override
    public void onDisable() {
        target = null;
        if (wasBlocking) unblock();

        if (mc.thePlayer != null) {
            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - serverYaw));
            float pitchDiff = Math.abs(mc.thePlayer.rotationPitch - serverPitch);

            // Zwiększyłem tolerancję do 45 stopni dla bezpieczeństwa na Vulcanie
            if (yawDiff > 45.0f || pitchDiff > 45.0f) {
                mc.thePlayer.rotationYaw = serverYaw;
                mc.thePlayer.rotationPitch = serverPitch;
            }
        }
        RotationUtil.reset();
        RotationUtil.isRotating = false;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (Client.INSTANCE.moduleManager.getModule(Scaffold.class).isToggled()) {
            target = null;
            RotationUtil.isRotating = false;
            return;
        }

        target = getTarget(scanRange.getValue());

        // AUTOBLOCK LOGIC
        if (wasBlocking && target == null) {
            unblock();
        }

        if (target != null) {
            // --- FIX 1: GŁADKIE ROTACJE (VULCAN ACCELERATION FIX) ---
            // Zamiast losować pozycję co chwilę, celujemy w środek/lekko wyżej
            // Vulcan nienawidzi nagłych zmian celu
            float[] rotations = RotationUtil.getRotationsToEntityWithOffset(target, 0.0, -0.1, 0.0);
            float targetYaw = rotations[0];
            float targetPitch = rotations[1];

            // Stała prędkość obrotu zamiast losowej (randomizacja powoduje flagi Acceleration)
            float turn = (float) turnSpeed.getValue();

            float fixedYaw = RotationUtil.updateRotation(serverYaw, targetYaw, turn);
            float fixedPitch = RotationUtil.updateRotation(serverPitch, targetPitch, turn);

            // GCD Fix
            float[] gcdFixed = RotationUtil.applyGCD(new float[]{serverYaw, serverPitch}, new float[]{fixedYaw, fixedPitch});
            serverYaw = gcdFixed[0];
            serverPitch = gcdFixed[1];

            event.setYaw(serverYaw);
            event.setPitch(serverPitch);

            mc.thePlayer.rotationYawHead = serverYaw;
            mc.thePlayer.renderYawOffset = serverYaw;
            RotationUtil.renderPitch = serverPitch;
            RotationUtil.shouldUseCustomPitch = true;

            RotationUtil.targetYaw = serverYaw;
            RotationUtil.isRotating = true;

            // --- FIX 2: SPRINT ANGLE FIX ---
            // Jeśli patrzysz w inną stronę niż aura, wyłączamy sprint, żeby nie dostać bana
            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - serverYaw));
            if (yawDiff > 50 && keepSprint.isEnabled()) {
                mc.thePlayer.setSprinting(false);
                mc.gameSettings.keyBindSprint.pressed = false;
            }

            // --- ATAK ---
            double distance = mc.thePlayer.getDistanceToEntity(target);
            if (distance <= range.getValue()) {
                // Raycast sprawdzający czy faktycznie patrzymy na entity
                Entity rayCastEntity = RotationUtil.rayCast(serverYaw, serverPitch, range.getValue());

                // Jeśli raycast nie trafił, ale jesteśmy bardzo blisko, to i tak bijemy (hitbox fix)
                boolean forceHit = distance < 1.0;
                boolean canHit = !rayCast.isEnabled() || forceHit || (rayCastEntity != null && rayCastEntity.getEntityId() == target.getEntityId());

                if (canHit && attackTimer.hasReached(nextAttackDelay)) {
                    attack(target);
                    randomizeAttackDelay();
                    attackTimer.reset();
                }
            }

        } else {
            // --- SMOOTH RETURN (Gdy brak celu) ---
            float yawDiff = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - serverYaw);
            float pitchDiff = mc.thePlayer.rotationPitch - serverPitch;

            if (Math.abs(yawDiff) > 10.0 || Math.abs(pitchDiff) > 10.0) {
                // Wolniejszy powrót, żeby nie flagować
                float turn = (float) turnSpeed.getValue() / 2.0f;

                float fixedYaw = RotationUtil.updateRotation(serverYaw, mc.thePlayer.rotationYaw, turn);
                float fixedPitch = RotationUtil.updateRotation(serverPitch, mc.thePlayer.rotationPitch, turn);

                float[] gcdFixed = RotationUtil.applyGCD(new float[]{serverYaw, serverPitch}, new float[]{fixedYaw, fixedPitch});
                serverYaw = gcdFixed[0];
                serverPitch = gcdFixed[1];

                event.setYaw(serverYaw);
                event.setPitch(serverPitch);
                mc.thePlayer.rotationYawHead = serverYaw;
                mc.thePlayer.renderYawOffset = serverYaw;
                RotationUtil.renderPitch = serverPitch;
                RotationUtil.targetYaw = serverYaw;
                RotationUtil.isRotating = true;
            } else {
                serverYaw = mc.thePlayer.rotationYaw;
                serverPitch = mc.thePlayer.rotationPitch;
                RotationUtil.isRotating = false;
            }
        }
    }

    private void randomizeAttackDelay() {
        // --- FIX 3: CONSISTENCY FIX ---
        // Bardziej naturalny rozkład randomizacji
        double min = minAps.getValue();
        double max = maxAps.getValue();

        // Zabezpieczenie przed błędnymi ustawieniami
        if (min > max) min = max;

        double cps = min + (random.nextDouble() * (max - min));
        long delay = (long) (1000.0 / cps);

        // Mniejszy jitter, Vulcan nie lubi dużych skoków
        if (random.nextBoolean()) {
            delay += random.nextInt(20);
        } else {
            delay -= random.nextInt(10);
        }

        nextAttackDelay = Math.max(50, delay);
    }

    private void attack(EntityLivingBase entity) {
        boolean blocking = isHoldingSword() && autoBlock.isEnabled();

        // Vulcan Unblock Fix: C07 musi iść przed atakiem
        if (blocking) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }

        // Jeśli kąt jest duży, upewniamy się że nie sprintujemy w momencie uderzenia
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - serverYaw));
        if (!keepSprint.isEnabled() || yawDiff > 50) {
            mc.thePlayer.setSprinting(false);
        }

        mc.thePlayer.swingItem();
        mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK));

        if (blocking) {
            // Interact packet jest wymagany na niektórych serwerach do animacji bloku
            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.INTERACT));
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            wasBlocking = true;
        }
    }

    private void unblock() {
        if (wasBlocking) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            wasBlocking = false;
        }
    }

    public boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private EntityLivingBase getTarget(double rangeCheck) {
        List<EntityLivingBase> targets = mc.theWorld.loadedEntityList.stream()
                .filter(e -> e instanceof EntityLivingBase)
                .map(e -> (EntityLivingBase) e)
                .filter(e -> isValidEntity(e, rangeCheck))
                .sorted(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceSqToEntity(e)))
                .collect(Collectors.toList());

        if (targets.isEmpty()) return null;
        return targets.get(0);
    }

    private boolean isValidEntity(EntityLivingBase entity, double rangeCheck) {
        if (entity == mc.thePlayer || entity.isDead || entity.getHealth() <= 0) return false;
        if (mc.thePlayer.getDistanceToEntity(entity) > rangeCheck) return false;
        if (!isInFOV(entity, fov.getValue())) return false;

        if (crackedMode.isEnabled()) {
            if (entity instanceof EntityPlayer) return true;
        }

        if (entity.isInvisible() && !targetsInvisibles.isEnabled()) return false;
        if (ignoreTeams.isEnabled() && isOnSameTeam(entity)) return false;

        if (entity instanceof EntityPlayer) return targetsPlayers.isEnabled();
        if (entity instanceof EntityMob) return targetsMobs.isEnabled();
        if (entity instanceof EntityAnimal) return targetsAnimals.isEnabled();
        return false;
    }

    private boolean isInFOV(EntityLivingBase entity, double angle) {
        angle *= 0.5;
        double angleDiff = getAngleDifference(mc.thePlayer.rotationYaw, RotationUtil.getRotationsToEntityWithOffset(entity, 0, 0, 0)[0]);
        return angleDiff > 0 && angleDiff < angle || angleDiff < 0 && angleDiff > -angle;
    }

    private float getAngleDifference(float a, float b) {
        return ((a - b) % 360.0F + 540.0F) % 360.0F - 180.0F;
    }

    private boolean isOnSameTeam(EntityLivingBase entity) {
        if (crackedMode.isEnabled()) return false;
        if (entity.getDisplayName() == null || mc.thePlayer.getDisplayName() == null) return false;
        String t = entity.getDisplayName().getFormattedText().replace("§r", "");
        String m = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
        return t.length() > 1 && m.length() > 1 && t.startsWith("§") && m.startsWith("§") && t.charAt(1) == m.charAt(1);
    }

    @EventTarget
    public void onPostUpdate(EventPostUpdate event) { }
}