package doom.util;

import net.minecraft.client.entity.EntityPlayerSP;

import java.lang.reflect.Field;

public class ReflectionUtil {

    private static Field serverSprintStateField = null;

    public static void setServerSprintState(EntityPlayerSP player, boolean sprinting) {
        try {
            if (serverSprintStateField == null) {
                serverSprintStateField = EntityPlayerSP.class.getDeclaredField("serverSprintState");
                serverSprintStateField.setAccessible(true);
            }
            serverSprintStateField.setBoolean(player, sprinting);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Błąd podczas dostępu do serverSprintState!");
        }
    }

    public static boolean getServerSprintState(EntityPlayerSP player) {
        try {
            if (serverSprintStateField == null) {
                serverSprintStateField = EntityPlayerSP.class.getDeclaredField("serverSprintState");
                serverSprintStateField.setAccessible(true);
            }
            return serverSprintStateField.getBoolean(player);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}