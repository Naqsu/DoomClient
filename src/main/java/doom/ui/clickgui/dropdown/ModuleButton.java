package doom.ui.clickgui.dropdown;

import doom.Client;
import doom.module.Module;
import doom.module.impl.render.ClickGuiModule;
import doom.settings.Setting;
import doom.settings.impl.*;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;

import java.awt.*;

public class ModuleButton {
    public Module module;
    public Panel parent;
    public float y;
    public float width;
    public float height;
    public boolean extended;

    public ModuleButton(Module module, Panel parent, float y) {
        this.module = module;
        this.parent = parent;
        this.y = y;
        this.width = parent.width;
        this.height = 16;
        this.extended = false;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float absX = parent.x;
        float absY = parent.y + y;
        boolean hovered = isHovered(mouseX, mouseY);
        Color themeColor = ClickGuiModule.getGuiColor();

        // Tło przycisku
        int color = hovered ? new Color(40, 40, 40, 200).getRGB() : new Color(30, 30, 30, 200).getRGB();
        if (module.isToggled()) {
            color = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 150).getRGB();
        }

        RenderUtil.drawRect(absX, absY, width, height, color);
        int textColor = module.isToggled() ? -1 : (hovered ? -1 : 0xFFAAAAAA);
        FontManager.r18.drawStringWithShadow(module.getName(), absX + 5, absY + 4, textColor);

        if (!Client.INSTANCE.settingsManager.getSettingsByMod(module).isEmpty()) {
            FontManager.r18.drawStringWithShadow(extended ? "-" : "+", absX + width - 10, absY + 4, 0xFFAAAAAA);
        }

