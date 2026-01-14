package doom.module.impl.render;

import doom.Client;
import doom.module.DraggableModule;
import doom.module.Module;
import doom.ui.font.CustomFontRenderer;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import doom.util.StencilUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveModules extends DraggableModule {

    public ActiveModules() {
        super("ArrayList", Category.RENDER);
        this.x = 2;
        this.y = 45;
    }

    @Override public float getWidth() { return 100; }
    @Override public float getHeight() { return 100; }

    @Override
    public void render(float x, float y) {
        HUD hud = Client.INSTANCE.moduleManager.getModule(HUD.class);
        CustomFontRenderer font = hud.font.is("Bold") ? FontManager.b18 : FontManager.r18;

        // FIX: Używamy alAlign z HUD
        boolean right = hud.alAlign.is("Right");

        List<Module> modules = Client.INSTANCE.moduleManager.getModules().stream()
                .filter(m -> m.isToggled() && !m.hidden)
                .sorted(Comparator.comparingDouble(m -> -font.getStringWidth(getDisplayText(m, hud))))
                .collect(Collectors.toList());

        if (modules.isEmpty()) return;

        float moduleHeight = 18;
        float radius = 5;
        int bgColor = new Color(10, 10, 10, 120).getRGB();

        // Pozycje Y
        float[] yPositions = new float[modules.size()];
        float tempY = y;
        for(int i = 0; i < modules.size(); i++) {
            yPositions[i] = tempY;
            if (i < modules.size() - 1) tempY += moduleHeight;
        }

        // =======================================================
        // FAZA 1: STENCIL
        // =======================================================
        Framebuffer fbo = mc.getFramebuffer();
        RenderUtil.checkSetupFBO(fbo);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 1);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);

        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            String text = getDisplayText(m, hud);
            float width = font.getStringWidth(text) + 14;
            float drawX = right ? (x + getWidth() - width) : x;
            float drawY = yPositions[i];

            boolean isLast = (i == modules.size() - 1);
            boolean isFirst = (i == 0);
            float renderH = isLast ? moduleHeight : moduleHeight + 1.0f;

            drawCustomRect(drawX, drawY, width, renderH, radius, -1, right, isFirst, isLast);
        }

        // =======================================================
        // FAZA 2: RYSOWANIE
        // =======================================================
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 1);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        // Blur z HUD (Globalny)
        if (hud.blur.isEnabled()) {
            RenderUtil.drawFullPageBlur(15);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        RenderUtil.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), bgColor);

        GL11.glDisable(GL11.GL_STENCIL_TEST);

        // =======================================================
        // FAZA 3: DETALE
        // =======================================================
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            String text = getDisplayText(m, hud);
            String name = m.getName(); // Fix: Oryginalna nazwa (CamelCase)
            String suffix = (m.getSuffix() != null && !m.getSuffix().isEmpty()) ? m.getSuffix() : "";

            float width = font.getStringWidth(text) + 14;

            // FIX: Nowa metoda kolorów z HUD
            int color = hud.getArrayListColor(i);

            float drawX = right ? (x + getWidth() - width) : x;
            float drawY = yPositions[i];

            // Sidebar
            int sidebarColor = 0xFF000000 | color;

            // FIX: Używamy alSidebar z HUD
            if (hud.alSidebar.isEnabled()) {
                if (right) Gui.drawRect((int)(drawX + width - 2), (int)drawY, (int)(drawX + width), (int)(drawY + moduleHeight), sidebarColor);
                else Gui.drawRect((int)drawX, (int)drawY, (int)(drawX + 2), (int)(drawY + moduleHeight), sidebarColor);
            }

            // Tekst
            float textY = drawY + 5;
            float textX = drawX + 6;
            font.drawStringWithShadow(name, textX, textY, color);

            if (!suffix.isEmpty()) {
                float nameW = font.getStringWidth(name);
                font.drawStringWithShadow(suffix, textX + nameW + 2, textY, 0xFFCDCDCD);
            }
        }
    }

    private void drawCustomRect(float x, float y, float w, float h, float r, int c, boolean rightAlign, boolean roundTop, boolean roundBot) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBegin(GL11.GL_POLYGON);

        if (rightAlign) {
            if (roundTop) {
                for (int i = 0; i <= 90; i += 10)
                    GL11.glVertex2d(x + r + Math.sin(i * Math.PI / 180.0) * r * -1.0, y + r + Math.cos(i * Math.PI / 180.0) * r * -1.0);
            } else {
                GL11.glVertex2d(x, y);
            }
            if (roundBot) {
                for (int i = 90; i <= 180; i += 10)
                    GL11.glVertex2d(x + r + Math.sin(i * Math.PI / 180.0) * r * -1.0, y + h - r + Math.cos(i * Math.PI / 180.0) * r * -1.0);
            } else {
                GL11.glVertex2d(x, y + h);
            }
            GL11.glVertex2d(x + w, y + h);
            GL11.glVertex2d(x + w, y);
        } else {
            GL11.glVertex2d(x, y);
            GL11.glVertex2d(x, y + h);
            if (roundBot) {
                for (int i = 90; i >= 0; i -= 10)
                    GL11.glVertex2d(x + w - r + Math.cos(Math.toRadians(i)) * r, y + h - r + Math.sin(Math.toRadians(i)) * r);
            } else {
                GL11.glVertex2d(x + w, y + h);
            }
            if (roundTop) {
                for (int i = 0; i >= -90; i -= 10)
                    GL11.glVertex2d(x + w - r + Math.cos(Math.toRadians(i)) * r, y + r + Math.sin(Math.toRadians(i)) * r);
            } else {
                GL11.glVertex2d(x + w, y);
            }
        }

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private String getDisplayText(Module m, HUD hud) {
        // FIX: Używamy alSuffix z HUD
        if (hud.alSuffix.isEnabled() && m.getSuffix() != null && !m.getSuffix().isEmpty()) {
            return m.getName() + " " + m.getSuffix();
        }
        return m.getName();
    }
}