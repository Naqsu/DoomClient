package doom.command.impl;

import doom.Client;
import doom.command.Command;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Shows list of commands", ".help");
    }

    @Override
    public void onChat(String[] args) {
        Client.addChatMessage("Available commands:");
        for (Command c : Client.INSTANCE.commandManager.commands) {
            Client.addChatMessage("§c." + c.getName() + " §7- " + c.getDescription());
        }
    }
}