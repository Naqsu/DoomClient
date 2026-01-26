package doom.irc;

import doom.Client;
import doom.account.DoomAccountManager;
import doom.module.Module;
import doom.settings.Setting;
import doom.settings.impl.*;
import doom.util.HWIDUtil;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.minecraft.client.Minecraft;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;

public class IRCClient {

    public static IRCClient INSTANCE = new IRCClient();
    private Socket socket;

    // ZMIANA: Adres domeny z HTTPS
    private final String BACKEND_URL = "https://atamanco.eu";

    public void connect() {
        if (!DoomAccountManager.INSTANCE.isLoggedIn()) return;

        try {
            // ZMIANA: Konfiguracja pod HTTPS i Nginx
            IO.Options options = IO.Options.builder()
                    // Pozwalamy na polling i websocket (tak jak na stronie)
                    .setTransports(new String[] { "polling", "websocket" })
                    .setReconnection(true)
                    .setSecure(true) // Wymuszamy SSL
                    .build();

            socket = IO.socket(URI.create(BACKEND_URL), options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                try {
                    JSONObject auth = new JSONObject();
                    auth.put("token", DoomAccountManager.INSTANCE.getToken());
                    auth.put("type", "GAME");
                    auth.put("hwid", HWIDUtil.getHWID());
                    socket.emit("authenticate", auth);
                    Client.addChatMessage("§a[IRC] Connected to Atamanco Cloud!");
                } catch (Exception e) { e.printStackTrace(); }
            });

            // ... (reszta listenerów bez zmian: chat_message, load_config itp.) ...
            socket.on("chat_message", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String author = data.getString("author");
                    String message = data.getString("message");
                    String role = data.optString("role", "User");
                    String prefix = "§7";
                    if (role.equalsIgnoreCase("Creator")) prefix = "§6[DEV] ";
                    else if (role.equalsIgnoreCase("Admin")) prefix = "§c[ADMIN] ";
                    Client.addChatMessage("§8[IRC] " + prefix + "§f" + author + "§7: " + message);
                } catch (Exception e) { e.printStackTrace(); }
            });

            socket.on("request_sync", args -> sendFullSync());

            socket.on("load_config", args -> {
                try {
                    String configContent = (String) args[0];
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        Client.INSTANCE.configManager.loadFromString(configContent);
                        sendFullSync();
                    });
                    Client.addChatMessage("§a[Cloud] Config loaded remotely!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            socket.on("apply_action", args -> {
                try {
                    JSONObject action = (JSONObject) args[0];
                    String type = action.getString("type");
                    String moduleName = action.getString("module");

                    Module m = Client.INSTANCE.moduleManager.getModuleByName(moduleName);
                    if (m != null) {
                        if (type.equals("toggle")) {
                            boolean state = action.getBoolean("value");
                            if (m.isToggled() != state) m.toggle();
                        } else if (type.equals("setting")) {
                            String settingName = action.getString("settingName");
                            Object value = action.get("value");
                            updateSetting(m, settingName, value);
                            sendFullSync();
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });

            socket.connect();

        } catch (Exception e) {
            e.printStackTrace();
            Client.addChatMessage("§c[IRC] Connection Error: " + e.getMessage());
        }
    }

    // ... (reszta metod sendMessage, sendFullSync, updateSetting bez zmian) ...
    public void sendMessage(String msg) {
        if (socket != null && socket.connected()) {
            socket.emit("send_message", msg);
        } else {
            Client.addChatMessage("§c[IRC] Not connected!");
        }
    }

    public void sendFullSync() {
        if (socket == null || !socket.connected()) return;
        try {
            JSONArray modulesArray = new JSONArray();
            for (Module m : Client.INSTANCE.moduleManager.modules) {
                JSONObject modJson = new JSONObject();
                modJson.put("name", m.getName());
                modJson.put("category", m.getCategory().name());
                modJson.put("toggled", m.isToggled());
                JSONArray settingsArray = new JSONArray();
                ArrayList<Setting> settings = Client.INSTANCE.settingsManager.getSettingsByMod(m);
                if (settings != null) {
                    for (Setting s : settings) {
                        JSONObject setJson = new JSONObject();
                        setJson.put("name", s.name);
                        setJson.put("visible", s.isVisible());
                        if (s instanceof BooleanSetting) {
                            setJson.put("type", "Boolean");
                            setJson.put("value", ((BooleanSetting) s).isEnabled());
                        } else if (s instanceof NumberSetting) {
                            setJson.put("type", "Number");
                            setJson.put("value", ((NumberSetting) s).getValue());
                            setJson.put("min", ((NumberSetting) s).getMin());
                            setJson.put("max", ((NumberSetting) s).getMax());
                        } else if (s instanceof ModeSetting) {
                            setJson.put("type", "Mode");
                            setJson.put("value", ((ModeSetting) s).getMode());
                            setJson.put("modes", new JSONArray(((ModeSetting) s).modes));
                        } else if (s instanceof ColorSetting) {
                            setJson.put("type", "Color");
                            setJson.put("value", ((ColorSetting) s).getColor());
                        } else if (s instanceof CategorySetting) {
                            setJson.put("type", "Category");
                            setJson.put("value", ((CategorySetting) s).getSelected());
                            setJson.put("options", new JSONArray(((CategorySetting) s).options));
                        }
                        settingsArray.put(setJson);
                    }
                }
                modJson.put("settings", settingsArray);
                modulesArray.put(modJson);
            }
            socket.emit("sync_client_data", modulesArray);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateSetting(Module m, String settingName, Object value) {
        Setting s = Client.INSTANCE.settingsManager.getSettingByName(m, settingName);
        if (s == null) return;
        if (s instanceof BooleanSetting && value instanceof Boolean) ((BooleanSetting) s).setEnabled((Boolean) value);
        else if (s instanceof NumberSetting) ((NumberSetting) s).setValue(Double.parseDouble(value.toString()));
        else if (s instanceof ModeSetting && value instanceof String) ((ModeSetting) s).setMode((String) value);
        else if (s instanceof ColorSetting) ((ColorSetting) s).setColor(Integer.parseInt(value.toString()));
        else if (s instanceof CategorySetting && value instanceof String) ((CategorySetting) s).setOption((String) value);
    }
}