package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventRender3D;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ColorSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.util.ColorUtil;
import doom.util.ShaderUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12; // <--- WAŻNY IMPORT DLA GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL20;

import java.awt.*;
import java.nio.FloatBuffer;

public class GlowESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Color", "Color", "Rainbow", "Team");
    private final NumberSetting radius = new NumberSetting("Radius", this, 10, 1, 30, 1);
    private final NumberSetting exposure = new NumberSetting("Exposure", this, 2.5, 0.5, 5.0, 0.1);
    private final ColorSetting color = new ColorSetting("Color", this, new Color(220, 20, 60).getRGB());
    private final BooleanSetting self = new BooleanSetting("Self", this, true);

    private Framebuffer framebuffer;
    private Framebuffer glowBuffer;
    private final ShaderUtil outlineShader = new ShaderUtil("doom/shaders/glow_esp.frag");

    public GlowESP() {
        super("GlowESP", 0, Category.RENDER);
        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(radius);
        Client.INSTANCE.settingsManager.rSetting(exposure);
        Client.INSTANCE.settingsManager.rSetting(color);
        Client.INSTANCE.settingsManager.rSetting(self);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        checkSetupFBO();

        // 1. Setup OpenGL State
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();

        // 2. Render Mask
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(true);
        GL11.glColor4f(1, 1, 1, 1);
        renderEntities(event.getPartialTicks());
        framebuffer.unbindFramebuffer();

        mc.getFramebuffer().bindFramebuffer(true);

        // 3. Render Glow
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1, 1, 1, 1);

        // -- Pass 1: Horizontal --
        glowBuffer.framebufferClear();
        glowBuffer.bindFramebuffer(true);
        outlineShader.init();
        setupUniforms(1.0f, 0.0f);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
        // FIX: Używamy GL12.GL_CLAMP_TO_EDGE
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        outlineShader.setUniformi("textureIn", 0);
        drawQuads();
        outlineShader.unload();
        glowBuffer.unbindFramebuffer();

        // -- Pass 2: Vertical --
        mc.getFramebuffer().bindFramebuffer(true);
        outlineShader.init();
        setupUniforms(0.0f, 1.0f);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glowBuffer.framebufferTexture);
        // FIX: Używamy GL12.GL_CLAMP_TO_EDGE
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        outlineShader.setUniformi("textureIn", 0);

        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawQuads();
        outlineShader.unload();

        // 4. Restore State
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
        GlStateManager.resetColor();
    }

    private void renderEntities(float ticks) {
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (shouldRender(entity)) {
                // Maksymalna jasność dla maski
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                mc.getRenderManager().renderEntityStatic(entity, ticks, true);
            }
        }
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.thePlayer && !self.isEnabled()) return false;
        if (entity.isInvisible()) return false;
        return entity instanceof EntityPlayer;
    }

    private void setupUniforms(float dir1, float dir2) {
        Color c;
        if (mode.is("Rainbow")) c = new Color(ColorUtil.getRainbow(4.0f, 0.6f, 1.0f, 0));
        else if (mode.is("Team")) c = Color.WHITE;
        else c = new Color(color.getColor());

        outlineShader.setUniformf("texelSize", 1.0f / mc.displayWidth, 1.0f / mc.displayHeight);
        outlineShader.setUniformf("direction", dir1, dir2);
        outlineShader.setUniformf("radius", (float) radius.getValue());

        float exp = (float) exposure.getValue();
        outlineShader.setUniformf("color", (c.getRed()/255f) * exp, (c.getGreen()/255f) * exp, (c.getBlue()/255f) * exp);

        final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(256);
        for (int i = 0; i <= radius.getValue(); i++) {
            weightBuffer.put(calculateGaussianValue(i, (float) radius.getValue() / 2));
        }
        weightBuffer.rewind();
        GL20.glUniform1(outlineShader.getUniform("weights"), weightBuffer);
    }

    private float calculateGaussianValue(float x, float sigma) {
        double output = 1.0 / Math.sqrt(2.0 * Math.PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    private void drawQuads() {
        ScaledResolution sr = new ScaledResolution(mc);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(0, 1); GL11.glVertex2d(0, 0);
        GL11.glTexCoord2d(0, 0); GL11.glVertex2d(0, sr.getScaledHeight());
        GL11.glTexCoord2d(1, 0); GL11.glVertex2d(sr.getScaledWidth(), sr.getScaledHeight());
        GL11.glTexCoord2d(1, 1); GL11.glVertex2d(sr.getScaledWidth(), 0);
        GL11.glEnd();
    }

    private void checkSetupFBO() {
        if (framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            if (framebuffer != null) framebuffer.deleteFramebuffer();
            if (glowBuffer != null) glowBuffer.deleteFramebuffer();

            framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
            glowBuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
        }
        framebuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
        glowBuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
    }
}