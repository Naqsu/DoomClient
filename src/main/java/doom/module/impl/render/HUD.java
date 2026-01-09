package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import org.lwjgl.input.Keyboard;

public class HUD extends Module {

    // Toggle elementów
    public BooleanSetting watermark;
    public BooleanSetting arraylist;
    public BooleanSetting info;

    // Ustawienia Wyglądu (Globalne dla HUD)
    public BooleanSetting background;
    public BooleanSetting sidebar;
    public BooleanSetting rainbow;

    // Ustawienia ArrayListy
    public ModeSetting alignMode; // Left, Right
    public ModeSetting suffixMode; // None, Simple, Bracket, Dash

    public HUD() {
        super("HUD", Keyboard.KEY_NONE, Category.RENDER);

        // Elementy
        watermark = new BooleanSetting("Elements: Watermark", this, true);
        arraylist = new BooleanSetting("Elements: ArrayList", this, true);
        info = new BooleanSetting("Elements: Info", this, true);

        // Wygląd
        background = new BooleanSetting("Background", this, true);
        sidebar = new BooleanSetting("Sidebar", this, true);
        rainbow = new BooleanSetting("Rainbow", this, true);

        // Konfiguracja ArrayListy
        alignMode = new ModeSetting("Align", this, "Right", "Right", "Left");
        suffixMode = new ModeSetting("Suffix", this, "Gray", "None", "Gray", "White", "Dash", "Bracket");

        // Rejestracja w ClickGUI
        Client.INSTANCE.settingsManager.rSetting(watermark);
        Client.INSTANCE.settingsManager.rSetting(arraylist);
        Client.INSTANCE.settingsManager.rSetting(info);
        Client.INSTANCE.settingsManager.rSetting(background);
        Client.INSTANCE.settingsManager.rSetting(sidebar);
        Client.INSTANCE.settingsManager.rSetting(rainbow);
        Client.INSTANCE.settingsManager.rSetting(alignMode);
        Client.INSTANCE.settingsManager.rSetting(suffixMode);
    }

    @Override
    public void onEnable() { updateElements(); }

    @Override
    public void onDisable() {
        toggleElement(Watermark.class, false);
        toggleElement(ActiveModules.class, false);
        toggleElement(InfoHUD.class, false);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        updateElements();
    }

    private void updateElements() {
        toggleElement(Watermark.class, watermark.isEnabled());
        toggleElement(ActiveModules.class, arraylist.isEnabled());
        toggleElement(InfoHUD.class, info.isEnabled());
    }

    private void toggleElement(Class<? extends Module> clazz, boolean shouldBeEnabled) {
        if (Client.INSTANCE.moduleManager == null) return;
        Module m = Client.INSTANCE.moduleManager.getModule(clazz);
        if (m != null && m.isToggled() != shouldBeEnabled) {
            m.setToggled(shouldBeEnabled);
        }
    }
}