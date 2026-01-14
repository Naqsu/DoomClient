package doom.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    public static String post(String urlString, String jsonInputString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        // --- KONFIGURACJA NAGŁÓWKÓW ---
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setDoOutput(true);
        con.setDoInput(true);

        // --- WYSYŁANIE DANYCH (BODY) ---
        // Zamieniamy String na bajty UTF-8
        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);

        // Ważne: Ustawiamy długość, żeby serwer wiedział ile czytać
        con.setRequestProperty("Content-Length", String.valueOf(input.length));

        try (OutputStream os = con.getOutputStream()) {
            os.write(input, 0, input.length);
            os.flush(); // Wymuszamy wysłanie
        }

        // --- ODCZYT ODPOWIEDZI ---
        // Sprawdzamy kod odpowiedzi, żeby obsłużyć błędy (np. 400/500)
        int status = con.getResponseCode();

        // Wybieramy strumień w zależności od statusu (błąd czy sukces)
        java.io.InputStream is = (status >= 200 && status < 300) ? con.getInputStream() : con.getErrorStream();

        if (is == null) return "{\"success\":false, \"message\":\"No response from server\"}";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }
}