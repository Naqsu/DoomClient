package doom.module.impl.render;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.*;
import doom.util.ColorUtil;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class HUD extends Module {

    // --- ZAKŁADKI GŁÓWNE ---
    public CategorySetting pages = new CategorySetting("Page", this, "General", "General", "Watermark", "ArrayList");

    // ===========================================
    //               SEKCJA: GENERAL
    // ===========================================
    // Tutaj globalne opcje renderowania
    public BooleanSetting blur = new BooleanSetting("Blur", this, true);
    public ModeSetting font = new ModeSetting("Font", this, "Regular", "Regular", "Bold");

    // ===========================================
    //               SEKCJA: WATERMARK
    // ===========================================
    public BooleanSetting wmEnabled = new BooleanSetting("Enabled", this, true);
    public ModeSetting wmTheme = new ModeSetting("WM Theme", this, "Custom", "Custom", "Rainbow", "Astolfo", "Doom");

    // Custom Colors dla Watermarka
    public ColorSetting wmColor1 = new ColorSetting("WM Color 1", this, new Color(225, 30, 30).getRGB());
    public ColorSetting wmColor2 = new ColorSetting("WM Color 2", this, new Color(100, 0, 0).getRGB());
    public NumberSetting wmSpeed = new NumberSetting("WM Speed", this, 3.0, 0.1, 10.0, 0.1);

    // ===========================================
    //               SEKCJA: ARRAYLIST
    // ===========================================
    public BooleanSetting alEnabled = new BooleanSetting("Enabled", this, true);
    public ModeSetting alAlign = new ModeSetting("Align", this, "Right", "Right", "Left");
    public BooleanSetting alSidebar = new BooleanSetting("Sidebar", this, true);
    public BooleanSetting alSuffix = new BooleanSetting("Suffix", this, true);

    public ModeSetting alTheme = new ModeSetting("AL Theme", this, "Custom", "Custom", "Rainbow", "Astolfo", "Doom");

    // Custom Colors dla ArrayListy
    public ColorSetting alColor1 = new ColorSetting("AL Color 1", this, new Color(225, 30, 30).getRGB());
    public ColorSetting alColor2 = new ColorSetting("AL Color 2", this, new Color(100, 0, 0).getRGB());
    public NumberSetting alSpeed = new NumberSetting("AL Speed", this, 3.0, 0.1, 10.0, 0.1);


    public HUD() {
        super("HUD", Keyboard.KEY_NONE, Category.RENDER);
        this.setToggled(true);

        // 1. ZAKŁADKI
        Client.INSTANCE.settingsManager.rSetting(pages);

        // 2. REJESTRACJA GENERAL
        Client.INSTANCE.settingsManager.rSetting(blur);
        Client.INSTANCE.settingsManager.rSetting(font);

        // 3. REJESTRACJA WATERMARK
        Client.INSTANCE.settingsManager.rSetting(wmEnabled);
        Client.INSTANCE.settingsManager.rSetting(wmTheme);
        Client.INSTANCE.settingsManager.rSetting(wmColor1);
        Client.INSTANCE.settingsManager.rSetting(wmColor2);
        Client.INSTANCE.settingsManager.rSetting(wmSpeed);

        // 4. REJESTRACJA ARRAYLIST
        Client.INSTANCE.settingsManager.rSetting(alEnabled);
        Client.INSTANCE.settingsManager.rSetting(alAlign);
        Client.INSTANCE.settingsManager.rSetting(alSidebar);
        Client.INSTANCE.settingsManager.rSetting(alSuffix);
        Client.INSTANCE.settingsManager.rSetting(alTheme);
        Client.INSTANCE.settingsManager.rSetting(alColor1);
        Client.INSTANCE.settingsManager.rSetting(alColor2);
        Client.INSTANCE.settingsManager.rSetting(alSpeed);

        // ================= ZALEŻNOŚCI (WIDOCZNOŚĆ) =================

        // --- GENERAL ---
        blur.setDependency(() -> pages.is("General"));
        font.setDependency(() -> pages.is("General"));

        // --- WATERMARK ---
        wmEnabled.setDependency(() -> pages.is("Watermark"));
        wmTheme.setDependency(() -> pages.is("Watermark") && wmEnabled.isEnabled());
        // Color 1: Widoczny dla Custom i Doom
        wmColor1.setDependency(() -> pages.is("Watermark") && wmEnabled.isEnabled() && (wmTheme.is("Custom") || wmTheme.is("Doom")));
        // Color 2: Widoczny tylko dla Doom (gradient)
        wmColor2.setDependency(() -> pages.is("Watermark") && wmEnabled.isEnabled() && wmTheme.is("Doom"));
        // Speed: Widoczny dla Doom i Rainbow i Astolfo
        wmSpeed.setDependency(() -> pages.is("Watermark") && wmEnabled.isEnabled() && !wmTheme.is("Custom"));

        // --- ARRAYLIST ---
        alEnabled.setDependency(() -> pages.is("ArrayList"));
        alAlign.setDependency(() -> pages.is("ArrayList") && alEnabled.isEnabled());
        alSidebar.setDependency(() -> pages.is("ArrayList") && alEnabled.isEnabled());
        alSuffix.setDependency(() -> pages.is("ArrayList") && alEnabled.isEnabled());
        alTheme.setDependency(() -> pages.is("ArrayList") && alEnabled.isEnabled());

        // Color 1: Custom i Doom
        alColor1.setDependency(() -> pages.is("ArrayList") && alEnabled.isEnabled() && (alTheme.is("Custom") || alTheme.is("Doom")));
        // Color 2: Doom
        alColor2.setDependency(() -> pages.is("ArrayList") && alEnabled.isEnabled() && alTheme.is("Doom"));
        // Speed: Wszystkie animowane
        alSpeed.setDependency(() -> pages.is("ArrayList") && alEnabled.isEnabled() && !alTheme.is("Custom"));
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        toggleElement(Watermark.class, wmEnabled.isEnabled());
        toggleElement(ActiveModules.class, alEnabled.isEnabled());
    }

    private void toggleElement(Class<? extends Module> clazz, boolean shouldBeEnabled) {
        Module m = Client.INSTANCE.moduleManager.getModule(clazz);
        if (m != null && m.isToggled() != shouldBeEnabled) m.setToggled(shouldBeEnabled);
    }

    // --- LOGIKA KOLORÓW DLA ARRAYLISTY ---
    public int getArrayListColor(int index) {
        double speed = alSpeed.getValue();
        switch (alTheme.getMode()) {
            case "Custom":
                return alColor1.getColor();
            case "Rainbow":
                return ColorUtil.getRainbow(4.0f, 0.6f, 1.0f, index * 200L);
            case "Astolfo":
                return ColorUtil.getAstolfo(index, 10, 0.5f, 1.0f);
            case "Doom":
                // Customowy Gradient (Color 1 -> Color 2)
                return getCustomGradient(alColor1.getColor(), alColor2.getColor(), index, speed);
            default: return -1;
        }
    }

    // --- LOGIKA KOLORÓW DLA WATERMARKA ---
    public int getWatermarkColor() {
        double speed = wmSpeed.getValue();
        switch (wmTheme.getMode()) {
            case "Custom":
                return wmColor1.getColor();
            case "Rainbow":
                return ColorUtil.getRainbow(4.0f, 0.6f, 1.0f, 0);
            case "Astolfo":
                return ColorUtil.getAstolfo(0, 10, 0.5f, 1.0f);
            case "Doom":
                // Index 0, bo watermark ma jeden kolor
                return getCustomGradient(wmColor1.getColor(), wmColor2.getColor(), 0, speed);
            default: return -1;
        }
    }

    // --- HELPER DO GRADIENTU (DOOM MODE) ---
    private int getCustomGradient(int c1, int c2, int index, double speed) {
        // Obliczamy czas (fala sinus)
        double time = (System.currentTimeMillis() * speed + index * 100) % 2000.0 / 1000.0; // 0.0 -> 2.0
        double wave = 0.5 + 0.5 * Math.sin(time * Math.PI); // 0.0 -> 1.0 (płynnie)

        Color col1 = new Color(c1);
        Color col2 = new Color(c2);

        int r = (int) (col1.getRed() * wave + col2.getRed() * (1.0 - wave));
        int g = (int) (col1.getGreen() * wave + col2.getGreen() * (1.0 - wave));
        int b = (int) (col1.getBlue() * wave + col2.getBlue() * (1.0 - wave));

        return new Color(r, g, b).getRGB();
    }
}