package doom.ui.clickgui;

import doom.Client;
import doom.module.Module;
import doom.settings.Setting;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.ui.hudeditor.GuiHudEditor;
import doom.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Desktop;
import java.io.IOException;
import java.util.ArrayList;

public class ClickGui extends GuiScreen {

    // --- ZMIENNE GŁÓWNE ---
    private float frameX, frameY, frameWidth, frameHeight;
    private final float sidebarWidth = 110;

    private Module.Category selectedCategory = Module.Category.COMBAT;
    private Module selectedModule = null;

    private boolean isBinding = false;
    private boolean showCredits = false;

    // --- ZMIENNE DRAGGING ---
    private boolean dragging;
    private float dragX, dragY;
    private NumberSetting draggingNumber = null;

    // --- ZMIENNE SCROLLOWANIA (NOWE) ---
    private float scrollY = 0;
    private float targetScrollY = 0;
    private float maxScroll = 0;

    // --- KOLORY (DOOM PALETTE) ---
    private final int COL_BG_DARK = new Color(15, 15, 15, 230).getRGB();
    private final int COL_ACCENT_START = new Color(220, 20, 20).getRGB();
    private final int COL_ACCENT_END = new Color(120, 0, 0).getRGB();
    private final int COL_TEXT_SEC = new Color(150, 150, 150).getRGB();

    @Override
    public void initGui() {
        frameWidth = 500;
        frameHeight = 350;

        if (frameX == 0 && frameY == 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            frameX = (sr.getScaledWidth() / 2f) - (frameWidth / 2f);
            frameY = (sr.getScaledHeight() / 2f) - (frameHeight / 2f);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawOverlayInfo();

        // Przesuwanie okna
        if (dragging) {
            frameX = mouseX - dragX;
            frameY = mouseY - dragY;
        }

        if (draggingNumber != null && !Mouse.isButtonDown(0)) {
            draggingNumber = null;
        }

        // ============================================================
        //                  LOGIKA SCROLLOWANIA
        // ============================================================
        handleScroll();

        // 1. TŁO I RAMKA
        RenderUtil.drawBlur(frameX, frameY, frameWidth, frameHeight, 15);
        RenderUtil.drawGlow(frameX, frameY, frameWidth, frameHeight, 20, new Color(0, 0, 0, 150).getRGB());
        RenderUtil.drawRoundedRect(frameX, frameY, frameWidth, frameHeight, 12, COL_BG_DARK);

        // 2. SIDEBAR
        RenderUtil.drawRoundedRect(frameX, frameY, sidebarWidth, frameHeight, 12, new Color(10, 10, 10, 100).getRGB());
        RenderUtil.drawRect(frameX + sidebarWidth, frameY + 10, frameX + sidebarWidth + 1, frameY + frameHeight - 10, new Color(40, 40, 40).getRGB());

        mc.fontRendererObj.drawStringWithShadow("§cDOOM", frameX + 20, frameY + 20, -1);
        mc.fontRendererObj.drawStringWithShadow("§7CLIENT", frameX + 20, frameY + 32, -1);

        drawCategories(mouseX, mouseY);
        drawSidebarBottom(mouseX, mouseY);

        // 3. CONTENT (Z OBSZAREM PRZYCINANIA / SCISSOR)
        float contentX = frameX + sidebarWidth + 20;
        float contentY = frameY + 20;
        float contentWidth = frameWidth - sidebarWidth - 40;
        float contentHeight = frameHeight - 40;

        // Włączamy przycinanie, żeby scrollowane elementy nie wyjeżdżały poza okno
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.scissor(contentX, contentY, contentWidth + 20, contentHeight); // +20 marginesu na scrollbar

        if (selectedModule == null) {
            drawModuleList(contentX, contentY, mouseX, mouseY);
        } else {
            drawSettings(contentX, contentY, mouseX, mouseY);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 4. POPUP CREDITS
        if (showCredits) {
            drawCreditsPopup();
        }
    }

    // --- LOGIKA SCROLLA ---
    private void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel < 0) {
            targetScrollY -= 25; // Przewijanie w dół
        } else if (wheel > 0) {
            targetScrollY += 25; // Przewijanie w górę
        }

        // Zabezpieczenia (Clamp)
        if (targetScrollY > 0) targetScrollY = 0;
        if (targetScrollY < -maxScroll) targetScrollY = -maxScroll;

        // Płynna animacja
        scrollY = RenderUtil.lerp(scrollY, targetScrollY, 0.2f);
    }

