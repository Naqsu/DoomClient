package doom.ui;

import doom.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class DoomButton extends GuiButton {

    // --- KONFIGURACJA ANIMACJI ---
    // Szybkość zmiany kolorów tła (0.1 = wolno/płynnie)
    private static final float BG_ANIMATION_SPEED = 0.15f;

    // Szybkość wysuwania się paska (0.4 = szybko/agresywnie, ale płynnie)
    // Zwiększ do 0.6 jeśli nadal za wolno, zmniejsz do 0.2 jeśli za szybko.
    private static final float BAR_ANIMATION_SPEED = 0.40f;

    private static final float CORNER_RADIUS = 4.0f;

    // --- KOLORY (DOOM THEME) ---
    // Normalne
    private static final int COL_BG_TOP = 0xDD1a1a2e;
    private static final int COL_BG_BOT = 0xDD0f3460;
    private static final int COL_BORDER = 0x55e94560;
    private static final int COL_TEXT   = 0xFFCCCCCC;

    // Hover (Po najechaniu)
    private static final int COL_BG_HOVER_TOP = 0xDD16213e;
    private static final int COL_BG_HOVER_BOT = 0xDD1a1a2e;
    private static final int COL_BORDER_HOVER = 0xFFe94560;
    private static final int COL_TEXT_HOVER   = 0xFFFFFFFF;

    // --- ZMIENNE STANU ---
    private float hoverProgress = 0.0f;     // Do tła
    private float barWidthProgress = 0.0f;  // Do paska (niezależna animacja)

    public DoomButton(int buttonId, int x, int y, String buttonText) {
        this(buttonId, x, y, 200, 20, buttonText);
    }

    public DoomButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        // 1. Wykrywanie myszki
        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        // 2. Obliczanie celu animacji (0.0 lub 1.0)
        float target = this.hovered ? 1.0f : 0.0f;

        // 3. Aktualizacja animacji (niezależne prędkości)
        this.hoverProgress = interpolate(this.hoverProgress, target, BG_ANIMATION_SPEED);
        this.barWidthProgress = interpolate(this.barWidthProgress, target, BAR_ANIMATION_SPEED);

        // 4. Mieszanie kolorów tła
        int topColor = blendColors(COL_BG_TOP, COL_BG_HOVER_TOP, hoverProgress);
        int bottomColor = blendColors(COL_BG_BOT, COL_BG_HOVER_BOT, hoverProgress);
        int borderColor = blendColors(COL_BORDER, COL_BORDER_HOVER, hoverProgress);
        int textColor = blendColors(COL_TEXT, COL_TEXT_HOVER, hoverProgress);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // 5. Cień pod przyciskiem (stały)
        RenderUtil.drawRoundedRect(
                this.xPosition + 2, this.yPosition + 2,
                this.width, this.height,
                CORNER_RADIUS, 0x55000000
        );

        // 6. Tło gradientowe
        RenderUtil.drawRoundedGradientRect(
                this.xPosition, this.yPosition,
                this.width, this.height,
                CORNER_RADIUS, topColor, bottomColor
        );

        // 7. Obramowanie
        RenderUtil.drawRoundedOutline(
                this.xPosition, this.yPosition,
                this.width, this.height,
                CORNER_RADIUS, 1.0f, borderColor
        );

        // 8. Efekt Glow (Poświata) - tylko gdy najechane
        if (hoverProgress > 0.05f) {
            int glowAlpha = (int)(60 * hoverProgress); // Max alpha ok. 60
            int glowColor = (glowAlpha << 24) | (COL_BORDER_HOVER & 0x00FFFFFF);

            RenderUtil.drawRoundedOutline(
                    this.xPosition - 1, this.yPosition - 1,
                    this.width + 2, this.height + 2,
                    CORNER_RADIUS + 1, 2.0f, glowColor
            );
        }

        // 9. Tekst i Pasek
        this.mouseDragged(mc, mouseX, mouseY);
        drawCustomText(mc, textColor);
    }

    private void drawCustomText(Minecraft mc, int color) {
        int textX = this.xPosition + this.width / 2;
        int textY = this.yPosition + (this.height - 8) / 2;

        // Delikatne uniesienie tekstu
        float textOffset = hoverProgress * -1.0f;

        // Cień tekstu (zanika przy hoverze)
        if (hoverProgress < 0.9f) {
            int shadowAlpha = (int)(255 * (1.0f - hoverProgress));
            if (shadowAlpha > 4) {
                this.drawCenteredString(mc.fontRendererObj, this.displayString,
                        textX + 1, (int)(textY + 1 + textOffset), (shadowAlpha << 24));
            }
        }

        // Główny tekst
        this.drawCenteredString(mc.fontRendererObj, this.displayString,
                textX, (int)(textY + textOffset), color);

        // --- PASEK AKCENTUJĄCY ---
        // Rysujemy tylko jeśli animacja paska ruszyła
        if (barWidthProgress > 0.01f) {
            int stringWidth = mc.fontRendererObj.getStringWidth(this.displayString);

            // Szerokość paska zależna od SZYBKIEJ animacji (barWidthProgress)
            float barWidth = stringWidth * barWidthProgress;

            // Centrowanie paska
            float barX = this.xPosition + (this.width - barWidth) / 2.0f;
            float barY = textY + 10;

            // Alpha paska też się animuje, żeby nie "wskakiwał" nagle
            int barAlpha = (int)(255 * barWidthProgress);
            if (barAlpha > 255) barAlpha = 255;

            int barColor = (barAlpha << 24) | (COL_BORDER_HOVER & 0x00FFFFFF);

            RenderUtil.drawRect(barX, barY, barX + barWidth, barY + 1, barColor);
        }
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    // --- Metody pomocnicze ---

    private float interpolate(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.005f) return target;
        return current + diff * speed;
    }

    private int blendColors(int color1, int color2, float ratio) {
        if (ratio <= 0) return color1;
        if (ratio >= 1) return color2;

        float iRatio = 1.0f - ratio;

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 * iRatio + a2 * ratio);
        int r = (int)(r1 * iRatio + r2 * ratio);
        int g = (int)(g1 * iRatio + g2 * ratio);
        int b = (int)(b1 * iRatio + b2 * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}