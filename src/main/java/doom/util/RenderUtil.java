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
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class RenderUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Framebuffer blurBuffer;

    // ====================================================================================
    //                                  SHADERS (GLSL)
    // ====================================================================================

    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "void main() { gl_TexCoord[0] = gl_MultiTexCoord0; gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; }";

    // 1. ROUNDED RECT
    private static final String ROUNDED_FRAG =
            "#version 120\n" +
                    "uniform vec2 size;\n" +
                    "uniform vec4 color;\n" +
                    "uniform float radius;\n" +
                    "void main() {\n" +
                    "    vec2 pos = abs(gl_TexCoord[0].st * size - size * 0.5);\n" +
                    "    float d = length(max(pos - size * 0.5 + radius, 0.0)) - radius;\n" +
                    "    float alpha = 1.0 - smoothstep(0.0, 1.5, d);\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
                    "}";

    // 2. ROUNDED OUTLINE (Solidna ramka)
    private static final String ROUNDED_OUTLINE_FRAG =
            "#version 120\n" +
                    "uniform vec2 size;\n" +
                    "uniform vec4 color;\n" +
                    "uniform float radius;\n" +
                    "uniform float thickness;\n" +
                    "void main() {\n" +
                    "    vec2 pos = abs(gl_TexCoord[0].st * size - size * 0.5);\n" +
                    "    float d = length(max(pos - size * 0.5 + radius, 0.0)) - radius;\n" +
                    "    float halfThick = thickness * 0.5;\n" +
                    "    // Rysujemy na granicy 0.0\n" +
                    "    float alpha = smoothstep(halfThick, halfThick - 1.5, abs(d));\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
                    "}";

    // 3. TIGHT GLOW / SHADOW (Poprawiony!)
    // To jest shader odpowiedzialny za poświatę. Został zmieniony, aby rogi nie uciekały.
    private static final String GLOW_FRAG =
            "#version 120\n" +
                    "uniform vec2 size;\n" +
                    "uniform vec4 color;\n" +
                    "uniform float radius;\n" +
                    "uniform float softness;\n" +
                    "float roundedSDF(vec2 centerPos, vec2 size, float radius) {\n" +
                    "    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 pos = abs(gl_TexCoord[0].st * size - size * 0.5);\n" +
                    "    // Obliczamy dystans od 'wirtualnego' obiektu wewnątrz\n" +
                    "    // Odejmujemy softness od wymiarów, aby poświata zaczynała się idealnie na krawędzi\n" +
                    "    float d = roundedSDF(pos, size * 0.5 - vec2(softness), radius);\n" +
                    "    \n" +
                    "    float alpha = 0.0;\n" +
                    "    if (d > 0.0) {\n" +
                    "        // Wykładniczy spadek (Tight curve)\n" +
                    "        // 4.0 to współczynnik ostrości. Im większy, tym bardziej 'ściśnięty' glow.\n" +
                    "        alpha = exp(-d * 4.0 / softness);\n" +
                    "    }\n" +
                    "    // Opcjonalnie: wytnij środek (jeśli d < 0)\n" +
                    "    // if (d <= 0.0) alpha = 0.0;\n" +
                    "    \n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
                    "}";

    // 4. RADIAL GLOW
    private static final String RADIAL_GLOW_FRAG =
            "#version 120\n" +
                    "uniform vec4 color;\n" +
                    "uniform float softness;\n" +
                    "void main() {\n" +
                    "    vec2 uv = gl_TexCoord[0].st - 0.5;\n" +
                    "    float d = length(uv) * 2.0;\n" +
                    "    float alpha = 0.0;\n" +
                    "    if (d < 1.0) {\n" +
                    "        alpha = 1.0 - d;\n" +
                    "        alpha = pow(alpha, softness);\n" +
                    "    }\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
                    "}";

    // 5. GRADIENT
    private static final String GRADIENT_FRAG =
            "#version 120\n" +
                    "uniform vec2 size;\n" +
                    "uniform vec4 color1, color2, color3, color4;\n" +
                    "uniform float radius;\n" +
                    "void main() {\n" +
                    "    vec2 pos = abs(gl_TexCoord[0].st * size - size * 0.5);\n" +
                    "    float d = length(max(pos - size * 0.5 + radius, 0.0)) - radius;\n" +
                    "    float alpha = 1.0 - smoothstep(0.0, 1.5, d);\n" +
                    "    vec4 gradient = mix(mix(color1, color2, gl_TexCoord[0].s), mix(color3, color4, gl_TexCoord[0].s), gl_TexCoord[0].t);\n" +
                    "    gl_FragColor = vec4(gradient.rgb, gradient.a * alpha);\n" +
                    "}";

    // 6. BLUR
    private static final String BLUR_FRAG =
            "#version 120\n" +
                    "uniform sampler2D textureIn;\n" +
                    "uniform vec2 texelSize, direction;\n" +
                    "uniform float radius;\n" +
                    "uniform float weights[256];\n" +
                    "void main() {\n" +
                    "    vec3 color = vec3(0.0);\n" +
                    "    float total = 0.0;\n" +
                    "    for (float i = -radius; i <= radius; i++) {\n" +
                    "        color += texture2D(textureIn, gl_TexCoord[0].st + i * texelSize * direction).rgb * weights[int(abs(i))];\n" +
                    "        total += weights[int(abs(i))];\n" +
                    "    }\n" +
                    "    gl_FragColor = vec4(color / total, 1.0);\n" +
                    "}";

    private static int blurProgram = -1;
    private static int roundedProgram = -1;
    private static int outlineProgram = -1;
    private static int glowProgram = -1;
    private static int radialGlowProgram = -1;
    private static int gradientProgram = -1;

    public static void initShaders() {
        if (blurProgram == -1) {
            blurProgram = createProgram(VERTEX_SHADER, BLUR_FRAG);
            roundedProgram = createProgram(VERTEX_SHADER, ROUNDED_FRAG);
            outlineProgram = createProgram(VERTEX_SHADER, ROUNDED_OUTLINE_FRAG);
            glowProgram = createProgram(VERTEX_SHADER, GLOW_FRAG);
            radialGlowProgram = createProgram(VERTEX_SHADER, RADIAL_GLOW_FRAG);
            gradientProgram = createProgram(VERTEX_SHADER, GRADIENT_FRAG);
        }
    }

    private static int createProgram(String vert, String frag) {
        int program = GL20.glCreateProgram();
        int v = createShader(vert, GL20.GL_VERTEX_SHADER);
        int f = createShader(frag, GL20.GL_FRAGMENT_SHADER);
        GL20.glAttachShader(program, v);
        GL20.glAttachShader(program, f);
        GL20.glLinkProgram(program);
        return program;
    }

    private static int createShader(String source, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println(GL20.glGetShaderInfoLog(shader, 4096));
        }
        return shader;
    }

    // ====================================================================================
    //                                  RENDERING METHODS
    // ====================================================================================

    // --- RECTANGLES ---

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        initShaders();
        GL20.glUseProgram(roundedProgram);
        setupUniforms(roundedProgram, width, height, radius);
        setShaderColor(roundedProgram, "color", color);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        drawQuads(x, y, width, height);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL20.glUseProgram(0);
    }
    public static void drawRoundedRect(double x, double y, double w, double h, double r, int c) {
        drawRoundedRect((float)x, (float)y, (float)w, (float)h, (float)r, c);
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float thickness, int color) {
        initShaders();
        GL20.glUseProgram(outlineProgram);

        float margin = thickness;
        setupUniforms(outlineProgram, width + margin*2, height + margin*2, radius + thickness);
        GL20.glUniform1f(GL20.glGetUniformLocation(outlineProgram, "thickness"), thickness);
        setShaderColor(outlineProgram, "color", color);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        drawQuads(x - thickness, y - thickness, width + thickness*2, height + thickness*2);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL20.glUseProgram(0);
    }
    public static void drawRoundedOutline(float x, float y, float w, float h, int r, float t, int c) {
        drawRoundedOutline(x, y, w, h, (float)r, t, c);
    }

    /**
     * TIGHT GLOW - Naprawiony
     * @param glowRadius Siła poświaty. W shaderze działa jako 'softness'.
     */
    public static void drawGlow(float x, float y, float width, float height, int glowRadius, int color) {
        initShaders();
        GL20.glUseProgram(glowProgram);

        // Zmniejszamy margines - to jest klucz do "tight glow"
        // Używamy dokładnie takiej wielkości marginesu jak radius poświaty.
        float margin = (float) glowRadius;

        // Rozmiar quada powiększony o margines
        float totalWidth = width + margin * 2.0f;
        float totalHeight = height + margin * 2.0f;

        // Przekazujemy rozmiar quada do shadera
        GL20.glUniform2f(GL20.glGetUniformLocation(glowProgram, "size"), totalWidth, totalHeight);
        // Radius obiektu - stały (np. 8 dla zaokrąglenia modułu)
        GL20.glUniform1f(GL20.glGetUniformLocation(glowProgram, "radius"), 8.0f);
        // Softness (siła rozmycia) = glowRadius
        GL20.glUniform1f(GL20.glGetUniformLocation(glowProgram, "softness"), margin);

        setShaderColor(glowProgram, "color", color);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Rysujemy quad centrowany wokół oryginalnego obiektu
        drawQuads(x - margin, y - margin, totalWidth, totalHeight);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL20.glUseProgram(0);
    }
    public static void drawGlow(double x, double y, double width, double height, int glowRadius, int color) {
        drawGlow((float)x, (float)y, (float)width, (float)height, glowRadius, color);
    }

    public static void drawRadialGlow(float cx, float cy, float radius, float softness, int color) {
        initShaders();
        GL20.glUseProgram(radialGlowProgram);
        setShaderColor(radialGlowProgram, "color", color);
        GL20.glUniform1f(GL20.glGetUniformLocation(radialGlowProgram, "softness"), softness);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        drawQuads(cx - radius, cy - radius, radius * 2, radius * 2);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL20.glUseProgram(0);
    }

    public static void drawRoundedGradientRect(float x, float y, float width, float height, float radius, int c1, int c2, int c3, int c4) {
        initShaders();
        GL20.glUseProgram(gradientProgram);
        setupUniforms(gradientProgram, width, height, radius);
        setShaderColor(gradientProgram, "color1", c1);
        setShaderColor(gradientProgram, "color2", c2);
        setShaderColor(gradientProgram, "color3", c3);
        setShaderColor(gradientProgram, "color4", c4);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        drawQuads(x, y, width, height);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL20.glUseProgram(0);
    }
    public static void drawRoundedGradientRect(float x, float y, float width, float height, float radius, int c1, int c2) {
        drawRoundedGradientRect(x, y, width, height, radius, c1, c1, c2, c2);
    }
    public static void drawRoundedGradientRect(int x, int y, int width, int height, float radius, int c1, int c2) {
        drawRoundedGradientRect((float)x, (float)y, (float)width, (float)height, radius, c1, c1, c2, c2);
    }



    public static void checkBlurBuffer() {
        if (blurBuffer == null || blurBuffer.framebufferWidth != mc.displayWidth || blurBuffer.framebufferHeight != mc.displayHeight) {
            if (blurBuffer != null) blurBuffer.deleteFramebuffer();
            blurBuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            blurBuffer.setFramebufferFilter(GL11.GL_LINEAR);
        }
    }
    public static void drawFullPageBlur(float radius) { drawBlur(radius); }
    public static void drawBlur(float x, float y, float width, float height, float radius) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(x, y, width, height);
        drawBlur(radius);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    public static void drawBlur(float x, float y, float width, float height, int radius) { drawBlur(x, y, width, height, (float) radius); }
    public static void drawBlur(float radius) {
        initShaders();
        if (mc.theWorld == null) return;
        checkBlurBuffer();
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        GlStateManager.ortho(0, mc.displayWidth, mc.displayHeight, 0, 1000, 3000);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix(); GlStateManager.loadIdentity(); GlStateManager.translate(0, 0, -2000);
        blurBuffer.bindFramebuffer(true); mc.getFramebuffer().bindFramebufferTexture();
        GL20.glUseProgram(blurProgram); setupGaussianWeights(radius);
        GL20.glUniform1i(GL20.glGetUniformLocation(blurProgram, "textureIn"), 0);
        GL20.glUniform2f(GL20.glGetUniformLocation(blurProgram, "texelSize"), 1.0f / mc.displayWidth, 1.0f / mc.displayHeight);
        GL20.glUniform2f(GL20.glGetUniformLocation(blurProgram, "direction"), 1.0f, 0.0f);
        GL20.glUniform1f(GL20.glGetUniformLocation(blurProgram, "radius"), radius);
        drawQuads(0, 0, (float)mc.displayWidth, (float)mc.displayHeight);
        mc.getFramebuffer().bindFramebuffer(true); blurBuffer.bindFramebufferTexture();
        GL20.glUniform2f(GL20.glGetUniformLocation(blurProgram, "direction"), 0.0f, 1.0f);
        drawQuads(0, 0, (float)mc.displayWidth, (float)mc.displayHeight);
        GL20.glUseProgram(0);
        GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.popMatrix();
    }
    private static void setupGaussianWeights(float radius) {
        float[] weights = new float[256]; float sum = 0;
        for (int i = 0; i <= radius; i++) { weights[i] = (float) (1.0 / Math.sqrt(2 * Math.PI * radius) * Math.exp(-(i * i) / (2 * radius * radius))); if (i == 0) sum += weights[i]; else sum += weights[i] * 2.0; }
        for (int i = 0; i < weights.length; i++) weights[i] /= sum;
        for (int i = 0; i <= radius; i++) GL20.glUniform1f(GL20.glGetUniformLocation(blurProgram, "weights[" + i + "]"), weights[i]);
    }

    // --- STANDARD UTILS ---
    public static void drawCircle(float x, float y, float radius, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius);
        }
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawGradientRect(float x, float y, float w, float h, int tl, int tr, int br, int bl) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        addVertexColor(buffer, x + w, y, tr);
        addVertexColor(buffer, x, y, tl);
        addVertexColor(buffer, x, y + h, bl);
        addVertexColor(buffer, x + w, y + h, br);
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    public static void drawGradientRectHorizontal(float x, float y, float width, float height, int startColor, int endColor) {
        drawGradientRect(x, y, width, height, startColor, endColor, endColor, startColor);
    }

    public static void drawRect(float x, float y, float w, float h, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        GL11.glBegin(7);
        GL11.glVertex2d(x, y + h); GL11.glVertex2d(x + w, y + h); GL11.glVertex2d(x + w, y); GL11.glVertex2d(x, y);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    public static void drawRect(double l, double t, double r, double b, int c) { drawRect((float)l, (float)t, (float)(r-l), (float)(b-t), c); }
    public static void drawRect(int l, int t, int r, int b, int c) { drawRect((float)l, (float)t, (float)(r-l), (float)(b-t), c); }

    public static void drawFilledBox(net.minecraft.util.AxisAlignedBB bb, int color) {
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawBoundingBox(net.minecraft.util.AxisAlignedBB bb, float width, int color) {
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(width);
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawTracerLine(BlockPos pos, Color color) {
        if (mc.getRenderManager() == null) return;
        double x = pos.getX() - mc.getRenderManager().viewerPosX + 0.5;
        double y = pos.getY() - mc.getRenderManager().viewerPosY + 0.5;
        double z = pos.getZ() - mc.getRenderManager().viewerPosZ + 0.5;
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f);
        GL11.glBegin(GL11.GL_LINES);
        Vec3 eyes = new Vec3(0, 0, 1).rotatePitch(-(float)Math.toRadians(mc.thePlayer.rotationPitch)).rotateYaw(-(float)Math.toRadians(mc.thePlayer.rotationYaw));
        GL11.glVertex3d(eyes.xCoord, eyes.yCoord + mc.thePlayer.getEyeHeight(), eyes.zCoord);
        GL11.glVertex3d(x, y, z);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    // --- HELPERS ---
    public static int interpolateColor(int startColor, int endColor, float fraction) {
        if (fraction > 1) fraction = 1; if (fraction < 0) fraction = 0;
        int a1 = (startColor >> 24) & 0xFF, r1 = (startColor >> 16) & 0xFF, g1 = (startColor >> 8) & 0xFF, b1 = startColor & 0xFF;
        int a2 = (endColor >> 24) & 0xFF, r2 = (endColor >> 16) & 0xFF, g2 = (endColor >> 8) & 0xFF, b2 = endColor & 0xFF;
        int a = (int) (a1 + (a2 - a1) * fraction), r = (int) (r1 + (r2 - r1) * fraction), g = (int) (g1 + (g2 - g1) * fraction), b = (int) (b1 + (b2 - b1) * fraction);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    public static float lerp(float c, float t, float s) { return c + (t - c) * s; }
    public static double lerp(double c, double t, double s) { return c + (t - c) * s; }

    public static void scissor(float x, float y, float width, float height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        if (width < 0) width = 0; if (height < 0) height = 0;
        GL11.glScissor((int) (x * f), (int) ((sr.getScaledHeight() - (y + height)) * f), (int) (width * f), (int) (height * f));
    }
    public static void prepareScissorBox(float x, float y, float width, float height) { scissor(x, y, width, height); }

    private static void setupUniforms(int program, float width, float height, float radius) {
        ScaledResolution sr = new ScaledResolution(mc);
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "location"), 0, 0);
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "size"), width * sr.getScaleFactor(), height * sr.getScaleFactor());
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "radius"), radius * sr.getScaleFactor());
    }
    private static void setShaderColor(int program, String name, int color) {
        Color c = new Color(color, true);
        GL20.glUniform4f(GL20.glGetUniformLocation(program, name), c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f);
    }
    private static void drawQuads(float x, float y, float width, float height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y);
        GL11.glEnd();
    }
    private static void addVertexColor(WorldRenderer r, float x, float y, int c) {
        float a=(c>>24&255)/255F, red=(c>>16&255)/255F, g=(c>>8&255)/255F, b=(c&255)/255F;
        r.pos(x, y, 0.0D).color(red, g, b, a).endVertex();
    }
    public static void checkSetupFBO(Framebuffer framebuffer) { if (framebuffer != null && framebuffer.depthBuffer > -1) { setupFBO(framebuffer); framebuffer.depthBuffer = -1; } }
    public static void setupFBO(Framebuffer framebuffer) {
        EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthBuffer);
        int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
        EXTFramebufferObject.glBindRenderbufferEXT(36161, stencilDepthBufferID);
        EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, mc.displayWidth, mc.displayHeight);
        EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencilDepthBufferID);
        EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencilDepthBufferID);
    }
}