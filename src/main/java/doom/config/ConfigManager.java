package doom.config;

import com.google.gson.*;
import doom.Client;
import doom.module.Module;
import doom.settings.Setting;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import net.minecraft.client.Minecraft;

import java.io.*;
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

    public void save() {
        saveToFile(this.defaultConfigFile);
    }

    public void load() {
        loadFromFile(this.defaultConfigFile);
    }

    public void save(String name) {
        File file = new File(configDir, name + ".json");
        saveToFile(file);
    }

    public boolean load(String name) {
        File file = new File(configDir, name + ".json");
        if (!file.exists()) return false;
        loadFromFile(file);
        return true;
    }

    private void saveToFile(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();
        JsonObject modulesObject = new JsonObject();

        for (Module m : Client.INSTANCE.moduleManager.modules) {
            JsonObject moduleJson = new JsonObject();

            // 1. Zapisujemy podstawy (stan i klawisz)
            moduleJson.addProperty("toggled", m.isToggled());
            moduleJson.addProperty("key", m.getKey());

            moduleJson.addProperty("posX", m.x);
            moduleJson.addProperty("posY", m.y);

            // 2. Zapisujemy ustawienia (Settings)
            JsonObject settingsJson = new JsonObject();
            for (Setting s : Client.INSTANCE.settingsManager.getSettingsByMod(m)) {
                if (s instanceof BooleanSetting) {
                    settingsJson.addProperty(s.name, ((BooleanSetting) s).isEnabled());
                } else if (s instanceof NumberSetting) {
                    settingsJson.addProperty(s.name, ((NumberSetting) s).getValue());
                } else if (s instanceof ModeSetting) {
                    settingsJson.addProperty(s.name, ((ModeSetting) s).getMode());
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
            JsonObject root = (JsonObject) parser.parse(new FileReader(file));

            if (!root.has("Modules")) return;

            JsonObject modulesObject = root.getAsJsonObject("Modules");

            for (Map.Entry<String, JsonElement> entry : modulesObject.entrySet()) {
                Module m = Client.INSTANCE.moduleManager.getModuleByName(entry.getKey());

                if (m != null) {
                    JsonObject moduleJson = entry.getValue().getAsJsonObject();

                    // 1. Wczytujemy podstawy
                    if (moduleJson.has("toggled")) {
                        boolean fileState = moduleJson.get("toggled").getAsBoolean();
                        boolean currentState = m.isToggled();
                        if (fileState != currentState) m.toggle();
                    }

                    if (moduleJson.has("key")) {
                        m.setKey(moduleJson.get("key").getAsInt());
                    }
                    if (moduleJson.has("posX")) m.x = moduleJson.get("posX").getAsFloat();
                    if (moduleJson.has("posY")) m.y = moduleJson.get("posY").getAsFloat();

                    // 2. Wczytujemy ustawienia
                    if (moduleJson.has("Settings")) {
                        JsonObject settingsJson = moduleJson.getAsJsonObject("Settings");

                        for (Map.Entry<String, JsonElement> setEntry : settingsJson.entrySet()) {
                            String settingName = setEntry.getKey();
                            JsonElement settingValue = setEntry.getValue();

                            Setting s = Client.INSTANCE.settingsManager.getSettingByName(m, settingName);

                            if (s != null) {
                                if (s instanceof BooleanSetting) {
                                    ((BooleanSetting) s).setEnabled(settingValue.getAsBoolean());
                                }
                                else if (s instanceof NumberSetting) {
                                    ((NumberSetting) s).setValue(settingValue.getAsDouble());
                                }
                                else if (s instanceof ModeSetting) {
                                    ModeSetting modeSetting = (ModeSetting) s;
                                    String modeName = settingValue.getAsString();

                                    // Musimy znaleźć index na podstawie nazwy
                                    for(int i = 0; i < modeSetting.modes.size(); i++) {
                                        if(modeSetting.modes.get(i).equalsIgnoreCase(modeName)) {
                                            modeSetting.index = i;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load config: " + file.getName());
        }
    }
}