package doom.module.impl.render;

import doom.Client;
import doom.module.Module;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import org.lwjgl.input.Keyboard;

public class Animations extends Module {

    public ModeSetting mode;
    public NumberSetting x, y, z; // Pozycje (opcjonalnie, do customizacji)
    public NumberSetting swingSpeed;

    public Animations() {
        super("Animations", Keyboard.KEY_NONE, Category.RENDER);

        // Tryby animacji
        mode = new ModeSetting("Mode", this, "1.7", "1.7", "Sigma", "Exhibition", "Slide", "Swang");

        // Szybkość machania (dla efektu Slow Swing)
        swingSpeed = new NumberSetting("Swing Speed", this, 1.0, 0.5, 2.0, 0.1);

        // Opcjonalne przesunięcia (jeśli chcesz przesuwać miecz)
        x = new NumberSetting("X", this, 0.0, -2.0, 2.0, 0.1);
        y = new NumberSetting("Y", this, 0.0, -2.0, 2.0, 0.1);
        z = new NumberSetting("Z", this, 0.0, -2.0, 2.0, 0.1);

        Client.INSTANCE.settingsManager.rSetting(mode);
        Client.INSTANCE.settingsManager.rSetting(swingSpeed);
         Client.INSTANCE.settingsManager.rSetting(x); // Odkomentuj jeśli chcesz suwaki pozycji
         Client.INSTANCE.settingsManager.rSetting(y);
         Client.INSTANCE.settingsManager.rSetting(z);
    }
}