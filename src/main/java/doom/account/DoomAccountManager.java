package doom.account;

import net.minecraft.client.Minecraft;

import java.io.*;

public class DoomAccountManager {

    public static DoomAccountManager INSTANCE = new DoomAccountManager();
    private File tokenFile;
    private String token = null;

    public DoomAccountManager() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Doom");
        if (!dir.exists()) dir.mkdir();
        tokenFile = new File(dir, "doom_token.txt");
        loadToken();
    }

    public void saveToken(String newToken) {
        this.token = newToken;
        try (PrintWriter out = new PrintWriter(new FileWriter(tokenFile))) {
            out.write(newToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadToken() {
        if (!tokenFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(tokenFile))) {
            this.token = br.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getToken() { return token; }
    public boolean isLoggedIn() { return token != null && !token.isEmpty(); }
}