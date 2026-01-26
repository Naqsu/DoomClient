package doom.ui.hudeditor;

import doom.Client;
import doom.module.Module;
import doom.module.impl.render.ActiveModules;
import doom.module.impl.render.Watermark;
import doom.ui.font.FontManager;
import doom.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;

public class GuiHudEditor extends GuiScreen {

    private Module draggingModule = null;
    private float dragX = 0, dragY = 0;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. TŁO Z SIATKĄ (Grid)
        drawGrid(this.width, this.height);

        // Instrukcja na środku
        FontManager.b20.drawCenteredString("HUD EDITOR", this.width / 2f, 20, -1);
        FontManager.r18.drawCenteredString("Drag elements to move. Press ESC to save.", this.width / 2f, 35, 0xFFAAAAAA);

        // 2. RYSOWANIE MODUŁÓW
        for (Module m : Client.INSTANCE.moduleManager.getModules()) {
            // Edytujemy tylko włączone moduły HUD (te, które mają ustawione wymiary width/height)
            if (m.isToggled() && m.getCategory() == Module.Category.RENDER && !(m instanceof doom.module.impl.render.ClickGuiModule)) {

                // Specjalna obsługa ArrayListy w edytorze (żeby było widać przykładową listę)
                if (m instanceof ActiveModules) {
                    ((ActiveModules) m).render(m.x, m.y);
                    // Aktualizujemy wymiary bounding boxa dla myszki
                    m.width = ((ActiveModules) m).getWidth();
                    m.height = ((ActiveModules) m).getHeight();
                }
                else if (m instanceof Watermark) {
                    ((Watermark) m).render(m.x, m.y);
                }
                else {
                    // Inne moduły (TargetHUD, InfoHUD itp.) niech rysują się same
                    // Jeśli moduł nie ma metody render w swojej klasie bazowej, musisz ją wywołać przez event,
                    // ale tutaj zakładamy, że w metodzie onRender2D ustawiasz m.width i m.height.

                    // Rysujemy ramkę zastępczą jeśli moduł nic nie wyświetla w onUpdate/Render
                    // (Opcjonalnie)
                }

                // Rysowanie Ramki Edycji (Biały obrys)
                boolean hover = m.isHovered(mouseX, mouseY);
                int color = hover ? new Color(0, 255, 0, 150).getRGB() : new Color(255, 255, 255, 50).getRGB();

                RenderUtil.drawRoundedOutline(m.x - 2, m.y - 2, m.width + 4, m.height + 4, 4, 1.0f, color);

                if (draggingModule == m) {
                    // Dodatkowy glow przy przesuwaniu
                    RenderUtil.drawGlow(m.x, m.y, m.width, m.height, 10, new Color(0, 255, 0, 100).getRGB());
                }
            }
        }

        // 3. LOGIKA PRZESUWANIA
        if (draggingModule != null) {
            draggingModule.x = mouseX - dragX;
            draggingModule.y = mouseY - dragY;

            // Przyciąganie do krawędzi (Snapping) - opcjonalne
            snapToGrid(draggingModule);
        }

        // Reset
        if (!Mouse.isButtonDown(0)) {
            draggingModule = null;
        }
    }

    private void snapToGrid(Module m) {
        float snapDist = 5;
        ScaledResolution sr = new ScaledResolution(mc);

        if (Math.abs(m.x) < snapDist) m.x = 2; // Lewa
        if (Math.abs(m.y) < snapDist) m.y = 2; // Góra
        if (Math.abs((m.x + m.width) - sr.getScaledWidth()) < snapDist) m.x = sr.getScaledWidth() - m.width - 2; // Prawa
        if (Math.abs((m.y + m.height) - sr.getScaledHeight()) < snapDist) m.y = sr.getScaledHeight() - m.height - 2; // Dół
    }

    private void drawGrid(int w, int h) {
        RenderUtil.drawRect(0, 0, w, h, new Color(0, 0, 0, 200).getRGB()); // Ciemne tło
        int gridSize = 20;
        for (int x = 0; x < w; x += gridSize) {
            RenderUtil.drawRect(x, 0, x + 0.5f, h, 0x10FFFFFF);
        }
        for (int y = 0; y < h; y += gridSize) {
            RenderUtil.drawRect(0, y, w, y + 0.5f, 0x10FFFFFF);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            // Odwrócona pętla (żeby klikać te na wierzchu)
            java.util.List<Module> modules = Client.INSTANCE.moduleManager.getModules();
            for (int i = modules.size() - 1; i >= 0; i--) {
                Module m = modules.get(i);
                if (m.isToggled() && m.getCategory() == Module.Category.RENDER && m.width > 0) {
                    if (m.isHovered(mouseX, mouseY)) {
                        draggingModule = m;
                        dragX = mouseX - m.x;
                        dragY = mouseY - m.y;
                        return; // Złapaliśmy jeden, kończymy
                    }
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Client.INSTANCE.configManager.save();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}