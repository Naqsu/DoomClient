package doom.ui;

import doom.module.impl.render.ClickGuiModule; // Import
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;

public class DoomButton extends GuiButton {

    private float hoverAnimation = 0.0f;

    public DoomButton(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, 200, 20, buttonText);
    }

    public DoomButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        float target = hovered ? 1.0f : 0.0f;
        hoverAnimation = RenderUtil.lerp(hoverAnimation, target, 0.2f);

        // --- POBIERANIE KOLORU Z CLICKGUI MODULE ---
        Color themeColor = ClickGuiModule.getGuiColor();

        Color bgNormal = new Color(20, 20, 25, 200);
        Color bgHover = new Color(35, 35, 40, 230);

        // Mieszanie tła
        int r = (int) (bgNormal.getRed() + (bgHover.getRed() - bgNormal.getRed()) * hoverAnimation);
        int g = (int) (bgNormal.getGreen() + (bgHover.getGreen() - bgNormal.getGreen()) * hoverAnimation);
        int b = (int) (bgNormal.getBlue() + (bgHover.getBlue() - bgNormal.getBlue()) * hoverAnimation);
        int a = (int) (bgNormal.getAlpha() + (bgHover.getAlpha() - bgNormal.getAlpha()) * hoverAnimation);
        int finalBg = new Color(r, g, b, a).getRGB();

        // Glow i Obrys w kolorze motywu GUI
        if (hoverAnimation > 0.05f) {
            int alpha = (int)(100 * hoverAnimation);
            int borderAlpha = (int)(255 * hoverAnimation);

            int glowRGB = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha).getRGB();
            int borderRGB = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), borderAlpha).getRGB();

            RenderUtil.drawGlow(this.xPosition, this.yPosition, this.width, this.height, 10, glowRGB);
            RenderUtil.drawRoundedOutline(this.xPosition, this.yPosition, this.width, this.height, 4, 1.0f, borderRGB);
        }

        RenderUtil.drawRoundedRect(this.xPosition, this.yPosition, this.width, this.height, 4, finalBg);

        this.mouseDragged(mc, mouseX, mouseY);
        int textColor = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        FontManager.r20.drawCenteredString(this.displayString, this.xPosition + this.width / 2f, this.yPosition + (this.height - 8) / 2f, textColor);
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }
}