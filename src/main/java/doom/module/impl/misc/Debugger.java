package doom.module.impl.misc;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;

import java.text.DecimalFormat;

public class Debugger extends Module {

    public BooleanSetting checkSimulation = new BooleanSetting("Check Simulation", this, true);
    private final DecimalFormat df = new DecimalFormat("0.000");

    public Debugger() {
        super("Debugger", 0, Category.MISC);
        Client.INSTANCE.settingsManager.rSetting(checkSimulation);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!checkSimulation.isEnabled()) return;

        // Inputy wysyłane do serwera (z Eventu, a nie z gracza!)
        float forward = event.getMoveForward();
        float strafe = event.getMoveStrafe();

        // Jeśli nie wysyłamy ruchu, ignoruj
        if (forward == 0 && strafe == 0) return;

        // Rotacja wysyłana do serwera
        float serverYaw = event.getYaw();

        // Obliczamy faktyczny kierunek ruchu wynikający z inputów
        // forward to oś Z (lokalna), strafe to oś X (lokalna)
        // Kąt w radianach dla inputów:
        double inputAngle = Math.atan2(strafe, forward);
        // Konwertujemy na stopnie i odejmujemy od rotacji serwera (MC math)
        float moveYaw = serverYaw - (float) Math.toDegrees(inputAngle);

        // Teraz sprawdzamy "Residue" - czyli czy ruch pasuje do siatki
        // Grim wymaga, aby inputy były całkowite (chyba że sneak).
        // Sprawdzamy czy forward/strafe są blisko -1, 0, 1

        boolean validForward = isInteger(forward);
        boolean validStrafe = isInteger(strafe);

        if (!validForward || !validStrafe) {
            // Tylko jeśli nie skradamy (przy skradaniu inputy są ułamkowe np. 0.3)
            if (!event.isSneaking()) {
                Client.addChatMessage("§c[FAIL] §fInvalid Input! F: " + df.format(forward) + " S: " + df.format(strafe));
            }
        }
    }

    private boolean isInteger(float val) {
        return Math.abs(val - Math.round(val)) < 0.0001;
    }
}