    // --- LISTA MODUŁÓW ---
// --- RYSOWANIE LISTY MODUŁÓW (GRID + SCROLL) ---
    private void drawModuleList(float startX, float startY, int mouseX, int mouseY) {
        ArrayList<Module> modsInCategory = new ArrayList<>();
        for (Module m : Client.INSTANCE.moduleManager.getModules()) {
            if (m.getCategory() == selectedCategory && !m.hidden) {
                modsInCategory.add(m);
            }
        }

        float modWidth = 160;
        float modHeight = 28;
        float gap = 10;

        // 1. Obliczanie wysokości contentu dla scrolla
        // Dzielimy liczbę modułów przez 2 (bo 2 kolumny) i zaokrąglamy w górę
        int rows = (int) Math.ceil(modsInCategory.size() / 2.0);
        float totalContentHeight = rows * (modHeight + gap);

        // Wysokość widocznego obszaru (okno minus marginesy)
        float visibleHeight = frameHeight - 40;

        // Ustawiamy maxScroll (ile można przewinąć w dół)
        // Jeśli content jest mniejszy niż okno, maxScroll to 0.
        maxScroll = Math.max(0, totalContentHeight - visibleHeight + 20); // +20 marginesu na dole

        // 2. Rysowanie przycisków
        int i = 0;
        for (Module m : modsInCategory) {
            // Logika siatki (Grid):
            // i % 2 == 0 -> Lewa kolumna, i % 2 == 1 -> Prawa kolumna
            int col = i % 2;
            int row = i / 2;

            float buttonX = startX + (col * (modWidth + gap));
            // Do pozycji Y dodajemy scrollY (który jest ujemny, więc przesuwa w górę)
            float buttonY = startY + scrollY + (row * (modHeight + gap));

            // OPTYMALIZACJA: Nie rysuj, jeśli przycisk wyjechał poza widoczny obszar
            // (Zwiększa FPS przy dużej liście)
            if (buttonY > frameY + frameHeight || buttonY + modHeight < frameY) {
                i++;
                continue;
            }

            // Sprawdzamy hover (czy myszka jest nad przyciskiem I czy jest wewnątrz okna contentu)
            boolean isInContentArea = mouseX >= frameX + sidebarWidth && mouseX <= frameX + frameWidth
                    && mouseY >= frameY && mouseY <= frameY + frameHeight;

            boolean isHovered = isInContentArea && mouseX >= buttonX && mouseX <= buttonX + modWidth
                    && mouseY >= buttonY && mouseY <= buttonY + modHeight;

            // Rysowanie wyglądu (Taki sam styl jak wcześniej)
            if (m.isToggled()) {
                RenderUtil.drawGlow(buttonX, buttonY, modWidth, modHeight, 10, new Color(220, 0, 0, 60).getRGB());
                RenderUtil.drawRoundedGradientRect(buttonX, buttonY, modWidth, modHeight, 6, COL_ACCENT_START, COL_ACCENT_END, COL_ACCENT_END, COL_ACCENT_START);
                mc.fontRendererObj.drawStringWithShadow(m.getName(), buttonX + 10, buttonY + 10, -1);
            } else {
                int bgCol = isHovered ? new Color(45, 45, 45).getRGB() : new Color(30, 30, 30).getRGB();
                RenderUtil.drawRoundedRect(buttonX, buttonY, modWidth, modHeight, 6, bgCol);

                if (isHovered) {
                    RenderUtil.drawRoundedOutline(buttonX, buttonY, modWidth, modHeight, 6, 1.0f, new Color(80, 80, 80).getRGB());
                }

                mc.fontRendererObj.drawStringWithShadow(m.getName(), buttonX + 10, buttonY + 10, isHovered ? -1 : 0xFFCCCCCC);
            }

            // Keybind info
            if (m.getKey() != 0) {
                String keyName = "[" + Keyboard.getKeyName(m.getKey()) + "]";
                mc.fontRendererObj.drawStringWithShadow(keyName, buttonX + modWidth - mc.fontRendererObj.getStringWidth(keyName) - 8, buttonY + 10, new Color(255, 255, 255, 100).getRGB());
            }

            i++;
        }
    }

