package doom.module.impl.render;

import doom.Client;
import doom.module.Category;
import doom.module.DraggableModule;
import doom.module.Module;
import doom.util.ColorUtil;
import doom.util.RenderUtil; // Upewnij się, że masz ten import
import doom.util.StencilUtil; // Upewnij się, że masz ten import
import net.minecraft.client.gui.Gui;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveModules extends DraggableModule {

    public ActiveModules() {
        super("ArrayList", Category.RENDER);
        this.hidden = true;
        this.x = 2;
        this.y = 2;
    }

    @Override
    public float getWidth() {
        HUD hud = Client.INSTANCE.moduleManager.getModule(HUD.class);
        List<Module> mods = Client.INSTANCE.moduleManager.getModules();
        int maxWidth = 0;
        for (Module m : mods) {
            if (m.isToggled() && !m.hidden) {
                int w = mc.fontRendererObj.getStringWidth(getFormattedName(m, hud));
                if (w > maxWidth) maxWidth = w;
            }
        }
        return maxWidth + 6;
    }

    @Override
    public float getHeight() {
        long count = Client.INSTANCE.moduleManager.getModules().stream()
                .filter(Module::isToggled)
                .filter(m -> !m.hidden)
                .count();
        return count * 12; // Wysokość jednego modułu
    }

    @Override
    public void render(float x, float y) {
        // 1. Pobieranie ustawień
        HUD hud = Client.INSTANCE.moduleManager.getModule(HUD.class);
        boolean alignRight = hud.alignMode.is("Right");
        boolean showBackground = hud.background.isEnabled();
        boolean showSidebar = hud.sidebar.isEnabled();
        boolean rainbow = hud.rainbow.isEnabled();

        // 2. Sortowanie listy modułów
        List<Module> modules = Client.INSTANCE.moduleManager.getModules().stream()
                .filter(Module::isToggled)
                .filter(m -> !m.hidden)
                .sorted(Comparator.comparingInt(m -> -mc.fontRendererObj.getStringWidth(getFormattedName(m, hud))))
                .collect(Collectors.toList());

        // ====================================================================
        // PASS 1: STENCIL + BLUR (Tworzenie rozmycia w tle)
        // ====================================================================
        if (showBackground) {
            // Włączamy zapisywanie kształtu do wycięcia
            StencilUtil.initStencilToWrite();

            float maskY = y;
            for (Module m : modules) {
                String text = getFormattedName(m, hud);
                int textWidth = mc.fontRendererObj.getStringWidth(text);

                // Obliczamy pozycje dla maski
                float rectStart, rectEnd;
                if (alignRight) {
                    float rightEdge = x + getWidth();
                    rectStart = rightEdge - textWidth - 6;
                    rectEnd = rightEdge;
                } else {
                    rectStart = x;
                    rectEnd = x + textWidth + 6;
                }

                // Rysujemy kształt ("foremkę")
                RenderUtil.drawRoundedRect(rectStart, maskY, rectEnd - rectStart, 12, 4, -1);
                maskY += 12;
            }

            // Teraz mówimy: Rysuj BLUR tylko tam, gdzie narysowaliśmy kształty
            StencilUtil.readStencilBuffer(1);
            RenderUtil.drawFullPageBlur(20.0f); // Moc blura
            StencilUtil.uninitStencilBuffer();
        }

        // ====================================================================
        // PASS 2: OVERLAY + TEKST (To sprawia, że blur jest ciemny i czytelny)
        // ====================================================================

        float currentY = y;
        int count = 0;

        for (Module m : modules) {
            String displayText = getFormattedName(m, hud);
            int textWidth = mc.fontRendererObj.getStringWidth(displayText);

            // Obliczanie pozycji
            float rectStart, rectEnd, textX;
            if (alignRight) {
                float rightEdge = x + getWidth();
                rectStart = rightEdge - textWidth - 6;
                rectEnd = rightEdge;
                textX = rightEdge - textWidth - 4;
            } else {
                rectStart = x;
                rectEnd = x + textWidth + 6;
                textX = x + 4;
            }
            float width = rectEnd - rectStart;

            // --- [WAŻNE] PRZYCIEMNIENIE (OVERLAY) ---
            // To naprawia problem "wycinków z kamery". Nakładamy czarny kolor z przezroczystością.
            if (showBackground) {
                // Alpha 115 (szesnastkowo ok. 0x73) to idealny "szklany" odcień
                int overlayColor = new Color(0, 0, 0, 115).getRGB();
                RenderUtil.drawRoundedRect(rectStart, currentY, width, 12, 4, overlayColor);
            }

            // Kolor tekstu i paska
            int color = rainbow
                    ? ColorUtil.getRainbow(4.0f, 0.6f, 1.0f, count * 300)
                    : new Color(255, 60, 60).getRGB();

            // Pasek boczny (Sidebar)
            if (showSidebar) {
                float barX = alignRight ? (rectEnd - 2) : rectStart;
                RenderUtil.drawRoundedRect(barX, currentY, 2, 12, 1, color);
            }

            // Tekst
            mc.fontRendererObj.drawStringWithShadow(displayText, textX, currentY + 2, color);

            currentY += 12;
            count++;
        }
    }

    private String getFormattedName(Module m, HUD hud) {
        String s = m.getSuffix();
        if (s == null || s.isEmpty()) return m.getName();

        String mode = hud.suffixMode.getMode();
        switch (mode.toLowerCase()) {
            case "gray": return m.getName() + " \u00A77" + s;
            case "white": return m.getName() + " \u00A7f" + s;
            case "dash": return m.getName() + " \u00A77- " + s;
            case "bracket": return m.getName() + " \u00A77[" + s + "]";
            default: return m.getName();
        }
    }
}