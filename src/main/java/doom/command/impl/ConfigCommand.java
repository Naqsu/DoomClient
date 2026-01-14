package doom.command.impl;

import doom.Client;
import doom.command.Command;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        // Nazwa, opis, składnia
        super("config", "Manages configs (save/load)", ".config <save/load> <name>");
    }

    @Override
    public void onChat(String[] args) {
        // Sprawdzamy czy użytkownik wpisał wystarczająco dużo argumentów
        if (args.length < 2) {
            Client.addChatMessage("§cUsage: .config <save/load> <name>");
            return;
        }

        String action = args[1]; // np. "save"
        String configName = args[2]; // np. "legit"

        switch (action.toLowerCase()) {
            case "save":
                // Wywołujemy zapis z nazwą (musisz zaktualizować ConfigManagera, kod niżej)
                Client.INSTANCE.configManager.save(configName);
                Client.addChatMessage("§aSuccessfully saved config: §e" + configName);
                break;

            case "load":
                // Wywołujemy wczytanie z nazwą
                boolean success = Client.INSTANCE.configManager.load(configName);
                if (success) {
                    Client.addChatMessage("§aSuccessfully loaded config: §e" + configName);
                } else {
                    Client.addChatMessage("§cConfig §e" + configName + " §cnot found!");
                }
                break;

            default:
                Client.addChatMessage("§cUnknown action. Use 'save' or 'load'.");
                break;
        }
    }
}