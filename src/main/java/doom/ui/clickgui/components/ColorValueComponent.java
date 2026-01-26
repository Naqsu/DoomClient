package doom.ui.clickgui.components;

import doom.settings.impl.ColorSetting;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.awt.*;

public class ColorValueComponent extends Component {

    private boolean draggingHue = false;
    private boolean draggingSaturation = false;
    private boolean draggingAlpha = false;

    private float[] hsbaCache;

    public ColorValueComponent(ColorSetting setting, float width, float height) {
        super(setting, width, height);
        hsbaCache = setting.getHSBA();
    }

    @Override
    public void draw(float x, float y, int mouseX, int mouseY) {
        this.x = x; this.y = y;
        ColorSetting colorSet = (ColorSetting) setting;

        // Wysokość zależy od tego czy rozwinięte
        this.height = colorSet.expanded ? 130 : 20;

        // --- WIZUALNA SEPARACJA (FIX) ---
        if (colorSet.expanded) {
            // 1. Tło pod całym komponentem (ciemniejsze)
            RenderUtil.drawRoundedRect(x, y, width, height, 4, new Color(15, 15, 18, 200).getRGB());

            // 2. Obrys w kolorze edytowanym (żeby wiedzieć który to który)
            // Pobieramy aktualny kolor z suwaków
            int currentColor = colorSet.getColor();
            // Rysujemy ramkę
            RenderUtil.drawRoundedOutline(x, y, width, height, 4, 1.0f, currentColor);

            // 3. Pasek z lewej strony (Akcent)
            RenderUtil.drawRoundedRect(x + 2, y + 4, 2, height - 8, 1, currentColor);
        }

        // Nazwa ustawienia (Przesuwamy lekko w prawo jeśli rozwinięte, bo pasek)
        float textX = colorSet.expanded ? x + 10 : x + 5;
        FontManager.r18.drawStringWithShadow(setting.name, textX, y + 6, -1);

        // Podgląd koloru (mały prostokąt po prawej)
        float previewX = x + width - 30;
        float previewY = y + 4;

        // Jeśli rozwinięte, nie musimy pokazywać małego podglądu, bo mamy ramkę,
        // ale zostawmy go dla spójności (lub ukryjmy jeśli wolisz).
        // Rysujemy go zawsze:
        drawCheckerboard(previewX, previewY, 20, 12);
        RenderUtil.drawRoundedRect(previewX, previewY, 20, 12, 4, colorSet.getColor());

        // Jeśli rozwinięte -> rysujemy picker
        if (colorSet.expanded) {
            drawPicker(x + 5, y + 25, width - 10, mouseX, mouseY, colorSet);
        }
    }

    private void drawPicker(float x, float y, float w, int mouseX, int mouseY, ColorSetting s) {
        // --- 1. BOX NASYCENIA I JASNOŚCI (SB Box) ---
        float sbHeight = 60;
        int colorHue = Color.HSBtoRGB(hsbaCache[0], 1.0f, 1.0f);

        // Gradient Boxa
        drawGradientRect(x, y, w, sbHeight, 0xFFFFFFFF, colorHue, 0xFF000000, 0xFF000000);

        // Kropka
        float satX = x + (hsbaCache[1] * w);
        float briY = y + ((1.0f - hsbaCache[2]) * sbHeight);
        int knobOutline = hsbaCache[2] < 0.5 ? -1 : 0xFF000000;
        RenderUtil.drawRoundedOutline(satX - 2, briY - 2, 4, 4, 2, 1.0f, knobOutline);

        if (draggingSaturation) {
            hsbaCache[1] = Math.min(Math.max((mouseX - x) / w, 0), 1);
            hsbaCache[2] = 1.0f - Math.min(Math.max((mouseY - y) / sbHeight, 0), 1);
            updateColor(s);
        }

        // --- 2. PASEK HUE ---
        float hueY = y + sbHeight + 5;
        float hueH = 10;
        int[] hues = { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000 };

        for (int i = 0; i < 6; i++) {
            float segX = x + (w / 6.0f) * i;
            float segW = (w / 6.0f);
            drawGradientRect(segX, hueY, segW + 0.5f, hueH, hues[i], hues[i+1], hues[i], hues[i+1]);
        }

        float hueKnobX = x + (hsbaCache[0] * w);
        RenderUtil.drawRect(hueKnobX - 1, hueY, 2, hueH, -1);

        if (draggingHue) {
            hsbaCache[0] = Math.min(Math.max((mouseX - x) / w, 0), 1);
            updateColor(s);
        }

        // --- 3. PASEK ALPHA ---
        float alphaY = hueY + hueH + 5;
        float alphaH = 10;

        drawCheckerboard(x, alphaY, w, alphaH);
        int col1 = Color.HSBtoRGB(hsbaCache[0], hsbaCache[1], hsbaCache[2]) & 0x00FFFFFF;
        int col2 = Color.HSBtoRGB(hsbaCache[0], hsbaCache[1], hsbaCache[2]) | 0xFF000000;

        drawGradientRect(x, alphaY, w, alphaH, col1, col2, col1, col2);

        float alphaKnobX = x + (hsbaCache[3] * w);
        RenderUtil.drawRect(alphaKnobX - 1, alphaY, 2, alphaH, -1);

        if (draggingAlpha) {
            hsbaCache[3] = Math.min(Math.max((mouseX - x) / w, 0), 1);
            updateColor(s);
        }
    }

    // --- RYSOWANIE LOW-LEVEL ---
    private void drawGradientRect(float x, float y, float w, float h, int tl, int tr, int bl, int br) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        addVertex(buffer, x + w, y, tr);
        addVertex(buffer, x, y, tl);
        addVertex(buffer, x, y + h, bl);
        addVertex(buffer, x + w, y + h, br);
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private void addVertex(WorldRenderer buffer, float x, float y, int color) {
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        buffer.pos(x, y, 0.0D).color(r, g, b, a).endVertex();
    }

    private void drawCheckerboard(float x, float y, float w, float h) {
        RenderUtil.drawRect(x, y, w, h, -1);
        int size = 4;
        for (float i = 0; i < w; i += size) {
            for (float j = 0; j < h; j += size) {
                if (((int)(i / size) + (int)(j / size)) % 2 == 0) {
                    float rw = Math.min(size, w - i);
                    float rh = Math.min(size, h - j);
                    RenderUtil.drawRect(x + i, y + j, rw, rh, 0xFFCCCCCC);
                }
            }
        }
    }

    private void updateColor(ColorSetting s) {
        s.setHSBA(hsbaCache[0], hsbaCache[1], hsbaCache[2], hsbaCache[3]);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        ColorSetting colorSet = (ColorSetting) setting;

        if (isHovered(mouseX, mouseY) && mouseY < y + 20 && button == 1) {
            colorSet.expanded = !colorSet.expanded;
            return;
        }

        if (colorSet.expanded) {
            if (mouseY >= y + 25 && mouseY <= y + 85 && mouseX >= x && mouseX <= x + width) {
                draggingSaturation = true;
            }
            else if (mouseY >= y + 90 && mouseY <= y + 100 && mouseX >= x && mouseX <= x + width) {
                draggingHue = true;
            }
            else if (mouseY >= y + 105 && mouseY <= y + 115 && mouseX >= x && mouseX <= x + width) {
                draggingAlpha = true;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        draggingHue = false; draggingSaturation = false; draggingAlpha = false;
    }

    @Override public void keyTyped(char typedChar, int keyCode) {}
}