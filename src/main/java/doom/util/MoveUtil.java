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
        // Oblicza prędkość poziomą (XZ)
        return Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
    }
    public static void setSpeed(double speed) {
        if (mc.thePlayer == null) return;

        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;

        if (forward == 0 && strafe == 0) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        } else {
            if (forward != 0) {
                if (strafe > 0) {
                    yaw += (forward > 0 ? -45 : 45);
                } else if (strafe < 0) {
                    yaw += (forward > 0 ? 45 : -45);
                }
                strafe = 0;
                if (forward > 0) {
                    forward = 1;
                } else if (forward < 0) {
                    forward = -1;
                }
            }

            double rad = Math.toRadians(yaw);
            double sin = Math.sin(rad);
            double cos = Math.cos(rad);

            mc.thePlayer.motionX = (forward * speed * -sin) + (strafe * speed * cos);
            mc.thePlayer.motionZ = (forward * speed * cos) - (strafe * speed * -sin);
        }
    }

    // --- DIAGONAL FIX (Hypixel Math) ---
    public static double getDiagonalSpeed(double speed) {
        boolean isDiagonal = mc.thePlayer.movementInput.moveForward != 0 && mc.thePlayer.movementInput.moveStrafe != 0;
        if (isDiagonal) {
            return speed * 0.98;
        }
        return speed;
    }

    public static float getDirection() {
        float yaw = mc.thePlayer.rotationYaw;
        float forward = mc.thePlayer.moveForward;
        float strafe = mc.thePlayer.moveStrafing;

        if (forward == 0.0F && strafe == 0.0F) return yaw;

        boolean reversed = forward < 0.0f;
        float strafeCorrection = 90f * (forward > 0.0f ? 0.5f : reversed ? -0.5f : 1.0f);

        if (reversed) yaw += 180.0f;
        if (strafe > 0.0f) yaw -= strafeCorrection;
        else if (strafe < 0.0f) yaw += strafeCorrection;

        return yaw;
    }

    // --- PRZYWRÓCONA METODA (FIX BŁĘDU KOMPILACJI) ---
    // Oblicza inputy (W/S/A/D) względem docelowej rotacji (np. Killaury)
    public static float[] getFixedInput(float cameraYaw, float targetYaw, float forward, float strafe) {
        // Obliczamy różnicę kątów
        float yaw = MathHelper.wrapAngleTo180_float(targetYaw - cameraYaw);
        double angle = Math.toRadians(yaw);

        // Jeśli nie ruszamy się, zwróć 0
        if (Math.abs(forward) < 0.001 && Math.abs(strafe) < 0.001) return new float[]{0, 0};

        // Czy gracz się skrada?
        boolean isSneaking = mc.thePlayer.isSneaking() || mc.gameSettings.keyBindSneak.isKeyDown();
        double sneakMultiplier = 0.3;

        // Matematyka wektorów
        double fixedForward = forward * Math.cos(angle) + strafe * Math.sin(angle);
        double fixedStrafe = strafe * Math.cos(angle) - forward * Math.sin(angle);

        // Grim/Vulcan Fix: Zaokrąglanie do całkowitych (Snap to Grid)
        float finalForward = 0;
        float finalStrafe = 0;

        if (fixedForward > 0.1) finalForward = 1;
        else if (fixedForward < -0.1) finalForward = -1;

        if (fixedStrafe > 0.1) finalStrafe = 1;
        else if (fixedStrafe < -0.1) finalStrafe = -1;

        // Jeśli się skradamy, aplikujemy mnożnik
        if (isSneaking) {
            finalForward *= sneakMultiplier;
            finalStrafe *= sneakMultiplier;
        }

        return new float[]{finalForward, finalStrafe};
    }
}