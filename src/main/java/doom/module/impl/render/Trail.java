package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.event.impl.EventUpdate;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.ColorUtil;
import doom.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Trail extends Module {

    // --- USTAWIENIA ---
    private final ModeSetting mode = new ModeSetting("Mode", this, "Ribbon", "Ribbon", "Line");
    private final NumberSetting length = new NumberSetting("Length", this, 1000, 100, 3000, 100); // Czas w ms
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", this, true);
    private final BooleanSetting seeThrough = new BooleanSetting("SeeThrough", this, false); // Czy widać przez ściany

    // --- LOGIKA ---
    private final List<Point> points = new ArrayList<>();

    public Trail() {
        super("Trail", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(length);
        Client.INSTANCE.settingsManager.rSetting(rainbow);
        Client.INSTANCE.settingsManager.rSetting(seeThrough);
    }

    @Override
    public void onEnable() {
        points.clear();
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // Dodajemy punkt tylko, jeśli gracz się rusza (żeby nie robić kuli w jednym miejscu)
        if (mc.thePlayer.lastTickPosX != mc.thePlayer.posX || mc.thePlayer.lastTickPosY != mc.thePlayer.posY || mc.thePlayer.lastTickPosZ != mc.thePlayer.posZ) {
            points.add(new Point(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
        }

        // Usuwanie starych punktów
        long lifeTime = (long) length.getValue();
        points.removeIf(p -> System.currentTimeMillis() - p.time > lifeTime);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (points.isEmpty()) return;

        // Ustawienia OpenGL
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_CULL_FACE); // Ważne dla Ribbona
        GL11.glShadeModel(GL11.GL_SMOOTH); // Płynne przejścia kolorów

        if (seeThrough.isEnabled()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
        }

        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        // Wybieramy metodę rysowania
        if (mode.is("Ribbon")) {
            drawRibbon(viewerX, viewerY, viewerZ, event.getPartialTicks());
        } else {
            drawLine(viewerX, viewerY, viewerZ, event.getPartialTicks());
        }

        // Przywracanie OpenGL
        GlStateManager.resetColor();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        if (seeThrough.isEnabled()) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
        }

        GL11.glPopMatrix();
    }

    // --- TRYB 1: WSTĘGA (RIBBON) ---
    // Rysuje "ścianę" łączącą dół (stopy) i górę (głowa)
    private void drawRibbon(double viewX, double viewY, double viewZ, float partialTicks) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        // 1. Rysuj historię punktów
        for (Point p : points) {
            float alpha = getAlpha(p);
            int color = getColor(p, alpha);
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            GL11.glColor4f(r, g, b, alpha);

            // Punkt dolny (stopy)
            GL11.glVertex3d(p.x - viewX, p.y - viewY, p.z - viewZ);
            // Punkt górny (głowa/wysokość gracza)
            GL11.glVertex3d(p.x - viewX, p.y + mc.thePlayer.height - viewY, p.z - viewZ);
        }

        // 2. Połącz ostatni punkt z aktualną pozycją gracza (żeby nie było przerwy)
        // Interpolujemy pozycję gracza
        double currentX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double currentY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double currentZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;

        int headColor = getColor(new Point(0,0,0), 1.0f); // Pełny kolor przy graczu
        float hr = (headColor >> 16 & 0xFF) / 255.0F;
        float hg = (headColor >> 8 & 0xFF) / 255.0F;
        float hb = (headColor & 0xFF) / 255.0F;

        GL11.glColor4f(hr, hg, hb, 0.7f); // Lekko przezroczyste przy samym graczu
        GL11.glVertex3d(currentX - viewX, currentY - viewY, currentZ - viewZ);
        GL11.glVertex3d(currentX - viewX, currentY + mc.thePlayer.height - viewY, currentZ - viewZ);

        GL11.glEnd();
    }

    // --- TRYB 2: LINIA (LINE) ---
    // Rysuje linię przy stopach
    private void drawLine(double viewX, double viewY, double viewZ, float partialTicks) {
        GL11.glLineWidth(3.0f); // Grubość linii
        GL11.glBegin(GL11.GL_LINE_STRIP);

        for (Point p : points) {
            float alpha = getAlpha(p);
            int color = getColor(p, alpha);
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            GL11.glColor4f(r, g, b, alpha);
            GL11.glVertex3d(p.x - viewX, p.y + 0.1 - viewY, p.z - viewZ); // +0.1 żeby nie migało w podłodze
        }

        // Połączenie z graczem
        double currentX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double currentY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double currentZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;

        int headColor = getColor(new Point(0,0,0), 1.0f);
        float hr = (headColor >> 16 & 0xFF) / 255.0F;
        float hg = (headColor >> 8 & 0xFF) / 255.0F;
        float hb = (headColor & 0xFF) / 255.0F;

        GL11.glColor4f(hr, hg, hb, 1.0f);
        GL11.glVertex3d(currentX - viewX, currentY + 0.1 - viewY, currentZ - viewZ);

        GL11.glEnd();
    }

    // --- POMOCNICY ---

    private float getAlpha(Point p) {
        long age = System.currentTimeMillis() - p.time;
        long maxAge = (long) length.getValue();
        float alpha = 1.0f - ((float) age / maxAge);
        return Math.max(0, alpha);
    }

    private int getColor(Point p, float alpha) {
        if (rainbow.isEnabled()) {
            // Kolor zależny od czasu utworzenia punktu (tworzy ładny gradient wzdłuż linii)
            return ColorUtil.getRainbow(4.0f, 0.7f, 1.0f, p.time);
        } else {
            // Czerwony (Doom Theme)
            return new Color(220, 20, 20).getRGB();
        }
    }

    private static class Point {
        public double x, y, z;
        public long time;

        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = System.currentTimeMillis();
        }
    }
}