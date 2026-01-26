package doom.module.impl.combat.killaura.modes;

import doom.event.impl.EventUpdate;
import doom.module.impl.combat.Killaura;
import doom.module.impl.combat.killaura.AuraMode;
import doom.util.RotationUtil;
import doom.util.TimeHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SingleMode extends AuraMode {
    private final TimeHelper attackTimer = new TimeHelper();
    private final TimeHelper pointChangeTimer = new TimeHelper();
    private long nextAttackDelay = 0;

    // Pamięć ostatniej rotacji wysłanej do serwera
    private float lastServerYaw;
    private float lastServerPitch;

    public SingleMode(Killaura parent) {
        super("Single", parent);
    }

    @Override
    public void onUpdate(EventUpdate event) {
        if (!event.isPre()) return;

        List<EntityLivingBase> targets = parent.getTargets();
        targets.sort(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceSqToEntity(e)));

        if (targets.isEmpty()) {
            parent.target = null;
            // Płynny powrót głowy do celownika (Opcjonalnie - dla estetyki)
            float back = RotationUtil.interpolateAngle(mc.thePlayer.rotationYawHead, mc.thePlayer.rotationYaw, 0.1f);
            mc.thePlayer.rotationYawHead = back;
            mc.thePlayer.renderYawOffset = back;

            RotationUtil.reset();
            return;
        }

        EntityLivingBase target = targets.get(0);
        parent.target = target;

        if (parent.target != target) RotationUtil.randomizePoint();

        // 1. LOGIKA SERWEROWA (Bypass - Sztywno)
        float startYaw = RotationUtil.isRotating ? lastServerYaw : mc.thePlayer.rotationYaw;
        float startPitch = RotationUtil.isRotating ? lastServerPitch : mc.thePlayer.rotationPitch;

        float[] serverRotations = RotationUtil.getAiRotations(target, startYaw, startPitch, (float) parent.turnSpeed.getValue());
        float[] fixedRotations = RotationUtil.applyGCD(new float[]{startYaw, startPitch}, serverRotations);

        lastServerYaw = fixedRotations[0];
        lastServerPitch = fixedRotations[1];

        event.setYaw(fixedRotations[0]);
        event.setPitch(fixedRotations[1]);

        // 2. LOGIKA WIZUALNA (Smooth - Dla oczu)
        RotationUtil.shouldUseCustomPitch = true;
        RotationUtil.isRotating = true;

        float visualSpeed = 0.25f; // Płynność

        // YAW (Boki) - bezpośrednio w gracza
        float smoothYaw = RotationUtil.interpolateAngle(mc.thePlayer.rotationYawHead, fixedRotations[0], visualSpeed);
        mc.thePlayer.rotationYawHead = smoothYaw;
        mc.thePlayer.renderYawOffset = smoothYaw;

        // PITCH (Góra-Dół) - do RenderUtil dla RendererLivingEntity
        if (Math.abs(RotationUtil.renderPitch) < 0.1f) RotationUtil.renderPitch = mc.thePlayer.rotationPitch;
        RotationUtil.renderPitch = RotationUtil.interpolateAngle(RotationUtil.renderPitch, fixedRotations[1], visualSpeed);

        // 3. Movement Fix
        RotationUtil.setRotation(fixedRotations[0], fixedRotations[1]);

        // 4. Atak
        if (nextAttackDelay == 0) updateAttackDelay();

        if (attackTimer.hasReached(nextAttackDelay)) {
            // RayCast sprawdzamy zawsze
            boolean canHit = true;
            if (parent.rayCast.isEnabled()) {
                Entity rayEntity = RotationUtil.rayCast(fixedRotations[0], fixedRotations[1], parent.targetRange.getValue());
                if (rayEntity != target) {
                    canHit = false;
                    // Wyjątek na bliski dystans (hitboxy)
                    if (mc.thePlayer.getDistanceToEntity(target) < 0.8) canHit = true;
                }
            }

            // --- LOCK RANGE LOGIC ---
            // Bijemy TYLKO jeśli cel jest w zasięgu ataku (range), a nie tylko namierzania (targetRange)
            if (canHit && mc.thePlayer.getDistanceToEntity(target) <= parent.range.getValue()) {
                attack(target);
                attackTimer.reset();
                updateAttackDelay();
                RotationUtil.randomizePoint();
            }
        }
    }

    private void updateAttackDelay() {
        double cps = parent.cps.getValue();
        long baseDelay = (long) (1000.0 / cps);
        long randomness = ThreadLocalRandom.current().nextLong(-20, 20);
        nextAttackDelay = Math.max(50, baseDelay + randomness);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            lastServerYaw = mc.thePlayer.rotationYaw;
            lastServerPitch = mc.thePlayer.rotationPitch;
        }
        attackTimer.reset();
        pointChangeTimer.reset();
        RotationUtil.randomizePoint();
        updateAttackDelay();
    }

    @Override
    public void onDisable() {
        parent.target = null;
        RotationUtil.reset();
    }
}