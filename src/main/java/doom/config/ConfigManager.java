package doom.config;

import com.google.gson.*;
import doom.Client;
import doom.module.Module;
import doom.settings.Setting;
import doom.settings.impl.*;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Map;

public class ConfigManager {

    public final File configDir;
    private final File defaultConfigFile;

    public ConfigManager() {
        this.configDir = new File(Minecraft.getMinecraft().mcDataDir, "Doom");
        if (!this.configDir.exists()) {
            this.configDir.mkdir();
        }
        this.defaultConfigFile = new File(configDir, "config.json");
    }

    public void save() { saveToFile(this.defaultConfigFile); }
    public void save(String name) { saveToFile(new File(configDir, name + ".json")); }

    public void load() { loadFromFile(this.defaultConfigFile); }
    public boolean load(String name) {
        File file = new File(configDir, name + ".json");
        if (!file.exists()) return false;
        loadFromFile(file);
        return true;
    }

    // --- NOWA METODA: ŁADOWANIE Z TEKSTU (Dla Cloud Configs) ---
    public void loadFromString(String jsonContent) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(jsonContent);
            if (element.isJsonObject()) {
                loadFromJson(element.getAsJsonObject()); // Używamy wspólnej logiki
                Client.addChatMessage("§a[Cloud] Config loaded successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Client.addChatMessage("§c[Cloud] Failed to parse config!");
        }
    }
    // -----------------------------------------------------------

    private void saveToFile(File file) {
        // (Twój dotychczasowy kod saveToFile - bez zmian)
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();
        JsonObject modulesObject = new JsonObject();

        for (Module m : Client.INSTANCE.moduleManager.modules) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("toggled", m.isToggled());
            moduleJson.addProperty("key", m.getKey());
            moduleJson.addProperty("posX", m.x);
            moduleJson.addProperty("posY", m.y);

            JsonObject settingsJson = new JsonObject();
            for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(m)) {
                if (s instanceof BooleanSetting) {
                    settingsJson.addProperty(s.name, ((BooleanSetting) s).isEnabled());
                } else if (s instanceof NumberSetting) {
                    settingsJson.addProperty(s.name, ((NumberSetting) s).getValue());
                } else if (s instanceof ModeSetting) {
                    settingsJson.addProperty(s.name, ((ModeSetting) s).getMode());
                } else if (s instanceof ColorSetting) {
                    settingsJson.addProperty(s.name, ((ColorSetting) s).getColor());
                } else if (s instanceof CategorySetting) {
                    settingsJson.addProperty(s.name, ((CategorySetting) s).getSelected());
                }
            }
            moduleJson.add("Settings", settingsJson);
            modulesObject.add(m.getName(), moduleJson);
        }
        root.add("Modules", modulesObject);

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(gson.toJson(root));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void loadFromFile(File file) {
        if (!file.exists()) return;
        try {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(new FileReader(file));
            if (!element.isJsonObject()) return;

            loadFromJson(element.getAsJsonObject()); // Wywołujemy wspólną logikę

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- LOGIKA PARSOWANIA WYDZIELONA DO OSOBNEJ METODY ---
    private void loadFromJson(JsonObject root) {
        if (!root.has("Modules")) return;

        JsonObject modulesObject = root.getAsJsonObject("Modules");

        for (Map.Entry<String, JsonElement> entry : modulesObject.entrySet()) {
            Module m = Client.INSTANCE.moduleManager.getModuleByName(entry.getKey());

            if (m != null) {
                JsonObject moduleJson = entry.getValue().getAsJsonObject();

                if (moduleJson.has("toggled")) {
                    boolean fileState = moduleJson.get("toggled").getAsBoolean();
                    if (m.isToggled() != fileState) m.setToggled(fileState);
                }
                if (moduleJson.has("key")) m.setKey(moduleJson.get("key").getAsInt());
                if (moduleJson.has("posX")) m.x = moduleJson.get("posX").getAsFloat();
                if (moduleJson.has("posY")) m.y = moduleJson.get("posY").getAsFloat();

                if (moduleJson.has("Settings")) {
                    JsonObject settingsJson = moduleJson.getAsJsonObject("Settings");

                    for (Map.Entry<String, JsonElement> setEntry : settingsJson.entrySet()) {
                        String settingName = setEntry.getKey();
                        JsonElement settingValue = setEntry.getValue();

                        Setting s = Client.INSTANCE.settingsManager.getSettingByName(m, settingName);

                        if (s != null) {
                            try {
                                if (s instanceof BooleanSetting) {
                                    ((BooleanSetting) s).setEnabled(settingValue.getAsBoolean());
                                } else if (s instanceof NumberSetting) {
                                    ((NumberSetting) s).setValue(settingValue.getAsDouble());
                                } else if (s instanceof ModeSetting) {
                                    ((ModeSetting) s).setMode(settingValue.getAsString());
                                } else if (s instanceof ColorSetting) {
                                    ((ColorSetting) s).setColor(settingValue.getAsInt());
                                } else if (s instanceof CategorySetting) {
                                    ((CategorySetting) s).setOption(settingValue.getAsString());
                                }
                            } catch (Exception e) {
                                System.out.println("Error loading setting: " + settingName);
                            }
                        }
                    }
                }
            }
        }
    }
}