package doom.ui.font;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomFontRenderer extends CFont {
    protected CharData[] boldChars = new CharData[65536]; // Zwiększamy rozmiar
    protected CharData[] italicChars = new CharData[65536];
    protected CharData[] boldItalicChars = new CharData[65536];

    private final int[] colorCode = new int[32];
    protected DynamicTexture texBold;
    protected DynamicTexture texItalic;
    protected DynamicTexture texItalicBold;

    public CustomFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        super(font, antiAlias, fractionalMetrics);
        setupMinecraftColorcodes();
        setupBoldItalicIDs();
    }

    public float drawStringWithShadow(String text, double x, double y, int color) {
        float shadowWidth = drawString(text, x + 0.5D, y + 0.5D, color, true);
        return Math.max(shadowWidth, drawString(text, x, y, color, false));
    }

    public float drawString(String text, float x, float y, int color) {
        return drawString(text, x, y, color, false);
    }

    public float drawCenteredString(String text, float x, float y, int color) {
        return drawString(text, x - getWidth(text) / 2, y, color);
    }

    public float drawCenteredStringWithShadow(String text, float x, float y, int color) {
        return drawStringWithShadow(text, x - getWidth(text) / 2, y, color);
    }

    public float drawString(String text, double x, double y, int color, boolean shadow) {
        x -= 1;
        y -= 2;
        if (text == null) return 0.0F;
        if (color == 553648127) color = 16777215;
        if ((color & 0xFC000000) == 0) color |= -16777216;

        if (shadow) color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;

        CharData[] currentData = this.charData;
        float alpha = (float) (color >> 24 & 0xFF) / 255.0F;
        boolean bold = false;
        boolean italic = false;
        boolean strikethrough = false;
        boolean underline = false;

        x *= 2.0D;
        y *= 2.0D;

        GL11.glPushMatrix();
        GlStateManager.scale(0.5D, 0.5D, 0.5D);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color((float) (color >> 16 & 0xFF) / 255.0F, (float) (color >> 8 & 0xFF) / 255.0F, (float) (color & 0xFF) / 255.0F, alpha);
        int size = text.length();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(tex.getGlTextureId());
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getGlTextureId());

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);
            if ((character == '\u00A7') && (i < size)) {
                int colorIndex = 21;
                try {
                    colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
                } catch (Exception e) {}

                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    GlStateManager.bindTexture(tex.getGlTextureId());
                    currentData = this.charData;
                    if ((colorIndex < 0) || (colorIndex > 15)) colorIndex = 15;
                    if (shadow) colorIndex += 16;
                    int colorcode = this.colorCode[colorIndex];
                    GlStateManager.color((float) (colorcode >> 16 & 0xFF) / 255.0F, (float) (colorcode >> 8 & 0xFF) / 255.0F, (float) (colorcode & 0xFF) / 255.0F, alpha);
                } else if (colorIndex == 17) {
                    bold = true;
                    if (italic) {
                        GlStateManager.bindTexture(texItalicBold.getGlTextureId());
                        currentData = this.boldItalicChars;
                    } else {
                        GlStateManager.bindTexture(texBold.getGlTextureId());
                        currentData = this.boldChars;
                    }
                } else if (colorIndex == 18) {
                    strikethrough = true;
                } else if (colorIndex == 19) {
                    underline = true;
                } else if (colorIndex == 20) {
                    italic = true;
                    if (bold) {
                        GlStateManager.bindTexture(texItalicBold.getGlTextureId());
                        currentData = this.boldItalicChars;
                    } else {
                        GlStateManager.bindTexture(texItalic.getGlTextureId());
                        currentData = this.italicChars;
                    }
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    GlStateManager.color((float) (color >> 16 & 0xFF) / 255.0F, (float) (color >> 8 & 0xFF) / 255.0F, (float) (color & 0xFF) / 255.0F, alpha);
                    GlStateManager.bindTexture(tex.getGlTextureId());
                    currentData = this.charData;
                }
                i++;
            } else if ((character < currentData.length) && (character >= 0)) {
                // --- CRITICAL FIX: Zabezpieczenie przed brakiem znaku ---
                if (currentData[character] == null) {
                    continue; // Pomiń znak, jeśli nie został wygenerowany
                }
                // --------------------------------------------------------

                GL11.glBegin(GL11.GL_TRIANGLES);
                drawChar(currentData, character, (float) x, (float) y);
                GL11.glEnd();

                if (strikethrough)
                    drawLine(x, y + currentData[character].height / 2, x + currentData[character].width - 8.0D, y + currentData[character].height / 2, 1.0F);
                if (underline)
                    drawLine(x, y + currentData[character].height - 2.0D, x + currentData[character].width - 8.0D, y + currentData[character].height - 2.0D, 1.0F);

                x += (currentData[character].width - 8 + this.charOffset);
            }
        }
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glPopMatrix();

        return (float) x / 2.0F;
    }

    public int getStringWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        CharData[] currentData = this.charData;
        boolean bold = false;
        boolean italic = false;
        int size = text.length();
        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);
            if ((character == '\u00A7') && (i < size)) {
                int colorIndex = "0123456789abcdefklmnor".indexOf(character);
                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                } else if (colorIndex == 17) {
                    bold = true;
                    if (italic) currentData = this.boldItalicChars;
                    else currentData = this.boldChars;
                } else if (colorIndex == 20) {
                    italic = true;
                    if (bold) currentData = this.boldItalicChars;
                    else currentData = this.italicChars;
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    currentData = this.charData;
                }
                i++;
            } else if ((character < currentData.length) && (character >= 0)) {
                // --- CRITICAL FIX: Zabezpieczenie ---
                if (currentData[character] == null) {
                    continue; // Pomiń przy obliczaniu szerokości
                }
                // ------------------------------------
                width += currentData[character].width - 8 + this.charOffset;
            }
        }
        return width / 2;
    }

    public int getWidth(String text) {
        return getStringWidth(text);
    }

    public int getHeight() {
        return (this.fontHeight - 8) / 2;
    }

    private void drawLine(double x, double y, double x1, double y1, float width) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x1, y1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    // Helpery do łamania linii pominięte dla czytelności (nie wpływają na crash)
    // Jeśli ich potrzebujesz, skopiuj z poprzedniej wersji, ale getStringWidth jest tu najważniejsze.

    private void setupMinecraftColorcodes() {
        for (int index = 0; index < 32; index++) {
            int noClue = (index >> 3 & 0x1) * 85;
            int red = (index >> 2 & 0x1) * 170 + noClue;
            int green = (index >> 1 & 0x1) * 170 + noClue;
            int blue = (index >> 0 & 0x1) * 170 + noClue;
            if (index == 6) red += 85;
            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            this.colorCode[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
        }
    }

    private void setupBoldItalicIDs() {
        texBold = setupTexture(font.deriveFont(1), antiAlias, fractionalMetrics, boldChars);
        texItalic = setupTexture(font.deriveFont(2), antiAlias, fractionalMetrics, italicChars);
        texItalicBold = setupTexture(font.deriveFont(3), antiAlias, fractionalMetrics, boldItalicChars);
    }
}