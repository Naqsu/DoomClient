package doom.ui.clickgui;

import doom.Client;
import doom.module.Module;
import doom.module.impl.render.ClickGuiModule;
import doom.settings.Setting;
import doom.settings.impl.*;
import doom.ui.clickgui.components.*;
import doom.ui.clickgui.components.Component;
import doom.ui.font.FontManager;
import doom.ui.font.Icons;
import doom.ui.hudeditor.GuiHudEditor;
import doom.util.AnimationUtil;
import doom.util.RenderUtil;
import net.minecraft.client.Minecraft;
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

    // --- SEARCH BAR VARIABLES ---
    private String searchString = "";
    private boolean isSearchFocused = true;
    private float searchHoverAnim = 0.0f;

    @Override
    public void initGui() {
        openProgress = 0.0f;
        frameWidth = 700;
        frameHeight = 450;

        // Wyśrodkowanie przy pierwszym uruchomieniu
        if (frameX == 0 && frameY == 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            frameX = (sr.getScaledWidth() / 2f) - (frameWidth / 2f);
            frameY = (sr.getScaledHeight() / 2f) - (frameHeight / 2f);
        }

        // Auto-focus na start
        isSearchFocused = true;
        Keyboard.enableRepeatEvents(true);

        if (selectedModule != null) initComponents();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        Module clickGui = Client.INSTANCE.moduleManager.getModule(ClickGuiModule.class);
        if (clickGui != null && clickGui.isToggled()) clickGui.setToggled(false);
        Client.INSTANCE.configManager.save();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (dragging) {
            frameX = mouseX - dragX;
            frameY = mouseY - dragY;
        }
        handleScroll();

        // --- CLAMP ---
        ScaledResolution sr = new ScaledResolution(mc);
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        if (frameX < 0) frameX = 0;
        if (frameY < 0) frameY = 0;
        if (frameX + frameWidth > screenW) frameX = screenW - frameWidth;
        if (frameY + frameHeight > screenH) frameY = screenH - frameHeight;

        // Animacja otwierania
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

        // Glow
        if (openProgress > 0.8) {
            RenderUtil.drawGlow(frameX, frameY, frameWidth, frameHeight, 15, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 100).getRGB());
        }

        // Tło okna
        RenderUtil.drawRoundedRect(frameX, frameY, frameWidth, frameHeight, 12, bgCol);
        RenderUtil.drawRoundedOutline(frameX, frameY, frameWidth, frameHeight, 12, 1.5f, new Color(255, 255, 255, 30).getRGB());

        // Sidebar
        drawSidebar(mouseX, mouseY, themeColor, alpha);

        // Obszar Contentu
        float contentX = frameX + sidebarWidth;
        float contentY = frameY + 60; // Miejsce na pasek wyszukiwania
        float contentW = frameWidth - sidebarWidth;
        float contentH = frameHeight - 60;

        // Rysowanie Search Bara (PREMIUM LOOK)
        if (selectedModule == null) {
            drawPremiumSearchBar(contentX, frameY + 15, contentW, mouseX, mouseY, themeColor);
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.scissor(contentX, contentY, contentW, contentH - 10);

        if (selectedModule == null) {
            drawModuleGrid(contentX, contentY, contentW, mouseX, mouseY, alpha);
        } else {
            drawSettingsPanel(contentX, contentY - 50, contentW, mouseX, mouseY, alpha);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
    }

    private void drawSidebar(int mouseX, int mouseY, Color theme, int alpha) {
        float startX = frameX + 20;

        // 1. TITLE
        FontManager.b20.drawStringWithShadow("DOOM", frameX + 20, frameY + 20, new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), alpha).getRGB());

        // 2. USER INFO
        String username = mc.session.getUsername();
        String stats = Minecraft.getDebugFPS() + " FPS";

        FontManager.r18.drawStringWithShadow("User: \u00A7f" + username, frameX + 20, frameY + 38, new Color(150, 150, 150, alpha).getRGB());
        FontManager.r18.drawStringWithShadow(stats, frameX + 20, frameY + 48, new Color(100, 100, 100, alpha).getRGB());

        // 3. CATEGORIES
        float catY = frameY + 80;

        for (Module.Category cat : Module.Category.values()) {
            boolean isSelected = (cat == selectedCategory) && searchString.isEmpty();
            float anim = getAnim("cat_" + cat.name(), isSelected ? 1.0f : 0.0f);

            float xOffset = 5 * anim;
            int baseColorVal = (!searchString.isEmpty()) ? 100 : 150;
            int color = RenderUtil.interpolateColor(new Color(baseColorVal, baseColorVal, baseColorVal, alpha).getRGB(), -1, anim);

            if (anim > 0.05) {
                int barC = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), (int)(255 * anim * (alpha/255f))).getRGB();
                RenderUtil.drawRoundedRect(startX, catY + 2, 2, 14, 1.0f, barC);
            }

            String icon = "";
            switch(cat) {
                case COMBAT: icon = Icons.COMBAT; break;
                case MOVEMENT: icon = Icons.MOVEMENT; break;
                case RENDER: icon = Icons.RENDER; break;
                case PLAYER: icon = Icons.PLAYER; break;
                case MISC: icon = Icons.MISC; break;
            }

            FontManager.icons18.drawStringWithShadow(icon, startX + 10 + xOffset, catY + 6, color);
            FontManager.r20.drawStringWithShadow(cat.name(), startX + 35 + xOffset, catY + 6, color);

            catY += 35;
        }

        // 4. BOTTOM BUTTONS
        float bottomY = frameY + frameHeight - 40;
        float iconX = startX + 10;

        drawIconButton(iconX, bottomY, mouseX, mouseY, "Settings", theme);
        drawIconButton(iconX + 35, bottomY, mouseX, mouseY, "HudEditor", theme);
        drawIconButton(iconX + 70, bottomY, mouseX, mouseY, "AltManager", theme);
    }

    // --- ULEPSZONY SEARCH BAR ---
    private void drawPremiumSearchBar(float x, float y, float w, int mouseX, int mouseY, Color theme) {
        float padding = 25; // Większy odstęp od krawędzi
        float searchH = 30; // Wyższy pasek
        float actualX = x + padding;
        float actualW = w - (padding * 2);

        boolean isHovered = mouseX >= actualX && mouseX <= actualX + actualW && mouseY >= y && mouseY <= y + searchH;

        // Animacja
        float targetAnim = (isHovered || isSearchFocused) ? 1.0f : 0.0f;
        searchHoverAnim = RenderUtil.lerp(searchHoverAnim, targetAnim, 0.2f);

        // Kolory (ciemniejsze, bardziej eleganckie)
        int bgNormal = new Color(22, 22, 26, 255).getRGB();
        int bgActive = new Color(30, 30, 35, 255).getRGB();
        int finalBg = RenderUtil.interpolateColor(bgNormal, bgActive, searchHoverAnim);

        // Obrys tylko na dole lub subtelny dookoła
        int outlineNormal = new Color(50, 50, 50).getRGB();
        int outlineActive = theme.getRGB();
        int finalOutline = RenderUtil.interpolateColor(outlineNormal, outlineActive, searchHoverAnim);

        // Rysowanie Tła
        RenderUtil.drawRoundedRect(actualX, y, actualW, searchH, 8, finalBg);

        // Subtelny cień pod paskiem
        RenderUtil.drawGlow(actualX, y + 2, actualW, searchH, 12, new Color(0,0,0,100).getRGB());

        // Akcentowy obrys (bardziej widoczny przy aktywności)
        if (searchHoverAnim > 0.05f) {
            int glowAlpha = (int)(80 * searchHoverAnim);
            int glowColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), glowAlpha).getRGB();
            // Glow w kolorze theme
            RenderUtil.drawGlow(actualX, y, actualW, searchH, 8, glowColor);
            RenderUtil.drawRoundedOutline(actualX, y, actualW, searchH, 8, 1.0f, finalOutline);
        } else {
            // Ciemny obrys w spoczynku
            RenderUtil.drawRoundedOutline(actualX, y, actualW, searchH, 8, 1.0f, finalOutline);
        }

        // IKONA LUPY
        FontManager.icons18.drawStringWithShadow(Icons.SEARCH, actualX + 10, y + 11, new Color(180, 180, 180).getRGB());

        // TEKST
        if (searchString.isEmpty() && !isSearchFocused) {
            FontManager.r20.drawStringWithShadow("Type to search...", actualX + 30, y + 11, new Color(90, 90, 90).getRGB());
        } else {
            String cursor = (isSearchFocused && System.currentTimeMillis() % 1000 > 500) ? "_" : "";
            // Używamy naszej metody trimString
            String displayString = trimString(searchString, (int)(actualW - 60)); // -60 na ikonki
            FontManager.r20.drawStringWithShadow(displayString + cursor, actualX + 30, y + 11, -1);
        }

        // IKONA CLEAR (X) - Pojawia się gdy jest tekst
        if (!searchString.isEmpty()) {
            float crossX = actualX + actualW - 20;
            float crossY = y + 11;
            boolean hoverX = mouseX >= crossX - 5 && mouseX <= crossX + 10 && mouseY >= crossY - 5 && mouseY <= crossY + 10;
            int crossColor = hoverX ? new Color(255, 100, 100).getRGB() : new Color(100, 100, 100).getRGB();

            FontManager.icons18.drawStringWithShadow(Icons.CROSS, crossX, crossY, crossColor);

            // Obsługa kliknięcia w X wewnątrz draw (dla uproszczenia, choć lepiej w mouseClicked)
            if (hoverX && Mouse.isButtonDown(0)) {
                searchString = "";
                // Mały delay żeby nie spamowało
            }
        }
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
            case "Settings": iconChar = Icons.SETTINGS; fontSize = 1.0f; break;
            case "HudEditor": iconChar = Icons.HUD_EDITOR; fontSize = 1.0f; break;
            case "AltManager": iconChar = Icons.ALT_MANAGER; fontSize = 1.0f; break;
        }

        int iconNormal = new Color(160, 160, 170).getRGB();
        int iconHover = -1;
        int finalIconColor = RenderUtil.interpolateColor(iconNormal, iconHover, anim);

        FontManager.icons18.drawCenteredString(iconChar, x + 12, y + 9 + fontSize, finalIconColor);
    }

    private void drawModuleGrid(float x, float y, float w, int mouseX, int mouseY, int alpha) {
        List<Module> mods;

        if (!searchString.isEmpty()) {
            mods = Client.INSTANCE.moduleManager.getModules().stream()
                    .filter(m -> m.getName().toLowerCase().contains(searchString.toLowerCase()))
                    .collect(Collectors.toList());

            if (mods.isEmpty()) {
                FontManager.r20.drawCenteredString("No modules found for \"" + searchString + "\"", x + w/2, y + 50, new Color(150, 150, 150).getRGB());
                return;
            }
        } else {
            mods = Client.INSTANCE.moduleManager.getModulesByCategory(selectedCategory);
        }

        float padding = 20;
        float modW = (w - (padding * 3)) / 2;
        float modH = 40;
        float gap = 15;

        int rows = (int) Math.ceil(mods.size() / 2.0);
        float totalContentHeight = (rows * (modH + gap));
        float viewHeight = frameHeight - 60;
        maxScroll = Math.max(0, totalContentHeight - viewHeight);

        int i = 0;
        for (Module m : mods) {
            int col = i % 2;
            int row = i / 2;
            float btnX = x + padding + (col * (modW + gap));
            float btnY = y + 10 + scrollY + (row * (modH + gap));

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

            if (!searchString.isEmpty()) {
                String catName = m.getCategory().name();
                float catW = FontManager.r18.getStringWidth(catName);
                FontManager.r18.drawStringWithShadow(catName, btnX + modW - catW - 10, btnY + 16, new Color(100, 100, 100).getRGB());
            }

            GL11.glPopMatrix();
            i++;
        }
    }

    private void drawSettingsPanel(float x, float y, float w, int mouseX, int mouseY, int alpha) {
        float startX = x + 20;
        float currentY = y + 10 + scrollY;

        FontManager.b20.drawStringWithShadow(selectedModule.getName(), startX + 20, currentY + 15, -1);
        float backX = startX + w - 70;
        FontManager.r18.drawStringWithShadow("< Back", backX, currentY + 15, new Color(160,160,160, alpha).getRGB());

        float setY = currentY + 50;

        float totalH = 50;
        for (Component comp : activeComponents) {
            if (comp.setting.isVisible()) totalH += comp.height + 5;
        }
        maxScroll = Math.max(0, totalH - (frameHeight - 60));

        for (Component comp : activeComponents) {
            if (!comp.setting.isVisible()) continue;
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
        if (mouseX >= frameX && mouseX <= frameX + frameWidth && mouseY >= frameY && mouseY <= frameY + 20 && mouseButton == 0) {
            dragging = true; dragX = mouseX - frameX; dragY = mouseY - frameY; return;
        }

        // --- SEARCH BAR LOGIC ---
        float contentX = frameX + sidebarWidth;
        float contentW = frameWidth - sidebarWidth;
        float padding = 25;
        float searchX = contentX + padding;
        float searchW = contentW - (padding * 2);
        float searchY = frameY + 15;
        float searchH = 30;

        // Kliknięcie w pole wyszukiwania
        if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
            // Sprawdź czy kliknięto w X (clear)
            if (!searchString.isEmpty()) {
                float crossX = searchX + searchW - 20;
                if (mouseX >= crossX - 5 && mouseX <= crossX + 15) { // Hitbox X
                    searchString = "";
                    isSearchFocused = true;
                    return;
                }
            }

            if (mouseButton == 1) {
                searchString = "";
                isSearchFocused = true;
            } else {
                isSearchFocused = true;
            }
            return;
        } else {
            // Kliknięcie poza pasek = utrata focusu, chyba że klikamy w moduły
            // isSearchFocused = false; // Opcjonalnie, ale user chciał "pisać od razu"
        }

        // Sidebar click
        if (mouseX >= frameX && mouseX <= frameX + sidebarWidth) {
            float cy = frameY + 80;
            for (Module.Category cat : Module.Category.values()) {
                if (mouseY >= cy && mouseY <= cy + 24) {
                    selectedCategory = cat;
                    selectedModule = null;
                    scrollY = 0; targetScrollY = 0;
                    searchString = "";
                    // isSearchFocused = false;
                    return;
                }
                cy += 35;
            }
        }

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

        float contentY = frameY + 50;

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + frameHeight) {
            if (selectedModule == null) {
                List<Module> mods;
                if (!searchString.isEmpty()) {
                    mods = Client.INSTANCE.moduleManager.getModules().stream()
                            .filter(m -> m.getName().toLowerCase().contains(searchString.toLowerCase()))
                            .collect(Collectors.toList());
                } else {
                    mods = Client.INSTANCE.moduleManager.getModulesByCategory(selectedCategory);
                }

                float modW = (contentW - (20 * 3)) / 2; float modH = 40; float gap = 15;
                int i = 0;
                for (Module m : mods) {
                    int col = i % 2; int row = i / 2;
                    float btnX = contentX + 20 + (col * (modW + gap));
                    float btnY = contentY + 10 + scrollY + (row * (modH + gap));
                    if (mouseX >= btnX && mouseX <= btnX + modW && mouseY >= btnY && mouseY <= btnY + modH) {
                        if (mouseButton == 0) m.toggle();
                        else if (mouseButton == 1) {
                            selectedModule = m; initComponents(); scrollY = 0; targetScrollY = 0;
                            isSearchFocused = false; // Wyłącz pisanie jak wejdziesz w settings
                        }
                        return;
                    }
                    i++;
                }
            } else {
                // Settings
                float settingsStartX = contentX + 20;
                float headY = contentY - 30 + scrollY; // Korekta
                float backX = settingsStartX + contentW - 70;
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
        // --- SEARCH INPUT & LOGIC ---
        // 1. Obsługa ESC (Clearing vs Closing)
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (isSearchFocused && !searchString.isEmpty()) {
                searchString = ""; // Wyczyść tekst
                return; // Nie zamykaj GUI
            }
            // Jeśli pusty tekst lub brak focusa, zamknij GUI
            super.keyTyped(typedChar, keyCode);
            return;
        }

        // 2. Pisanie w Search Bar
        if (isSearchFocused && selectedModule == null) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (searchString.length() > 0) {
                    // --- FIX: CTRL + BACKSPACE ---
                    // Sprawdzamy czy wciśnięty jest CTRL (LWJGL Key codes: 29 = LCTRL, 157 = RCTRL)
                    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                        String trimmed = searchString.trim();
                        int lastSpace = trimmed.lastIndexOf(' ');
                        if (lastSpace == -1) {
                            searchString = ""; // Brak spacji -> usuń wszystko
                        } else {
                            // Usuń ostatnie słowo, zachowaj tekst do spacji
                            searchString = trimmed.substring(0, lastSpace);
                            // Opcjonalnie dodaj spację na końcu jeśli chcesz zachować odstęp: + " "
                        }
                    } else {
                        // Zwykły Backspace (jedna litera)
                        searchString = searchString.substring(0, searchString.length() - 1);
                    }
                    scrollY = 0; targetScrollY = 0;
                }
            } else if (keyCode == Keyboard.KEY_RETURN) {
                isSearchFocused = false; // Enter kończy pisanie
            } else if (isAllowedCharacter(typedChar)) {
                searchString += typedChar;
                scrollY = 0; targetScrollY = 0;
            }
            return; // Zjedz event, żeby nie odpalać bindów
        }

        if (selectedModule != null) for (Component c : activeComponents) if (c.setting.isVisible()) c.keyTyped(typedChar, keyCode);

        // Jeśli nie w searchu i nie w settings, standardowe zachowanie (bindy, esc)
        super.keyTyped(typedChar, keyCode);
    }

    private boolean isAllowedCharacter(char character) {
        return character >= 32 && character != 127;
    }

    private String trimString(String text, int width) {
        if (FontManager.r18.getStringWidth(text) <= width) return text;
        String trimmed = "";
        for (int i = 0; i < text.length(); i++) {
            String temp = trimmed + text.charAt(i);
            if (FontManager.r18.getStringWidth(temp) > width) {
                return trimmed;
            }
            trimmed = temp;
        }
        return trimmed;
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

    @Override public boolean doesGuiPauseGame() { return false; }
}