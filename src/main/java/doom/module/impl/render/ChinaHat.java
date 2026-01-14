package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.ColorUtil;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ChinaHat extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", "Normal", "Flat");
    private final NumberSetting radius = new NumberSetting("Radius", this, 0.7, 0.4, 1.5, 0.1);
    private final NumberSetting height = new NumberSetting("Height", this, 0.4, 0.1, 1.0, 0.1);
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", this, true);
    private final BooleanSetting outline = new BooleanSetting("Outline", this, true);

    public ChinaHat() {
        super("ChinaHat", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(radius);
        Client.INSTANCE.settingsManager.rSetting(height);
        Client.INSTANCE.settingsManager.rSetting(rainbow);
        Client.INSTANCE.settingsManager.rSetting(outline);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.gameSettings.thirdPersonView == 0) return; // Nie rysuj w pierwszej osobie

        // Interpolacja pozycji gracza
        double ix = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.getPartialTicks();
        double iy = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.getPartialTicks();
        double iz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.getPartialTicks();

        double x = ix - mc.getRenderManager().viewerPosX;
        double y = iy - mc.getRenderManager().viewerPosY + mc.thePlayer.height + 0.1; // +0.1 nad głową
        double z = iz - mc.getRenderManager().viewerPosZ;

        // Jeśli kucamy, kapelusz musi być niżej
        if (mc.thePlayer.isSneaking()) {
            y -= 0.2;
        }

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        // Kapelusz musi się obracać razem z głową gracza? Nie, zazwyczaj jest statyczny lub obraca się z ciałem.
        // Tutaj ustawiamy obrót zgodny z yaw gracza (interpolowany)
        float yaw = mc.thePlayer.prevRotationYaw + (mc.thePlayer.rotationYaw - mc.thePlayer.prevRotationYaw) * event.getPartialTicks();
        // Pitch (pochylenie głowy)
        float pitch = mc.thePlayer.prevRotationPitch + (mc.thePlayer.rotationPitch - mc.thePlayer.prevRotationPitch) * event.getPartialTicks();

        // Poprawka: Chcemy, żeby kapelusz podążał za pochyleniem głowy
        GL11.glRotatef(-yaw, 0, 1, 0);
        GL11.glRotatef(pitch, 1, 0, 0);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST); // Widać przez ściany
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        // Rysowanie stożka
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        // Czubek kapelusza
        Color topC = rainbow.isEnabled()
                ? new Color(ColorUtil.getRainbow(4.0f, 0.6f, 1.0f, 0))
                : new Color(220, 20, 20);

        GL11.glColor4f(topC.getRed()/255f, topC.getGreen()/255f, topC.getBlue()/255f, 0.4f);
        GL11.glVertex3d(0, height.getValue(), 0); // Czubek jest wyżej

        // Podstawa kapelusza (koło)
        double r = radius.getValue();
        for (int i = 0; i <= 360; i += 5) {
            double rad = Math.toRadians(i);

            Color baseC = rainbow.isEnabled()
                    ? new Color(ColorUtil.getRainbow(4.0f, 0.6f, 1.0f, i * 2L))
                    : new Color(220, 20, 20);

            GL11.glColor4f(baseC.getRed()/255f, baseC.getGreen()/255f, baseC.getBlue()/255f, 0.4f);

            // Jeśli tryb Flat, y podstawy = 0. Jeśli Normal, podstawa jest lekko wygięta?
            // Zostawmy płaską podstawę dla prostoty
            GL11.glVertex3d(Math.cos(rad) * r, 0, Math.sin(rad) * r);
        }
        GL11.glEnd();

        // Obrys (Outline)
        if (outline.isEnabled()) {
            GL11.glLineWidth(2.0f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i <= 360; i += 5) {
                double rad = Math.toRadians(i);
                Color outC = rainbow.isEnabled()
                        ? new Color(ColorUtil.getRainbow(4.0f, 0.6f, 1.0f, i * 2L))
                        : new Color(220, 20, 20);

                GL11.glColor4f(outC.getRed()/255f, outC.getGreen()/255f, outC.getBlue()/255f, 1.0f);
                GL11.glVertex3d(Math.cos(rad) * r, 0, Math.sin(rad) * r);
            }
            GL11.glEnd();
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopMatrix();
    }
}