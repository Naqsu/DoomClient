package doom.settings;

import doom.module.Module;

import java.util.function.Supplier;

public class Setting {
    public String name;
    public Module parent;
    public boolean hidden = false;

    // To jest nasza nowość - funkcja sprawdzająca widoczność
    private Supplier<Boolean> dependency;

    public Setting(String name, Module parent) {
        this.name = name;
        this.parent = parent;
        this.dependency = null; // Domyślnie brak zależności (zawsze widoczne)
    }

    // Metoda "Addonowa" - pozwala dodawać zależność w jednej linijce
    public void setDependency(Supplier<Boolean> dependency) {
        this.dependency = dependency;
    }

    // Zaktualizowana metoda isVisible
    public boolean isVisible() {
        if (hidden) return false;
        // Jeśli jest zależność, sprawdź ją. Jeśli nie ma, zwróć true.
        if (dependency != null) {
            return dependency.get();
        }
        return true;
    }
}