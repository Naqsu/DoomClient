package doom.ui.clickgui.dropdown;

import doom.Client;
import doom.module.Module;
import doom.module.impl.render.ClickGuiModule;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Panel {
    public float x, y, width, height;
    public Module.Category category;
    public boolean dragging;
    public boolean extended;
    public float dragX, dragY;
    public List<ModuleButton> buttons = new ArrayList<>();

    public Panel(Module.Category category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = 100;
        this.height = 20;
        this.extended = true;
        this.dragging = false;

        int offset = 0;
        for (Module m : Client.INSTANCE.moduleManager.getModules()) {
            if (m.getCategory() == category) {
                buttons.add(new ModuleButton(m, this, offset));
                offset += 16;
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }

        // --- FIX: ZABEZPIECZENIE PRZED UCIECZKĄ POZA EKRAN ---
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        // Blokada lewo/prawo
        if (x < 0) x = 0;
        if (x + width > screenW) x = screenW - width;

        // Blokada góra/dół (nagłówek musi być widoczny)
        if (y < 0) y = 0;
        if (y + height > screenH) y = screenH - height;
        // -----------------------------------------------------

        Color themeColor = ClickGuiModule.getGuiColor();

        // --- NAGŁÓWEK ---
        // Tło nagłówka
        RenderUtil.drawRoundedRect(x, y, width, height, 4, themeColor.getRGB());

        // Tekst nagłówka (wyśrodkowany)
        FontManager.b20.drawCenteredString(category.name(), x + width / 2, y + 6, -1);

        if (extended) {
            // Tło pod listą modułów
            float currentHeight = 0;
            for(ModuleButton btn : buttons) currentHeight += btn.getHeight();

            // Czarne półprzezroczyste tło pod modułami
            RenderUtil.drawRect(x, y + height, width, currentHeight, new Color(20, 20, 20, 230).getRGB());

            // Obrys dolny dla estetyki
            RenderUtil.drawRect(x, y + height + currentHeight, width, 2, themeColor.getRGB());

            float yOffset = height;
            for (ModuleButton button : buttons) {
                button.y = yOffset;
                button.drawScreen(mouseX, mouseY, partialTicks);
                yOffset += button.getHeight();
            }
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            if (mouseButton == 0) {
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
            } else if (mouseButton == 1) {
                extended = !extended;
            }
            return;
        }

        if (extended) {
            for (ModuleButton button : buttons) {
                button.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        if (extended) {
            for (ModuleButton button : buttons) {
                button.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (extended) {
            for (ModuleButton button : buttons) {
                button.keyTyped(typedChar, keyCode);
            }
        }
    }

    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}