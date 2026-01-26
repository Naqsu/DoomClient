package doom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RotationUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // --- STATE FIELDS ---
    public static boolean isRotating = false;
    public static boolean rotationRequested = false;
    public static boolean shouldUseCustomPitch = false;

    public static float targetYaw;
    public static float renderPitch;

    // --- ZARZĄDZANIE STANEM ---

    public static void setRotation(float yaw, float pitch) {
        targetYaw = yaw;
        renderPitch = pitch;
        isRotating = true;
        rotationRequested = true;
        shouldUseCustomPitch = true;
    }

    public static void reset() {
        isRotating = false;
        rotationRequested = false;
        shouldUseCustomPitch = false;
        if (mc.thePlayer != null) {
            targetYaw = mc.thePlayer.rotationYaw;
            renderPitch = mc.thePlayer.rotationPitch;
        }
    }

    // =================================================================================
    //                                  SCAFFOLD ROTATIONS
    // =================================================================================

    /**
     * ZAAWANSOWANE ROTACJE NA GRIMA
     * Oblicza losowy punkt na powierzchni bloku (zamiast środka), co omija
     * wykrywanie "Center Click" i "Bad Rotations".
     */
    public static float[] getGrimRotations(BlockPos pos, EnumFacing face) {
        // Losujemy punkt na ścianie bloku (zakres 0.1 - 0.9, żeby nie trafić w krawędź)
        double randomXY = ThreadLocalRandom.current().nextDouble(0.3, 0.7);
        double randomZ = ThreadLocalRandom.current().nextDouble(0.3, 0.7);

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // Przesuwamy punkt na odpowiednią ścianę
        x += face.getFrontOffsetX() * 0.5;
        y += face.getFrontOffsetY() * 0.5;
        z += face.getFrontOffsetZ() * 0.5;

        // Dodajemy losowość na płaszczyźnie ściany
        if (face == EnumFacing.UP || face == EnumFacing.DOWN) {
            x += (randomXY - 0.5);
            z += (randomZ - 0.5);
        } else if (face == EnumFacing.NORTH || face == EnumFacing.SOUTH) {
            x += (randomXY - 0.5);
            y += (randomZ - 0.5);
        } else { // WEST, EAST
            z += (randomXY - 0.5);
            y += (randomZ - 0.5);
        }

        return getRotationsToVec(new Vec3(x, y, z));
    }
    public static EnumFacing getClosestFacing(float yaw) {
        yaw = MathHelper.wrapAngleTo180_float(yaw) + 180; // Adjust to point backwards
        if (yaw >= 45 && yaw < 135) {
            return EnumFacing.EAST;
        } else if (yaw >= 135 && yaw < 225) {
            return EnumFacing.SOUTH;
        } else if (yaw >= 225 && yaw < 315) {
            return EnumFacing.WEST;
        } else {
            return EnumFacing.NORTH;
        }
    }
    /**
     * SNAP ROTATION - Naprawia "Zdublowany Most" / Skręcanie
     * Zwraca idealną rotację kardynalną (0, 90, 180, 270) lub połówkową (45, 135...)
     * Dzięki temu MovementFix nie dodaje dziwnych inputów.
     */
    public static float getSnapYaw(float currentYaw) {
        float rotation = currentYaw;
        // Zaokrąglamy do najbliższych 45 stopni
        // 0 = S, 90 = W, 180 = N, -90 = E
        return Math.round(rotation / 45f) * 45f;
    }
    public static float interpolateAngle(float current, float target, float speed) {
        float f = MathHelper.wrapAngleTo180_float(target - current);

        // Zamiast stałej prędkości, używamy procenta dystansu (np. 0.2).
        // To daje efekt "Ease-Out" - szybko na początku, wolno na końcu.
        float delta = f * speed;

        return current + delta;
    }
    // Płynna interpolacja kąta (tylko wizualna)
    public static float smoothAngle(float current, float target, float speed) {
        float f = MathHelper.wrapAngleTo180_float(target - current);

        // Ograniczamy prędkość obrotu wizualnego
        if (f > speed) f = speed;
        if (f < -speed) f = -speed;

        return current + f;
    }
    // =================================================================================
    //                                  HELPERY
    // =================================================================================

    public static float[] getRotationsToVec(Vec3 vec) {
        double x = vec.xCoord - mc.thePlayer.posX;
        double y = vec.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = vec.zCoord - mc.thePlayer.posZ;

        double dist = MathHelper.sqrt_double(x * x + z * z);

        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(y, dist) * 180.0D / Math.PI);

        return new float[]{yaw, pitch};
    }

    /**
     * GCD FIX (Greatest Common Divisor)
     * Symuluje kroki sensora myszki. Niezbędne na każdy nowoczesny antycheat (Grim, Watchdog).
     */
    public static float[] applyGCD(float[] currentRotations, float[] targetRotations) {
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = sensitivity * sensitivity * sensitivity * 1.2F; // Wzór z kodu gry

        // Obliczamy różnicę
        float yawDiff = targetRotations[0] - currentRotations[0];
        float pitchDiff = targetRotations[1] - currentRotations[1];

        // Zaokrąglamy różnicę do wielokrotności GCD (siatki pikseli myszy)
        float fixedYawDiff = Math.round(yawDiff / gcd) * gcd;
        float fixedPitchDiff = Math.round(pitchDiff / gcd) * gcd;

        return new float[]{
                currentRotations[0] + fixedYawDiff,
                MathHelper.clamp_float(currentRotations[1] + fixedPitchDiff, -90.0F, 90.0F)
        };
    }

    /**
     * Wygładzanie (Smooth)
     */
    public static float[] smooth(float[] current, float[] target, float speed) {
        float yawDiff = MathHelper.wrapAngleTo180_float(target[0] - current[0]);
        float pitchDiff = MathHelper.wrapAngleTo180_float(target[1] - current[1]);

        if (yawDiff > speed) yawDiff = speed;
        if (yawDiff < -speed) yawDiff = -speed;

        if (pitchDiff > speed) pitchDiff = speed;
        if (pitchDiff < -speed) pitchDiff = -speed;

        return new float[]{
                current[0] + yawDiff,
                current[1] + pitchDiff
        };
    }

    // --- RAYCAST HELPERY ---

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static Entity rayCast(float yaw, float pitch, double range) {
        if (mc.theWorld == null || mc.thePlayer == null) return null;
        Vec3 position = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVector = getVectorForRotation(pitch, yaw);
        Vec3 reachVector = position.addVector(lookVector.xCoord * range, lookVector.yCoord * range, lookVector.zCoord * range);
        Entity pointedEntity = null;
        List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer,
                mc.thePlayer.getEntityBoundingBox().addCoord(lookVector.xCoord * range, lookVector.yCoord * range, lookVector.zCoord * range).expand(1.0D, 1.0D, 1.0D));
        double minDistance = range;
        for (Entity entity : entities) {
            if (entity.canBeCollidedWith()) {
                float borderSize = entity.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(position, reachVector);
                if (axisalignedbb.isVecInside(position)) {
                    if (minDistance >= 0.0D) {
                        pointedEntity = entity;
                        minDistance = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double dist = position.distanceTo(movingobjectposition.hitVec);
                    if (dist < minDistance || minDistance == 0.0D) {
                        pointedEntity = entity;
                        minDistance = dist;
                    }
                }
            }
        }
        return pointedEntity;
    }

    // Potrzebne do AI KillAura (zachowane z poprzedniej wersji)
    public static float updateRotation(float current, float target, float speed) {
        float f = MathHelper.wrapAngleTo180_float(target - current);
        if (f > speed) f = speed;
        if (f < -speed) f = -speed;
        return current + f;
    }

    private static double randomX = 0;
    private static double randomY = 0;
    private static double randomZ = 0;

    public static void randomizePoint() {
        randomX = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
        randomY = ThreadLocalRandom.current().nextDouble(0.1, 0.9);
        randomZ = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
    }

    public static float[] getAiRotations(EntityLivingBase target, float currentYaw, float currentPitch, float speed) {
        double x = (target.posX + (randomX * target.width)) - mc.thePlayer.posX;
        double y = (target.posY + (randomY * target.height)) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = (target.posZ + (randomZ * target.width)) - mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(y, dist) * 180.0D / Math.PI);
        float yawJitter = (float) (ThreadLocalRandom.current().nextDouble(-0.5, 0.5));
        float pitchJitter = (float) (ThreadLocalRandom.current().nextDouble(-0.5, 0.5));
        float finalYaw = updateRotation(currentYaw, yaw + yawJitter, speed);
        float finalPitch = updateRotation(currentPitch, pitch + pitchJitter, speed);
        return new float[]{finalYaw, finalPitch};
    }

    public static float[] getRotations(Entity entity) {
        if (entity == null) return null;
        return getRotations(entity.posX, entity.posY + entity.getEyeHeight() - 0.4, entity.posZ);
    }

    public static float[] getRotations(double x, double y, double z) {
        double xDiff = x - mc.thePlayer.posX;
        double yDiff = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double zDiff = z - mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
        float yaw = (float) (Math.atan2(zDiff, xDiff) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(yDiff, dist) * 180.0D / Math.PI);
        return new float[]{yaw, pitch};
    }
}