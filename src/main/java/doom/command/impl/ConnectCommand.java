package doom.command.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import doom.Client;
import doom.account.DoomAccountManager;
import doom.command.Command;
import doom.irc.IRCClient;
import doom.util.HWIDUtil;
import doom.util.HttpUtil;

public class ConnectCommand extends Command {

    public ConnectCommand() {
        super("connectaccount", "Link web account", ".connectaccount <code>");
    }

    @Override
    public void onChat(String[] args) {
        if (args.length < 2) {
            Client.addChatMessage("Usage: " + getSyntax());
            return;
        }

        String code = args[1];
        String hwid = HWIDUtil.getHWID();

        Client.addChatMessage("§eConnecting to Doom Cloud...");

        new Thread(() -> {
            try {
                // Budowanie JSONa
                JsonObject json = new JsonObject();
                json.addProperty("code", code);
                json.addProperty("hwid", hwid);

                // ZMIEŃ IP NA SWOJEGO DEDYKA
                String response = HttpUtil.post("http://127.0.0.1:3000/api/client/link", json.toString());

                JsonObject respObj = new JsonParser().parse(response).getAsJsonObject();

                if (respObj.get("success").getAsBoolean()) {
                    String token = respObj.get("token").getAsString();
                    String username = respObj.get("username").getAsString();

                    // Zapisujemy token
                    DoomAccountManager.INSTANCE.saveToken(token);

                    Client.addChatMessage("§aSuccessfully linked account: §e" + username);
                    Client.addChatMessage("§7Connecting to IRC...");

                    // Łączymy z IRC
                    IRCClient.INSTANCE.connect();
                } else {
                    Client.addChatMessage("§cError: " + respObj.get("message").getAsString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                Client.addChatMessage("§cConnection failed! Check console.");
            }
        }).start();
    }
}