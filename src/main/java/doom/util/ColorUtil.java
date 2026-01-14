package doom.util;

import java.awt.*;

public class ColorUtil {

    public static int getRainbow(float seconds, float saturation, float brightness, long index) {
        float hue = ((System.currentTimeMillis() + index) % (int)(seconds * 1000)) / (float)(seconds * 1000);
        return Color.getHSBColor(hue, saturation, brightness).getRGB();
    }

    public static int getAstolfo(int index, int speed, float saturation, float brightness) {
        // FIX: Używamy modulo (%) zamiast pętli while, żeby nie crashowało
        long time = System.currentTimeMillis() * speed - index * 70L;
        float hue = ((time % 3000) / 3000.0f);
        if (hue < 0.5f) {
            // 0.0 -> 0.5 (Wzrost)
            hue = hue * 2.0f;
        } else {
            // 0.5 -> 1.0 (Spadek)
            hue = 2.0f - (hue * 2.0f);
        }
        return Color.getHSBColor(hue, saturation, brightness).getRGB();
    }

    public static int getDoomColor(int index, int speed) {
        double time = (System.currentTimeMillis() * speed + index * 100) % 2000.0 / 1000.0;
        double wave = 0.5 + 0.5 * Math.sin(time * Math.PI);
        Color c1 = new Color(255, 20, 20);
        Color c2 = new Color(100, 0, 0);
        int r = (int) (c1.getRed() * wave + c2.getRed() * (1.0 - wave));
        int g = (int) (c1.getGreen() * wave + c2.getGreen() * (1.0 - wave));
        int b = (int) (c1.getBlue() * wave + c2.getBlue() * (1.0 - wave));
        return new Color(r, g, b).getRGB();
    }
}