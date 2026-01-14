package doom.ui.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.InputStream;

public class FontManager {

    // Tutaj przechowujemy nasze czcionki
    // Regularne
    public static CustomFontRenderer r18;
    public static CustomFontRenderer r20;
    public static CustomFontRenderer r22;
    public static CustomFontRenderer r24;

    // Pogrubione (Bold)
    public static CustomFontRenderer b18;
    public static CustomFontRenderer b20;
    public static CustomFontRenderer icons18; // Do małych ikonek
    public static CustomFontRenderer icons24; // Do dużych ikonek (np. kategorie)

    public static void init() {
        // Ładowanie czcionek przy starcie
        // Zakładamy, że masz plik fontu w: src/main/resources/assets/minecraft/doom/fonts/font.ttf
        // Jeśli nie masz, użyjemy domyślnej systemowej "Arial" jako fallback.

        r18 = new CustomFontRenderer(getFont("font.ttf", 18, Font.PLAIN), true, true);
        r20 = new CustomFontRenderer(getFont("font.ttf",20, Font.PLAIN), true, true);
        r22 = new CustomFontRenderer(getFont("font.ttf",22, Font.PLAIN), true, true);
        r24 = new CustomFontRenderer(getFont("font.ttf",24, Font.PLAIN), true, true);

        b18 = new CustomFontRenderer(getFont("font.ttf",18, Font.BOLD), true, true);
        b20 = new CustomFontRenderer(getFont("font.ttf",20, Font.BOLD), true, true);


        icons18 = new CustomFontRenderer(getFont("icon.ttf", 18, Font.PLAIN), true, true);
        icons24 = new CustomFontRenderer(getFont("icon.ttf", 24, Font.PLAIN), true, true);
    }

    private static Font getFont(String fileName, int size, int style) {
        Font font;
        try {
            // Używamy ResourceManagera Minecrafta - to jest najpewniejsza metoda
            InputStream is = Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation("doom/fonts/" + fileName))
                    .getInputStream();

            font = Font.createFont(Font.TRUETYPE_FONT, is);
            font = font.deriveFont(style, size);
        } catch (Exception ex) {
            System.err.println("Error loading font: " + fileName);
            font = new Font("Arial", style, size);
        }
        return font;
    }
}