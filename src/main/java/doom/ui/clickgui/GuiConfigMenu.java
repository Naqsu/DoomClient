package doom.ui.clickgui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import doom.Client;
import doom.module.impl.render.ClickGuiModule;
import doom.ui.font.FontManager;
import doom.util.NetworkUtil;
import doom.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiConfigMenu extends GuiScreen {

    private final GuiScreen parent;
    private final String API_URL = "https://atamanco.eu/api/client";

    private boolean isOnlineTab = false;
    private final List<ConfigEntry> localConfigs = new ArrayList<>();
    private final List<ConfigEntry> onlineConfigs = new ArrayList<>();

    private float scrollY = 0;
    private float targetScrollY = 0;

    // Status message system
    private String status = "Idle";
    private long statusTimer = 0;

    public GuiConfigMenu(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        refreshLocal();
    }

    private void setStatus(String msg, boolean error) {
        this.status = (error ? "§c" : "§a") + msg;
        this.statusTimer = System.currentTimeMillis();
    }

    private void refreshLocal() {
        localConfigs.clear();
        try {
            File dir = Client.INSTANCE.configManager.configDir;
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".json") && !f.getName().equals("config.json") && !f.getName().equals("alts.json")) {
                            // Local configi nie mają daty z serwera, więc dajemy "Local"
                            localConfigs.add(new ConfigEntry(f.getName().replace(".json", ""), "You", "", "Local File", true));
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String safeGet(JsonObject obj, String key) {
        try {
            if (obj == null || !obj.has(key)) return "";
            JsonElement el = obj.get(key);
            return (el == null || el.isJsonNull()) ? "" : el.getAsString();
        } catch (Exception e) { return ""; }
    }

    private void fetchOnline() {
        onlineConfigs.clear();
        setStatus("Fetching...", false);
        new Thread(() -> {
            try {
                String jsonRaw = NetworkUtil.getTextFromURL(API_URL + "/configs");
                if (jsonRaw == null || jsonRaw.isEmpty()) { setStatus("Connection Failed", true); return; }

                JsonElement root = new JsonParser().parse(jsonRaw);
                if (root != null && root.isJsonArray()) {
                    JsonArray array = root.getAsJsonArray();
                    for (JsonElement e : array) {
                        if (!e.isJsonObject()) continue;
                        JsonObject obj = e.getAsJsonObject();

                        String name = safeGet(obj, "name");
                        if(name.isEmpty()) name = "Unknown";
                        String author = safeGet(obj, "author");

                        // Parsowanie daty (bierzemy tylko YYYY-MM-DD)
                        String rawDate = safeGet(obj, "date");
                        String date = rawDate.length() >= 10 ? rawDate.substring(0, 10) : "Unknown Date";

                        String id = safeGet(obj, "_id");
                        String url = id.isEmpty() ? safeGet(obj, "url") : API_URL + "/config/" + id;

                        if (!url.isEmpty()) onlineConfigs.add(new ConfigEntry(name, author, url, date, false));
                    }
                    // Nie ustawiamy statusu tutaj, żeby nie spamować "Loaded X configs" przy każdym kliknięciu
                    // setStatus("Loaded " + onlineConfigs.size(), false);
                }
            } catch (Exception e) { setStatus("Error fetching", true); }
        }).start();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Ciemne tło całego ekranu
        RenderUtil.drawRect(0, 0, width, height, new Color(0, 0, 0, 200).getRGB());

        float w = 550;
        float h = 350;
        float x = width / 2f - w/2;
        float y = height / 2f - h/2;

        // --- GŁÓWNE OKNO ---
        RenderUtil.drawRoundedRect(x, y, w, h, 8, new Color(20, 20, 25, 255).getRGB());
        RenderUtil.drawRoundedOutline(x, y, w, h, 8, 1.5f, new Color(40, 40, 45).getRGB());

        // Akcentowy pasek na górze
        RenderUtil.drawRoundedRect(x, y + 30, w, 1, 0, new Color(255, 255, 255, 20).getRGB());

        // --- HEADER ---
        FontManager.b20.drawStringWithShadow("Config Manager", x + 15, y + 10, -1);

        // Status (Znika po 3 sekundach)
        if (System.currentTimeMillis() - statusTimer < 3000) {
            float statusW = FontManager.r18.getStringWidth(status);
            FontManager.r18.drawStringWithShadow(status, x + w - statusW - 15, y + 12, -1);
        }

        // --- ZAKŁADKI (TABS) ---
        float tabW = 120;
        float tabX = x + 15;
        float tabY = y + 40;

        // Local Tab
        boolean activeLocal = !isOnlineTab;
        int colLocal = activeLocal ? ClickGuiModule.getGuiColor().getRGB() : new Color(60, 60, 60).getRGB();
        RenderUtil.drawRoundedRect(tabX, tabY, tabW, 20, 4, colLocal);
        FontManager.b18.drawCenteredString("Local Configs", tabX + tabW/2, tabY + 6, activeLocal ? -1 : 0xFFAAAAAA);

        // Online Tab
        boolean activeOnline = isOnlineTab;
        int colOnline = activeOnline ? ClickGuiModule.getGuiColor().getRGB() : new Color(60, 60, 60).getRGB();
        RenderUtil.drawRoundedRect(tabX + tabW + 10, tabY, tabW, 20, 4, colOnline);
        FontManager.b18.drawCenteredString("Cloud Configs", tabX + tabW + 10 + tabW/2, tabY + 6, activeOnline ? -1 : 0xFFAAAAAA);

        // --- LISTA ---
        float listX = x + 15;
        float listY = y + 70;
        float listW = w - 30;
        float listH = h - 85;

        // Scroll Logic
        int wheel = Mouse.getDWheel();
        if (wheel < 0) targetScrollY -= 25; else if (wheel > 0) targetScrollY += 25;

        List<ConfigEntry> currentList = isOnlineTab ? onlineConfigs : localConfigs;
        float entryHeight = 35;
        float gap = 5;
        float maxScroll = Math.max(0, (currentList.size() * (entryHeight + gap)) - listH);

        if (targetScrollY > 0) targetScrollY = 0;
        if (targetScrollY < -maxScroll) targetScrollY = -maxScroll;
        scrollY = RenderUtil.lerp(scrollY, targetScrollY, 0.2f);

        // SCISSOR START
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.scissor(listX, listY, listW, listH);

        float entryY = listY + scrollY;

        for (ConfigEntry entry : currentList) {
            if (entryY + entryHeight > listY && entryY < listY + listH) {

                // Tło wpisu
                RenderUtil.drawRoundedRect(listX, entryY, listW, entryHeight, 6, new Color(30, 30, 35).getRGB());
                RenderUtil.drawRoundedOutline(listX, entryY, listW, entryHeight, 6, 1.0f, new Color(50, 50, 55).getRGB());

                // Nazwa configu (Duża)
                FontManager.b18.drawStringWithShadow(entry.name, listX + 10, entryY + 8, -1);

                // Autor i Data (Małe, szare)
                String meta = "by \u00A7f" + entry.author + "\u00A77 - " + entry.date;
                FontManager.r18.drawStringWithShadow(meta, listX + 10, entryY + 20, new Color(150, 150, 150).getRGB());

                // --- PRZYCISKI (TEXT BASED) ---
                float btnH = 20;
                float btnY = entryY + (entryHeight - btnH) / 2;
                float rightX = listX + listW - 10;

                if (isOnlineTab) {
                    // DOWNLOAD BUTTON
                    float dlW = 70;
                    float dlX = rightX - dlW;
                    boolean hover = isHovered(dlX, btnY, dlW, btnH, mouseX, mouseY);
                    int col = hover ? ClickGuiModule.getGuiColor().getRGB() : new Color(50, 50, 60).getRGB();

                    RenderUtil.drawRoundedRect(dlX, btnY, dlW, btnH, 4, col);
                    FontManager.b18.drawCenteredString("Download", dlX + dlW/2, btnY + 6, -1);
                } else {
                    // DELETE BUTTON
                    float delW = 50;
                    float delX = rightX - delW;
                    boolean hoverDel = isHovered(delX, btnY, delW, btnH, mouseX, mouseY);
                    int colDel = hoverDel ? new Color(200, 50, 50).getRGB() : new Color(50, 50, 60).getRGB();

                    RenderUtil.drawRoundedRect(delX, btnY, delW, btnH, 4, colDel);
                    FontManager.b18.drawCenteredString("Del", delX + delW/2, btnY + 6, -1);

                    // LOAD BUTTON
                    float loadW = 50;
                    float loadX = delX - loadW - 5;
                    boolean hoverLoad = isHovered(loadX, btnY, loadW, btnH, mouseX, mouseY);
                    int colLoad = hoverLoad ? new Color(50, 200, 50).getRGB() : new Color(50, 50, 60).getRGB();

                    RenderUtil.drawRoundedRect(loadX, btnY, loadW, btnH, 4, colLoad);
                    FontManager.b18.drawCenteredString("Load", loadX + loadW/2, btnY + 6, -1);
                }
            }
            entryY += entryHeight + gap;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // --- SCROLLBAR ---
        if (maxScroll > 0) {
            float barH = listH * (listH / (currentList.size() * (entryHeight + gap)));
            float barY = listY + (-scrollY / maxScroll) * (listH - barH);
            RenderUtil.drawRoundedRect(listX + listW - 3, barY, 3, barH, 1.5f, new Color(80, 80, 80).getRGB());
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;

        float w = 550; float h = 350;
        float x = width / 2f - w/2; float y = height / 2f - h/2;

        // Tabs Click
        float tabW = 120;
        float tabY = y + 40;
        if (isHovered(x + 15, tabY, tabW, 20, mouseX, mouseY)) {
            isOnlineTab = false; targetScrollY = 0; refreshLocal(); return;
        }
        if (isHovered(x + 15 + tabW + 10, tabY, tabW, 20, mouseX, mouseY)) {
            isOnlineTab = true; targetScrollY = 0; fetchOnline(); return;
        }

        // List Click
        float listX = x + 15; float listY = y + 70; float listW = w - 30; float listH = h - 85;
        float entryHeight = 35; float gap = 5;
        float entryY = listY + scrollY;

        List<ConfigEntry> currentList = isOnlineTab ? onlineConfigs : localConfigs;

        for (ConfigEntry entry : currentList) {
            if (entryY + entryHeight > listY && entryY < listY + listH) {
                float btnH = 20;
                float btnY = entryY + (entryHeight - btnH) / 2;
                float rightX = listX + listW - 10;

                if (isOnlineTab) {
                    float dlW = 70;
                    if (isHovered(rightX - dlW, btnY, dlW, btnH, mouseX, mouseY)) {
                        downloadConfig(entry);
                        return;
                    }
                } else {
                    float delW = 50;
                    if (isHovered(rightX - delW, btnY, delW, btnH, mouseX, mouseY)) {
                        File f = new File(Client.INSTANCE.configManager.configDir, entry.name + ".json");
                        if(f.exists()) f.delete();
                        refreshLocal();
                        setStatus("Deleted " + entry.name, false);
                        return;
                    }
                    float loadW = 50;
                    if (isHovered(rightX - delW - loadW - 5, btnY, loadW, btnH, mouseX, mouseY)) {
                        Client.INSTANCE.configManager.load(entry.name);
                        setStatus("Loaded " + entry.name, false);
                        return;
                    }
                }
            }
            entryY += entryHeight + gap;
        }
    }

    private void downloadConfig(ConfigEntry entry) {
        setStatus("Downloading...", false);
        new Thread(() -> {
            try {
                String responseRaw = NetworkUtil.getTextFromURL(entry.url);
                if (responseRaw == null) { setStatus("Download Fail", true); return; }

                JsonObject response = new JsonParser().parse(responseRaw).getAsJsonObject();
                if (response.has("success") && response.get("success").getAsBoolean()) {
                    String configContent = response.get("content").getAsString();
                    File f = new File(Client.INSTANCE.configManager.configDir, entry.name + ".json");
                    java.io.PrintWriter writer = new java.io.PrintWriter(f);
                    writer.write(configContent);
                    writer.close();
                    setStatus("Downloaded " + entry.name, false);
                } else {
                    setStatus("Server Error", true);
                }
            } catch (Exception e) { setStatus("Save Error", true); }
        }).start();
    }

    private boolean isHovered(float x, float y, float w, float h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) mc.displayGuiScreen(parent);
    }

    private static class ConfigEntry {
        String name, author, url, date;
        boolean isLocal;
        public ConfigEntry(String name, String author, String url, String date, boolean isLocal) {
            this.name = name; this.author = author; this.url = url; this.date = date; this.isLocal = isLocal;
        }
    }
}