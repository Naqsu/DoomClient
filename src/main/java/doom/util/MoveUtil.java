package doom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public class MoveUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward != 0F || mc.thePlayer.movementInput.moveStrafe != 0F);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.2873D;
        if (mc.thePlayer != null && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            int amplifier = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0D + 0.2D * (double)(amplifier + 1);
        }
        return baseSpeed;
    }

    public static double getSpeed() {
        return Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
    }
    public static void strafe(double speed) {
        if (!isMoving()) return;

        float yaw = getDirection();
        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }
    public static void setSpeed(double speed) {
        if (mc.thePlayer == null) return;
        float yaw = mc.thePlayer.rotationYaw;
        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;

        if (forward == 0 && strafe == 0) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        } else {
            if (forward != 0) {
                if (strafe > 0) yaw += (forward > 0 ? -45 : 45);
                else if (strafe < 0) yaw += (forward > 0 ? 45 : -45);
                strafe = 0;
                if (forward > 0) forward = 1;
                else if (forward < 0) forward = -1;
            }
            double rad = Math.toRadians(yaw);
            mc.thePlayer.motionX = -Math.sin(rad) * speed;
            mc.thePlayer.motionZ = Math.cos(rad) * speed;
        }
    }

    public static float getDirection() {
        return getDirection(mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
    }

    public static float getDirection(float yaw, float forward, float strafe) {
        if (forward == 0.0F && strafe == 0.0F) return yaw;
        boolean reversed = forward < 0.0f;
        float strafeCorrection = 90f * (forward > 0.0f ? 0.5f : reversed ? -0.5f : 1.0f);
        if (reversed) yaw += 180.0f;
        if (strafe > 0.0f) yaw -= strafeCorrection;
        else if (strafe < 0.0f) yaw += strafeCorrection;
        return yaw;
    }

    /**
     * NAPRAWIONY MOVEMENT FIX
     * Oblicza wektory ruchu względem rotacji serwera.
     */
    public static float[] getFixedInput(float cameraYaw, float serverYaw, float forward, float strafe) {
        // Jeśli nie wciskasz klawiszy, zwróć 0
        if (forward == 0.0F && strafe == 0.0F) {
            return new float[]{0.0F, 0.0F};
        }

        // 1. Obliczamy kąt, w którym gracz chce iść (względem jego kamery)
        // np. wciśnięcie 'W' przy kamerze 0 stopni to 0 stopni.
        float moveYaw = getDirection(cameraYaw, forward, strafe);

        // 2. Obliczamy różnicę między tym gdzie chcesz iść, a gdzie patrzy serwer (Scaffold)
        float diff = serverYaw - moveYaw;

        // 3. Konwersja na radiany
        // Ważne: MathHelper.wrapAngleTo180_float zapobiega błędom przy przejściu przez 360 stopni
        float angle = (float) Math.toRadians(MathHelper.wrapAngleTo180_float(diff));

        // 4. Obliczamy nowe wartości Forward/Strafe
        // Używamy cos/sin różnicy kątów, aby obrócić wektor sterowania
        // Math.round jest opcjonalne, ale pomaga na Grima (usuwa mikro-ruchy typu 0.001)
        float fixedForward = MathHelper.cos(angle);
        float fixedStrafe = MathHelper.sin(angle);

        // Opcjonalne: Przycinanie do -1, 0, 1 dla czystego inputu (legit look)
        if (Math.abs(fixedForward) > 0.2f) fixedForward = fixedForward > 0 ? 1.0f : -1.0f; else fixedForward = 0.0f;
        if (Math.abs(fixedStrafe) > 0.2f) fixedStrafe = fixedStrafe > 0 ? 1.0f : -1.0f; else fixedStrafe = 0.0f;

        return new float[]{fixedForward, fixedStrafe};
    }
}