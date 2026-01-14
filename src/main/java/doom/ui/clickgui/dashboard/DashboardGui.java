package doom.ui.clickgui.dashboard;

import doom.Client;
import doom.module.Module;
import doom.module.impl.render.ClickGuiModule;
import doom.settings.Setting;
import doom.settings.impl.*;
import doom.ui.clickgui.components.*;
import doom.ui.clickgui.components.Component;
import doom.ui.font.FontManager;
import doom.util.AnimationUtil;
import doom.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardGui extends GuiScreen {

    private float x, y, width, height;
    private boolean dragging;
    private float dragX, dragY;

    private Module.Category selectedCategory = Module.Category.COMBAT;
    private Module selectedModule = null;
    private final ArrayList<Component> activeComponents = new ArrayList<>();

    // --- ANIMATION VARIABLES ---
    private float openProgress = 0.0f;
    private final Map<String, Float> hoverAnims = new HashMap<>();

    public void resetAnimation() {
        this.openProgress = 0.0f;
    }

    @Override
    public void initGui() {
        ScaledResolution sr = new ScaledResolution(mc);
        width = 600;
        height = 400;

        // Jeśli pozycja nie była nigdy ustawiona (0,0), wyśrodkuj
        if (x == 0 && y == 0) {
            x = (sr.getScaledWidth() / 2f) - (width / 2f);
            y = (sr.getScaledHeight() / 2f) - (height / 2f);
        }

        if (selectedModule != null) initComponents();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        // --- FIX: ZABEZPIECZENIE PRZED WYJŚCIEM POZA EKRAN ---
        // To automatycznie naprawi zbugowane GUI jak tylko wejdziesz do gry
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + width > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y + height > sr.getScaledHeight()) y = sr.getScaledHeight() - height;
        // ----------------------------------------------------

        // --- ANIMATE OPENING ---
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
                GL11.glTranslated(0, (1.0f - ease) * height, 0);
                break;
            case "Fade":
                // Alpha logic below
                break;
        }

        // Colors
        int alpha = animMode.equals("Fade") ? (int)(255 * openProgress) : 255;
        if(alpha < 10) alpha = 10;

        Color themeColor = ClickGuiModule.getGuiColor();
        int bgMain = new Color(20, 20, 25, (int)(240 * (alpha/255f))).getRGB();
        int bgSidebar = new Color(15, 15, 18, alpha).getRGB();
        // int bgSettings = new Color(25, 25, 30, alpha).getRGB(); // Nieużywane

        // 1. BACKGROUND
        RenderUtil.drawRoundedRect(x, y, width, height, 10, bgMain);
        if(openProgress > 0.8) {
            RenderUtil.drawGlow(x, y, width, height, 20, new Color(0, 0, 0, (int)(150 * (alpha/255f))).getRGB());
        }

        // 2. SIDEBAR
        float sidebarW = 120;
        RenderUtil.drawRoundedRect(x, y, sidebarW, height, 10, bgSidebar);
        RenderUtil.drawRect(x + sidebarW - 10, y, 10, height, bgSidebar);

        float catY = y + 40;
        for (Module.Category cat : Module.Category.values()) {
            boolean isSelected = (cat == selectedCategory);
            // Category Animation
            float catAnim = getHoverAnim("cat_" + cat.name(), isSelected ? 1.0f : 0.0f);

            if (catAnim > 0.01) {
                int barCol = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(255 * catAnim * (alpha/255f))).getRGB();
                RenderUtil.drawRoundedRect(x + 10, catY, sidebarW - 20, 25, 6, barCol);
            }

            int textColor = isSelected ? -1 : new Color(150, 150, 150, alpha).getRGB();
            FontManager.r20.drawCenteredString(cat.name(), x + sidebarW / 2, catY + 8, textColor);
            catY += 35;
        }

        FontManager.b20.drawCenteredString("DOOM", x + sidebarW/2, y + height - 30, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha).getRGB());

        // 3. MODULE GRID
        float contentX = x + sidebarW;
        float settingsW = 180;
        float gridW = width - sidebarW - (selectedModule != null ? settingsW : 0);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.scissor(contentX, y, gridW, height);

        List<Module> modules = Client.INSTANCE.moduleManager.getModulesByCategory(selectedCategory);
        float modX = contentX + 15;
        float modY = y + 20;
        float modCardW = 130;
        float modCardH = 35;

        for (Module m : modules) {
            if (modX + modCardW > contentX + gridW) {
                modX = contentX + 15;
                modY += modCardH + 10;
            }

            boolean active = m.isToggled();
            boolean selected = (m == selectedModule);
            boolean hovered = mouseX >= modX && mouseX <= modX + modCardW && mouseY >= modY && mouseY <= modY + modCardH;

            // --- ANIMATIONS ---
            float scaleTarget = (hovered && ClickGuiModule.hoverScale.isEnabled()) ? 1.05f : 1.0f;
            float scale = getHoverAnim("scale_" + m.getName(), scaleTarget);
            float toggleAnim = getHoverAnim("toggle_" + m.getName(), active ? 1.0f : 0.0f);

            // Scale from center
            float cx = modX + modCardW/2f;
            float cy = modY + modCardH/2f;

            GL11.glPushMatrix();
            GL11.glTranslated(cx, cy, 0);
            GL11.glScalef(scale, scale, 1f);
            GL11.glTranslated(-cx, -cy, 0);

            int cOff = new Color(40, 40, 45, alpha).getRGB();
            int cOn = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(150 * (alpha/255f))).getRGB();
            int cardColor = RenderUtil.interpolateColor(cOff, cOn, toggleAnim);

            if (selected) {
                RenderUtil.drawRoundedOutline(modX, modY, modCardW, modCardH, 6, 2f, themeColor.getRGB());
            }

            RenderUtil.drawRoundedRect(modX, modY, modCardW, modCardH, 6, cardColor);

            int textC = active ? -1 : new Color(170, 170, 170, alpha).getRGB();
            FontManager.r18.drawStringWithShadow(m.getName(), modX + 10, modY + 12, textC);

            GL11.glPopMatrix();

            modX += modCardW + 10;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 4. SETTINGS PANEL (Slide In)
        float settingsAnim = getHoverAnim("settings_panel", selectedModule != null ? 1.0f : 0.0f);
        if (settingsAnim > 0.01) {
            float setX = x + width - settingsW;
            float slideOffset = settingsW * (1.0f - AnimationUtil.easeOutExpo(settingsAnim));
            float actualSetX = setX + slideOffset;

            // Adjust alpha for settings
            int safeAlpha = (int) Math.max(10, Math.min(255, alpha * settingsAnim));
            int bgSetColor = new Color(25, 25, 30, safeAlpha).getRGB();

            RenderUtil.drawRoundedRect(actualSetX, y, settingsW, height, 10, bgSetColor);
            RenderUtil.drawRect(actualSetX, y, 10, height, bgSetColor);
            RenderUtil.drawRect(actualSetX, y + 10, 1, height - 20, new Color(60,60,60, safeAlpha).getRGB());

            if(settingsAnim > 0.8) {
                FontManager.b20.drawCenteredString(selectedModule != null ? selectedModule.getName() : "", actualSetX + settingsW/2, y + 20, new Color(255,255,255, safeAlpha).getRGB());
                FontManager.r18.drawCenteredString("Settings", actualSetX + settingsW/2, y + 32, new Color(136,136,136, safeAlpha).getRGB());

                float compY = y + 60;
                float compX = actualSetX + 10;

                if (!activeComponents.isEmpty() && selectedModule != null) {
                    // Check if components match selected module (simple check)
                    if(activeComponents.get(0).setting.parent != selectedModule) initComponents();
                } else if (activeComponents.isEmpty() && selectedModule != null && !Client.INSTANCE.settingsManager.getSettingsByMod(selectedModule).isEmpty()) {
                    initComponents();
                }

                for (Component comp : activeComponents) {
                    if (!comp.setting.isVisible()) continue;
                    if (compY + comp.height > y + height) break;
                    comp.draw(compX, compY, mouseX, mouseY);
                    compY += comp.height + 5;
                }
            }
        }

        GL11.glPopMatrix();
    }

    private float getHoverAnim(String key, float target) {
        float current = hoverAnims.getOrDefault(key, 0.0f);
        float next = RenderUtil.lerp(current, target, 0.2f);
        hoverAnims.put(key, next);
        return next;
    }

    private void initComponents() {
        activeComponents.clear();
        float compWidth = 160;
        for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(selectedModule)) {
            if (s instanceof BooleanSetting) activeComponents.add(new BooleanValueComponent((BooleanSetting)s, compWidth, 25));
            else if (s instanceof NumberSetting) activeComponents.add(new NumberValueComponent((NumberSetting)s, compWidth, 25));
            else if (s instanceof ModeSetting) activeComponents.add(new ModeValueComponent((ModeSetting)s, compWidth, 25));
            else if (s instanceof ColorSetting) activeComponents.add(new ColorValueComponent((ColorSetting)s, compWidth, 20));
            else if (s instanceof CategorySetting) activeComponents.add(new CategoryComponent((CategorySetting)s, compWidth, 25));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        float sidebarW = 120;
        // Sidebar interactions
        if (mouseX > x && mouseX < x + sidebarW) {
            float catY = y + 40;
            for (Module.Category cat : Module.Category.values()) {
                if (mouseY > catY && mouseY < catY + 25) {
                    selectedCategory = cat;
                    return;
                }
                catY += 35;
            }
        }

        float contentX = x + sidebarW;
        float settingsW = 180;
        float gridW = width - sidebarW - (selectedModule != null ? settingsW : 0);

        // Grid interactions
        if (mouseX > contentX && mouseX < contentX + gridW) {
            List<Module> modules = Client.INSTANCE.moduleManager.getModulesByCategory(selectedCategory);
            float modX = contentX + 15;
            float modY = y + 20;
            float modCardW = 130;
            float modCardH = 35;

            for (Module m : modules) {
                if (modX + modCardW > contentX + gridW) {
                    modX = contentX + 15;
                    modY += modCardH + 10;
                }
                if (mouseX >= modX && mouseX <= modX + modCardW && mouseY >= modY && mouseY <= modY + modCardH) {
                    if (mouseButton == 0) m.toggle();
                    if (mouseButton == 1) {
                        selectedModule = m;
                        initComponents();
                    }
                    return;
                }
                modX += modCardW + 10;
            }
        }

        // Settings interactions
        if (selectedModule != null && mouseX > x + width - settingsW) {
            for (Component comp : activeComponents) {
                if (comp.setting.isVisible()) comp.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        // Dragging
        if (mouseY >= y && mouseY <= y + 20 && mouseX >= x && mouseX <= x + width) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        if (selectedModule != null) {
            for (Component c : activeComponents) {
                if (c.setting.isVisible()) c.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (selectedModule != null) {
            for (Component c : activeComponents) {
                if (c.setting.isVisible()) c.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override public boolean doesGuiPauseGame() { return false; }
    @Override
    public void onGuiClosed() {
        Module clickGui = Client.INSTANCE.moduleManager.getModule(ClickGuiModule.class);
        if (clickGui != null && clickGui.isToggled()) {
            clickGui.setToggled(false);
        }
        Client.INSTANCE.configManager.save();
    }
}