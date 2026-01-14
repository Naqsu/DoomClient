package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Module;
import doom.module.impl.combat.AntiBot;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.util.ColorUtil;
import doom.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Box", "Box", "2D", "Outline");
    private final BooleanSetting players = new BooleanSetting("Targets: Players", this, true);
    private final BooleanSetting mobs = new BooleanSetting("Targets: Mobs", this, false);
    private final BooleanSetting animals = new BooleanSetting("Targets: Animals", this, false);
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", this, true);
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", this, false);

    public ESP() {
        super("ESP", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(players);
        Client.INSTANCE.settingsManager.rSetting(mobs);
        Client.INSTANCE.settingsManager.rSetting(animals);
        Client.INSTANCE.settingsManager.rSetting(healthBar);
        Client.INSTANCE.settingsManager.rSetting(rainbow);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (shouldRender(entity)) {
                EntityLivingBase living = (EntityLivingBase) entity;
                Color color = getColor(living);

                if (mode.is("Box")) {
                    renderBox(living, color, event.getPartialTicks());
                } else if (mode.is("2D")) {
                    render2D(living, color, event.getPartialTicks());
                }

                // Outline to osobny shader (zrobimy go później, teraz Box jest priorytetem)
            }
        }
    }

    private void renderBox(EntityLivingBase entity, Color color, float partialTicks) {
        RenderManager rm = mc.getRenderManager();

        // 1. Interpolacja Pozycji (Klucz do gładkiego ESP)
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.viewerPosZ;

        // Wymiary pudełka (dopasowane do hitboxa)
        double width = entity.width / 2.0 + 0.1;
        double height = entity.height + 0.1; // Lekki margines nad głową

        // Jeśli kucamy, hitbox jest niższy, ale renderowanie modelu w 1.8.9 jest specyficzne.
        // Bierzemy to pod uwagę.
        if (entity.isSneaking()) {
            y -= 0.125; // Korekta dla sneaka
        }

        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                x - width, y, z - width,
                x + width, y + height, z + width
        );

        // 2. Setup OpenGL
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST); // Żeby widzieć przez ściany
        GL11.glDepthMask(false);
        GL11.glLineWidth(1.5f); // Grubość linii

        // 3. Rysowanie WYPEŁNIENIA (Fill)
        // Alpha 40/255 (bardzo delikatne)
        RenderUtil.drawFilledBox(axisAlignedBB, new Color(color.getRed(), color.getGreen(), color.getBlue(), 40).getRGB());

        // 4. Rysowanie OBRYSU (Outline)
        // Pełny kolor, ale bez górnej i dolnej "klapy", żeby wyglądało czyściej (tzw. Corner Box lub Full Box)
        // Tutaj rysujemy pełny box wireframe
        RenderUtil.drawBoundingBox(axisAlignedBB, 1.5f, new Color(color.getRed(), color.getGreen(), color.getBlue(), 255).getRGB());

        // 5. HEALTH BAR (Opcjonalny)
        if (healthBar.isEnabled()) {
            drawHealthBar(entity, x, y, z, width, height);
        }

        // Cleanup
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GlStateManager.resetColor();
    }

    private void drawHealthBar(EntityLivingBase entity, double x, double y, double z, double width, double height) {
        float hp = entity.getHealth();
        float maxHp = entity.getMaxHealth();
        float percentage = hp / maxHp;

        // Kolor paska (Zielony -> Czerwony)
        Color hpColor = Color.getHSBColor(percentage * 0.33f, 1.0f, 1.0f);

        // Pozycja paska (po lewej lub prawej stronie boxa)
        // Rysujemy po prawej
        double barX = x - width - 0.2; // Odsunięcie od boxa
        double barZ = z - width; // Wyrównanie do krawędzi

        // W 3D paski są trudne, bo muszą się obracać do gracza.
        // Najlepiej rysować je jako linię 3D wzdłuż krawędzi boxa.

        GL11.glLineWidth(2.0f);

        // Tło paska (Szare)
        GL11.glColor4f(0.2f, 0.2f, 0.2f, 0.5f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x - width - 0.1, y, z);
        GL11.glVertex3d(x - width - 0.1, y + height, z);
        GL11.glEnd();

        // Pasek HP (Kolorowy)
        GL11.glColor4f(hpColor.getRed()/255f, hpColor.getGreen()/255f, hpColor.getBlue()/255f, 1.0f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x - width - 0.1, y, z);
        // Wysokość paska zależna od HP
        GL11.glVertex3d(x - width - 0.1, y + (height * percentage), z);
        GL11.glEnd();
    }

    // --- STARY DOBRY 2D ESP (POPRAWIONY) ---
    private void render2D(EntityLivingBase entity, Color color, float partialTicks) {
        RenderManager rm = mc.getRenderManager();

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.viewerPosZ;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        // Obracamy billboard do gracza (żeby zawsze był przodem)
        GL11.glRotated(-rm.playerViewY, 0.0, 1.0, 0.0);

        // Skalowanie w zależności od dystansu (żeby ramka była stałej wielkości na ekranie lub dopasowana do gracza)
        // Tutaj robimy dopasowaną do gracza (w świecie 3D)

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        double w = entity.width / 1.5;
        double h = entity.height + (entity.isSneaking() ? -0.2 : 0.1);

        // Obrys (Czarny podkład)
        GL11.glLineWidth(3.5f);
        GL11.glColor4f(0, 0, 0, 0.5f);
        drawRect2D(-w, 0, w, h);

        // Obrys (Kolorowy wierzch)
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, 1.0f);
        drawRect2D(-w, 0, w, h);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void drawRect2D(double x1, double y1, double x2, double y2) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(x1, y1);
        GL11.glVertex2d(x1, y2);
        GL11.glVertex2d(x2, y2);
        GL11.glVertex2d(x2, y1);
        GL11.glEnd();
    }

    private Color getColor(EntityLivingBase entity) {
        if (rainbow.isEnabled()) {
            return new Color(ColorUtil.getRainbow(4.0f, 0.7f, 1.0f, entity.getEntityId() * 100L));
        }
        if (entity.hurtTime > 0) return new Color(255, 0, 0, 255);
        if (entity instanceof EntityPlayer) return new Color(255, 255, 255);
        if (entity instanceof EntityMob) return new Color(255, 100, 100);
        if (entity instanceof EntityAnimal) return new Color(100, 255, 100);
        return Color.WHITE;
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.thePlayer) return false;
        if (entity.isDead) return false;
        if (!(entity instanceof EntityLivingBase)) return false;

        // AntiBot Check
        if (entity instanceof EntityPlayer) {
            AntiBot ab = Client.INSTANCE.moduleManager.getModule(AntiBot.class);
            if (ab != null && ab.isToggled() && ab.isBot((EntityPlayer) entity)) return false;
        }

        if (entity instanceof EntityPlayer && players.isEnabled()) return true;
        if (entity instanceof EntityMob && mobs.isEnabled()) return true;
        if (entity instanceof EntityAnimal && animals.isEnabled()) return true;

        return false;
    }
}