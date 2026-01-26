package doom.module.impl.combat.killaura.modes;

import doom.event.impl.EventUpdate;
import doom.module.impl.combat.Killaura;
import doom.module.impl.combat.killaura.AuraMode;
import doom.util.RotationUtil;
import doom.util.TimeHelper;
import net.minecraft.entity.EntityLivingBase;

import java.util.List;

public class SwitchMode extends AuraMode {
    private final TimeHelper switchTimer = new TimeHelper();
    private final TimeHelper attackTimer = new TimeHelper();
    private int targetIndex = 0;

    public SwitchMode(Killaura parent) {
        super("Switch", parent);
    }

    @Override
    public void onUpdate(EventUpdate event) {
        List<EntityLivingBase> targets = parent.getTargets();

        if (targets.isEmpty()) {
            parent.target = null;
            return;
        }

        if (switchTimer.hasReached(400)) {
            targetIndex++;
            if (targetIndex >= targets.size()) {
                targetIndex = 0;
            }
            switchTimer.reset();
        }

        if (targetIndex >= targets.size()) targetIndex = 0;

        EntityLivingBase target = targets.get(targetIndex);
        parent.target = target;

        // Rotacje
        float[] rotations = RotationUtil.getRotations(target);
        float sensitivity = (float) parent.turnSpeed.getValue();
        float yaw = RotationUtil.updateRotation(mc.thePlayer.rotationYaw, rotations[0], sensitivity);
        float pitch = RotationUtil.updateRotation(mc.thePlayer.rotationPitch, rotations[1], sensitivity);

        float[] fixedRotations = RotationUtil.applyGCD(new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, new float[]{yaw, pitch});

        // Ustawiamy w pakiecie
        event.setYaw(fixedRotations[0]);
        event.setPitch(fixedRotations[1]);

        // Sygnał dla Movement Fix i Renderowania
        RotationUtil.setRotation(fixedRotations[0], fixedRotations[1]);

        // --- USUNIĘTE ---
        // mc.thePlayer.rotationYawHead = fixedRotations[0];
        // mc.thePlayer.renderYawOffset = fixedRotations[0];

        // USUNIĘTO - To powodowało blokowanie kamery góra/dół
        // mc.thePlayer.rotationPitch = fixedRotations[1];

        // Atak
        double aps = parent.cps.getValue();
        long delay = (long) (1000.0 / aps);

        if (attackTimer.hasReached(delay)) {
            if (mc.thePlayer.getDistanceToEntity(target) <= parent.range.getValue()) {
                attack(target);
                attackTimer.reset();
            }
        }
    }

    @Override
    public void onEnable() {
        switchTimer.reset();
        attackTimer.reset();
        targetIndex = 0;
    }

    @Override
    public void onDisable() {
        parent.target = null;
    }
}