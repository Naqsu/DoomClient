package doom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import java.lang.reflect.Field;

public class TimerUtil {

    // Przechowujemy referencję do Timera, żeby nie szukać go w kółko (optymalizacja)
    private static Timer timerCache = null;

    public static void setTimerSpeed(float speed) {
        try {
            // Jeśli jeszcze nie mamy dostępu do timera, pobieramy go refleksją
            if (timerCache == null) {
                // W MCP pole nazywa się po prostu "timer"
                Field field = Minecraft.class.getDeclaredField("timer");
                field.setAccessible(true); // Tu łamiemy zabezpieczenie "private"
                timerCache = (Timer) field.get(Minecraft.getMinecraft());
            }

            // Ustawiamy prędkość
            timerCache.timerSpeed = speed;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Błąd podczas dostępu do Timera!");
        }
    }

    public static void reset() {
        setTimerSpeed(1.0f);
    }
}