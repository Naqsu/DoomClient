package doom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    public static float targetYaw;
    public static boolean isRotating;
    public static boolean shouldUseCustomPitch = false;
    public static float renderPitch = 0f;

    // Podstawowa metoda
    public static float[] getRotationsToVec(Vec3 vec) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double x = vec.xCoord - eyes.xCoord;
        double y = vec.yCoord - eyes.yCoord;
        double z = vec.zCoord - eyes.zCoord;
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(y, dist) * 180.0D / Math.PI);
        return new float[]{yaw, pitch};
    }

    public static float[] getRotationsToEntityWithOffset(Entity entity, double offX, double offY, double offZ) {
        double diffX = (entity.posX + offX) - mc.thePlayer.posX;
        double diffZ = (entity.posZ + offZ) - mc.thePlayer.posZ;
        double diffY;

        if (entity instanceof net.minecraft.entity.EntityLivingBase) {
            net.minecraft.entity.EntityLivingBase living = (net.minecraft.entity.EntityLivingBase) entity;
            // Celujemy w środek ciała + offset Y (żeby nie celować zawsze w stopy)
            diffY = (living.posY + living.getEyeHeight() * 0.8 + offY) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        } else {
            diffY = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0D - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        }

        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);

        return new float[]{yaw, pitch};
    }

    /**
     * Fix na Aim (Q) GCD.
     * Oblicza minimalną możliwą zmianę kąta na podstawie czułości myszy
     * i przyciąga rotację docelową do tej siatki.
     */
    public static float[] applyGCD(float[] currentRotations, float[] targetRotations) {
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = sensitivity * sensitivity * sensitivity * 1.2F;
        // Ważne: stała 0.15D jest używana w kodzie Minecrafta, mnożymy przez nią
        // aby uzyskać dokładność jak w vanilli.

        // Obliczamy deltę
        float yawDiff = targetRotations[0] - currentRotations[0];
        float pitchDiff = targetRotations[1] - currentRotations[1];

        // Normalizujemy deltę (-180 do 180)
        yawDiff = MathHelper.wrapAngleTo180_float(yawDiff);

        // Przyciągamy deltę do siatki GCD
        // Używamy Math.round, aby znaleźć najbliższy punkt siatki
        yawDiff -= yawDiff % gcd;
        pitchDiff -= pitchDiff % gcd;

        // Zwracamy nową rotację = stara + naprawiona delta
        return new float[]{
                currentRotations[0] + yawDiff,
                MathHelper.clamp_float(currentRotations[1] + pitchDiff, -90.0F, 90.0F)
        };
    }

    public static Entity rayCast(float yaw, float pitch, double range) {
        if (mc.theWorld == null || mc.thePlayer == null) return null;

        Vec3 position = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVector = getVectorForRotation(pitch, yaw);
        Vec3 reachVector = position.addVector(lookVector.xCoord * range, lookVector.yCoord * range, lookVector.zCoord * range);

        Entity pointedEntity = null;
        java.util.List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer,
                mc.thePlayer.getEntityBoundingBox().addCoord(lookVector.xCoord * range, lookVector.yCoord * range, lookVector.zCoord * range).expand(1.0D, 1.0D, 1.0D));

        double minDistance = range;

        for (Entity entity : entities) {
            if (entity.canBeCollidedWith()) {
                float borderSize = entity.getCollisionBorderSize();
                net.minecraft.util.AxisAlignedBB axisalignedbb = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
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

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
    }

    public static void reset() {
        shouldUseCustomPitch = false;
        renderPitch = 0f;
        isRotating = false;
    }
}