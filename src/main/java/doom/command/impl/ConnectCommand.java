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

        Client.addChatMessage("§eConnecting to Atamanco Cloud...");

        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("code", code);
                json.addProperty("hwid", hwid);

                // ZMIANA: HTTPS + Domena
                String response = HttpUtil.post("https://atamanco.eu/api/client/link", json.toString());

                JsonObject respObj = new JsonParser().parse(response).getAsJsonObject();

                if (respObj.has("success") && respObj.get("success").getAsBoolean()) {
                    String token = respObj.get("token").getAsString();
                    String username = respObj.get("username").getAsString();

                    DoomAccountManager.INSTANCE.saveToken(token);

                    Client.addChatMessage("§aSuccessfully linked account: §e" + username);
                    Client.addChatMessage("§7Connecting to Services...");

                    IRCClient.INSTANCE.connect();
                } else {
                    String msg = respObj.has("message") ? respObj.get("message").getAsString() : "Unknown error";
                    Client.addChatMessage("§cError: " + msg);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Client.addChatMessage("§cConnection failed! Server might be offline.");
            }
        }).start();
    }
}