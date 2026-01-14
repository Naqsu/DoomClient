package doom.command.impl;

import doom.Client;
import doom.command.Command;
import doom.module.impl.misc.AnticheatDetector;

public class DetectCommand extends Command {

    public DetectCommand() {
        super("detect", "Manually scans for AntiCheat", ".detect");
    }

    @Override
    public void onChat(String[] args) {
        AnticheatDetector ac = (AnticheatDetector) Client.INSTANCE.moduleManager.getModule(AnticheatDetector.class);

        if (ac == null) {
            Client.addChatMessage("§cError: AnticheatDetector module not found!");
            return;
        }

        // Włącz moduł jeśli wyłączony
        if (!ac.isToggled()) {
            ac.setToggled(true);
        }

        // Wymuś reset i ponowne skanowanie
        // Używamy refleksji lub publicznej metody (zakładam, że dodasz publiczną metodę do modułu)
        // Ale najprościej: wyłącz i włącz ponownie (to wywoła onEnable -> reset)
        ac.onEnable();

        Client.addChatMessage("§aStarting manual AntiCheat scan...");
    }
}