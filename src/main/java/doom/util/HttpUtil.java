package doom.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    public static String post(String urlString, String jsonInputString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            // Ustawienia połączenia
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Accept", "application/json");
            // Ważne: User-Agent, żeby nie zostać zablokowanym przez zabezpieczenia
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            con.setDoOutput(true);
            con.setDoInput(true);
            con.setConnectTimeout(10000); // 10s timeout
            con.setReadTimeout(10000);

            // Wysyłanie danych
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Odczyt odpowiedzi (Obsługa błędów)
            int status = con.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? con.getInputStream() : con.getErrorStream();

            if (is == null) {
                return "{\"success\":false, \"message\":\"Server returned empty response (Code: " + status + ")\"}";
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // Debug: Jeśli odpowiedź to HTML (zaczyna się od <), zwróć błąd JSON zamiast crasha
                String respStr = response.toString();
                if (respStr.trim().startsWith("<")) {
                    System.out.println("Server returned HTML instead of JSON: " + respStr); // Zobaczysz to w konsoli
                    return "{\"success\":false, \"message\":\"Server Error (HTML Response). Check console.\"}";
                }

                return respStr;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"success\":false, \"message\":\"Connection Exception: " + e.getMessage() + "\"}";
        }
    }

    // Metoda GET (dla configów)
    public static String getTextFromURL(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(5000);

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

            if(is == null) return "";

            BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
            in.close();
            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}