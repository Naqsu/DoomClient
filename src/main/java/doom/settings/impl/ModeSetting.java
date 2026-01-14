package doom.settings.impl;

import doom.module.Module;
import doom.settings.Setting;
import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting {
    public int index;
    public List<String> modes;
    public boolean expanded = false; // Czy lista jest rozwinięta

    public ModeSetting(String name, Module parent, String defaultMode, String... modes) {
        super(name, parent);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(defaultMode);
    }

    public String getMode() {
        return modes.get(index);
    }

    public boolean is(String mode) {
        return index == modes.indexOf(mode);
    }

    // Ustawia tryb po nazwie (dla klikania w listę)
    public void setMode(String mode) {
        int i = modes.indexOf(mode);
        if (i != -1) index = i;
    }

    public void cycle() {
        if (index < modes.size() - 1) index++;
        else index = 0;
    }
}