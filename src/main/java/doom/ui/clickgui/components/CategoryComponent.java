package doom.ui.clickgui.components;

import doom.module.impl.render.ClickGuiModule;
import doom.settings.impl.CategorySetting;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;

import java.awt.*;

public class CategoryComponent extends Component {

    public CategoryComponent(CategorySetting setting, float width, float height) {
        super(setting, width, height);
    }

    @Override
    public void draw(float x, float y, int mouseX, int mouseY) {
        this.x = x; this.y = y;
        CategorySetting catSet = (CategorySetting) setting;

        // Tło kontenera
        // RenderUtil.drawRoundedRect(x, y, width, height, 4, new Color(30, 30, 35).getRGB());

        int optionsCount = catSet.options.size();
        float buttonWidth = (width - ((optionsCount - 1) * 2)) / optionsCount; // Szerokość jednego przycisku z odstępami 2px

        Color themeColor = ClickGuiModule.getGuiColor();

        for (int i = 0; i < optionsCount; i++) {
            String option = catSet.options.get(i);
            boolean isSelected = i == catSet.index;

            float btnX = x + (i * (buttonWidth + 2));
            float btnY = y;

            boolean hover = mouseX >= btnX && mouseX <= btnX + buttonWidth && mouseY >= btnY && mouseY <= btnY + height;

            // Kolor tła przycisku
            int color;
            if (isSelected) {
                color = themeColor.getRGB(); // Aktywny = Theme
            } else if (hover) {
                color = new Color(60, 60, 65).getRGB(); // Hover = Jaśniejszy szary
            } else {
                color = new Color(40, 40, 45).getRGB(); // Zwykły = Ciemny szary
            }

            // Rysowanie przycisku
            RenderUtil.drawRoundedRect(btnX, btnY, buttonWidth, height, 4, color);

            // Jeśli aktywny, dodaj Glow
            if (isSelected) {
                int glowC = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 100).getRGB();
                RenderUtil.drawGlow(btnX, btnY, buttonWidth, height, 5, glowC);
            }

            // Tekst
            int textColor = isSelected ? -1 : (hover ? -1 : 0xFFAAAAAA);
            FontManager.r18.drawCenteredString(option, btnX + buttonWidth / 2f, btnY + height / 2f - 4, textColor);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        CategorySetting catSet = (CategorySetting) setting;
        int optionsCount = catSet.options.size();
        float buttonWidth = (width - ((optionsCount - 1) * 2)) / optionsCount;

        for (int i = 0; i < optionsCount; i++) {
            float btnX = x + (i * (buttonWidth + 2));
            if (mouseX >= btnX && mouseX <= btnX + buttonWidth && mouseY >= y && mouseY <= y + height && button == 0) {
                catSet.index = i; // Zmień zakładkę
                return;
            }
        }
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int button) {}
    @Override public void keyTyped(char typedChar, int keyCode) {}
}