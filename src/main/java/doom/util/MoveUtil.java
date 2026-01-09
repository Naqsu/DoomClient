package doom.util;

import doom.event.impl.EventUpdate;
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

    public static float[] getFixedInput(float cameraYaw, float targetYaw, float forward, float strafe) {
        // Obliczamy różnicę kątów
        float yaw = MathHelper.wrapAngleTo180_float(targetYaw - cameraYaw);
        double angle = Math.toRadians(yaw);

        // Jeśli nie ruszamy się, zwróć 0
        if (Math.abs(forward) < 0.001 && Math.abs(strafe) < 0.001) return new float[]{0, 0};

        // Czy gracz się skrada? (Wtedy inputy są mnożone przez 0.3)
        boolean isSneaking = mc.thePlayer.isSneaking() || mc.gameSettings.keyBindSneak.isKeyDown();
        double sneakMultiplier = 0.3;

        // Matematyka wektorów (Standardowy MoveFix)
        double fixedForward = forward * Math.cos(angle) + strafe * Math.sin(angle);
        double fixedStrafe = strafe * Math.cos(angle) - forward * Math.sin(angle);

        // --- GRIM FIX: SNAP TO GRID ---
        // Grim wymaga liczb całkowitych (1.0, 0.0, -1.0) lub ich odpowiedników przy skradaniu.
        // Używamy niskiego progu (0.1), żeby nie ucinać skradania (które ma 0.3).

        float finalForward = 0;
        float finalStrafe = 0;

        // Zaokrąglanie Forward
        if (fixedForward > 0.1) finalForward = 1;
        else if (fixedForward < -0.1) finalForward = -1;

        // Zaokrąglanie Strafe
        if (fixedStrafe > 0.1) finalStrafe = 1;
        else if (fixedStrafe < -0.1) finalStrafe = -1;

        // Jeśli się skradamy, aplikujemy mnożnik PO zaokrągleniu
        if (isSneaking) {
            finalForward *= sneakMultiplier;
            finalStrafe *= sneakMultiplier;
        }

        return new float[]{finalForward, finalStrafe};
    }
    public static void setSpeed(double speed) {
        // Pobieramy instancję gracza i jego inputy
        if (mc.thePlayer == null) return;

        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;

        // Jeśli gracz się nie rusza (brak inputu), zatrzymujemy go
        if (forward == 0 && strafe == 0) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
        } else {
            // Obliczanie kąta przy poruszaniu się na ukos
            if (forward != 0) {
                if (strafe > 0) {
                    yaw += (forward > 0 ? -45 : 45);
                } else if (strafe < 0) {
                    yaw += (forward > 0 ? 45 : -45);
                }
                strafe = 0;
                // Normalizacja wartości forward
                if (forward > 0) {
                    forward = 1;
                } else if (forward < 0) {
                    forward = -1;
                }
            }

            // Matematyka obliczająca wektor ruchu na podstawie kąta Yaw
            double rad = Math.toRadians(yaw);
            double sin = Math.sin(rad);
            double cos = Math.cos(rad);

            mc.thePlayer.motionX = (forward * speed * -sin) + (strafe * speed * cos);
            mc.thePlayer.motionZ = (forward * speed * cos) - (strafe * speed * -sin);
        }
    }
    public static void fixMovement(EventUpdate event, float targetYaw) {
        float[] fixed = getFixedInput(mc.thePlayer.rotationYaw, targetYaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
        event.setMoveForward(fixed[0]);
        event.setMoveStrafe(fixed[1]);
    }


}