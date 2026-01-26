package doom.module.impl.render;

import doom.Client;
import doom.module.DraggableModule;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class Watermark extends DraggableModule {

    public Watermark() {
        super("Watermark", Category.RENDER);
        this.x = 10;
        this.y = 10;
    }

    @Override public float getWidth() { return 150; }
    @Override public float getHeight() { return 24; }

    @Override
    public void render(float x, float y) {
        HUD hud = Client.INSTANCE.moduleManager.getModule(HUD.class);
        int mainColor = hud.getWatermarkColor();

        String title = "Doom";
        String ver = Client.INSTANCE.version;
        String fps = Minecraft.getDebugFPS() + " FPS";

        float titleW = FontManager.b20.getStringWidth(title);
        float totalWidth = titleW + FontManager.r18.getStringWidth(" | " + ver + " | " + fps) + 12;
        float height = 20;

        // 1. BLUR (Prostokątny, ale pod zaokrąglonym tłem wygląda ok)
        if (hud.blur.isEnabled()) {
            RenderUtil.drawBlur(x, y, totalWidth, height, 20);
        }

        // 2. TŁO (Zaokrąglone, Półprzezroczyste)
        // Alpha 100/255 = widać blur pod spodem!
        RenderUtil.drawRoundedRect(x, y, totalWidth, height, 6, new Color(10, 10, 15, 100).getRGB());

        // 3. LOGO (Kolor)
        FontManager.b20.drawStringWithShadow(title, x + 5, y + 5, mainColor);

        // 4. RESZTA (Szara)
        String rest = "\u00A77 | \u00A7f" + ver + " \u00A77| \u00A7f" + fps;
        FontManager.r18.drawStringWithShadow(rest, x + 5 + titleW, y + 6, -1);

        // 5. GÓRNA LINIA (Opcjonalnie, jak w Astralis)
        // RenderUtil.drawRoundedRect(x + 2, y, totalWidth - 4, 1, 0.5f, mainColor);
    }
}