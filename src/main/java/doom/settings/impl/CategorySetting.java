package doom.settings.impl;

import doom.module.Module;
import doom.settings.Setting;

import java.util.Arrays;
import java.util.List;

public class CategorySetting extends Setting {
    public int index = 0;
    public List<String> options;

    public CategorySetting(String name, Module parent, String defaultOption, String... options) {
        super(name, parent);
        this.options = Arrays.asList(options);
        this.index = this.options.indexOf(defaultOption);
    }

    public String getSelected() {
        return options.get(index);
    }

    // Metoda pomocnicza do dependency
    public boolean is(String optionName) {
        return options.get(index).equalsIgnoreCase(optionName);
    }

    public void setOption(String optionName) {
        int i = options.indexOf(optionName);
        if (i != -1) index = i;
    }
}