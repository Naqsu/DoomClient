package doom.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    // Metoda do odczytywania pliku (zwraca listę linijek tekstu)
    public static List<String> readFile(File file) {
        List<String> lines = new ArrayList<>();
        try {
            if (!file.exists()) {
                return lines; // Jeśli plik nie istnieje, zwracamy pustą listę
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lines;
    }

    // Metoda do zapisywania listy tekstów do pliku
    public static void saveFile(File file, List<String> content) {
        try {
            // Jeśli folder nadrzędny nie istnieje (np. Configs), to go stwórz
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            PrintWriter writer = new PrintWriter(new FileWriter(file));
            for (String line : content) {
                writer.println(line);
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}