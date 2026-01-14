package doom.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetworkUtil {

    public static String getTextFromURL(String urlString) {
        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Java/1.8");
            conn.setConnectTimeout(5000); // 5 sekund na połączenie
            conn.setReadTimeout(5000);    // 5 sekund na pobranie danych

            // Sprawdzamy kod HTTP (np. 200 OK, 404 Not Found)
            int status = conn.getResponseCode();

            // Jeśli błąd, czytamy ErrorStream, żeby wiedzieć dlaczego
            java.io.InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

            if (stream == null) return "";

            BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            in.close();

            // Jeśli status nie był OK, wypisz błąd w konsoli, ale nie crashuj
            if (status != 200) {
                System.err.println("[NetworkUtil] Error " + status + ": " + content.toString());
                return "";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return content.toString();
    }
}