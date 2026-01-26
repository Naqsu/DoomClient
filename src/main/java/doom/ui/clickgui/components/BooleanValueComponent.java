package doom.ui.clickgui.components;

import doom.module.impl.render.ClickGuiModule;
import doom.settings.impl.BooleanSetting;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;

import java.awt.*;

public class BooleanValueComponent extends Component {

    private float animation = 0.0f;

    public BooleanValueComponent(BooleanSetting setting, float width, float height) {
        super(setting, width, height);
    }

    @Override
    public void draw(float x, float y, int mouseX, int mouseY) {
        this.x = x; this.y = y;
        BooleanSetting bool = (BooleanSetting) setting;

        FontManager.r18.drawStringWithShadow(setting.name, x + 5, y + height / 2f - 4, -1);

        float switchWidth = 24;
        float switchHeight = 12;
        float switchX = x + width - switchWidth - 5;
        float switchY = y + (height - switchHeight) / 2f;

        float target = bool.isEnabled() ? 1.0f : 0.0f;
        animation = RenderUtil.lerp(animation, target, 0.15f);

        // POBIERANIE KOLORU Z THEME
        Color themeColor = ClickGuiModule.getGuiColor();
        int activeColor = themeColor.getRGB();
        int inactiveColor = new Color(60, 60, 60).getRGB();

        // Mieszanie kolorów (Szary -> Theme)
        int colorBg = interpolateColor(inactiveColor, activeColor, animation);

        RenderUtil.drawRoundedRect(switchX, switchY, switchWidth, switchHeight, switchHeight / 2.0f, colorBg);

        float knobSize = switchHeight - 4;
        float knobX = switchX + 2 + ((switchWidth - 4 - knobSize) * animation);
        RenderUtil.drawRoundedRect(knobX, switchY + 2, knobSize, knobSize, knobSize / 2.0f, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0) {
            ((BooleanSetting) setting).toggle();
        }
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int button) {}
    @Override public void keyTyped(char typedChar, int keyCode) {}

    private int interpolateColor(int start, int end, float fraction) {
        if(fraction > 1) fraction = 1; if(fraction < 0) fraction = 0;
        int a1 = (start >> 24) & 0xFF, r1 = (start >> 16) & 0xFF, g1 = (start >> 8) & 0xFF, b1 = start & 0xFF;
        int a2 = (end >> 24) & 0xFF, r2 = (end >> 16) & 0xFF, g2 = (end >> 8) & 0xFF, b2 = end & 0xFF;
        int a = (int)(a1+(a2-a1)*fraction), r = (int)(r1+(r2-r1)*fraction), g = (int)(g1+(g2-g1)*fraction), b = (int)(b1+(b2-b1)*fraction);
        return (a<<24)|(r<<16)|(g<<8)|b;
    }
}