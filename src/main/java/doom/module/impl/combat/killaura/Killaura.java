package doom.module.impl.combat;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.module.impl.combat.killaura.AuraMode;
import doom.module.impl.combat.killaura.modes.SingleMode;
import doom.module.impl.combat.killaura.modes.SwitchMode;
import doom.module.impl.player.Scaffold;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.FightUtil;
import doom.util.RotationUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class Killaura extends Module {

    // --- USTAWIENIA ---
    public ModeSetting mode = new ModeSetting("Mode", this, "Single", "Single", "Switch");

    // Attack Range - dystans, w którym aura BIJE
    public NumberSetting range = new NumberSetting("Range", this, 3.0, 3.0, 6.0, 0.1);

    // Target Range - dystans, w którym aura NAMIERZA (rotuje), ale jeszcze nie bije
    public NumberSetting targetRange = new NumberSetting("Target Range", this, 6.0, 3.0, 10.0, 0.1);

    public NumberSetting cps = new NumberSetting("CPS", this, 11.0, 1.0, 20.0, 1.0);
    public NumberSetting turnSpeed = new NumberSetting("Turn Speed", this, 180.0, 10.0, 180.0, 10.0);
    public NumberSetting fov = new NumberSetting("FOV", this, 360.0, 10.0, 360.0, 10.0);

    public BooleanSetting autoBlock = new BooleanSetting("AutoBlock", this, true);
    public ModeSetting autoBlockMode = new ModeSetting("Block Mode", this, "Fake", "Fake", "Packet", "Vanilla");

    public BooleanSetting keepSprint = new BooleanSetting("KeepSprint", this, true);
    public BooleanSetting rayCast = new BooleanSetting("RayCast", this, true);

    public BooleanSetting players = new BooleanSetting("Players", this, true);
    public BooleanSetting mobs = new BooleanSetting("Mobs", this, false);
    public BooleanSetting animals = new BooleanSetting("Animals", this, false);
    public BooleanSetting invisibles = new BooleanSetting("Invisibles", this, false);

    // --- STATE ---
    public EntityLivingBase target;
    private AuraMode currentMode;
    private final SingleMode singleMode;
    private final SwitchMode switchMode;

    public boolean isBlocking = false;

    public Killaura() {
        super("Killaura", Keyboard.KEY_R, Category.COMBAT);

        singleMode = new SingleMode(this);
        switchMode = new SwitchMode(this);
        currentMode = singleMode;

        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(range);
        Client.INSTANCE.settingsManager.rSetting(targetRange); // <--- NOWE
        Client.INSTANCE.settingsManager.rSetting(cps);
        Client.INSTANCE.settingsManager.rSetting(turnSpeed);
        Client.INSTANCE.settingsManager.rSetting(fov);

        Client.INSTANCE.settingsManager.rSetting(autoBlock);
        Client.INSTANCE.settingsManager.rSetting(autoBlockMode);
        autoBlockMode.setDependency(() -> autoBlock.isEnabled());

        Client.INSTANCE.settingsManager.rSetting(keepSprint);
        Client.INSTANCE.settingsManager.rSetting(rayCast);

        Client.INSTANCE.settingsManager.rSetting(players);
        Client.INSTANCE.settingsManager.rSetting(mobs);
        Client.INSTANCE.settingsManager.rSetting(animals);
        Client.INSTANCE.settingsManager.rSetting(invisibles);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;
        updateMode();
        currentMode.onEnable();
        isBlocking = false;
        target = null;
    }

    @Override
    public void onDisable() {
        currentMode.onDisable();
        if (isBlocking) stopBlocking();
        target = null;
        RotationUtil.reset();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        this.setSuffix(mode.getMode());

        if (Client.INSTANCE.moduleManager.getModule(Scaffold.class).isToggled()) {
            target = null;
            return;
        }

        updateMode();
        currentMode.onUpdate(event);

        // --- SMART SPRINT LOGIC (FIX SIMULATION) ---
        if (target != null && RotationUtil.isRotating) {

            // Movement Fix Logic
            float[] fixedInputs = doom.util.MoveUtil.getFixedInput(
                    mc.thePlayer.rotationYaw,
                    event.getYaw(),
                    mc.thePlayer.movementInput.moveForward,
                    mc.thePlayer.movementInput.moveStrafe
            );

            event.setMoveForward(fixedInputs[0]);
            event.setMoveStrafe(fixedInputs[1]);

            if (keepSprint.isEnabled()) {
                boolean canSprint = fixedInputs[0] > 0
                        && !mc.thePlayer.isSneaking()
                        && !mc.thePlayer.isCollidedHorizontally
                        && doom.util.MoveUtil.isMoving();

                event.setSprinting(canSprint);
            }
        }
        // -------------------------------------------

        if (autoBlock.isEnabled() && target != null && isHoldingSword()) {
            if (mc.thePlayer.getDistanceToEntity(target) <= range.getValue()) {
                startBlocking();
            } else {
                stopBlocking();
            }
        } else {
            stopBlocking();
        }
    }

    public List<EntityLivingBase> getTargets() {
        // Używamy targetRange do wyszukiwania
        List<EntityLivingBase> targets = new ArrayList<>(FightUtil.getMultipleTargets(
                targetRange.getValue(),
                players.isEnabled(),
                animals.isEnabled(),
                true,
                mobs.isEnabled(),
                invisibles.isEnabled()
        ));
        targets.removeIf(entity -> !isInFov(entity, fov.getValue()));
        return targets;
    }

    private boolean isInFov(EntityLivingBase entity, double fov) {
        if (fov >= 360.0) return true;
        float[] rotations = RotationUtil.getRotations(entity);
        if (rotations == null) return false;
        float yawDiff = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
        return Math.abs(yawDiff) <= fov / 2.0;
    }

    public boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private void updateMode() {
        switch (mode.getMode()) {
            case "Single":
                if (currentMode != singleMode) currentMode = singleMode;
                break;
            case "Switch":
                if (currentMode != switchMode) currentMode = switchMode;
                break;
        }
    }

    private void startBlocking() {
        if (isBlocking) return;
        switch (autoBlockMode.getMode()) {
            case "Vanilla":
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                break;
            case "Packet":
                mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                break;
            case "Fake":
                break;
        }
        isBlocking = true;
    }

    private void stopBlocking() {
        if (!isBlocking) return;
        switch (autoBlockMode.getMode()) {
            case "Vanilla":
            case "Packet":
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                break;
        }
        isBlocking = false;
    }
}