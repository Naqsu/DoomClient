package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.ColorUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class Wings extends Module {

    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", this, true);
    private final NumberSetting scale = new NumberSetting("Scale", this, 150.0, 50.0, 400.0, 10.0);

    private final NumberSetting height = new NumberSetting("Height", this, 0.0, -1.0, 1.0, 0.05);
    private final NumberSetting distance = new NumberSetting("Distance", this, 0.0, -1.0, 1.0, 0.05);

    private final ModelDragonWings dragonWingsModel = new ModelDragonWings();
    private final ResourceLocation wingTexture = new ResourceLocation("doom/textures/img.png");

    public Wings() {
        super("Wings", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(rainbow);
        Client.INSTANCE.settingsManager.rSetting(scale);
        Client.INSTANCE.settingsManager.rSetting(height);
        Client.INSTANCE.settingsManager.rSetting(distance);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.gameSettings.thirdPersonView == 0 || mc.thePlayer.isInvisible()) return;

        double ix = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.getPartialTicks();
        double iy = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.getPartialTicks();
        double iz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.getPartialTicks();

        double x = ix - mc.getRenderManager().viewerPosX;
        double y = iy - mc.getRenderManager().viewerPosY;
        double z = iz - mc.getRenderManager().viewerPosZ;

        // Używamy nowej metody interpolateRotation
        float bodyYaw = interpolateRotation(mc.thePlayer.prevRenderYawOffset, mc.thePlayer.renderYawOffset, event.getPartialTicks());

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        GL11.glRotatef(180 - bodyYaw, 0, 1, 0);
        GL11.glRotatef(180, 0, 1, 0); // Fix modelu

        // --- GFX ---
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GlStateManager.enableRescaleNormal();
        GL11.glEnable(GL11.GL_COLOR_MATERIAL); // Fix Kolorów
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // Pozycjonowanie
        double configHeight = height.getValue();
        double configDist = distance.getValue();

        if (mc.thePlayer.isSneaking()) {
            GL11.glTranslated(0, 1.2 + configHeight, -0.1 + configDist);
            GL11.glRotatef(25, 1, 0, 0);
        } else {
            GL11.glTranslated(0, 1.4 + configHeight, -0.1 + configDist);
        }

        // Skala
        float s = (float) scale.getValue() / 100.0f;
        GL11.glScalef(-s, -s, s);

        // Kolory
        if (rainbow.isEnabled()) {
            int c = ColorUtil.getRainbow(4.0f, 0.8f, 1.0f, 0);
            float r = (c >> 16 & 0xFF) / 255.0F;
            float g = (c >> 8 & 0xFF) / 255.0F;
            float b = (c & 0xFF) / 255.0F;
            GlStateManager.color(r, g, b, 1.0f);
        } else {
            GlStateManager.color(1, 1, 1, 1);
        }

        // Render
        mc.getTextureManager().bindTexture(wingTexture);
        dragonWingsModel.setRotationAngles(event.getPartialTicks());
        dragonWingsModel.render(null, 0, 0, 0, 0, 0, 0.0625f);

        // Cleanup
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL11.glDisable(GL11.GL_LIGHTING); // Fix szarości w innych modułach
        GlStateManager.disableRescaleNormal();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GlStateManager.color(1, 1, 1, 1);
        GL11.glPopMatrix();
    }

    // --- NAPRAWIONA INTERPOLACJA ---
    // Ta metoda sprawdza, czy obrót przekroczył 180 stopni i koryguje go,
    // wybierając najkrótszą drogę. Dzięki temu skrzydła nie wariują.
    private float interpolateRotation(float prev, float current, float partialTicks) {
        float diff = current - prev;

        while (diff < -180.0F) {
            diff += 360.0F;
        }
        while (diff >= 180.0F) {
            diff -= 360.0F;
        }

        return prev + diff * partialTicks;
    }
}