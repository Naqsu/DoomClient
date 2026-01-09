package doom.module.impl.render;

import doom.Client;
import doom.module.Category;
import doom.module.DraggableModule;
import doom.util.ColorUtil;
import doom.util.RenderUtil;
import java.awt.Color;

public class Watermark extends DraggableModule {

    public Watermark() {
        super("Watermark", Category.RENDER);
        this.hidden = true;
        this.x = 5;
        this.y = 5;
    }

    @Override
    public float getWidth() {
        String text = "Doom Client " + Client.INSTANCE.version;
        return mc.fontRendererObj.getStringWidth(text) + 8;
    }

    @Override
    public float getHeight() { return 18; }

    @Override
    public void render(float x, float y) {
        // Pobieramy ustawienia z HUD
        HUD hud = Client.INSTANCE.moduleManager.getModule(HUD.class);
        boolean showBg = hud.background.isEnabled();
        boolean rainbow = hud.rainbow.isEnabled();

        String title = "D" + "\u00A7foo" + "m";
        String ver = " \u00A77" + Client.INSTANCE.version;
        String text = title + ver;

        // Tło (sterowane z HUD)
        if (showBg) {
            RenderUtil.drawRoundedRect(x, y, getWidth(), getHeight(), 4, new Color(20, 20, 20, 200).getRGB());
        }

        // Pasek ozdobny
        int color = rainbow ? ColorUtil.getRainbow(4.0f, 0.8f, 1.0f, 0) : new Color(200, 0, 0).getRGB();
        // Jeśli nie ma tła, to pasek rysujemy trochę inaczej (np. pod tekstem), ale tutaj zostawmy na górze
        if (showBg || hud.sidebar.isEnabled()) {
            RenderUtil.drawRoundedRect(x, y, getWidth(), 2, 2, color);
        }

        mc.fontRendererObj.drawStringWithShadow(text, x + 4, y + 6, -1);
    }
}