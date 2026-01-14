package doom.command.impl;

import doom.Client;
import doom.command.Command;
import doom.irc.IRCClient;

public class ChatCommand extends Command {

    public ChatCommand() {
        super("chat", "Send message to global IRC", ".chat <message>");
    }

    @Override
    public void onChat(String[] args) {
        if (args.length < 2) {
            Client.addChatMessage("Usage: .chat <message>");
            return;
        }

        StringBuilder msg = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            msg.append(args[i]).append(" ");
        }

        IRCClient.INSTANCE.sendMessage(msg.toString().trim());
    }
}