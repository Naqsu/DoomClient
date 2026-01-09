package doom.util;

import java.awt.Color;

public class ColorUtil {

    // Metoda generująca tęczę
    // seconds = szybkość, saturation = nasycenie, brightness = jasność, index = przesunięcie (dla fali)
    public static int getRainbow(float seconds, float saturation, float brightness, long index) {
        float hue = ((System.currentTimeMillis() + index) % (int)(seconds * 1000)) / (float)(seconds * 1000);
        return Color.getHSBColor(hue, saturation, brightness).getRGB();
    }
}