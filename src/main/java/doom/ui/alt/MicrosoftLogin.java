package doom.ui.alt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.minecraft.util.Session;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MicrosoftLogin {

    private static final String CLIENT_ID = "00000000402b5328";
    private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();

    public interface LoginCallback {
        // Zmieniamy sygnaturę, aby zwracała też RefreshToken
        void onSuccess(Session session, String refreshToken);
        void onStatus(String status);
        void onError(String error);
    }

    // --- LOGOWANIE WEB (Pierwsze użycie) ---
    public void loginWeb(LoginCallback callback) {
        try { new JFXPanel(); } catch (Exception e) {
            callback.onError("JavaFX Error. Ensure you run on JDK 8 with JavaFX.");
            return;
        }

        Platform.runLater(() -> {
            try {
                callback.onStatus("Opening Login Window...");
                WebView webView = new WebView();
                WebEngine engine = webView.getEngine();
                JFrame frame = new JFrame("Microsoft Login");
                JFXPanel jfxPanel = new JFXPanel();
                frame.add(jfxPanel);
                frame.setSize(500, 600);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                Platform.runLater(() -> {
                    Scene scene = new Scene(webView);
                    jfxPanel.setScene(scene);
                    java.net.CookieHandler.setDefault(new java.net.CookieManager());
                    engine.load("about:blank");
                    engine.setJavaScriptEnabled(true);
                    engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                });

                String authUrl = "https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID +
                        "&response_type=code&redirect_uri=" + REDIRECT_URI +
                        "&scope=XboxLive.signin%20offline_access&prompt=select_account";

                Platform.runLater(() -> {
                    engine.locationProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue != null && newValue.startsWith(REDIRECT_URI)) {
                            if (newValue.contains("code=")) {
                                String code = newValue.split("code=")[1];
                                if (code.contains("&")) code = code.split("&")[0];
                                frame.setVisible(false);
                                frame.dispose();
                                final String authCode = code;
                                executor.submit(() -> processCode(authCode, callback, false));
                            } else if (newValue.contains("error=")) {
                                frame.setVisible(false);
                                frame.dispose();
                                callback.onError("Login cancelled.");
                            }
                        }
                    });
                    engine.load(authUrl);
                });
            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("GUI Error: " + e.getMessage());
            }
        });
    }

    // --- LOGOWANIE Z ZAPISANEGO TOKENA (Bez GUI) ---
    public void loginWithRefreshToken(String refreshToken, LoginCallback callback) {
        executor.submit(() -> processCode(refreshToken, callback, true));
    }

    private void processCode(String codeOrToken, LoginCallback callback, boolean isRefresh) {
        try {
            callback.onStatus("Authorizing Microsoft...");

            // Tutaj pobieramy parę: [AccessToken, NewRefreshToken]
            String[] msTokens = isRefresh ? refreshMicrosoftToken(codeOrToken) : getMicrosoftToken(codeOrToken);
            String accessToken = msTokens[0];
            String newRefreshToken = msTokens[1];

            callback.onStatus("Authorizing Xbox Live...");
            String[] xblData = getXboxLiveToken(accessToken);

            callback.onStatus("Authorizing XSTS...");
            String xstsToken = getXSTSToken(xblData[0]);

            callback.onStatus("Logging into Minecraft...");
            String mcToken = getMinecraftToken(xblData[1], xstsToken);

            callback.onStatus("Fetching Profile...");
            Session session = getMinecraftProfile(mcToken);

            // Zwracamy sesję ORAZ nowy refresh token (one się zmieniają!)
            callback.onSuccess(session, newRefreshToken);

        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Auth Error: " + e.getMessage());
        }
    }

    // --- HTTP METHODS ---

    private String[] getMicrosoftToken(String code) throws IOException {
        String params = "client_id=" + CLIENT_ID + "&code=" + code + "&grant_type=authorization_code&redirect_uri=" + REDIRECT_URI;
        JsonObject json = post(TOKEN_URL, params, "application/x-www-form-urlencoded");
        if (!json.has("access_token")) throw new IOException("No access token: " + json);

        // ZWRACAMY AccessToken ORAZ RefreshToken
        String access = json.get("access_token").getAsString();
        String refresh = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;
        return new String[]{access, refresh};
    }

    private String[] refreshMicrosoftToken(String refreshToken) throws IOException {
        String params = "client_id=" + CLIENT_ID + "&refresh_token=" + refreshToken + "&grant_type=refresh_token&redirect_uri=" + REDIRECT_URI;
        JsonObject json = post(TOKEN_URL, params, "application/x-www-form-urlencoded");
        if (!json.has("access_token")) throw new IOException("Refresh failed: " + json);

        String access = json.get("access_token").getAsString();
        // Czasami API zwraca nowy refresh token, czasami używamy starego. Bezpieczniej pobrać nowy.
        String refresh = json.has("refresh_token") ? json.get("refresh_token").getAsString() : refreshToken;
        return new String[]{access, refresh};
    }

    // Reszta metod bez większych zmian, tylko pomocnicze...
    private String[] getXboxLiveToken(String accessToken) throws IOException {
        String url = "https://user.auth.xboxlive.com/user/authenticate";
        JsonObject payload = new JsonObject();
        JsonObject props = new JsonObject();
        props.addProperty("AuthMethod", "RPS");
        props.addProperty("SiteName", "user.auth.xboxlive.com");
        props.addProperty("RpsTicket", "d=" + accessToken);
        payload.add("Properties", props);
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        JsonObject json = post(url, payload.toString(), "application/json");
        String token = json.get("Token").getAsString();
        String uhs = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
        return new String[]{token, uhs};
    }

    private String getXSTSToken(String xblToken) throws IOException {
        String url = "https://xsts.auth.xboxlive.com/xsts/authorize";
        JsonObject payload = new JsonObject();
        JsonObject props = new JsonObject();
        props.addProperty("SandboxId", "RETAIL");
        props.add("UserTokens", gson.toJsonTree(new String[]{xblToken}));
        payload.add("Properties", props);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");
        JsonObject json = post(url, payload.toString(), "application/json");
        return json.get("Token").getAsString();
    }

    private String getMinecraftToken(String uhs, String xstsToken) throws IOException {
        String url = "https://api.minecraftservices.com/authentication/login_with_xbox";
        JsonObject payload = new JsonObject();
        payload.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
        JsonObject json = post(url, payload.toString(), "application/json");
        return json.get("access_token").getAsString();
    }

    private Session getMinecraftProfile(String mcToken) throws IOException {
        URL obj = new URL("https://api.minecraftservices.com/minecraft/profile");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + mcToken);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            JsonObject json = gson.fromJson(in, JsonObject.class);
            return new Session(json.get("name").getAsString(), json.get("id").getAsString(), mcToken, "mojang");
        }
    }

    private JsonObject post(String url, String data, String contentType) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", contentType);
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) { os.write(data.getBytes(StandardCharsets.UTF_8)); }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            return gson.fromJson(in, JsonObject.class);
        } catch (IOException e) {
            // Obsługa błędów, żeby zobaczyć co Microsoft zwraca
            if(con.getErrorStream() != null) {
                try(BufferedReader err = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                    String errorLine = err.readLine();
                    // Parsowanie błędu jako JSON jeśli to możliwe
                    try { return gson.fromJson(errorLine, JsonObject.class); } catch(Exception ex) {}
                    throw new IOException("API Error: " + errorLine);
                }
            }
            throw e;
        }
    }
}