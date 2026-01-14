package doom.ui.clickgui.dropdown;

import doom.Client;
import doom.module.Module;
import doom.module.impl.render.ClickGuiModule;
import doom.util.AnimationUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DropdownClickGui extends GuiScreen {

    private final List<Panel> panels = new ArrayList<>();
    private float openProgress = 0.0f;

    public DropdownClickGui() {
        int x = 20;
        for (Module.Category category : Module.Category.values()) {
            panels.add(new Panel(category, x, 20));
            x += 110;
        }
    }

    @Override
    public void initGui() {
        openProgress = 0.0f;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Animation Logic
        openProgress = AnimationUtil.animate(1.0f, openProgress, 0.1f);
        float ease = AnimationUtil.getEase(openProgress, ClickGuiModule.easing.getMode());

        ScaledResolution sr = new ScaledResolution(mc);
        float cx = sr.getScaledWidth() / 2f;
        float cy = sr.getScaledHeight() / 2f;

        GL11.glPushMatrix();

        String animMode = ClickGuiModule.openAnim.getMode();
        switch (animMode) {
            case "Zoom":
                GL11.glTranslated(cx, cy, 0);
                GL11.glScalef(ease, ease, 1f);
                GL11.glTranslated(-cx, -cy, 0);
                break;
            case "SlideUp":
                GL11.glTranslated(0, (1.0f - ease) * sr.getScaledHeight(), 0);
                break;
        }

        this.drawDefaultBackground(); // Gradient background

        for (Panel panel : panels) {
            panel.drawScreen(mouseX, mouseY, partialTicks);
        }

        GL11.glPopMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (Panel panel : panels) {
            panel.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (Panel panel : panels) {
            panel.keyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }
    @Override
    public void onGuiClosed() {
        Module clickGui = Client.INSTANCE.moduleManager.getModule(ClickGuiModule.class);
        if (clickGui != null && clickGui.isToggled()) {
            clickGui.setToggled(false);
        }
        Client.INSTANCE.configManager.save();
    }
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}