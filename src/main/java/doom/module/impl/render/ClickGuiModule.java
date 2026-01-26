package doom.module.impl.render;

import doom.Client;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.ColorSetting;
import doom.settings.impl.ModeSetting;
import doom.settings.impl.NumberSetting;
import doom.ui.clickgui.ClickGui;
import doom.ui.clickgui.FluxGui;
import doom.ui.clickgui.dashboard.DashboardGui;
import doom.ui.clickgui.dropdown.DropdownClickGui;
import doom.util.ColorUtil;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class ClickGuiModule extends Module {

    public static ModeSetting style = new ModeSetting("Style", null, "Dashboard", "Window", "Dropdown", "Dashboard");
    public static ModeSetting theme = new ModeSetting("Theme", null, "Custom", "Custom", "Rainbow", "Gradient");

    public static ModeSetting openAnim = new ModeSetting("Open Anim", null, "Zoom", "Zoom", "SlideUp", "Fade", "None");
    public static ModeSetting easing = new ModeSetting("Easing", null, "Expo", "Linear", "Elastic", "Expo", "Back", "Bounce");
    public static BooleanSetting hoverScale = new BooleanSetting("Hover Scale", null, true);

    public static ColorSetting customColor = new ColorSetting("Main Color", null, new Color(225, 30, 30).getRGB());
    public static ColorSetting gradientColor2 = new ColorSetting("Gradient Color 2", null, new Color(100, 0, 0).getRGB());
    public static NumberSetting animSpeed = new NumberSetting("Anim Speed", null, 3.0, 0.1, 10.0, 0.1);

    private DropdownClickGui dropdownGui;
    private ClickGui windowGui;
    private DashboardGui dashboardGui;

    public ClickGuiModule() {
        super("ClickGui", Keyboard.KEY_RSHIFT, Category.RENDER);

        style.parent = this;
        theme.parent = this;
        openAnim.parent = this;
        easing.parent = this;
        hoverScale.parent = this;
        customColor.parent = this;
        gradientColor2.parent = this;
        animSpeed.parent = this;

        Client.INSTANCE.settingsManager.rSetting(style);
        Client.INSTANCE.settingsManager.rSetting(theme);
        Client.INSTANCE.settingsManager.rSetting(openAnim);
        Client.INSTANCE.settingsManager.rSetting(easing);
        Client.INSTANCE.settingsManager.rSetting(hoverScale);
        Client.INSTANCE.settingsManager.rSetting(customColor);
        Client.INSTANCE.settingsManager.rSetting(gradientColor2);
        Client.INSTANCE.settingsManager.rSetting(animSpeed);

        customColor.setDependency(() -> theme.is("Custom") || theme.is("Gradient"));
        gradientColor2.setDependency(() -> theme.is("Gradient"));
        animSpeed.setDependency(() -> !theme.is("Custom"));
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            this.toggled = false;
            return;
        }

        String currentStyle = style.getMode();

        try {
            switch (currentStyle) {
                case "Window":
                    if (windowGui == null) windowGui = new ClickGui();
                    // REMOVED: windowGui.initGui(); -> This caused the crash!
                    mc.displayGuiScreen(windowGui);
                    break;

                case "Dropdown":
                    if (dropdownGui == null) dropdownGui = new DropdownClickGui();
                    // REMOVED: dropdownGui.initGui(); -> This caused the crash!
                    mc.displayGuiScreen(dropdownGui);
                    break;

                case "Dashboard": // Możesz nazwać to "Modern" w ustawieniach ModeSetting
                    if (mc.currentScreen instanceof FluxGui) return;
                    mc.displayGuiScreen(new FluxGui());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Client.addChatMessage("§cGUI Crash: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            this.toggled = false;
        }
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof ClickGui ||
                mc.currentScreen instanceof DropdownClickGui ||
                mc.currentScreen instanceof DashboardGui) {
            mc.displayGuiScreen(null);
        }
    }

    public static Color getGuiColor() {
        return getGuiColor(0);
    }

    public static Color getGuiColor(int index) {
        if (animSpeed == null || theme == null) return Color.WHITE;

        double speed = animSpeed.getValue();

        if (theme.is("Rainbow")) {
            return new Color(ColorUtil.getRainbow(4.0f, 0.6f, 1.0f, index * 200L));
        }
        else if (theme.is("Gradient")) {
            double time = (System.currentTimeMillis() * speed + index * 100) % 2000.0 / 1000.0;
            double wave = 0.5 + 0.5 * Math.sin(time * Math.PI);

            Color c1 = new Color(customColor.getColor());
            Color c2 = new Color(gradientColor2.getColor());

            int r = (int) (c1.getRed() * wave + c2.getRed() * (1.0 - wave));
            int g = (int) (c1.getGreen() * wave + c2.getGreen() * (1.0 - wave));
            int b = (int) (c1.getBlue() * wave + c2.getBlue() * (1.0 - wave));

            return new Color(r, g, b);
        }
        else {
            return new Color(customColor.getColor());
        }
    }
}