package doom.ui.clickgui.components;

import doom.module.impl.render.ClickGuiModule;
import doom.settings.impl.NumberSetting;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;

import java.awt.*;

public class NumberValueComponent extends Component {
    private boolean dragging = false;

    public NumberValueComponent(NumberSetting setting, float width, float height) {
        super(setting, width, height);
    }

    @Override
    public void draw(float x, float y, int mouseX, int mouseY) {
        this.x = x; this.y = y;
        NumberSetting num = (NumberSetting) setting;

        if (dragging) {
            float sliderWidth = width - 10;
            float diff = Math.min(sliderWidth, Math.max(0, mouseX - (x + 5)));
            double val = num.getMin() + (diff / sliderWidth) * (num.getMax() - num.getMin());
            num.setValue(val);
        }

        FontManager.r18.drawStringWithShadow(setting.name, x + 5, y + 2, -1);
        String valStr = String.format("%.2f", num.getValue());
        FontManager.r18.drawStringWithShadow(valStr, x + width - 5 - FontManager.r18.getStringWidth(valStr), y + 2, 0xFFAAAAAA);

        float sliderX = x + 5;
        float sliderY = y + 16;
        float sliderW = width - 10;
        float sliderH = 4;

        // POBIERANIE KOLORU Z THEME
        int themeColor = ClickGuiModule.getGuiColor().getRGB();

        // Tło paska
        RenderUtil.drawRoundedRect(sliderX, sliderY, sliderW, sliderH, 2, new Color(40, 40, 40).getRGB());

        // Wypełnienie (Theme Color)
        float fill = (float) ((num.getValue() - num.getMin()) / (num.getMax() - num.getMin()) * sliderW);
        RenderUtil.drawRoundedRect(sliderX, sliderY, fill, sliderH, 2, themeColor);

        // Kółeczko
        RenderUtil.drawRoundedRect(sliderX + fill - 4, sliderY - 2, 8, 8, 4, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0) dragging = true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;
    }

    @Override public void keyTyped(char typedChar, int keyCode) {}
}