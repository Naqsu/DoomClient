package doom.ui.clickgui;

import doom.Client;
import doom.module.Category;
import doom.module.Module;
import doom.module.impl.render.ClickGuiModule;
import doom.settings.Setting;
import doom.settings.impl.*;
import doom.ui.clickgui.components.*;
import doom.ui.clickgui.components.Component;
import doom.ui.font.FontManager;
import doom.ui.hudeditor.GuiHudEditor;
import doom.util.AnimationUtil;
import doom.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClickGui extends GuiScreen {

    private float frameX, frameY, frameWidth, frameHeight;
    private final float sidebarWidth = 150;

    private Module.Category selectedCategory = Module.Category.COMBAT;
    private Module selectedModule = null;

    private float scrollY = 0, targetScrollY = 0, maxScroll = 0;
    private boolean dragging;
    private float dragX, dragY;

    private ArrayList<Component> activeComponents = new ArrayList<>();
    private final Map<String, Float> animMap = new HashMap<>();
    private float openProgress = 0.0f;

    @Override
    public void initGui() {
        openProgress = 0.0f;
        frameWidth = 700;
        frameHeight = 450;

        // Wyśrodkowanie przy pierwszym uruchomieniu (lub gdy pozycja to 0,0)
        if (frameX == 0 && frameY == 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            frameX = (sr.getScaledWidth() / 2f) - (frameWidth / 2f);
            frameY = (sr.getScaledHeight() / 2f) - (frameHeight / 2f);
        }
        if (selectedModule != null) initComponents();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (dragging) {
            frameX = mouseX - dragX;
            frameY = mouseY - dragY;
        }
        handleScroll();

        // --- FIX: ZABEZPIECZENIE PRZED UCIECZKĄ POZA EKRAN (CLAMP) ---
        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        // Jeśli okno jest za bardzo w lewo -> ustaw na 0
        if (frameX < 0) frameX = 0;
        // Jeśli okno jest za bardzo w górę -> ustaw na 0
        if (frameY < 0) frameY = 0;
        // Jeśli okno jest za bardzo w prawo -> przyciągnij do prawej krawędzi
        if (frameX + frameWidth > screenW) frameX = screenW - frameWidth;
        // Jeśli okno jest za bardzo w dół -> przyciągnij do dolnej krawędzi
        if (frameY + frameHeight > screenH) frameY = screenH - frameHeight;
        // -------------------------------------------------------------

        openProgress = AnimationUtil.animate(1.0f, openProgress, 0.1f);
        float ease = AnimationUtil.getEase(openProgress, ClickGuiModule.easing.getMode());

        float centerX = sr.getScaledWidth() / 2f;
        float centerY = sr.getScaledHeight() / 2f;

        GL11.glPushMatrix();

        String animMode = ClickGuiModule.openAnim.getMode();
        switch (animMode) {
            case "Zoom":
                GL11.glTranslated(centerX, centerY, 0);
                GL11.glScalef(ease, ease, 1f);
                GL11.glTranslated(-centerX, -centerY, 0);
                break;
            case "SlideUp":
                GL11.glTranslated(0, (1.0f - ease) * sr.getScaledHeight(), 0);
                break;
        }

        int alpha = animMode.equals("Fade") ? (int)(255 * openProgress) : 255;
        if (alpha < 10) alpha = 10;

        Color themeColor = ClickGuiModule.getGuiColor();
        int bgCol = new Color(18, 18, 22, (int)(230 * (alpha/255f))).getRGB();

        if (openProgress > 0.8) {
            RenderUtil.drawGlow(frameX, frameY, frameWidth, frameHeight, 15, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 100).getRGB());
        }
        RenderUtil.drawRoundedRect(frameX, frameY, frameWidth, frameHeight, 12, bgCol);
        RenderUtil.drawRoundedOutline(frameX, frameY, frameWidth, frameHeight, 12, 1.5f, new Color(255, 255, 255, 60).getRGB());

        drawSidebar(mouseX, mouseY, themeColor, alpha);

        float contentX = frameX + sidebarWidth;
        float contentY = frameY + 40;
        float contentW = frameWidth - sidebarWidth;
        float contentH = frameHeight - 40;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.scissor(contentX, contentY, contentW, contentH - 10);

        if (selectedModule == null) {
            drawModuleGrid(contentX, contentY, contentW, mouseX, mouseY, alpha);
        } else {
            drawSettingsPanel(contentX, contentY, contentW, mouseX, mouseY, alpha);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
    }

    private void drawSidebar(int mouseX, int mouseY, Color theme, int alpha) {
        float startX = frameX + 20;
        float startY = frameY + 50;

        // --- 1. TITLE ---
        FontManager.b20.drawStringWithShadow("DOOM", frameX + 20, frameY + 20, new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), alpha).getRGB());

        // --- 2. USER INFO ---
        String username = mc.session.getUsername();
        String stats = Minecraft.getDebugFPS() + " FPS";

        FontManager.r18.drawStringWithShadow("User: \u00A7f" + username, frameX + 20, frameY + 38, new Color(150, 150, 150, alpha).getRGB());
        FontManager.r18.drawStringWithShadow(stats, frameX + 20, frameY + 48, new Color(100, 100, 100, alpha).getRGB());

        // --- 3. CATEGORIES ---
        float catY = startY + 40;

        for (Module.Category cat : Module.Category.values()) {
            String icon = "";
            switch(cat) {
                case COMBAT: icon = doom.ui.font.Icons.COMBAT; break;
                case MOVEMENT: icon = doom.ui.font.Icons.MOVEMENT; break;
                case RENDER: icon = doom.ui.font.Icons.RENDER; break;
                case PLAYER: icon = doom.ui.font.Icons.PLAYER; break;
                case MISC: icon = doom.ui.font.Icons.MISC; break;
            }
            boolean isSelected = (cat == selectedCategory);
            float anim = getAnim("cat_" + cat.name(), isSelected ? 1.0f : 0.0f);

            float xOffset = 5 * anim;
            int color = RenderUtil.interpolateColor(new Color(150,150,150, alpha).getRGB(), -1, anim);

            if (anim > 0.05) {
                int barC = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), (int)(255 * anim * (alpha/255f))).getRGB();
                RenderUtil.drawRoundedRect(startX, catY + 2, 2, 14, 1.0f, barC);
            }

            // Ikona i tekst
            FontManager.icons18.drawStringWithShadow(icon, startX + 10 + xOffset, catY + 6, color);
            FontManager.r20.drawStringWithShadow(cat.name(), startX + 35 + xOffset, catY + 6, color);

            catY += 35;
        }

        // --- 4. BOTTOM BUTTONS ---
        float bottomY = frameY + frameHeight - 40;
        float iconX = startX + 10;

        drawIconButton(iconX, bottomY, mouseX, mouseY, "Settings", theme);
        drawIconButton(iconX + 35, bottomY, mouseX, mouseY, "HudEditor", theme);
        drawIconButton(iconX + 70, bottomY, mouseX, mouseY, "AltManager", theme);
    }

    private void drawIconButton(float x, float y, int mouseX, int mouseY, String type, Color themeColor) {
        boolean hover = mouseX >= x && mouseX <= x + 24 && mouseY >= y && mouseY <= y + 24;
        float anim = getAnim("btn_" + type, hover ? 1.0f : 0.0f);

        int bgNormal = new Color(40, 40, 45, 150).getRGB();
        int bgHover = new Color(70, 70, 80, 220).getRGB();
        int finalBg = RenderUtil.interpolateColor(bgNormal, bgHover, anim);

        RenderUtil.drawRoundedRect(x, y, 24, 24, 6, finalBg);

        int outlineNormal = new Color(255, 255, 255, 30).getRGB();
        int outlineHover = new Color(255, 255, 255, 150).getRGB();
        int finalOutline = RenderUtil.interpolateColor(outlineNormal, outlineHover, anim);

        RenderUtil.drawRoundedOutline(x, y, 24, 24, 6, 1.0f, finalOutline);

        if (anim > 0.05f) {
            int glowAlpha = (int)(150 * anim);
            int glowColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), glowAlpha).getRGB();
            RenderUtil.drawGlow(x, y, 24, 24, 8, glowColor);
        }

        String iconChar = "?";
        float fontSize = 0;

        switch (type) {
            case "Settings": iconChar = doom.ui.font.Icons.SETTINGS; fontSize = 1.0f; break;
            case "HudEditor": iconChar = doom.ui.font.Icons.HUD_EDITOR; fontSize = 1.0f; break;
            case "AltManager": iconChar = doom.ui.font.Icons.ALT_MANAGER; fontSize = 1.0f; break;
        }

        int iconNormal = new Color(160, 160, 170).getRGB();
        int iconHover = -1;
        int finalIconColor = RenderUtil.interpolateColor(iconNormal, iconHover, anim);

        doom.ui.font.FontManager.icons18.drawCenteredString(iconChar, x + 12, y + 9 + fontSize, finalIconColor);
    }

    private void drawModuleGrid(float x, float y, float w, int mouseX, int mouseY, int alpha) {
        java.util.List<Module> mods = Client.INSTANCE.moduleManager.getModulesByCategory(selectedCategory);
        float padding = 20;
        float modW = (w - (padding * 3)) / 2;
        float modH = 40;
        float gap = 15;

        // Poprawka: Obliczanie wysokości kontenera dla scrolla
        int rows = (int) Math.ceil(mods.size() / 2.0);
        float totalContentHeight = (rows * (modH + gap));
        float viewHeight = frameHeight - 60; // Dostępna wysokość
        maxScroll = Math.max(0, totalContentHeight - viewHeight);

        int i = 0;
        for (Module m : mods) {
            int col = i % 2;
            int row = i / 2;
            float btnX = x + padding + (col * (modW + gap));
            float btnY = y + 10 + scrollY + (row * (modH + gap));

            // Optymalizacja: Nie rysuj jeśli poza widokiem
            if (btnY > frameY + frameHeight || btnY + modH < frameY) { i++; continue; }

            boolean hover = mouseX >= btnX && mouseX <= btnX + modW && mouseY >= btnY && mouseY <= btnY + modH;
            float scaleTarget = (hover && ClickGuiModule.hoverScale.isEnabled()) ? 1.03f : 1.0f;
            float scale = getAnim("scale_" + m.getName(), scaleTarget);

            float cx = btnX + modW/2;
            float cy = btnY + modH/2;

            GL11.glPushMatrix();
            GL11.glTranslated(cx, cy, 0);
            GL11.glScalef(scale, scale, 1f);
            GL11.glTranslated(-cx, -cy, 0);

            float toggleAnim = getAnim("mod_" + m.getName(), m.isToggled() ? 1.0f : 0.0f);
            Color modColor = ClickGuiModule.getGuiColor(i * 2);
            int cOff = new Color(25, 25, 30, alpha).getRGB();
            int cOn = new Color(modColor.getRed(), modColor.getGreen(), modColor.getBlue(), (int)(50 * (alpha/255f))).getRGB();
            int bgCol = RenderUtil.interpolateColor(cOff, cOn, toggleAnim);

            RenderUtil.drawRoundedRect(btnX, btnY, modW, modH, 8, bgCol);
            int outlineC = RenderUtil.interpolateColor(new Color(255,255,255, 60).getRGB(), modColor.getRGB(), toggleAnim);
            RenderUtil.drawRoundedOutline(btnX, btnY, modW, modH, 8, 1.0f + (0.5f * toggleAnim), outlineC);
            FontManager.r20.drawStringWithShadow(m.getName(), btnX + 10, btnY + 16, -1);
            GL11.glPopMatrix();
            i++;
        }
    }

    private void drawSettingsPanel(float x, float y, float w, int mouseX, int mouseY, int alpha) {
        float startX = x + 20;
        float currentY = y + 10 + scrollY;

        // Header
        FontManager.b20.drawStringWithShadow(selectedModule.getName(), startX + 20, currentY + 15, -1);
        float backX = startX + w - 70;
        FontManager.r18.drawStringWithShadow("< Back", backX, currentY + 15, new Color(160,160,160, alpha).getRGB());

        float setY = currentY + 50;

        // Obliczanie maxScroll dla settings
        float totalH = 50;
        for (Component comp : activeComponents) {
            if (comp.setting.isVisible()) totalH += comp.height + 5;
        }
        maxScroll = Math.max(0, totalH - (frameHeight - 60));

        for (Component comp : activeComponents) {
            if (!comp.setting.isVisible()) continue;
            // Optymalizacja renderowania
            if (setY > frameY + frameHeight || setY + comp.height < frameY) {
                setY += comp.height + 5;
                continue;
            }
            comp.draw(startX + 20, setY, mouseX, mouseY);
            setY += comp.height + 5;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Dragging Title Bar
        if (mouseX >= frameX && mouseX <= frameX + frameWidth && mouseY >= frameY && mouseY <= frameY + 20 && mouseButton == 0) {
            dragging = true; dragX = mouseX - frameX; dragY = mouseY - frameY; return;
        }

        float clickStartY = frameY + 90;

        // Sidebar click
        if (mouseX >= frameX && mouseX <= frameX + sidebarWidth) {
            float cy = clickStartY;
            for (Module.Category cat : Module.Category.values()) {
                if (mouseY >= cy && mouseY <= cy + 24) {
                    selectedCategory = cat; selectedModule = null; scrollY = 0; targetScrollY = 0; return;
                }
                cy += 35;
            }
        }

        // Bottom buttons
        float bottomY = frameY + frameHeight - 40;
        float startX = frameX + 20;
        float iconX = startX + 10;

        if (mouseButton == 0 && mouseY >= bottomY && mouseY <= bottomY + 24) {
            if (mouseX >= iconX && mouseX <= iconX + 24) {
                mc.displayGuiScreen(new GuiConfigMenu(this)); return;
            }
            if (mouseX >= iconX + 35 && mouseX <= iconX + 35 + 24) {
                mc.displayGuiScreen(new GuiHudEditor()); return;
            }
            if (mouseX >= iconX + 70 && mouseX <= iconX + 70 + 24) {
                mc.displayGuiScreen(new doom.ui.alt.GuiAltManager(this)); return;
            }
        }

        // Module/Settings Content
        float contentX = frameX + sidebarWidth;
        float contentW = frameWidth - sidebarWidth;
        float contentY = frameY + 40;

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + frameHeight) {
            if (selectedModule == null) {
                // Modules
                java.util.List<Module> mods = Client.INSTANCE.moduleManager.getModulesByCategory(selectedCategory);
                float padding = 20; float modW = (contentW - (padding * 3)) / 2; float modH = 40; float gap = 15;
                int i = 0;
                for (Module m : mods) {
                    int col = i % 2; int row = i / 2;
                    float btnX = contentX + padding + (col * (modW + gap));
                    float btnY = contentY + 10 + scrollY + (row * (modH + gap));
                    if (mouseX >= btnX && mouseX <= btnX + modW && mouseY >= btnY && mouseY <= btnY + modH) {
                        if (mouseButton == 0) m.toggle();
                        else if (mouseButton == 1) {
                            selectedModule = m; initComponents(); scrollY = 0; targetScrollY = 0;
                        }
                        return;
                    }
                    i++;
                }
            } else {
                // Settings
                startX = contentX + 20;
                float headY = contentY + 10 + scrollY + 15;
                float backX = startX + contentW - 70;
                if (mouseButton == 0 && mouseX >= backX && mouseX <= backX + 40 && mouseY >= headY - 10 && mouseY <= headY + 20) {
                    selectedModule = null; scrollY = 0; targetScrollY = 0; return;
                }
                for (Component comp : activeComponents) {
                    if (comp.setting.isVisible()) comp.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    private void initComponents() {
        activeComponents.clear();
        float width = (frameWidth - sidebarWidth) - 80;
        for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(selectedModule)) {
            if (s instanceof BooleanSetting) activeComponents.add(new BooleanValueComponent((BooleanSetting)s, width, 28));
            else if (s instanceof NumberSetting) activeComponents.add(new NumberValueComponent((NumberSetting)s, width, 30));
            else if (s instanceof ModeSetting) activeComponents.add(new ModeValueComponent((ModeSetting)s, width, 32));
            else if (s instanceof ColorSetting) activeComponents.add(new ColorValueComponent((ColorSetting)s, width, 20));
            else if (s instanceof CategorySetting) activeComponents.add(new CategoryComponent((CategorySetting)s, width, 24));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        if (selectedModule != null) for (Component c : activeComponents) if (c.setting.isVisible()) c.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (selectedModule != null) for (Component c : activeComponents) if (c.setting.isVisible()) c.keyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    private void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel < 0) targetScrollY -= 35; else if (wheel > 0) targetScrollY += 35;
        if (targetScrollY > 0) targetScrollY = 0; if (targetScrollY < -maxScroll) targetScrollY = -maxScroll;
        scrollY = RenderUtil.lerp(scrollY, targetScrollY, 0.2f);
    }

    private float getAnim(String key, float target) {
        float current = animMap.getOrDefault(key, 0.0f);
        float next = RenderUtil.lerp(current, target, 0.2f);
        animMap.put(key, next);
        return next;
    }

    @Override
    public void onGuiClosed() {
        Module clickGui = Client.INSTANCE.moduleManager.getModule(ClickGuiModule.class);
        if (clickGui != null && clickGui.isToggled()) clickGui.setToggled(false);
        Client.INSTANCE.configManager.save();
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}