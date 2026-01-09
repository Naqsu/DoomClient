package doom.module.impl.render;

import doom.Client;
import doom.module.Category;
import doom.module.DraggableModule;
import doom.module.impl.combat.Killaura;
import doom.util.RenderUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.text.DecimalFormat;

public class TargetHUD extends DraggableModule {

    private double hpWidth = 0;

    public TargetHUD() {
        super("TargetHUD", Category.RENDER);
        this.setToggled(true);
    }

    @Override
    public float getWidth() {
        return 140;
    }

    @Override
    public float getHeight() {
        return 45;
    }

    @Override
    public void render(float x, float y) {
        // --- OBSŁUGA EDYTORA HUD (DUMMY) ---
        if (mc.currentScreen instanceof doom.ui.hudeditor.GuiHudEditor) {
            renderDummy(x, y);
            return;
        }

        EntityLivingBase target = null;
        Killaura killaura = (Killaura) Client.INSTANCE.moduleManager.getModule(Killaura.class);

        if (killaura != null) {
            target = killaura.target;
        }

        if (target != null && !target.isDead && target.getHealth() > 0) {
            drawTarget(x, y, target);
        }
    }

    // --- GŁÓWNA METODA RYSOWANIA ---
    private void drawTarget(float x, float y, EntityLivingBase target) {
        // 1. TŁO
        RenderUtil.drawRoundedRect(x, y, getWidth(), getHeight(), 6, new Color(20, 20, 20, 200).getRGB());

        // 2. GŁOWA 2D (PIXEL ART)
        // Resetujemy kolory, żeby głowa nie była np. czerwona od bicia
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        boolean renderedFace = false;

        // A. JEŚLI TO GRACZ -> POBIERZ JEGO SKINA
        if (target instanceof AbstractClientPlayer) {
            mc.getTextureManager().bindTexture(((AbstractClientPlayer) target).getLocationSkin());
            renderedFace = true;
        }
        // B. JEŚLI TO ZOMBIE (Tak jak chciałeś na obrazku)
        else if (target instanceof EntityZombie) {
            mc.getTextureManager().bindTexture(new ResourceLocation("textures/entity/zombie/zombie.png"));
            renderedFace = true;
        }
        // C. JEŚLI TO SZKIELET
        else if (target instanceof EntitySkeleton) {
            mc.getTextureManager().bindTexture(new ResourceLocation("textures/entity/skeleton/skeleton.png"));
            renderedFace = true;
        }
        // D. JEŚLI TO PIGMAN
        else if (target instanceof EntityPigZombie) {
            mc.getTextureManager().bindTexture(new ResourceLocation("textures/entity/zombie_pigman.png"));
            renderedFace = true;
        }

        if (renderedFace) {
            // Rysujemy wycinek tekstury (Twarz jest na koordynatach 8,8 i ma rozmiar 8x8 pikseli)
            // Powiększamy to do rozmiaru 32x32 na ekranie
            Gui.drawScaledCustomSizeModalRect((int)x + 6, (int)y + 6, 8.0F, 8.0F, 8, 8, 32, 32, 64.0F, 64.0F);

            // Opcjonalnie: Warstwa zewnętrzna (np. hełm/włosy)
            if (target instanceof AbstractClientPlayer) {
                Gui.drawScaledCustomSizeModalRect((int)x + 6, (int)y + 6, 40.0F, 8.0F, 8, 8, 32, 32, 64.0F, 64.0F);
            }
        } else {
            // FALLBACK: Dla Pająków, Koni itp. rysujemy stary model 3D,
            // bo one nie mają kwadratowych głów w tym samym miejscu tekstury.
            GuiInventory.drawEntityOnScreen((int)(x + 22), (int)(y + 40), 18, target.rotationYaw, target.rotationPitch, target);
        }

        // 3. NAZWA
        mc.fontRendererObj.drawStringWithShadow(target.getName(), x + 45, y + 6, -1);

        // 4. HP (Liczbowo)
        DecimalFormat df = new DecimalFormat("##.#");
        String hpText = df.format(target.getHealth()) + " HP";
        mc.fontRendererObj.drawStringWithShadow(hpText, x + 45, y + 18, new Color(200, 200, 200).getRGB());

        // 5. PASEK ŻYCIA (HealthBar)
        double healthPercent = Math.min(target.getHealth() / target.getMaxHealth(), 1.0);
        double maxBarWidth = getWidth() - 50;

        // Animacja paska
        hpWidth = RenderUtil.lerp(hpWidth, maxBarWidth * healthPercent, 0.1);

        // Tło paska
        RenderUtil.drawRoundedRect(x + 45, y + 32, (float)maxBarWidth, 6, 2, new Color(40, 40, 40).getRGB());
        // Pasek właściwy
        RenderUtil.drawRoundedRect(x + 45, y + 32, (float)hpWidth, 6, 2, getHealthColor(target.getHealth(), target.getMaxHealth()));
    }

    private void renderDummy(float x, float y) {
        RenderUtil.drawRoundedRect(x, y, getWidth(), getHeight(), 6, new Color(20, 20, 20, 200).getRGB());

        // Rysujemy głowę Steve'a w edytorze
        if (mc.thePlayer != null && mc.thePlayer.getLocationSkin() != null) {
            mc.getTextureManager().bindTexture(mc.thePlayer.getLocationSkin());
            Gui.drawScaledCustomSizeModalRect((int)x + 6, (int)y + 6, 8.0F, 8.0F, 8, 8, 32, 32, 64.0F, 64.0F);
        } else {
            // Fallback (kolorowy kwadrat)
            RenderUtil.drawRect(x + 6, y + 6, x + 38, y + 38, -1);
        }

        mc.fontRendererObj.drawStringWithShadow("Target Name", x + 45, y + 6, -1);
        mc.fontRendererObj.drawStringWithShadow("20.0 HP", x + 45, y + 18, new Color(200, 200, 200).getRGB());

        RenderUtil.drawRoundedRect(x + 45, y + 32, getWidth() - 50, 6, 2, new Color(40, 40, 40).getRGB());
        RenderUtil.drawRoundedRect(x + 45, y + 32, (getWidth() - 50) * 0.8f, 6, 2, new Color(0, 255, 0).getRGB());
    }

    private int getHealthColor(float health, float maxHealth) {
        float percentage = health / maxHealth;
        return Color.getHSBColor(percentage / 3f, 1f, 1f).getRGB();
    }
}