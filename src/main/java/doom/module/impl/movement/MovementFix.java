package doom.module.impl.movement;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.util.RotationUtil;
import org.lwjgl.input.Keyboard;

public class MovementFix extends Module {

    public BooleanSetting silent = new BooleanSetting("Silent", this, true);
    // Możesz dodać np. "Aggressive" lub "Strict" mode w przyszłości

    public MovementFix() {
        super("MovementFix", Keyboard.KEY_NONE, Category.MOVEMENT);
        // Ukrywamy go, bo jest systemowy, ale można go toggle'ować przez np. komendę .toggle MovementFix
        // albo dać go do GUI w zakładce Movement jako opcję
        this.hidden = true;
        Client.INSTANCE.settingsManager.rSetting(silent);
        this.setToggled(true); // Domyślnie włączony
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // Resetujemy flagę na początku klatki (EntityPlayerSP czyta ją później)
        // Jeśli żaden moduł nie ustawił rotationRequested = true, to isRotating będzie false.

        if (RotationUtil.rotationRequested) {
            // Jakiś moduł (Killaura/Scaffold) chce rotować.

            if (this.silent.isEnabled()) {
                // Pozwalamy na Silent Move Fix
                RotationUtil.isRotating = true;
            } else {
                // Jeśli Silent wyłączony, to musimy obrócić gracza fizycznie (Lock View)
                // Wtedy Movement Fix nie jest potrzebny, bo W idzie w stronę celownika.
                mc.thePlayer.rotationYaw = RotationUtil.targetYaw;
                RotationUtil.isRotating = false;
            }

            // Reset prośby na następną klatkę (moduły muszą prosić co tick)
            RotationUtil.rotationRequested = false;
        } else {
            RotationUtil.isRotating = false;
        }
    }

    @Override
    public void onDisable() {
        RotationUtil.reset();
    }
}