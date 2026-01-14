package doom.ui.alt;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class AltManager {
    public static AltManager INSTANCE = new AltManager();
    public ArrayList<Alt> alts = new ArrayList<>();
    private File altsFile;

    public AltManager() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Doom");
        if (!dir.exists()) dir.mkdir();
        altsFile = new File(dir, "alts.json");
        loadAlts();
    }

    public ArrayList<Alt> getAlts() { return alts; }

    public void addAlt(Alt alt) {
        // Usuń starego, jeśli istnieje (aktualizacja tokena)
        alts.removeIf(a -> a.getUsername().equalsIgnoreCase(alt.getUsername()));
        alts.add(alt);
        saveAlts();
    }

    public void removeAlt(Alt alt) {
        alts.remove(alt);
        saveAlts();
    }

    public void saveAlts() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray array = new JsonArray();

        for (Alt alt : alts) {
            JsonObject json = new JsonObject();
            json.addProperty("username", alt.getUsername());
            json.addProperty("type", alt.getType().name());
            // Zapisujemy token (tylko dla MS)
            if (alt.getRefreshToken() != null) {
                json.addProperty("refreshToken", alt.getRefreshToken());
            }
            array.add(json);
        }

        try (PrintWriter writer = new PrintWriter(altsFile)) {
            writer.write(gson.toJson(array));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void loadAlts() {
        if (!altsFile.exists()) return;
        alts.clear();

        try {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(new FileReader(altsFile));

            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (JsonElement e : array) {
                    JsonObject json = e.getAsJsonObject();
                    String username = json.get("username").getAsString();
                    String typeStr = json.has("type") ? json.get("type").getAsString() : "OFFLINE";
                    Alt.AltType type = Alt.AltType.valueOf(typeStr);

                    String refreshToken = null;
                    if (json.has("refreshToken")) {
                        refreshToken = json.get("refreshToken").getAsString();
                    }

                    alts.add(new Alt(username, "", type, refreshToken));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}