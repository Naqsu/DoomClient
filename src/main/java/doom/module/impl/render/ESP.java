package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Category;
import doom.module.Module;
import doom.module.impl.combat.AntiBot;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.util.RenderUtil;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class ESP extends Module {

    public ModeSetting mode;
    public BooleanSetting players;
    public BooleanSetting mobs;
    public BooleanSetting animals;

    public ESP() {
        super("ESP", 0, Category.RENDER);

        mode = new ModeSetting("Mode", this, "3D", "3D", "2D");
        players = new BooleanSetting("Targets: Players", this, true);
        mobs = new BooleanSetting("Targets: Mobs", this, false);
        animals = new BooleanSetting("Targets: Animals", this, false);

        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(players);
        Client.INSTANCE.settingsManager.rSetting(mobs);
        Client.INSTANCE.settingsManager.rSetting(animals);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (shouldRender(entity)) {

                // Ustalanie koloru
                Color color = getColor((EntityLivingBase) entity);

                if (mode.is("3D")) {
                    render3D((EntityLivingBase) entity, color, event.getPartialTicks());
                } else {
                    render2D((EntityLivingBase) entity, color, event.getPartialTicks());
                }
            }
        }
    }

    // --- RENDEROWANIE 3D (ŁADNE PUDEŁKA) ---
    private void render3D(EntityLivingBase entity, Color color, float partialTicks) {
        RenderManager rm = mc.getRenderManager();

        double viewerX = rm.viewerPosX;
        double viewerY = rm.viewerPosY;
        double viewerZ = rm.viewerPosZ;

        // 1. Obliczamy pozycję gracza z interpolacją (żeby było płynnie)
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - viewerX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - viewerY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - viewerZ;

        // 2. Tworzymy pudełko (BoundingBox)
        // Wymiary bierzemy z encji (width/height), a pozycję z obliczeń wyżej.
        // Dodajemy mały margines (0.1), żeby ramka nie dotykała modelu gracza.
        double width = entity.width / 2 + 0.1;
        double height = entity.height + 0.1; // +0.1 nad głową

        AxisAlignedBB bb = new AxisAlignedBB(
                x - width, y, z - width,
                x + width, y + height, z + width
        );

        // 3. Rysowanie WYPEŁNIENIA (Fill)
        // Alpha 50/255 -> bardzo przezroczyste
        int fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 40).getRGB();
        RenderUtil.drawFilledBox(bb, fillColor);

        // 4. Rysowanie OBRYSU (Outline)
        // Alpha 255/255 -> pełna widoczność
        int outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255).getRGB();
        RenderUtil.drawBoundingBox(bb, 1.5f, outlineColor);
    }

    // --- RENDEROWANIE 2D (STARY STYL) ---
    private void render2D(EntityLivingBase entity, Color color, float partialTicks) {
        RenderManager rm = mc.getRenderManager();
        double viewerX = rm.viewerPosX;
        double viewerY = rm.viewerPosY;
        double viewerZ = rm.viewerPosZ;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - viewerX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - viewerY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - viewerZ;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glRotated(-rm.playerViewY, 0.0, 1.0, 0.0);

        // Ustawienia GL
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        GL11.glColor4f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, 1f);

        double width = entity.width / 1.5;
        double height = entity.height + (entity.isSneaking() ? -0.3 : 0.2);

        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(-width, 0);
        GL11.glVertex2d(-width, height);
        GL11.glVertex2d(width, height);
        GL11.glVertex2d(width, 0);
        GL11.glEnd();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private Color getColor(EntityLivingBase entity) {
        if (entity.hurtTime > 0) return Color.RED; // Jak dostaje dmg to czerwony
        if (entity.isOnSameTeam(mc.thePlayer)) return Color.GREEN;
        if (entity.isInvisible()) return Color.YELLOW;

        // Kolor zależny od dystansu (im bliżej tym bardziej czerwony) - opcjonalne
        // return Color.WHITE;
        return new Color(255, 255, 255);
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.thePlayer) return false;
        if (entity.isDead) return false;
        if (!(entity instanceof EntityLivingBase)) return false;

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