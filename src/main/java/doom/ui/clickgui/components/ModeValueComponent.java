package doom.ui.clickgui.components;

import doom.module.impl.render.ClickGuiModule;
import doom.settings.impl.ModeSetting;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import java.awt.Color;

public class ModeValueComponent extends Component {

    public ModeValueComponent(ModeSetting setting, float width, float height) {
        super(setting, width, height);
    }

    @Override
    public void draw(float x, float y, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        ModeSetting mode = (ModeSetting) setting;

        // Obliczanie wysokości dynamicznie
        float headerHeight = 20; // Wysokość nagłówka
        float optionHeight = 16; // Wysokość jednej opcji

        if (mode.expanded) {
            this.height = headerHeight + (mode.modes.size() * optionHeight) + 4;
        } else {
            this.height = headerHeight;
        }

        // --- NAGŁÓWEK ---
        // Nazwa ustawienia
        FontManager.r18.drawStringWithShadow(setting.name, x + 2, y + 6, -1);

        // Box z aktualnym trybem
        float boxW = 90;
        float boxH = 16;
        float boxX = x + width - boxW - 2;
        float boxY = y + 2;

        RenderUtil.drawRoundedRect(boxX, boxY, boxW, boxH, 4, new Color(40, 40, 45).getRGB());

        // Strzałka i tekst
        String arrow = mode.expanded ? "-" : "+";
        FontManager.r18.drawStringWithShadow(arrow, boxX + boxW - 10, boxY + 5, 0xFFAAAAAA);
        FontManager.r18.drawCenteredString(mode.getMode(), boxX + boxW / 2, boxY + 5, -1);

        // --- ROZWINIĘTA LISTA ---
        if (mode.expanded) {
            float listY = boxY + boxH + 2;
            int themeColor = ClickGuiModule.getGuiColor().getRGB();

            // Tło listy
            RenderUtil.drawRoundedRect(boxX, listY, boxW, (mode.modes.size() * optionHeight), 4, new Color(25, 25, 30).getRGB());
            RenderUtil.drawRoundedOutline(boxX, listY, boxW, (mode.modes.size() * optionHeight), 4, 1.0f, new Color(60,60,60).getRGB());

            for (String modeName : mode.modes) {
                boolean isSelected = modeName.equals(mode.getMode());
                boolean isHovered = mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= listY && mouseY <= listY + optionHeight;

                if (isHovered) {
                    RenderUtil.drawRoundedRect(boxX + 1, listY, boxW - 2, optionHeight, 2, new Color(50, 50, 55).getRGB());
                }

                int textColor = isSelected ? themeColor : (isHovered ? -1 : 0xFFAAAAAA);
                FontManager.r18.drawCenteredString(modeName, boxX + boxW / 2, listY + 4, textColor);

                listY += optionHeight;
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        ModeSetting mode = (ModeSetting) setting;

        float headerHeight = 20;
        float boxW = 90;
        float boxH = 16;
        float boxX = x + width - boxW - 2;
        float boxY = y + 2;

        // Kliknięcie w nagłówek (otwieranie/zamykanie)
        if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH && button == 0) {
            mode.expanded = !mode.expanded;
            return;
        }

        // Kliknięcie w opcje (jeśli otwarte)
        if (mode.expanded) {
            float listY = boxY + boxH + 2;
            float optionHeight = 16;

            for (String modeName : mode.modes) {
                if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= listY && mouseY <= listY + optionHeight && button == 0) {
                    mode.setMode(modeName);
                    // Opcjonalnie: mode.expanded = false; // Odkomentuj, jeśli chcesz zamykać po wyborze
                    return;
                }
                listY += optionHeight;
            }
        }
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int button) {}
    @Override public void keyTyped(char typedChar, int keyCode) {}
}