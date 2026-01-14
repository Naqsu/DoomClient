package doom.ui.clickgui.components;

import doom.settings.Setting;
import net.minecraft.client.Minecraft;

public abstract class Component {
    protected Minecraft mc = Minecraft.getMinecraft();
    public Setting setting;
    public float x, y, width, height;

    public Component(Setting setting, float width, float height) {
        this.setting = setting;
        this.width = width;
        this.height = height;
    }

    public abstract void draw(float x, float y, int mouseX, int mouseY);
    public abstract void mouseClicked(int mouseX, int mouseY, int button);
    public abstract void mouseReleased(int mouseX, int mouseY, int button);
    public abstract void keyTyped(char typedChar, int keyCode);

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}