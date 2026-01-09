package doom.ui.hudeditor;

import doom.Client;
import doom.module.Module;
import doom.util.RenderUtil;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;

public class GuiHudEditor extends GuiScreen {

    private Module draggingModule = null;
    private float dragX = 0, dragY = 0;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground(); // Przyciemnione tło

        mc.fontRendererObj.drawStringWithShadow("§cHUD Editor §7(Drag elements, ESC to save)", this.width / 2f - 80, 20, -1);

        // Przechodzimy przez wszystkie moduły
        for (Module m : Client.INSTANCE.moduleManager.getModules()) {
            // Interesują nas tylko włączone moduły, które mają ustawioną szerokość (czyli są HUD-em)
            if (m.isToggled() && m.width > 0) {

                // Rysujemy białą ramkę wokół elementu
                RenderUtil.drawRoundedOutline(m.x, m.y, m.width, m.height, 2, 1.0f, new Color(255, 255, 255, 100).getRGB());

                // Jeśli najeżdżasz myszką - podświetl
                if (m.isHovered(mouseX, mouseY)) {
                    RenderUtil.drawRoundedRect(m.x, m.y, m.width, m.height, 2, new Color(255, 255, 255, 50).getRGB());
                }
            }
        }

        // LOGIKA PRZESUWANIA
        if (draggingModule != null) {
            draggingModule.x = mouseX - dragX;
            draggingModule.y = mouseY - dragY;
        }

        // Jeśli puścisz przycisk myszy - przestań przesuwać
        if (!Mouse.isButtonDown(0)) {
            draggingModule = null;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) { // Lewy przycisk
            // Sprawdzamy, w co kliknąłeś
            for (Module m : Client.INSTANCE.moduleManager.getModules()) {
                if (m.isToggled() && m.width > 0 && m.isHovered(mouseX, mouseY)) {
                    draggingModule = m;
                    dragX = mouseX - m.x;
                    dragY = mouseY - m.y;
                    break;
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        // Zapisujemy config przy wyjściu z edytora!
        Client.INSTANCE.configManager.save();
    }
}