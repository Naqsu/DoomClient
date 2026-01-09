package doom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.awt.Color;

public class RenderUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ==========================================================
    //                  SHADERS SOURCE (GLSL) - POPRAWIONE
    // ==========================================================

    private static final String ROUNDED_RECT_FRAG =
            "#version 120\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform vec4 color;\n" +
                    "uniform float radius;\n" +
                    "float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {\n" +
                    "    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 distance = abs(gl_TexCoord[0].st * rectSize - rectSize * 0.5);\n" +
                    "    float smoothedAlpha =  (1.0-smoothstep(0.0, 1.5, roundedBoxSDF(distance, rectSize * 0.5, radius))) * color.a;\n" +
                    "    gl_FragColor = vec4(color.rgb, smoothedAlpha);\n" +
                    "}";

    private static final String ROUNDED_GRADIENT_FRAG =
            "#version 120\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform vec4 color1, color2, color3, color4;\n" +
                    "uniform float radius;\n" +
                    "float roundedBoxSDF(vec2 centerPos, vec2 size, float radius) {\n" +
                    "    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 distance = abs(gl_TexCoord[0].st * rectSize - rectSize * 0.5);\n" +
                    "    float smoothedAlpha =  (1.0-smoothstep(0.0, 1.5, roundedBoxSDF(distance, rectSize * 0.5, radius)));\n" +
                    "    vec4 gradient = mix(mix(color1, color2, gl_TexCoord[0].s), mix(color3, color4, gl_TexCoord[0].s), gl_TexCoord[0].t);\n" +
                    "    gl_FragColor = vec4(gradient.rgb, gradient.a * smoothedAlpha);\n" +
                    "}";

    // !!! TU BYŁ PROBLEM: Usunąłem tablicę 'weights' i dodałem obliczanie wagi wewnątrz shadera !!!
    // Dzięki temu nie będzie czarnego ekranu.
    private static final String BLUR_FRAG =
            "#version 120\n" +
                    "uniform sampler2D textureIn;\n" +
                    "uniform vec2 texelSize, direction;\n" +
                    "uniform float radius;\n" +
                    "void main() {\n" +
                    "    vec3 color = vec3(0.0);\n" +
                    "    float total = 0.0;\n" +
                    "    for (float i = -radius; i <= radius; i++) {\n" +
                    "        vec4 sample = texture2D(textureIn, gl_TexCoord[0].st + i * texelSize * direction);\n" +
                    "        float weight = 1.0 - abs(i) / (radius + 1.0);\n" +
                    "        color += sample.rgb * weight;\n" +
                    "        total += weight;\n" +
                    "    }\n" +
                    "    gl_FragColor = vec4(color / total, 1.0);\n" +
                    "}";

    private static ShaderUtil roundedShader;
    private static ShaderUtil gradientShader;
    private static ShaderUtil blurShader;
    private static Framebuffer blurBuffer = new Framebuffer(1, 1, false);

    public static void initShaders() {
        if (roundedShader == null) roundedShader = new ShaderUtil(ROUNDED_RECT_FRAG, true);
        if (gradientShader == null) gradientShader = new ShaderUtil(ROUNDED_GRADIENT_FRAG, true);
        if (blurShader == null) blurShader = new ShaderUtil(BLUR_FRAG, true);
    }

    // ==========================================================
    //                  CORE METHODS (SHADERS)
    // ==========================================================

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        initShaders();
        Color c = new Color(color, true);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformf("color", c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);

        drawQuads(x - 1, y - 1, width + 2, height + 2);

        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color) {
        drawRoundedRect((float)x, (float)y, (float)width, (float)height, (float)radius, color);
    }
    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        drawRoundedRect((float)x, (float)y, (float)width, (float)height, (float)radius, color);
    }

    public static void drawRoundedGradientRect(float x, float y, float width, float height, float radius, int c1, int c2, int c3, int c4) {
        initShaders();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        gradientShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, gradientShader);
        setColorUniform(gradientShader, "color1", c1);
        setColorUniform(gradientShader, "color2", c2);
        setColorUniform(gradientShader, "color3", c3);
        setColorUniform(gradientShader, "color4", c4);

        drawQuads(x - 1, y - 1, width + 2, height + 2);

        gradientShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedGradientRect(float x, float y, float width, float height, float radius, int startColor, int endColor) {
        drawRoundedGradientRect(x, y, width, height, radius, startColor, startColor, endColor, endColor);
    }
    public static void drawRoundedGradientRect(double x, double y, double width, double height, double radius, int startColor, int endColor) {
        drawRoundedGradientRect((float)x, (float)y, (float)width, (float)height, (float)radius, startColor, endColor);
    }

    public static void drawGlow(float x, float y, float width, float height, int glowRadius, int color) {
        initShaders();
        Color c = new Color(color, true);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        roundedShader.init();

        setupRoundedRectUniforms(x - glowRadius, y - glowRadius, width + glowRadius * 2, height + glowRadius * 2, glowRadius * 2, roundedShader);
        roundedShader.setUniformf("color", c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, (c.getAlpha() / 255f) * 0.4f);

        drawQuads(x - glowRadius - 1, y - glowRadius - 1, width + glowRadius * 2 + 2, height + glowRadius * 2 + 2);

        roundedShader.unload();
        GlStateManager.disableBlend();
    }
    public static void drawGlow(double x, double y, double width, double height, int glowRadius, int color) {
        drawGlow((float)x, (float)y, (float)width, (float)height, glowRadius, color);
    }

    // !!! TO JEST WERSJA POPRAWIONA - ELIMINUJE BŁĄD 1282 I GLITCH KĄTA !!!
