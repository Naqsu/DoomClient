package doom.settings;

import doom.module.Module;

public class Setting {
    public String name;
    public Module parent;
    public boolean hidden = false; // Opcja ukrywania ustawień

    public Setting(String name, Module parent) {
        this.name = name;
        this.parent = parent;
    }

    // Opcjonalne: czy pokazać ustawienie (przydatne do zależności)
    public boolean isVisible() {
        return !hidden;
    }
}