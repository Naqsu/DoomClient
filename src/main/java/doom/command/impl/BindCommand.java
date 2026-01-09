package doom.command.impl;

import doom.Client;
import doom.command.Command;
import doom.module.Module;
import org.lwjgl.input.Keyboard;

public class BindCommand extends Command {

    public BindCommand() {
        // Opis po angielsku
        super("bind", "Sets keybind for a module", ".bind <module> <key>");
    }

    @Override
    public void onChat(String[] args) {
        if (args.length < 3) {
            Client.addChatMessage("Usage: " + getSyntax());
            return;
        }

        String moduleName = args[1];
        String keyName = args[2].toUpperCase();

        Module m = Client.INSTANCE.moduleManager.getModuleByName(moduleName);

        if (m == null) {
            Client.addChatMessage("Module not found: " + moduleName);
            return;
        }

        int key = Keyboard.getKeyIndex(keyName);

        m.setKey(key);
        // Wiadomość po angielsku
        Client.addChatMessage("Bound §c" + m.getName() + "§f to key §a" + keyName);
    }
}