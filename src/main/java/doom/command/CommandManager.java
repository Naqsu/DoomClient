package doom.command;

import doom.Client;
import doom.command.impl.*;
import doom.event.EventManager;
import doom.event.EventTarget;
import doom.event.impl.EventChat;

import java.util.ArrayList;

public class CommandManager {
    public ArrayList<Command> commands = new ArrayList<>();

    public CommandManager() {
        commands.add(new BindCommand());
        commands.add(new HelpCommand());
        commands.add(new ConfigCommand());
        EventManager.register(this);
    }

    @EventTarget
    public void onChat(EventChat event) {
        String message = event.getMessage();

        if (message.startsWith(".")) {
            event.setCancelled(true);

            String[] args = message.substring(1).split(" ");

            if (args.length > 0) {
                String commandName = args[0];

                for (Command c : commands) {
                    if (c.getName().equalsIgnoreCase(commandName)) {
                        c.onChat(args);
                        return;
                    }
                }

                // ZMIANA NA ANGIELSKI
                Client.addChatMessage("Unknown command. Type .help for list.");
            }
        }
    }
}