package doom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

public class FightUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean canHit(double chance) {
        return Math.random() <= chance / 100.0;
    }

    public static boolean isValid(EntityLivingBase entity, double range, boolean players, boolean animals, boolean mobs, boolean invis, boolean walls) {
        if (entity == null || entity == mc.thePlayer || entity.isDead || entity.getHealth() <= 0) return false;

        if (mc.thePlayer.getDistanceToEntity(entity) > range) return false;

        if (!walls && !mc.thePlayer.canEntityBeSeen(entity)) return false;

        if (entity.isInvisible() && !invis) return false;
        if (entity instanceof EntityArmorStand) return false;

        if (entity instanceof EntityPlayer && !players) return false;
        if (entity instanceof EntityAnimal && !animals) return false;
        if ((entity instanceof EntityMob) && !mobs) return false;

        // Tutaj można dodać sprawdzanie AntiBot z Twojego istniejącego modułu
        // if (AntiBot.isBot(entity)) return false;

        return true;
    }

    public static List<EntityLivingBase> getMultipleTargets(double range, boolean players, boolean animals, boolean walls, boolean mobs, boolean invis) {
        List<EntityLivingBase> list = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                if (isValid(living, range, players, animals, mobs, invis, walls)) {
                    list.add(living);
                }
            }
        }
        return list;
    }

    // Metoda pomocnicza dla Atani "Range calculation" (Raycast distance vs DistanceToEntity)
    public static double getRange(Entity entity) {
        if (mc.thePlayer == null) return 0;
        return mc.thePlayer.getDistanceToEntity(entity);
    }
}