        if (extended) {
            drawSettings(mouseX, mouseY, absX, absY + 16);
        }
    }

    private void drawSettings(int mouseX, int mouseY, float startX, float startY) {
        float currentY = startY;
        float totalH = getSettingsHeight();

        // Tło ustawień
        RenderUtil.drawRect(startX, currentY, width, totalH, new Color(15, 15, 15, 230).getRGB());
        RenderUtil.drawRect(startX, currentY, 2, totalH, ClickGuiModule.getGuiColor().getRGB()); // Pasek boczny

        for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(module)) {
            if (!s.isVisible()) continue;

            float setX = startX + 6;
            float setW = width - 10;

            // --- OBLICZANIE WYSOKOŚCI ELEMENTU ---
            float setH = 14;
            if (s instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) s;
                if (ms.expanded) {
                    setH = 14 + (ms.modes.size() * 12) + 2; // Expand height logic for Dropdown
                }
            }
            if (s instanceof ColorSetting && ((ColorSetting)s).expanded) {
                setH = 80;
            }
            // -------------------------------------

            if (s instanceof BooleanSetting) {
                BooleanSetting bool = (BooleanSetting) s;
                FontManager.r18.drawStringWithShadow(s.name, setX, currentY + 3, -1);
                RenderUtil.drawRoundedRect(setX + setW - 12, currentY + 2, 10, 10, 2, bool.isEnabled() ? ClickGuiModule.getGuiColor().getRGB() : new Color(60,60,60).getRGB());
            }
            else if (s instanceof NumberSetting) {
                NumberSetting num = (NumberSetting) s;
                boolean hover = mouseX >= setX && mouseX <= setX + setW && mouseY >= currentY && mouseY <= currentY + setH;
                FontManager.r18.drawStringWithShadow(s.name + ": " + num.getValue(), setX, currentY + 2, 0xFFAAAAAA);
                RenderUtil.drawRect(setX, currentY + 12, setW, 2, new Color(60,60,60).getRGB());
                float fill = (float)((num.getValue() - num.getMin()) / (num.getMax() - num.getMin()) * setW);
                RenderUtil.drawRect(setX, currentY + 12, fill, 2, ClickGuiModule.getGuiColor().getRGB());
                if (hover && org.lwjgl.input.Mouse.isButtonDown(0)) {
                    double val = num.getMin() + ((mouseX - setX) / setW) * (num.getMax() - num.getMin());
                    num.setValue(val);
                }
            }
            else if (s instanceof ModeSetting) {
                ModeSetting mode = (ModeSetting) s;
                FontManager.r18.drawStringWithShadow(s.name, setX, currentY + 3, -1);
                FontManager.r18.drawStringWithShadow(mode.getMode(), setX + setW - FontManager.r18.getStringWidth(mode.getMode()), currentY + 3, ClickGuiModule.getGuiColor().getRGB());

                // Rysowanie listy w dropdownie
                if (mode.expanded) {
                    float modeY = currentY + 14;
                    for (String mName : mode.modes) {
                        boolean selected = mName.equals(mode.getMode());
                        int color = selected ? ClickGuiModule.getGuiColor().getRGB() : 0xFFAAAAAA;
                        FontManager.r18.drawCenteredString(mName, setX + setW / 2, modeY, color);
                        modeY += 12;
                    }
                }
            }
            else if (s instanceof CategorySetting) {
                CategorySetting cat = (CategorySetting) s;
                FontManager.r18.drawStringWithShadow(s.name + ": " + cat.getSelected(), setX, currentY + 3, -1);
            }

            currentY += setH;
        }
    }

    public float getHeight() {
        return extended ? height + getSettingsHeight() : height;
    }

    private float getSettingsHeight() {
        float h = 0;
        for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(module)) {
            if (!s.isVisible()) continue;
            float setH = 14;
            if (s instanceof ModeSetting && ((ModeSetting)s).expanded) {
                setH = 14 + (((ModeSetting)s).modes.size() * 12) + 2;
            }
            if (s instanceof ColorSetting && ((ColorSetting)s).expanded) setH = 80;
            h += setH;
        }
        return h;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float absX = parent.x;
        float absY = parent.y + y;

        if (mouseX >= absX && mouseX <= absX + width && mouseY >= absY && mouseY <= absY + height) {
            if (mouseButton == 0) module.toggle();
            else if (mouseButton == 1) extended = !extended;
            return;
        }

        if (extended) {
            float currentY = absY + 16;
            for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(module)) {
                if (!s.isVisible()) continue;

                float setH = 14;
                if (s instanceof ModeSetting && ((ModeSetting)s).expanded) {
                    setH = 14 + (((ModeSetting)s).modes.size() * 12) + 2;
                }
                if (s instanceof ColorSetting && ((ColorSetting)s).expanded) setH = 80;

                // Logika klikania w ustawienia
                float setX = absX + 6;
                float setW = width - 10;

                if (s instanceof BooleanSetting) {
                    if (mouseX >= setX && mouseX <= setX + setW && mouseY >= currentY && mouseY <= currentY + 14)
                        ((BooleanSetting)s).toggle();
                }
                else if (s instanceof ModeSetting) {
                    ModeSetting ms = (ModeSetting) s;
                    // Kliknięcie w nagłówek mode
                    if (mouseX >= setX && mouseX <= setX + setW && mouseY >= currentY && mouseY <= currentY + 14) {
                        if (mouseButton == 1) ms.expanded = !ms.expanded;
                    }
                    // Kliknięcie w opcje
                    else if (ms.expanded && mouseX >= setX && mouseX <= setX + setW && mouseY > currentY + 14 && mouseY < currentY + setH) {
                        int index = (int)((mouseY - (currentY + 14)) / 12);
                        if (index >= 0 && index < ms.modes.size()) {
                            ms.setMode(ms.modes.get(index));
                        }
                    }
                }
                else if (s instanceof CategorySetting) {
                    CategorySetting cs = (CategorySetting) s;
                    if (mouseX >= setX && mouseX <= setX + setW && mouseY >= currentY && mouseY <= currentY + 14) {
                        // Prosta cykliczna zmiana dla CategorySetting w dropdownie
                        int next = cs.index + 1;
                        if (next >= cs.options.size()) next = 0;
                        cs.index = next;
                    }
                }

                currentY += setH;
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (extended) {
            float currentY = parent.y + y + 16; // Header height
            for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(module)) {
                if (!s.isVisible()) continue;

                // Logic for finding component position would replicate drawSettings logic
                // But for Dropdown with manual rendering, we often only need it for Sliders (NumberSetting).
                // Since this is a simple implementation:

                // We assume NumberSettings handle their own dragging release state internally
                // if they rely on global mouse up, or we pass it down.
                // But wait, the standard components like NumberValueComponent aren't used here!
                // Dropdown uses inline drawing.

                // If you are using the manual rendering in drawSettings, you need to handle drag release there
                // or just ensure dragging stops here.

                // Since 'dragging' for sliders usually needs a global release:
                // We'll leave this empty unless your inline sliders need it.
                // But to fix the COMPILATION ERROR, this method must simply exist.
            }
        }
    }
    public void keyTyped(char typedChar, int keyCode) {}
    private boolean isHovered(int mouseX, int mouseY) { return false; } // Helper nieużywany w tej logice
}