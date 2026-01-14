package doom.settings.impl;

import doom.module.Module;
import doom.settings.Setting;

public class StringSetting extends Setting {
    private String text;

    public StringSetting(String name, Module parent, String text) {
        super(name, parent);
        this.text = text;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}