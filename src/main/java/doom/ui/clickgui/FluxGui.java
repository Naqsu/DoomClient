package doom.ui.clickgui;

import doom.Client;
import doom.module.Module;
import doom.module.impl.render.ClickGuiModule;
import doom.settings.Setting;
import doom.settings.impl.*;
import doom.ui.font.FontManager;
import doom.ui.font.Icons;
import doom.util.AnimationUtil;
import doom.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FluxGui extends GuiScreen {

    // --- CONFIGURATION ---
    private final float CATEGORY_WIDTH = 50;
    private final float MODULE_WIDTH = 240;

    // --- STATE ---
    private Module.Category currentCategory = Module.Category.COMBAT;
    private boolean showingFavorites = false;
    private Module expandedModule = null;

    private float startX = 50;
    private float startY = 100;

    // --- DRAGGING LOGIC ---
    private boolean dragging = false;
    private boolean pendingDrag = false;
    private float dragX, dragY;
    private float clickX, clickY;

    // --- SCROLLING ---
    private float scrollY = 0;
    private float targetScrollY = 0;

    // --- ANIMATIONS ---
    private final Map<Object, Float> animMap = new HashMap<>();
    private float introAnim = 0.0f;

    // --- SEARCH BAR STATE ---
    private String searchString = "";
    private boolean isSearchFocused = true;
    private float searchHoverAnim = 0.0f;

    // --- COLOR PICKER STATE ---
    private boolean draggingHue = false;
    private boolean draggingSat = false;
    private boolean draggingAlpha = false;
    private ColorSetting activePicker = null;

    @Override
    public void initGui() {
        introAnim = 0.0f;
        if (startX == 0 && startY == 0) {
            startX = 50;
            startY = 100;
        }
        Keyboard.enableRepeatEvents(true);
        isSearchFocused = true;
        searchString = "";
    }

    @Override
    public void onGuiClosed() {
        Module clickGui = Client.INSTANCE.moduleManager.getModule(ClickGuiModule.class);
        if (clickGui != null) clickGui.setToggled(false);
        Client.INSTANCE.configManager.save();
        Keyboard.enableRepeatEvents(false);

        dragging = false; pendingDrag = false;
        draggingHue = false; draggingSat = false; draggingAlpha = false; activePicker = null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. DRAGGING
        if (pendingDrag) {
            double dist = Math.hypot(mouseX - clickX, mouseY - clickY);
            if (dist > 5) {
                dragging = true;
                pendingDrag = false;
                dragX = mouseX - startX;
                dragY = mouseY - startY;
            }
        }
        if (dragging) {
            startX = mouseX - dragX;
            startY = mouseY - dragY;
        }

        // 2. SCROLL & ANIM
        scrollY = RenderUtil.lerp(scrollY, targetScrollY, 0.2f);
        introAnim = AnimationUtil.animate(1.0f, introAnim, 0.1f);

        Color themeColor = ClickGuiModule.getGuiColor();

        // 3. MAIN GUI RENDER
        GL11.glPushMatrix();
        float introOffset = (1.0f - introAnim) * -50;
        GL11.glTranslatef(introOffset, 0, 0);

        drawCategoryBar(mouseX, mouseY, themeColor);
        drawModuleList(mouseX, mouseY, themeColor);

        GL11.glPopMatrix();

        // 4. RENDEROWANIE PASKÓW NA GÓRZE (TOOLBAR)
        drawTopToolbar(mouseX, mouseY, themeColor);
    }

    private void drawTopToolbar(int mouseX, int mouseY, Color themeColor) {
        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();

        // Wymiary elementów
        float searchW = 300;
        float btnSize = 26;
        float gap = 8;

        // Całkowita szerokość (Search + 3 przyciski + odstępy)
        float totalW = searchW + (3 * (btnSize + gap));

        // Start X (wyśrodkowane)
        float startX = (screenW / 2) - (totalW / 2);
        float barY = 15;

        // --- 1. SEARCH BAR ---
        drawSearchBar(startX, barY, searchW, btnSize, mouseX, mouseY, themeColor);

        // --- 2. PRZYCISKI (SETTINGS, HUD, ALTS) ---
        float btnStartX = startX + searchW + gap;

        drawTopButton(btnStartX, barY, btnSize, mouseX, mouseY, "Settings", themeColor);
        drawTopButton(btnStartX + btnSize + gap, barY, btnSize, mouseX, mouseY, "HudEditor", themeColor);
        drawTopButton(btnStartX + (btnSize + gap) * 2, barY, btnSize, mouseX, mouseY, "AltManager", themeColor);
    }

    private void drawSearchBar(float x, float y, float w, float h, int mouseX, int mouseY, Color themeColor) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        float targetAnim = (hovered || isSearchFocused) ? 1.0f : 0.0f;
        searchHoverAnim = RenderUtil.lerp(searchHoverAnim, targetAnim, 0.2f);

        // BLUR & TŁO
        RenderUtil.drawBlur(x, y, w, h, 15);
        int bgCol = new Color(20, 20, 20, 180).getRGB();
        RenderUtil.drawRoundedRect(x, y, w, h, 8, bgCol);

        // OBRYS
        int outlineColor = RenderUtil.interpolateColor(new Color(60, 60, 60).getRGB(), themeColor.getRGB(), searchHoverAnim);
        RenderUtil.drawRoundedOutline(x, y, w, h, 8, 1.0f, outlineColor);

        // LUPA
        FontManager.icons18.drawStringWithShadow(Icons.SEARCH, x + 10, y + 9, new Color(180, 180, 180).getRGB());

        // TEKST
        if (searchString.isEmpty()) {
            FontManager.r18.drawStringWithShadow("Type to search...", x + 28, y + 9, new Color(100, 100, 100).getRGB());
        } else {
            FontManager.r18.drawStringWithShadow(searchString, x + 28, y + 9, -1);
        }

        if (isSearchFocused) {
            String cursor = (System.currentTimeMillis() % 1000 > 500) ? "_" : "";
            float cursorOffset = searchString.isEmpty() ? 0 : FontManager.r18.getStringWidth(searchString);
            FontManager.r18.drawStringWithShadow(cursor, x + 28 + cursorOffset, y + 9, -1);
        }

        // CLEAR (X)
        if (!searchString.isEmpty()) {
            float crossX = x + w - 20;
            FontManager.icons18.drawStringWithShadow(Icons.CROSS, crossX, y + 9, new Color(150, 150, 150).getRGB());
            if (Mouse.isButtonDown(0) && mouseX >= crossX - 5 && mouseX <= crossX + 15 && mouseY >= y && mouseY <= y + h) {
                searchString = "";
            }
        }
    }

    private void drawTopButton(float x, float y, float size, int mouseX, int mouseY, String type, Color themeColor) {
        boolean hovered = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
        float anim = getAnim("top_btn_" + type, hovered ? 1.0f : 0.0f);

        // BLUR & TŁO
        RenderUtil.drawBlur(x, y, size, size, 10);
        int bgCol = new Color(20, 20, 20, 180).getRGB();
        RenderUtil.drawRoundedRect(x, y, size, size, 6, bgCol);

        // OBRYS (Animowany)
        int outlineColor = RenderUtil.interpolateColor(new Color(60, 60, 60).getRGB(), themeColor.getRGB(), anim);
        RenderUtil.drawRoundedOutline(x, y, size, size, 6, 1.0f, outlineColor);

        // IKONA
        String icon = "";
        switch (type) {
            case "Settings": icon = Icons.SETTINGS; break;
            case "HudEditor": icon = Icons.HUD_EDITOR; break;
            case "AltManager": icon = Icons.ALT_MANAGER; break;
        }

        int iconColor = RenderUtil.interpolateColor(new Color(180, 180, 180).getRGB(), -1, anim);
        FontManager.icons18.drawCenteredString(icon, x + size/2, y + size/2 - 4, iconColor);
    }

    private void drawCategoryBar(int mouseX, int mouseY, Color themeColor) {
        // Wysokość tylko dla kategorii i ulubionych (bez przycisków na dole)
        float totalCatsHeight = (Module.Category.values().length * 45) + 50;

        RenderUtil.drawBlur(startX, startY, CATEGORY_WIDTH, totalCatsHeight, 15);
        RenderUtil.drawRoundedRect(startX, startY, CATEGORY_WIDTH, totalCatsHeight, 8, new Color(10, 10, 10, 150).getRGB());

        int c1 = themeColor.getRGB();
        int c2 = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0).getRGB();
        RenderUtil.drawGradientRect(startX, startY, 2, totalCatsHeight, c1, c1, c2, c2);

        // --- 1. ULUBIONE (GWIAZDKA) ---
        float favY = startY + 10;
        boolean favHover = mouseX >= startX && mouseX <= startX + CATEGORY_WIDTH && mouseY >= favY && mouseY <= favY + 35;
        float favAnim = getAnim("cat_FAVORITES", showingFavorites ? 1.0f : (favHover ? 0.5f : 0.0f));

        if (favAnim > 0.01) {
            int goldColor = new Color(255, 215, 0, (int)(40 * favAnim)).getRGB();
            RenderUtil.drawRoundedRect(startX + 5, favY, CATEGORY_WIDTH - 10, 35, 6, goldColor);
        }

        int starColor = showingFavorites ? new Color(255, 215, 0).getRGB() :
                RenderUtil.interpolateColor(new Color(150, 150, 150).getRGB(), new Color(255, 255, 100).getRGB(), favAnim);

        // Używamy Icons.STAR zdefiniowanego w Icons.java
        FontManager.icons24.drawCenteredString(Icons.STAR, startX + CATEGORY_WIDTH/2, favY + 10, starColor);

        RenderUtil.drawRect(startX + 10, favY + 40, CATEGORY_WIDTH - 20, 1, new Color(255,255,255,30).getRGB());

        // --- 2. KATEGORIE ---
        float catY = favY + 50;
        for (Module.Category cat : Module.Category.values()) {
            boolean selected = (cat == currentCategory) && !showingFavorites && searchString.isEmpty();
            boolean hovered = mouseX >= startX && mouseX <= startX + CATEGORY_WIDTH && mouseY >= catY && mouseY <= catY + 45;
            float anim = getAnim(cat, selected ? 1.0f : (hovered ? 0.5f : 0.0f));

            if (anim > 0.01) {
                int bgCol = new Color(255, 255, 255, (int)(30 * anim)).getRGB();
                RenderUtil.drawRoundedRect(startX + 5, catY + 5, CATEGORY_WIDTH - 10, 35, 6, bgCol);
            }

            String icon = getCategoryIcon(cat);
            int iconColor = RenderUtil.interpolateColor(new Color(150, 150, 150).getRGB(), themeColor.getRGB(), anim);

            GL11.glPushMatrix();
            float scale = 1.0f + (0.2f * anim);
            float cx = startX + CATEGORY_WIDTH/2;
            float cy = catY + 22.5f;
            GL11.glTranslatef(cx, cy, 0);
            GL11.glScalef(scale, scale, 1);
            GL11.glTranslatef(-cx, -cy, 0);
            FontManager.icons24.drawCenteredString(icon, cx, cy - 6, iconColor);
            GL11.glPopMatrix();

            catY += 45;
        }
    }

    private void drawModuleList(int mouseX, int mouseY, Color themeColor) {
        float listX = startX + CATEGORY_WIDTH + 10;
        float listY = startY;
        float listH = 350;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.scissor(listX - 15, listY - 15, MODULE_WIDTH + 30, listH + 30);

        float currentY = listY + scrollY;

        List<Module> modules;
        if (!searchString.isEmpty()) {
            modules = Client.INSTANCE.moduleManager.getModules().stream()
                    .filter(m -> m.getName().toLowerCase().contains(searchString.toLowerCase()))
                    .collect(Collectors.toList());
        } else if (showingFavorites) {
            modules = Client.INSTANCE.moduleManager.getModules().stream()
                    .filter(Module::isFavorite)
                    .collect(Collectors.toList());
        } else {
            modules = Client.INSTANCE.moduleManager.getModulesByCategory(currentCategory);
        }

        if (showingFavorites && modules.isEmpty() && searchString.isEmpty()) {
            FontManager.r18.drawCenteredString("No favorites yet.", listX + MODULE_WIDTH/2, listY + 50, new Color(150, 150, 150).getRGB());
            FontManager.r18.drawCenteredString("Middle-click modules to add!", listX + MODULE_WIDTH/2, listY + 65, new Color(100, 100, 100).getRGB());
        }

        float totalHeight = 0;
        for(Module m : modules) {
            totalHeight += 35;
            float expandAnim = getAnim(m.getName() + "_expand", expandedModule == m ? 1.0f : 0.0f);
            if (expandAnim > 0.01) totalHeight += (getSettingsHeight(m) * expandAnim);
            totalHeight += 5;
        }

        float maxScroll = Math.max(0, totalHeight - listH);
        if (targetScrollY > 0) targetScrollY = 0;
        if (targetScrollY < -maxScroll) targetScrollY = -maxScroll;

        for (Module m : modules) {
            float modH = 35;
            float expandAnim = getAnim(m.getName() + "_expand", expandedModule == m ? 1.0f : 0.0f);
            float settingsH = getSettingsHeight(m) * expandAnim;

            if (currentY + modH + settingsH > listY && currentY < listY + listH) {
                drawModule(m, listX, currentY, MODULE_WIDTH, modH, settingsH, mouseX, mouseY, themeColor, expandAnim);
            }
            currentY += modH + settingsH + 5;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawModule(Module m, float x, float y, float w, float h, float settingsH, int mouseX, int mouseY, Color themeColor, float expandAnim) {
        boolean toggled = m.isToggled();
        float toggleAnim = getAnim(m, toggled ? 1.0f : 0.0f);

        int bgBase = new Color(20, 20, 20, 200).getRGB();
        int bgActive = new Color(30, 30, 35, 230).getRGB();
        int color = RenderUtil.interpolateColor(bgBase, bgActive, toggleAnim);

        RenderUtil.drawBlur(x, y, w, h + settingsH, 10);
        RenderUtil.drawRoundedRect(x, y, w, h + settingsH, 6, color);

        if (toggleAnim > 0.01) {
            int glowColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(150 * toggleAnim)).getRGB();
            RenderUtil.drawGlow(x, y, w, h, 15, glowColor);
        }

        int textColor = toggled ? -1 : new Color(180, 180, 180).getRGB();
        FontManager.b18.drawStringWithShadow(m.getName(), x + 10, y + 12, textColor);

        if (m.isFavorite()) {
            float nameW = FontManager.b18.getStringWidth(m.getName());
            FontManager.icons18.drawStringWithShadow(Icons.STAR, x + 12 + nameW, y + 13, new Color(255, 215, 0).getRGB());
        }

        float switchW = 24;
        float switchH = 12;
        float switchX = x + w - switchW - 10;
        float switchY = y + 11.5f;

        int switchBg = RenderUtil.interpolateColor(new Color(60, 60, 60).getRGB(), themeColor.getRGB(), toggleAnim);
        RenderUtil.drawRoundedRect(switchX, switchY, switchW, switchH, 6, switchBg);

        float knobX = switchX + 2 + (12 * toggleAnim);
        if (toggled) RenderUtil.drawRadialGlow(knobX + 4, switchY + 6, 8, 3.0f, themeColor.getRGB());
        RenderUtil.drawCircle(knobX + 4, switchY + 6, 4, -1);

        if (!Client.INSTANCE.settingsManager.getSettingsByMod(m).isEmpty()) {
            GL11.glPushMatrix();
            float arrowX = x + w - 50;
            float arrowY = y + 12;
            GL11.glTranslatef(arrowX, arrowY + 4, 0);
            GL11.glRotatef(90 * expandAnim, 0, 0, 1);
            GL11.glTranslatef(-arrowX, -(arrowY + 4), 0);
            FontManager.r18.drawStringWithShadow(">", arrowX, arrowY, new Color(150, 150, 150).getRGB());
            GL11.glPopMatrix();
        }

        if (expandAnim > 0.01) {
            drawSettings(m, x, y + h, w, settingsH, mouseX, mouseY, themeColor);
        }
    }

    private void drawSettings(Module m, float x, float y, float w, float h, int mouseX, int mouseY, Color themeColor) {
        float curY = y + 5;

        for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(m)) {
            if (!s.isVisible()) continue;

            float setX = x + 10;
            float setW = w - 20;

            if (s instanceof BooleanSetting) {
                BooleanSetting bool = (BooleanSetting) s;
                FontManager.r18.drawStringWithShadow(s.name, setX, curY, -1);
                float swW = 24, swH = 12; float swX = setX + setW - swW;
                boolean active = bool.isEnabled();
                float anim = getAnim("set_" + s.name, active ? 1.0f : 0.0f);
                int color = RenderUtil.interpolateColor(new Color(60,60,60).getRGB(), themeColor.getRGB(), anim);
                RenderUtil.drawRoundedRect(swX, curY + 1, swW, swH, 6, color);
                float kX = swX + 2 + (12 * anim);
                if(active) RenderUtil.drawRadialGlow(kX + 4, curY + 7, 8, 3.0f, themeColor.getRGB());
                RenderUtil.drawCircle(kX + 4, curY + 7, 4, -1);

                if (Mouse.isButtonDown(0) && isHovered(setX, curY, setW, 14, mouseX, mouseY) && !dragging) {
                    if (getAnim("click_" + s.name, 0) == 0) {
                        bool.toggle();
                        animMap.put("click_" + s.name, 1.0f);
                    }
                } else {
                    animMap.put("click_" + s.name, 0.0f);
                }
            }
            else if (s instanceof NumberSetting) {
                NumberSetting num = (NumberSetting) s;
                String val = String.format("%.2f", num.getValue());
                FontManager.r18.drawStringWithShadow(s.name, setX, curY, -1);
                FontManager.r18.drawStringWithShadow(val, setX + setW - FontManager.r18.getStringWidth(val), curY, new Color(150,150,150).getRGB());
                float slideY = curY + 12;
                RenderUtil.drawRoundedRect(setX, slideY, setW, 4, 2, new Color(50,50,50).getRGB());
                float range = (float)(num.getMax() - num.getMin());
                float percent = (float)((num.getValue() - num.getMin()) / range);
                RenderUtil.drawRoundedRect(setX, slideY, setW * percent, 4, 2, themeColor.getRGB());
                float kbX = setX + (setW * percent);
                RenderUtil.drawRadialGlow(kbX, slideY + 2, 8, 3.0f, themeColor.getRGB());
                RenderUtil.drawCircle(kbX, slideY + 2, 3, -1);

                if (Mouse.isButtonDown(0) && isHovered(setX, curY, setW, 20, mouseX, mouseY)) {
                    double newVal = num.getMin() + ((mouseX - setX) / setW) * range;
                    num.setValue(newVal);
                }
            }
            else if (s instanceof ModeSetting) {
                ModeSetting mode = (ModeSetting) s;
                FontManager.r18.drawStringWithShadow(s.name, setX, curY, -1);
                FontManager.r18.drawStringWithShadow(mode.getMode(), setX + setW - FontManager.r18.getStringWidth(mode.getMode()), curY, themeColor.getRGB());
            }
            else if (s instanceof ColorSetting) {
                drawColorPicker((ColorSetting)s, setX, curY, setW, mouseX, mouseY);
            }

            curY += getSettingHeight(s);
        }
    }

    private void drawColorPicker(ColorSetting s, float x, float y, float w, int mouseX, int mouseY) {
        FontManager.r18.drawStringWithShadow(s.name, x, y + 2, -1);
        float previewW = 20;
        float previewX = x + w - previewW;
        RenderUtil.drawRoundedRect(previewX, y, previewW, 10, 3, s.getColor());

        if (isHovered(x, y, w, 15, mouseX, mouseY) && Mouse.isButtonDown(1)) {
            if (getAnim("click_col_" + s.name, 0) == 0) {
                s.expanded = !s.expanded;
                animMap.put("click_col_" + s.name, 1.0f);
            }
        } else {
            animMap.put("click_col_" + s.name, 0.0f);
        }

        if (s.expanded) {
            float pickerY = y + 15;
            float[] hsb = s.getHSBA();

            float boxH = 60;
            int hueColor = java.awt.Color.HSBtoRGB(hsb[0], 1.0f, 1.0f);
            RenderUtil.drawGradientRectHorizontal(x, pickerY, w, boxH, -1, hueColor);
            RenderUtil.drawGradientRect(x, pickerY, w, boxH, 0, 0, 0xFF000000, 0xFF000000);

            float satX = x + (hsb[1] * w);
            float briY = pickerY + ((1.0f - hsb[2]) * boxH);
            RenderUtil.drawRoundedOutline(satX - 2, briY - 2, 4, 4, 2, 1.0f, -1);

            if (Mouse.isButtonDown(0)) {
                if (activePicker == null && isHovered(x, pickerY, w, boxH, mouseX, mouseY)) {
                    activePicker = s; draggingSat = true;
                }
            }
            if (activePicker == s && draggingSat) {
                float sat = (mouseX - x) / w;
                float bri = 1.0f - ((mouseY - pickerY) / boxH);
                s.setHSBA(hsb[0], Math.max(0, Math.min(1, sat)), Math.max(0, Math.min(1, bri)), hsb[3]);
            }

            float hueY = pickerY + boxH + 5;
            float hueH = 10;
            float segW = w / 4;
            RenderUtil.drawGradientRectHorizontal(x, hueY, segW, hueH, 0xFFFF0000, 0xFF00FF00);
            RenderUtil.drawGradientRectHorizontal(x + segW, hueY, segW, hueH, 0xFF00FF00, 0xFF0000FF);
            RenderUtil.drawGradientRectHorizontal(x + segW*2, hueY, segW, hueH, 0xFF0000FF, 0xFFFF00FF);
            RenderUtil.drawGradientRectHorizontal(x + segW*3, hueY, segW, hueH, 0xFFFF00FF, 0xFFFF0000);

            float hueX = x + (hsb[0] * w);
            RenderUtil.drawRect(hueX - 1, hueY, 2, hueH, -1);

            if (Mouse.isButtonDown(0)) {
                if (activePicker == null && isHovered(x, hueY, w, hueH, mouseX, mouseY)) {
                    activePicker = s; draggingHue = true;
                }
            }
            if (activePicker == s && draggingHue) {
                float hue = (mouseX - x) / w;
                s.setHSBA(Math.max(0, Math.min(1, hue)), hsb[1], hsb[2], hsb[3]);
            }

            float alphaY = hueY + hueH + 5;
            RenderUtil.drawRect(x, alphaY, w, hueH, 0xFF808080);
            RenderUtil.drawGradientRectHorizontal(x, alphaY, w, hueH, 0x00FFFFFF, s.getColor() | 0xFF000000);

            float alphaX = x + (hsb[3] * w);
            RenderUtil.drawRect(alphaX - 1, alphaY, 2, hueH, -1);

            if (Mouse.isButtonDown(0)) {
                if (activePicker == null && isHovered(x, alphaY, w, hueH, mouseX, mouseY)) {
                    activePicker = s; draggingAlpha = true;
                }
            }
            if (activePicker == s && draggingAlpha) {
                float alpha = (mouseX - x) / w;
                s.setHSBA(hsb[0], hsb[1], hsb[2], Math.max(0, Math.min(1, alpha)));
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // --- KLIKANIE NA PASKU GÓRNYM (SEARCH + BUTTONS) ---
        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float searchW = 300; float btnSize = 26; float gap = 8;
        float totalW = searchW + (3 * (btnSize + gap));
        float barX = (screenW / 2) - (totalW / 2);
        float barY = 15;

        // 1. SEARCH FOCUS
        if (mouseX >= barX && mouseX <= barX + searchW && mouseY >= barY && mouseY <= barY + btnSize) {
            isSearchFocused = true; return;
        } else {
            if (mouseButton == 0) isSearchFocused = false;
        }

        // 2. GÓRNE PRZYCISKI
        float btnStartX = barX + searchW + gap;
        if (mouseButton == 0) {
            if (isHoveredButton(btnStartX, barY, btnSize, mouseX, mouseY)) { mc.displayGuiScreen(new GuiConfigMenu(this)); return; }
            if (isHoveredButton(btnStartX + btnSize + gap, barY, btnSize, mouseX, mouseY)) { mc.displayGuiScreen(new doom.ui.hudeditor.GuiHudEditor()); return; }
            if (isHoveredButton(btnStartX + (btnSize + gap) * 2, barY, btnSize, mouseX, mouseY)) { mc.displayGuiScreen(new doom.ui.alt.GuiAltManager(this)); return; }
        }

        // --- DRAG SIDEBAR (WITH THRESHOLD) ---
        if (isHovered(startX, startY, CATEGORY_WIDTH, 200, mouseX, mouseY)) {
            clickX = mouseX; clickY = mouseY; pendingDrag = true;

            // Kliknięcie w Ulubione
            float favY = startY + 10;
            if (mouseY >= favY && mouseY <= favY + 35) {
                showingFavorites = true;
                searchString = ""; targetScrollY = 0; scrollY = 0;
                return;
            }

            // Kliknięcie w Kategorie
            float catY = favY + 50;
            for (Module.Category cat : Module.Category.values()) {
                if (mouseY >= catY && mouseY <= catY + 45) {
                    currentCategory = cat;
                    showingFavorites = false;
                    searchString = ""; targetScrollY = 0; scrollY = 0;
                    return;
                }
                catY += 45;
            }
            return;
        }

        float listX = startX + CATEGORY_WIDTH + 10;
        float currentY = startY + scrollY;

        List<Module> modules;
        if (!searchString.isEmpty()) {
            modules = Client.INSTANCE.moduleManager.getModules().stream()
                    .filter(m -> m.getName().toLowerCase().contains(searchString.toLowerCase()))
                    .collect(Collectors.toList());
        } else if (showingFavorites) {
            modules = Client.INSTANCE.moduleManager.getModules().stream()
                    .filter(Module::isFavorite)
                    .collect(Collectors.toList());
        } else {
            modules = Client.INSTANCE.moduleManager.getModulesByCategory(currentCategory);
        }

        for (Module m : modules) {
            float modH = 35;
            float expandAnim = getAnim(m.getName() + "_expand", expandedModule == m ? 1.0f : 0.0f);
            float settingsH = getSettingsHeight(m) * expandAnim;
            float totalH = modH + settingsH;

            if (isHovered(listX, currentY, MODULE_WIDTH, modH, mouseX, mouseY)) {
                if (mouseButton == 0) m.toggle();
                else if (mouseButton == 1) expandedModule = (expandedModule == m) ? null : m;
                else if (mouseButton == 2) m.toggleFavorite(); // Middle Click -> Ulubione
                return;
            }

            if (expandedModule == m && isHovered(listX, currentY + modH, MODULE_WIDTH, settingsH, mouseX, mouseY)) {
                float setY = currentY + modH + 5;
                for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(m)) {
                    if (!s.isVisible()) continue;

                    if (s instanceof ModeSetting) {
                        float sH = getSettingHeight(s);
                        if (mouseY >= setY && mouseY <= setY + sH && mouseButton == 0) ((ModeSetting)s).cycle();
                    }
                    setY += getSettingHeight(s);
                }
                return;
            }
            currentY += totalH + 5;
        }
    }

    private boolean isHoveredButton(float x, float y, float size, int mx, int my) {
        return mx >= x && mx <= x + size && my >= y && my <= y + size;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false; pendingDrag = false;
        draggingHue = false; draggingSat = false; draggingAlpha = false; activePicker = null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (!searchString.isEmpty()) {
                searchString = "";
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }

        if (isSearchFocused) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (searchString.length() > 0) {
                    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                        String trimmed = searchString.trim();
                        int lastSpace = trimmed.lastIndexOf(' ');
                        if (lastSpace == -1) searchString = ""; else searchString = trimmed.substring(0, lastSpace);
                    } else {
                        searchString = searchString.substring(0, searchString.length() - 1);
                    }
                }
            } else if (keyCode == Keyboard.KEY_RETURN) {
                isSearchFocused = false;
            } else if (isAllowedCharacter(typedChar)) {
                searchString += typedChar;
            }
            scrollY = 0; targetScrollY = 0;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean isAllowedCharacter(char character) {
        return character >= 32 && character != 127;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel < 0) targetScrollY -= 30; else if (wheel > 0) targetScrollY += 30;
    }

    // --- HELPERY ---
    private float getSettingsHeight(Module m) {
        float h = 0;
        for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(m)) {
            if (s.isVisible()) h += getSettingHeight(s);
        }
        return h + 5;
    }
    private float getSettingHeight(Setting s) {
        if (s instanceof NumberSetting) return 22;
        if (s instanceof ColorSetting && ((ColorSetting)s).expanded) return 100;
        return 16;
    }
    private float getAnim(Object key, float target) {
        float current = animMap.getOrDefault(key, 0.0f);
        if (Math.abs(target - current) < 0.001) return target;
        float next = RenderUtil.lerp(current, target, 0.2f);
        animMap.put(key, next);
        return next;
    }
    private boolean isHovered(float x, float y, float w, float h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    private String getCategoryIcon(Module.Category cat) {
        switch (cat) {
            case COMBAT: return Icons.COMBAT;
            case MOVEMENT: return Icons.MOVEMENT;
            case RENDER: return Icons.RENDER;
            case PLAYER: return Icons.PLAYER;
            case MISC: return Icons.MISC;
            default: return Icons.SETTINGS;
        }
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}