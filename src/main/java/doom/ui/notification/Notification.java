package doom.ui.notification;

import doom.util.RenderUtil;
import doom.util.TimeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class Notification {
    private String title;
    private String message;
    private NotificationType type;
    private TimeHelper timer;
    private float x, y; // Aktualna pozycja
    private float width, height;
    private long duration;

    // Animacja
    private float animationX;
    private boolean isLeaving = false;

    public Notification(String title, String message, NotificationType type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.duration = duration;
        this.timer = new TimeHelper();

        Minecraft mc = Minecraft.getMinecraft();
        this.width = Math.max(mc.fontRendererObj.getStringWidth(title), mc.fontRendererObj.getStringWidth(message)) + 40;
        this.height = 30;

        ScaledResolution sr = new ScaledResolution(mc);
        this.animationX = sr.getScaledWidth();

        // --- POPRAWKA: USTAW STARTOWE Y ---
        // Ustawiamy Y na dół ekranu, żeby nie przylatywało z góry (z Y=0)
        // Dzięki temu powiadomienie pojawi się "na wysokości", a tylko wsunie z boku.
        this.y = sr.getScaledHeight() - 40;
        // ----------------------------------
    }

    public void render(float targetY) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);

        // 1. Obliczanie celu animacji X
        // Jeśli czas minął -> wyjeżdżamy w prawo. Jeśli nie -> wjeżdżamy na ekran.
        float targetX;
        if (isLeaving) {
            targetX = sr.getScaledWidth() + width + 10; // Poza ekran
        } else {
            targetX = sr.getScaledWidth() - width - 10; // Na ekranie
        }

        // Sprawdzamy czas
        if (timer.hasReached(duration) && !isLeaving) {
            isLeaving = true;
        }

        // 2. Interpolacja (Płynny ruch)
        // Im większa liczba na końcu (0.15), tym szybciej.
        animationX = (float) RenderUtil.lerp(animationX, targetX, 0.15);

        // Interpolacja Y (żeby ładnie się przesuwały w górę/dół gdy inne znikają)
        y = (float) RenderUtil.lerp(y, targetY, 0.15);

        // 3. Rysowanie
        float drawX = animationX;
        float drawY = y;

        // Tło
        RenderUtil.drawRoundedRect(drawX, drawY, width, height, 4, 0xDD202020); // Ciemnoszare półprzezroczyste

        // Pasek koloru z lewej (oznacza typ)
        RenderUtil.drawRoundedRect(drawX, drawY, 2, height, 1, type.getColor());

        // Ikonka/Typ (Możesz tu dać obrazek, na razie damy pierwszą literę typu w kółeczku)
        // RenderUtil.drawRoundedRect(drawX + 5, drawY + 5, 20, 20, 10, 0x55000000);
        // mc.fontRendererObj.drawStringWithShadow(type.getName().substring(0, 1), drawX + 11, drawY + 11, -1);

        // Tekst
        mc.fontRendererObj.drawStringWithShadow(title, drawX + 10, drawY + 6, -1);
        mc.fontRendererObj.drawStringWithShadow(message, drawX + 10, drawY + 18, 0xFFAAAAAA);

        // Pasek postępu na dole (opcjonalne)
        if (!isLeaving) {
            float timeData = (float)timer.getTime() / (float)duration; // 0.0 do 1.0
            float barWidth = width * (1.0f - timeData);
            RenderUtil.drawRect(drawX, drawY + height - 2, drawX + barWidth, drawY + height, type.getColor());
        }
    }

    // Sprawdza, czy powiadomienie powinno zostać usunięte (wyjechało za ekran)
    public boolean shouldDelete() {
        return isLeaving && animationX > Minecraft.getMinecraft().displayWidth;
    }

    public float getHeight() { return height; }
}