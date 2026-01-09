package doom.module.impl.render;

import doom.Client;
import doom.module.Category;
import doom.module.DraggableModule;
import doom.module.impl.player.Scaffold;
import doom.util.RenderUtil;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class BlockCounter extends DraggableModule {

    public BlockCounter() {
        super("BlockCounter", Category.RENDER);
        this.x = 400; // Domyślna pozycja
        this.y = 300;
        this.setToggled(true); // Włączony domyślnie
    }

    @Override
    public float getWidth() {
        return 60; // Szerokość okienka
    }

    @Override
    public float getHeight() {
        return 24; // Wysokość okienka
    }

    @Override
    public void render(float x, float y) {
        // Pobieramy instancję Scaffolda
        Scaffold scaffold = Client.INSTANCE.moduleManager.getModule(Scaffold.class);

        // Rysujemy tylko gdy Scaffold jest włączony (lub jesteśmy w edytorze HUD)
        if (scaffold == null || !scaffold.isToggled()) {
            // W edytorze HUD chcemy widzieć "ducha" elementu, żeby go ustawić
            if (mc.currentScreen instanceof doom.ui.hudeditor.GuiHudEditor) {
                renderDummy(x, y);
            }
            return;
        }

        int count = 64; //scaffold.getBlockCount();
        ItemStack currentStack = mc.thePlayer.getCurrentEquippedItem();

        // Logika kolorów (Czerwony jak mało bloków, Biały jak dużo)
        String color = "\u00A7f"; // Biały
        if (count < 32) color = "\u00A7c"; // Czerwony
        else if (count < 64) color = "\u00A7e"; // Żółty

        // 1. TŁO
        // Ciemne tło z zaokrąglonymi rogami
        RenderUtil.drawRoundedRect(x, y, getWidth(), getHeight(), 4, new Color(20, 20, 20, 180).getRGB());

        // Czerwony pasek na dole (Styl Doom)
        RenderUtil.drawRoundedRect(x, y + getHeight() - 2, getWidth(), 2, 1, new Color(180, 0, 0).getRGB());

        // 2. RYSOWANIE BLOKU (ITEMU)
        // Musimy włączyć oświetlenie GUI, żeby blok wyglądał jak 3D
        GL11.glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        // Jeśli trzymamy blok, rysujemy go. Jeśli nie, szukamy pierwszego lepszego w eq (wizualnie)
        if (currentStack != null && currentStack.getItem() instanceof net.minecraft.item.ItemBlock) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(currentStack, (int)x + 4, (int)y + 4);
        } else {
            // Fallback - rysujemy np. kamień jeśli nic nie trzyma, albo pustkę
            // mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(net.minecraft.init.Blocks.stone), (int)x + 4, (int)y + 4);
        }

        RenderHelper.disableStandardItemLighting();
        GL11.glPopMatrix();

        // 3. TEKST (LICZBA)
        String text = color + count + " \u00A77blocks";

        // Wyśrodkowanie tekstu w pionie
        float fontY = y + (getHeight() / 2) - (mc.fontRendererObj.FONT_HEIGHT / 2);
        mc.fontRendererObj.drawStringWithShadow(text, x + 24, fontY, -1);
    }

    // To się wyświetla tylko w edytorze, gdy Scaffold jest wyłączony
    private void renderDummy(float x, float y) {
        RenderUtil.drawRoundedRect(x, y, getWidth(), getHeight(), 4, new Color(20, 20, 20, 100).getRGB());
        mc.fontRendererObj.drawStringWithShadow("Blocks", x + 15, y + 8, -1);
    }
}