package doom.settings.impl;

import doom.module.Module;
import doom.settings.Setting;

import java.util.ArrayList;

public class SettingsManager {
    private ArrayList<Setting> settings;

    public SettingsManager() {
        this.settings = new ArrayList<>();
    }

    // Dodawanie pojedynczego ustawienia
    public void rSetting(Setting setting) {
        this.settings.add(setting);
    }

    // 🔥 NOWA METODA: Dodawanie wielu ustawień naraz
    public void addSettings(Setting... settingsToAdd) {
        for (Setting setting : settingsToAdd) {
            rSetting(setting); // korzystamy z istniejącej logiki
        }
    }

    // Pobieranie listy ustawień dla konkretnego modułu
    public ArrayList<Setting> getSettingsByMod(Module mod) {
        ArrayList<Setting> out = new ArrayList<>();
        for (Setting s : settings) {
            if (s.parent.equals(mod)) {
                out.add(s);
            }
        }
        return out;
    }

    // Pobieranie konkretnego ustawienia po nazwie i module
    public Setting getSettingByName(Module mod, String name) {
        for (Setting s : settings) {
            if (s.parent.equals(mod) && s.name.equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }
}