    // --- POPRAWIONA METODA RYSOWANIA MODUŁÓW (GRID) ---
    // Zastępuje tę wyżej, bo tamta miała dziwną logikę kolumn
    private void drawModuleListProper(float startX, float startY, int mouseX, int mouseY) {
        ArrayList<Module> modsInCategory = new ArrayList<>();
        for(Module m : Client.INSTANCE.moduleManager.getModules()) {
            if(m.getCategory() == selectedCategory && !m.hidden) modsInCategory.add(m);
        }

        float modWidth = 160;
        float modHeight = 28;
        float gap = 10;

        // Obliczamy całkowitą wysokość
        int rows = (int) Math.ceil(modsInCategory.size() / 2.0); // 2 kolumny
        float totalContentHeight = rows * (modHeight + gap);
        float visibleHeight = frameHeight - 40;
        maxScroll = Math.max(0, totalContentHeight - visibleHeight + 20); // +20 margines na dole

        int i = 0;
        for (Module m : modsInCategory) {
            int col = i % 2; // 0 = lewa, 1 = prawa
            int row = i / 2; // 0, 1, 2...

            float buttonX = startX + (col * (modWidth + gap));
            float buttonY = startY + scrollY + (row * (modHeight + gap));

            // Optymalizacja: Nie rysuj jeśli poza ekranem
            if (buttonY > frameY + frameHeight || buttonY + modHeight < frameY) {
                i++;
                continue;
            }

            boolean isInContentArea = mouseX >= frameX + sidebarWidth && mouseX <= frameX + frameWidth && mouseY >= frameY && mouseY <= frameY + frameHeight;
            boolean isHovered = isInContentArea && mouseX >= buttonX && mouseX <= buttonX + modWidth && mouseY >= buttonY && mouseY <= buttonY + modHeight;

            if (m.isToggled()) {
                RenderUtil.drawGlow(buttonX, buttonY, modWidth, modHeight, 10, new Color(220, 0, 0, 60).getRGB());
                RenderUtil.drawRoundedGradientRect(buttonX, buttonY, modWidth, modHeight, 6, COL_ACCENT_START, COL_ACCENT_END, COL_ACCENT_END, COL_ACCENT_START);
                mc.fontRendererObj.drawStringWithShadow(m.getName(), buttonX + 10, buttonY + 10, -1);
            } else {
                int bgCol = isHovered ? new Color(45, 45, 45).getRGB() : new Color(30, 30, 30).getRGB();
                RenderUtil.drawRoundedRect(buttonX, buttonY, modWidth, modHeight, 6, bgCol);
                if (isHovered) RenderUtil.drawRoundedOutline(buttonX, buttonY, modWidth, modHeight, 6, 1.0f, new Color(80, 80, 80).getRGB());
                mc.fontRendererObj.drawStringWithShadow(m.getName(), buttonX + 10, buttonY + 10, isHovered ? -1 : 0xFFCCCCCC);
            }

            if (m.getKey() != 0) {
                String keyName = "[" + Keyboard.getKeyName(m.getKey()) + "]";
                mc.fontRendererObj.drawStringWithShadow(keyName, buttonX + modWidth - mc.fontRendererObj.getStringWidth(keyName) - 8, buttonY + 10, new Color(255, 255, 255, 100).getRGB());
            }

            i++;
        }
    }

