package doom.ui;

import doom.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class ParticleEngine {
    public static ArrayList<Particle> particles = new ArrayList<>();
    private static final Random random = new Random();

    // Zapamiętujemy ostatni rozmiar ekranu
    private static int lastWidth = 0;
    private static int lastHeight = 0;

    public static void render(int width, int height, int mouseX, int mouseY) {

        // Reset przy zmianie rozmiaru okna
        if (width != lastWidth || height != lastHeight) {
            particles.clear();
            lastWidth = width;
            lastHeight = height;
        }

        if (particles.isEmpty()) {
            for (int i = 0; i < 100; i++) {
                particles.add(new Particle(width, height));
            }
        }

        // --- ZMIANA: TŁO GRADIENTOWE ZAMIAST CZARNEGO ---
        // Rysujemy gradient od Ciemnoszarego (góra) do Czarnego (dół)
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425); // Model cieniowania dla gradientów

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        // Kolory: Góra (Top) = lekki szary (#232323), Dół (Bottom) = czarny (#000000)
        Color topColor = new Color(35, 35, 35, 255);
        Color bottomColor = new Color(0, 0, 0, 255);

        worldrenderer.pos(width, 0, 0).color(topColor.getRed(), topColor.getGreen(), topColor.getBlue(), 255).endVertex();
        worldrenderer.pos(0, 0, 0).color(topColor.getRed(), topColor.getGreen(), topColor.getBlue(), 255).endVertex();
        worldrenderer.pos(0, height, 0).color(bottomColor.getRed(), bottomColor.getGreen(), bottomColor.getBlue(), 255).endVertex();
        worldrenderer.pos(width, height, 0).color(bottomColor.getRed(), bottomColor.getGreen(), bottomColor.getBlue(), 255).endVertex();

        tessellator.draw();

        GlStateManager.shadeModel(7424); // Reset modelu cieniowania
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        // ------------------------------------------------

        // Dalej ten sam kod rysowania cząsteczek...
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        for (Particle p : particles) {
            // ... (Reszta kodu cząsteczek bez zmian) ...
            p.update(width, height);

            // KROPKI
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2d(p.x, p.y + 2);
            GL11.glVertex2d(p.x + 2, p.y + 2);
            GL11.glVertex2d(p.x + 2, p.y);
            GL11.glVertex2d(p.x, p.y);
            GL11.glEnd();

            // LINIE
            for (Particle p2 : particles) {
                float distance = (float) Math.sqrt(Math.pow(p.x - p2.x, 2) + Math.pow(p.y - p2.y, 2));
                if (distance < 100) {
                    float alpha = 1.0f - (distance / 100.0f);
                    GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
                    GL11.glLineWidth(0.5f);
                    GL11.glBegin(GL11.GL_LINES);
                    GL11.glVertex2d(p.x + 1, p.y + 1);
                    GL11.glVertex2d(p2.x + 1, p2.y + 1);
                    GL11.glEnd();
                }
            }
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static class Particle {
        public float x, y, velocityX, velocityY;

        public Particle(int width, int height) {
            this.x = random.nextInt(width);
            this.y = random.nextInt(height);
            this.velocityX = (random.nextFloat() - 0.5f) * 1.5f;
            this.velocityY = (random.nextFloat() - 0.5f) * 1.5f;
        }

        public void update(int width, int height) {
            x += velocityX;
            y += velocityY;
            if (x > width || x < 0) velocityX *= -1;
            if (y > height || y < 0) velocityY *= -1;
        }
    }
}