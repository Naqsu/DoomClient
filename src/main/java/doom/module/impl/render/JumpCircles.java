package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.ColorUtil;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JumpCircles extends Module {

    // --- USTAWIENIA ---
    private final ModeSetting mode = new ModeSetting("Mode", this, "Disc", "Disc", "Outline", "Bloom");
    private final NumberSetting maxRadius = new NumberSetting("Radius", this, 2.5, 1.0, 5.0, 0.1);
    private final NumberSetting speed = new NumberSetting("Speed", this, 0.05, 0.01, 0.2, 0.01); // Szybkość rozszerzania
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", this, true);

    // --- LOGIKA ---
    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround; // Do wykrywania momentu skoku

    public JumpCircles() {
        super("JumpCircles", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(maxRadius);
        Client.INSTANCE.settingsManager.rSetting(speed);
        Client.INSTANCE.settingsManager.rSetting(rainbow);
    }

    @Override
    public void onEnable() {
        circles.clear();
        wasOnGround = mc.thePlayer != null && mc.thePlayer.onGround;
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // DETEKCJA SKOKU
        // Jeśli w poprzedniej klatce był na ziemi, a teraz nie jest i ma ruch w górę -> SKOK
        if (wasOnGround && !mc.thePlayer.onGround && mc.thePlayer.motionY > 0) {
            circles.add(new Circle(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
        }

        wasOnGround = mc.thePlayer.onGround;

        // Aktualizacja animacji (rozszerzanie się)
        // Używamy Iteratora, żeby bezpiecznie usuwać elementy podczas pętli
        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) {
            Circle c = iterator.next();
            c.update((float) speed.getValue());

            // Jeśli animacja się skończyła (wiek > 1.0), usuwamy kółko
            if (c.age >= 1.0f) {
                iterator.remove();
            }
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        // Ustawienia OpenGL dla wszystkich kółek
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE); // Żeby było widać od spodu
        GL11.glDisable(GL11.GL_DEPTH_TEST); // Żeby widzieć przez bloki (opcjonalne)
        GL11.glDepthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH); // Gładkie przejścia kolorów

        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        for (Circle c : circles) {
            double x = c.x - viewerX;
            double y = c.y - viewerY;
            double z = c.z - viewerZ;

            // Obliczamy kolor (zanikanie alpha wraz z wiekiem)
            float alpha = 1.0f - c.age; // Od 1.0 do 0.0

            // Pobieramy kolor (Tęcza lub Czerwony Doom)
            Color baseColor;
            if (rainbow.isEnabled()) {
                baseColor = new Color(ColorUtil.getRainbow(4.0f, 0.7f, 1.0f, (long) (c.age * 1000)));
            } else {
                baseColor = new Color(220, 20, 20); // Czerwony
            }

            // Rysowanie
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);

            // Obrót, żeby kółko leżało płasko na ziemi
            GL11.glRotatef(90, 1, 0, 0);

            float radius = (float) (c.age * maxRadius.getValue());

            switch (mode.getMode()) {
                case "Disc":
                    drawFilledCircle(radius, baseColor, alpha);
                    break;
                case "Outline":
                    drawOutlineCircle(radius, baseColor, alpha, 2.0f);
                    break;
                case "Bloom":
                    // Dysk z gradientem (środek przezroczysty, brzegi kolorowe)
                    drawBloomCircle(radius, baseColor, alpha);
                    break;
            }

            GL11.glPopMatrix();
        }

        // Przywracanie OpenGL
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GlStateManager.resetColor();
    }

    // --- METODY RYSUJĄCE ---

    private void drawFilledCircle(float radius, Color c, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        // Środek (trochę bardziej przezroczysty)
        GL11.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, alpha * 0.5f);
        GL11.glVertex2d(0, 0);

        // Krawędzie
        GL11.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, alpha);
        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(Math.cos(angle) * radius, Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private void drawOutlineCircle(float radius, Color c, float alpha, float lineWidth) {
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, alpha);
        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(Math.cos(angle) * radius, Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    private void drawBloomCircle(float radius, Color c, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        // Środek (Całkowicie przezroczysty - efekt pierścienia rozchodzącego się do środka)
        GL11.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, 0.0f);
        GL11.glVertex2d(0, 0);

        // Krawędzie (Pełny kolor)
        GL11.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, alpha);
        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(Math.cos(angle) * radius, Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    // --- KLASA POMOCNICZA ---
    private static class Circle {
        double x, y, z;
        float age; // Od 0.0 (start) do 1.0 (koniec)

        public Circle(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.age = 0;
        }

        public void update(float speed) {
            this.age += speed;
        }
    }
}