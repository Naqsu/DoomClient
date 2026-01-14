package doom.module.impl.render;

import doom.module.DraggableModule;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;

import java.awt.*;

public class SessionInfo extends DraggableModule {

    // Przykladowe statystyki
    private long startTime = System.currentTimeMillis();

    public SessionInfo() {
        super("SessionInfo", Category.RENDER);
        this.setToggled(true); // Włączony domyślnie
    }

    @Override
    public float getWidth() {
        return 120; // Stała szerokość okienka
    }

    @Override
    public float getHeight() {
        return 50; // Stała wysokość
    }

    @Override
    public void render(float x, float y) {
        // 1. TŁO
        RenderUtil.drawRoundedRect(x, y, getWidth(), getHeight(), 4, new Color(20, 20, 20, 200).getRGB());
        // Opcjonalnie: Pasek na górze
        RenderUtil.drawRoundedRect(x, y, getWidth(), 2, 2, new Color(230, 0, 0).getRGB());

        // 2. DANE
        long duration = System.currentTimeMillis() - startTime;
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        long hours = (duration / (1000 * 60 * 60)) % 24;
        String timePlayed = String.format("%02dh %02dm %02ds", hours, minutes, seconds);

        FontManager.r20.drawStringWithShadow("Session Info", x + 5, y + 6, -1);

        // Linia oddzielająca
        RenderUtil.drawRect(x + 5, y + 16, x + getWidth() - 5, y + 17, new Color(60, 60, 60).getRGB());

        FontManager.r20.drawStringWithShadow("Time: \u00A77" + timePlayed, x + 5, y + 22, -1);
        FontManager.r20.drawStringWithShadow("Name: \u00A77" + mc.session.getUsername(), x + 5, y + 34, -1);
    }
}