package doom.ui.notification;

import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import doom.util.TimeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

public class Notification {
    private String title;
    private String message;
    private NotificationType type;
    private TimeHelper timer;
    private float x, y;
    private float width, height;
    private long duration;

    private float animationX;
    private boolean isLeaving = false;

    public Notification(String title, String message, NotificationType type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.duration = duration;
        this.timer = new TimeHelper();

        Minecraft mc = Minecraft.getMinecraft();
        // Obliczamy szerokość na podstawie tekstu + miejsce na ikonkę
        this.width = Math.max(120, Math.max(FontManager.b18.getStringWidth(title), FontManager.r18.getStringWidth(message)) + 50);
        this.height = 32;

        ScaledResolution sr = new ScaledResolution(mc);
        this.animationX = sr.getScaledWidth();
        this.y = sr.getScaledHeight() - 50;
    }

    public void render(float targetY) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);

        // --- ANIMACJA ---
        float targetX;
        if (isLeaving) {
            targetX = sr.getScaledWidth() + width + 20; // Wyjazd w prawo
        } else {
            targetX = sr.getScaledWidth() - width - 10; // Wjazd na ekran
        }

        if (timer.hasReached(duration) && !isLeaving) {
            isLeaving = true;
        }

        // Płynna interpolacja (Szybsza: 0.2f)
        animationX = RenderUtil.lerp(animationX, targetX, 0.2f);
        y = RenderUtil.lerp(y, targetY, 0.2f);

        float drawX = animationX;
        float drawY = y;

        // Pobieramy kolor typu (np. Czerwony dla Error)
        Color typeColor = type.getColorObj();
        int colorRGB = typeColor.getRGB();
        int glowRGB = new Color(typeColor.getRed(), typeColor.getGreen(), typeColor.getBlue(), 100).getRGB();

        // --- RENDEROWANIE ---

        // 1. POŚWIATA (Kolorowa, pod spodem)
        RenderUtil.drawGlow(drawX, drawY, width, height, 3, glowRGB);

        // 2. BLUR (Rozmycie tła gry)
        RenderUtil.drawBlur(drawX, drawY, width, height, 10);

        // 3. TŁO (Ciemne szkło)
        RenderUtil.drawRoundedRect(drawX, drawY, width, height, 6, new Color(20, 20, 20, 220).getRGB());

        // 4. OBRYS (Cienki, w kolorze typu)
        RenderUtil.drawRoundedOutline(drawX, drawY, width, height, 6, 1.0f, colorRGB);

        // 5. IKONA (Kółko z literą)
        float iconSize = 20;
        float iconX = drawX + 6;
        float iconY = drawY + (height - iconSize) / 2;

        // Tło ikonki (półprzezroczyste w kolorze typu)
        RenderUtil.drawRoundedRect(iconX, iconY, iconSize, iconSize, iconSize/2, new Color(typeColor.getRed(), typeColor.getGreen(), typeColor.getBlue(), 50).getRGB());
        // Obrys ikonki
        RenderUtil.drawRoundedOutline(iconX, iconY, iconSize, iconSize, iconSize/2, 1.0f, colorRGB);
        // Znak (np. "!" lub "v")
        FontManager.b20.drawCenteredString(type.getIcon(), iconX + iconSize/2, iconY + 5, -1);

        // 6. TEKST
        FontManager.b18.drawStringWithShadow(title, drawX + 32, drawY + 6, -1);
        FontManager.r18.drawStringWithShadow(message, drawX + 32, drawY + 18, new Color(200, 200, 200).getRGB());

        // 7. PASEK POSTĘPU (Na dole)
        if (!isLeaving) {
            float timeData = (float)timer.getTime() / (float)duration; // 0.0 -> 1.0
            float barWidth = width * (1.0f - timeData); // Zmniejsza się

            // Rysujemy pasek jako cieniutką linię na dole
            // Używamy drawRoundedRect z wysokością 2px
            if (barWidth > 0) {
                // Rysujemy na samym dole, z zaokrągleniem pasującym do okna (tylko dolne rogi by się przydały, ale małe zaokrąglenie jest ok)
                RenderUtil.drawRoundedRect(drawX + 2, drawY + height - 3, barWidth - 4, 1.5f, 0.5f, colorRGB);
            }
        }
    }

    public boolean shouldDelete() {
        // Usuwamy dopiero jak wyjedzie całkowicie poza ekran
        return isLeaving && animationX > Minecraft.getMinecraft().displayWidth;
    }

    public float getHeight() { return height; }
}