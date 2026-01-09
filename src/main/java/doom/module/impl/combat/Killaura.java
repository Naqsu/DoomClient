package doom.module.impl.combat;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPostUpdate;
import doom.event.impl.EventUpdate;
import doom.module.Category;
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
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Killaura extends Module {

    // --- USTAWIENIA ---
    public NumberSetting range = new NumberSetting("Range", this, 3.0, 3.0, 6.0, 0.1);
    public NumberSetting minAps = new NumberSetting("Min APS", this, 8.0, 1.0, 20.0, 0.5);
    public NumberSetting maxAps = new NumberSetting("Max APS", this, 12.0, 1.0, 20.0, 0.5);
    public NumberSetting smoothness = new NumberSetting("Smoothness", this, 6.0, 1.0, 20.0, 1.0);

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

    public static boolean isToggled = false;
    public EntityLivingBase target;
    private boolean wasBlocking = false;

    private final TimeHelper attackTimer = new TimeHelper();
    private final Random random = new Random();

    // Przechowujemy ostatnie rotacje wysłane do serwera
    private float lastYaw, lastPitch;
    private long nextAttackDelay = 0;

    // Zmienne do płynnego celowania
    private float driftX = 0, driftY = 0, driftZ = 0;
    private float driftTimer = 0;

    public Killaura() {
        super("Killaura", Keyboard.KEY_R, Category.COMBAT);
        Client.INSTANCE.settingsManager.rSetting(range);
        Client.INSTANCE.settingsManager.rSetting(minAps);
        Client.INSTANCE.settingsManager.rSetting(maxAps);
        Client.INSTANCE.settingsManager.rSetting(smoothness);
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
        isToggled = true;
        target = null;
        wasBlocking = false;

        // Inicjalizujemy rotacje na aktualną pozycję kamery
        if (mc.thePlayer != null) {
            lastYaw = mc.thePlayer.rotationYaw;
            lastPitch = mc.thePlayer.rotationPitch;
        }

        driftTimer = 0;
        generateNextDelay();
    }

    @Override
    public void onDisable() {
        isToggled = false;
        target = null;
        unblock();
        RotationUtil.reset();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // Blokada przy Scaffoldzie
        if (Client.INSTANCE.moduleManager.getModule(Scaffold.class).isToggled()) {
            target = null;
            RotationUtil.isRotating = false;
            return;
        }

        target = getTarget();

        // Auto Unblock
        if (wasBlocking) {
            unblock();
        }

        // --- LOGIKA ROTACJI (TARGETING + POWRÓT) ---

        float destYaw, destPitch;
        boolean isAimingAtTarget;

        if (target != null) {
            isAimingAtTarget = true;

            // 1. Drifting (pływanie celownika)
            driftTimer += 0.05f;
            driftX = (float) (Math.sin(driftTimer) * 0.25);
            driftY = (float) (Math.cos(driftTimer * 0.5) * 0.25);
            driftZ = (float) (Math.sin(driftTimer * 1.5) * 0.25);

            // 2. Obliczanie kątów do celu
            float[] rotationsToTarget = RotationUtil.getRotationsToEntityWithOffset(target, driftX, driftY, driftZ);
            destYaw = rotationsToTarget[0];
            destPitch = rotationsToTarget[1];

        } else {
            isAimingAtTarget = false;

            // Jeśli nie ma celu, celujemy tam gdzie gracz patrzy myszką (powrót do kamery)
            destYaw = mc.thePlayer.rotationYaw;
            destPitch = mc.thePlayer.rotationPitch;
        }

        // 3. Wygładzanie (Smoothness)
        // Jeśli wracamy do kamery (brak celu), używamy nieco szybszego smootha (0.7x), żeby nie zamulało
        float smoothVal = (float) Math.max(1.0, smoothness.getValue());
        if (!isAimingAtTarget) smoothVal *= 0.7f;

        float yawDiff = MathHelper.wrapAngleTo180_float(destYaw - lastYaw);
        float pitchDiff = destPitch - lastPitch;

        // 4. Sprawdzenie końca powrotu
        // Jeśli nie mamy celu i różnica kątów jest minimalna (< 5 stopni),
        // uznajemy, że celownik wrócił na miejsce i oddajemy kontrolę graczowi.
        if (!isAimingAtTarget && Math.abs(yawDiff) < 5.0f && Math.abs(pitchDiff) < 5.0f) {
            lastYaw = mc.thePlayer.rotationYaw;
            lastPitch = mc.thePlayer.rotationPitch;
            RotationUtil.isRotating = false; // Wyłącz MoveFix
            return; // Kończymy działanie aury w tym ticku
        }

        // 5. Aplikowanie Smootha (Ease-Out)
        float newYaw = lastYaw + (yawDiff / smoothVal);
        float newPitch = lastPitch + (pitchDiff / smoothVal);

        // 6. Micro-Jitter & GCD Fix
        newYaw += (random.nextFloat() - 0.5f) * 0.02f;
        newPitch += (random.nextFloat() - 0.5f) * 0.02f;

        float[] fixedRots = RotationUtil.applyGCD(new float[]{lastYaw, lastPitch}, new float[]{newYaw, newPitch});
        lastYaw = fixedRots[0];
        lastPitch = fixedRots[1];

        // 7. Ustawienie Eventu
        event.setYaw(lastYaw);
        event.setPitch(lastPitch);

        // Ustaw MoveFix na aktualną rotację aury (ważne przy powrocie, żeby nie rzucało kamerą)
        RotationUtil.targetYaw = lastYaw;
        RotationUtil.isRotating = true;

        // --- ATAKOWANIE (Tylko jeśli mamy cel) ---
        if (isAimingAtTarget) {
            if (attackTimer.hasReached(nextAttackDelay)) {

                boolean canHit = true;
                if (rayCast.isEnabled()) {
                    Entity hit = RotationUtil.rayCast(lastYaw, lastPitch, range.getValue() + 0.1);
                    if (hit == null || hit.getEntityId() != target.getEntityId()) {
                        if (mc.thePlayer.getDistanceToEntity(target) > 1.0) {
                            canHit = false;
                        }
                    }
                }

                if (canHit) {
                    if (!keepSprint.isEnabled() || Math.abs(yawDiff) > 15.0f) {
                        mc.thePlayer.setSprinting(false);
                    }

                    mc.thePlayer.swingItem();
                    mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));

                    attackTimer.reset();
                    generateNextDelay();
                }
            }
        }
    }

    @EventTarget
    public void onPostUpdate(EventPostUpdate event) {
        if (target != null && autoBlock.isEnabled() && isHoldingSword()) {
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            wasBlocking = true;
        }
    }

    private void unblock() {
        if (wasBlocking && isHoldingSword()) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
            ));
            wasBlocking = false;
        }
    }

    private void generateNextDelay() {
        double min = minAps.getValue();
        double max = maxAps.getValue();
        double cps = min + (ThreadLocalRandom.current().nextGaussian() * 0.5 + 0.5) * (max - min);
        cps = MathHelper.clamp_double(cps, min, max);

        long delay = (long) (1000.0 / cps);

        if (System.currentTimeMillis() % 3 == 0) {
            delay -= ThreadLocalRandom.current().nextInt(15);
        } else {
            delay += ThreadLocalRandom.current().nextInt(15);
        }

        if (ThreadLocalRandom.current().nextInt(12) == 0) {
            delay += 40 + ThreadLocalRandom.current().nextInt(40);
        }

        nextAttackDelay = Math.max(50, delay);
    }

    public boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private EntityLivingBase getTarget() {
        List<EntityLivingBase> targets = mc.theWorld.loadedEntityList.stream()
                .filter(e -> e instanceof EntityLivingBase)
                .map(e -> (EntityLivingBase) e)
                .filter(this::isValidEntity)
                .sorted(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceSqToEntity(e)))
                .collect(Collectors.toList());

        if (targets.isEmpty()) return null;
        return targets.get(0);
    }

    private boolean isValidEntity(EntityLivingBase entity) {
        if (entity == mc.thePlayer) return false;
        if (entity.isDead || entity.getHealth() <= 0) return false;
        if (mc.thePlayer.getDistanceToEntity(entity) > range.getValue()) return false;

        if (crackedMode.isEnabled()) {
            if (entity instanceof EntityPlayer) return true;
            if (entity instanceof EntityMob) return targetsMobs.isEnabled();
            if (entity instanceof EntityAnimal) return targetsAnimals.isEnabled();
            return false;
        }

        if (entity.isInvisible() && !targetsInvisibles.isEnabled()) return false;
        if (ignoreTeams.isEnabled() && isOnSameTeam(entity)) return false;

        if (entity instanceof EntityPlayer) {
            return targetsPlayers.isEnabled();
        }
        if (entity instanceof EntityMob) return targetsMobs.isEnabled();
        if (entity instanceof EntityAnimal) return targetsAnimals.isEnabled();
        return false;
    }

    private boolean isOnSameTeam(EntityLivingBase entity) {
        if (entity.getDisplayName() == null || mc.thePlayer.getDisplayName() == null) return false;
        String targetName = entity.getDisplayName().getFormattedText().replace("§r", "");
        String myName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
        if (targetName.length() < 2 || myName.length() < 2) return false;
        return targetName.startsWith("§") && myName.startsWith("§") && targetName.charAt(1) == myName.charAt(1);
    }
}