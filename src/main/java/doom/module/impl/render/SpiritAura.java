package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Module;
import doom.module.impl.combat.Killaura;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.ColorUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SpiritAura extends Module {

    // Ustawienia
    private final NumberSetting speed = new NumberSetting("Speed", this, 3.0, 0.5, 10.0, 0.5);
    private final NumberSetting radius = new NumberSetting("Radius", this, 1.2, 0.5, 3.0, 0.1);
    private final NumberSetting height = new NumberSetting("Height", this, 1.5, 0.5, 3.0, 0.1);
    private final NumberSetting trailLength = new NumberSetting("Trail Length", this, 20, 5, 50, 1); // Długość ogona
    private final BooleanSetting teamColor = new BooleanSetting("Team Color", this, true);

    // Historia pozycji dla 2 duszków (lista list)
    private final List<List<WispPoint>> trails = new ArrayList<>();

    public SpiritAura() {
        super("SpiritAura", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(speed);
        Client.INSTANCE.settingsManager.rSetting(radius);
        Client.INSTANCE.settingsManager.rSetting(height);
        Client.INSTANCE.settingsManager.rSetting(trailLength);
        Client.INSTANCE.settingsManager.rSetting(teamColor);

        // Inicjalizacja list dla 2 duszków
        trails.add(new ArrayList<>());
        trails.add(new ArrayList<>());
    }

    @Override
    public void onDisable() {
        // Czyszczenie szlaków po wyłączeniu
        for (List<WispPoint> trail : trails) {
            trail.clear();
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        EntityLivingBase target = null;
        Killaura killaura = (Killaura) Client.INSTANCE.moduleManager.getModule(Killaura.class);

        if (killaura != null) {
            target = killaura.target;
        }

        // Jeśli nie ma celu, czyścimy szlaki i wychodzimy
        if (target == null || target.isDead || target.getHealth() <= 0) {
            for (List<WispPoint> trail : trails) {
                if (!trail.isEmpty()) trail.clear();
            }
            return;
        }

        // Zabezpieczenie GL
        GL11.glPushMatrix();
        try {
            // Interpolowana pozycja celu
            double interpX = target.lastTickPosX + (target.posX - target.lastTickPosX) * event.getPartialTicks();
            double interpY = target.lastTickPosY + (target.posY - target.lastTickPosY) * event.getPartialTicks();
            double interpZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * event.getPartialTicks();

            // Setup GL dla efektów
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST); // Widać przez ściany
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glShadeModel(GL11.GL_SMOOTH); // Gładkie przejścia kolorów

            // Pętla dla 2 duszków
            for (int i = 0; i < 2; i++) {
                long time = System.currentTimeMillis();
                double offset = (i * Math.PI); // Przesunięcie o 180 stopni dla drugiego duszka

                // Matematyka ruchu
                double angle = (time * speed.getValue() / 1000.0) + offset;
                double yOffset = Math.sin((time * speed.getValue() / 1000.0) + offset) * (height.getValue() / 2.0) + (target.height / 2.0);

                // Aktualna pozycja duszka (ABSOLUTNA w świecie)
                double currentX = interpX + Math.cos(angle) * radius.getValue();
                double currentY = interpY + yOffset;
                double currentZ = interpZ + Math.sin(angle) * radius.getValue();

                Color color = getColor(target, i);

                // --- ZARZĄDZANIE SZLAKIEM (OGONEM) ---
                List<WispPoint> trail = trails.get(i);
                // Dodajemy aktualną pozycję na początek listy
                trail.add(0, new WispPoint(currentX, currentY, currentZ));
                // Usuwamy stare punkty, jeśli jest ich za dużo
                while (trail.size() > trailLength.getValue()) {
                    trail.remove(trail.size() - 1);
                }

                // Koordynaty kamery
                double viewerX = mc.getRenderManager().viewerPosX;
                double viewerY = mc.getRenderManager().viewerPosY;
                double viewerZ = mc.getRenderManager().viewerPosZ;

                // 1. RYSOWANIE OGONA (Trail)
                if (trail.size() > 1) {
                    drawTrail(trail, color, viewerX, viewerY, viewerZ);
                }

                // 2. RYSOWANIE GŁOWY (Sfera 3D) w najnowszej pozycji
                // Konwertujemy na pozycję relatywną do kamery
                drawHead(currentX - viewerX, currentY - viewerY, currentZ - viewerZ, color);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // === CLEANUP ===
            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);
            GlStateManager.resetColor();
            GL11.glLineWidth(1.0f); // Reset grubości linii
            GL11.glPopMatrix();
        }
    }

    // Rysowanie ogona (szlaku)
    private void drawTrail(List<WispPoint> trail, Color color, double vX, double vY, double vZ) {
        GL11.glLineWidth(3.0f); // Grubość ogona
        GL11.glBegin(GL11.GL_LINE_STRIP); // Używamy LINE_STRIP do łączenia punktów

        for (int i = 0; i < trail.size(); i++) {
            WispPoint point = trail.get(i);

            // Obliczanie zanikania (alpha)
            // Im dalszy indeks (starszy punkt), tym mniejsza alpha.
            float alphaProgress = (float) i / (float) trail.size(); // 0.0 (nowy) -> 1.0 (stary)
            float alpha = 1.0f - alphaProgress; // 1.0 (nowy) -> 0.0 (stary)

            // Można też dodać zanikanie grubości, ale GL_LINE_STRIP ma stałą grubość w jednym wywołaniu.
            // Alpha załatwia sprawę "animacji".

            GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha);
            GL11.glVertex3d(point.x - vX, point.y - vY, point.z - vZ);
        }

        GL11.glEnd();
    }

    // Rysowanie głowy (Sfera 3D - tak jak poprzednio)
    private void drawHead(double x, double y, double z, Color color) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        // Obroty dla efektu 3D
        GL11.glRotatef((System.currentTimeMillis() % 2000) / 5f, 0, 1, 0);
        GL11.glRotatef((System.currentTimeMillis() % 1500) / 5f, 1, 0, 0);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        // Wypełnienie
        GL11.glColor4f(r, g, b, 0.7f); // Większa alpha dla głowy
        drawSphereShape(0.2f, 8, 8); // Mniejsza sfera

        // Obrys (Wireframe)
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(r, g, b, 1.0f);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        drawSphereShape(0.22f, 8, 8);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

        GL11.glPopMatrix();
    }

    // Helper do rysowania sfery
    private void drawSphereShape(float radius, int slices, int stacks) {
        // (Kod identyczny jak w poprzedniej wersji - matematyka sfery)
        for (int i = 0; i < slices; i++) {
            float lat0 = (float) (Math.PI * (-0.5 + (float) (i - 1) / slices));
            float z0 = (float) Math.sin(lat0) * radius;
            float zr0 = (float) Math.cos(lat0) * radius;

            float lat1 = (float) (Math.PI * (-0.5 + (float) i / slices));
            float z1 = (float) Math.sin(lat1) * radius;
            float zr1 = (float) Math.cos(lat1) * radius;

            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int j = 0; j <= stacks; j++) {
                float lng = (float) (2 * Math.PI * (float) (j - 1) / stacks);
                float x = (float) Math.cos(lng);
                float y = (float) Math.sin(lng);

                GL11.glNormal3f(x * zr0, y * zr0, z0);
                GL11.glVertex3f(x * zr0, y * zr0, z0);
                GL11.glNormal3f(x * zr1, y * zr1, z1);
                GL11.glVertex3f(x * zr1, y * zr1, z1);
            }
            GL11.glEnd();
        }
    }

    // --- Kolory i Helpery ---

    private Color getColor(EntityLivingBase target, int offsetIndex) {
        if (teamColor.isEnabled() && target instanceof EntityPlayer) {
            return getTeamColor((EntityPlayer) target);
        } else {
            return new Color(ColorUtil.getRainbow(4.0f, 0.8f, 1.0f, offsetIndex * 1000L));
        }
    }

    private Color getTeamColor(EntityPlayer player) {
        try {
            String displayName = player.getDisplayName().getFormattedText();
            for (int i = 0; i < displayName.length() - 1; i++) {
                if (displayName.charAt(i) == '§') {
                    int colorCode = getColorCodeSafe(displayName.charAt(i + 1));
                    if (colorCode != -1) return new Color(colorCode);
                }
            }
        } catch (Exception ignored) {}
        return new Color(ColorUtil.getRainbow(4.0f, 0.8f, 1.0f, 0));
    }

    private int getColorCodeSafe(char code) {
        switch (code) {
            case '0': return 0x000000; case '1': return 0x0000AA; case '2': return 0x00AA00; case '3': return 0x00AAAA;
            case '4': return 0xAA0000; case '5': return 0xAA00AA; case '6': return 0xFFAA00; case '7': return 0xAAAAAA;
            case '8': return 0x555555; case '9': return 0x5555FF; case 'a': return 0x55FF55; case 'b': return 0x55FFFF;
            case 'c': return 0xFF5555; case 'd': return 0xFF55FF; case 'e': return 0xFFFF55; case 'f': return 0xFFFFFF;
            default: return -1;
        }
    }

    // --- KLASA POMOCNICZA DO PRZECHOWYWANIA HISTORII POZYCJI ---
    private static class WispPoint {
        double x, y, z;
        public WispPoint(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }
}