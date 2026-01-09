package doom.settings.impl;

import doom.module.Module;
import doom.settings.Setting;
import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting {
    public int index;
    public List<String> modes;

    public ModeSetting(String name, Module parent, String defaultMode, String... modes) {
        super(name, parent);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(defaultMode);
    }

    public String getMode() {
        return modes.get(index);
    }

    // Sprawdza czy dany tryb jest wybrany (np. if (mode.is("NCP")))
    public boolean is(String mode) {
        return index == modes.indexOf(mode);
    }

    // Przełącza na następny tryb (cyklicznie)
    public void cycle() {
        if (index < modes.size() - 1) {
            index++;
        } else {
            index = 0;
        }
    }
}