package doom.settings.impl;

import doom.module.Module;
import doom.settings.Setting;
import java.awt.Color;

public class ColorSetting extends Setting {
    private int color;
    public boolean expanded = false;

    public ColorSetting(String name, Module parent, int color) {
        super(name, parent);
        this.color = color;
    }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    // Zwraca float[4]: {Hue, Saturation, Brightness, Alpha}
    public float[] getHSBA() {
        Color c = new Color(color, true); // true = obsługuje alpha
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        return new float[] { hsb[0], hsb[1], hsb[2], c.getAlpha() / 255.0f };
    }

    // Ustawia kolor z HSBA
    public void setHSBA(float h, float s, float b, float a) {
        int rgb = Color.HSBtoRGB(h, s, b);
        int alpha = (int)(a * 255);
        this.color = (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}