// !!! OSTATECZNA POPRAWKA - METODA COPY TEXTURE !!!
    public static void drawFullPageBlur(float radius) {
        if (mc.displayWidth == 0 || mc.displayHeight == 0) return;

        // 1. Tworzymy bufor (jeśli nie istnieje)
        if (blurBuffer == null || blurBuffer.framebufferWidth != mc.displayWidth || blurBuffer.framebufferHeight != mc.displayHeight) {
            if (blurBuffer != null) blurBuffer.deleteFramebuffer();
            // Tworzymy FBO. Ważne: false oznacza brak depth buffera (niepotrzebny do blura)
            blurBuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            blurBuffer.setFramebufferFilter(GL11.GL_LINEAR); // Liniowe filtrowanie (gładszy obraz)
        }

        initShaders();
        blurShader.init();

        setupBlurUniforms(radius);

        // 2. ROBIMY "ZDJĘCIE" EKRANU DO TEKSTURY
        // Zamiast bawić się w FBO bind/unbind, po prostu kopiujemy obecne piksele ekranu do naszej tekstury.
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurBuffer.framebufferTexture);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, mc.displayWidth, mc.displayHeight);

        // 3. RYSUJEMY
        // Teraz bindujemy tę teksturę (ze zdjęciem) i rysujemy ją przez shader.
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurBuffer.framebufferTexture);

        // Rysujemy na cały ekran (ScaledResolution dba o to, żeby pasowało do GUI)
        ScaledResolution sr = new ScaledResolution(mc);

        // Ważne: Renderujemy z kolorem białym, żeby nie przyciemniać blura (overlay robimy w ActiveModules)
        GL11.glColor4f(1f, 1f, 1f, 1f);

        drawQuads(0, 0, sr.getScaledWidth(), sr.getScaledHeight());

        blurShader.unload();

        // Przywracamy zwykły framebuffer texture, żeby reszta gry się nie zepsuła
        mc.getFramebuffer().bindFramebufferTexture();
    }

    // Metoda pomocnicza do uniformów (żeby było czyściej)
    private static void setupBlurUniforms(float radius) {
        blurShader.setUniform1i("textureIn", 0);
        blurShader.setUniformf("radius", radius);
        blurShader.setUniformf("texelSize", 1.0f / (float)mc.displayWidth, 1.0f / (float)mc.displayHeight);
        blurShader.setUniformf("direction", 1.0f, 0.0f); // Kierunek poziomy wystarczy dla prostego blura
    }

    // Stara metoda z Scissorem (zostawiam żeby nie psuć kompatybilności wstecznej)
    public static void drawBlur(float x, float y, float width, float height, float blurRadius) {
        initShaders();
        if (blurBuffer == null || blurBuffer.framebufferWidth != mc.displayWidth || blurBuffer.framebufferHeight != mc.displayHeight) {
            if (blurBuffer != null) blurBuffer.deleteFramebuffer();
            blurBuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(x, y, width, height);

        mc.getFramebuffer().bindFramebufferTexture();
        blurShader.init();
        blurShader.setUniform1i("textureIn", 0);
        blurShader.setUniformf("radius", blurRadius);
        blurShader.setUniformf("texelSize", 1.0f / mc.displayWidth, 1.0f / mc.displayHeight);
        blurShader.setUniformf("direction", 1.0f, 0.0f);

        ScaledResolution sr = new ScaledResolution(mc);
        drawQuads(0, 0, sr.getScaledWidth(), sr.getScaledHeight());

        blurShader.unload();
        mc.getFramebuffer().unbindFramebufferTexture();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    // ==========================================================
    //                  LEGACY COMPATIBILITY
    // ==========================================================

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left < right) { float i = left; left = right; right = i; }
        if (top < bottom) { float j = top; top = bottom; bottom = j; }
        drawRoundedRect(left, top, right - left, bottom - top, 0.0f, color);
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        drawRect((float)left, (float)top, (float)right, (float)bottom, color);
    }

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        drawRect((float)left, (float)top, (float)right, (float)bottom, color);
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
        float x1 = x + width;
        float y1 = y + height;
        float f = (color >> 24 & 0xFF) / 255.0F;
        float f1 = (color >> 16 & 0xFF) / 255.0F;
        float f2 = (color >> 8 & 0xFF) / 255.0F;
        float f3 = (color & 0xFF) / 255.0F;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glColor4f(f1, f2, f3, f);
        GL11.glLineWidth(lineWidth);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 180; i <= 270; i+=5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius);
        }
        for (int i = 270; i <= 360; i+=5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x1 - radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius);
        }
        for (int i = 0; i <= 90; i+=5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x1 - radius + Math.cos(angle) * radius, y1 - radius + Math.sin(angle) * radius);
        }
        for (int i = 90; i <= 180; i+=5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + radius + Math.cos(angle) * radius, y1 - radius + Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawRoundedOutline(double x, double y, double width, double height, double radius, double lineWidth, int color) {
        drawRoundedOutline((float)x, (float)y, (float)width, (float)height, (float)radius, (float)lineWidth, color);
    }
    public static void drawRoundedOutline(int x, int y, int width, int height, float radius, float lineWidth, int color) {
        drawRoundedOutline((float)x, (float)y, (float)width, (float)height, radius, lineWidth, color);
    }

    // ==========================================================
    //                  MATH & 3D (PRZYWRÓCONE)
    // ==========================================================

    public static double lerp(double current, double target, double speed) {
        return current + (target - current) * speed;
    }
    public static float lerp(float current, float target, double speed) {
        return (float) (current + (target - current) * speed);
    }

    public static void drawFilledBox(net.minecraft.util.AxisAlignedBB aa, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        GL11.glColor4f(r, g, b, a);
        drawBoxVertices(aa, 7); // GL_QUADS

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawBoundingBox(net.minecraft.util.AxisAlignedBB aa, float width, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(width);

        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        GL11.glColor4f(r, g, b, a);
        drawBoxVertices(aa, 3); // GL_LINE_STRIP

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void drawBoxVertices(net.minecraft.util.AxisAlignedBB aa, int mode) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        w.begin(mode, DefaultVertexFormats.POSITION);
        w.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        w.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        w.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        w.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        w.pos(aa.minX, aa.minY, aa.minZ).endVertex();
        w.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        w.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        w.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        w.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        w.pos(aa.minX, aa.maxY, aa.minZ).endVertex();
        w.pos(aa.minX, aa.maxY, aa.maxZ).endVertex();
        w.pos(aa.minX, aa.minY, aa.maxZ).endVertex();
        w.pos(aa.maxX, aa.minY, aa.maxZ).endVertex();
        w.pos(aa.maxX, aa.maxY, aa.maxZ).endVertex();
        w.pos(aa.maxX, aa.maxY, aa.minZ).endVertex();
        w.pos(aa.maxX, aa.minY, aa.minZ).endVertex();
        t.draw();
    }

    // ==========================================================
    //                  INTERNAL HELPERS
    // ==========================================================

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtil shader) {
        ScaledResolution sr = new ScaledResolution(mc);
        shader.setUniformf("location", x * sr.getScaleFactor(), (mc.displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        shader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        shader.setUniformf("radius", radius * sr.getScaleFactor());
    }

    private static void setColorUniform(ShaderUtil shader, String name, int color) {
        Color c = new Color(color, true);
        shader.setUniformf(name, c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
    }

    public static void drawQuads(float x, float y, float width, float height) {
        GL11.glBegin(GL11.GL_QUADS);
        // Lewy Dół (0, 1) -> Prawy Dół (1, 1) -> Prawy Góra (1, 0) -> Lewy Góra (0, 0)
        // W Minecraft GUI zazwyczaj Y idzie w dół, więc:
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y);          // Lewy Góra (Texture Y jest odwrócone w FBO)
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y + height); // Lewy Dół
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y + height); // Prawy Dół
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y);  // Prawy Góra
        GL11.glEnd();
    }

    public static void scissor(float x, float y, float width, float height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int finalY = (int) (mc.displayHeight - (y * scale) - (height * scale));
        GL11.glScissor((int) (x * scale), Math.max(0, finalY), (int) (width * scale), (int) (height * scale));
    }

    public static void drawTracerLine(BlockPos pos, Color color) {
        double x = pos.getX() - mc.getRenderManager().viewerPosX + 0.5;
        double y = pos.getY() - mc.getRenderManager().viewerPosY + 0.5;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ + 0.5;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glLineWidth(1.5f);
        GL11.glColor4f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, 1.0f);

        GL11.glBegin(GL11.GL_LINES);
        Vec3 forward = new Vec3(0, 0, 1).rotatePitch(- (float) Math.toRadians(mc.thePlayer.rotationPitch)).rotateYaw(- (float) Math.toRadians(mc.thePlayer.rotationYaw));

        GL11.glVertex3d(forward.xCoord, forward.yCoord + mc.thePlayer.getEyeHeight(), forward.zCoord);
        GL11.glVertex3d(x, y, z);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}