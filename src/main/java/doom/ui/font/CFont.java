package doom.ui.font;

import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class CFont {
    // Zwiększamy rozmiar tekstury, żeby pomieścić ikony
    protected float imgSize = 1024;
    // Zwiększamy tablicę, aby obsłużyć znaki Unicode (ikony są w okolicy 61000)
    protected CharData[] charData = new CharData[65536];
    protected Font font;
    protected boolean antiAlias;
    protected boolean fractionalMetrics;
    protected int fontHeight = -1;
    protected int charOffset = 0;
    protected DynamicTexture tex;

    public CFont(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        tex = setupTexture(font, antiAlias, fractionalMetrics, charData);
    }

    protected DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        BufferedImage img = new BufferedImage((int) imgSize, (int) imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setFont(font);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, (int) imgSize, (int) imgSize);
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        FontMetrics fontMetrics = g.getFontMetrics();
        int charHeight = 0;
        int positionX = 0;
        int positionY = 0;

        // Lista znaków do wygenerowania: ASCII + Twoje ikony
        // Dodajemy tutaj ręcznie znaki, które są w pliku Icons.java
        String customChars = "\uf015\uf013\uf129\uf007\uf6de\uf70c\uf06e\uf57d\uf0ad\uf00c\uf00d\uf002\uf009\uf0c0";

        for (int i = 0; i < chars.length; i++) {
            // Generuj standardowe znaki (0-256) ORAZ te specjalne
            char ch = (char) i;
            if (i > 255 && customChars.indexOf(ch) == -1) {
                continue; // Pomiń znaki, które nie są ASCII i nie są naszymi ikonami
            }

            CharData charData = new CharData();
            Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g);
            charData.width = (dimensions.getBounds().width + 8);
            charData.height = dimensions.getBounds().height;

            if (positionX + charData.width >= imgSize) {
                positionX = 0;
                positionY += charHeight;
                charHeight = 0;
            }
            if (charData.height > charHeight) {
                charHeight = charData.height;
            }
            charData.storedX = positionX;
            charData.storedY = positionY;
            if (charData.height > fontHeight) {
                fontHeight = charData.height;
            }
            chars[i] = charData;
            g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
            positionX += charData.width;
        }
        return new DynamicTexture(img);
    }

    public void drawChar(CharData[] chars, char c, float x, float y) throws ArrayIndexOutOfBoundsException {
        try {
            CharData data = chars[c];
            // --- FIX: Zabezpieczenie przed brakiem znaku ---
            if (data == null) {
                return; // Jeśli znak nie został wygenerowany (np. dziwny symbol w MOTD), po prostu go nie rysuj.
            }
            // -----------------------------------------------

            drawQuad(x, y, data.width, data.height, data.storedX, data.storedY, data.width, data.height);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void drawQuad(float x, float y, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight) {
        float renderSRCX = srcX / imgSize;
        float renderSRCY = srcY / imgSize;
        float renderSRCWidth = srcWidth / imgSize;
        float renderSRCHeight = srcHeight / imgSize;
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2d(x + width, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY);
        GL11.glVertex2d(x, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2d(x + width, y);
    }

    // Metody dostępowe bez zmian
    public void setAntiAlias(boolean antiAlias) {
        if (this.antiAlias != antiAlias) {
            this.antiAlias = antiAlias;
            tex = setupTexture(font, antiAlias, fractionalMetrics, charData);
        }
    }

    public boolean isAntiAlias() { return antiAlias; }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        if (this.fractionalMetrics != fractionalMetrics) {
            this.fractionalMetrics = fractionalMetrics;
            tex = setupTexture(font, antiAlias, fractionalMetrics, charData);
        }
    }

    public boolean isFractionalMetrics() { return fractionalMetrics; }

    public void setFont(Font font) {
        this.font = font;
        tex = setupTexture(font, antiAlias, fractionalMetrics, charData);
    }

    public Font getFont() { return font; }

    protected static class CharData {
        public int width;
        public int height;
        public int storedX;
        public int storedY;
    }
}