    // --- USTAWIENIA (Z UWZGLĘDNIENIEM SCROLLA) ---
    private void drawSettings(float startX, float startY, int mouseX, int mouseY) {
        // Obliczamy wysokość contentu na start (żeby ustawić maxScroll)
        ArrayList<Setting> settings = Client.INSTANCE.settingsManager.getSettingsByMod(selectedModule);
        float calculatedHeight = 80; // Nagłówek + bind
        for (Setting s : settings) {
            if (!s.isVisible()) continue;
            if (s instanceof BooleanSetting) calculatedHeight += 22;
            else if (s instanceof NumberSetting) calculatedHeight += 28;
            else if (s instanceof ModeSetting) calculatedHeight += 30;
        }
        float visibleHeight = frameHeight - 40;
        maxScroll = Math.max(0, calculatedHeight - visibleHeight + 20);

        // HEADER (Statyczny - nie scrolluje się, lub scrolluje - tutaj scrolluje się wszystko dla spójności)
        float currentY = startY + scrollY;

        RenderUtil.drawRoundedRect(startX, currentY, 350, 30, 6, new Color(30, 30, 30).getRGB());
        mc.fontRendererObj.drawStringWithShadow("Editing: §c" + selectedModule.getName(), startX + 10, currentY + 11, -1);

        // Back Button
        String backText = "< Back";
        float backW = mc.fontRendererObj.getStringWidth(backText) + 12;
        float backX = startX + 350 - backW - 5;
        boolean isInContent = mouseY >= frameY && mouseY <= frameY + frameHeight;
        boolean hoverBack = isInContent && mouseX >= backX && mouseX <= backX + backW && mouseY >= currentY + 5 && mouseY <= currentY + 25;

        RenderUtil.drawRoundedRect(backX, currentY + 5, backW, 20, 4, hoverBack ? new Color(60, 60, 60).getRGB() : new Color(45, 45, 45).getRGB());
        mc.fontRendererObj.drawStringWithShadow(backText, backX + 6, currentY + 11, -1);

        // Bind Button
        String bindText = isBinding ? "Listening..." : "Bind: " + (selectedModule.getKey() == 0 ? "NONE" : Keyboard.getKeyName(selectedModule.getKey()));
        currentY += 45;
        RenderUtil.drawRoundedRect(startX, currentY, 150, 20, 4, isBinding ? COL_ACCENT_START : new Color(35, 35, 35).getRGB());
        mc.fontRendererObj.drawStringWithShadow(bindText, startX + 6, currentY + 6, -1);

        currentY += 30;

        if (settings.isEmpty()) {
            mc.fontRendererObj.drawStringWithShadow("No settings for this module.", startX, currentY, 0xFFAAAAAA);
            return;
        }

        for (Setting s : settings) {
            if (!s.isVisible()) continue;

            // Optymalizacja renderowania (poza ekranem)
            if (currentY > frameY + frameHeight || currentY + 30 < frameY) {
                if (s instanceof BooleanSetting) currentY += 22;
                else if (s instanceof NumberSetting) currentY += 28;
                else if (s instanceof ModeSetting) currentY += 30;
                continue;
            }

            if (s instanceof BooleanSetting) {
                BooleanSetting bool = (BooleanSetting) s;
                float switchX = startX + 200;
                int trackColor = bool.isEnabled() ? COL_ACCENT_START : new Color(50, 50, 50).getRGB();

                RenderUtil.drawRoundedRect(switchX, currentY + 2, 24, 12, 6, trackColor);
                if (bool.isEnabled()) RenderUtil.drawGlow(switchX, currentY + 2, 24, 12, 5, trackColor);
                float knobX = bool.isEnabled() ? (switchX + 24 - 10) : (switchX + 2);
                RenderUtil.drawRoundedRect(knobX, currentY + 4, 8, 8, 4, -1);

                mc.fontRendererObj.drawStringWithShadow(s.name, startX, currentY + 4, -1);
                currentY += 22;
            }
            else if (s instanceof NumberSetting) {
                NumberSetting num = (NumberSetting) s;
                mc.fontRendererObj.drawStringWithShadow(s.name, startX, currentY, -1);
                String valStr = String.format("%.2f", num.getValue());
                mc.fontRendererObj.drawStringWithShadow(valStr, startX + 340 - mc.fontRendererObj.getStringWidth(valStr), currentY, COL_TEXT_SEC);

                float sliderX = startX;
                float sliderY = currentY + 12;
                float sliderW = 340;

                RenderUtil.drawRoundedRect(sliderX, sliderY, sliderW, 6, 3, new Color(40, 40, 40).getRGB());
                float fill = (float) ((num.getValue() - num.getMin()) / (num.getMax() - num.getMin()) * sliderW);
                if (fill > sliderW) fill = sliderW;
                if (fill < 0) fill = 0;

                if (fill > 2) {
                    RenderUtil.drawRoundedGradientRect(sliderX, sliderY, fill, 6, 3, COL_ACCENT_START, COL_ACCENT_END, COL_ACCENT_END, COL_ACCENT_START);
                    RenderUtil.drawGlow(sliderX, sliderY, fill, 6, 5, COL_ACCENT_START);
                }
                if (fill > 4) RenderUtil.drawRoundedRect(sliderX + fill - 4, sliderY - 2, 8, 10, 4, -1);

                // Update dragging value logic here to be precise with scroll
                if (draggingNumber == num) {
                    double val = num.getMin() + (Math.max(0, Math.min(sliderW, mouseX - sliderX)) / sliderW) * (num.getMax() - num.getMin());
                    num.setValue(val);
                }

                currentY += 28;
            }
            else if (s instanceof ModeSetting) {
                ModeSetting mode = (ModeSetting) s;
                mc.fontRendererObj.drawStringWithShadow(s.name + ":", startX, currentY + 6, -1);
                float modeX = startX + 100;
                RenderUtil.drawRoundedRect(modeX, currentY, 150, 20, 4, new Color(40, 40, 40).getRGB());
                RenderUtil.drawRoundedOutline(modeX, currentY, 150, 20, 4, 1.0f, new Color(60, 60, 60).getRGB());
                String mText = mode.getMode();
                mc.fontRendererObj.drawStringWithShadow(mText, modeX + (150/2f) - (mc.fontRendererObj.getStringWidth(mText)/2f), currentY + 6, -1);
                currentY += 30;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (showCredits) { showCredits = false; return; }

        // Sidebar
        float bottomAreaY = frameY + frameHeight - 45;
        float startX = frameX + (sidebarWidth - (24 * 3 + 20)) / 2;
        if (mouseY >= bottomAreaY && mouseY <= bottomAreaY + 24 && mouseX >= startX && mouseX <= startX + 100) {
            // ... (logika przycisków sidebar - skopiowana z poprzedniego, bez zmian)
            // Uproszczenie dla czytelności scrolla:
            if (mouseX >= startX && mouseX <= startX+24) { showCredits=true; return; }
            if (mouseX >= startX+34 && mouseX <= startX+58) { mc.displayGuiScreen(new GuiHudEditor()); return; }
            if (mouseX >= startX+68 && mouseX <= startX+92) { try{Desktop.getDesktop().open(Client.INSTANCE.configManager.configDir);}catch(Exception e){} return; }
        }

        // Dragging window
        if (mouseButton == 0 && mouseX >= frameX && mouseX <= frameX + frameWidth && mouseY >= frameY && mouseY <= frameY + 20) {
            dragging = true;
            dragX = mouseX - frameX;
            dragY = mouseY - frameY;
            return;
        }

        // Category selection
        if (selectedModule == null && mouseX > frameX && mouseX < frameX + sidebarWidth) {
            float catY = frameY + 70;
            for (Module.Category cat : Module.Category.values()) {
                if (mouseY >= catY && mouseY <= catY + 20) {
                    selectedCategory = cat;
                    scrollY = 0; targetScrollY = 0; // Reset scrolla
                    return;
                }
                catY += 30;
            }
        }

        // Content Area Click Logic (UWZGLĘDNIAJĄCA SCROLL)
        float contentX = frameX + sidebarWidth + 20;
        float contentY = frameY + 20;
        float contentW = frameWidth - sidebarWidth - 20;
        float contentH = frameHeight - 40;

        // Ważne: Sprawdzamy czy kliknięcie jest W OBSZARZE VIDOKU
        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH) {

            if (selectedModule == null) {
                // --- LOGIKA KLIKANIA W MODUŁY (GRID) ---
                ArrayList<Module> mods = new ArrayList<>();
                for(Module m : Client.INSTANCE.moduleManager.getModules()) {
                    if(m.getCategory() == selectedCategory && !m.hidden) mods.add(m);
                }

                float modW = 160;
                float modH = 28;
                float gap = 10;

                int i = 0;
                for (Module m : mods) {
                    int col = i % 2;
                    int row = i / 2;

                    float btnX = contentX + (col * (modW + gap));
                    float btnY = contentY + scrollY + (row * (modH + gap)); // Pamiętaj o scrollY!

                    // Sprawdzamy kliknięcie
                    if (mouseX >= btnX && mouseX <= btnX + modW && mouseY >= btnY && mouseY <= btnY + modH) {
                        if (mouseButton == 0) {
                            // Lewy przycisk: Toggle
                            m.toggle();
                        } else if (mouseButton == 1) {
                            // Prawy przycisk: Otwórz ustawienia
                            selectedModule = m;
                            isBinding = false;
                            scrollY = 0;
                            targetScrollY = 0; // Reset scrolla po wejściu w settingsy
                        }
                        return;
                    }
                    i++;
                }
            } else {
                // Settings
                float currY = contentY + scrollY; // Scrollujemy cały panel, łącznie z nagłówkiem

                // Back button
                if (mouseButton == 0 && mouseX >= contentX + 280 && mouseX <= contentX + 350 && mouseY >= currY + 5 && mouseY <= currY + 25) {
                    selectedModule = null; scrollY = 0; targetScrollY = 0; return;
                }
                // Bind button
                if (mouseButton == 0 && mouseX >= contentX && mouseX <= contentX + 150 && mouseY >= currY + 45 && mouseY <= currY + 65) {
                    isBinding = !isBinding; return;
                }

                currY += 75; // Offset nagłówka

                for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(selectedModule)) {
                    if (!s.isVisible()) continue;

                    if (s instanceof BooleanSetting) {
                        float swX = contentX + 200;
                        if (mouseButton == 0 && mouseX >= swX && mouseX <= swX + 24 && mouseY >= currY + 2 && mouseY <= currY + 14) {
                            ((BooleanSetting)s).toggle();
                        }
                        currY += 22;
                    } else if (s instanceof NumberSetting) {
                        if (mouseButton == 0 && mouseX >= contentX && mouseX <= contentX + 340 && mouseY >= currY + 10 && mouseY <= currY + 20) {
                            draggingNumber = (NumberSetting) s;
                        }
                        currY += 28;
                    } else if (s instanceof ModeSetting) {
                        if (mouseButton == 0 && mouseX >= contentX + 100 && mouseX <= contentX + 250 && mouseY >= currY && mouseY <= currY + 20) {
                            ((ModeSetting)s).cycle();
                        }
                        currY += 30;
                    }
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    // --- RESZTA (Bez zmian) ---
    private void drawOverlayInfo() {
        int infoX = 10; int infoY = this.height - 30;
        String text = "Doom Client " + Client.INSTANCE.version + " | " + mc.session.getUsername();
        float w = mc.fontRendererObj.getStringWidth(text) + 20;
        RenderUtil.drawBlur(infoX, infoY, w, 20, 10);
        RenderUtil.drawRoundedRect(infoX, infoY, w, 20, 6, new Color(0, 0, 0, 150).getRGB());
        RenderUtil.drawRoundedOutline(infoX, infoY, w, 20, 6, 1.5f, COL_ACCENT_START);
        mc.fontRendererObj.drawStringWithShadow(text, infoX + 10, infoY + 6, -1);
    }

    private void drawCategories(int mouseX, int mouseY) {
        float catY = frameY + 70;
        for (Module.Category cat : Module.Category.values()) {
            boolean isSelected = (cat == selectedCategory);
            boolean isHovered = mouseX >= frameX && mouseX <= frameX + sidebarWidth && mouseY >= catY && mouseY <= catY + 20;
            if (isSelected) {
                RenderUtil.drawGlow(frameX + 10, catY, sidebarWidth - 20, 20, 10, new Color(220, 20, 20, 50).getRGB());
                RenderUtil.drawRoundedGradientRect(frameX + 10, catY, sidebarWidth - 20, 20, 6, COL_ACCENT_START, COL_ACCENT_END, COL_ACCENT_END, COL_ACCENT_START);
                mc.fontRendererObj.drawStringWithShadow(cat.name(), frameX + 25, catY + 6, -1);
            } else {
                if (isHovered) RenderUtil.drawRoundedRect(frameX + 10, catY, sidebarWidth - 20, 20, 6, new Color(255, 255, 255, 10).getRGB());
                mc.fontRendererObj.drawStringWithShadow(cat.name(), frameX + 25, catY + 6, COL_TEXT_SEC);
            }
            catY += 30;
        }
    }

    private void drawSidebarBottom(int mouseX, int mouseY) {
        float bottomAreaY = frameY + frameHeight - 45;
        float btnSize = 24; float gap = 10;
        float startX = frameX + (sidebarWidth - (btnSize * 3 + gap * 2)) / 2;
        drawIconButton(startX, bottomAreaY, btnSize, mouseX, mouseY, "credits");
        drawIconButton(startX + btnSize + gap, bottomAreaY, btnSize, mouseX, mouseY, "editor");
        drawIconButton(startX + (btnSize + gap) * 2, bottomAreaY, btnSize, mouseX, mouseY, "folder");
    }

    private void drawIconButton(float x, float y, float size, int mouseX, int mouseY, String type) {
        boolean hover = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
        if (hover) {
            RenderUtil.drawGlow(x, y, size, size, 8, new Color(220, 20, 20, 50).getRGB());
            RenderUtil.drawRoundedRect(x, y, size, size, 6, new Color(40, 40, 40).getRGB());
            RenderUtil.drawRoundedOutline(x, y, size, size, 6, 1.0f, COL_ACCENT_START);
        } else {
            RenderUtil.drawRoundedRect(x, y, size, size, 6, new Color(25, 25, 25).getRGB());
            RenderUtil.drawRoundedOutline(x, y, size, size, 6, 1.0f, new Color(50, 50, 50).getRGB());
        }
        float cx = x + size / 2; float cy = y + size / 2; int iconCol = hover ? -1 : 0xFFAAAAAA;
        if (type.equals("folder")) {
            RenderUtil.drawRect(x + 6, y + 10, x + size - 6, y + size - 6, iconCol);
            RenderUtil.drawRect(x + 6, y + 7, x + size/2, y + 10, iconCol);
            RenderUtil.drawRect(x + 7, y + 11, x + size - 7, y + size - 7, COL_BG_DARK);
        } else if (type.equals("editor")) {
            RenderUtil.drawRect(cx - 5, cy - 5, cx - 1, cy - 1, iconCol);
            RenderUtil.drawRect(cx + 1, cy - 5, cx + 5, cy - 1, iconCol);
            RenderUtil.drawRect(cx - 5, cy + 1, cx - 1, cy + 5, iconCol);
            RenderUtil.drawRect(cx + 1, cy + 1, cx + 5, cy + 5, iconCol);
        } else {
            RenderUtil.drawRoundedRect(cx - 1, cy - 5, 2, 2, 1, iconCol);
            RenderUtil.drawRoundedRect(cx - 1, cy - 1, 2, 6, 1, iconCol);
        }
    }

    private void drawCreditsPopup() {
        RenderUtil.drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 180).getRGB());
        float w = 250, h = 150;
        float x = this.width / 2f - w / 2f, y = this.height / 2f - h / 2f;
        RenderUtil.drawGlow(x, y, w, h, 20, COL_ACCENT_START);
        RenderUtil.drawRoundedRect(x, y, w, h, 12, new Color(20, 20, 20).getRGB());
        RenderUtil.drawRoundedOutline(x, y, w, h, 12, 2.0f, COL_ACCENT_START);
        String title = "DOOM CLIENT";
        mc.fontRendererObj.drawStringWithShadow("§c" + title, x + w/2 - mc.fontRendererObj.getStringWidth(title)/2f, y + 30, -1);
        mc.fontRendererObj.drawStringWithShadow("Developed by: §cNAQSU", x + w/2 - 50, y + 70, -1);
        mc.fontRendererObj.drawStringWithShadow("Version: §7" + Client.INSTANCE.version, x + w/2 - 30, y + 85, -1);
        mc.fontRendererObj.drawStringWithShadow("§7(Click anywhere to close)", x + w/2 - 60, y + 120, -1);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        draggingNumber = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(isBinding && selectedModule != null) {
            selectedModule.setKey((keyCode == 1 || keyCode == 211) ? 0 : keyCode);
            isBinding = false;
            return;
        }
        if(keyCode == 1) { // ESC
            if(showCredits) { showCredits = false; return; }
            if(selectedModule != null) { selectedModule = null; isBinding = false; scrollY = 0; targetScrollY = 0; return; }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        Client.INSTANCE.configManager.save();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}