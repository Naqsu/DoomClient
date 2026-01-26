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

        // Dodatkowe sprawdzanie dla Simulation - długość wektora ruchu
        float len = (float) Math.sqrt(forward * forward + strafe * strafe);
        if (len > 1.0f + 0.0001f) { // Mały epsilon na floating point
            Client.addChatMessage("§c[FAIL Simulation] §fMovement vector too long! Len: " + df.format(len) + " (should be <=1.0)");
        }

        // Symulacja BadPacketsX / DuplicateRotPlace - sprawdzamy czy yaw się zmienił od ostatniego
        // (wymaga pola lastYaw, dodaję poniżej)
        if (Math.abs(serverYaw - lastYaw) < 0.01f) { // Minimalna zmiana
            Client.addChatMessage("§c[FAIL DuplicateRot] §fYaw almost unchanged: " + df.format(serverYaw) + " (prev: " + df.format(lastYaw) + ")");
        }
        lastYaw = serverYaw;

        // Symulacja PacketOrderF - jeśli sprinting=true, a place jest w toku, ale to trudniejsze bez eventu place
        // Zakładam, że jeśli sprinting i forward !=0, to może flagować order
        if (event.isSprinting() && (forward != 0 || strafe != 0)) {
            Client.addChatMessage("§c[FAIL PacketOrderF] §fSprinting + movement detected (may cause order flags)");
        }
    }

    private float lastYaw = 0.0f; // Pole do śledzenia poprzedniego yaw

    private boolean isInteger(float val) {
        return Math.abs(val - Math.round(val)) < 0.0001;
    }
}